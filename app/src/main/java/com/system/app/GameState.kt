package com.system.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent "Solo Leveling" state: level, XP, the five attributes, and tasks.
 * Stored locally on the device via SharedPreferences (no server, no login).
 */

data class Attributes(
    var strength: Int = 5,
    var vitality: Int = 5,
    var agility: Int = 5,
    var intelligence: Int = 5,
    var perception: Int = 5
) {
    fun valueFor(attribute: String): Int = when (attribute) {
        "STRENGTH" -> strength
        "VITALITY" -> vitality
        "AGILITY" -> agility
        "INTELLIGENCE" -> intelligence
        "PERCEPTION" -> perception
        else -> 0
    }

    fun add(attribute: String, points: Int) {
        when (attribute) {
            "STRENGTH" -> strength += points
            "VITALITY" -> vitality += points
            "AGILITY" -> agility += points
            "INTELLIGENCE" -> intelligence += points
            "PERCEPTION" -> perception += points
        }
    }
}

data class TaskItem(
    val id: String,
    var title: String,
    var attribute: String, // STRENGTH | VITALITY | AGILITY | INTELLIGENCE | PERCEPTION
    var xp: Int,
    var done: Boolean,
    var recurringDay: Int? = null // 1..7 (Calendar.DAY_OF_WEEK), null = one-off
)

data class GameState(
    var level: Int = 1,
    var xp: Int = 0,
    var jobTitle: String = "Novice",
    var attributes: Attributes = Attributes(),
    var tasks: MutableList<TaskItem> = mutableListOf()
)

data class LevelUpResult(val newLevel: Int, val attributeGained: String)

object GameRepository {
    private const val PREFS = "system_gamestate"
    private const val KEY_STATE = "state_json"

    fun xpForLevel(level: Int): Int = (100 * Math.pow(1.15, level.toDouble())).toInt()

    /** How much XP a task is worth, roughly scaled by current level so late-game tasks matter more. */
    fun xpForTask(baseXp: Int, level: Int): Int = baseXp + (level / 3)

    fun load(context: Context): GameState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_STATE, null) ?: return defaultState()
        return try {
            fromJson(raw)
        } catch (e: Exception) {
            defaultState()
        }
    }

    fun save(context: Context, state: GameState) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STATE, toJson(state)).apply()
    }

    /** Marks a task done/undone, applies XP + level-up logic, persists, and returns a level-up event if one happened. */
    fun toggleTask(context: Context, state: GameState, taskId: String): LevelUpResult? {
        val task = state.tasks.find { it.id == taskId } ?: return null
        var result: LevelUpResult? = null

        if (!task.done) {
            task.done = true
            val gained = xpForTask(task.xp, state.level)
            state.xp += gained
            var threshold = xpForLevel(state.level)
            while (state.xp >= threshold) {
                state.xp -= threshold
                state.level += 1
                state.attributes.add(task.attribute, 1)
                result = LevelUpResult(state.level, task.attribute)
                threshold = xpForLevel(state.level)
            }
        } else {
            task.done = false
            // undo isn't perfectly symmetric (levels already banked), intentionally simple
        }
        save(context, state)
        return result
    }

    fun addTask(context: Context, state: GameState, title: String, attribute: String, xp: Int, recurringDay: Int?) {
        state.tasks.add(TaskItem(id = System.currentTimeMillis().toString(), title = title, attribute = attribute, xp = xp, done = false, recurringDay = recurringDay))
        save(context, state)
    }

    private fun defaultState(): GameState {
        val state = GameState()
        state.tasks.addAll(
            listOf(
                TaskItem("seed1", "Zi de spate — sală", "STRENGTH", 40, false, recurringDay = 2),
                TaskItem("seed2", "Research nișă eCommerce", "INTELLIGENCE", 25, false),
                TaskItem("seed3", "Diagnoză cod eroare OBD2", "PERCEPTION", 30, false)
            )
        )
        return state
    }

    private fun toJson(state: GameState): String {
        val obj = JSONObject()
        obj.put("level", state.level)
        obj.put("xp", state.xp)
        obj.put("jobTitle", state.jobTitle)
        val attrs = JSONObject()
        attrs.put("strength", state.attributes.strength)
        attrs.put("vitality", state.attributes.vitality)
        attrs.put("agility", state.attributes.agility)
        attrs.put("intelligence", state.attributes.intelligence)
        attrs.put("perception", state.attributes.perception)
        obj.put("attributes", attrs)
        val tasksArr = JSONArray()
        for (t in state.tasks) {
            val to = JSONObject()
            to.put("id", t.id)
            to.put("title", t.title)
            to.put("attribute", t.attribute)
            to.put("xp", t.xp)
            to.put("done", t.done)
            to.put("recurringDay", t.recurringDay ?: -1)
            tasksArr.put(to)
        }
        obj.put("tasks", tasksArr)
        return obj.toString()
    }

    private fun fromJson(raw: String): GameState {
        val obj = JSONObject(raw)
        val attrsObj = obj.getJSONObject("attributes")
        val attributes = Attributes(
            strength = attrsObj.getInt("strength"),
            vitality = attrsObj.getInt("vitality"),
            agility = attrsObj.getInt("agility"),
            intelligence = attrsObj.getInt("intelligence"),
            perception = attrsObj.getInt("perception")
        )
        val tasksArr = obj.getJSONArray("tasks")
        val tasks = mutableListOf<TaskItem>()
        for (i in 0 until tasksArr.length()) {
            val to = tasksArr.getJSONObject(i)
            val day = to.getInt("recurringDay")
            tasks.add(
                TaskItem(
                    id = to.getString("id"),
                    title = to.getString("title"),
                    attribute = to.getString("attribute"),
                    xp = to.getInt("xp"),
                    done = to.getBoolean("done"),
                    recurringDay = if (day == -1) null else day
                )
            )
        }
        return GameState(
            level = obj.getInt("level"),
            xp = obj.getInt("xp"),
            jobTitle = obj.optString("jobTitle", "Novice"),
            attributes = attributes,
            tasks = tasks
        )
    }
}

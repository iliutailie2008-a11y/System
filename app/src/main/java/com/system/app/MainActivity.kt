package com.system.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------- Colors ----------
val BgDark = Color(0xFF04060D)
val BlueCore = Color(0xFF0B3D91)
val BlueGlow = Color(0xFF3B8FE8)
val BlueBright = Color(0xFF7FD4FF)
val PurpleCore = Color(0xFF4C1D95)
val PurpleGlow = Color(0xFFA855F7)
val PurpleBright = Color(0xFFE2B6FF)
val GlassBg = Color(0x11FFFFFF)
val GlassBorder = Color(0x22FFFFFF)
val TextDim = Color(0xFF8B93AB)

data class AccentSet(val core: Color, val glow: Color, val bright: Color)

class MainActivity : ComponentActivity() {
    private var voiceManager: VoiceManager? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — UI mic button will simply fail silently if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = VoiceManager(applicationContext)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            SystemApp(voiceManager = voiceManager!!)
        }
    }

    override fun onDestroy() {
        voiceManager?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun SystemApp(voiceManager: VoiceManager) {
    var soloMode by remember { mutableStateOf(false) }
    val accent = animateAccent(soloMode)
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var gameState by remember { mutableStateOf(GameRepository.load(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(soloMode = soloMode, onToggle = { soloMode = !soloMode }, accent = accent)
            ParticleSphere(
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
            TabBar(selectedTab, accent) { selectedTab = it }
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ChatPanel(accent, voiceManager)
                    1 -> TasksPanel(accent, gameState) { gameState = GameRepository.load(context) }
                    else -> StatsPanel(accent, gameState)
                }
            }
        }
    }
}

@Composable
fun animateAccent(soloMode: Boolean): AccentSet {
    val target = if (soloMode) AccentSet(PurpleCore, PurpleGlow, PurpleBright)
    else AccentSet(BlueCore, BlueGlow, BlueBright)
    val core by animateColorAsState(target.core, label = "core")
    val glow by animateColorAsState(target.glow, label = "glow")
    val bright by animateColorAsState(target.bright, label = "bright")
    return AccentSet(core, glow, bright)
}

fun greetingText(): Pair<String, String> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "BUNĂ DIMINEAȚA, MASTER"
        in 12..17 -> "BUNĂ ZIUA, MASTER"
        in 18..22 -> "BUNĂ SEARA, MASTER"
        else -> "E TÂRZIU, MASTER"
    }
    val days = listOf("Duminică", "Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă")
    val cal = Calendar.getInstance()
    val dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return greeting to dayName
}

@Composable
fun TopBar(soloMode: Boolean, onToggle: () -> Unit, accent: AccentSet) {
    val (greeting, day) = remember { greetingText() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 22.dp, 20.dp, 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = greeting,
                color = accent.bright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(text = day, color = TextDim, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GlassBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .then(noRippleClickable(onToggle))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "SOLO LEVELING",
                color = accent.bright,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------- Particle Sphere ----------
data class Particle(val phi: Double, val theta: Double, val rOffset: Double, val speed: Double, val noise: Double)

@Composable
fun ParticleSphere(accent: AccentSet, modifier: Modifier = Modifier) {
    val particles = remember {
        val n = 700
        (0 until n).map { i ->
            val idx = i + 0.5
            val phi = kotlin.math.acos(1 - 2 * idx / n)
            val theta = Math.PI * (1 + kotlin.math.sqrt(5.0)) * idx
            Particle(phi, theta, Random.nextDouble(0.94, 1.03), Random.nextDouble(0.15, 0.4), Random.nextDouble(0.0, 1000.0))
        }
    }
    var t by remember { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        while (true) {
            t += 0.02
            delay(16)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val radius = size.minDimension * 0.36f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rotY = t * 0.5
            val rotX = sin(t * 0.3) * 0.15

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.core.copy(alpha = 0.7f), accent.core.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius * 1.3f
                ),
                radius = radius * 1.3f,
                center = Offset(cx, cy)
            )

            val projected = particles.map { p ->
                val wobble = sin(t * p.speed * 4 + p.noise) * 2.0
                val r = radius * p.rOffset + wobble
                val x = r * sin(p.phi) * cos(p.theta + rotY)
                val y = r * cos(p.phi)
                val z = r * sin(p.phi) * sin(p.theta + rotY)
                val y2 = y * cos(rotX) - z * sin(rotX)
                val z2 = y * sin(rotX) + z * cos(rotX)
                val scale = 300.0 / (300.0 + z2)
                Triple(cx + (x * scale).toFloat(), cy + (y2 * scale).toFloat(), z2)
            }.sortedBy { it.third }

            for ((x, y, z) in projected) {
                val depthFactor = ((z + radius) / (radius * 2)).toFloat().coerceIn(0f, 1f)
                val alpha = (0.15f + depthFactor * 0.75f).coerceIn(0f, 1f)
                val dotSize = (0.8f + depthFactor * 1.8f)
                drawCircle(
                    color = (if (depthFactor > 0.7f) accent.bright else accent.glow).copy(alpha = alpha),
                    radius = dotSize,
                    center = Offset(x, y)
                )
            }
        }
        Text(
            text = "SYSTEM ONLINE",
            color = TextDim,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

// ---------- Tabs ----------
@Composable
fun TabBar(selected: Int, accent: AccentSet, onSelect: (Int) -> Unit) {
    val labels = listOf("Chat", "Quests", "Status")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 6.dp, 20.dp, 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val isActive = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isActive) Brush.horizontalGradient(listOf(accent.bright, accent.glow))
                        else Brush.horizontalGradient(listOf(GlassBg, GlassBg))
                    )
                    .then(noRippleClickable { onSelect(i) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(),
                    color = if (isActive) BgDark else TextDim,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------- Chat panel ----------
data class ChatMsg(val fromSystem: Boolean, val text: String)

val initialMessages = listOf(
    ChatMsg(true, "Bună seara, Master. Sunt online și conectat. Cu ce te pot ajuta?")
)

@Composable
fun ChatPanel(accent: AccentSet, voiceManager: VoiceManager) {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(*initialMessages.toTypedArray()) }
    var isThinking by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send(overrideText: String? = null) {
        val text = (overrideText ?: input).trim()
        if (text.isEmpty() || isThinking) return
        messages.add(ChatMsg(false, text))
        input = ""
        isThinking = true
        scope.launch {
            val history = messages.map { it.fromSystem.not() to it.text }
            val result = ClaudeApiClient.sendMessage(history)
            isThinking = false
            result.onSuccess { reply ->
                messages.add(ChatMsg(true, reply))
                voiceManager.speak(reply)
            }.onFailure { err ->
                val errText = "Nu am putut ajunge la server, Master. (${err.message ?: "eroare necunoscută"})"
                messages.add(ChatMsg(true, errText))
            }
        }
    }

    val wakeListener = remember {
        WakeWordListener(context) { command ->
            isListening = false
            send(command)
        }
    }

    DisposableEffect(Unit) {
        onDispose { wakeListener.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg, accent)
            }
            if (isThinking) {
                item { ThinkingBubble(accent) }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(GlassBg)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Scrie-i lui System...", color = TextDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isListening) Brush.linearGradient(listOf(PurpleBright, PurpleGlow))
                        else Brush.linearGradient(listOf(accent.bright, accent.glow))
                    )
                    .then(noRippleClickable {
                        if (input.isNotBlank()) {
                            send()
                        } else {
                            isListening = !isListening
                            if (isListening) wakeListener.start() else wakeListener.stop()
                        }
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83C\uDF99", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ThinkingBubble(accent: AccentSet) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GlassBg)
                .padding(14.dp)
        ) {
            Text("System procesează...", color = accent.bright, fontSize = 13.sp)
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMsg, accent: AccentSet) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromSystem) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (msg.fromSystem) GlassBg else BlueCore)
                .padding(14.dp)
        ) {
            Column {
                if (msg.fromSystem) {
                    Text("SYSTEM", color = accent.bright, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
                Text(msg.text, color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

// ---------- Tasks panel ----------
@Composable
fun TasksPanel(accent: AccentSet, gameState: GameState, onChanged: () -> Unit) {
    val context = LocalContext.current
    var levelUpMessage by remember { mutableStateOf<String?>(null) }
    var newTaskText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        levelUpMessage?.let { msg ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(accent.core, accent.glow)))
                        .padding(12.dp)
                ) {
                    Text(msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { SectionLabel("Quests de azi") }
        items(gameState.tasks, key = { it.id }) { q ->
            QuestRow(q, accent) {
                val result = GameRepository.toggleTask(context, gameState, q.id)
                if (result != null) {
                    levelUpMessage = "Level Up! Ești acum Level ${result.newLevel}. +1 ${result.attributeGained}"
                }
                onChanged()
            }
        }
        item { SectionLabel("Adaugă quest nou") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(GlassBg)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ex: alergare 5km luni și joi...", color = TextDim) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(listOf(accent.bright, accent.glow)))
                        .then(noRippleClickable {
                            if (newTaskText.isNotBlank()) {
                                val attribute = guessAttribute(newTaskText)
                                GameRepository.addTask(context, gameState, newTaskText.trim(), attribute, 25, null)
                                newTaskText = ""
                                onChanged()
                            }
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 20.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Very simple keyword-based guess for which attribute a task belongs to.
 * Good enough as a default — a future version can ask Claude to classify it instead.
 */
fun guessAttribute(text: String): String {
    val t = text.lowercase()
    return when {
        listOf("sală", "spate", "picioare", "piept", "alergare", "cardio", "antrenament", "gym", "fugă").any { t.contains(it) } -> "STRENGTH"
        listOf("somn", "mâncare", "dietă", "sănătate", "apă").any { t.contains(it) } -> "VITALITY"
        listOf("viteză", "reflex", "mobilitate").any { t.contains(it) } -> "AGILITY"
        listOf("research", "citit", "studiu", "business", "ecommerce", "shopify", "curs", "învăț").any { t.contains(it) } -> "INTELLIGENCE"
        listOf("diagnoză", "cod eroare", "auto", "mașină", "motor", "electric").any { t.contains(it) } -> "PERCEPTION"
        else -> "INTELLIGENCE"
    }
}

@Composable
fun QuestRow(q: TaskItem, accent: AccentSet, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (q.done) accent.glow else Color.Transparent)
                .border(1.dp, accent.glow, RoundedCornerShape(7.dp))
                .then(noRippleClickable { onToggle() })
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(q.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(q.attribute, color = TextDim, fontSize = 11.sp)
        }
        Text("+${q.xp} XP", color = accent.bright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextDim,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(4.dp, 8.dp)
    )
}

// ---------- Stats panel ----------
data class StatEntry(val short: String, val name: String, val value: Int, val max: Int)

@Composable
fun StatsPanel(accent: AccentSet, gameState: GameState) {
    val a = gameState.attributes
    val stats = listOf(
        StatEntry("STR", "Strength", a.strength, 30),
        StatEntry("VIT", "Vitality", a.vitality, 30),
        StatEntry("AGI", "Agility", a.agility, 30),
        StatEntry("INT", "Intelligence", a.intelligence, 30),
        StatEntry("PER", "Perception", a.perception, 30)
    )
    val xpThreshold = GameRepository.xpForLevel(gameState.level)
    val progress = (gameState.xp.toFloat() / xpThreshold).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBg)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(gameState.jobTitle.uppercase(), color = accent.bright, fontSize = 13.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text("LVL ${gameState.level}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x14FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(accent.core, accent.bright)))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${gameState.xp} XP", color = TextDim, fontSize = 11.sp)
                    Text("$xpThreshold XP", color = TextDim, fontSize = 11.sp)
                }
            }
        }
        item { SectionLabel("Atribute") }
        items(stats) { s -> StatRow(s, accent) }
    }
}

@Composable
fun StatRow(s: StatEntry, accent: AccentSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .padding(12.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x0FFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(s.short, color = accent.bright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(s.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x14FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((s.value.toFloat() / s.max))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(accent.core, accent.bright)))
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(s.value.toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

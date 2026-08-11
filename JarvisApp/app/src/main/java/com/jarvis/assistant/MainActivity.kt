package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.jarvis.assistant.databinding.ActivityMainBinding
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeech
    private var apiClient: ClaudeApiClient? = null
    private val history = mutableListOf<Pair<String, String>>()
    private val conversationLog = StringBuilder()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                sendMessage(spoken)
            }
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput() else toast("Am nevoie de acces la microfon pentru comenzi vocale.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this) { }
        tts.language = Locale("ro", "RO")

        loadApiKeyOrPrompt()

        binding.sendButton.setOnClickListener {
            val text = binding.inputText.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }

        binding.micButton.setOnClickListener {
            checkMicPermissionAndListen()
        }

        binding.settingsButton.setOnClickListener {
            promptForApiKey()
        }

        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun loadApiKeyOrPrompt() {
        val prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
        val savedKey = prefs.getString("api_key", null)
        if (savedKey.isNullOrBlank()) {
            promptForApiKey()
        } else {
            apiClient = ClaudeApiClient(savedKey)
        }
    }

    private fun promptForApiKey() {
        val input = AppCompatEditText(this)
        input.hint = "sk-ant-..."
        AlertDialog.Builder(this)
            .setTitle("Cheia API Anthropic")
            .setMessage("Introdu cheia ta API (din console.anthropic.com). Se salvează doar local, pe telefon.")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
                        .edit().putString("api_key", key).apply()
                    apiClient = ClaudeApiClient(key)
                    toast("Cheie salvată.")
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun checkMicPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spune comanda...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            toast("Recunoașterea vocală nu e disponibilă pe acest telefon.")
        }
    }

    private fun sendMessage(text: String) {
        val client = apiClient
        if (client == null) {
            toast("Adaugă mai întâi cheia API din Setări.")
            return
        }
        appendToLog("Tu: $text")
        binding.inputText.setText("")

        client.sendMessage(
            userMessage = text,
            history = history,
            onResult = { reply ->
                runOnUiThread {
                    val (spokenPart, action) = extractAction(reply)
                    appendToLog("Jarvis: $spokenPart")
                    history.add("user" to text)
                    history.add("assistant" to reply)
                    tts.speak(spokenPart, TextToSpeech.QUEUE_FLUSH, null, null)
                    action?.let { JarvisActions.serviceInstance?.executeAction(it) }
                }
            },
            onError = { error ->
                runOnUiThread {
                    appendToLog("Eroare: $error")
                    toast(error)
                }
            }
        )
    }

    /** Separă textul vorbit de blocul opțional [ACTION:{...}] pus de model la final. */
    private fun extractAction(reply: String): Pair<String, JSONObject?> {
        val marker = "[ACTION:"
        val idx = reply.indexOf(marker)
        if (idx == -1) return reply to null
        val closingIdx = reply.lastIndexOf("]")
        if (closingIdx == -1 || closingIdx < idx) return reply to null
        val jsonPart = reply.substring(idx + marker.length, closingIdx)
        val spokenPart = reply.substring(0, idx).trim()
        return try {
            spokenPart to JSONObject(jsonPart)
        } catch (e: Exception) {
            reply to null
        }
    }

    private fun appendToLog(line: String) {
        conversationLog.append(line).append("\n\n")
        binding.conversationView.text = conversationLog.toString()
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}

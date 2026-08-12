package com.system.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Continuous listener built on Android's on-device SpeechRecognizer.
 * While active, it keeps restarting itself so it's always listening.
 * When it hears a phrase containing the wake word "system", it extracts
 * whatever comes after it and passes it to onCommand as the actual request.
 *
 * This is a pragmatic wake-word approach that needs no external account
 * or custom model file — everything works out of the box. It's less
 * battery-efficient than a dedicated wake-word engine (like Picovoice),
 * which is the trade-off for zero extra setup.
 */
class WakeWordListener(
    private val context: Context,
    private val onCommand: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    fun start() {
        if (listening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        listening = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull()?.lowercase().orEmpty()
                if (heard.contains("system")) {
                    val command = heard.substringAfter("system").trim(':', ' ', ',', '.')
                    if (command.isNotBlank()) onCommand(command)
                }
                restart()
            }

            override fun onError(error: Int) {
                restart()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        listenOnce()
    }

    private fun listenOnce() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            restart()
        }
    }

    private fun restart() {
        if (listening) listenOnce()
    }

    fun stop() {
        listening = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}

package com.system.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Speaks System's replies aloud with a male voice, auto-detecting
 * Romanian vs English from the text so System can speak both.
 */
class VoiceManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                pickMaleVoice(Locale("ro", "RO"))
            }
        }
    }

    private fun pickMaleVoice(locale: Locale) {
        val engine = tts ?: return
        engine.language = locale
        val voices: Set<Voice>? = engine.voices
        val maleVoice = voices?.firstOrNull {
            it.locale == locale && !it.name.contains("female", ignoreCase = true)
        }
        if (maleVoice != null) {
            engine.voice = maleVoice
        }
    }

    private fun looksRomanian(text: String): Boolean {
        val diacritics = listOf('ă', 'â', 'î', 'ș', 'ş', 'ț', 'ţ')
        if (text.any { it.lowercaseChar() in diacritics }) return true
        val commonRoWords = listOf(" master", " și ", " sunt ", " este ", " nu ", " da ", " bine")
        val lower = " ${text.lowercase()} "
        return commonRoWords.any { lower.contains(it) }
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready) return
        val locale = if (looksRomanian(text)) Locale("ro", "RO") else Locale.US
        engine.language = locale
        pickMaleVoice(locale)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "system_utterance")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

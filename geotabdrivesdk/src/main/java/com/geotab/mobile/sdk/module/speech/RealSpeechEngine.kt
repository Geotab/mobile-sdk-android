package com.geotab.mobile.sdk.module.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import com.geotab.mobile.sdk.publicInterfaces.SpeechEngine
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

private data class Phrase(val text: String, val rate: Float, val language: String)

class RealSpeechEngine(context: Context) : SpeechEngine, TextToSpeech.OnInitListener {
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var ready: Boolean = false
    private val phrases: ConcurrentLinkedQueue<Phrase> = ConcurrentLinkedQueue()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            phrases.forEach { speak(it) }
            phrases.clear()
        }
    }

    private fun speak(phrase: Phrase) {
        speak(phrase.text, phrase.rate, phrase.language)
    }

    override fun speak(text: String, rate: Float, language: String) {
        if (ready) {
            tts.language = Locale(language)
            tts.setSpeechRate(rate)
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "")
        } else {
            phrases.add(Phrase(text, rate, language))
        }
    }

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

package com.geotab.mobile.sdk.module.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import com.geotab.mobile.sdk.publicInterfaces.SpeechEngine
import java.util.Locale

class RealSpeechEngine(context: Context) : SpeechEngine, TextToSpeech.OnInitListener {
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var ready: Boolean = false
    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    override fun speak(text: String, rate: Float, language: String) {
        tts.language = Locale(language)
        tts.setSpeechRate(rate)
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "")
    }

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

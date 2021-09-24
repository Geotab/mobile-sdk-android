package com.geotab.mobile.sdk.module.speech

import android.content.Context
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.publicInterfaces.SpeechEngine

class SpeechModule(var context: Context, override val name: String = "speech") : Module(name) {
    companion object {
        const val SPEECH_TEXT_NULL = "Speech text can\'t be null"
    }
    var speechEngine: SpeechEngine = RealSpeechEngine(context)

    init {
        functions.add(NativeSpeakFunction(module = this))
    }

    fun engineShutDown() {
        speechEngine.shutdown()
    }
}

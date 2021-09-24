package com.geotab.mobile.sdk.publicInterfaces

interface SpeechEngine {
    fun speak(text: String, rate: Float, language: String)
    fun shutdown()
}

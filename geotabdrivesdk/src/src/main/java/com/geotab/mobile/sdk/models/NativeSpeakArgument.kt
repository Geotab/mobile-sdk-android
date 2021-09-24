package com.geotab.mobile.sdk.models

import com.geotab.mobile.sdk.module.speech.SpeechModule.Companion.SPEECH_TEXT_NULL

data class NativeSpeakArgument(
    val text: String?,
    val rate: Float?,
    val lang: String?
) {
    val speechText
        get() = text ?: throw Exception(SPEECH_TEXT_NULL)
    val speechRate
        get() = rate ?: 1.0f
    val speechLang
        get() = lang ?: "en_US"
}

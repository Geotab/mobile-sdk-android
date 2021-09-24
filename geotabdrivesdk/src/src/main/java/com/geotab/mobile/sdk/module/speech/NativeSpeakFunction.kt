package com.geotab.mobile.sdk.module.speech

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.NativeSpeakArgument
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class NativeSpeakFunction(
    override val name: String = "nativeSpeak",
    override val module: SpeechModule
) : ModuleFunction, BaseFunction<NativeSpeakArgument>() {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        try {
            val arguments = this.transformOrInvalidate(jsonString, jsCallback) ?: return
            module.speechEngine.speak(arguments.speechText, arguments.speechRate, arguments.speechLang)
            jsCallback(Success("undefined"))
        } catch (exception: Exception) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_SPEECH_ERROR, exception.message)))
        }
    }

    override fun getType(): Type {
        return object : TypeToken<NativeSpeakArgument>() {}.type
    }
}

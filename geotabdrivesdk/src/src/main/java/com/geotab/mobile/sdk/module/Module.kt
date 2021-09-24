package com.geotab.mobile.sdk.module

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheFactory
import java.io.IOException
import java.io.Reader
import java.io.Serializable
import java.io.StringWriter

@Keep
abstract class Module(open val name: String) : Serializable {
    companion object {
        const val geotabModules = "geotabModules"
        const val geotabNativeCallbacks = "___geotab_native_callbacks"
        const val callbackPrefix = "geotab_native_api_"
        const val interfaceName = "AndroidFunctionProvider"
        lateinit var mustacheFactory: MustacheFactory
    }

    var functions = ArrayList<ModuleFunction>()

    fun findFunction(name: String): ModuleFunction? {
        return functions.find { it.name == name }
    }

    private fun getScriptData(): HashMap<String, Any> {
        return hashMapOf(
            "geotabModules" to geotabModules,
            "moduleName" to name
        )
    }

    open fun scripts(context: Context): String {
        var script = getScriptFromTemplate(context, "Module.Script.js", getScriptData())
        for (func in functions) {
            script += func.scripts(context)
        }
        return script
    }

    fun getScriptFromTemplate(context: Context, templateFileName: String, scriptData: HashMap<String, Any>): String {
        var mustache: Mustache? = null
        try {
            mustache = mustacheFactory.compile(getFunctionTemplate(context, templateFileName), templateFileName)
        } catch (e: IOException) {
            Log.e(templateFileName, e.message, e)
        }
        val stringWriter = StringWriter()
        mustache?.execute(stringWriter, scriptData)
        return stringWriter.toString()
    }

    @Throws(IOException::class)
    open fun getFunctionTemplate(context: Context, templateFileName: String): Reader {
        return context.assets.open(templateFileName).bufferedReader()
    }
}

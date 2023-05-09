package com.geotab.mobile.sdk.module.appearance

import android.app.UiModeManager
import android.content.Context
import com.geotab.mobile.sdk.models.enums.AppearanceType
import com.geotab.mobile.sdk.module.Module

class AppearanceModule(
    private val context: Context
) : Module(MODULE_NAME) {
    private val appearanceProperty = "appearanceType"

    companion object {
        const val MODULE_NAME = "appearance"
    }

    override fun scripts(context: Context): String {
        var scripts = super.scripts(context)
        val appearanceType = getAppearance()
        scripts += """window.$geotabModules.$name.$appearanceProperty = ${appearanceType.appearanceTypeId};"""
        return scripts
    }

    private fun getAppearance(): AppearanceType {
        val uiManager: UiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

        return if (uiManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
            AppearanceType.DARK
        } else {
            AppearanceType.LIGHT
        }
    }
}

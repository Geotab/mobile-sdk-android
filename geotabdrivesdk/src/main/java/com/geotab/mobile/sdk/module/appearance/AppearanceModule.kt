package com.geotab.mobile.sdk.module.appearance

import android.app.UiModeManager
import android.content.Context
import com.geotab.mobile.sdk.models.enums.AppearanceType
import com.geotab.mobile.sdk.module.Module

class AppearanceModule(
    private val context: Context,
    override val name: String = "appearance"
) : Module(name) {
    private val appearanceProperty = "appearanceType"

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

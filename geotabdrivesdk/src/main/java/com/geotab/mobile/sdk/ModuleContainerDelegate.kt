package com.geotab.mobile.sdk

import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction

interface ModuleContainerDelegate {
    fun findModule(module: String): Module?
    fun findModuleFunction(module: String, function: String): ModuleFunction?
}

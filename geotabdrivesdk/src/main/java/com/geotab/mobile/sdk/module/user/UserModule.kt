package com.geotab.mobile.sdk.module.user

import com.geotab.mobile.sdk.models.DriverActionNecessaryArgument
import com.geotab.mobile.sdk.models.LoginRequiredArgument
import com.geotab.mobile.sdk.module.Module

typealias DriverActionNecessaryCallbackType = (driverActionArg: DriverActionNecessaryArgument) -> Unit
typealias PageNavigationCallbackType = (path: String) -> Unit
typealias LoginRequiredCallbackType = (loginRequiredArg: LoginRequiredArgument) -> Unit

class UserModule : Module(MODULE_NAME) {
    var driverActionNecessaryCallback: DriverActionNecessaryCallbackType = {}
    var pageNavigationCallback: PageNavigationCallbackType = {}
    var loginRequiredCallback: LoginRequiredCallbackType = {}

    init {
        functions.add(GetAllUsersFunction(module = this))
        functions.add(GetViolationsFunction(module = this))
        functions.add(SetDriverSeatFunction(module = this))
        functions.add(GetAvailabilityFunction(module = this))
        functions.add(GetMinAvailabilityHtmlFunction(module = this))
        functions.add(GetHosRuleSetFunction(module = this))
        functions.add(DriverActionNecessaryFunction(module = this))
        functions.add(PageNavigationFunction(module = this))
        functions.add(LoginRequiredFunction(module = this))
        functions.add(GetOpenCabAvailabilityFunction(module = this))
    }

    companion object {
        const val MODULE_NAME = "user"
    }
}

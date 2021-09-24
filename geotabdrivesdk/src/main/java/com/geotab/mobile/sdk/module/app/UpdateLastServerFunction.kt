package com.geotab.mobile.sdk.module.app

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Success

class UpdateLastServerFunction(
    override val name: String = "updateLastServer",
    override val module: AppModule
) : ModuleFunction {

    /*** Gives a regular expersion pattern for domain validation
     *  Domain name should be A-Z|a-z|0-9|-
     *  Domain name should be between 1 to 63 character long
     *  Top level should be between 2 to 6 character long
     *  Domain name should not start or end with hypen
     *  Domain can be a subdomain
     */
    private fun getPattern() = """^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\.)+[A-Za-z]{2,6}$"""

    /**
     * Handle incoming JavaScript calls from Geotab Drive's web component
     *
     * @param jsonString object from JavaScript caller to parse
     * @param jsCallback callback to notify JavaScript caller of [Success] or [Failure]
     */
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (com.geotab.mobile.sdk.module.Result<Success<String>, Failure>) -> Unit
    ) {
        val serverStr = jsonString?.takeIf { it.isNotBlank() } ?: run {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
            return
        }
        val pattern = Regex(getPattern())
        pattern.find(serverStr)?.let {
            module.lastServerUpdatedCallback(serverStr)
            jsCallback(Success("undefined"))
        } ?: run {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
        }
    }
}

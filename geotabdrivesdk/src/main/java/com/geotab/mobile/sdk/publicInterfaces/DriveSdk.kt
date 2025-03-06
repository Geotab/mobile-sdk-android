package com.geotab.mobile.sdk.publicInterfaces

import com.geotab.mobile.sdk.models.publicModels.CredentialResult
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.app.LastServerUpdatedCallbackType
import com.geotab.mobile.sdk.module.user.DriverActionNecessaryCallbackType
import com.geotab.mobile.sdk.module.user.LoginRequiredCallbackType
import com.geotab.mobile.sdk.module.user.PageNavigationCallbackType

interface DriveSdk {
    /**
     * Charging state of the Android device
     * @return [Boolean] if the device is charging or not.
     */
    val isCharging: Boolean

    /**
     * Callback to inform that the Web application can't be loaded, due to network failure.
     */
    var webAppLoadFailed: (() -> Unit)?

    /**
     * Returns all users, including the active driver and co-drivers
     * @param includeAllUsers defaults to true, to return all users including the active driver and co-drivers
     * if set to false, it will return only the active driver
     * @param callback returns error when there is no user logged in. Otherwise returns a Json string with all logged in users
     * @return Json string with all logged in users
     */

    fun getAllUsers(includeAllUsers: Boolean = true, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns all the violations the active driver has since the start of their cycle
     * @return Json with user violations
     */
    fun getUserViolations(userName: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns the active driver's availability hours
     * @return Json string with the duty status availability
     */
    fun getAvailability(userName: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns the active driver's Minimum availability as html
     * @return Html string with the minimum availability
     */
    fun getMinAvailabilityHtml(userName: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns the active driver's availability hours for OpenCab
     * @return Json string with the duty status availability
     */
    fun getOpenCabAvailability(version: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns the active driver's duty status log
     * @return Json string with the duty status log
     */
    fun getDutyStatusLog(userName: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns the active driver's current driving log
     * @return Json string with the driving log
     */
    fun getCurrentDrivingLog(userName: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * - set the vehicle's active driver to the driver id provided.
     * - only works with driverId that are logged in
     * - tries to perform a Set API call, or updates local database if offline
     * @return Json string with the user
     */
    fun setDriverSeat(driverId: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns the configuration of the active driver's current hours of service rule
     * @return Json string with the HOS rule set
     */
    fun getHosRuleSet(userName: String, callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Returns Json string with properties of the GO Device
     * @return Json string with the GO Device state
     */
    fun getStateDevice(callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Sets the [SpeechEngine] used by the SpeechModule
     */
    fun setSpeechEngine(speechEngine: SpeechEngine)

    /**
     * `driverActionNecessary` is a list of events that occur in our app where the application owner needs to bring the Drive app activity UI to the foreground.
     * Example: "Your vehicle has been selected by another driver. You now need to select a vehicle".
     * Set a callback to listen for driverActionNecessary events.
     */
    fun setDriverActionNecessaryCallback(callback: DriverActionNecessaryCallbackType)

    /**
     * Clears the previously set driverActionNecessary callback.
     */
    fun clearDriverActionNecessaryCallback()

    /**
     * pageNavigation event indicates any navigation changes by the driver in Geotab Drive.
     * Set a callback to listen for Web Drive's page navigation changes.
     */
    fun setPageNavigationCallback(callback: PageNavigationCallbackType)

    /**
     * Clears the previously set pageNavigation callback.
     */
    fun clearPageNavigationCallback()

    /**
     * Set a callback to listen for session changes. That includes: no session, invalid session, session expired, co-driver login is requested.
     * There are three defined values and variance of different error messages that could be passed in the callback.
     * - "", empty string, indicates no login required or login is successful, or the login is in progress. At this state, implementor should present the DriveViewController/Fragment.
     * - "LoginRequired": indicates the login UI is going to show a login form (No valid user is available or the current activeSession is expired/invalid). At this state, implementor presents their own login screen.
     * - "AddCoDriver": indicates that a co-driver login is requested. At this state, implementor presents their own co-driver login screen.
     * - "<Any error message>", any other error messages. At this state, implementor presents its own login screen.
     */
    fun setLoginRequiredCallback(callback: LoginRequiredCallbackType)

    /**
     * Clears the previously set loginRequired callback.
     */
    fun clearLoginRequiredCallback()

    /**
     * Sets a valid session credential obtained from calling MyGeotab Login API. Set isCoDriver to true if you are adding co-driver.
     */
    fun setSession(credentialResult: CredentialResult, isCoDriver: Boolean = false)

    /**
     * Cancels the co-driver login view and go back to the drive view
     */
    fun cancelLogin()

    /**
     * Set a callback to listen for "last server address" change event.
     * LastServerUpdatedCallbackType is a function with the new server address as the argument.
     */
    fun setLastServerAddressUpdatedCallback(callback: LastServerUpdatedCallbackType)

    /**
     * Clears the previously set "last server address" callback
     */
    fun clearLastServerAddressUpdatedCallback()

    /**
     * Set custom url for the webview with the given path and last server address will be the host.
     */
    fun setCustomURLPath(path: String)

    /**
     * Get device events from IOXBLE and IOXUSB as Json string in the callback.
     * Callback returns Json string {Type:Int and DeviceEvent : Json String}
     *  - Type being the type of IOX, 0 for USB and 1 for BLE
     *  - DeviceEvent is the Json string of the device event object.
     */
    fun getDeviceEvents(callback: (Result<Success<String>, Failure>) -> Unit)

    /**
     * Set a callback to get notified when the SDK's app module is initialized.
     */
    fun setDriveReadyListener(callback: () -> Unit)
}

package com.geotab.mobile.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.lifecycle.lifecycleScope
import com.geotab.mobile.sdk.databinding.FragmentGeotabDriveSdkBinding
import com.geotab.mobile.sdk.fileChooser.FileChooserHelper
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.logging.Logging
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.models.publicModels.CredentialResult
import com.geotab.mobile.sdk.module.BaseCallbackFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.NetworkErrorDelegate
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.models.database.AppDatabase
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.module.app.AppModule
import com.geotab.mobile.sdk.module.app.LastServerUpdatedCallbackType
import com.geotab.mobile.sdk.module.appearance.AppearanceModule
import com.geotab.mobile.sdk.module.auth.AuthModule
import com.geotab.mobile.sdk.module.auth.AuthUtil
import com.geotab.mobile.sdk.module.battery.BatteryModule
import com.geotab.mobile.sdk.module.browser.BrowserModule
import com.geotab.mobile.sdk.module.camera.CameraDelegate
import com.geotab.mobile.sdk.module.camera.CameraModule
import com.geotab.mobile.sdk.module.camera.GetPictureAttribute
import com.geotab.mobile.sdk.module.camera.TakePictureContract
import com.geotab.mobile.sdk.module.connectivity.ConnectivityModule
import com.geotab.mobile.sdk.module.device.DeviceModule
import com.geotab.mobile.sdk.module.dutyStatusLog.DutyStatusLogModule
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule
import com.geotab.mobile.sdk.module.geolocation.GeolocationModule
import com.geotab.mobile.sdk.module.iox.ioxBle.IoxBleModule
import com.geotab.mobile.sdk.module.iox.ioxUsb.IoxUsbModule
import com.geotab.mobile.sdk.module.localNotification.LocalNotificationModule
import com.geotab.mobile.sdk.module.photoLibrary.PhotoLibraryDelegate
import com.geotab.mobile.sdk.module.photoLibrary.PhotoLibraryModule
import com.geotab.mobile.sdk.module.photoLibrary.PickImageAttribute
import com.geotab.mobile.sdk.module.photoLibrary.PickImageContract
import com.geotab.mobile.sdk.module.screen.ScreenModule
import com.geotab.mobile.sdk.module.secureStorage.SecureStorageModule
import com.geotab.mobile.sdk.module.speech.SpeechModule
import com.geotab.mobile.sdk.module.sso.SSOModule
import com.geotab.mobile.sdk.module.state.DeviceFunction
import com.geotab.mobile.sdk.module.state.StateModule
import com.geotab.mobile.sdk.module.user.DriverActionNecessaryCallbackType
import com.geotab.mobile.sdk.module.dutyStatusLog.GetDutyStatusLogFunction
import com.geotab.mobile.sdk.module.dutyStatusLog.GetCurrentDrivingLogFunction
import com.geotab.mobile.sdk.module.login.LoginModule
import com.geotab.mobile.sdk.module.user.GetAllUsersFunction
import com.geotab.mobile.sdk.module.user.GetAvailabilityFunction
import com.geotab.mobile.sdk.module.user.GetHosRuleSetFunction
import com.geotab.mobile.sdk.module.user.GetMinAvailabilityHtmlFunction
import com.geotab.mobile.sdk.module.user.GetOpenCabAvailabilityFunction
import com.geotab.mobile.sdk.module.user.GetViolationsFunction
import com.geotab.mobile.sdk.module.user.LoginRequiredCallbackType
import com.geotab.mobile.sdk.module.user.PageNavigationCallbackType
import com.geotab.mobile.sdk.module.user.SetDriverSeatFunction
import com.geotab.mobile.sdk.module.user.UserModule
import com.geotab.mobile.sdk.module.webview.WebViewModule
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionAttribute
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import com.geotab.mobile.sdk.permission.PermissionResultContract
import com.geotab.mobile.sdk.publicInterfaces.DriveSdk
import com.geotab.mobile.sdk.publicInterfaces.SpeechEngine
import com.geotab.mobile.sdk.util.PushScriptUtil
import com.geotab.mobile.sdk.util.UserAgentUtil
import com.geotab.mobile.sdk.util.serializable
import com.github.mustachejava.DefaultMustacheFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// fragment initialization parameters
private const val ARG_MODULES = "modules"
const val MODULE_PREF_DRIVE = "MODULE_PREF"

/**
 * A simple [Fragment] subclass.
 * Use the [DriveFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DriveFragment :
    BaseGeotabFragment(),
    DriveSdk,
    CameraDelegate,
    PhotoLibraryDelegate,
    PermissionDelegate,
    ModuleContainerDelegate,
    NetworkErrorDelegate {
    private var _binding: FragmentGeotabDriveSdkBinding? = null
    private val alphaVersionString = "alpha"

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val pushScriptUtil: PushScriptUtil by lazy {
        PushScriptUtil()
    }

    /**
     * Provides access to the WebView for lifecycle-safe execution checks.
     * Implementation of abstract method from BaseGeotabFragment.
     */
    override fun getWebView(): WebView? = webView

    val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit =
        { moduleEvent, callBack ->
            val validEvent = pushScriptUtil.validEvent(moduleEvent, callBack)

            if (validEvent) {
                val script = """
    window.dispatchEvent(new CustomEvent("${moduleEvent.event}", ${moduleEvent.params}));
"""

                this.webView?.post {
                    executeIfValid {
                        this.webView?.evaluateJavascript(script, null)
                    }
                }

                callBack(Success(""))
            }
        }

    private val evaluate: (String, (String) -> Unit) -> Unit =
        { script: String, callback: (String) -> Unit ->
            this.webView?.post {
                executeIfValid {
                    this.webView?.evaluateJavascript(script) { result ->
                        // Double-check lifecycle state before invoking  callback
                        executeIfValid {
                            callback(result)
                        }
                    }
                }
            }
        }

    private val goBack = {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            moveAppToBackground()
        }
    }

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance()
    }

    private val mustacheFactory by lazy { DefaultMustacheFactory() }
    private lateinit var preference: SharedPreferences
    private var appPreferences: SharedPreferences? = null

    private val userAgentUtil: UserAgentUtil by lazy {
        UserAgentUtil(requireContext())
    }

    private var webView: WebView? = null
    private var bigQueryLogListener: com.geotab.mobile.sdk.logging.BigQueryLogListener? = null
    private lateinit var errorView: View

    private var isWebViewConfigured: Boolean = false
    private val contentController = WebViewClientUserContentController(this)
    private var modules: ArrayList<Module> = arrayListOf()
    private var geotabCredentials: CredentialResult? = null
    private var customUrl: String? = null
    private var logger: Logging = Logger.shared
    private val userModule: UserModule by lazy {
        UserModule()
    }
    private val dutyStatusLogModule: DutyStatusLogModule by lazy {
        DutyStatusLogModule()
    }
    private val startForPermissionResult = registerForActivityResult(PermissionResultContract()) {
        it.callback(it.result)

        if (it.wasPermissionAttributeNull) {
            Logger.shared.debug(TAG, it.permissions.toString())
            Logger.shared.error(TAG, "Permissions being asked: ${it.permissions}")
        }
    }
    private val takePicture = registerForActivityResult(TakePictureContract()) {
        it.callback(it.result)
    }
    private val pickImage = registerForActivityResult(PickImageContract()) {
        it.callback(it.uri)
    }
    private val speechModule: SpeechModule by lazy {
        SpeechModule(requireContext())
    }
    private val geolocationModule: GeolocationModule by lazy {
        GeolocationModule(
            requireContext(),
            permissionDelegate = this,
            evaluate = evaluate,
            push = push
        )
    }

    private val appModule: AppModule by lazy {
        AppModule(evaluate = evaluate, push = push, moveAppToBackground = ::moveAppToBackground)
    }
    private val deviceModule: DeviceModule by lazy {
        DeviceModule(requireContext(), preference, userAgentUtil)
    }
    private val batteryModule: BatteryModule by lazy {
        BatteryModule(requireContext(), push = push)
    }

    private val appearanceModule: AppearanceModule by lazy {
        AppearanceModule(requireContext())
    }

    private val ioxUsbModule: IoxUsbModule by lazy {
        IoxUsbModule(requireContext(), push = push)
    }
    private val ioxbleModule: IoxBleModule by lazy {
        IoxBleModule(requireContext(), permissionDelegate = this, push = push, evaluate = evaluate)
    }

    private val webViewModule: WebViewModule? by lazy {
        activity?.let { WebViewModule(it, goBack) }
    }

    private val ssoModule: SSOModule by lazy {
        SSOModule(
            this.parentFragmentManager,
            appPreferences
        )
    }
    private val authUtil: AuthUtil by lazy {
        AuthUtil.init(secureStorageRepository, STORAGE_PREFIX)
    }

    private var loginModule: LoginModule? = null
    private var authModule: AuthModule? = null

    private val secureStorageRepository: SecureStorageRepository by lazy {
        SecureStorageRepository(
            requireContext().packageName,
            AppDatabase.getDatabase(requireContext()).secureStorageDao()
        )
    }

    private val secureStorageModule: SecureStorageModule by lazy {
        SecureStorageModule(secureStorageRepository)
    }

    private val modulesInternal: ArrayList<Module?> by lazy {
        arrayListOf(
            deviceModule,
            activity?.let { ScreenModule(it) },
            userModule,
            dutyStatusLogModule,
            StateModule(),
            speechModule,
            context?.let { BrowserModule(this.parentFragmentManager, it) },
            webViewModule,
            context?.let { LocalNotificationModule(it, this) },
            batteryModule,
            appearanceModule,
            appModule,
            context?.let { ConnectivityModule(it, evaluate, push) },
            context?.let { FileSystemModule(it) },
            context?.let { CameraModule(it, this, this, this) },
            context?.let { PhotoLibraryModule(it, this, this) },
            ioxUsbModule,
            geolocationModule,
            ioxbleModule,
            ssoModule,
            secureStorageModule
        )
    }

    override val isCharging
        get() = batteryModule.isCharging

    override var webAppLoadFailed: (() -> Unit)? = null

    private val moduleScripts: String by lazy {
        var scripts = """
            window.${Module.geotabModules} = {};
            window.${Module.geotabNativeCallbacks} = {};
        """
        context?.let {
            for (module in modules) {
                scripts += module.scripts(it)
            }

            // This line needs to be the last one added to the scripts, after all the
            // initialization
            scripts += deviceModule.getScriptFromTemplate(
                it,
                "Module.DeviceReady.Script.js",
                hashMapOf()
            )
        }

        scripts
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Module.mustacheFactory = mustacheFactory
        arguments?.let { bundle ->
            initializeModules((bundle.serializable<ArrayList<*>>(ARG_MODULES))?.filterIsInstance<Module>())
        }
        activity?.onBackPressedDispatcher?.addCallback(onBackPressedCallback)
        contentController.setWebViewCallBack(webViewModule?.onBackPressedCallback)
        contentController.setAppCallBack(onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeotabDriveSdkBinding.inflate(inflater, container, false)
        webView = binding.geotabDriveSdkWebview
        // The following might show as an error in the IDE. It thinks errorLayout is a view, not a viewbinding
        errorView = binding.errorLayout.root
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The following might show as an error in the IDE. It thinks errorLayout is a view, not a viewbinding
        binding.errorLayout.refreshButton.setOnClickListener {
            webView?.let { webView ->
                if (webView.url != null) {
                    webView.reload()
                }
                webView.visibility = View.VISIBLE
            }

            errorView.visibility = View.GONE
        }

        configureWebView()
        configureWebViewScript(contentController)

        batteryModule.startMonitoringBatteryStatus()

        with(appModule) {
            initValues(this@DriveFragment.requireContext())
            // Add BigQuery listener to the broadcaster
            bigQueryLogListener = com.geotab.mobile.sdk.logging.BigQueryLogListener(push)
            bigQueryLogListener?.let { listener ->
                (Logger.shared as? com.geotab.mobile.sdk.logging.LogBroadcaster)?.addListener(listener)
            }
            startForegroundService()
            driveReadyCallback()
        }
        if (DriveSdkConfig.includeAppAuthModules) {
            loginModule?.let { module ->
                with(module) {
                    initValues(requireActivity())
                }
            }
            authModule?.let { module ->
                with(module) {
                    initValues(requireActivity())
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            authUtil.startTokenRefresh(requireContext())
                        }
                    }
                }
            }
        }

        ioxUsbModule.start()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preference = context.getSharedPreferences(MODULE_PREF_DRIVE, Context.MODE_PRIVATE)
    }

    override fun onDetach() {
        super.onDetach()
        batteryModule.stopMonitoringBatteryStatus()
        speechModule.engineShutDown()
    }

    override fun onDestroy() {
        webView?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.apply {
                removeJavascriptInterface(Module.interfaceName)
                removeAllViews()
                destroy()
            }
        }

        webView = null

        ioxUsbModule.stop()

        context?.let {
            authUtil.dispose(it)
        }

        // Remove BigQuery listener to prevent memory leaks
        bigQueryLogListener?.let { listener ->
            (Logger.shared as? com.geotab.mobile.sdk.logging.LogBroadcaster)?.removeListener(listener)
        }
        bigQueryLogListener = null

        appModule.stopForegroundService()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        val delayVisibilityInMilliseconds = 1000L
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                delay(delayVisibilityInMilliseconds)
                webView?.dispatchWindowVisibilityChanged(View.VISIBLE)
            }
        }
    }

    @Keep
    companion object {
        private const val STORAGE_PREFIX = "geotabDrive_@"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param modules list of modules to send to fragment
         * @return A new instance of fragment GeotabDriveFragment.
         */
        @JvmStatic
        fun newInstance(
            modules: ArrayList<Module> = arrayListOf(),
            logger: Logging = Logger.shared,
            appPreferences: SharedPreferences? = null
        ): DriveFragment =
            DriveFragment().apply {
                this.logger = logger
                this.appPreferences = appPreferences
                arguments = Bundle().apply {
                    putSerializable(ARG_MODULES, modules)
                }
            }

        private const val TAG = "DriveFragment"
    }

    private fun initializeModules(modules: List<Module>?) {
        if (modules != null) {
            this.modules = ArrayList(modules)
        }
        this.modules.addAll(modulesInternal.filterNotNull())
        if (DriveSdkConfig.includeAppAuthModules) {
            loginModule = LoginModule(authUtil)
            authModule = AuthModule(authUtil)
            loginModule?.let { this.modules.add(it) }
            authModule?.let { this.modules.add(it) }
        }
        logger.info(TAG, "modules initialized")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {

        if (BuildConfig.DEBUG || getString(R.string.app_flavor) == alphaVersionString) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView?.let { webView ->
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                setSupportMultipleWindows(true)
                mediaPlaybackRequiresUserGesture = false
                userAgentString = userAgentUtil.getUserAgent(webView.settings.userAgentString)
                setGeolocationEnabled(true)
            }

            cookieManager.setAcceptThirdPartyCookies(
                webView,
                DriveSdkConfig.allowThirdPartyCookies
            )

            logger.info(TAG, "loading webView")
            webView.loadUrl("javascript:document.open();document.close();")
        }
    }

    private fun configureWebViewScript(webViewClientUserContentController: WebViewClientUserContentController) {
        val url = customUrl
        if (url != null) {
            logger.info(TAG, "opening custom url")
            setUrlToWebView(url)
        } else {
            logger.info(TAG, "opening geotab drive url")
            val geotabDriveUrl = "https://${DriveSdkConfig.serverAddress}/drive/default.html"
            geotabCredentials?.let {
                this.webView?.loadUrl("$geotabDriveUrl#ui/login,(server:'${it.path}',credentials:(database:'${(it.credentials.database)}',sessionId:'${(it.credentials.sessionId)}',userName:'${(it.credentials.userName)}'))")
            } ?: run { this.webView?.loadUrl(geotabDriveUrl) }
        }
        webViewClientUserContentController.addScriptOnPageFinished(moduleScripts)

        context?.let {
            val downloadFiles = DownloadFiles(evaluate, it, this)
            this.webView?.setDownloadListener(downloadFiles)
            this.webView?.addJavascriptInterface(downloadFiles, DownloadFiles.interfaceName)
        }

        this.webView?.addJavascriptInterface(this, Module.interfaceName)
        this.webView?.webViewClient = webViewClientUserContentController
        isWebViewConfigured = true
        this.webView?.webChromeClient = context?.let {
            WebViewChromeClient(
                PermissionHelper(it, this),
                FileChooserHelper(this)
            )
        }
    }

    @JavascriptInterface
    fun postMessage(name: String, function: String, result: String, callback: String) {
        executeIfValid {
            try {
                val jsonObject = JSONObject(result)
                val params: String? =
                    if (jsonObject.isNull("result")) null else jsonObject.getString("result")
                val moduleFunction = findModuleFunction(name, function)
                if (moduleFunction == null) {
                    buildErrorJavaScript(callback, Error(GeotabDriveError.JS_ISSUED_ERROR, "Module function not found for $name, $function"))
                } else {
                    callModuleFunction(moduleFunction, callback, params)
                }
            } catch (e: Exception) {
                val crashLocation = " in module $name, function $function with params $result."
                val crashMessage = e.message ?: "Unknown JS exception occurred"

                Logger.shared.error(TAG, crashMessage + crashLocation)
                // callback needs to be removed, after accumulating will lead to JNI errors
                buildErrorJavaScript(
                    callback,
                    Error(GeotabDriveError.JS_ISSUED_ERROR, crashMessage)
                )
            }
        }
    }

    override fun takePictureResult(imageUri: Uri, callback: (Boolean) -> Unit) {
        takePicture.launch(GetPictureAttribute(imageUri = imageUri, callback = callback))
    }

    override fun askPermissionsResult(permissions: Array<Permission>, callback: (Boolean) -> Unit) {
        startForPermissionResult.launch(
            PermissionAttribute(
                permissions = permissions.toCollection(
                    ArrayList()
                ),
                callback = callback
            )
        )
    }

    override fun pickImageResult(callback: (Uri?) -> Unit) {
        pickImage.launch(PickImageAttribute(input = "image/*", uri = null, callback = callback))
    }

    override fun findModule(module: String): Module? {
        return modules.firstOrNull { it.name == module }
    }

    override fun findModuleFunction(module: String, function: String): ModuleFunction? {
        val resModule = modules.firstOrNull { it.name == module }
        return resModule?.findFunction(function)
    }

    override fun onNetworkError() {
        logger.error(TAG, "network error - web app load failed")
        errorView.let {
            webView?.visibility = View.GONE
            it.visibility = View.VISIBLE
        }
        webAppLoadFailed?.invoke()
    }

    private fun buildSuccessJavaScript(callback: String, value: String): String {
        return """
        try {
            var t = $callback(null, $value);
            if (t instanceof Promise) {
                t.catch(err => { console.log(">>>>> Unexpected exception in Promise: ", err); });
            }
        } catch(err) {
            console.log(">>>>> Unexpected exception in callback: ", err);
        }
        """.trimIndent()
    }

    private fun callModuleFunction(
        moduleFunction: ModuleFunction,
        callback: String,
        params: String?
    ) {
        moduleFunction.handleJavascriptCall(params) { result ->
            executeIfValid {
                val jsScript = when (result) {
                    is Success -> buildSuccessJavaScript(callback, result.value)
                    is Failure -> {
                        logger.error(
                            TAG,
                            "module function call failed, ${result.reason}, $moduleFunction"
                        )
                        buildErrorJavaScript(callback, result.reason)
                    }
                }
                evaluate(jsScript) {}
            }
        }
    }

    override fun getAllUsers(includeAllUsers: Boolean, callback: (Result<Success<String>, Failure>) -> Unit) {
        (findModuleFunction(UserModule.MODULE_NAME, "getAll") as? GetAllUsersFunction)?.let {
            it.includeAllUsers = includeAllUsers
            functionCall(callback, it)
        }
    }

    override fun getUserViolations(
        userName: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(UserModule.MODULE_NAME, "getViolations") as? GetViolationsFunction)?.let {
            it.userName = userName
            functionCall(callback, it)
        }
    }

    override fun getAvailability(
        userName: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(UserModule.MODULE_NAME, "getAvailability") as? GetAvailabilityFunction)?.let {
            it.userName = userName
            functionCall(callback, it)
        }
    }

    override fun getDutyStatusLog(
        userName: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(DutyStatusLogModule.MODULE_NAME, "getDutyStatusLog") as? GetDutyStatusLogFunction)?.let {
            it.userName = userName
            functionCall(callback, it)
        }
    }

    override fun getCurrentDrivingLog(
        userName: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(DutyStatusLogModule.MODULE_NAME, "getCurrentDrivingLog") as? GetCurrentDrivingLogFunction)?.let {
            it.userName = userName
            functionCall(callback, it)
        }
    }

    override fun getMinAvailabilityHtml(
        userName: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(UserModule.MODULE_NAME, "getMinAvailabilityHtml") as? GetMinAvailabilityHtmlFunction)?.let {
            it.userName = userName
            functionCall(callback, it)
        }
    }

    override fun getOpenCabAvailability(
        version: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(UserModule.MODULE_NAME, "getOpenCabAvailability") as? GetOpenCabAvailabilityFunction)?.let {
            it.version = version
            functionCall(callback, it)
        }
    }

    override fun setDriverSeat(
        driverId: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val moduleFunction = findModuleFunction(UserModule.MODULE_NAME, "setDriverSeat") as? SetDriverSeatFunction
        moduleFunction?.let {
            it.driverId = driverId
            functionCall(callback, it)
        }
    }

    override fun getHosRuleSet(
        userName: String,
        callback: (Result<Success<String>, Failure>) -> Unit
    ) {
        (findModuleFunction(UserModule.MODULE_NAME, "getHosRuleSet") as? GetHosRuleSetFunction)?.let {
            it.userName = userName
            functionCall(callback, it)
        }
    }

    override fun getStateDevice(callback: (Result<Success<String>, Failure>) -> Unit) {
        (findModuleFunction(StateModule.MODULE_NAME, "device") as? DeviceFunction)?.let {
            functionCall(callback, it)
        }
    }

    override fun setSpeechEngine(speechEngine: SpeechEngine) {
        val speechModule = modules.firstOrNull { m -> m.name == SpeechModule.MODULE_NAME } as? SpeechModule
        speechModule?.speechEngine = speechEngine
    }

    override fun setDriverActionNecessaryCallback(callback: DriverActionNecessaryCallbackType) {
        userModule.driverActionNecessaryCallback = callback
    }

    override fun setDriveReadyListener(callback: () -> Unit) {
        appModule.driveReadyCallback = callback
    }

    override fun clearDriverActionNecessaryCallback() {
        userModule.driverActionNecessaryCallback = {}
    }

    override fun setPageNavigationCallback(callback: PageNavigationCallbackType) {
        userModule.pageNavigationCallback = callback
    }

    override fun clearPageNavigationCallback() {
        userModule.pageNavigationCallback = {}
    }

    override fun setLoginRequiredCallback(callback: LoginRequiredCallbackType) {
        userModule.loginRequiredCallback = callback
    }

    override fun clearLoginRequiredCallback() {
        userModule.loginRequiredCallback = {}
    }

    /**
     * Set a callback to listen for "last server address" change event.
     * LastServerUpdatedCallbackType is a function with the new last server address as the argument.
     */
    override fun setLastServerAddressUpdatedCallback(callback: LastServerUpdatedCallbackType) {
        appModule.lastServerUpdatedCallback = callback
    }

    /**
     * Set a callback to be invoked when the WebView navigates to a different domain.
     * This is useful for detecting server changes that occur in the WebView login page.
     * @param callback Function that receives the new domain (hostname) as a String parameter
     */
    fun setOnDomainChangeCallback(callback: ((String) -> Unit)?) {
        contentController.setOnDomainChangeCallback(callback)
    }

    /**
     * Clears the previously set LastServerAddressUpdated Callback
     */
    override fun clearLastServerAddressUpdatedCallback() {
        appModule.lastServerUpdatedCallback = {}
    }

    override fun setCustomURLPath(path: String) {
        path.trim().ifEmpty {
            return
        }

        if (isWebViewConfigured) {
            this.webView?.evaluateJavascript(
                """
                    window.location.hash="$path";
                """.trimIndent(),
                null
            )
            this.customUrl = null
        } else {
            this.customUrl = "https://${DriveSdkConfig.serverAddress}/drive/default.html#$path"
        }
    }

    override fun getDeviceEvents(callback: (Result<Success<String>, Failure>) -> Unit) {
        ioxbleModule.deviceEventCallback = callback
        ioxUsbModule.deviceEventCallback = callback
    }

    private fun setUrlToWebView(urlString: String) {
        this.webView?.loadUrl(urlString)
        this.customUrl = null
    }

    override fun setSession(credentialResult: CredentialResult, isCoDriver: Boolean) {
        geotabCredentials = credentialResult
        if (!isWebViewConfigured) {
            logger.error(TAG, "webView not configured")
            return
        }

        val geotabDriveUrl = "https://${DriveSdkConfig.serverAddress}/drive/default.html"

        if (isCoDriver) {
            this.webView?.loadUrl("$geotabDriveUrl#ui/login,(addCoDriver:!t,server:'${credentialResult.path}',credentials:(database:'${credentialResult.credentials.database}',sessionId:'${credentialResult.credentials.sessionId}',userName:'${credentialResult.credentials.userName}'))")
        } else {
            this.webView?.loadUrl("$geotabDriveUrl#ui/login,(server:'${credentialResult.path}',credentials:(database:'${credentialResult.credentials.database}',sessionId:'${credentialResult.credentials.sessionId}',userName:'${credentialResult.credentials.userName}'))")
        }
    }

    override fun cancelLogin() {
        webView?.let { webView ->
            val list = webView.copyBackForwardList()
            list.currentItem?.url?.takeIf { url ->
                val currentHash = url.substring(url.indexOf('#') + 1)
                currentHash.contains("login", ignoreCase = true)
            }?.let {
                for (i in list.size - 1 downTo 0) {
                    val url = list.getItemAtIndex(i).url
                    val indx = url.indexOf('#')
                    if (indx != -1 && !url.substring(indx + 1)
                        .contains("login", ignoreCase = true)
                    ) {
                        webView.goBackOrForward(i - (list.size - 1))
                        return
                    }
                }
            } ?: return
        }
    }

    /**
     * Helper function to call functions that implement [BaseCallbackFunction]
     */
    private fun functionCall(
        callback: (Result<Success<String>, Failure>) -> Unit,
        moduleFunction: BaseCallbackFunction
    ) {
        context?.let { context ->
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    moduleFunction.callJavascript(
                        context = context,
                        evaluate = evaluate,
                        callback = callback,
                        coroutineScope = this
                    )
                }
            }
        } ?: run {
            logger.error(
                TAG,
                "function ${moduleFunction.name} call failed - no context",
                Error(GeotabDriveError.NO_CONTEXT)
            )
            callback(Failure(Error(GeotabDriveError.NO_CONTEXT)))
        }
    }

    private fun moveAppToBackground() {
        requireActivity().moveTaskToBack(true)
    }
}

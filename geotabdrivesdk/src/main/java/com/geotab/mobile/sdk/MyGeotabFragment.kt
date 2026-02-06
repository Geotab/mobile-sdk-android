package com.geotab.mobile.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.lifecycle.lifecycleScope
import com.geotab.mobile.sdk.databinding.FragmentGeotabDriveSdkBinding
import com.geotab.mobile.sdk.fileChooser.FileChooserHelper
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.database.AppDatabase
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.NetworkErrorDelegate
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.auth.AuthModule
import com.geotab.mobile.sdk.module.auth.AuthUtil
import com.geotab.mobile.sdk.module.browser.BrowserModule
import com.geotab.mobile.sdk.module.device.DeviceModule
import com.geotab.mobile.sdk.module.login.LoginModule
import com.geotab.mobile.sdk.module.sso.SSOModule
import com.geotab.mobile.sdk.module.webview.WebViewModule
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionAttribute
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import com.geotab.mobile.sdk.permission.PermissionResultContract
import com.geotab.mobile.sdk.publicInterfaces.MyGeotabSdk
import com.geotab.mobile.sdk.util.PushScriptUtil
import com.geotab.mobile.sdk.util.UserAgentUtil
import com.geotab.mobile.sdk.util.serializable
import com.github.mustachejava.DefaultMustacheFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// fragment initialization parameters
private const val ARG_MODULES = "modules"
private const val MODULE_PREF = "MODULE_PREF"

class MyGeotabFragment :
    BaseGeotabFragment(),
    ModuleContainerDelegate,
    NetworkErrorDelegate,
    PermissionDelegate,
    MyGeotabSdk {
    private var _binding: FragmentGeotabDriveSdkBinding? = null
    private val alphaVersionString = "alpha"

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val myGeotabUrl = "https://${MyGeotabConfig.serverAddress}"
    private val mustacheFactory by lazy { DefaultMustacheFactory() }
    private lateinit var preference: SharedPreferences
    private var appPreferences: SharedPreferences? = null

    private val pushScriptUtil: PushScriptUtil by lazy {
        PushScriptUtil()
    }

    val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit =
        { moduleEvent, callBack ->
            val validEvent = pushScriptUtil.validEvent(moduleEvent, callBack)

            if (validEvent) {
                val script = """
            window.dispatchEvent(new CustomEvent("${moduleEvent.event}", ${moduleEvent.params}));
        """
                this.webView.post {
                    executeIfValid {
                        this.webView.evaluateJavascript(script, null)
                    }
                }

                callBack(Success(""))
            }
        }

    private val goBack = {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            moveAppToBackground()
        }
    }

    private val userAgentUtil: UserAgentUtil by lazy {
        UserAgentUtil(requireContext())
    }

    private val ssoModule: SSOModule by lazy {
        SSOModule(this.parentFragmentManager, appPreferences)
    }

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance()
    }

    private lateinit var webView: WebView
    private lateinit var errorView: View
    private var bigQueryLogListener: com.geotab.mobile.sdk.logging.BigQueryLogListener? = null

    private val deviceModule: DeviceModule by lazy {
        DeviceModule(requireContext(), preference, userAgentUtil)
    }

    private val webViewModule: WebViewModule? by lazy {
        activity?.let { WebViewModule(it, goBack) }
    }

    private val authUtil: AuthUtil by lazy {
        AuthUtil.init(secureStorageRepository)
    }
    private var loginModule: LoginModule? = null
    private var authModule: AuthModule? = null

    private val secureStorageRepository: SecureStorageRepository by lazy {
        SecureStorageRepository(
            requireContext().packageName,
            AppDatabase.getDatabase(requireContext()).secureStorageDao()
        )
    }

    /**
     * Provides access to the WebView for lifecycle-safe execution checks.
     * Implementation of abstract method from BaseGeotabFragment.
     */
    override fun getWebView(): WebView? = if (::webView.isInitialized) webView else null

    private val modulesInternal: ArrayList<Module?> by lazy {
        arrayListOf(
            deviceModule,
            context?.let { BrowserModule(this.parentFragmentManager, it) },
            webViewModule,
            ssoModule
        )
    }

    private val contentController = WebViewClientUserContentController(this)
    private var modules: ArrayList<Module> = arrayListOf()

    @Keep
    companion object {
        private const val TAG = "MyGeotabFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param modules list of modules to send to fragment
         * @return A new instance of fragment GeotabDriveFragment.
         */
        @Keep
        @JvmStatic
        fun newInstance(
            modules: ArrayList<Module> = arrayListOf(),
            appPreferences: SharedPreferences? = null
        ) =
            MyGeotabFragment().apply {
                this.appPreferences = appPreferences
                arguments = Bundle().apply {
                    putSerializable(ARG_MODULES, modules)
                }
            }
    }

    private val moduleScripts: String by lazy {
        var scripts = """
            window.${Module.geotabModules} = {};        
            window.${Module.geotabNativeCallbacks} = {};
            
        """
        context?.let {
            for (module in modules) {
                scripts += module.scripts(it)
            }

            scripts += deviceModule.getScriptFromTemplate(
                it,
                "Print.Script.js",
                hashMapOf("interfaceName" to "AndroidFunctionProvider")
            )

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

    override var webAppLoadFailed: (() -> Unit)? = null

    private val startForPermissionResult = registerForActivityResult(PermissionResultContract()) {
        it.callback(it.result)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Module.mustacheFactory = mustacheFactory
        arguments?.let { bundle ->
            initializeModules(
                bundle.serializable<ArrayList<*>>(ARG_MODULES)?.filterIsInstance<Module>()
            )
        }
        activity?.onBackPressedDispatcher?.addCallback(onBackPressedCallback)
        contentController.setWebViewCallBack(webViewModule?.onBackPressedCallback)
        contentController.setAppCallBack(onBackPressedCallback)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preference = context.getSharedPreferences(MODULE_PREF, Context.MODE_PRIVATE)
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
            if (webView.url != null) {
                webView.reload()
            }
            webView.visibility = View.VISIBLE
            errorView.visibility = View.GONE
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

        configureWebView()
        configureWebViewScript(contentController)
        // Add BigQuery listener to the broadcaster
        bigQueryLogListener = com.geotab.mobile.sdk.logging.BigQueryLogListener(push)
        bigQueryLogListener?.let { listener ->
            (Logger.shared as? com.geotab.mobile.sdk.logging.LogBroadcaster)?.addListener(listener)
        }
    }

    override fun findModule(module: String): Module? {
        return modules.firstOrNull { it.name == module }
    }

    override fun findModuleFunction(module: String, function: String): ModuleFunction? {
        val resModule = modules.firstOrNull { it.name == module }
        return resModule?.findFunction(function)
    }

    @JavascriptInterface
    fun postMessage(name: String, function: String, result: String, callback: String) = executeIfValid {
        val jsonObject = JSONObject(result)
        val params: String? =
            if (jsonObject.isNull("result")) null else jsonObject.getString("result")
        val moduleFunction = findModuleFunction(name, function)
        moduleFunction?.let { callModuleFunction(it, callback, params) }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun createWebPrintJob() {
        val printManager = activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager

        webView.let { webViewObject ->
            webViewObject.post {
                printManager?.let { printManagerObject ->

                    val jobName = "${getString(R.string.app_name)} Document"

                    val printAdapter = webViewObject.createPrintDocumentAdapter(jobName)

                    printManagerObject.print(
                        jobName,
                        printAdapter,
                        PrintAttributes.Builder().build()
                    )
                }
            }
        }
    }

    private fun callModuleFunction(
        moduleFunction: ModuleFunction,
        callback: String,
        params: String?
    ) {
        moduleFunction.handleJavascriptCall(params) { result ->
            when (result) {
                is Success -> {
                    this.webView.post {
                        executeIfValid {
                            this.webView.evaluateJavascript(
                                """
                                try {
                                    var t = $callback(null, ${result.value});
                                    if (t instanceof Promise) {
                                        t.catch(err => { console.log(">>>>> Unexpected exception: ", err); });
                                    }
                                } catch(err) {
                                    console.log(">>>>> Unexpected exception: ", err);
                                }
                                """.trimIndent(),
                                null
                            )
                        }
                    }
                }

                is Failure -> {
                    this.webView.post {
                        executeIfValid {
                            val jsScript = buildErrorJavaScript(callback, result.reason)
                            this.webView.evaluateJavascript(jsScript, null)
                        }
                    }
                }
            }
        }
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
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        if (BuildConfig.DEBUG || getString(R.string.app_flavor) == alphaVersionString) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        with(this.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            userAgentString = if (getString(R.string.app_flavor) != alphaVersionString) {
                userAgentUtil.getUserAgent(webView.settings.userAgentString)
            } else {
                Log.i(
                    TAG,
                    "UserAgent: ${userAgentUtil.getUserAgent(webView.settings.userAgentString)}"
                )
                userAgentUtil.getUserAgent(webView.settings.userAgentString).replace("; wv", "")
            }
        }

        cookieManager.setAcceptThirdPartyCookies(
            this.webView,
            MyGeotabConfig.allowThirdPartyCookies
        )
    }

    private fun configureWebViewScript(webViewClientUserContentController: WebViewClientUserContentController) {
        this.webView.loadUrl(myGeotabUrl)
        webViewClientUserContentController.addScriptOnPageFinished(moduleScripts)

        context?.let {
            val downloadFiles = DownloadFiles(evaluate, it, this)
            this.webView.setDownloadListener(downloadFiles)
            this.webView.addJavascriptInterface(downloadFiles, DownloadFiles.interfaceName)
        }

        this.webView.addJavascriptInterface(this, Module.interfaceName)
        this.webView.webViewClient = webViewClientUserContentController
        this.webView.webChromeClient = context?.let {
            WebViewChromeClient(
                PermissionHelper(it, this),
                FileChooserHelper(this)
            )
        }
    }

    private val evaluate: (String, (String) -> Unit) -> Unit =
        { script: String, callback: (String) -> Unit ->
            this.webView.post {
                executeIfValid {
                    this.webView.evaluateJavascript(script) { result ->
                        executeIfValid {
                            callback(result)
                        }
                    }
                }
            }
        }

    override fun onNetworkError() {
        errorView.let {
            webView.visibility = View.GONE
            it.visibility = View.VISIBLE
        }
        webAppLoadFailed?.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove BigQuery listener to prevent memory leaks
        bigQueryLogListener?.let { listener ->
            (Logger.shared as? com.geotab.mobile.sdk.logging.LogBroadcaster)?.removeListener(listener)
        }
        bigQueryLogListener = null

        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
            webView.clearHistory()
            webView.clearCache(true)
            webView.removeAllViews()
        }
        webView.destroy()
        authUtil.dispose(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Set a callback to be invoked when the WebView navigates to a different domain.
     * This is useful for detecting server changes that occur in the WebView login page.
     * @param callback Function that receives the new domain (hostname) as a String parameter
     */
    fun setOnDomainChangeCallback(callback: ((String) -> Unit)?) {
        contentController.setOnDomainChangeCallback(callback)
    }

    private fun moveAppToBackground() {
        requireActivity().moveTaskToBack(true)
    }
}

package com.geotab.mobile.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import com.geotab.mobile.sdk.databinding.FragmentGeotabDriveSdkBinding
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.NetworkErrorDelegate
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.app.AppModule
import com.geotab.mobile.sdk.module.app.LastServerUpdatedCallbackType
import com.geotab.mobile.sdk.module.browser.BrowserModule
import com.geotab.mobile.sdk.module.device.DeviceModule
import com.geotab.mobile.sdk.module.webview.WebViewModule
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionAttribute
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import com.geotab.mobile.sdk.permission.PermissionResultContract
import com.geotab.mobile.sdk.publicInterfaces.MyGeotabSdk
import com.geotab.mobile.sdk.util.UserAgentUtil
import com.github.mustachejava.DefaultMustacheFactory
import org.json.JSONObject

// fragment initialization parameters
private const val ARG_MODULES = "modules"
private const val MODULE_PREF = "MODULE_PREF"

class MyGeotabFragment :
    Fragment(),
    ModuleContainerDelegate,
    NetworkErrorDelegate,
    PermissionDelegate,
    MyGeotabSdk {
    private var _binding: FragmentGeotabDriveSdkBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val myGeotabUrl = "https://${MyGeotabConfig.serverAddress}"
    private val mustacheFactory by lazy { DefaultMustacheFactory() }
    private lateinit var preference: SharedPreferences
    private var customUrl: String? = null
    private var isWebViewConfigured: Boolean = false

    private val goBack = {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    private val userAgentUtil: UserAgentUtil by lazy {
        UserAgentUtil(requireContext())
    }

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance()
    }

    private lateinit var webView: WebView
    private lateinit var errorView: View

    private val deviceModule: DeviceModule by lazy {
        DeviceModule(requireContext(), preference, userAgentUtil)
    }

    private val appModule: AppModule by lazy {
        AppModule(evaluate = { _, _ -> }, push = {})
    }

    private val modulesInternal: ArrayList<Module?> by lazy {
        arrayListOf(
            deviceModule,
            appModule,
            context?.let { BrowserModule(this.parentFragmentManager, it) },
            activity?.let { WebViewModule(it, goBack) }
        )
    }

    private val contentController = WebViewClientUserContentController(this)
    private var modules: ArrayList<Module> = arrayListOf()

    @Keep
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param modules list of modules to send to fragment
         * @return A new instance of fragment GeotabDriveFragment.
         */
        @Keep
        @JvmStatic
        fun newInstance(modules: ArrayList<Module> = arrayListOf()) =
            MyGeotabFragment().apply {
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
                hashMapOf()
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
            initializeModules((bundle.getSerializable(ARG_MODULES) as? ArrayList<*>)?.filterIsInstance<Module>())
        }
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

        with(appModule) {
            initValues(this@MyGeotabFragment.requireContext())
        }

        configureWebView()
        configureWebViewScript(contentController)
    }

    override fun findModule(module: String): Module? {
        return modules.firstOrNull { it.name == module }
    }

    override fun findModuleFunction(module: String, function: String): ModuleFunction? {
        val resModule = modules.firstOrNull { it.name == module }
        return resModule?.findFunction(function)
    }

    override fun setCustomURLPath(path: String) {
        val urlString = "https://${MyGeotabConfig.serverAddress}#$path"
        this.customUrl = urlString
        if (isWebViewConfigured) {
            setUrlToWebView(urlString)
        }
    }

    @JavascriptInterface
    fun postMessage(name: String, function: String, result: String, callback: String) {
        val jsonObject = JSONObject(result)
        val params: String? =
            if (jsonObject.isNull("result")) null else jsonObject.getString("result")
        val moduleFunction = findModuleFunction(name, function)
        moduleFunction?.let { callModuleFunction(it, callback, params) }
    }

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
                            """.trimIndent()
                        ) {}
                    }
                }
                is Failure -> {
                    this.webView.post {
                        this.webView.evaluateJavascript(
                            """
                            try {
                                var t = $callback(new Error(`${result.reason.getErrorCode()}: ${result.reason.getErrorMessage()}`));
                                if (t instanceof Promise) {
                                    t.catch(err => { console.log(">>>>> Unexpected exception: ", err); });
                                }
                            } catch(err) {
                                console.log(">>>>> Unexpected exception: ", err);
                            }
                            """.trimIndent()
                        ) {}
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
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        with(this.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setAppCacheEnabled(true)
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            setAppCachePath(webView.context.cacheDir.path)
            setSupportMultipleWindows(true)
            userAgentString = userAgentUtil.getUserAgent(webView.settings.userAgentString)
        }

        cookieManager.setAcceptThirdPartyCookies(this.webView, MyGeotabConfig.allowThirdPartyCookies)
    }

    private fun configureWebViewScript(webViewClientUserContentController: WebViewClientUserContentController) {
        customUrl?.let { url ->
            setUrlToWebView(url)
        } ?: run {
            this.webView.loadUrl(myGeotabUrl)
        }

        webViewClientUserContentController.addScriptOnPageFinished(moduleScripts)
        this.webView.addJavascriptInterface(this, Module.interfaceName)
        this.webView.webViewClient = webViewClientUserContentController
        val downloadFiles = context?.let { DownloadFiles(it, this) }
        this.webView.setDownloadListener(downloadFiles)
        isWebViewConfigured = true
        this.webView.webChromeClient = context?.let {
            WebViewChromeClient(
                PermissionHelper(it, this)
            )
        }
    }

    private fun setUrlToWebView(url: String) {
        this.webView.loadUrl(url)
        this.customUrl = null
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
        if (webView.parent != null) {
            (webView.parent as ViewGroup).removeView(webView)
            webView.clearHistory()
            webView.clearCache(true)
            webView.removeAllViews()
        }
        webView.destroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Set a callback to listen for "last server address" change event.
     * LastServerUpdatedCallbackType is a function with the new last server address as the argument.
     */
    override fun setLastServerAddressUpdatedCallback(callback: LastServerUpdatedCallbackType) {
        appModule.lastServerUpdatedCallback = callback
    }

    /**
     * Clears the previously set LastServerAddressUpdated Callback
     */
    override fun clearLastServerAddressUpdatedCallback() {
        appModule.lastServerUpdatedCallback = {}
    }
}

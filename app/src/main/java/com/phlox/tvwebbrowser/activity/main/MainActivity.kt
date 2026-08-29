package com.phlox.tvwebbrowser.activity.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.IncognitoModeMainActivity
import com.phlox.tvwebbrowser.activity.downloads.DownloadsActivity
import com.phlox.tvwebbrowser.activity.history.HistoryActivity
import com.phlox.tvwebbrowser.activity.main.dialogs.favorites.FavoriteEditorDialog
import com.phlox.tvwebbrowser.activity.main.dialogs.favorites.FavoritesDialog
import com.phlox.tvwebbrowser.activity.main.dialogs.settings.SettingsDialog
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.model.HomePageLink
import com.phlox.tvwebbrowser.model.HostConfig
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.service.downloads.DownloadService
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import com.phlox.tvwebbrowser.ui.components.TabUi
import com.phlox.tvwebbrowser.ui.screens.MainScreen
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.utils.BackNavigationEventsAdapter
import com.phlox.tvwebbrowser.utils.DownloadUtils
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.VoiceSearchHelper
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import com.phlox.tvwebbrowser.utils.sameDay
import com.phlox.tvwebbrowser.webengine.WebEngine
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import com.phlox.tvwebbrowser.webengine.WebEngineWindowProviderCallback
import com.phlox.tvwebbrowser.widgets.cursor.CursorDrawerDelegate
import com.phlox.tvwebbrowser.widgets.cursor.CursorLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import kotlin.system.exitProcess


open class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val VOICE_SEARCH_REQUEST_CODE = 10001
        const val MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS = 10003
        const val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS = 10004
        const val PICK_FILE_REQUEST_CODE = 10005
        private const val REQUEST_CODE_HISTORY_ACTIVITY = 10006
        const val REQUEST_CODE_UNKNOWN_APP_SOURCES = 10007
        const val KEY_PROCESS_ID_TO_KILL = "proc_id_to_kill"
        private const val MY_PERMISSIONS_REQUEST_VOICE_SEARCH_PERMISSIONS = 10008
        private const val COMMON_REQUESTS_START_CODE = 10100
    }

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var tabsModel: TabsModel
    private lateinit var settingsModel: SettingsModel
    private lateinit var adblockModel: AdblockModel
    private lateinit var autoUpdateModel: AutoUpdateModel
    private lateinit var uiHandler: Handler
    private var isFullscreen: Boolean = false
    private lateinit var prefs: SharedPreferences
    protected val config = AppContext.provideConfig()
    private val voiceSearchHelper = VoiceSearchHelper(this, VOICE_SEARCH_REQUEST_CODE,
        MY_PERMISSIONS_REQUEST_VOICE_SEARCH_PERMISSIONS)
    private var lastCommonRequestsCode = COMMON_REQUESTS_START_CODE
    private var downloadService: DownloadService? = null
    private var downloadIntent: Download? = null
    var openUrlInExternalAppDialog: AlertDialog? = null
    private var linkActionsMenu: PopupMenu? = null

    // Compose state — replaces ActivityMainBinding vb
    private var addressText by mutableStateOf("")
    private var addressTextColor by mutableStateOf(Color.BLACK)
    private var webProgress by mutableStateOf(0)
    private var isProgressVisible by mutableStateOf(false)
    private var isGenericLoading by mutableStateOf(false)
    private var isMenuVisible by mutableStateOf(false)
    private var isCursorMenuVisible by mutableStateOf(false)
    private var canGoBack by mutableStateOf(false)
    private var canGoForward by mutableStateOf(false)
    private var shieldsOn by mutableStateOf(false)
    private var blockedAds by mutableStateOf(0)
    private var blockedPopups by mutableStateOf(0)
    private var thumbnail by mutableStateOf<Bitmap?>(null)
    private var tabsUi by mutableStateOf<List<TabUi>>(emptyList())
    private var currentTabIndex by mutableStateOf(0)
    private var cursorLayout: CursorLayout? = null
    private var pendingLoadState = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incognitoMode = config.incognitoMode
        Log.d(TAG, "onCreate incognitoMode: $incognitoMode")
        if (incognitoMode xor (this is IncognitoModeMainActivity)) {
            switchProcess(incognitoMode, intent?.extras)
            finish()
            return
        }
        val pidToKill = intent?.getIntExtra(KEY_PROCESS_ID_TO_KILL, -1) ?: -1
        if (pidToKill != -1) {
            Process.killProcess(pidToKill)
        }

        viewModel = ActiveModelsRepository.get(MainActivityViewModel::class, this)
        if (incognitoMode) {
            viewModel.prepareSwitchToIncognito()
        }
        settingsModel = ActiveModelsRepository.get(SettingsModel::class, this)
        adblockModel = ActiveModelsRepository.get(AdblockModel::class, this)
        tabsModel = ActiveModelsRepository.get(TabsModel::class, this)
        autoUpdateModel = ActiveModelsRepository.get(AutoUpdateModel::class, this)
        uiHandler = Handler()
        prefs = getSharedPreferences(TVBro.MAIN_PREFS_NAME, Context.MODE_PRIVATE)

        // Hybrid root: CursorLayout (WebView container) as traditional view below Compose overlay — fixes white-screen crash (was AndroidView factory race)
        cursorLayout = CursorLayout(this)
        val root = android.widget.FrameLayout(this)
        root.addView(cursorLayout, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                XeraTheme {
                    MainScreen(
                        url = addressText,
                        tabs = tabsUi,
                        isMenuVisible = isMenuVisible,
                        thumbnail = thumbnail,
                        isProgressVisible = isProgressVisible,
                        progress = webProgress,
                        isGenericLoading = isGenericLoading,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        isShieldsOn = shieldsOn,
                        blockedCount = blockedAds,
                        blockedPopups = blockedPopups,
                        isCursorMenuVisible = isCursorMenuVisible,
                        hasExternalContainer = true,
                        onUrlChanged = { addressText = it },
                        onSearch = { search(addressText) },
                        onMenu = { closeWindow() },
                        onVoice = { initiateVoiceSearch() },
                        onHistory = { showHistory() },
                        onFavorites = { showFavorites() },
                        onDownloads = { showDownloads() },
                        onIncognito = { toggleIncognitoMode() },
                        onSettings = { showSettings() },
                        onTabSelected = { ui -> tabByTitleIndex(tabsUi.indexOf(ui))?.let { changeTab(it) } },
                        onTabClose = { ui -> tabByTitleIndex(tabsUi.indexOf(ui))?.let { closeTab(it) } },
                        onAddTab = { openInNewTab(settingsModel.homePage, tabsModel.tabsStates.size) },
                        onBack = { navigateBack() },
                        onForward = {
                            val tab = tabsModel.currentTab.value ?: return@MainScreen
                            if (tab.webEngine.canGoForward()) tab.webEngine.goForward()
                        },
                        onRefresh = { refresh() },
                        onCloseTab = { tabsModel.currentTab.value?.let { closeTab(it) } },
                        onHome = { navigate(settingsModel.homePage) },
                    onAdBlock = { showAdBlockOverlay() },
                    onPopupBlock = { lifecycleScope.launch(Dispatchers.Main) { showPopupBlockOptions() } },
                    onGrab = { tabsModel.currentTab.value?.webEngine?.setVirtualCursorMode(false); isCursorMenuVisible = false },
                    onZoomIn = { tabsModel.currentTab.value?.webEngine?.zoomIn(); isCursorMenuVisible = false },
                    onZoomOut = { tabsModel.currentTab.value?.webEngine?.zoomOut(); isCursorMenuVisible = false },
                    onContextMenu = { isCursorMenuVisible = false; lifecycleScope.launch(Dispatchers.Main) { showPopupBlockOptions() } },
                    onDpad = { tabsModel.currentTab.value?.webEngine?.setVirtualCursorMode(true); isCursorMenuVisible = false }
                    )
                }
            }
        }
        root.addView(composeView, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)

        config.userAgentString.subscribe(this.lifecycle, false) {
            for (tab in tabsModel.tabsStates) {
                tab.webEngine.userAgentString = it
            }
        }

        config.theme.subscribe(this.lifecycle, false) {
            when (it) {
                Config.Theme.BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Config.Theme.WHITE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            WebEngineFactory.onThemeSettingUpdated(it)
        }

        settingsModel.keepScreenOn.subscribe(this.lifecycle) {
            if (it) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        viewModel.homePageLinks.subscribe(this) {
            Log.i(TAG, "homePageLinks updated")
            val currentUrl = tabsModel.currentTab.value?.url ?: return@subscribe
            if (Config.HOME_PAGE_URL == currentUrl) {
                tabsModel.currentTab.value?.webEngine?.reload()
            }
        }

        tabsModel.currentTab.subscribe(this) {
            addressText = it?.url ?: ""
            it?.let {
                onWebViewUpdated(it)
            }
            refreshTabsUi()
        }

        tabsModel.tabsStates.subscribe(this, false) {
            refreshTabsUi()
            if (it.isEmpty()) {
                if (!config.isWebEngineGecko()) {
                    cursorLayout?.removeAllViews()
                }
            }
        }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        // Hybrid: cursorLayout already attached to root, safe to init WebEngine now
        loadState()
    }

    private fun refreshTabsUi() {
        tabsUi = tabsModel.tabsStates.mapIndexed { idx, s ->
            TabUi(id = idx.toLong(), title = s.title ?: "New Tab", selected = s.selected)
        }
        // sync current index for thumbnail check
        currentTabIndex = tabsModel.tabsStates.indexOfFirst { it.selected }.coerceAtLeast(0)
    }

    private var progressBarHideRunnable: Runnable = Runnable {
        isProgressVisible = false
    }

    private val mConnectivityChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
            val tab = tabsModel.currentTab.value ?: return
            tab.webEngine.setNetworkAvailable(isConnected)
        }
    }

    private val displayThumbnailRunnable = object : Runnable {
        var tabState: WebTabState? = null
        override fun run() {
            tabState?.let {
                lifecycleScope.launch(Dispatchers.Main) {
                    displayThumbnail(it)
                }
            }
        }
    }

    fun closeWindow() {
        Log.d(TAG, "closeWindow")
        lifecycleScope.launch {
            if (config.incognitoMode) {
                toggleIncognitoMode(false).join()
            }
            finish()
        }
    }

    fun showDownloads() {
        startActivity(Intent(this@MainActivity, DownloadsActivity::class.java))
    }

    fun showHistory() {
        startActivityForResult(
                Intent(this@MainActivity, HistoryActivity::class.java),
                REQUEST_CODE_HISTORY_ACTIVITY)
        hideMenuOverlay()
    }

    fun showFavorites() {
        val currentTab = tabsModel.currentTab.value
        val currentPageTitle = currentTab?.title ?: ""
        val currentPageUrl = currentTab?.url ?: ""

        FavoritesDialog(this@MainActivity, lifecycleScope, object : FavoritesDialog.Callback {
            override fun onFavoriteChoosen(item: FavoriteItem?) {
                navigate(item!!.url!!)
            }
        }, currentPageTitle, currentPageUrl).show()
        hideMenuOverlay()
    }

    private fun tabByTitleIndex(index: Int) =
            if (index >= 0 && index < tabsModel.tabsStates.size) tabsModel.tabsStates[index] else null

    fun showSettings() {
        SettingsDialog(this, settingsModel).show()
    }

    fun onExtendedAddressBarMode() {
        isMenuVisible = false
    }

    fun onUrlInputDone() {
        hideMenuOverlay()
    }

    fun navigateBack(goHomeIfNoHistory: Boolean = false) {
        val currentTab = tabsModel.currentTab.value
        if (currentTab != null && currentTab.webEngine.canGoBack()) {
            currentTab.webEngine.goBack()
        } else if (goHomeIfNoHistory) {
            navigate(settingsModel.homePage)
        } else if (!isMenuVisible) {
            showMenuOverlay()
        } else {
            hideMenuOverlay()
        }
    }

    fun refresh() {
        tabsModel.currentTab.value?.webEngine?.reload()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (::tabsModel.isInitialized) {
            tabsModel.onDetachActivity()
        }
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) {
            handleIntent(intent)
        }
    }

    private fun loadState() = lifecycleScope.launch(Dispatchers.Main) {
        Log.d(TAG, "loadState cursorLayout=${cursorLayout != null} pending=$pendingLoadState")
        val container = cursorLayout
        if (container == null) {
            Log.w(TAG, "loadState called but cursorLayout is null, deferring")
            pendingLoadState = true
            return@launch
        }
        try {
            WebEngineFactory.initialize(this@MainActivity, container)
        } catch (e: Exception) {
            Log.e(TAG, "WebEngineFactory.initialize failed", e)
            Toast.makeText(this@MainActivity, "Engine init failed: ${e.message}", Toast.LENGTH_LONG).show()
            isGenericLoading = false
            return@launch
        }

        isGenericLoading = true
        try {
            viewModel.loadState().join()
            tabsModel.loadState().join()
        } catch (e: Exception) {
            Log.e(TAG, "loadState viewModel/tabsModel failed", e)
            Toast.makeText(this@MainActivity, "Load failed: ${e.message}", Toast.LENGTH_LONG).show()
            isGenericLoading = false
            return@launch
        }

        if (!isActive) {
            isGenericLoading = false
            return@launch
        }

        isGenericLoading = false

        try {
            if (intent.data == null) {
            if (tabsModel.tabsStates.isEmpty()) {
                openInNewTab(settingsModel.homePage, 0,
                    needToHideMenuOverlay = true,
                    navigateImmediately = true
                )
            } else {
                var foundSelectedTab = false
                for (i in tabsModel.tabsStates.indices) {
                    val tab = tabsModel.tabsStates[i]
                    if (tab.selected) {
                        changeTab(tab)
                        foundSelectedTab = true
                        break
                    }
                }
                if (!foundSelectedTab) {
                    changeTab(tabsModel.tabsStates[0])
                }
            }
        } else {
            handleIntent(intent)
        }

        val currentTab = tabsModel.currentTab.value
        if (currentTab == null || currentTab.url == settingsModel.homePage) {
            showMenuOverlay()
        }
        if (autoUpdateModel.needAutoCheckUpdates &&
            autoUpdateModel.updateChecker.versionCheckResult == null &&
                !autoUpdateModel.lastUpdateNotificationTime.sameDay(Calendar.getInstance())) {
            autoUpdateModel.checkUpdate(false){
                if (autoUpdateModel.updateChecker.hasUpdate()) {
                    autoUpdateModel.showUpdateDialogIfNeeded(this@MainActivity)
                }
            }
        }
        } catch (e: Exception) {
            Log.e(TAG, "loadState tab handling failed", e)
            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            isGenericLoading = false
        }
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent: " + intent.data)
        if (intent.getBooleanExtra("com.phlox.tvwebbrowser.EXTRA_OPEN_IN_SAME_TAB", false) &&
            tabsModel.tabsStates.isNotEmpty()) {
            if (tabsModel.currentTab.value == null) {
                changeTab(tabsModel.tabsStates[0])
            }
            navigate(intent.data.toString())
            return
        }

        openInNewTab(
            intent.data.toString(), tabsModel.tabsStates.size, needToHideMenuOverlay = true,
            navigateImmediately = true
        )
    }

    private fun openInNewTab(url: String?, index: Int = 0, needToHideMenuOverlay: Boolean = true, navigateImmediately: Boolean = true): WebEngine? {
        Log.d(TAG, "openInNewTab: url: $url, index: $index, needToHideMenuOverlay: $needToHideMenuOverlay, navigateImmediately: $navigateImmediately")
        if (url == null) {
            return null
        }
        val tab = WebTabState(url = url, incognito = config.incognitoMode)
        createWebView(tab) ?: return null
        tabsModel.tabsStates.add(index, tab)
        changeTab(tab)
        if (navigateImmediately) {
            navigate(url)
        }
        if (needToHideMenuOverlay && isMenuVisible) {
            hideMenuOverlay(true)
        }
        return tab.webEngine
    }

    private fun closeTab(tab: WebTabState?) {
        if (tab == null) return
        val position = tabsModel.tabsStates.indexOf(tab)
        if (tabsModel.currentTab.value == tab) {
            tabsModel.currentTab.value = null
        }
        when {
            tabsModel.tabsStates.size == 1 -> openInNewTab(settingsModel.homePage, 0, needToHideMenuOverlay = true, navigateImmediately = true)

            position > 0 -> changeTab(tabsModel.tabsStates[position - 1])

            else -> changeTab(tabsModel.tabsStates[position + 1])
        }
        tabsModel.onCloseTab(tab)
        hideMenuOverlay(true)
        isCursorMenuVisible = false
    }

    private fun changeTab(newTab: WebTabState) {
        val container = cursorLayout
        if (container == null) {
            Log.w(TAG, "changeTab called but cursorLayout is null, deferring")
            // still update currentTab so UI reflects, but don't attach view yet
            tabsModel.currentTab.value = newTab
            refreshTabsUi()
            return
        }
        tabsModel.changeTab(newTab, { tab: WebTabState -> createWebView(tab) }, container, WebEngineCallback(newTab))
        refreshTabsUi()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: WebTabState): View? {
        val webView: View
        try {
            webView = tab.webEngine.getOrCreateView(this)
        } catch (e: Throwable) {
            e.printStackTrace()

            if (!config.isWebEngineGecko()) {
                val dialogBuilder = AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setCancelable(false)
                    .setMessage(R.string.err_webview_can_not_link)
                    .setNegativeButton(R.string.exit) { _, _ -> finish() }

                val appPackageName = "com.google.android.webview"
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                val activities = packageManager.queryIntentActivities(intent, 0)
                if (activities.size > 0) {
                    dialogBuilder.setPositiveButton(R.string.find_in_apps_store) { _, _ ->
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        finish()
                    }
                }
                dialogBuilder.show()
            }
            return null
        }

        var ua = config.userAgentString.value
        if (ua?.contains("TV Bro/1.0 ") == true) {
            config.userAgentString.value = null
            ua = null
        }
        if (ua != null) {
            tab.webEngine.userAgentString = ua
        }

        return webView
    }

    private fun onWebViewUpdated(tab: WebTabState) {
        canGoBack = tab.webEngine.canGoBack() == true
        canGoForward = tab.webEngine.canGoForward() == true

        val adblockEnabled = tab.adblock ?: config.adBlockEnabled
        shieldsOn = adblockEnabled
        blockedAds = tab.blockedAds
        blockedPopups = tab.blockedPopups
    }

    private fun showAdBlockOverlay() {
        val tab = tabsModel.currentTab.value
        com.phlox.tvwebbrowser.activity.main.dialogs.AdBlockOverlayDialog(
            this,
            tab,
            onToggle = { enabled ->
                tab?.apply {
                    adblock = enabled
                    webEngine.onUpdateAdblockSetting(enabled)
                    onWebViewUpdated(this)
                    refresh()
                } ?: run {
                    config.adBlockEnabled = enabled
                    tabsModel.currentTab.value?.let { onWebViewUpdated(it) }
                }
            },
            onManage = {
                showSettings()
            }
        ).show()
    }

    private fun onDownloadRequested(url: String, referer: String, originalDownloadFileName: String, userAgent: String?, mimeType: String? = null,
                                    operationAfterDownload: Download.OperationAfterDownload = Download.OperationAfterDownload.NOP,
                                    base64BlobData: String? = null, stream: InputStream?, size: Long = 0L) {
        downloadIntent = Download(url, originalDownloadFileName, null, operationAfterDownload,
            mimeType, referer, userAgent, base64BlobData, stream, size)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS
            )
        } else {
            startDownload()
        }
    }

    private fun startDownload() {
        val download = this.downloadIntent ?: return
        this.downloadIntent = null
        downloadService?.startDownload(download)
        onDownloadStarted(download.filename)
    }

    override fun onTrimMemory(level: Int) {
        for (tab in tabsModel.tabsStates) {
            if (!tab.selected) {
                tab.trimMemory()
            }
        }
        super.onTrimMemory(level)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (voiceSearchHelper.processPermissionsResult(requestCode, permissions, grantResults)) {
            return
        }
        if (tabsModel.currentTab.value?.webEngine?.onPermissionsResult(requestCode, permissions, grantResults) == true) return
        if (grantResults.isEmpty()) return
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE_ACCESS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDownload()
                }
            }
            MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS_ACCESS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDownload()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (voiceSearchHelper.processActivityResult(requestCode, resultCode, data)) {
            return
        }
        when (requestCode) {
            PICK_FILE_REQUEST_CODE -> {
                tabsModel.currentTab.value?.webEngine?.onFilePicked(resultCode, data)
            }
            REQUEST_CODE_HISTORY_ACTIVITY -> if (resultCode == Activity.RESULT_OK) {
                val url = data?.getStringExtra(HistoryActivity.KEY_URL)
                if (url != null) {
                    navigate(url)
                }
                hideMenuOverlay()
            }
            REQUEST_CODE_UNKNOWN_APP_SOURCES -> if (autoUpdateModel.needToShowUpdateDlgAgain) {
                autoUpdateModel.showUpdateDialogIfNeeded(this)
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, DownloadService::class.java), downloadServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(downloadServiceConnection)
        downloadService = null
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(mConnectivityChangeReceiver, intentFilter)
        tabsModel.currentTab.value?.webEngine?.onResume()
    }

    override fun onPause() {
        unregisterReceiver(mConnectivityChangeReceiver)
        tabsModel.currentTab.value?.apply {
            webEngine.onPause()
            onPause()
            runBlocking { tabsModel.saveTab(this@apply) }
        }
        super.onPause()
    }

    private fun toggleAdBlockForTab() {
        tabsModel.currentTab.value?.apply {
            val currentState = adblock ?: config.adBlockEnabled
            val newState = !currentState
            adblock = newState
            webEngine.onUpdateAdblockSetting(newState)
            onWebViewUpdated(this)
            refresh()
        }
    }

    private suspend fun showPopupBlockOptions() {
        val tab = tabsModel.currentTab.value ?: return
        val currentHostConfig = tabsModel.findHostConfig(tab,false)
        val currentBlockPopupsLevelValue = currentHostConfig?.popupBlockLevel ?: HostConfig.DEFAULT_BLOCK_POPUPS_VALUE
        val hostName = currentHostConfig?.hostName ?: try { URL(tab.url).host } catch (e: Exception) { "" }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.block_popups_s, hostName))
            .setSingleChoiceItems(R.array.popup_blocking_level, currentBlockPopupsLevelValue) {
                    dialog, itemId -> lifecycleScope.launch {
                        tabsModel.changePopupBlockingLevel(itemId, tab)
                        dialog.dismiss()
                    }
            }
            .show()
    }

    fun navigate(url: String) {
        Log.d(TAG, "navigate: $url")
        addressTextColor = Color.BLACK
        val tab = tabsModel.currentTab.value
        if (tab != null) {
            tab.url = url
            tab.webEngine.loadUrl(url)
        } else {
            openInNewTab(url, 0, needToHideMenuOverlay = true, navigateImmediately = true)
        }
    }

    fun search(aText: String) {
        var text = aText
        val trimmedLowercased = text.trim { it <= ' ' }.lowercase(Locale.ROOT)
        if (Patterns.WEB_URL.matcher(text).matches() || trimmedLowercased.startsWith("http://") || trimmedLowercased.startsWith("https://")) {
            if (!text.lowercase(Locale.ROOT).contains("://")) {
                text = "https://$text"
            }
            navigate(text)
        } else {
            var query: String? = null
            try {
                query = URLEncoder.encode(text, "utf-8")
            } catch (e1: UnsupportedEncodingException) {
                e1.printStackTrace()
                Utils.showToast(this, R.string.error)
                return
            }

            val searchUrl = config.searchEngineURL.value.replace("[query]", query!!)
            navigate(searchUrl)
        }
    }

    fun toggleIncognitoMode() {
        toggleIncognitoMode(true)
    }

    private fun toggleIncognitoMode(andSwitchProcess: Boolean) = lifecycleScope.launch(Dispatchers.Main) {
        Log.d(TAG, "toggleIncognitoMode andSwitchProcess: $andSwitchProcess")
        val becomingIncognitoMode = !config.incognitoMode
        isGenericLoading = true
        if (!becomingIncognitoMode) {
            if (!config.isWebEngineGecko()) {
                withContext(Dispatchers.IO) {
                    WebStorage.getInstance().deleteAllData()
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                }

                WebEngineFactory.clearCache(this@MainActivity)
            }

            tabsModel.onCloseAllTabs().join()
            tabsModel.currentTab.value = null

            if (!config.isWebEngineGecko()) {
                viewModel.clearIncognitoData().join()
            }
        }
        isGenericLoading = false
        config.incognitoMode = becomingIncognitoMode
        if (andSwitchProcess) {
            switchProcess(becomingIncognitoMode)
        }
    }

    private fun switchProcess(incognitoMode: Boolean, intentDataToCopy: Bundle? = null) {
        Log.d(TAG, "switchProcess incognitoMode: $incognitoMode")
        val activityClass = if (incognitoMode) IncognitoModeMainActivity::class.java
        else MainActivity::class.java
        val intent = Intent(this@MainActivity, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(KEY_PROCESS_ID_TO_KILL, Process.myPid())
        intentDataToCopy?.let {
            intent.putExtras(it)
        }
        startActivity(intent)
        exitProcess(0)
    }

    fun toggleMenu() {
        if (!isMenuVisible) {
            showMenuOverlay()
        } else {
            hideMenuOverlay()
        }
    }

    val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backNavigationEventsAdapter.dispatchSystemBackNavigationEvent()
        }
    }

    private val backNavigationEventsAdapter = BackNavigationEventsAdapter(
        onEmulatedBackEvent = {
            if (!hideSoftwareKeyboardIfVisible()) {
                handleBackNavigation()
            }
        }
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val localCallback = window.callback
        window.callback = object : Window.Callback by localCallback {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                backNavigationEventsAdapter.dispatchKeyEvent(event)

                val keyCode = if (event.keyCode != 0) event.keyCode else event.scanCode
                val keyCodeBackNavigation = keyCode == KeyEvent.KEYCODE_ESCAPE ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_BACK
                val shortcutMgr = ShortcutMgr.getInstance()
                val currentTab = tabsModel.currentTab.value
                if (!keyCodeBackNavigation &&
                    shortcutMgr.handle(event, this@MainActivity, currentTab)) {
                    return true
                }

                return localCallback.dispatchKeyEvent(event)
            }

            override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                if (backNavigationEventsAdapter.dispatchGenericMotionEvent(event)) {
                    return true
                }
                return localCallback.dispatchGenericMotionEvent(event)
            }
        }
    }

    private fun hideSoftwareKeyboardIfVisible(): Boolean {
        val root = window.decorView.rootView
        val insets = ViewCompat.getRootWindowInsets(root) ?: return false
        if (!insets.isVisible(WindowInsetsCompat.Type.ime())) {
            return false
        }
        val view = currentFocus ?: root
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        return true
    }

    private fun handleBackNavigation() {
        Log.d(TAG, "handleBackNavigation")
        if (tabsModel.currentTab.value?.webEngine?.isVirtualCursorMode() == false) {
            tabsModel.currentTab.value?.webEngine?.setVirtualCursorMode(true)
            backNavigationEventsAdapter.gameControllersLongPressBForBackNavigation = false
            return
        }

        if (isCursorMenuVisible) {
            isCursorMenuVisible = false
        } else if (cursorLayout?.cursorDrawerDelegate?.canHandleBackNavigation() == true) {
            cursorLayout?.cursorDrawerDelegate?.handleBackNavigation()
        } else if (isFullscreen) {
            tabsModel.currentTab.value?.webEngine?.hideFullscreenView()
        } else if (isMenuVisible) {
            hideMenuOverlay()
        } else {
            toggleMenu()
        }
    }

    private fun showMenuOverlay() {
        val currentTab = tabsModel.currentTab.value
        if (currentTab != null) {
            lifecycleScope.launch {
                currentTab.thumbnail = currentTab.webEngine.renderThumbnail(currentTab.thumbnail)
                displayThumbnail(currentTab)
            }
        }
        isMenuVisible = true
    }

    private suspend fun displayThumbnail(currentTab: WebTabState?) {
        if (currentTab != null) {
            if (tabByTitleIndex(currentTabIndex) != currentTab) return
            if (currentTab.thumbnail != null) {
                thumbnail = currentTab.thumbnail
            } else if (currentTab.thumbnailHash != null) {
                withContext(Dispatchers.IO) {
                    val thumb = currentTab.loadThumbnail()
                    withContext(Dispatchers.Main) {
                        thumbnail = thumb ?: currentTab.thumbnail
                    }
                }
            } else {
                thumbnail = null
            }
        } else {
            thumbnail = null
        }
    }

    private fun hideMenuOverlay(hideBottomButtons: Boolean = true) {
        if (!isMenuVisible) {
            return
        }
        isMenuVisible = false
        // sync tab with titles after hide
        syncTabWithTitles()
        if (hideBottomButtons) {
            tabsModel.currentTab.value?.webEngine?.getView()?.requestFocus()
        }
    }

    private fun syncTabWithTitles() {
        val tab = tabByTitleIndex(currentTabIndex)
        if (tab == null) {
            openInNewTab(settingsModel.homePage, if (currentTabIndex < 0) 0 else tabsModel.tabsStates.size,
                needToHideMenuOverlay = true,
                navigateImmediately = true
            )
        } else if (!tab.selected) {
            changeTab(tab)
        }
    }

    private fun onDownloadStarted(fileName: String) {
        Utils.showToast(this, getString(R.string.download_started,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName))
        showMenuOverlay()
    }

    fun initiateVoiceSearch() {
        hideMenuOverlay()
        voiceSearchHelper.initiateVoiceSearch(object : VoiceSearchHelper.Callback {
            override fun onResult(text: String?) {
                if (text == null) {
                    Utils.showToast(this@MainActivity, getString(R.string.can_not_recognize))
                    return
                }
                search(text)
                hideMenuOverlay()
            }
        })
    }

    private fun onEditHomePageBookmark(favoriteItem: FavoriteItem) {
        FavoriteEditorDialog(this, object : FavoriteEditorDialog.Callback {
            override fun onDone(item: FavoriteItem) {
                viewModel.onHomePageLinkEdited(item)
            }
        }, favoriteItem).show()
    }

    private inner class WebEngineCallback(val tab: WebTabState) : WebEngineWindowProviderCallback {
        override fun getActivity(): Activity {
            return this@MainActivity
        }

        override fun onOpenInNewTabRequested(url: String, navigateImmediately: Boolean): WebEngine? {
            var index = tabsModel.tabsStates.indexOf(tabsModel.currentTab.value)
            index = if (index == -1) tabsModel.tabsStates.size else index + 1
            return openInNewTab(url, index, true, navigateImmediately)
        }

        override fun onDownloadRequested(url: String) {
            Log.i(TAG, "onDownloadRequested url: $url")
            val fileName = Uri.parse(url).lastPathSegment
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
            onDownloadRequested(url, tab.url,
                fileName, tab.webEngine.userAgentString, mimeType)
        }

        override fun onDownloadRequested(url: String, referer: String,
                                         originalDownloadFileName: String?, userAgent: String?, mimeType: String?,
                                         operationAfterDownload: Download.OperationAfterDownload, base64BlobData: String?,
                                         stream: InputStream?, size: Long, contentDisposition: String?) {
            val fileName = DownloadUtils.guessFileName(url, contentDisposition, mimeType)

            this@MainActivity.onDownloadRequested(url, referer, fileName,
                userAgent, mimeType, operationAfterDownload, base64BlobData, stream, size)
        }

        override fun onDownloadRequested(url: String, userAgent: String?, contentDisposition: String,
                                         mimetype: String?, contentLength: Long ) {
            Log.i(TAG, "DownloadListener.onDownloadStart url: $url")
            onDownloadRequested(url= url, referer = tab.url, originalDownloadFileName = null,
                userAgent = userAgent, mimeType = mimetype, size = contentLength, contentDisposition = contentDisposition)
        }

        override fun onProgressChanged(newProgress: Int) {
            isProgressVisible = true
            webProgress = newProgress
            uiHandler.removeCallbacks(progressBarHideRunnable)
            if (newProgress == 100) {
                uiHandler.postDelayed(progressBarHideRunnable, 1000)
            } else {
                uiHandler.postDelayed(progressBarHideRunnable, 5000)
            }
        }

        override fun onReceivedTitle(title: String) {
            tab.title = title
            refreshTabsUi()
            viewModel.onTabTitleUpdated(tab)
        }

        override fun requestPermissions(array: Array<String>): Int {
            val requestCode = lastCommonRequestsCode++
            this@MainActivity.requestPermissions(array, requestCode)
            return requestCode
        }

        override fun onShowFileChooser(intent: Intent): Boolean {
            try {
                startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                try {
                    intent.type = "*/*"
                    startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
                } catch (e: ActivityNotFoundException) {
                    Utils.showToast(applicationContext, getString(R.string.err_cant_open_file_chooser))
                    return false
                }
            }
            return true
        }

        override fun onReceivedIcon(icon: Bitmap) {
            refreshTabsUi()
        }

        override fun shouldOverrideUrlLoading(url: String): Boolean {
            tab.lastLoadingUrl = url

            val uri = try {
                Uri.parse(url)
            } catch (e: Exception) {
                Log.e(TAG, "shouldOverrideUrlLoading: ", e)
                return true
            }

            if (uri.scheme == null) {
                Log.d(TAG, "shouldOverrideUrlLoading: no scheme: $url")
                return true
            }

            if (URLUtil.isNetworkUrl(url) || uri.scheme.equals("javascript", true) ||
                    uri.scheme.equals("data", true) || uri.scheme.equals("about", true) ||
                    uri.scheme.equals("blob", true)) {
                Log.d(TAG, "shouldOverrideUrlLoading: network url: $url")
                return false
            }

            if (uri.scheme.equals("intent", true)) {
                Log.d(TAG, "shouldOverrideUrlLoading: intent url: $url")
                onOpenInExternalAppRequested(url)
                return true
            }

            return try {
                Log.d(TAG, "shouldOverrideUrlLoading: non-network url: $url")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(TVBro.instance.packageManager) != null) {
                    runOnUiThread {
                        askUserAndOpenInExternalApp(url, intent)
                    }
                    true
                } else {
                    Log.d(TAG, "shouldOverrideUrlLoading: no activity to handle intent")
                    runOnUiThread {
                        Utils.showToast(applicationContext, getString(R.string.err_no_app_to_handle_url))
                    }
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "shouldOverrideUrlLoading: ", e)
                true
            }
        }

        override fun onPageStarted(url: String?) {
            onWebViewUpdated(tab)
            val webViewUrl = tab.webEngine.url
            if (webViewUrl != null) {
                tab.url = webViewUrl
            } else if (url != null) {
                tab.url = url
            }
            if (tabByTitleIndex(currentTabIndex) == tab) {
                addressText = tab.url
            }
            tab.blockedAds = 0
            tab.blockedPopups = 0
            blockedAds = 0
            blockedPopups = 0
        }

        override fun onPageFinished(url: String?) {
            if (tabsModel.currentTab.value == null) {
                return
            }
            onWebViewUpdated(tab)

            val webViewUrl = tab.webEngine.url
            if (webViewUrl != null) {
                tab.url = webViewUrl
            } else if (url != null) {
                tab.url = url
            }
            if (tabByTitleIndex(currentTabIndex) == tab) {
                addressText = tab.url
            }

            tabsModel.tabsStates.onEach { if (it != tab) it.thumbnail = null }
            lifecycleScope.launch {
                val newThumbnail = tab.webEngine.renderThumbnail(tab.thumbnail)
                if (newThumbnail != null) {
                    tab.updateThumbnail(this@MainActivity, newThumbnail)
                    if (isMenuVisible && tab == tabsModel.currentTab.value) {
                        displayThumbnail(tab)
                    }
                }
            }
        }

        override fun onPageCertificateError(url: String?) {
            addressTextColor = Color.RED
        }

        override fun isAd(url: Uri, acceptHeader: String?, baseUri: Uri): Boolean? {
            return adblockModel.isAd(url, acceptHeader, baseUri)
        }

        override fun isAdBlockingEnabled(): Boolean {
            tabsModel.currentTab.value?.adblock?.apply {
                return this
            }
            return  config.adBlockEnabled
        }

        override fun isDialogsBlockingEnabled(): Boolean {
            if (tab.url == Config.HOME_PAGE_URL) return false
            return shouldBlockNewWindow(dialog = true, userGesture = false)
        }

        override fun shouldBlockNewWindow(dialog: Boolean, userGesture: Boolean): Boolean {
            val hostConfig = runBlocking(Dispatchers.Main.immediate){ tabsModel.findHostConfig(tab, false) }
            val currentBlockPopupsLevelValue = hostConfig?.popupBlockLevel ?: HostConfig.DEFAULT_BLOCK_POPUPS_VALUE
            return when (currentBlockPopupsLevelValue) {
                HostConfig.POPUP_BLOCK_NONE -> false
                HostConfig.POPUP_BLOCK_DIALOGS -> dialog
                HostConfig.POPUP_BLOCK_NEW_AUTO_OPENED_TABS -> dialog || !userGesture
                else -> true
            }
        }

        override fun onBlockedAd(uri: String) {
            Log.i(TAG, "onBlockedAd: $uri")
            if (!config.adBlockEnabled) return
            tab.blockedAds++
            blockedAds = tab.blockedAds
        }

        override fun onBlockedDialog(newTab: Boolean) {
            tab.blockedPopups++
            runOnUiThread {
                blockedPopups = tab.blockedPopups
                val msg = getString(if (newTab) R.string.new_tab_blocked else R.string.popup_dialog_blocked)
                // Use Compose snackbar fallback: Toast + NotificationView no longer available via vb, use Toast
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCreateWindow(dialog: Boolean, userGesture: Boolean): View? {
            if (shouldBlockNewWindow(dialog, userGesture)) {
                onBlockedDialog(!dialog)
                return null
            }
            val tab = WebTabState(incognito = config.incognitoMode)
            val webView = createWebView(tab) ?: return null
            val currentTab = this@MainActivity.tabsModel.currentTab.value ?: return null
            val index = tabsModel.tabsStates.indexOf(currentTab) + 1
            tabsModel.tabsStates.add(index, tab)
            changeTab(tab)
            return webView
        }

        override fun closeWindow(internalRepresentation: Any) {
            for (tab in tabsModel.tabsStates) {
                if (tab.webEngine.isSameSession(internalRepresentation)) {
                    closeTab(tab)
                    break
                }
            }
        }

        override fun onScaleChanged(oldScale: Float, newScale: Float) {
            Log.d(TAG, "onScaleChanged: oldScale: $oldScale newScale: $newScale")
            tab.scale = newScale
        }

        override fun onCopyTextToClipboardRequested(url: String) {
            val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("URL", url)
            clipBoard.setPrimaryClip(clipData)
            Toast.makeText(this@MainActivity, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }

        override fun onShareUrlRequested(url: String) {
            val share = Intent(Intent.ACTION_SEND)
            share.type = "text/plain"
            share.putExtra(Intent.EXTRA_SUBJECT, R.string.share_url)
            share.putExtra(Intent.EXTRA_TEXT, url)
            try {
                startActivity(share)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, R.string.external_app_open_error, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onOpenInExternalAppRequested(url: String) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            val activityComponent = intent.resolveActivity(this@MainActivity.packageManager)
            if (activityComponent != null && activityComponent.packageName == this@MainActivity.packageName) {
                Toast.makeText(this@MainActivity, R.string.external_app_open_error, Toast.LENGTH_SHORT).show()
                return
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, R.string.external_app_open_error, Toast.LENGTH_SHORT).show()
            }
        }

        override fun initiateVoiceSearch() {
            this@MainActivity.initiateVoiceSearch()
        }

        override fun onEditHomePageBookmarkSelected(index: Int) {
            lifecycleScope.launch {
                val bookmark = viewModel.homePageLinks.firstOrNull { it.order == index }
                var favoriteItem: FavoriteItem? = bookmark?.favoriteId?.let {
                    AppDatabase.db.favoritesDao().getById(it)
                }

                if (favoriteItem == null) {
                    favoriteItem = FavoriteItem()
                    favoriteItem.title = bookmark?.title
                    favoriteItem.url = bookmark?.url
                    favoriteItem.order = index
                    favoriteItem.homePageBookmark = true
                    onEditHomePageBookmark(favoriteItem)
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.bookmarks)
                        .setItems(arrayOf(getString(R.string.edit), getString(R.string.delete))) { _, which ->
                            when (which) {
                                0 -> onEditHomePageBookmark(favoriteItem)
                                1 -> viewModel.removeHomePageLink(bookmark!!)
                            }
                        }
                        .show()
                }
            }
        }

        override fun getHomePageLinks(): List<HomePageLink> {
            return viewModel.homePageLinks
        }

        override fun onPrepareForFullscreen() {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            isFullscreen = true
        }

        override fun onExitFullscreen() {
            if (!config.keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            isFullscreen = false
        }

        override fun onVisited(url: String) {
            val tab = tabsModel.currentTab.value ?: return

            if (!config.incognitoMode) {
                viewModel.logVisitedHistory(tab.title, url, tab.faviconHash)
            }
        }

        override fun onContextMenu(
            cursorDrawer: CursorDrawerDelegate,
            baseUri: String?,
            linkUri: String?,
            srcUri: String?,
            title: String?,
            altText: String?,
            textContent: String?,
            x: Int,
            y: Int
        ) {
            uiHandler.post {
                isCursorMenuVisible = true
            }
        }

        override fun suggestActionsForLink(baseUri: String?, linkUri: String?, srcUri: String?,
                                           title: String?, altText: String?, textContent: String?,
                                           x: Int, y: Int) {
            var s = linkUri ?: srcUri
            if (s != null && s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length - 1)
            }
            val url = s
            val isHTTPUrl = url != null && (url.startsWith("http://") || url.startsWith("https://"))
            val container = cursorLayout ?: return
            val anchor = View(this@MainActivity)
            val lp = FrameLayout.LayoutParams(1, 1)
            lp.setMargins(x, y, 0, 0)
            container.addView(anchor, lp)
            linkActionsMenu = PopupMenu(this@MainActivity, anchor, Gravity.BOTTOM).also {
                it.inflate(R.menu.menu_link)
                it.menu.findItem(R.id.miOpenInNewTab).isVisible = isHTTPUrl
                it.menu.findItem(R.id.miOpenInExternalApp).isVisible = isHTTPUrl
                it.menu.findItem(R.id.miDownload).isVisible = isHTTPUrl
                it.menu.findItem(R.id.miCopyToClipboard).isVisible = url != null
                it.menu.findItem(R.id.miShare).isVisible = url != null
                it.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.miRefreshPage -> tab.webEngine.reload()
                        R.id.miOpenInNewTab -> onOpenInNewTabRequested(url!!, true)
                        R.id.miOpenInExternalApp -> onOpenInExternalAppRequested(url!!)
                        R.id.miDownload -> onDownloadRequested(url!!)
                        R.id.miCopyToClipboard -> onCopyTextToClipboardRequested(url!!)
                        R.id.miShare -> onShareUrlRequested(url!!)
                    }
                    true
                }

                it.setOnDismissListener {
                    cursorLayout?.removeView(anchor)
                    linkActionsMenu = null
                }
                it.show()
            }
        }

        override fun markBookmarkRecommendationAsUseful(bookmarkOrder: Int) {
            viewModel.markBookmarkRecommendationAsUseful(bookmarkOrder)
        }
    }

    private fun askUserAndOpenInExternalApp(url: String, intent: Intent) {
        if (openUrlInExternalAppDialog != null) {
            return
        }
        openUrlInExternalAppDialog = AlertDialog.Builder(this)
            .setTitle(R.string.site_asks_to_open_unknown_url)
            .setMessage(getString(R.string.site_asks_to_open_unknown_url_message) + "\n\n" + url)
            .setPositiveButton(R.string.yes) { _, _ ->
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, R.string.external_app_open_error, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .setOnDismissListener {
                openUrlInExternalAppDialog = null
            }
            .show()
    }

    private val downloadServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as? DownloadService.Binder
            if (binder == null) {
                Log.e(TAG, "Download service connection failed")
                uiHandler.postDelayed({
                    bindService(Intent(this@MainActivity, DownloadService::class.java),
                        this, Context.BIND_AUTO_CREATE)
                }, 1000)
                return
            }
            downloadService = binder.service
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            downloadService = null
        }
    }
}

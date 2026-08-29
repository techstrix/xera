package com.phlox.tvwebbrowser.activity.main.dialogs.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewFeature
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.main.AdblockModel
import com.phlox.tvwebbrowser.activity.main.MainActivity
import com.phlox.tvwebbrowser.activity.main.SettingsModel
import com.phlox.tvwebbrowser.databinding.ViewSettingsMainBinding
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import com.phlox.tvwebbrowser.utils.activity
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import com.phlox.tvwebbrowser.webengine.webview.WebViewWebEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainSettingsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {
    private var vb = ViewSettingsMainBinding.inflate(LayoutInflater.from(getContext()), this, true)
    var settingsModel = ActiveModelsRepository.get(SettingsModel::class, activity!!)
    var adblockModel = ActiveModelsRepository.get(AdblockModel::class, activity!!)
    var config = AppContext.provideConfig()

    init {
        initWebBrowserEngineSettingsUI()

        initHomePageAndSearchEngineConfigUI()

        initUAStringConfigUI(context)

        initAdBlockConfigUI()

        initThemeSettingsUI()

        initWebViewAlgorithmicDarkeningWithDarkUiModeUI()

        initAllowAutoplayMediaUI()

        initWebEngineDebugUI()

        initKeepScreenOnUI()

        initJoystickAxesNavigationUI()

        initVirtualCursorPhysicsSettingsUI()

        vb.btnClearWebCache.setOnClickListener {
            (activity as MainActivity).lifecycleScope.launch {
                WebEngineFactory.clearCache(context)
                Toast.makeText(context, android.R.string.ok, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initWebBrowserEngineSettingsUI() {
        if (WebEngineFactory.getProviders().size == 1) {
            vb.llWebEngine.visibility = View.GONE
            return
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, Config.SupportedWebEngines)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        vb.spWebEngine.adapter = adapter

        vb.spWebEngine.setSelection(Config.SupportedWebEngines.indexOf(config.webEngine), false)

        vb.spWebEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (config.webEngine == Config.SupportedWebEngines[position]) return
                if (Config.SupportedWebEngines[position] == Config.ENGINE_GECKO_VIEW && !Config.canRecommendGeckoView()) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.settings_engine_change_gecko_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            config.webEngine = Config.SupportedWebEngines[position]
                            showRestartDialog()
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            vb.spWebEngine.setSelection(Config.SupportedWebEngines.indexOf(config.webEngine), false)
                        }
                        .show()
                    return
                } else if (Config.SupportedWebEngines[position] == Config.ENGINE_WEB_VIEW) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.settings_engine_change_webview_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            config.webEngine = Config.SupportedWebEngines[position]
                            showRestartDialog()
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            vb.spWebEngine.setSelection(Config.SupportedWebEngines.indexOf(config.webEngine), false)
                        }
                        .show()
                    return
                }
                config.webEngine = Config.SupportedWebEngines[position]
                showRestartDialog()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(context)
            .setTitle(R.string.need_restart)
            .setMessage(R.string.need_restart_message)
            .setPositiveButton(R.string.exit) { _, _ ->
                TVBro.instance.needToExitProcessAfterMainActivityFinish = true
                TVBro.instance.needRestartMainActivityAfterExitingProcess = true
                activity!!.finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun initThemeSettingsUI() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, context.resources.getStringArray(R.array.themes))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        vb.spTheme.adapter = adapter

        vb.spTheme.setSelection(config.theme.value.ordinal, false)

        vb.spTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (config.theme.value.ordinal == position) return
                config.theme.value = Config.Theme.values()[position]
                Toast.makeText(context, context.getString(R.string.need_restart), Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun initWebViewAlgorithmicDarkeningWithDarkUiModeUI() {
        vb.scWebViewAlgorithmicDarkeningWithDarkUiMode.isChecked =
            config.webviewUseAlgorithmicDarkeningWithDarkUiMode
        vb.scWebViewAlgorithmicDarkeningWithDarkUiMode.setOnCheckedChangeListener { _, isChecked ->
            config.webviewUseAlgorithmicDarkeningWithDarkUiMode = isChecked
        }
    }

    private fun initAllowAutoplayMediaUI() {
        vb.scAllowAutoplayMedia.isChecked = config.allowAutoplayMedia
        vb.scAllowAutoplayMedia.setOnCheckedChangeListener { _, isChecked ->
            config.allowAutoplayMedia = isChecked
        }
    }

    private fun initWebEngineDebugUI() {
        vb.scWebEngineDebug.isChecked = config.webEngineDebug
        vb.scWebEngineDebug.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                config.webEngineDebug = false
                return@setOnCheckedChangeListener
            }

            AlertDialog.Builder(context)
                .setTitle(R.string.warning)
                .setMessage(R.string.web_engine_debug_warning_message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    config.webEngineDebug = true
                    Toast.makeText(context, context.getString(R.string.need_restart), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    vb.scWebEngineDebug.isChecked = false
                }
                .setOnCancelListener {
                    vb.scWebEngineDebug.isChecked = false
                }
                .show()
        }
    }

    private fun initKeepScreenOnUI() {
        vb.scKeepScreenOn.isChecked = settingsModel.keepScreenOn.value

        vb.scKeepScreenOn.setOnCheckedChangeListener { buttonView, isChecked ->
            settingsModel.keepScreenOn.value = isChecked
        }
    }

    private fun initJoystickAxesNavigationUI() {
        vb.scNavigateWithJoystickAxes.isChecked = !config.disableMotionAxesDpadNavigation
        vb.scNavigateWithJoystickAxes.setOnCheckedChangeListener { _, isChecked ->
            config.disableMotionAxesDpadNavigation = !isChecked
        }
    }

    private fun initVirtualCursorPhysicsSettingsUI() {
        val minP = Config.CURSOR_PHYSICS_PERCENT_MIN
        val maxP = Config.CURSOR_PHYSICS_PERCENT_MAX
        val range = maxP - minP
        vb.sbCursorMaxSpeed.max = range
        vb.sbCursorAcceleration.max = range
        fun refreshValueLabels() {
            vb.tvCursorMaxSpeedValue.text = context.getString(R.string.cursor_physics_percent, config.cursorMaxSpeedPercent)
            vb.tvCursorAccelerationValue.text = context.getString(R.string.cursor_physics_percent, config.cursorAccelerationPercent)
        }
        vb.sbCursorMaxSpeed.progress = config.cursorMaxSpeedPercent - minP
        vb.sbCursorAcceleration.progress = config.cursorAccelerationPercent - minP
        refreshValueLabels()
        vb.sbCursorMaxSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                config.cursorMaxSpeedPercent = minP + progress
                refreshValueLabels()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        vb.sbCursorAcceleration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                config.cursorAccelerationPercent = minP + progress
                refreshValueLabels()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initAdBlockConfigUI() {
        // Master toggle — single source of truth, preserved
        vb.scAdblock.isChecked = config.adBlockEnabled
        vb.scAdblock.setOnCheckedChangeListener { _, isChecked ->
            config.adBlockEnabled = isChecked
            vb.llAdBlockerDetails.visibility = if (isChecked) VISIBLE else GONE
        }
        vb.llAdblock.setOnClickListener {
            vb.scAdblock.isChecked = !vb.scAdblock.isChecked
        }
        vb.llAdBlockerDetails.visibility = if (config.adBlockEnabled) VISIBLE else GONE

        // Dedicated section: uBlock defaults (expands master)
        vb.etAdBlockerListUrl.setText(config.adBlockListURL.value)
        buildUblockListsUI()

        adblockModel.clientLoading.subscribe(activity as FragmentActivity) {
            updateAdBlockInfo()
        }
        adblockModel.blockedCount.subscribe(activity as FragmentActivity) {
            updateAdBlockInfo()
        }
        adblockModel.lastError.subscribe(activity as FragmentActivity) {
            updateAdBlockInfo()
        }

        vb.btnAdBlockerUpdate.setOnClickListener {
            if (adblockModel.clientLoading.value) return@setOnClickListener
            saveAdBlockListUrl()
            saveUblockEnabledLists()
            adblockModel.loadAdBlockList(true)
            it.isEnabled = false
        }

        updateAdBlockInfo()
    }

    private fun buildUblockListsUI() {
        vb.llUblockLists.removeAllViews()
        val enabled = config.adBlockEnabledLists
        // Default lists
        for (i in Config.DEFAULT_UBLOCK_LIST_NAMES.indices) {
            val name = Config.DEFAULT_UBLOCK_LIST_NAMES[i]
            val url = Config.DEFAULT_UBLOCK_LIST_URLS[i]
            val cb = android.widget.CheckBox(context).apply {
                text = name
                isChecked = enabled.contains(url)
                isFocusable = true
                isFocusableInTouchMode = true
                textSize = 14f
                setPadding(8, 12, 8, 12)
                // TV D-pad focus visuals come from default selector
            }
            cb.setOnCheckedChangeListener { _, isChecked ->
                val cur = config.adBlockEnabledLists.toMutableSet()
                if (isChecked) cur.add(url) else cur.remove(url)
                config.adBlockEnabledLists = cur
            }
            vb.llUblockLists.addView(cb)
        }
        // Extra opt-in
        val extraHeader = android.widget.TextView(context).apply {
            text = context.getString(R.string.adblock_extra_lists_header)
            textSize = 12f
            alpha = 0.6f
            setPadding(8, 16, 8, 4)
        }
        vb.llUblockLists.addView(extraHeader)
        for (i in Config.EXTRA_UBLOCK_LIST_NAMES.indices) {
            val name = Config.EXTRA_UBLOCK_LIST_NAMES[i]
            val url = Config.EXTRA_UBLOCK_LIST_URLS[i]
            val cb = android.widget.CheckBox(context).apply {
                text = name
                isChecked = enabled.contains(url)
                isFocusable = true
                isFocusableInTouchMode = true
                textSize = 14f
                setPadding(8, 12, 8, 12)
            }
            cb.setOnCheckedChangeListener { _, isChecked ->
                val cur = config.adBlockEnabledLists.toMutableSet()
                if (isChecked) cur.add(url) else cur.remove(url)
                config.adBlockEnabledLists = cur
            }
            vb.llUblockLists.addView(cb)
        }
        // Show custom if present and not in known lists
        val customs = enabled.filter { it !in Config.DEFAULT_UBLOCK_LIST_URLS && it !in Config.EXTRA_UBLOCK_LIST_URLS }
        if (customs.isNotEmpty()) {
            val customHeader = android.widget.TextView(context).apply {
                text = context.getString(R.string.adblock_custom_active, customs.size.toString())
                textSize = 12f
                alpha = 0.6f
                setPadding(8, 12, 8, 4)
            }
            vb.llUblockLists.addView(customHeader)
        }
    }

    private fun saveUblockEnabledLists() {
        // Already saved on checkbox toggle; just ensure at least one remains enabled
        if (config.adBlockEnabledLists.isEmpty()) {
            config.adBlockEnabledLists = Config.DEFAULT_UBLOCK_LIST_URLS.toSet()
            buildUblockListsUI()
        }
    }

    private fun saveAdBlockListUrl() {
        val value = vb.etAdBlockerListUrl.text.toString().trim()
        if (value.isEmpty()) {
            // keep custom field but don't overwrite legacy if empty
            return
        }
        // If user entered a custom URL not already in enabled set, add it
        if (value != Config.DEFAULT_ADBLOCK_LIST_URL && value.startsWith("http")) {
            val cur = config.adBlockEnabledLists.toMutableSet()
            cur.add(value)
            config.adBlockEnabledLists = cur
            config.adBlockListURL.value = value
            buildUblockListsUI()
        } else if (value.isNotEmpty()) {
            config.adBlockListURL.value = value
        }
    }

    private fun updateAdBlockInfo() {
        val dateFormat = SimpleDateFormat("hh:mm dd MMMM yyyy", Locale.getDefault())
        val lastUpdate = if (config.adBlockListLastUpdate == 0L)
            context.getString(R.string.never) else
            dateFormat.format(Date(config.adBlockListLastUpdate))
        val infoText = "${context.getString(R.string.last_update)}: $lastUpdate"
        vb.tvAdBlockerListInfo.text = infoText
        vb.tvAdBlockStats.text = context.getString(R.string.adblock_stats_s, config.adBlockStatsBlocked.toString())
        val loadingAdBlockList = adblockModel.clientLoading.value
        vb.btnAdBlockerUpdate.visibility = if (loadingAdBlockList) View.GONE else View.VISIBLE
        vb.pbAdBlockerListLoading.visibility = if (loadingAdBlockList) View.VISIBLE else View.GONE
        val err = adblockModel.lastError.value
        if (err != null && !loadingAdBlockList) {
            vb.tvAdBlockerError.text = err
            vb.tvAdBlockerError.visibility = View.VISIBLE
        } else {
            vb.tvAdBlockerError.visibility = View.GONE
        }
    }

    private fun initUAStringConfigUI(context: Context) {
        if (config.userAgentString.value?.contains("TV Bro/1.0 ") == true) {//legacy ua string - now default one should be used
            config.userAgentString.value = null
        }
        val selected = if (config.userAgentString.value == null) {
            0
        } else {
            settingsModel.uaStrings.indexOf(config.userAgentString.value ?: "")
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, settingsModel.userAgentStringTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vb.spTitles.adapter = adapter

        if (selected != -1) {
            vb.spTitles.setSelection(selected, false)
            vb.etUAString.setText(settingsModel.uaStrings[selected])
        } else {
            vb.spTitles.setSelection(settingsModel.userAgentStringTitles.size - 1, false)
            vb.llUAString.visibility = View.VISIBLE
            vb.etUAString.setText(config.userAgentString.value ?: "")
            vb.etUAString.requestFocus()
        }
        vb.spTitles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position == settingsModel.userAgentStringTitles.size - 1 && vb.llUAString.visibility == View.GONE) {
                    vb.llUAString.visibility = View.VISIBLE
                    vb.llUAString.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
                    vb.etUAString.requestFocus()
                }
                vb.etUAString.setText(settingsModel.uaStrings[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
    }

    private fun initHomePageAndSearchEngineConfigUI() {
        var selected = 0
        if ("" != config.searchEngineURL.value) {
            selected = Config.SearchEnginesURLs.indexOf(config.searchEngineURL.value)
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, Config.SearchEnginesTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        vb.spEngine.adapter = adapter

        if (selected != -1) {
            vb.spEngine.setSelection(selected)
            vb.etUrl.setText(Config.SearchEnginesURLs[selected])
        } else {
            vb.spEngine.setSelection(Config.SearchEnginesTitles.size - 1)
            vb.llURL.visibility = View.VISIBLE
            vb.etUrl.setText(config.searchEngineURL.value)
            vb.etUrl.requestFocus()
        }
        vb.spEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position == (Config.SearchEnginesTitles.size - 1)) {
                    if (vb.llURL.visibility == View.GONE) {
                        vb.llURL.visibility = View.VISIBLE
                        vb.llURL.startAnimation(
                            AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
                        )
                    }
                    vb.etUrl.setText(config.searchEngineURL.value)
                    vb.etUrl.requestFocus()
                    return
                } else {
                    vb.llURL.visibility = View.GONE
                    vb.etUrl.setText(Config.SearchEnginesURLs[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val homePageSpinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, context.resources.getStringArray(R.array.home_page_modes))
        homePageSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vb.spHomePage.adapter = homePageSpinnerAdapter
        vb.spHomePage.setSelection(settingsModel.homePageMode.ordinal)

        vb.spHomePage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val homePageMode = Config.HomePageMode.entries[position]
                vb.llCustomHomePage.visibility = if (homePageMode == Config.HomePageMode.CUSTOM) View.VISIBLE else View.GONE
                vb.llHomePageLinksMode.visibility = if (homePageMode == Config.HomePageMode.HOME_PAGE) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val homePageLinksSpinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, context.resources.getStringArray(R.array.home_page_links_modes))
        homePageLinksSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vb.spHomePageLinks.adapter = homePageLinksSpinnerAdapter
        vb.spHomePageLinks.setSelection(settingsModel.homePageLinksMode.ordinal)

        vb.etCustomHomePageUrl.setText(settingsModel.homePage)
    }

    fun save() {
        val customSearchEngineUrl = vb.etUrl.text.toString()
        settingsModel.setSearchEngineURL(customSearchEngineUrl)

        val homePageMode = Config.HomePageMode.entries[vb.spHomePage.selectedItemPosition]
        val customHomePageURL = vb.etCustomHomePageUrl.text.toString()
        val homePageLinksMode = Config.HomePageLinksMode.entries[vb.spHomePageLinks.selectedItemPosition]
        settingsModel.setHomePageProperties(homePageMode, customHomePageURL, homePageLinksMode)

        val userAgent = vb.etUAString.text.toString().trim(' ')
        config.userAgentString.value = userAgent.ifEmpty { null }
        saveAdBlockListUrl()
    }
}

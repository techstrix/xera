package com.phlox.tvwebbrowser.activity.main.dialogs.settings

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
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

        (vb.spWebEngine as? android.widget.AutoCompleteTextView)?.let { actv ->
            actv.setAdapter(adapter)
            val sel = Config.SupportedWebEngines.indexOf(config.webEngine)
            if (sel != -1) actv.setText(adapter.getItem(sel).toString(), false)
            actv.setOnItemClickListener { _, _, position, _ ->
                if (config.webEngine == Config.SupportedWebEngines[position]) return@setOnItemClickListener
                if (Config.SupportedWebEngines[position] == Config.ENGINE_GECKO_VIEW && !Config.canRecommendGeckoView()) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.settings_engine_change_gecko_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            config.webEngine = Config.SupportedWebEngines[position]
                            showRestartDialog()
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            val cur = Config.SupportedWebEngines.indexOf(config.webEngine)
                            if (cur != -1) actv.setText(adapter.getItem(cur).toString(), false)
                        }
                        .show()
                    return@setOnItemClickListener
                } else if (Config.SupportedWebEngines[position] == Config.ENGINE_WEB_VIEW) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.settings_engine_change_webview_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            config.webEngine = Config.SupportedWebEngines[position]
                            showRestartDialog()
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            val cur = Config.SupportedWebEngines.indexOf(config.webEngine)
                            if (cur != -1) actv.setText(adapter.getItem(cur).toString(), false)
                        }
                        .show()
                    return@setOnItemClickListener
                }
                config.webEngine = Config.SupportedWebEngines[position]
                showRestartDialog()
            }
        }
    }

    private fun showRestartDialog() {
        MaterialAlertDialogBuilder(context)
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

        (vb.spTheme as? android.widget.AutoCompleteTextView)?.let { actv ->
            actv.setAdapter(adapter)
            actv.setText(adapter.getItem(config.theme.value.ordinal).toString(), false)
            actv.setOnItemClickListener { _, _, position, _ ->
                if (config.theme.value.ordinal == position) return@setOnItemClickListener
                config.theme.value = Config.Theme.values()[position]
                Toast.makeText(context, context.getString(R.string.need_restart), Toast.LENGTH_SHORT).show()
            }
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

            MaterialAlertDialogBuilder(context)
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
        vb.sbCursorMaxSpeed.valueFrom = minP.toFloat()
        vb.sbCursorMaxSpeed.valueTo = maxP.toFloat()
        vb.sbCursorMaxSpeed.stepSize = 1f
        vb.sbCursorAcceleration.valueFrom = minP.toFloat()
        vb.sbCursorAcceleration.valueTo = maxP.toFloat()
        vb.sbCursorAcceleration.stepSize = 1f
        fun refreshValueLabels() {
            vb.tvCursorMaxSpeedValue.text = context.getString(R.string.cursor_physics_percent, config.cursorMaxSpeedPercent)
            vb.tvCursorAccelerationValue.text = context.getString(R.string.cursor_physics_percent, config.cursorAccelerationPercent)
        }
        vb.sbCursorMaxSpeed.value = config.cursorMaxSpeedPercent.toFloat()
        vb.sbCursorAcceleration.value = config.cursorAccelerationPercent.toFloat()
        refreshValueLabels()
        vb.sbCursorMaxSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                config.cursorMaxSpeedPercent = value.toInt()
                refreshValueLabels()
            }
        }
        vb.sbCursorAcceleration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                config.cursorAccelerationPercent = value.toInt()
                refreshValueLabels()
            }
        }
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
            val cb = com.google.android.material.checkbox.MaterialCheckBox(context).apply {
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
            val cb = com.google.android.material.checkbox.MaterialCheckBox(context).apply {
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
        (vb.spTitles as? android.widget.AutoCompleteTextView)?.let { actv ->
            actv.setAdapter(adapter)
            if (selected != -1) {
                actv.setText(adapter.getItem(selected).toString(), false)
                vb.etUAString.setText(settingsModel.uaStrings[selected])
            } else {
                actv.setText(adapter.getItem(settingsModel.userAgentStringTitles.size - 1).toString(), false)
                vb.llUAString.visibility = View.VISIBLE
                vb.etUAString.setText(config.userAgentString.value ?: "")
                vb.etUAString.requestFocus()
            }
            actv.setOnItemClickListener { _, _, position, _ ->
                if (position == settingsModel.userAgentStringTitles.size - 1 && vb.llUAString.visibility == View.GONE) {
                    vb.llUAString.visibility = View.VISIBLE
                    vb.llUAString.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
                    vb.etUAString.requestFocus()
                }
                vb.etUAString.setText(settingsModel.uaStrings[position])
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

        (vb.spEngine as? android.widget.AutoCompleteTextView)?.let { actv ->
            actv.setAdapter(adapter)
            if (selected != -1) {
                actv.setText(adapter.getItem(selected).toString(), false)
                vb.etUrl.setText(Config.SearchEnginesURLs[selected])
            } else {
                actv.setText(adapter.getItem(Config.SearchEnginesTitles.size - 1).toString(), false)
                vb.llURL.visibility = View.VISIBLE
                vb.etUrl.setText(config.searchEngineURL.value)
                vb.etUrl.requestFocus()
            }
            actv.setOnItemClickListener { _, _, position, _ ->
                if (position == (Config.SearchEnginesTitles.size - 1)) {
                    if (vb.llURL.visibility == View.GONE) {
                        vb.llURL.visibility = View.VISIBLE
                        vb.llURL.startAnimation(
                            AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
                        )
                    }
                    vb.etUrl.setText(config.searchEngineURL.value)
                    vb.etUrl.requestFocus()
                    return@setOnItemClickListener
                } else {
                    vb.llURL.visibility = View.GONE
                    vb.etUrl.setText(Config.SearchEnginesURLs[position])
                }
            }
        }

        val homePageSpinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, context.resources.getStringArray(R.array.home_page_modes))
        homePageSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        (vb.spHomePage as? android.widget.AutoCompleteTextView)?.let { actv ->
            actv.setAdapter(homePageSpinnerAdapter)
            actv.setText(homePageSpinnerAdapter.getItem(settingsModel.homePageMode.ordinal).toString(), false)
            // Apply initial visibility for stored mode (CUSTOM/HOME_PAGE) without requiring selection
            run {
                val mode = settingsModel.homePageMode
                vb.llCustomHomePage.visibility = if (mode == Config.HomePageMode.CUSTOM) View.VISIBLE else View.GONE
                vb.llHomePageLinksMode.visibility = if (mode == Config.HomePageMode.HOME_PAGE) View.VISIBLE else View.GONE
            }
            actv.setOnItemClickListener { _, _, position, _ ->
                val homePageMode = Config.HomePageMode.entries[position]
                vb.llCustomHomePage.visibility = if (homePageMode == Config.HomePageMode.CUSTOM) View.VISIBLE else View.GONE
                vb.llHomePageLinksMode.visibility = if (homePageMode == Config.HomePageMode.HOME_PAGE) View.VISIBLE else View.GONE
            }
        }

        val homePageLinksSpinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, context.resources.getStringArray(R.array.home_page_links_modes))
        homePageLinksSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        (vb.spHomePageLinks as? android.widget.AutoCompleteTextView)?.let { actv ->
            actv.setAdapter(homePageLinksSpinnerAdapter)
            actv.setText(homePageLinksSpinnerAdapter.getItem(settingsModel.homePageLinksMode.ordinal).toString(), false)
            // no extra listener needed, just selection for save
        }

        vb.etCustomHomePageUrl.setText(settingsModel.homePage)
    }

    fun save() {
        val customSearchEngineUrl = vb.etUrl.text.toString()
        settingsModel.setSearchEngineURL(customSearchEngineUrl)

        val homePageMode = (vb.spHomePage as? android.widget.AutoCompleteTextView)?.let { actv ->
            val txt = actv.text.toString()
            val arr = context.resources.getStringArray(R.array.home_page_modes)
            val idx = arr.indexOf(txt).takeIf { it != -1 } ?: settingsModel.homePageMode.ordinal
            Config.HomePageMode.entries[idx]
        } ?: Config.HomePageMode.entries[0]
        val customHomePageURL = vb.etCustomHomePageUrl.text.toString()
        val homePageLinksMode = (vb.spHomePageLinks as? android.widget.AutoCompleteTextView)?.let { actv ->
            val txt = actv.text.toString()
            val arr = context.resources.getStringArray(R.array.home_page_links_modes)
            val idx = arr.indexOf(txt).takeIf { it != -1 } ?: settingsModel.homePageLinksMode.ordinal
            Config.HomePageLinksMode.entries[idx]
        } ?: Config.HomePageLinksMode.entries[0]
        settingsModel.setHomePageProperties(homePageMode, customHomePageURL, homePageLinksMode)

        val userAgent = vb.etUAString.text.toString().trim(' ')
        config.userAgentString.value = userAgent.ifEmpty { null }
        saveAdBlockListUrl()
    }
}

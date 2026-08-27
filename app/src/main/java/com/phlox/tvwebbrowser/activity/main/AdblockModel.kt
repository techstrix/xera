package com.phlox.tvwebbrowser.activity.main

import android.net.Uri
import android.widget.Toast
import com.brave.adblock.AdBlockClient
import com.brave.adblock.AdBlockClient.FilterOption
import com.brave.adblock.Utils
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModel
import com.phlox.tvwebbrowser.utils.observable.ObservableValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.*

class AdblockModel : ActiveModel() {
    companion object {
        val TAG: String = AdblockModel::class.java.simpleName

        const val SERIALIZED_LIST_FILE = "adblock_ser.dat"
        const val AUTO_UPDATE_INTERVAL_MINUTES = 60 * 24 * 7 //7 days for multi-list (more frequent than old 30d)
    }

    private var client: AdBlockClient? = null
    val clientLoading = ObservableValue(false)
    val clientReady = ObservableValue(false)
    val lastError = ObservableValue<String?>(null)
    // Keep for UI: total blocked this session + persisted
    val blockedCount = ObservableValue(0L)
    val config = AppContext.provideConfig()

    init {
        config.migrateLegacyAdBlockUrlIfNeeded()
        loadAdBlockList(false)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun loadAdBlockList(forceReload: Boolean) = modelScope.launch {
        if (clientLoading.value) return@launch
        val checkDate = Calendar.getInstance()
        checkDate.timeInMillis = config.adBlockListLastUpdate
        checkDate.add(Calendar.MINUTE, AUTO_UPDATE_INTERVAL_MINUTES)
        val now = Calendar.getInstance()
        val needUpdate = forceReload || checkDate.before(now)
        clientLoading.value = true
        lastError.value = null
        val newClient = AdBlockClient()
        var success = false
        var errorMsg: String? = null
        withContext(Dispatchers.IO) ioContext@ {
            val serializedFile = File(TVBro.instance.filesDir, SERIALIZED_LIST_FILE)
            // Try cached deserialize only if no force and cache matches enabled lists
            if (!needUpdate && serializedFile.exists() && newClient.deserialize(serializedFile.absolutePath)) {
                success = true
                return@ioContext
            }
            try {
                val enabledUrls = resolveEnabledListUrls()
                val combined = StringBuilder()
                var fetchedAtLeastOne = false
                for (urlStr in enabledUrls) {
                    try {
                        val text = URL(urlStr).openConnection().apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                        }.getInputStream().bufferedReader().use { it.readText() }
                        if (text.isNotBlank()) {
                            // Normalize Peter Lowe hosts format if needed — AdBlockClient handles hosts files
                            combined.append("\n").append(text)
                            fetchedAtLeastOne = true
                        }
                    } catch (e: Exception) {
                        // Don't fail whole update on single list failure — log and continue
                        e.printStackTrace()
                        errorMsg = e.message
                    }
                }
                // Fallback: if all remote fetches failed but we had old cache, keep old
                if (!fetchedAtLeastOne && serializedFile.exists() && newClient.deserialize(serializedFile.absolutePath)) {
                    success = true
                    errorMsg = "Some lists failed; using cached data"
                    return@ioContext
                }
                val combinedText = combined.toString()
                if (combinedText.isNotBlank()) {
                    success = newClient.parse(combinedText)
                    if (success) {
                        newClient.serialize(serializedFile.absolutePath)
                    } else {
                        errorMsg = "Parse failed"
                    }
                } else {
                    errorMsg = "All list downloads failed"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = e.message
            }
        }
        this@AdblockModel.client = newClient
        if (success) {
            config.adBlockListLastUpdate = now.timeInMillis
            clientReady.value = true
            if (errorMsg != null) {
                lastError.value = errorMsg
            }
        } else {
            clientReady.value = false
            lastError.value = errorMsg ?: "Unknown error"
            Toast.makeText(TVBro.instance, "Error loading ad-blocker list: ${lastError.value}", Toast.LENGTH_LONG).show()
        }
        clientLoading.value = false
    }

    private fun resolveEnabledListUrls(): List<String> {
        val enabled = config.adBlockEnabledLists
        // If user still has single legacy URL stored and enabled set includes it, honor it
        // Otherwise return in defined order for determinism
        val ordered = mutableListOf<String>()
        for (url in Config.DEFAULT_UBLOCK_LIST_URLS) if (enabled.contains(url)) ordered.add(url)
        for (url in Config.EXTRA_UBLOCK_LIST_URLS) if (enabled.contains(url)) ordered.add(url)
        // Include any custom URLs that are not in known lists but are in enabled set
        for (url in enabled) if (!Config.DEFAULT_UBLOCK_LIST_URLS.contains(url) && !Config.EXTRA_UBLOCK_LIST_URLS.contains(url)) ordered.add(url)
        // Fallback to defaults if empty (should not happen)
        return if (ordered.isEmpty()) Config.DEFAULT_UBLOCK_LIST_URLS.toList() else ordered
    }

    fun isAd(url: Uri, type: String?, baseUri: Uri): Boolean {
        val client = client ?: return false
        val baseHost = baseUri.host
        val filterOption = try {
            mapRequestToFilterOption(url, type)
        } catch (e: Exception) {
            return false
        }
        val result = try {
            baseHost != null && client.matches(url.toString(), filterOption, baseHost)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        if (result) {
            // Increment persisted stats off main thread via apply (already async)
            config.incrementAdBlockStats()
            blockedCount.value = config.adBlockStatsBlocked
        }
        return result
    }

    fun getStatsText(): String = "${blockedCount.value} blocked (total)"

    private fun mapRequestToFilterOption(url: Uri?, type: String?): FilterOption? {
        if (type != null) {
            if (type == "image" || type.contains("image/")) {
                return FilterOption.IMAGE
            }
            if (type == "style" || type.contains("/css")) {
                return FilterOption.CSS
            }
            if (type == "script" || type.contains("javascript")) {
                return FilterOption.SCRIPT
            }
            if (type.contains("video/")) {
                return FilterOption.OBJECT
            }
        }
        if (url != null) {
            if (Utils.uriHasExtension(url, "css")) {
                return FilterOption.CSS
            }
            if (Utils.uriHasExtension(url, "js")) {
                return FilterOption.SCRIPT
            }
            if (Utils.uriHasExtension(
                    url,
                    "png",
                    "jpg",
                    "jpeg",
                    "webp",
                    "svg",
                    "gif",
                    "bmp",
                    "tiff"
                )
            ) {
                return FilterOption.IMAGE
            }
            if (Utils.uriHasExtension(url, "mp4", "mov", "avi")) {
                return FilterOption.OBJECT
            }
        }
        return FilterOption.UNKNOWN
    }
}

package com.phlox.tvwebbrowser.activity.main.dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.AdblockModel
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import java.text.SimpleDateFormat
import java.util.*

class AdBlockOverlayDialog(
    context: Context,
    private val tab: WebTabState?,
    private val onToggle: (Boolean) -> Unit,
    private val onManage: () -> Unit
) : Dialog(context, R.style.SettingsDialog) {

    private val config = AppContext.provideConfig()
    private val adblockModel = ActiveModelsRepository.get(AdblockModel::class, context as android.app.Activity)

    init {
        setContentView(R.layout.dialog_adblock_overlay)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        val scShields = findViewById<SwitchCompat>(R.id.scShields)
        val tvStatus = findViewById<TextView>(R.id.tvShieldsStatus)
        val tvBlockedTab = findViewById<TextView>(R.id.tvMetricsBlockedTab)
        val tvBlockedTotal = findViewById<TextView>(R.id.tvMetricsBlockedTotal)
        val tvLists = findViewById<TextView>(R.id.tvMetricsLists)
        val tvLastUpdate = findViewById<TextView>(R.id.tvMetricsLastUpdate)
        val tvHost = findViewById<TextView>(R.id.tvMetricsHost)
        val btnManage = findViewById<TextView>(R.id.btnManageFilters)

        val currentEnabled = tab?.adblock ?: config.adBlockEnabled
        scShields.isChecked = currentEnabled

        fun refreshMetrics() {
            val enabled = tab?.adblock ?: config.adBlockEnabled
            tvStatus.text = if (enabled) "SHIELDS ON — Xera" else "SHIELDS OFF"
            tvStatus.setTextColor(
                context.getColor(
                    if (enabled) android.R.color.holo_green_dark else android.R.color.darker_gray
                )
            )
            val blockedTab = tab?.blockedAds ?: 0
            tvBlockedTab.text = "Blocked on this page: $blockedTab"
            tvBlockedTotal.text = "Total blocked: ${config.adBlockStatsBlocked}"
            val enabledCount = config.adBlockEnabledLists.size
            tvLists.text = "Lists: $enabledCount enabled"
            val dateFormat = SimpleDateFormat("hh:mm dd MMM yyyy", Locale.getDefault())
            val lastUpdate = if (config.adBlockListLastUpdate == 0L)
                context.getString(R.string.never) else dateFormat.format(Date(config.adBlockListLastUpdate))
            tvLastUpdate.text = "Last update: $lastUpdate"
            val host = try { tab?.url?.let { java.net.URL(it).host } ?: "-" } catch (e: Exception) { "-" }
            tvHost.text = "Host: $host"
        }

        refreshMetrics()

        scShields.setOnCheckedChangeListener { _, isChecked ->
            onToggle(isChecked)
            refreshMetrics()
        }

        // Also allow tapping row
        findViewById<View>(R.id.scShields).parent.let { parent ->
            (parent as? View)?.setOnClickListener {
                scShields.isChecked = !scShields.isChecked
            }
        }

        btnManage.setOnClickListener {
            dismiss()
            onManage()
        }
    }
}

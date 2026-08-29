package com.phlox.tvwebbrowser.activity.main.dialogs

import android.app.Dialog
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.WebTabState
import com.phlox.tvwebbrowser.ui.dialogs.ShieldsOverlayCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import java.text.SimpleDateFormat
import java.util.*

class AdBlockOverlayDialog(
    context: Context,
    private val tab: WebTabState?,
    private val onToggle: (Boolean) -> Unit,
    private val onManage: () -> Unit
) : Dialog(context, R.style.SettingsDialog) {

    private val config = AppContext.provideConfig()

    init {
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(context as? LifecycleOwner)
            setViewTreeViewModelStoreOwner(context as? ViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(context as? SavedStateRegistryOwner)
            setContent {
                var isEnabled by mutableStateOf(tab?.adblock ?: config.adBlockEnabled)
                val dateFormat = SimpleDateFormat("hh:mm dd MMM yyyy", Locale.getDefault())
                val lastUpdate = if (config.adBlockListLastUpdate == 0L) context.getString(R.string.never) else dateFormat.format(Date(config.adBlockListLastUpdate))
                val host = try { tab?.url?.let { java.net.URL(it).host } ?: "-" } catch (e: Exception) { "-" }
                XeraTheme {
                    ShieldsOverlayCompose(
                        isEnabled = isEnabled,
                        blockedTab = tab?.blockedAds ?: 0,
                        blockedTotal = config.adBlockStatsBlocked,
                        listsEnabled = config.adBlockEnabledLists.size,
                        lastUpdate = lastUpdate,
                        host = host,
                        onToggle = {
                            isEnabled = it
                            onToggle(it)
                        },
                        onManage = {
                            dismiss()
                            onManage()
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
        setContentView(composeView)
    }
}

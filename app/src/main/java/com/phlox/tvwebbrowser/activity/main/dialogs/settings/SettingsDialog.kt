package com.phlox.tvwebbrowser.activity.main.dialogs.settings

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fedir.segmentedbutton.SegmentedButton
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.SettingsModel
import com.phlox.tvwebbrowser.ui.dialogs.SettingsDialogCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.widgets.SegmentedButtonTabsAdapter

class SettingsDialog(context: Context, val model: SettingsModel) :
    Dialog(context, R.style.SettingsDialog),
    DialogInterface.OnDismissListener, VersionSettingsView.Callback {
    private var mainView: MainSettingsView? = null
    private var sbTabs: SegmentedButton

    init {
        setTitle(R.string.settings)
        // Step 8 Compose migration — primary path uses Compose, fallback to XML if Compose not available
        try {
            val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
                setViewTreeLifecycleOwner(context as? androidx.lifecycle.LifecycleOwner)
                setViewTreeViewModelStoreOwner(context as? androidx.lifecycle.ViewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(context as? androidx.savedstate.SavedStateRegistryOwner)
                setContent {
                    XeraTheme {
                        SettingsDialogCompose(
                            settingsModel = model,
                            onDismiss = { dismiss() },
                            onVersionLink = { url ->
                                dismiss()
                                val activity = context as? android.app.Activity
                                val incognito = model.config.incognitoMode
                                val target = if (incognito) com.phlox.tvwebbrowser.activity.IncognitoModeMainActivity::class.java else com.phlox.tvwebbrowser.activity.main.MainActivity::class.java
                                val intent = android.content.Intent(activity, target).apply { data = android.net.Uri.parse(url) }
                                activity?.startActivity(intent)
                            }
                        )
                    }
                }
            }
            setContentView(composeView)
            // Dummy sbTabs for legacy onDismiss save path (not used in Compose)
            sbTabs = SegmentedButton(context)
        } catch (e: Exception) {
            // Fallback to XML for TV devices without Compose runtime (should not happen after Step 5)
            setContentView(R.layout.dialog_settings)
            sbTabs = findViewById(R.id.sbTabs)
            val tabContentAdapter = object : SegmentedButtonTabsAdapter(sbTabs, findViewById(R.id.flTabsContent)) {
                override fun createContentViewForSegmentButtonId(id: Int): View {
                    return when (id) {
                        R.id.btnMainTab -> {
                            mainView = MainSettingsView(context)
                            mainView!!
                        }
                        R.id.btnShortcutsTab -> ShortcutsSettingsView(context)
                        else -> {
                            val view = VersionSettingsView(context)
                            view.callback = this@SettingsDialog
                            view
                        }
                    }
                }
            }
        }

        setOnDismissListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        mainView?.save()
    }

    override fun onNeedToCloseSettings() {
        dismiss()
    }
}
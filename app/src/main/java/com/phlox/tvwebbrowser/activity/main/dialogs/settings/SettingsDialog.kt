package com.phlox.tvwebbrowser.activity.main.dialogs.settings

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.SettingsModel
import com.phlox.tvwebbrowser.ui.dialogs.SettingsDialogCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

class SettingsDialog(context: Context, val model: SettingsModel) :
    Dialog(context, R.style.SettingsDialog),
    DialogInterface.OnDismissListener {

    init {
        setTitle(R.string.settings)
        val composeView = ComposeView(context).apply {
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
        setOnDismissListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        // Compose SettingsMainScreen updates model directly; no legacy save needed
    }
}

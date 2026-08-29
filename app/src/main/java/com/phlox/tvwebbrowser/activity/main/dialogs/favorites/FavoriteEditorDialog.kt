package com.phlox.tvwebbrowser.activity.main.dialogs.favorites

import android.app.Dialog
import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.ui.dialogs.NewFavoriteItemDialogCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme

class FavoriteEditorDialog(context: Context, private val callback: Callback, private val item: FavoriteItem) : Dialog(context) {

    interface Callback {
        fun onDone(item: FavoriteItem)
    }

    init {
        setCancelable(true)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(context as? LifecycleOwner)
            setViewTreeViewModelStoreOwner(context as? ViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(context as? SavedStateRegistryOwner)
            setContent {
                XeraTheme {
                    NewFavoriteItemDialogCompose(
                        initialTitle = item.title ?: "",
                        initialUrl = item.url ?: "",
                        onCancel = { dismiss() },
                        onDone = { title, url ->
                            var urlStr = url
                            if (!urlStr.matches(Regex("^[A-Za-z]+://.*$"))) urlStr = "https://$urlStr"
                            item.title = title
                            item.url = urlStr
                            callback.onDone(item)
                            dismiss()
                        }
                    )
                }
            }
        }
        setContentView(composeView)
    }
}

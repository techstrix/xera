package com.phlox.tvwebbrowser.activity.main.dialogs.favorites

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
import com.phlox.tvwebbrowser.model.FavoriteItem
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.ui.dialogs.FavoriteUi
import com.phlox.tvwebbrowser.ui.dialogs.FavoritesDialogCompose
import com.phlox.tvwebbrowser.ui.dialogs.NewFavoriteItemDialogCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesDialog(context: Context, val scope: CoroutineScope, private val callback: Callback, private val currentPageTitle: String?, private val currentPageUrl: String?) : Dialog(context) {

    interface Callback {
        fun onFavoriteChoosen(item: FavoriteItem?)
    }

    init {
        setCancelable(true)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(context as? LifecycleOwner)
            setViewTreeViewModelStoreOwner(context as? ViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(context as? SavedStateRegistryOwner)
        }
        // state holders
        var itemsState by mutableStateOf<List<FavoriteItem>>(emptyList())
        var isLoadingState by mutableStateOf(true)
        var isEditModeState by mutableStateOf(false)
        var showEditorState by mutableStateOf<FavoriteItem?>(null)

        // helpers
        fun toUi(items: List<FavoriteItem>) = items.map { FavoriteUi(it.id, it.title ?: "", it.url ?: "") }

        // load
        scope.launch(Dispatchers.Main) {
            val all = AppDatabase.db.favoritesDao().getAll()
            itemsState = all
            isLoadingState = false
        }

        composeView.setContent {
            XeraTheme {
                if (showEditorState != null) {
                    val editing = showEditorState!!
                    NewFavoriteItemDialogCompose(
                        initialTitle = editing.title ?: "",
                        initialUrl = editing.url ?: "",
                        onCancel = { showEditorState = null },
                        onDone = { title, url ->
                            val item = editing.apply {
                                this.title = title
                                var urlStr = url
                                if (!urlStr.matches(Regex("^[A-Za-z]+://.*$"))) urlStr = "https://$urlStr"
                                this.url = urlStr
                            }
                            showEditorState = null
                            isLoadingState = true
                            scope.launch(Dispatchers.Main) {
                                if (item.id == 0L) {
                                    val id = AppDatabase.db.favoritesDao().insert(item)
                                    item.id = id
                                } else {
                                    AppDatabase.db.favoritesDao().update(item)
                                }
                                val all = AppDatabase.db.favoritesDao().getAll()
                                itemsState = all
                                isLoadingState = false
                            }
                        }
                    )
                } else {
                    FavoritesDialogCompose(
                        items = toUi(itemsState),
                        isLoading = isLoadingState,
                        isEditMode = isEditModeState,
                        onToggleEdit = { isEditModeState = !isEditModeState },
                        onAdd = {
                            val newItem = FavoriteItem().apply {
                                title = currentPageTitle
                                url = currentPageUrl
                            }
                            showEditorState = newItem
                        },
                        onFavoriteClick = { ui ->
                            val fav = itemsState.find { it.id == ui.id }
                            if (fav != null && !fav.isFolder) {
                                callback.onFavoriteChoosen(fav)
                                dismiss()
                            }
                        },
                        onDelete = { ui ->
                            val fav = itemsState.find { it.id == ui.id } ?: return@FavoritesDialogCompose
                            scope.launch(Dispatchers.Main) {
                                AppDatabase.db.favoritesDao().delete(fav)
                                itemsState = AppDatabase.db.favoritesDao().getAll()
                            }
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
        setContentView(composeView)
    }
}

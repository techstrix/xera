package com.phlox.tvwebbrowser.activity.history

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.HistoryItem
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.ui.screens.HistoryScreen
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.utils.BaseAnimationListener
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.VoiceSearchHelper
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by fedex on 29.12.16.
 */

class HistoryActivity : AppCompatActivity(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    private var ibDelete: ImageButton? = null
    private var adapter: HistoryAdapter? = null
    private lateinit var historyModel: HistoryModel
    private val voiceSearchHelper = VoiceSearchHelper(this, VOICE_SEARCH_REQUEST_CODE,
        VOICE_SEARCH_PERMISSIONS_REQUEST_CODE)

    // Compose state (Step 8 migration) — keeps single source of truth with adapter
    private val composeItems = mutableStateListOf<HistoryItem>()
    private var composeIsMultiSelect by mutableStateOf(false)
    private var composeSelectedIds by mutableStateOf(setOf<Long>())

    internal var onListScrollListener: AbsListView.OnScrollListener = object : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {

        }

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            if (totalItemCount != 0 && firstVisibleItem + visibleItemCount >= totalItemCount - 1 && "" == historyModel.searchQuery) {
                historyModel.loadItems(false, adapter!!.realCount)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        historyModel = ActiveModelsRepository.get(HistoryModel::class, this)

        // Keep ViewBinding for legacy fallback (interim AndroidViewBinding step), but primary is Compose
        // Compose path — uses HistoryScreen with same ViewModel, preserving pagination/voice/search
        setContent {
            XeraTheme {
                HistoryScreen(
                    items = composeItems,
                    onItemClick = { item -> onHistoryItemClick(item) },
                    onItemLongClick = { item -> onHistoryItemLongClick(item) },
                    onClearHistory = { showDeleteDialog(true) },
                    onDeleteSelected = { showDeleteDialog(false) },
                    isMultiSelect = composeIsMultiSelect,
                    selectedIds = composeSelectedIds,
                    onLoadMore = {
                        // Preserve legacy pagination: only when not searching, use adapter realCount
                        if ("" == historyModel.searchQuery) {
                            historyModel.loadItems(false, adapter!!.realCount)
                        }
                    }
                )
            }
        }

        adapter = HistoryAdapter()

        historyModel.lastLoadedItems.subscribe(this, false) {
            if (it.isEmpty()) return@subscribe
            adapter!!.addItems(it)
            composeItems.clear()
            composeItems.addAll(adapter!!.items.filter { !it.isDateHeader || true })
        }

        historyModel.loadItems(false)
    }

    private fun onHistoryItemClick(item: HistoryItem) {
        if (item.isDateHeader) return
        if (composeIsMultiSelect) {
            val newSet = composeSelectedIds.toMutableSet()
            if (newSet.contains(item.id)) newSet.remove(item.id) else newSet.add(item.id)
            composeSelectedIds = newSet
            item.selected = newSet.contains(item.id)
        } else {
            val resultIntent = Intent()
            resultIntent.putExtra(KEY_URL, item.url)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun onHistoryItemLongClick(item: HistoryItem) {
        if (composeIsMultiSelect) return
        composeIsMultiSelect = true
        composeSelectedIds = setOf(item.id)
        item.selected = true
    }

    private fun showDeleteDialog(deleteAll: Boolean) {
        val hasComposeSelection = composeItems.any { composeSelectedIds.contains(it.id) || it.selected }
        if (adapter!!.items.isEmpty() || ((adapter!!.selectedItems.isEmpty() && !hasComposeSelection) && !deleteAll)) return
        AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(if (deleteAll) R.string.msg_delete_history_all else R.string.msg_delete_history)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (deleteAll) {
                            AppDatabase.db.historyDao().deleteWhereTimeLessThan(Long.MAX_VALUE)
                            adapter!!.erase()
                            composeItems.clear()
                            composeSelectedIds = emptySet()
                            composeIsMultiSelect = false
                        } else {
                            val toDelete = adapter!!.selectedItems.ifEmpty {
                                composeItems.filter { composeSelectedIds.contains(it.id) || it.selected }
                            }
                            AppDatabase.db.historyDao().delete(*toDelete.toTypedArray())
                            adapter!!.remove(toDelete)
                            composeItems.removeAll(toDelete.toSet())
                            composeSelectedIds = emptySet()
                            composeIsMultiSelect = false
                        }
                    }
                }
                .setNeutralButton(android.R.string.cancel) { dialogInterface, i -> }
                .show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_SEARCH -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    //nop
                } else if (event.action == KeyEvent.ACTION_UP) {
                    voiceSearchHelper.initiateVoiceSearch(object : VoiceSearchHelper.Callback {
                        override fun onResult(text: String?) {
                            if (text == null) {
                                Utils.showToast(this@HistoryActivity, getString(R.string.can_not_recognize))
                                return
                            }
                            adapter!!.erase()
                            composeItems.clear()
                            composeSelectedIds = emptySet()
                            composeIsMultiSelect = false
                            historyModel.searchQuery = text
                            historyModel.loadItems(true)
                        }
                    })
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!voiceSearchHelper.processActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
        grantResults: IntArray) {
        if (!voiceSearchHelper.processPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val hi = (view as HistoryItemView).historyItem
        if (hi!!.isDateHeader) return
        if (adapter!!.isMultiselectMode) {
            view.setSelection(!hi.selected)
            updateMenu()
        } else {
            val resultIntent = Intent()
            resultIntent.putExtra(KEY_URL, hi.url)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long): Boolean {
        if (adapter!!.isMultiselectMode) return false
        adapter!!.isMultiselectMode = true
        val v = view as HistoryItemView
        v.setSelection(true)
        updateMenu()
        return true
    }

    private fun updateMenu() {
        // Legacy ListView path only — Compose uses hasSelection in HistoryScreen
        if (ibDelete == null) return
        val selection = adapter!!.selectedItems
        if (selection.isEmpty()) {
            if (ibDelete!!.visibility == View.GONE) return
            val anim = AnimationUtils.loadAnimation(this, R.anim.right_menu_out_anim)
            anim.setAnimationListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animation) {
                    ibDelete!!.visibility = View.GONE
                }
            })
            ibDelete!!.startAnimation(anim)
        } else {
            if (ibDelete!!.visibility == View.VISIBLE) return
            ibDelete!!.visibility = View.VISIBLE
            ibDelete!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.right_menu_in_anim))
        }
    }

    override fun onBackPressed() {
        if (composeIsMultiSelect) {
            composeIsMultiSelect = false
            composeSelectedIds = emptySet()
            // clear selected flags
            composeItems.forEach { it.selected = false }
            adapter!!.isMultiselectMode = false
            return
        }
        if (adapter!!.isMultiselectMode) {
            adapter!!.isMultiselectMode = false
            updateMenu()
            return
        }
        super.onBackPressed()
    }

    private fun showItemOptionsPopup(v: HistoryItemView) {
        val pm = PopupMenu(this, v, Gravity.BOTTOM)
        pm.menu.add(R.string.delete)
        pm.setOnMenuItemClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                AppDatabase.db.historyDao().delete(v.historyItem!!)
                adapter!!.remove(v.historyItem!!)
            }
            true
        }
        pm.show()
    }

    fun onClearHistoryClick(view: View) {
        showDeleteDialog(true)
    }

    fun onClearHistoryItemsClick(view: View) {
        showDeleteDialog(false)
    }

    companion object {
        private const val VOICE_SEARCH_REQUEST_CODE = 10001
        private const val VOICE_SEARCH_PERMISSIONS_REQUEST_CODE = 10002

        const val KEY_URL = "url"
    }
}

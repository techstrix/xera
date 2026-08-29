package com.phlox.tvwebbrowser.activity.history

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
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
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.VoiceSearchHelper
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyModel: HistoryModel
    private val voiceSearchHelper = VoiceSearchHelper(this, VOICE_SEARCH_REQUEST_CODE,
        VOICE_SEARCH_PERMISSIONS_REQUEST_CODE)

    private val composeItems = mutableStateListOf<HistoryItem>()
    private var composeIsMultiSelect by mutableStateOf(false)
    private var composeSelectedIds by mutableStateOf(setOf<Long>())
    private var lastHeaderDate: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        historyModel = ActiveModelsRepository.get(HistoryModel::class, this)

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
                        if ("" == historyModel.searchQuery) {
                            val offset = composeItems.count { !it.isDateHeader }.toLong()
                            historyModel.loadItems(false, offset)
                        }
                    }
                )
            }
        }

        historyModel.lastLoadedItems.subscribe(this, false) {
            if (it.isEmpty()) return@subscribe
            addItemsWithHeaders(it)
        }

        historyModel.loadItems(false)
    }

    private fun addItemsWithHeaders(newItems: List<HistoryItem>) {
        for (hi in newItems) {
            if (!Utils.isSameDate(hi.time, lastHeaderDate)) {
                lastHeaderDate = hi.time
                composeItems.add(HistoryItem.createDateHeaderInfo(hi.time))
            }
            composeItems.add(hi)
        }
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
        val hasSelection = composeItems.any { composeSelectedIds.contains(it.id) || it.selected }
        if (composeItems.isEmpty() || (!hasSelection && !deleteAll)) return
        AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(if (deleteAll) R.string.msg_delete_history_all else R.string.msg_delete_history)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (deleteAll) {
                            AppDatabase.db.historyDao().deleteWhereTimeLessThan(Long.MAX_VALUE)
                            composeItems.clear()
                            lastHeaderDate = -1
                            composeSelectedIds = emptySet()
                            composeIsMultiSelect = false
                        } else {
                            val toDelete = composeItems.filter { composeSelectedIds.contains(it.id) || it.selected }
                            AppDatabase.db.historyDao().delete(*toDelete.toTypedArray())
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
                } else if (event.action == KeyEvent.ACTION_UP) {
                    voiceSearchHelper.initiateVoiceSearch(object : VoiceSearchHelper.Callback {
                        override fun onResult(text: String?) {
                            if (text == null) {
                                Utils.showToast(this@HistoryActivity, getString(R.string.can_not_recognize))
                                return
                            }
                            composeItems.clear()
                            lastHeaderDate = -1
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

    override fun onBackPressed() {
        if (composeIsMultiSelect) {
            composeIsMultiSelect = false
            composeSelectedIds = emptySet()
            composeItems.forEach { it.selected = false }
            return
        }
        super.onBackPressed()
    }

    companion object {
        private const val VOICE_SEARCH_REQUEST_CODE = 10001
        private const val VOICE_SEARCH_PERMISSIONS_REQUEST_CODE = 10002
        const val KEY_URL = "url"
    }
}

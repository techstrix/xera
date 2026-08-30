package com.phlox.tvwebbrowser.activity.history

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.databinding.ActivityHistoryBinding
import com.phlox.tvwebbrowser.singleton.AppDatabase
import com.phlox.tvwebbrowser.utils.BaseAnimationListener
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.VoiceSearchHelper
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by fedex on 29.12.16.
 */

class HistoryActivity : AppCompatActivity(){

    private lateinit var vb: ActivityHistoryBinding
    private var ibDelete: View? = null
    private var adapter: HistoryAdapter? = null
    private lateinit var historyModel: HistoryModel
    private val voiceSearchHelper = VoiceSearchHelper(this, VOICE_SEARCH_REQUEST_CODE,
        VOICE_SEARCH_PERMISSIONS_REQUEST_CODE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(vb.root)

        historyModel = ActiveModelsRepository.get(HistoryModel::class, this)

        ibDelete = findViewById(R.id.ibDelete)

        adapter = HistoryAdapter(this)
        vb.listView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        vb.listView.adapter = adapter
        // MaterialDivider already in item layout, no extra decoration needed
        vb.listView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                if (total != 0 && lastVisible >= total - 1 && "" == historyModel.searchQuery) {
                    historyModel.loadItems(false, adapter!!.realCount)
                }
            }
        })

        historyModel.lastLoadedItems.subscribe(this, false) {
            if (it.isEmpty()) return@subscribe
            adapter!!.addItems(it)
            vb.listView.requestFocus()
        }

        historyModel.loadItems(false)
    }

    fun onHistoryItemClick(view: HistoryItemView) {
        val hi = view.historyItem
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

    fun onHistoryItemLongClick(view: HistoryItemView): Boolean {
        if (adapter!!.isMultiselectMode) return false
        adapter!!.isMultiselectMode = true
        view.setSelection(true)
        updateMenu()
        return true
    }

    private fun showDeleteDialog(deleteAll: Boolean) {
        if (adapter!!.items.isEmpty() || (adapter!!.selectedItems.isEmpty() && !deleteAll)) return
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(if (deleteAll) R.string.msg_delete_history_all else R.string.msg_delete_history)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (deleteAll) {
                            AppDatabase.db.historyDao().deleteWhereTimeLessThan(Long.MAX_VALUE)
                            adapter!!.erase()
                        } else {
                            AppDatabase.db.historyDao().delete(*(adapter!!.selectedItems).toTypedArray())
                            adapter!!.remove(adapter!!.selectedItems)
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



    private fun updateMenu() {
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

package com.phlox.tvwebbrowser.activity.history

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.phlox.tvwebbrowser.model.HistoryItem
import com.phlox.tvwebbrowser.utils.Utils

import java.util.ArrayList

/**
 * Created by fedex on 29.12.16.
 * Migrated to RecyclerView + MaterialCardView (M3)
 */

class HistoryAdapter(private val activity: HistoryActivity? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val items = ArrayList<HistoryItem>()
    private var lastHeaderDate: Long = -1
    var realCount: Long = 0
        private set
    var isMultiselectMode = false
        set(multiselectMode) {
            field = multiselectMode
            if (!multiselectMode) {
                for (hi in items) {
                    hi.selected = false
                }
            }
            notifyDataSetChanged()
        }
    private val _tmpSelected = ArrayList<HistoryItem>()

    val selectedItems: List<HistoryItem>
        get() {
            _tmpSelected.clear()
            for (hi in items) {
                if (hi.selected) {
                    _tmpSelected.add(hi)
                }
            }
            return _tmpSelected
        }

    fun addItems(items: List<HistoryItem>) {
        if (items.isEmpty()) {
            return
        }
        for (hi in items) {
            if (!Utils.isSameDate(hi.time, lastHeaderDate)) {
                lastHeaderDate = hi.time
                this.items.add(HistoryItem.createDateHeaderInfo(hi.time))
            }
            this.items.add(hi)
            realCount++
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isDateHeader) VIEW_TYPE_HEADER else VIEW_TYPE_HISTORY_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = HistoryItemView(parent.context, viewType)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView as HistoryItemView
        view.setHistoryItem(items[position], isMultiselectMode)
        // TV focus + click handling (keep positions, handle header as non-clickable)
        view.setOnClickListener {
            activity?.onHistoryItemClick(view)
        }
        view.setOnLongClickListener {
            activity?.onHistoryItemLongClick(view) ?: false
        }
    }

    fun erase() {
        items.clear()
        notifyDataSetChanged()
    }

    fun remove(historyItem: HistoryItem) {
        items.remove(historyItem)
        notifyDataSetChanged()
    }

    fun remove(selectedItems: List<HistoryItem>) {
        items.removeAll(selectedItems)
        notifyDataSetChanged()
    }

    companion object {
        val VIEW_TYPE_HISTORY_ITEM = 0
        val VIEW_TYPE_HEADER = 1
    }
}

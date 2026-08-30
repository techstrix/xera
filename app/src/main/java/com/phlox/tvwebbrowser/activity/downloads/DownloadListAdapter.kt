package com.phlox.tvwebbrowser.activity.downloads

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.utils.Utils

import java.util.ArrayList

/**
 * Created by PDT on 24.01.2017.
 * Migrated to RecyclerView + MaterialCardView (M3)
 */

class DownloadListAdapter(private val downloadsActivity: DownloadsActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val downloads = ArrayList<Download>()
    private var lastHeaderDate: Long = -1
    var realCount: Long = 0
        private set

    fun addItems(items: List<Download>) {
        if (items.isEmpty()) {
            return
        }
        for (download in items) {
            if (!Utils.isSameDate(download.time, lastHeaderDate)) {
                lastHeaderDate = download.time
                this.downloads.add(Download.createDateHeaderInfo(download.time))
            }
            this.downloads.add(download)
            realCount++
        }
        notifyDataSetChanged()
    }

    val items: List<Download> get() = downloads

    override fun getItemCount(): Int = downloads.size

    override fun getItemViewType(position: Int): Int {
        return if (downloads[position].isDateHeader) VIEW_TYPE_HEADER else VIEW_TYPE_DOWNLOAD_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = DownloadListItemView(downloadsActivity, viewType)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView as DownloadListItemView
        view.download = downloads[position]
        // Click handling via view's own listeners (set in DownloadsActivity via view.setOnClickListener)
        view.setOnClickListener {
            downloadsActivity.onDownloadItemClick(view)
        }
        view.setOnLongClickListener {
            downloadsActivity.onDownloadItemLongClick(view)
            true
        }
    }

    fun remove(download: Download) {
        downloads.remove(download)
        notifyDataSetChanged()
    }

    companion object {
        val VIEW_TYPE_DOWNLOAD_ITEM = 0
        val VIEW_TYPE_HEADER = 1
    }
}

package com.phlox.tvwebbrowser.widgets

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButtonToggleGroup

abstract class SegmentedButtonTabsAdapter(val segmentedButton: MaterialButtonToggleGroup, val contentLayout: ViewGroup) {
    var currentContentView: View? = null
        private  set
    private val contentViewsCache = SparseArray<View>()
    var callback: Callback? = null

    interface Callback {
        fun onCheckedChanged(button: MaterialButtonToggleGroup, checkedButtonId: Int, byUser: Boolean)
    }

    private val segmentedButtonCheckedChangeListener = MaterialButtonToggleGroup.OnButtonCheckedListener { button, checkedButtonId, isChecked ->
        if (isChecked) {
            showTab(checkedButtonId)
            callback?.onCheckedChanged(button, checkedButtonId, true)
        }
    }

    init {
        segmentedButton.addOnButtonCheckedListener(segmentedButtonCheckedChangeListener)
        showTab(segmentedButton.checkedButtonId)
    }

    private fun showTab(checkedSegmentId: Int) {
        if (checkedSegmentId == View.NO_ID) return
        contentLayout.removeAllViews()
        var view = contentViewsCache.get(checkedSegmentId)
        if (view == null) {
            view = createContentViewForSegmentButtonId(checkedSegmentId)
            contentViewsCache.put(checkedSegmentId, view)
        }
        contentLayout.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        currentContentView = view
    }

    abstract fun createContentViewForSegmentButtonId(id: Int): View
}
package com.phlox.tvwebbrowser.activity.main.dialogs.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.RelativeLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.res.ResourcesCompat
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.dialogs.ShortcutDialog
import com.phlox.tvwebbrowser.singleton.shortcuts.Shortcut
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import com.phlox.tvwebbrowser.ui.components.ShortcutRow
import com.phlox.tvwebbrowser.ui.theme.XeraTheme


class ShortcutsSettingsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ListView(context, attrs, defStyleAttr), AdapterView.OnItemClickListener {

    val items = Shortcut.entries.map { it.titleResId }

    init {
        selector = ResourcesCompat.getDrawable(context.resources, R.drawable.list_item_bg_selector, null)
        adapter = ShortcutItemAdapter()
        onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val dialog = ShortcutDialog(context,
                ShortcutMgr.getInstance()
                        .findForId(position)
        )
        dialog.setOnDismissListener {
            (adapter as BaseAdapter).notifyDataSetChanged()
        }
        dialog.show()
    }

    inner class ShortcutItemAdapter: BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = if (convertView != null) {
                convertView as ShortcutItemView
            } else {
                ShortcutItemView(context)
            }
            view.bind(position, items[position])
            return view
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return items.size
        }
    }

    inner class ShortcutItemView @JvmOverloads constructor(
            context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : RelativeLayout(context, attrs, defStyleAttr) {
        private var titleState by mutableStateOf("")
        private var keyState by mutableStateOf("")
        private val composeView = ComposeView(context).apply {
            setContent { XeraTheme { ShortcutRow(title = titleState, key = keyState) } }
        }

        init {
            addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        fun bind(position: Int, titleRes: Int) {
            val shortcut = ShortcutMgr.getInstance().findForId(position)
            titleState = context.getString(titleRes)
            keyState = if (shortcut.keyCode == 0) context.getString(R.string.not_set) else Shortcut.shortcutKeysToString(shortcut, context)
        }
    }
}
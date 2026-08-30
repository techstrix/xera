package com.phlox.tvwebbrowser.activity.main.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.phlox.tvwebbrowser.Config
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.SettingsModel

/**
 * Created by fedex on 18.01.17.
 */

object SearchEngineConfigDialogFactory {
    interface Callback {
        fun onDone(url: String)
    }

    fun show(context: Context, settings: SettingsModel, cancellable: Boolean, callback: Callback) {

        var selected = 0
        if ("" != settings.config.searchEngineURL.value) {
            selected = Config.SearchEnginesURLs.indexOf(settings.config.searchEngineURL.value)
        }

        val builder = MaterialAlertDialogBuilder(context)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_search_engine, null)
        val etUrl = view.findViewById(R.id.etUrl) as EditText
        val llUrl = view.findViewById(R.id.llURL) as LinearLayout

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, Config.SearchEnginesTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spEngine = view.findViewById(R.id.spEngine) as android.widget.AutoCompleteTextView
        spEngine.setAdapter(adapter)

        if (selected != -1) {
            spEngine.setText(adapter.getItem(selected).toString(), false)
            etUrl.setText(Config.SearchEnginesURLs[selected])
        } else {
            spEngine.setText(adapter.getItem(Config.SearchEnginesTitles.size - 1).toString(), false)
            llUrl.visibility = View.VISIBLE
            etUrl.setText(settings.config.searchEngineURL.value)
            etUrl.requestFocus()
        }
        spEngine.setOnItemClickListener { _, _, position, _ ->
            if (position == Config.SearchEnginesTitles.size - 1 && llUrl.visibility == View.GONE) {
                llUrl.visibility = View.VISIBLE
                llUrl.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
                etUrl.requestFocus()
            }
            etUrl.setText(Config.SearchEnginesURLs[position])
        }

        builder.setView(view)
                .setCancelable(cancellable)
                .setTitle(R.string.engine)
                .setPositiveButton(R.string.save) { dialog, which ->
                    val url = etUrl.text.toString()
                    settings.setSearchEngineURL(url)
                    callback.onDone(url)
                }
                .show()
    }
}

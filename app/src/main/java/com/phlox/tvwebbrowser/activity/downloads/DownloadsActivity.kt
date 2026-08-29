package com.phlox.tvwebbrowser.activity.downloads

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.phlox.tvwebbrowser.BuildConfig
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.ui.screens.DownloadsScreen
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadsActivity : AppCompatActivity(), ActiveDownloadsModel.Listener {
    private val listeners = ArrayList<ActiveDownloadsModel.Listener>()

    private lateinit var activeDownloadsModel: ActiveDownloadsModel
    private lateinit var downloadsHistoryModel: DownloadsHistoryModel

    private val composeItems = mutableStateListOf<Download>()
    private var lastHeaderDate: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate this:" + System.identityHashCode(this))
        activeDownloadsModel = ActiveModelsRepository.get(ActiveDownloadsModel::class, this)
        downloadsHistoryModel = ActiveModelsRepository.get(DownloadsHistoryModel::class, this)

        setContent {
            XeraTheme {
                DownloadsScreen(
                    items = composeItems,
                    onItemClick = { dl -> handleDownloadClick(dl) },
                    onItemLongClick = { dl -> handleDownloadLongClick(dl) },
                    onLoadMore = { downloadsHistoryModel.loadNextItems() }
                )
            }
        }

        downloadsHistoryModel.lastLoadedItems.subscribe(this, false, {
            if (it.isNotEmpty()) {
                addItemsWithHeaders(it)
            }
        })

        if (downloadsHistoryModel.allItems.isEmpty()) {
            downloadsHistoryModel.loadNextItems()
        } else {
            addItemsWithHeaders(downloadsHistoryModel.allItems)
        }
    }

    private fun addItemsWithHeaders(newItems: List<Download>) {
        for (download in newItems) {
            if (!Utils.isSameDate(download.time, lastHeaderDate)) {
                lastHeaderDate = download.time
                composeItems.add(Download.createDateHeaderInfo(download.time))
            }
            composeItems.add(download)
        }
    }

    private fun handleDownloadClick(download: Download) {
        if (download.isDateHeader) return
        if (download.size != download.bytesReceived) return
        val fileURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Uri.parse(download.filepath)
        } else {
            val file = File(download.filepath)
            if (!file.exists()) {
                Utils.showToast(this, R.string.file_not_found)
                return
            }
            FileProvider.getUriForFile(this@DownloadsActivity, BuildConfig.APPLICATION_ID + ".provider", file)
        }
        val openIntent = Intent(Intent.ACTION_VIEW)
        val mimeType = contentResolver.getType(fileURI)
        openIntent.setDataAndType(fileURI, mimeType)
        openIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            startActivity(openIntent)
        } catch (e: ActivityNotFoundException) {
            Utils.showToast(this, getString(R.string.no_app_for_file_type))
        }
    }

    private fun handleDownloadLongClick(download: Download) {
        if (download.isDateHeader) return
        val isFinished = download.size == Download.BROKEN_MARK || download.size == Download.CANCELLED_MARK || download.size == download.bytesReceived
        if (!isFinished) {
            AlertDialog.Builder(this)
                .setTitle(download.filename)
                .setItems(arrayOf(getString(R.string.cancel))) { _, _ ->
                    activeDownloadsModel.cancelDownload(download)
                }
                .show()
        } else {
            val options = mutableListOf<String>()
            val ids = mutableListOf<Int>()
            if (download.filename.endsWith(".apk", true) && download.size == download.bytesReceived) {
                options.add(getString(R.string.install)); ids.add(0)
            }
            options.add(getString(R.string.open_folder)); ids.add(1)
            options.add(getString(R.string.delete)); ids.add(2)
            AlertDialog.Builder(this)
                .setTitle(download.filename)
                .setItems(options.toTypedArray()) { _, which ->
                    when (ids[which]) {
                        0 -> installAPK(download)
                        1 -> {
                            val uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
                            val intent = Intent(Intent.ACTION_VIEW); intent.setDataAndType(uri, "resource/folder")
                            if (intent.resolveActivityInfo(packageManager, 0) != null) startActivity(intent) else Utils.showToast(this, R.string.no_file_explorer_msg)
                        }
                        2 -> lifecycleScope.launch(Dispatchers.Main) {
                            activeDownloadsModel.deleteItem(download)
                            composeItems.remove(download)
                        }
                    }
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        activeDownloadsModel.registerListener(this)
    }

    override fun onPause() {
        activeDownloadsModel.unregisterListener(this@DownloadsActivity)
        super.onPause()
    }

    private fun installAPK(download: Download) {
        val canInstallFromOtherSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) true else Settings.Secure.getInt(this.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1
        if(canInstallFromOtherSources) {
            launchInstallAPKActivity(this, download)
        } else {
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) R.string.turn_on_unknown_sources_for_app else R.string.turn_on_unknown_sources)
                    .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which -> run {
                        val intentSettings = Intent()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            intentSettings.action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                            intentSettings.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        } else {
                            intentSettings.action = Settings.ACTION_SECURITY_SETTINGS
                        }
                        intentSettings.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            startActivityForResult(intentSettings, REQUEST_CODE_UNKNOWN_APP_SOURCES)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
                        }
                    }})
                    .show()
        }
    }

    override fun onDownloadUpdated(downloadInfo: Download) {
        for (i in listeners.indices) listeners[i].onDownloadUpdated(downloadInfo)
        // trigger recomposition for progress
        val idx = composeItems.indexOfFirst { it.id == downloadInfo.id }
        if (idx != -1) {
            composeItems[idx] = downloadInfo
        }
    }

    override fun onDownloadError(downloadInfo: Download, responseCode: Int, responseMessage: String) {
        for (i in listeners.indices) listeners[i].onDownloadError(downloadInfo, responseCode, responseMessage)
    }

    override fun onAllDownloadsComplete() {}

    fun registerListener(listener: ActiveDownloadsModel.Listener) { listeners.add(listener) }
    fun unregisterListener(listener: ActiveDownloadsModel.Listener) { listeners.remove(listener) }

    companion object {
        const val REQUEST_CODE_UNKNOWN_APP_SOURCES = 10007
        private const val REQUEST_CODE_INSTALL_PACKAGE = 10008
        val TAG: String = DownloadsActivity::class.java.simpleName

        fun launchInstallAPKActivity(activity: Activity, download: Download) {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("apk")
            val apkURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Uri.parse(download.filepath) else {
                val file = File(download.filepath)
                FileProvider.getUriForFile(activity, activity.applicationContext.packageName + ".provider", file)
            }
            val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
            install.setDataAndType(apkURI, mimeType)
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                activity.startActivityForResult(install, REQUEST_CODE_INSTALL_PACKAGE)
            } catch (e: Exception) {
                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

package com.phlox.tvwebbrowser.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    fun install(context: Context) {
        if (defaultHandler != null) return
        appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i(TAG, "CrashHandler installed")
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val stack = sw.toString()
            Log.e(TAG, "Uncaught exception in thread ${t.name}", e)

            // Write to file for later retrieval
            try {
                val ctx = appContext
                if (ctx != null) {
                    val dir = File(ctx.filesDir, "crashes")
                    dir.mkdirs()
                    val fmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                    val file = File(dir, "crash_${fmt.format(Date())}.log")
                    file.writeText(
                        "Thread: ${t.name}\n" +
                        "SDK: ${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "Time: ${Date()}\n\n" +
                        stack + "\n"
                    )
                    Log.i(TAG, "Crash written to ${file.absolutePath}")
                }
            } catch (ioe: Exception) {
                Log.e(TAG, "Failed to write crash file", ioe)
            }
        } catch (ignore: Exception) {
        }
        // Chain to default handler (will show system crash dialog and kill process)
        defaultHandler?.uncaughtException(t, e)
    }

    fun getLastCrashLog(context: Context): String? {
        return try {
            val dir = File(context.filesDir, "crashes")
            if (!dir.exists()) return null
            val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return null
            files.firstOrNull()?.readText()
        } catch (e: Exception) {
            null
        }
    }
}

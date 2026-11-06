package com.ljyh.mei.utils.log

import android.content.Context
import android.util.Log
import com.ljyh.mei.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val context: Context) : Timber.Tree() {

    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // 使用单线程 IO 顺序写入文件，避免并发锁竞争与频繁创建协程阻塞
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Release 模式下仅落盘 WARN / ERROR，彻底消除高频 DEBUG 日志对 IO 与帧率的影响
        val minPriority = if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN
        if (priority < minPriority) return

        scope.launch {
            saveLogToFile(priority, tag, message, t)
        }
    }

    private fun saveLogToFile(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val logDir = File(context.filesDir, "app_logs")
            if (!logDir.exists()) logDir.mkdirs()

            val now = Date()
            val date = fileDateFormat.format(now)
            val file = File(logDir, "log-$date.txt")

            val time = logDateFormat.format(now)
            val level = when (priority) {
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                else -> "V"
            }

            val logEntry = StringBuilder()
            logEntry.append("$time $level/$tag: $message\n")

            if (t != null) {
                logEntry.append(Log.getStackTraceString(t))
            }

            FileWriter(file, true).use { writer ->
                writer.append(logEntry.toString())
            }

        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error writing log to file", e)
        }
    }
}
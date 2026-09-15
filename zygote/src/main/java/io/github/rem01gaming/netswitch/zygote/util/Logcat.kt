package io.github.rem01gaming.netswitch.zygote.util

import android.util.Log

/**
 * 精简日志。
 *
 * 去掉了 HMA 原版对 android.os.SystemProperties 的引用（@hide API，
 * 编译期无存根），改为无条件 Log.println。Zygisk 环境下 logd 必然可用。
 */
@Suppress("SpellCheckingInspection")
object Logcat {
    private const val LOGCAT_TAG = "net-switch-zygote"

    fun logV(tag: String, cause: Throwable? = null, msg: () -> String) =
        logWithLevel(Log.VERBOSE, tag, cause, msg)

    fun logD(tag: String, cause: Throwable? = null, msg: () -> String) =
        logWithLevel(Log.DEBUG, tag, cause, msg)

    fun logI(tag: String, cause: Throwable? = null, msg: () -> String) =
        logWithLevel(Log.INFO, tag, cause, msg)

    fun logW(tag: String, cause: Throwable? = null, msg: () -> String) =
        logWithLevel(Log.WARN, tag, cause, msg)

    fun logE(tag: String, cause: Throwable? = null, msg: () -> String) =
        logWithLevel(Log.ERROR, tag, cause, msg)

    @JvmStatic
    fun logILegacy(tag: String, msg: String) = logI(tag) { msg }

    @JvmStatic
    fun logELegacy(tag: String, msg: String, cause: Throwable?) = logE(tag, cause) { msg }

    private fun logWithLevel(level: Int, tag: String, cause: Throwable?, msg: () -> String) {
        val parsed = parseLog(level, tag, msg(), cause)
        Log.println(level, LOGCAT_TAG, parsed)
    }

    private fun parseLog(level: Int, tag: String, msg: String, cause: Throwable? = null) = buildString {
        val levelStr = when (level) {
            Log.VERBOSE -> "V"
            Log.DEBUG   -> "D"
            Log.INFO    -> "I"
            Log.WARN    -> "W"
            Log.ERROR   -> "E"
            else        -> "?"
        }
        append("[$levelStr] ($tag) $msg")
        if (!endsWith('\n')) append('\n')
        if (cause != null) {
            append(Log.getStackTraceString(cause))
            if (!endsWith('\n')) append('\n')
        }
    }
}

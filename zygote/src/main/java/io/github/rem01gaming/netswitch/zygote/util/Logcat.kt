package io.github.rem01gaming.netswitch.zygote.util

import android.os.SystemProperties
import android.util.Log

/**
 * 精简自 HMA-OSS Logcat.kt。
 *
 * 去掉 service.config.errorOnlyLog / detailLog 依赖（那是 HMA 的 app 配置），
 * 只保留 logd 可用性与 logcat 输出。tag 固定 net-switch-zygote，
 * 便于用 `logcat -s net-switch-zygote:V` 过滤。
 */
@Suppress("SpellCheckingInspection")
object Logcat {
    private const val LOGCAT_TAG = "net-switch-zygote"

    private val logdReady: Boolean by lazy {
        SystemProperties.get("init.svc.logd") == "running"
    }

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
        if (!logdReady) return
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

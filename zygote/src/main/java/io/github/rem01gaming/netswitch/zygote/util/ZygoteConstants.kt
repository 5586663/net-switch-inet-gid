package io.github.rem01gaming.netswitch.zygote.util

/**
 * 精简自 HMA-OSS ZygoteConstants.kt。
 *
 * 只保留 ZygoteHook 需要 hook 的三个方法所属的类名。
 */
@Suppress("SpellCheckingInspection")
object ZygoteConstants {
    const val ZYGOTE_PROCESS_CLASS = "android.os.ZygoteProcess"
    const val NATIVE_ZYGOTE_PROCESS_CLASS = "android.os.NativeZygoteProcess"
    const val SERVICE_RECORD_CLASS = "com.android.server.am.ServiceRecord"
    const val CONSTRUCTOR_METHOD_NAME = "<init>"
}

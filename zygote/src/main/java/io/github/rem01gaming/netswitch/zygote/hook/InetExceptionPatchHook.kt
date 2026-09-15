package io.github.rem01gaming.netswitch.zygote.hook

import com.v7878.unsafe.invoke.Transformers
import com.v7878.vmtools.Hooks
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logD
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logE
import java.net.UnknownHostException

/**
 * 在 App 进程内把 libcore 抛出的 SecurityException("Permission denied (missing INTERNET permission?)")
 * 转成 UnknownHostException (IOException 子类)。
 *
 * 背景：剥掉 INET_GID(3003) 后，getaddrinfo 返回 EPERM，libcore 的
 * Inet6AddressImpl.lookupHostByName 硬编码把它包装成 SecurityException(RuntimeException)，
 * okhttp 等库不 catch 该类型，导致后台线程崩溃。
 *
 * 本 hook 只在 lookupHostByName 真的抛 SecurityException 时替换异常类型；
 * 对正常进程零影响。
 *
 * 来源：设备作者 2026-09-15 补丁，已实测修复魅族图库 com.meizu.media.gallery 闪退。
 */
object InetExceptionPatchHook {

    private const val TAG = "InetExceptionPatch"

    fun load() {
        try {
            val lookup = Class.forName("java.net.Inet6AddressImpl")
                .getDeclaredMethod("lookupHostByName", String::class.java)
            lookup.isAccessible = true

            Hooks.hook(
                lookup,
                Hooks.EntryPointType.CURRENT,
                { original, frame ->
                    try {
                        Transformers.invokeExact(original, frame)
                    } catch (th: Throwable) {
                        val cause = th.cause ?: th
                        if (cause is SecurityException) {
                            logD(TAG) {
                                "patch SecurityException -> UnknownHostException: ${cause.message}"
                            }
                            val uhe = UnknownHostException(cause.message)
                            uhe.initCause(cause)
                            throw uhe
                        }
                        throw th
                    }
                },
                Hooks.EntryPointType.DIRECT
            )

            logD(TAG) { "InetExceptionPatchHook installed" }
        } catch (th: Throwable) {
            logE(TAG, th) { "failed to install hook" }
        }
    }
}

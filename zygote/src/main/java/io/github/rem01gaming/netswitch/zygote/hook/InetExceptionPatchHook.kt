package io.github.rem01gaming.netswitch.zygote.hook

import com.v7878.unsafe.invoke.Transformers
import com.v7878.vmtools.Hooks
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logD
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logE
import java.lang.reflect.Method
import java.net.UnknownHostException

/**
 * 在 App 进程内把 libcore 抛出的 SecurityException("Permission denied (missing INTERNET permission?)")
 * 转成 UnknownHostException (IOException 子类)。
 *
 * 背景：剥掉 INET_GID(3003) 后，getaddrinfo 返回 EPERM/EACCES，libcore 的
 * Inet6AddressImpl.lookupHostByName 硬编码把它包装成 SecurityException(RuntimeException)，
 * okhttp 等库不 catch 该类型，导致后台线程崩溃。
 *
 * 方法签名（AOSP 11~15 通用）：
 *   private static InetAddress[] lookupHostByName(String host, int netId)
 * 早期代码误写成单参 String，导致 NoSuchMethodException。
 */
object InetExceptionPatchHook {

    private const val TAG = "InetExceptionPatch"

    fun load() {
        val method = findLookupHostByName()
        if (method == null) {
            logE(TAG, null) { "lookupHostByName not found on this ROM, skip" }
            return
        }

        try {
            method.isAccessible = true
            Hooks.hook(
                method,
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
            logD(TAG) { "InetExceptionPatchHook installed via ${method.toGenericString()}" }
        } catch (th: Throwable) {
            logE(TAG, th) { "failed to install hook" }
        }
    }

    /**
     * 兼容不同 ROM/版本：
     *  - AOSP 11+  : lookupHostByName(String, int)
     *  - 极少数旧版 : lookupHostByName(String)
     * 也考虑偶尔出现的方法名变体。
     */
    private fun findLookupHostByName(): Method? {
        val clazz = try {
            Class.forName("java.net.Inet6AddressImpl")
        } catch (th: Throwable) {
            logE(TAG, th) { "Inet6AddressImpl not found" }
            return null
        }

        // 优先：Android 11+ 双参
        runCatching {
            clazz.getDeclaredMethod(
                "lookupHostByName",
                String::class.java,
                Int::class.javaPrimitiveType,
            )
        }.getOrNull()?.let { return it }

        // 回退：单参
        runCatching {
            clazz.getDeclaredMethod("lookupHostByName", String::class.java)
        }.getOrNull()?.let { return it }

        // 兜底：扫描所有方法名含 lookupHostByName 的
        return clazz.declaredMethods.firstOrNull { it.name == "lookupHostByName" }
    }
}

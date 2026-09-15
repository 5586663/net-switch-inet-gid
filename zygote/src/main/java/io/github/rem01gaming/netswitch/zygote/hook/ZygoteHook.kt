package io.github.rem01gaming.netswitch.zygote.hook

import android.os.Build
import com.v7878.unsafe.invoke.EmulatedStackFrame
import com.v7878.unsafe.invoke.Transformers
import com.v7878.vmtools.Hooks
import io.github.rem01gaming.netswitch.zygote.util.CollectionUtils.lastOrNullWithType
import io.github.rem01gaming.netswitch.zygote.util.Constants
import io.github.rem01gaming.netswitch.zygote.util.IsolatedList
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logD
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logE
import io.github.rem01gaming.netswitch.zygote.util.Logcat.logI
import io.github.rem01gaming.netswitch.zygote.util.ZLUtils.argTypes
import io.github.rem01gaming.netswitch.zygote.util.ZLUtils.args
import io.github.rem01gaming.netswitch.zygote.util.ZLUtils.getArgument
import io.github.rem01gaming.netswitch.zygote.util.ZLUtils.setArgument
import io.github.rem01gaming.netswitch.zygote.util.ZygoteConstants.NATIVE_ZYGOTE_PROCESS_CLASS
import io.github.rem01gaming.netswitch.zygote.util.ZygoteConstants.ZYGOTE_PROCESS_CLASS

/**
 * 在 system_server 内 hook ZygoteProcess.start，按 net-switch 的 isolated.json
 * 名单，在 App 进程 fork 之前从 gids 参数里剔除 INET_GID(3003)。
 *
 * 与 iptables 层联动：
 *  - 本 hook 拦在「创建进程」阶段，效果是该进程从出生就没有 INTERNET 权限组；
 *  - iptables 拦在 connect() 阶段，覆盖 native 直连 IP 的漏网场景。
 *
 * 名单唯一真源：/data/adb/.config/net-switch/isolated.json
 * 由 net-switch 的 netswitch CLI 与 WebUI 写入。
 *
 * 来源：移植自 HMA-OSS ZygoteHook.kt，去掉 HMAService/JsonConfig/PMS hook，
 * 名单改为读 isolated.json。
 */
class ZygoteHook {

    private companion object {
        const val TAG = "ZygoteHook"
        /** Android 16 (API 36) 起 zygote 走 NativeZygoteProcess */
        const val SDK_ANDROID_16 = 36
    }

    fun load() {
        hookZygoteStart(ZYGOTE_PROCESS_CLASS)
        if (Build.VERSION.SDK_INT >= SDK_ANDROID_16) {
            hookZygoteStart(NATIVE_ZYGOTE_PROCESS_CLASS)
        }
    }

    private fun hookZygoteStart(className: String) {
        val clazz = try {
            Class.forName(className)
        } catch (th: Throwable) {
            logD(TAG) { "Class $className not found, skip" }
            return
        }

        val startMethods = clazz.declaredMethods.filter { it.name == "start" }
        if (startMethods.isEmpty()) {
            logD(TAG) { "No start() found in $className, skip" }
            return
        }

        for (method in startMethods) {
            method.isAccessible = true
            try {
                Hooks.hook(
                    method,
                    Hooks.EntryPointType.DIRECT,
                    { original, frame ->
                        try {
                            hookIntoZygoteProcess(frame)
                        } catch (th: Throwable) {
                            logE(TAG, th) { "hookIntoZygoteProcess failed" }
                        }
                        Transformers.invokeExact(original, frame)
                    },
                    Hooks.EntryPointType.DIRECT
                )
                logI(TAG) { "Hooked $className.start(${method.parameterCount} args)" }
            } catch (th: Throwable) {
                logE(TAG, th) { "Failed to hook $className.start(${method.parameterCount} args)" }
            }
        }
    }

    private fun hookIntoZygoteProcess(frame: EmulatedStackFrame) {
        // caller 包名是最后一个 String 参数
        val caller = frame.args.lastOrNullWithType<String>() ?: return
        if (caller.isEmpty()) return

        // 白名单：名单里若混入系统包，直接跳过，避免系统 UI 崩溃
        if (caller in Constants.packagesShouldNotBlock) {
            return
        }

        // 只处理 isolated.json 名单内的包
        if (!IsolatedList.contains(caller)) return

        // 定位 gids 参数（第一个 IntArray），剔除 INET_GID
        val types = frame.argTypes
        for ((index, clazz) in types.withIndex()) {
            if (clazz != IntArray::class.java) continue

            val gids = frame.getArgument(index) as? IntArray ?: return
            if (Constants.INET_GID !in gids) return

            val filtered = gids.filter { it != Constants.INET_GID }.toIntArray()
            frame.setArgument(index, filtered)
            logI(TAG) {
                "Stripped INET_GID from $caller: ${gids.size} -> ${filtered.size} gids"
            }
            return
        }

        logD(TAG) { "No gids IntArray found for $caller, skip" }
    }
}

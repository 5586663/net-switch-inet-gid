package io.github.rem01gaming.netswitch.zygote;

import static io.github.rem01gaming.netswitch.zygote.util.Logcat.logELegacy;
import static io.github.rem01gaming.netswitch.zygote.util.Logcat.logILegacy;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;

import io.github.rem01gaming.netswitch.zygote.hook.InetExceptionPatchHook;
import io.github.rem01gaming.netswitch.zygote.hook.ZygoteHook;

/**
 * Zygisk 入口。
 *
 * 分叉策略：
 *  - "android"（system_server）：装 ZygoteHook，在 startZygoteProcess 时按
 *    isolated.json 剔除 GID 3003(INET_GID)。
 *  - 其他 App 进程：装 InetExceptionPatchHook，把 libcore 抛的
 *    SecurityException 改写成 UnknownHostException，避免剥组后崩溃。
 *
 * 名单来源为 net-switch 的 /data/adb/.config/net-switch/isolated.json，
 * 与 iptables 层共用同一份配置。
 */
@SuppressWarnings("all")
@DoNotObfuscateType
@DoNotShrinkType
public class ZygoteEntry {
    public static final String TAG = "ZygoteEntry";

    @DoNotObfuscate
    @DoNotShrink
    public static void premain() throws Throwable {

    }

    @DoNotObfuscate
    @DoNotShrink
    public static void main() throws Throwable {
        final String injected = ZygoteLoader.getPackageName();
        logILegacy(TAG, "Injected into " + injected);

        if ("android".equals(injected)) {
            try {
                new ZygoteHook().load();
                logILegacy(TAG, "ZygoteHook loaded");
            } catch (Throwable th) {
                logELegacy(TAG, "An exception occurred while ZygoteHook init", th);
            }
            return;
        }

        try {
            InetExceptionPatchHook.INSTANCE.load();
            logILegacy(TAG, "InetExceptionPatchHook loaded for " + injected);
        } catch (Throwable th) {
            logELegacy(TAG, "An exception occurred while InetExceptionPatchHook init", th);
        }
    }
}

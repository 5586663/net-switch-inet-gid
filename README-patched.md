# net-switch + INET_GID 融合版（方案 A）

生成时间：2026-09-15（设备本地时间）

## 目标

把 net-switch 的 iptables 拦截，与 HMA-OSS 的 INET_GID 剔除合并成**一个模块**，
并修复剥组后的 App 闪退。两层拦截共用同一份名单：

    /data/adb/.config/net-switch/isolated.json

## 架构

    system_server
      └─ ZygoteHook         读 isolated.json，fork App 前从 gids 剔除 3003(INET_GID)
    App 进程
      └─ InetExceptionPatchHook  把 libcore 抛的 SecurityException 改写成
                                 UnknownHostException（IOException 子类，okhttp 可 catch）
    iptables（service.sh）
      └─ 按 uid REJECT         拦 connect()，覆盖 native 直连 IP 的场景

## 与 HMA-OSS 原版的差异

砍掉的部分：HMAService / PMS hook / AMS hook / manager.apk / JsonConfig /
AppPresets / FilterHolder / IResukyPackageUtils 等（约 90% common 代码）。

保留的部分：
- ZygoteHook 的 GID 剔除逻辑（改为读 isolated.json）
- 你的 InetExceptionPatchHook 闪退补丁
- ZLUtils / CollectionUtils / Logcat 的精简子集

## 关键文件

| 文件 | 作用 |
|---|---|
| `zygote/src/main/java/.../ZygoteEntry.java` | 分叉入口 |
| `zygote/src/main/java/.../hook/ZygoteHook.kt` | GID 剔除 |
| `zygote/src/main/java/.../hook/InetExceptionPatchHook.kt` | 闪退修复 |
| `zygote/src/main/java/.../util/IsolatedList.kt` | 读 net-switch 名单 |
| `zygote/src/main/java/.../util/Constants.kt` | INET_GID=3003 + 系统包白名单 |

## 编译（必须 CI，本地无 JDK/SDK）

    git submodule update --init --recursive
    ./gradlew :zygote:assembleRelease

`external/AndroidVMTools` 是必填 submodule，空目录会导致编译失败。

## 安全边界

- **GID 剔除只覆盖 Java 层 DNS**：native 直连 IP 或 connect() 不经 libcore，
  必须靠 iptables 兜底。两层都开时，App 解析域名会先报错，
  直连 IP 才轮到 iptables —— 排查时看 logcat tag 区分：
  `net-switch-zygote`（GID 层）、`net-switch: blocked`（iptables 层，写 /dev/kmsg）。
- **注入面是全部 App 进程**：Zygisk `PACKAGE_APP` 编译期写死，名单只能做行为级
  过滤（本实现里 App 侧无脑装补丁，GID 剔除在 system_server 侧按名单判断）。
- **系统包白名单**：`Constants.packagesShouldNotBlock` 含 android / systemui /
  permissioncontroller 等，名单里混入这些包会被跳过，避免系统 UI 崩溃。

## 回滚

- 卸载模块重启 → 立即恢复（GID 与 iptables 同时消失）
- 若只想停 GID 层：清空 isolated.json 后重启

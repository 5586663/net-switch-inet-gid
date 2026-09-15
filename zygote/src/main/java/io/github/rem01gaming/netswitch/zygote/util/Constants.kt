package io.github.rem01gaming.netswitch.zygote.util

/**
 * 精简自 HMA-OSS common/Constants.kt。
 *
 * GID_PAIRS：ZygoteHook 剔除 GID 前先过滤，避免误伤。
 * packagesShouldNotBlock：名单里若混入这些包，直接跳过——GID 剔除发生在
 * 进程创建期，系统核心包被剔会导致无法开机/UI 崩溃。
 *
 * 白名单取 HMA-OSS 原版全集（12 项），不缩减。
 */
@Suppress("SpellCheckingInspection")
object Constants {
    const val SDCARD_RW_GID: Int = 1015
    const val MEDIA_RW_GID: Int = 1023
    const val PACKAGE_INFO_GID: Int = 1032
    const val EXTERNAL_STORAGE_GID: Int = 1077
    const val EXT_DATA_RW_GID: Int = 1078
    const val EXT_OBB_RW_GID: Int = 1079

    /** 对应 AID_INET，剥掉后 App 进程失去 INTERNET 权限组 */
    const val INET_GID: Int = 3003

    const val SHARED_USER_GID: Int = 9997
    const val APP_ZYGOTE_GID: Int = 3009

    val GID_PAIRS: Map<String, Int> = mapOf(
        "SDCARD_RW_GID" to SDCARD_RW_GID,
        "MEDIA_RW_GID" to MEDIA_RW_GID,
        "PACKAGE_INFO_GID" to PACKAGE_INFO_GID,
        "EXTERNAL_STORAGE_GID" to EXTERNAL_STORAGE_GID,
        "EXT_DATA_RW_GID" to EXT_DATA_RW_GID,
        "EXT_OBB_RW_GID" to EXT_OBB_RW_GID,
        "INET_GID" to INET_GID,
        "SHARED_USER_GID" to SHARED_USER_GID,
        "APP_ZYGOTE_GID" to APP_ZYGOTE_GID,
    )

    /**
     * 名单内若混入这些包，GID 剔除直接跳过。
     *
     * 取自 HMA-OSS 原版 packagesShouldNotHide 全集 + 额外补充
     * 常见系统服务（bluetooth / nfc / phone / telephony），
     * 这些包一旦被剥夺 INTERNET，会导致开机失败或系统服务循环崩溃。
     */
    val packagesShouldNotBlock: Set<String> = setOf(
        "android",
        "android.media",
        "android.uid.system",
        "android.uid.shell",
        "android.uid.systemui",
        "com.android.permissioncontroller",
        "com.android.providers.downloads",
        "com.android.providers.downloads.ui",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.settings",
        "com.google.android.providers.media.module",
        "com.google.android.permissioncontroller",
        // 额外补充：系统通信/设置核心，被剔 GID 会崩
        "com.android.settings",
        "com.android.phone",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.providers.telephony",
        "com.android.providers.contacts",
        "com.android.server.telecom",
        "com.android.networkstack",
        "com.android.networkstack.tethering",
        "com.android.connectivity.resources",
        "com.android.cellbroadcastreceiver",
        "com.android.cellbroadcastservice",
    )
}

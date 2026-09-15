package io.github.rem01gaming.netswitch.zygote.util

/**
 * 精简自 HMA-OSS common/Constants.kt，只保留 GID 校验用到的部分。
 *
 * GID_PAIRS 的用途：ZygoteHook 在剔除 GID 之前，先用它过滤掉
 * 不在名单里的 GID 值，避免误伤。
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

    /** 名单内若混入这些包，直接跳过，避免系统 UI 崩溃 */
    val packagesShouldNotBlock: Set<String> = setOf(
        "android",
        "android.media",
        "android.uid.system",
        "android.uid.shell",
        "android.uid.systemui",
        "com.android.permissioncontroller",
    )
}

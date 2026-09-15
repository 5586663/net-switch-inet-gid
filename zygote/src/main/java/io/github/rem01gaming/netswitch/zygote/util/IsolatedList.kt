package io.github.rem01gaming.netswitch.zygote.util

import java.io.File

/**
 * 读取 net-switch 的 isolated.json 名单。
 *
 * 路径优先级：
 *  1. /data/system/net-switch/isolated.json  ← system_server 可读（system_data_file context）
 *  2. /data/adb/net-switch/isolated.json     ← 数据源，仅 root 可读，system_server 读不到
 *
 * 说明：/data/adb 是 u:object_r:adb_data_file:s0 + 700 root，system_server
 * （uid 1000，u:r:system_server:s0）无 DAC 也无 MAC 权限。故 service.sh
 * 以 root 身份把名单同步到 /data/system/net-switch/，本类读同步后的副本。
 *
 * 缓存：按 lastModified 失效，service.sh 每 10 秒同步，改动自动生效。
 */
object IsolatedList {
    private const val PATH_PRIMARY = "/data/system/net-switch/isolated.json"
    private const val PATH_FALLBACK = "/data/adb/net-switch/isolated.json"

    @Volatile private var cachedSet: Set<String>? = null
    @Volatile private var cachedMtime: Long = -1L
    @Volatile private var cachedPath: String = PATH_PRIMARY

    fun contains(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        return load().contains(pkg)
    }

    fun reload() {
        cachedSet = null
        cachedMtime = -1L
    }

    @Synchronized
    private fun load(): Set<String> {
        val file = pickFile()
        val path = file.absolutePath
        val mtime = try { file.lastModified() } catch (_: Throwable) { 0L }

        val cached = cachedSet
        if (cached != null && mtime == cachedMtime && path == cachedPath) return cached

        val parsed = parse(file)
        cachedSet = parsed
        cachedMtime = mtime
        cachedPath = path
        return parsed
    }

    private fun pickFile(): File {
        val primary = File(PATH_PRIMARY)
        if (primary.exists()) return primary
        val fallback = File(PATH_FALLBACK)
        if (fallback.exists()) return fallback
        return primary
    }

    private fun parse(file: File): Set<String> {
        return try {
            if (!file.exists()) return emptySet()
            file.readText()
                .trim()
                .removeSurrounding("[", "]")
                .split(",")
                .asSequence()
                .map { it.trim().trim('"').trim('\'') }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (_: Throwable) {
            emptySet()
        }
    }
}

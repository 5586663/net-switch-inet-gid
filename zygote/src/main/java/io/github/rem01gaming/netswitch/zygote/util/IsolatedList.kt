package io.github.rem01gaming.netswitch.zygote.util

import java.io.File

/**
 * 读取 net-switch 的 isolated.json 名单。
 *
 * 路径统一为 /data/adb/net-switch/isolated.json —— 与设备上现装
 * net-switch v1.2 的 service.sh、netswitch CLI、WebUI 全部一致。
 *
 * 兼容：若旧路径不存在但 v1.3 新路径存在，回退读新路径。
 * 缓存：按 lastModified 失效，名单改动后自动重读。
 */
object IsolatedList {
    private const val PATH_PRIMARY = "/data/adb/net-switch/isolated.json"
    private const val PATH_FALLBACK = "/data/adb/.config/net-switch/isolated.json"

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

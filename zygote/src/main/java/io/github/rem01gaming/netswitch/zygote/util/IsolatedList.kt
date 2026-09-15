package io.github.rem01gaming.netswitch.zygote.util

import java.io.File

/**
 * 读取 net-switch 的 isolated.json 名单。
 *
 * 文件路径与 net-switch 模块共享：/data/adb/.config/net-switch/isolated.json
 * 文件格式：[ "pkg.a", "pkg.b" ]（net-switch 的 write_json 输出）
 *
 * 缓存策略：按 lastModified 判断失效，名单被 WebUI/CLI 改动后自动重读。
 * 名字为 .all 的标记文件不参与；只认包名字符串。
 */
object IsolatedList {
    private const val PATH = "/data/adb/.config/net-switch/isolated.json"

    @Volatile private var cachedSet: Set<String>? = null
    @Volatile private var cachedMtime: Long = -1L

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
        val file = File(PATH)
        val mtime = try { file.lastModified() } catch (_: Throwable) { 0L }

        val cached = cachedSet
        if (cached != null && mtime == cachedMtime) return cached

        val parsed = parse(file)
        cachedSet = parsed
        cachedMtime = mtime
        return parsed
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

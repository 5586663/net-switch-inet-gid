package io.github.rem01gaming.netswitch.zygote.util

/**
 * 精简自 HMA-OSS CollectionUtils.kt。
 *
 * 只保留 ZygoteHook.getForceMountArgs 用到的两个类型过滤扩展。
 */
object CollectionUtils {

    inline fun <reified T> Array<*>.firstOrNullWithType(): T? {
        return this.firstOrNull { it is T } as? T
    }

    inline fun <reified T> Array<*>.lastOrNullWithType(): T? {
        return this.lastOrNull { it is T } as? T
    }
}

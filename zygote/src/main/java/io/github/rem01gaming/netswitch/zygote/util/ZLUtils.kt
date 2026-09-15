package io.github.rem01gaming.netswitch.zygote.util

import com.v7878.unsafe.invoke.EmulatedStackFrame

/**
 * 精简自 HMA-OSS ZLUtils.kt。
 *
 * 只保留 ZygoteHook 用到的部分：args / argTypes / getArgument / setArgument / shortyEquals。
 * 去掉了 SystemServerHook.classLoader 依赖（本模块没有 PMS hook）。
 */
object ZLUtils {

    /** args[0] 是 thisObject，args[1:] 是函数参数 */
    val EmulatedStackFrame.args: Array<Any?> get() = dumpArgs()

    /** argTypes[0] 是 thisObject 类型，argTypes[1:] 是参数类型 */
    val EmulatedStackFrame.argTypes: Array<Class<*>> get() = dumpArgTypes()

    internal fun EmulatedStackFrame.dumpArgs(): Array<Any?> {
        return mutableListOf<Any?>().let {
            for (index in 0 until type().parameterCount()) {
                it.add(getArgument(index))
            }
            it.toTypedArray()
        }
    }

    internal fun EmulatedStackFrame.dumpArgTypes(): Array<Class<*>> {
        return mutableListOf<Class<*>>().let {
            for (index in 0 until type().parameterCount()) {
                it.add(getArgumentType(index))
            }
            it.toTypedArray()
        }
    }

    fun EmulatedStackFrame.getArgumentType(index: Int): Class<*> {
        return accessor().getArgumentType(index)
    }

    fun EmulatedStackFrame.getArgument(index: Int): Any {
        val accessor = accessor()
        return when (accessor.getArgumentShorty(index)) {
            'L' -> accessor.getReference(index)
            'Z' -> accessor.getBoolean(index)
            'B' -> accessor.getByte(index)
            'C' -> accessor.getChar(index)
            'S' -> accessor.getShort(index)
            'I' -> accessor.getInt(index)
            'J' -> accessor.getLong(index)
            'F' -> accessor.getFloat(index)
            'D' -> accessor.getDouble(index)
            else -> throw Exception("Should not reach here")
        }
    }

    fun EmulatedStackFrame.setArgument(index: Int, value: Any) {
        val accessor = accessor()
        when (accessor.getArgumentShorty(index)) {
            'L' -> accessor.setReference(index, value)
            'Z' -> accessor.setBoolean(index, value as Boolean)
            'B' -> accessor.setByte(index, value as Byte)
            'C' -> accessor.setChar(index, value as Char)
            'S' -> accessor.setShort(index, value as Short)
            'I' -> accessor.setInt(index, value as Int)
            'J' -> accessor.setLong(index, value as Long)
            'F' -> accessor.setFloat(index, value as Float)
            'D' -> accessor.setDouble(index, value as Double)
            else -> throw Exception("Should not reach here")
        }
    }

    fun EmulatedStackFrame.shortyEquals(index: Int, shorty: Char): Boolean {
        return accessor().getArgumentShorty(index) == shorty
    }
}

package com.ivieleague.smbtranslation.utils

/** Abstraction for a readable/writable byte register used by delegates. */
interface ByteAccess { var value: Byte }
interface ByteArrayAccess {
    operator fun get(index: Int): Byte
    operator fun set(index: Int, value: Byte)
}

fun ByteArray.access(range: IntRange): ByteArrayAccess = object : ByteArrayAccess {
    override fun get(index: Int): Byte = this@access[range.first + index]
    override fun set(index: Int, value: Byte) { this@access[range.first + index] = value }
}
fun ByteArray.access(index: Int): ByteAccess = object : ByteAccess {
    override var value: Byte
        get() = this@access[index]
        set(value) { this@access[index] = value }
}
package com.ivieleague.smbtranslation.utils

fun Byte.bit(index: Int): Boolean = (this.toInt() and (1 shl index)) != 0

fun Byte.bit(index: Int, value: Boolean): Byte {
    val mask = 1 shl index
    val result = if (value) {
        (this.toInt() or mask)
    } else {
        (this.toInt() and mask.inv())
    }
    return result.toByte()
}

fun Byte.bitRange(start: Int, endInclusive: Int): Byte {
    val width = endInclusive - start + 1
    val mask = (1 shl width) - 1
    return ((this.toInt() ushr start) and mask).toByte()
}

fun Byte.bitRange(start: Int, endInclusive: Int, value: Byte): Byte {
    val width = endInclusive - start + 1
    val valueMask = (1 shl width) - 1
    val rangeMask = valueMask shl start
    val valueShifted = (value.toInt() and valueMask) shl start
    val cleared = (this.toInt() and 0xFF) and rangeMask.inv()
    return (cleared or valueShifted).toByte()
}


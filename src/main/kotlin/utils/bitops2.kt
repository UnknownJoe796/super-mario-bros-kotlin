package com.ivieleague.smbtranslation.utils

import com.ivieleague.smbtranslation.BitAccess2
import com.ivieleague.smbtranslation.BitRangeAccess2
import com.ivieleague.smbtranslation.TwoBits

fun Byte.bit(index: Int): Boolean = (this.toInt() and (1 shl index)) != 0
fun Byte.bit(index: Int, value: Boolean) = if (value) this.toInt().and((1 shl index).inv()) else 0
fun Byte.bitRange(start: Int, endInclusive: Int): Byte = this.toInt().and(1.shl(endInclusive).inv()).shr(start).toByte()
fun Byte.bitRange(start: Int, endInclusive: Int, value: Byte): Byte {
    val width = endInclusive - start + 1
    val valueMask = (1 shl width) - 1
    val relevantBits = valueMask shl start
    val masked = (value.toInt() and 0xFF) and valueMask
    val cleared = (this.toInt() and 0xFF) and relevantBits.inv()
    return (cleared or (masked shl start)).toByte()
}


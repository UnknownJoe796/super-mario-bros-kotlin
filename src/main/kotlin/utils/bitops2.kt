@file:OptIn(ExperimentalUnsignedTypes::class)

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
    val cleared = this.toInt() and rangeMask.inv()
    return (cleared or valueShifted).toByte()
}

typealias IndexingByte = UByte
typealias NumericalByte = Byte
typealias ScreenPositionByte = UByte
typealias DataBitsByte = Byte

infix fun Byte.bytePlus(other: Byte): Byte = ((this) + (other)).toByte()
infix fun Byte.shr(other: Byte): Byte = (this.toInt() shr other.toInt()).toByte()
infix fun Byte.ushr(other: Byte): Byte = (this.toInt() ushr other.toInt()).toByte()
infix fun Byte.shr(other: Int): Byte = (this.toInt() shr other).toByte()
infix fun Byte.ushr(other: Int): Byte = (this.toInt() ushr other).toByte()
infix fun Byte.shl(other: Byte): Byte = (this.toInt() shl other.toInt()).toByte()
infix fun Byte.shl(other: Int): Byte = (this.toInt() shl other).toByte()
operator fun ByteArray.get(index: Byte): Byte = this[index.toUByte().toInt()]
operator fun ByteArray.set(index: Byte, value: Byte) { this[index.toUByte().toInt()] = value }
operator fun UByteArray.get(index: Byte): UByte = this[index.toUByte().toInt()]
operator fun UByteArray.set(index: Byte, value: UByte) { this[index.toUByte().toInt()] = value }
operator fun IntArray.get(index: Byte): Int = this[index.toUByte().toInt()]
operator fun IntArray.set(index: Byte, value: Int) { this[index.toUByte().toInt()] = value }
operator fun UIntArray.get(index: Byte): UInt = this[index.toUByte().toInt()]
operator fun UIntArray.set(index: Byte, value: UInt) { this[index.toUByte().toInt()] = value }
operator fun ShortArray.get(index: Byte): Short = this[index.toUByte().toInt()]
operator fun ShortArray.set(index: Byte, value: Short) { this[index.toUByte().toInt()] = value }
operator fun UShortArray.get(index: Byte): UShort = this[index.toUByte().toInt()]
operator fun UShortArray.set(index: Byte, value: UShort) { this[index.toUByte().toInt()] = value }
operator fun <T> Array<T>.get(index: Byte): T = this[index.toUByte().toInt()]
operator fun <T> Array<T>.set(index: Byte, value: T) { this[index.toUByte().toInt()] = value }
operator fun <T> List<T>.get(index: Byte): T = this[index.toUByte().toInt()]
operator fun <T> MutableList<T>.set(index: Byte, value: T) { this[index.toUByte().toInt()] = value }

infix fun UByte.bytePlus(other: UByte): UByte = ((this) + (other)).toUByte()
infix fun UByte.shr(other: UByte): UByte = (this.toInt() ushr other.toInt()).toUByte()
infix fun UByte.shr(other: Int): UByte = (this.toInt() ushr other).toUByte()
infix fun UByte.shl(other: UByte): UByte = (this.toInt() shl other.toInt()).toUByte()
infix fun UByte.shl(other: Int): UByte = (this.toInt() shl other).toUByte()
operator fun ByteArray.get(index: UByte): Byte = this[index.toInt()]
operator fun ByteArray.set(index: UByte, value: Byte) { this[index.toInt()] = value }
operator fun UByteArray.get(index: UByte): UByte = this[index.toInt()]
operator fun UByteArray.set(index: UByte, value: UByte) { this[index.toInt()] = value }
operator fun IntArray.get(index: UByte): Int = this[index.toInt()]
operator fun IntArray.set(index: UByte, value: Int) { this[index.toInt()] = value }
operator fun UIntArray.get(index: UByte): UInt = this[index.toInt()]
operator fun UIntArray.set(index: UByte, value: UInt) { this[index.toInt()] = value }
operator fun ShortArray.get(index: UByte): Short = this[index.toInt()]
operator fun ShortArray.set(index: UByte, value: Short) { this[index.toInt()] = value }
operator fun UShortArray.get(index: UByte): UShort = this[index.toInt()]
operator fun UShortArray.set(index: UByte, value: UShort) { this[index.toInt()] = value }
operator fun <T> Array<T>.get(index: UByte): T = this[index.toInt()]
operator fun <T> List<T>.get(index: UByte): T = this[index.toInt()]
operator fun <T> MutableList<T>.set(index: UByte, value: T) { this[index.toInt()] = value }

operator fun ByteArrayAccess.get(index: Byte): Byte = this[index.toInt()]
operator fun ByteArrayAccess.set(index: Byte, value: Byte) { this[index.toInt()] = value }


fun test() {
    val x = 0.toByte()
    val y = 1.toByte()
    val z = x bytePlus y
}

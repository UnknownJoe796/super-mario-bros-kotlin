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

infix fun Byte.bytePlus(other: Byte): Byte = ((this.toInt() and 0xFF) + (other.toInt() and 0xFF)).toByte()


/**
 * Proposed replacement to using Byte - enables modders to later use larger numbers without trouble.
 */
@JvmInline
value class NesNumber(val int: Int): Comparable<NesNumber> {
    companion object {
        val signedRange = Byte.MIN_VALUE.toInt()..Byte.MAX_VALUE.toInt()
        val unsignedRange = UByte.MIN_VALUE.toInt()..UByte.MAX_VALUE.toInt()
    }
    private fun assertInSignedRange(): NesNumber {
        if (int !in signedRange) throw IllegalArgumentException("Expected result to stay in signed range")
        return this
    }
    private fun assertInUnsignedRange(): NesNumber {
        if (int !in unsignedRange) throw IllegalArgumentException("Expected result to stay in signed range")
        return this
    }
    fun coerceInSignedRange(): NesNumber = NesNumber(int.coerceIn(signedRange))
    fun coerceInUnsignedRange(): NesNumber = NesNumber(int.coerceIn(unsignedRange))
    operator fun plus(other: NesNumber) = NesNumber(this.int.plus(other.int))
    operator fun minus(other: NesNumber) = NesNumber(this.int.minus(other.int))
    operator fun times(other: NesNumber) = NesNumber(this.int.times(other.int))
    operator fun div(other: NesNumber) = NesNumber(this.int.div(other.int))
    operator fun rem(other: NesNumber) = NesNumber(this.int.rem(other.int))
    override fun compareTo(other: NesNumber): Int = this.int.compareTo(other.int)
    operator fun inc() = NesNumber(this.int.inc())
    operator fun dec() = NesNumber(this.int.dec())
    operator fun unaryMinus() = NesNumber(this.int.unaryMinus())
    infix fun and(other: NesNumber) = NesNumber(this.int and other.int)
    infix fun or(other: NesNumber) = NesNumber(this.int or other.int)
    infix fun xor(other: NesNumber) = NesNumber(this.int xor other.int)
    infix fun shl(other: NesNumber) = NesNumber(this.int shl other.int)
    infix fun shr(other: NesNumber) = NesNumber(this.int shr other.int)
    fun toByte(): Byte = int.toByte()
    fun toUByte(): UByte = int.toUByte()
    override fun toString(): String = "0x" + int.toString(16)

    fun addWithCarry(other: NesNumber, carry: Boolean): Pair<NesNumber, Boolean> {
        val sum = this + other + (if(carry) NesNumber(1) else NesNumber(0))
        val carry = sum.int !in signedRange
        return sum.coerceInSignedRange() to carry
    }
    fun subtractWithCarry(other: NesNumber, carry: Boolean): Pair<NesNumber, Boolean> {
        val sum = this - other - (if(carry) NesNumber(0) else NesNumber(-1))
        val carry = sum.int !in signedRange
        return sum.coerceInSignedRange() to carry
    }
}

operator fun <T> List<T>.get(index: NesNumber) = this[index.int]
operator fun <T> MutableList<T>.set(index: NesNumber, value: T) { this[index.int] = value }

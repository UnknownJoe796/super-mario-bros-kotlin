package com.ivieleague.smbtranslation.utils

import com.ivieleague.smbtranslation.GameRam

/**
 * Converts between a Kotlin type and its NES RAM byte representation.
 * Implement directly when conversion needs GameRam context (e.g. variant-aware enums).
 */
interface RamConverter<T> {
    fun toByte(value: T, ram: GameRam): Byte
    fun fromByte(byte: Byte, ram: GameRam): T
}

/**
 * RamConverter for types whose byte representation doesn't depend on GameRam context.
 */
abstract class SimpleRamConverter<T> : RamConverter<T> {
    abstract fun toByte(value: T): Byte
    abstract fun fromByte(byte: Byte): T
    override fun toByte(value: T, ram: GameRam): Byte = toByte(value)
    override fun fromByte(byte: Byte, ram: GameRam): T = fromByte(byte)
}

/** Identity converter for raw Byte fields. */
object ByteRamConverter : SimpleRamConverter<Byte>() {
    override fun toByte(value: Byte): Byte = value
    override fun fromByte(byte: Byte): Byte = byte
}

/** Converter for UByte fields. */
object UByteRamConverter : SimpleRamConverter<UByte>() {
    override fun toByte(value: UByte): Byte = value.toByte()
    override fun fromByte(byte: Byte): UByte = byte.toUByte()
}

/** Converter for Boolean fields (nonzero = true). */
object BooleanRamConverter : SimpleRamConverter<Boolean>() {
    override fun toByte(value: Boolean): Byte = if (value) 1 else 0
    override fun fromByte(byte: Byte): Boolean = byte != 0.toByte()
}

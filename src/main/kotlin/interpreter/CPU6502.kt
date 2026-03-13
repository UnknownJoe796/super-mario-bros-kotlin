// Ported from decompiler-6502-kotlin by Claude
package com.ivieleague.smbtranslation.interpreter

/**
 * Represents the 6502 CPU state including registers and flags.
 */
class CPU6502 {
    // Registers
    var A: UByte = 0u        // Accumulator
    var X: UByte = 0u        // X Index Register
    var Y: UByte = 0u        // Y Index Register
    var SP: UByte = 0xFFu    // Stack Pointer (points to $01xx)
    var PC: UShort = 0u      // Program Counter

    // Status Flags (stored in a single byte, but exposed individually)
    var N: Boolean = false   // Negative flag (bit 7)
    var V: Boolean = false   // Overflow flag (bit 6)
    var B: Boolean = false   // Break flag (bit 4)
    var D: Boolean = false   // Decimal mode flag (bit 3)
    var I: Boolean = false   // Interrupt disable flag (bit 2)
    var Z: Boolean = false   // Zero flag (bit 1)
    var C: Boolean = false   // Carry flag (bit 0)

    // Cycle counter for instruction timing
    var cycles: Long = 0L

    /**
     * Get the status register as a byte
     */
    fun getStatusByte(): UByte {
        var status = 0b00100000 // Bit 5 is always set
        if (N) status = status or 0b10000000
        if (V) status = status or 0b01000000
        if (B) status = status or 0b00010000
        if (D) status = status or 0b00001000
        if (I) status = status or 0b00000100
        if (Z) status = status or 0b00000010
        if (C) status = status or 0b00000001
        return status.toUByte()
    }

    /**
     * Set the status register from a byte
     */
    fun setStatusByte(value: UByte) {
        val v = value.toInt()
        N = (v and 0b10000000) != 0
        V = (v and 0b01000000) != 0
        B = (v and 0b00010000) != 0
        D = (v and 0b00001000) != 0
        I = (v and 0b00000100) != 0
        Z = (v and 0b00000010) != 0
        C = (v and 0b00000001) != 0
    }

    /**
     * Update the Zero and Negative flags based on a value
     */
    fun updateZN(value: UByte) {
        Z = value == 0.toUByte()
        N = (value.toInt() and 0x80) != 0
    }

    /**
     * Reset the CPU to initial state
     */
    fun reset() {
        A = 0u
        X = 0u
        Y = 0u
        SP = 0xFFu
        PC = 0u
        N = false
        V = false
        B = false
        D = false
        I = true  // Interrupts disabled on reset
        Z = false
        C = false
        cycles = 0L
    }

    /**
     * Get a string representation of the CPU state for debugging
     */
    override fun toString(): String {
        return buildString {
            append("PC=$${PC.toString(16).padStart(4, '0').uppercase()} ")
            append("A=$${A.toString(16).padStart(2, '0').uppercase()} ")
            append("X=$${X.toString(16).padStart(2, '0').uppercase()} ")
            append("Y=$${Y.toString(16).padStart(2, '0').uppercase()} ")
            append("SP=$${SP.toString(16).padStart(2, '0').uppercase()} ")
            append("Flags=[")
            append(if (N) "N" else "-")
            append(if (V) "V" else "-")
            append("-")  // Bit 5 (unused)
            append(if (B) "B" else "-")
            append(if (D) "D" else "-")
            append(if (I) "I" else "-")
            append(if (Z) "Z" else "-")
            append(if (C) "C" else "-")
            append("]")
        }
    }

    companion object {
        const val STACK_BASE = 0x0100
    }
}

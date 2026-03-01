// Ported from decompiler-6502-kotlin by Claude
package com.ivieleague.smbtranslation.interpreter

/**
 * Represents 64KB of memory for the 6502 processor.
 * Address space: $0000 - $FFFF
 */
class Memory6502 {
    private val data = ByteArray(0x10000) // 64KB

    /**
     * Read a byte from memory at the specified address
     */
    fun readByte(address: Int): UByte {
        return data[address and 0xFFFF].toUByte()
    }

    /**
     * Write a byte to memory at the specified address
     */
    fun writeByte(address: Int, value: UByte) {
        data[address and 0xFFFF] = value.toByte()
    }

    /**
     * Read a 16-bit word (little-endian) from memory
     */
    fun readWord(address: Int): UShort {
        val lo = readByte(address).toInt()
        val hi = readByte(address + 1).toInt()
        return ((hi shl 8) or lo).toUShort()
    }

    /**
     * Write a 16-bit word (little-endian) to memory
     */
    fun writeWord(address: Int, value: UShort) {
        writeByte(address, (value.toInt() and 0xFF).toUByte())
        writeByte(address + 1, ((value.toInt() shr 8) and 0xFF).toUByte())
    }

    /**
     * Load a program into memory at the specified address
     */
    fun loadProgram(startAddress: Int, program: ByteArray) {
        program.forEachIndexed { index, byte ->
            data[(startAddress + index) and 0xFFFF] = byte
        }
    }

    /**
     * Load a program into memory at the specified address
     */
    fun loadProgram(startAddress: Int, program: List<UByte>) {
        program.forEachIndexed { index, byte ->
            data[(startAddress + index) and 0xFFFF] = byte.toByte()
        }
    }

    /**
     * Reset RAM to power-on state.
     * Only RAM ($0000-$07FF) is initialized.
     * ROM and other memory areas are left untouched.
     *
     * Note: Real NES hardware has undefined/random RAM on power-on.
     * FCEUX uses a specific pattern: alternating 4 bytes of 0x00 and 4 bytes of 0xFF.
     * Pattern: 00 00 00 00 FF FF FF FF 00 00 00 00 FF FF FF FF ...
     * We match this exactly for TAS compatibility.
     */
    fun reset() {
        // Initialize RAM to match FCEUX's power-on pattern for TAS compatibility
        // Pattern: 4 bytes of 0x00, then 4 bytes of 0xFF, repeating
        // Do NOT touch ROM areas ($8000+) - they contain the loaded program
        for (i in 0 until 0x800) {
            // Every 8 bytes: first 4 are 0x00, next 4 are 0xFF
            data[i] = if ((i and 0x04) == 0) 0x00.toByte() else 0xFF.toByte()
        }
    }

    /**
     * Get a copy of memory for debugging/inspection
     */
    fun dump(startAddress: Int, length: Int): ByteArray {
        return data.copyOfRange(startAddress and 0xFFFF, (startAddress + length) and 0xFFFF)
    }
}

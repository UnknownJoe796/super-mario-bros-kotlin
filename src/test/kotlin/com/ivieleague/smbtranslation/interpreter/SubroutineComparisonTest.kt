// by Claude - Test harness for comparing interpreter execution against Kotlin translations
package com.ivieleague.smbtranslation.interpreter

import com.ivieleague.smbtranslation.GameRam
import com.ivieleague.smbtranslation.RamLocation
import com.ivieleague.smbtranslation.System
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

/**
 * Captures a snapshot of relevant RAM state for comparison.
 * Used to verify that a Kotlin translation produces the same output as the 6502 interpreter.
 */
data class RamSnapshot(
    val values: Map<Int, UByte>,
) {
    companion object {
        /** Addresses to monitor - covers zero page + game RAM area */
        val MONITORED_RANGES = listOf(
            0x00..0xFF,       // Zero page
            0x200..0x2FF,     // OAM/Sprite data
            0x300..0x3FF,     // VRAM buffers & misc
            0x400..0x4FF,     // Object data
            0x500..0x7FF,     // Block buffers, display data, sound
        )

        fun capture(memory: Memory6502): RamSnapshot {
            val values = mutableMapOf<Int, UByte>()
            for (range in MONITORED_RANGES) {
                for (addr in range) {
                    values[addr] = memory.readByte(addr)
                }
            }
            return RamSnapshot(values)
        }

        fun capture(ram: GameRam): RamSnapshot {
            val values = mutableMapOf<Int, UByte>()
            for (prop in GameRam::class.declaredMemberProperties) {
                val addr = prop.findAnnotation<RamLocation>()?.address ?: continue
                val value = prop.getter.call(ram)
                when (value) {
                    is Byte -> values[addr] = value.toUByte()
                    is UByte -> values[addr] = value
                    is Boolean -> values[addr] = if (value) 1u else 0u
                    is ByteArray -> {
                        for (i in value.indices) {
                            values[addr + i] = value[i].toUByte()
                        }
                    }
                    is UByteArray -> {
                        for (i in value.indices) {
                            values[addr + i] = value[i]
                        }
                    }
                    // Skip complex types like Stack, Sprite arrays, etc.
                }
            }
            return RamSnapshot(values)
        }
    }

    /** Compare this snapshot against another, returning differences */
    fun diff(other: RamSnapshot): List<RamDifference> {
        val diffs = mutableListOf<RamDifference>()
        val allAddresses = (values.keys + other.values.keys).sorted()
        for (addr in allAddresses) {
            val v1 = values[addr]
            val v2 = other.values[addr]
            if (v1 != v2) {
                diffs.add(RamDifference(addr, v1, v2))
            }
        }
        return diffs
    }
}

data class RamDifference(
    val address: Int,
    val interpreterValue: UByte?,
    val translationValue: UByte?,
) {
    override fun toString(): String {
        val addrStr = "$$${address.toString(16).padStart(4, '0')}"
        val intStr = interpreterValue?.toString(16)?.padStart(2, '0') ?: "??"
        val transStr = translationValue?.toString(16)?.padStart(2, '0') ?: "??"
        return "$addrStr: interpreter=$intStr translation=$transStr"
    }
}

/**
 * Utility to set up a comparison between the 6502 interpreter and a Kotlin translation.
 *
 * Usage:
 * ```
 * val comparison = SubroutineComparison.create("smb.nes")
 * comparison.setupRam { memory ->
 *     // Set up initial RAM state
 *     memory.writeByte(0x0770, 0x01u) // OperMode = Game
 * }
 * val interpreterResult = comparison.runInterpreter(subroutineAddress = 0x8000)
 * val translationResult = comparison.runTranslation { system ->
 *     system.start() // Run the Kotlin translation
 * }
 * val diffs = interpreterResult.diff(translationResult)
 * ```
 */
class SubroutineComparison private constructor(
    val interpreter: BinaryInterpreter6502,
    private val labelToAddress: Map<String, Int>,
) {
    companion object {
        /**
         * Create a comparison harness by loading the SMB ROM.
         * The ROM file should be in iNES format.
         */
        fun create(romPath: String): SubroutineComparison {
            val romFile = File(romPath)
            require(romFile.exists()) { "ROM file not found: $romPath" }

            val rom = NESLoader.load(romFile)
            val interpreter = BinaryInterpreter6502()
            NESLoader.loadIntoMemory(rom, interpreter.memory)

            // Parse assembly label addresses from smbdism.asm if available
            val labels = mutableMapOf<String, Int>()
            val asmFile = File("smbdism.asm")
            if (asmFile.exists()) {
                val definePattern = Regex("""^(\w+)\s*=\s*\$([0-9a-fA-F]+)""")
                for (line in asmFile.readLines()) {
                    val match = definePattern.matchEntire(line.trim()) ?: continue
                    labels[match.groupValues[1]] = match.groupValues[2].toInt(16)
                }
            }

            return SubroutineComparison(interpreter, labels)
        }

        /**
         * Create from raw ROM bytes (for testing without file I/O).
         */
        fun createFromBytes(romBytes: ByteArray): SubroutineComparison {
            val rom = NESLoader.load(romBytes)
            val interpreter = BinaryInterpreter6502()
            NESLoader.loadIntoMemory(rom, interpreter.memory)
            return SubroutineComparison(interpreter, emptyMap())
        }
    }

    /** Copy a snapshot of initial RAM state from the interpreter to a System's GameRam */
    fun syncRamToSystem(system: System) {
        for (prop in GameRam::class.declaredMemberProperties) {
            val addr = prop.findAnnotation<RamLocation>()?.address ?: continue
            val mutableProp = prop as? KMutableProperty1<GameRam, *> ?: continue
            when (val current = prop.getter.call(system.ram)) {
                is Byte -> {
                    @Suppress("UNCHECKED_CAST")
                    (mutableProp as KMutableProperty1<GameRam, Byte>).set(
                        system.ram, interpreter.memory.readByte(addr).toByte()
                    )
                }
                is UByte -> {
                    @Suppress("UNCHECKED_CAST")
                    (mutableProp as KMutableProperty1<GameRam, UByte>).set(
                        system.ram, interpreter.memory.readByte(addr)
                    )
                }
                is Boolean -> {
                    @Suppress("UNCHECKED_CAST")
                    (mutableProp as KMutableProperty1<GameRam, Boolean>).set(
                        system.ram, interpreter.memory.readByte(addr) != 0.toUByte()
                    )
                }
                // ByteArray and other complex types need special handling per-property
            }
        }
    }

    /** Run a subroutine in the interpreter starting at the given address */
    fun runInterpreterSubroutine(address: Int, maxCycles: Long = 100_000): RamSnapshot {
        // Push a sentinel return address so RTS will halt execution
        val sentinel = 0xFFF0
        interpreter.memory.writeByte(sentinel, 0x00u)  // BRK at sentinel
        // Push sentinel-1 on stack (RTS adds 1)
        val returnAddr = sentinel - 1
        interpreter.memory.writeByte(
            0x100 + interpreter.cpu.SP.toInt(),
            ((returnAddr shr 8) and 0xFF).toUByte()
        )
        interpreter.cpu.SP = (interpreter.cpu.SP.toInt() - 1 and 0xFF).toUByte()
        interpreter.memory.writeByte(
            0x100 + interpreter.cpu.SP.toInt(),
            (returnAddr and 0xFF).toUByte()
        )
        interpreter.cpu.SP = (interpreter.cpu.SP.toInt() - 1 and 0xFF).toUByte()

        // Set PC to the subroutine start
        interpreter.cpu.PC = address.toUShort()
        interpreter.halted = false

        // Run until BRK or maxCycles
        interpreter.run(maxCycles)

        return RamSnapshot.capture(interpreter.memory)
    }

    /** Run a subroutine by label name */
    fun runInterpreterSubroutine(label: String, maxCycles: Long = 100_000): RamSnapshot {
        val address = labelToAddress[label]
            ?: error("Label '$label' not found in assembly defines")
        return runInterpreterSubroutine(address, maxCycles)
    }

    /** Capture the current interpreter RAM state without running anything */
    fun captureInterpreterState(): RamSnapshot = RamSnapshot.capture(interpreter.memory)
}

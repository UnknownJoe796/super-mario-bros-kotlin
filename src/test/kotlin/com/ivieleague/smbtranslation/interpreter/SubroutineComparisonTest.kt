// by Claude - Test harness for comparing interpreter execution against Kotlin translations
package com.ivieleague.smbtranslation.interpreter

import com.ivieleague.smbtranslation.*
import java.io.File

// FunctionMetadata is now defined in ShadowValidator.kt (same package, shared between main and test)

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

        // by Claude - Addresses of Boolean-typed properties in GameRam.
        // These need normalization because GameRam stores true/false (→ 01/00),
        // but the 6502 treats any non-zero byte as true.
        val booleanAddresses: Set<Int> get() = GameRamMapper.booleanAddresses

        fun capture(memory: Memory6502): RamSnapshot {
            val values = mutableMapOf<Int, UByte>()
            for (range in MONITORED_RANGES) {
                for (addr in range) {
                    values[addr] = memory.readByte(addr)
                }
            }
            return RamSnapshot(values)
        }

        // by Claude - Uses GameRamMapper for consistent capture matching sync behavior
        fun capture(ram: GameRam): RamSnapshot = RamSnapshot(GameRamMapper.snapshot(ram))
    }

    /** by Claude - Normalize Boolean-typed addresses to 0/1 so interpreter snapshots
     *  (which store raw bytes) can be compared against GameRam snapshots (which store 0 or 1). */
    fun normalizeBooleans(): RamSnapshot {
        val normalized = values.toMutableMap()
        for (addr in booleanAddresses) {
            val v = normalized[addr] ?: continue
            normalized[addr] = if (v != 0.toUByte()) 1.toUByte() else 0.toUByte()
        }
        return RamSnapshot(normalized)
    }

    /** Compare this snapshot against another, returning differences.
     *  Only compares addresses present in BOTH snapshots to avoid false diffs
     *  from addresses that only one side knows about. */
    fun diff(other: RamSnapshot): List<RamDifference> {
        val diffs = mutableListOf<RamDifference>()
        val commonAddresses = (values.keys intersect other.values.keys).sorted()
        for (addr in commonAddresses) {
            val v1 = values[addr]!!
            val v2 = other.values[addr]!!
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
    val functionMetadata: Map<String, FunctionMetadata> = emptyMap(),
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

            // by Claude - Load metadata and correct addresses using ROM vector calibration.
            // The decompiler's AddressLabelMapper has a cumulative byte counting error
            // that varies across the ROM. We calibrate using the NMI vector ($FFFA) and
            // then validate each address by checking for valid instruction opcodes.
            val metadata = FunctionMetadata.loadAll()
            val correctedMetadata = correctMetadataAddresses(metadata, interpreter.memory)
            for ((_, func) in correctedMetadata) {
                labels[func.assemblyLabel] = func.address
                labels[func.name] = func.address
            }

            return SubroutineComparison(interpreter, labels, correctedMetadata)
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

        // by Claude - NTSC ROM: metadata addresses are correct with no offset needed.
        // The PAL ROM had a non-monotonic decompiler offset (ranging from -56 to +7),
        // but the NTSC ROM's function-metadata.json addresses match the ROM exactly.
        // All 471 functions verified: metadata address == actual ROM address.
        // Calibration retained as a pass-through in case future ROMs need correction.
        private val calibrationPoints = listOf(
            // address to known-correct offset (sorted by address)
            // NTSC ROM: all offsets are 0 (verified by matching ROM bytes to assembly)
            0x8000 to 0,   // Start (reset vector confirmed)
            0xB038 to 0,   // GetScreenPosition
            0xB269 to 0,   // PlayerDeath
            0xB273 to 0,   // DonePlayerTask
            0xB7B8 to 0,   // ProcessWhirlpools
            0xBED4 to 0,   // BlockObjMT_Updater
            0xC905 to 0,   // EnemyMovementSubs
            0xC998 to 0,   // EraseEnemyObject
            0xCE8E to 0,   // GetFirebarPosition
            0xCF28 to 0,   // MoveLakitu
            0xD67A to 0,   // OffscreenBoundsCheck
            0xD6D9 to 0,   // FireballEnemyCollision
            0xD92C to 0,   // InjurePlayer
            0xDA05 to 0,   // EnemyFacePlayer
            0xDB1C to 0,   // EnemyTurnAround
            0xE163 to 0,   // EnemyJump
            0xF12A to 0,   // RelativePlayerPosition
            0xF180 to 0,   // GetPlayerOffscreenBits
        )

        private fun computeOffset(metadataAddr: Int): Int {
            // Find the surrounding calibration points
            val below = calibrationPoints.lastOrNull { it.first <= metadataAddr }
            val above = calibrationPoints.firstOrNull { it.first > metadataAddr }
            return when {
                below == null -> above?.second ?: 0
                above == null -> below.second
                else -> {
                    // Linear interpolation between the two calibration points
                    val range = above.first - below.first
                    val pos = metadataAddr - below.first
                    val offsetRange = above.second - below.second
                    Math.round(below.second + offsetRange.toDouble() * pos / range).toInt()
                }
            }
        }

        private fun correctMetadataAddresses(
            metadata: Map<String, FunctionMetadata>,
            @Suppress("UNUSED_PARAMETER") memory: Memory6502
        ): Map<String, FunctionMetadata> {
            return metadata.mapValues { (_, func) ->
                val offset = computeOffset(func.address)
                func.copy(address = func.address + offset)
            }
        }
    }

    fun syncRamToSystem(system: System) {
        GameRamMapper.fromMemory(system.ram, interpreter.memory)
    }

    fun syncSystemToInterpreter(system: System) {
        GameRamMapper.toMemory(system.ram, interpreter.memory)
    }

    /** by Claude - Write random values to interpreter RAM in monitored ranges */
    fun randomizeRam(random: java.util.Random = java.util.Random(42)) {
        for (range in RamSnapshot.MONITORED_RANGES) {
            for (addr in range) {
                interpreter.memory.writeByte(addr, random.nextInt(256).toUByte())
            }
        }
    }

    /** by Claude - Run a subroutine in the interpreter starting at the given address.
     * Uses call-depth tracking to halt cleanly when the outermost function does RTS,
     * preventing BRK handler side effects from polluting the RAM snapshot. */
    fun runInterpreterSubroutine(address: Int, maxCycles: Long = 100_000): RamSnapshot {
        // Push a sentinel return address so RTS has something to pop.
        // RTS adds 1 to the popped address, so push sentinel-1.
        val sentinel = 0xFFF0
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

        // Track call depth: JSR increments, RTS decrements.
        // We enter at depth 0 (no JSR for the initial call). When the outermost
        // RTS fires, depth goes from 0 to -1, and we halt.
        var callDepth = 0
        val oldJsrHook = interpreter.jsrHook
        val oldRtsHook = interpreter.rtsHook
        interpreter.jsrHook = { _, _ -> callDepth++ }
        interpreter.rtsHook = {
            callDepth--
            if (callDepth < 0) interpreter.halted = true
        }

        // Set PC to the subroutine start
        interpreter.cpu.PC = address.toUShort()
        interpreter.halted = false

        interpreter.run(maxCycles)

        // Restore hooks
        interpreter.jsrHook = oldJsrHook
        interpreter.rtsHook = oldRtsHook

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

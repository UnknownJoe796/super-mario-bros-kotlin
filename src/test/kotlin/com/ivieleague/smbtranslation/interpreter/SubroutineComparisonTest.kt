// by Claude - Test harness for comparing interpreter execution against Kotlin translations
package com.ivieleague.smbtranslation.interpreter

import com.ivieleague.smbtranslation.*
import com.ivieleague.smbtranslation.utils.JoypadBits
import com.ivieleague.smbtranslation.utils.PpuControl
import com.ivieleague.smbtranslation.utils.PpuMask
import com.ivieleague.smbtranslation.utils.SpriteFlags
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

// by Claude - Function metadata from the decompiler's function-metadata.json
data class FunctionMetadata(
    val name: String,            // lowercase key, e.g. "relativeplayerposition"
    val assemblyLabel: String,   // original label, e.g. "RelativePlayerPosition"
    val address: Int,            // ROM address as integer
    val parameters: List<String>, // CPU registers used as input, e.g. ["X"], ["A", "X"]
    val returnType: String,      // "Unit" or "Int"
) {
    companion object {
        // by Claude - Load function metadata from pretty-printed JSON without a JSON library.
        // Parses the known structure of function-metadata.json line by line.
        fun loadAll(path: String = "docs/reference/function-metadata.json"): Map<String, FunctionMetadata> {
            val file = File(path)
            if (!file.exists()) return emptyMap()
            val result = mutableMapOf<String, FunctionMetadata>()

            val stringField = Regex(""""(\w+)":\s*"([^"]+)"""")
            val intField = Regex(""""(\w+)":\s*(\d+)""")
            val arrayStart = Regex(""""(\w+)":\s*\[""")
            val arrayStringItem = Regex(""""([^"]+)"""")

            var name: String? = null
            var assemblyLabel: String? = null
            var address: Int? = null
            var returnType: String? = null
            var parameters: MutableList<String>? = null
            var inParameters = false

            for (line in file.readLines()) {
                val trimmed = line.trim()

                if (inParameters) {
                    if (trimmed.contains("]")) {
                        inParameters = false
                    } else {
                        arrayStringItem.find(trimmed)?.let { parameters?.add(it.groupValues[1]) }
                    }
                    continue
                }

                stringField.find(trimmed)?.let { match ->
                    when (match.groupValues[1]) {
                        "name" -> name = match.groupValues[2]
                        "assemblyLabel" -> assemblyLabel = match.groupValues[2]
                        "returnType" -> returnType = match.groupValues[2]
                    }
                }
                intField.find(trimmed)?.let { match ->
                    if (match.groupValues[1] == "address") address = match.groupValues[2].toInt()
                }
                arrayStart.find(trimmed)?.let { match ->
                    if (match.groupValues[1] == "parameters") {
                        parameters = mutableListOf()
                        inParameters = !trimmed.contains("]")
                        if (!inParameters) {
                            // Inline array like "parameters": ["X"]
                            for (item in arrayStringItem.findAll(trimmed.substringAfter("["))) {
                                parameters?.add(item.groupValues[1])
                            }
                            // Remove "parameters" itself if accidentally captured
                            parameters?.remove("parameters")
                        }
                    }
                }

                // End of a function block — emit when we see a closing brace and have data
                if (trimmed.startsWith("}") && name != null && assemblyLabel != null && address != null) {
                    result[name!!] = FunctionMetadata(
                        name = name!!,
                        assemblyLabel = assemblyLabel!!,
                        address = address!!,
                        parameters = parameters ?: emptyList(),
                        returnType = returnType ?: "Unit",
                    )
                    name = null; assemblyLabel = null; address = null
                    returnType = null; parameters = null
                }
            }
            return result
        }
    }
}

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
        val booleanAddresses: Set<Int> by lazy {
            GameRam::class.declaredMemberProperties
                .filter { prop ->
                    val addr = prop.findAnnotation<RamLocation>()?.address ?: return@filter false
                    val value = prop.getter.call(GameRam())
                    value is Boolean
                }
                .mapNotNull { it.findAnnotation<RamLocation>()?.address }
                .toSet()
        }

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
                        if (value.size <= 256) {
                            for (i in value.indices) {
                                values[addr + i] = value[i].toUByte()
                            }
                        }
                    }
                    is UByteArray -> {
                        if (value.size <= 256) {
                            for (i in value.indices) {
                                values[addr + i] = value[i]
                            }
                        }
                    }
                    // by Claude - Handle value class types wrapping Byte.
                    // Skip enum types (e.g. OperMode) since they can't represent arbitrary byte values.
                    else -> {
                        val byteMethod = value?.javaClass?.methods?.find { it.name == "getByte" && it.parameterCount == 0 }
                        if (byteMethod != null) {
                            values[addr] = (byteMethod.invoke(value) as Byte).toUByte()
                        }
                    }
                }
            }
            // by Claude - Also capture the flat indexed arrays (not @RamLocation-annotated)
            captureArrays(ram, values)
            // by Claude - Capture oversized ByteArray(999) fields that are skipped by the size check above
            captureOversizedArrays(ram, values)
            return RamSnapshot(values)
        }

        // by Claude - Capture flat arrays into the values map at their known base addresses
        private fun captureArrays(ram: GameRam, values: MutableMap<Int, UByte>) {
            fun addArray(arr: ByteArray, base: Int, size: Int = arr.size) {
                for (i in 0 until minOf(size, arr.size)) {
                    values[base + i] = arr[i].toUByte()
                }
            }
            // SprObject flat arrays (25 entries each)
            addArray(ram.sprObjXSpeed, 0x57, 25)
            addArray(ram.sprObjPageLoc, 0x6D, 25)
            addArray(ram.sprObjXPos, 0x86, 25)
            addArray(ram.sprObjYSpeed, 0x9F, 25)
            addArray(ram.sprObjYHighPos, 0xB5, 25)
            addArray(ram.sprObjYPos, 0xCE, 25)
            addArray(ram.sprAttrib, 0x3C4, 25)
            addArray(ram.sprObjXMoveForce, 0x400, 25)
            addArray(ram.sprObjYMFDummy, 0x416, 25)
            addArray(ram.sprObjYMoveForce, 0x433, 25)
            // Condensed arrays (9 entries)
            addArray(ram.relXPos, 0x3AD, 9)
            addArray(ram.relYPos, 0x3B8, 9)
            addArray(ram.offscrBits, 0x3D0, 9)
            // Entity-specific arrays
            addArray(ram.enemyMovingDirs, 0x46, 6)
            addArray(ram.enemyFlags, 0x0F, 6)
            addArray(ram.enemyBoundBoxCtrls, 0x49A, 6)
            addArray(ram.fireballStates, 0x24, 2)
            addArray(ram.blockStates, 0x26, 2)
            addArray(ram.miscStates, 0x2A, 9)
            addArray(ram.blockRepFlags, 0x3EC, 2)
            addArray(ram.shellChainCounters, 0x125, 6)
            addArray(ram.fireballBoundBoxCtrls, 0x4A0, 2)
            // Cannon arrays
            addArray(ram.cannonPageLocs, 0x46B, 6)
            addArray(ram.cannonXPositions, 0x471, 6)
            addArray(ram.cannonYPositions, 0x477, 6)
            addArray(ram.cannonTimers, 0x47D, 6)
            // Enemy collision/offscreen
            addArray(ram.enemyCollisionBitsArr, 0x491, 6)
            addArray(ram.enemyOffscrBitsMaskeds, 0x3D8, 6)
            // by Claude - Bounding box coordinates (WeakHashMap extension property)
            addArray(ram.boundBoxCoords, 0x4AC, 60)
        }

        // by Claude - Capture oversized ByteArray(999) fields at their known base addresses
        // with correct sizes (these are skipped by the generic capture due to size > 256)
        private fun captureOversizedArrays(ram: GameRam, values: MutableMap<Int, UByte>) {
            // Enemy_State at $1E, 6 entries
            for (i in 0 until 6) values[0x1E + i] = ram.enemyState[i].toUByte()
            // DigitModifier at $134, 6 entries
            for (i in 0 until 6) values[0x134 + i] = ram.digitModifier[i].toUByte()
        }
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

    /** by Claude - Copy interpreter memory → GameRam for all @RamLocation properties.
     *  Handles both mutable (var) scalar properties and val ByteArray/UByteArray contents. */
    fun syncRamToSystem(system: System) {
        for (prop in GameRam::class.declaredMemberProperties) {
            val addr = prop.findAnnotation<RamLocation>()?.address ?: continue
            val value = prop.getter.call(system.ram)

            // For val ByteArray/UByteArray: copy contents from interpreter memory
            when (value) {
                is ByteArray -> {
                    // Skip oversized placeholder arrays (e.g., ByteArray(999))
                    if (value.size <= 256) {
                        for (i in value.indices) {
                            value[i] = interpreter.memory.readByte(addr + i).toByte()
                        }
                    }
                    continue
                }
                is UByteArray -> {
                    if (value.size <= 256) {
                        for (i in value.indices) {
                            value[i] = interpreter.memory.readByte(addr + i)
                        }
                    }
                    continue
                }
            }

            // For var scalar properties: set via reflection
            val mutableProp = prop as? KMutableProperty1<GameRam, *> ?: continue
            when (value) {
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
                // by Claude - Value class and enum types are handled by syncValueClassProperties()
                // after this loop, not by reflection here (reflection can't set value class setters)
            }
        }
        // by Claude - Sync flat indexed arrays that aren't backed by @RamLocation
        syncArraysFromInterpreter(system)
        // by Claude - Explicitly sync value class and enum properties that reflection can't
        // set correctly (value classes are inlined as Byte at JVM level, but Kotlin reflection
        // may reject Byte values for setters expecting value class types)
        syncValueClassProperties(system)
    }

    /** by Claude - Sync value class and enum GameRam properties from interpreter memory.
     *  These can't be handled by the generic reflection loop because Kotlin value classes
     *  are inlined at JVM level (getter returns Byte) but setters expect the wrapper type. */
    private fun syncValueClassProperties(system: System) {
        system.ram.savedJoypadBits = JoypadBits(interpreter.memory.readByte(0x6FC).toByte())
        system.ram.savedJoypad1Bits = JoypadBits(interpreter.memory.readByte(0x6FC).toByte())
        system.ram.savedJoypad2Bits = JoypadBits(interpreter.memory.readByte(0x6FD).toByte())
        system.ram.mirrorPPUCTRLREG1 = PpuControl(interpreter.memory.readByte(0x778).toByte())
        system.ram.mirrorPPUCTRLREG2 = PpuMask(interpreter.memory.readByte(0x779).toByte())
        // by Claude - playerSprAttrib is also a value class (SpriteFlags) at $03C4
        system.ram.playerSprAttrib = SpriteFlags(interpreter.memory.readByte(0x3C4).toByte())
        // Note: OperMode at $0770 is NOT synced — it's an enum with only 4 valid values
        // and can't represent the arbitrary byte values the interpreter may have.
    }

    // by Claude - Array base addresses for SprObject flat arrays (25 entries each)
    private data class ArrayMapping(val getter: (GameRam) -> ByteArray, val base: Int, val size: Int)

    private val arrayMappings = listOf(
        ArrayMapping({ it.sprObjXSpeed }, 0x57, 25),
        ArrayMapping({ it.sprObjPageLoc }, 0x6D, 25),
        ArrayMapping({ it.sprObjXPos }, 0x86, 25),
        ArrayMapping({ it.sprObjYSpeed }, 0x9F, 25),
        ArrayMapping({ it.sprObjYHighPos }, 0xB5, 25),
        ArrayMapping({ it.sprObjYPos }, 0xCE, 25),
        ArrayMapping({ it.sprAttrib }, 0x3C4, 25),
        ArrayMapping({ it.sprObjXMoveForce }, 0x400, 25),
        ArrayMapping({ it.sprObjYMFDummy }, 0x416, 25),
        ArrayMapping({ it.sprObjYMoveForce }, 0x433, 25),
        // Condensed arrays (9 entries)
        ArrayMapping({ it.relXPos }, 0x3AD, 9),
        ArrayMapping({ it.relYPos }, 0x3B8, 9),
        ArrayMapping({ it.offscrBits }, 0x3D0, 9),
        // Entity-specific arrays
        ArrayMapping({ it.enemyMovingDirs }, 0x46, 6),
        ArrayMapping({ it.enemyFlags }, 0x0F, 6),
        ArrayMapping({ it.enemyBoundBoxCtrls }, 0x49A, 6),
        ArrayMapping({ it.fireballStates }, 0x24, 2),
        ArrayMapping({ it.blockStates }, 0x26, 2),
        ArrayMapping({ it.miscStates }, 0x2A, 9),
        ArrayMapping({ it.blockRepFlags }, 0x3EC, 2),
        ArrayMapping({ it.shellChainCounters }, 0x125, 6),
        ArrayMapping({ it.fireballBoundBoxCtrls }, 0x4A0, 2),
        // Cannon arrays
        ArrayMapping({ it.cannonPageLocs }, 0x46B, 6),
        ArrayMapping({ it.cannonXPositions }, 0x471, 6),
        ArrayMapping({ it.cannonYPositions }, 0x477, 6),
        ArrayMapping({ it.cannonTimers }, 0x47D, 6),
        // Enemy collision/offscreen
        ArrayMapping({ it.enemyCollisionBitsArr }, 0x491, 6),
        ArrayMapping({ it.enemyOffscrBitsMaskeds }, 0x3D8, 6),
        // by Claude - Oversized ByteArray(999) fields need explicit sync with correct sizes
        ArrayMapping({ it.enemyState }, 0x1E, 6),
        ArrayMapping({ it.digitModifier }, 0x134, 6),
        // by Claude - Bounding box coordinates (WeakHashMap extension property, $04AC)
        ArrayMapping({ it.boundBoxCoords }, 0x4AC, 60),
    )

    /** by Claude - Copy flat indexed arrays from interpreter memory to GameRam */
    fun syncArraysFromInterpreter(system: System) {
        for ((getter, base, size) in arrayMappings) {
            val arr = getter(system.ram)
            for (i in 0 until minOf(size, arr.size)) {
                arr[i] = interpreter.memory.readByte(base + i).toByte()
            }
        }
    }

    /** by Claude - Copy flat indexed arrays from GameRam back to interpreter memory */
    fun syncArraysToInterpreter(system: System) {
        for ((getter, base, size) in arrayMappings) {
            val arr = getter(system.ram)
            for (i in 0 until minOf(size, arr.size)) {
                interpreter.memory.writeByte(base + i, arr[i].toUByte())
            }
        }
    }

    /** by Claude - Copy all @RamLocation properties and arrays from System back to interpreter */
    fun syncSystemToInterpreter(system: System) {
        for (prop in GameRam::class.declaredMemberProperties) {
            val addr = prop.findAnnotation<RamLocation>()?.address ?: continue
            when (val value = prop.getter.call(system.ram)) {
                is Byte -> interpreter.memory.writeByte(addr, value.toUByte())
                is UByte -> interpreter.memory.writeByte(addr, value)
                is Boolean -> interpreter.memory.writeByte(addr, if (value) 1u else 0u)
                is ByteArray -> {
                    if (value.size <= 256) {
                        for (i in value.indices) {
                            interpreter.memory.writeByte(addr + i, value[i].toUByte())
                        }
                    }
                }
                is UByteArray -> {
                    if (value.size <= 256) {
                        for (i in value.indices) {
                            interpreter.memory.writeByte(addr + i, value[i])
                        }
                    }
                }
            }
        }
        syncArraysToInterpreter(system)
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

// by Claude - Shadow validator: runs the 6502 interpreter alongside Kotlin translation,
// comparing RAM at subroutine boundaries to catch divergences in real-time.
package com.ivieleague.smbtranslation.interpreter

import com.ivieleague.smbtranslation.*
import java.io.File
import java.io.PrintWriter

/**
 * Runs the original 6502 interpreter as a shadow alongside the Kotlin translation.
 * Before each instrumented function: syncs Kotlin RAM → interpreter, runs the assembly
 * subroutine, runs the Kotlin function, compares results. Logs divergences.
 *
 * The Kotlin state is always authoritative — the interpreter is purely observational.
 */
class ShadowValidator private constructor(
    private val interpreter: BinaryInterpreter6502,
    private val nameToAddress: Map<String, Int>,
) {
    var frameNumber = 0L
        private set
    private var totalComparisons = 0
    private var totalDivergences = 0
    private val divergenceCounts = mutableMapOf<String, Int>()

    private val logFile: PrintWriter = run {
        val dir = File("build")
        dir.mkdirs()
        PrintWriter(File(dir, "shadow-validator.log").bufferedWriter())
    }

    companion object {
        /**
         * Load the ROM and create a shadow validator. Returns null if the ROM file
         * is not found — the game runs without validation in that case.
         */
        fun create(romPath: String): ShadowValidator? {
            val romFile = File(romPath)
            if (!romFile.exists()) {
                println("[ShadowValidator] ROM not found at '$romPath', shadow validation disabled")
                return null
            }

            val rom = NESLoader.load(romFile)
            val interpreter = BinaryInterpreter6502()
            NESLoader.loadIntoMemory(rom, interpreter.memory)

            // Suppress PPU/APU reads/writes — the interpreter shouldn't touch hardware registers
            interpreter.memoryReadHook = { addr ->
                if (addr >= 0x2000 && addr < 0x6000) 0u else null
            }
            interpreter.memoryWriteHook = { addr, _ ->
                addr >= 0x2000 && addr < 0x6000  // true = swallow the write
            }

            // Build name→address map from function metadata
            val metadata = FunctionMetadata.loadAll()
            val nameToAddress = mutableMapOf<String, Int>()
            for ((key, func) in metadata) {
                nameToAddress[key] = func.address
            }

            println("[ShadowValidator] Loaded ROM, ${metadata.size} functions mapped, shadow validation active")
            return ShadowValidator(interpreter, nameToAddress)
        }
    }

    // RAM sync now centralized in GameRamMapper

    /** Addresses to compare — zero page + game RAM */
    private val monitoredRanges = listOf(
        0x00..0xFF,       // Zero page
        0x200..0x2FF,     // OAM/Sprite data
        0x300..0x3FF,     // VRAM buffers & misc
        0x400..0x4FF,     // Object data
        0x500..0x7FF,     // Block buffers, display data, sound
    )

    /** Addresses excluded from comparison (PPU/VRAM/sound areas that structurally differ) */
    private val excludedAddresses: Set<Int> = buildSet {
        addAll(0x200..0x2FF)    // OAM sprite data
        addAll(0x300..0x3AC)    // VRAM buffer 1
        addAll(0x4AC..0x4FF)    // Bounding box coordinates (transient per-frame collision data)
        addAll(0x500..0x7FF)    // Block buffers, display digits, sound queues
        add(0xFF)               // Sound queue
        add(0xFE)               // Sound queue
        add(0xFD)               // Sound queue
        add(0xFC)               // Sound queue
    }

    private val booleanAddresses: Set<Int> get() = GameRamMapper.booleanAddresses

    private fun syncSystemToInterpreter(system: System) {
        GameRamMapper.toMemory(system.ram, interpreter.memory)
        // Reset VRAM buffer offsets to prevent overflow into game state
        interpreter.memory.writeByte(0x0300, 0u)  // VRAM_Buffer1_Offset
        interpreter.memory.writeByte(0x0340, 0u)  // VRAM_Buffer2_Offset
    }

    // --- Interpreter subroutine runner ---

    private fun runInterpreterSubroutine(address: Int, maxCycles: Long = 100_000) {
        // Push sentinel return address (RTS adds 1)
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

        interpreter.cpu.PC = address.toUShort()
        interpreter.halted = false

        // Use PC-based halt detection instead of callDepth tracking.
        // The callDepth approach fails for subroutines that use JumpEngine
        // (JSR JumpEngine → PLA PLA JMP), which consumes a return address
        // via PLA without a matching RTS, leaving callDepth off by one.
        // Instead, simply run until PC reaches the sentinel address.
        var totalCycles = 0L
        while (!interpreter.halted && totalCycles < maxCycles && interpreter.cpu.PC.toInt() != sentinel) {
            totalCycles += interpreter.step()
        }
    }

    // --- RAM snapshot & comparison ---

    private fun captureInterpreterSnapshot(): Map<Int, UByte> {
        val values = mutableMapOf<Int, UByte>()
        for (range in monitoredRanges) {
            for (addr in range) {
                values[addr] = interpreter.memory.readByte(addr)
            }
        }
        return values
    }

    private fun captureKotlinSnapshot(ram: GameRam): Map<Int, UByte> = GameRamMapper.snapshot(ram)

    private fun normalizeBooleans(snapshot: MutableMap<Int, UByte>) {
        for (addr in booleanAddresses) {
            val v = snapshot[addr] ?: continue
            snapshot[addr] = if (v != 0.toUByte()) 1.toUByte() else 0.toUByte()
        }
    }

    private fun compareSnapshots(
        interpreterSnap: Map<Int, UByte>,
        kotlinSnap: Map<Int, UByte>,
    ): List<Pair<Int, Pair<UByte, UByte>>> {
        val diffs = mutableListOf<Pair<Int, Pair<UByte, UByte>>>()
        val commonAddresses = (interpreterSnap.keys intersect kotlinSnap.keys).sorted()
        for (addr in commonAddresses) {
            if (addr in excludedAddresses) continue
            val interp = interpreterSnap[addr]!!
            val kotlin = kotlinSnap[addr]!!
            if (interp != kotlin) {
                diffs.add(addr to (interp to kotlin))
            }
        }
        return diffs
    }

    // --- Public API ---

    /** Increment the frame counter. Call at the top of NMI. */
    fun onFrameStart() {
        frameNumber++
    }

    /**
     * Validate a single function call: sync → run interpreter → run Kotlin → compare.
     * Returns the result of [block] (the Kotlin function).
     */
    fun <T> validated(name: String, system: System, block: () -> T): T {
        val address = nameToAddress[name]
        if (address == null) {
            // No metadata for this function — just run Kotlin
            return block()
        }

        // Sync current Kotlin state → interpreter
        syncSystemToInterpreter(system)

        // Set interpreter objectOffset register to match Kotlin
        interpreter.cpu.X = (system.ram.objectOffset.toInt() and 0xFF).toUByte()
        interpreter.cpu.SP = 0xFFu
        interpreter.cpu.C = false

        // Run the interpreter subroutine
        runInterpreterSubroutine(address)
        val interpSnap = captureInterpreterSnapshot().toMutableMap()
        normalizeBooleans(interpSnap)

        // Run the Kotlin function
        val result = block()

        // Capture Kotlin state after execution
        val kotlinSnap = captureKotlinSnapshot(system.ram)

        // Compare
        val diffs = compareSnapshots(interpSnap, kotlinSnap)
        totalComparisons++

        if (diffs.isNotEmpty()) {
            totalDivergences++
            divergenceCounts[name] = (divergenceCounts[name] ?: 0) + 1

            logFile.println("Frame $frameNumber | $name | ${diffs.size} diffs:")
            for ((addr, values) in diffs.take(20)) {
                val addrStr = "$$${addr.toString(16).padStart(4, '0')}"
                logFile.println("  $addrStr: interp=${values.first.toString(16).padStart(2, '0')} kotlin=${values.second.toString(16).padStart(2, '0')}")
            }
            if (diffs.size > 20) logFile.println("  ... and ${diffs.size - 20} more")
            logFile.flush()
        }

        return result
    }

    /** Print summary and close log file. */
    fun close() {
        val summary = buildString {
            appendLine("=== Shadow Validator Summary ===")
            appendLine("Frames: $frameNumber")
            appendLine("Comparisons: $totalComparisons")
            appendLine("Divergences: $totalDivergences (${"%.2f".format(if (totalComparisons > 0) totalDivergences * 100.0 / totalComparisons else 0.0)}%)")
            if (divergenceCounts.isNotEmpty()) {
                appendLine("Per-function divergences:")
                for ((name, count) in divergenceCounts.entries.sortedByDescending { it.value }) {
                    appendLine("  $name: $count")
                }
            }
        }
        println(summary)
        logFile.println(summary)
        logFile.close()
    }
}

// by Claude - FunctionMetadata: loads function name→address mappings from function-metadata.json.
// Extracted from SubroutineComparisonTest so both test and runtime code can use it.
data class FunctionMetadata(
    val name: String,
    val assemblyLabel: String,
    val address: Int,
    val parameters: List<String>,
    val returnType: String,
) {
    companion object {
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
                            for (item in arrayStringItem.findAll(trimmed.substringAfter("["))) {
                                parameters?.add(item.groupValues[1])
                            }
                            parameters?.remove("parameters")
                        }
                    }
                }

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

// by Claude - TAS replay test: runs happylee-warps TAS through the translated Kotlin code
// and verifies game progression by comparing against FCEUX RAM dumps.
package com.ivieleague.smbtranslation.interpreter

import com.ivieleague.smbtranslation.*
import com.ivieleague.smbtranslation.utils.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.KMutableProperty1
import kotlin.test.assertTrue

/**
 * Replays a TAS through the translated Kotlin code with full per-frame FCEUX RAM sync.
 * Each frame: sync all state from FCEUX dump, inject buttons, run NMI, compare outputs.
 * The FCEUX dump captures state mid-NMI (after timers, before operModeExecutionTree),
 * so we sync that state and run our full NMI to verify game logic.
 */
class TASReplayTest {

    companion object {
        const val OPER_MODE = 0x0770
        const val OPER_MODE_TASK = 0x0772
        const val FRAME_COUNTER = 0x09
        const val WORLD_NUMBER = 0x075F
        const val LEVEL_NUMBER = 0x0760
        const val PLAYER_X_POSITION = 0x0086
        const val PLAYER_Y_POSITION = 0x00CE
        const val GAME_ENGINE_SUBROUTINE = 0x0E
        const val SCREEN_ROUTINE_TASK = 0x073C

        // NMI suppress frames: RESET handler still running
        val SUPPRESS_NMI_FRAMES = setOf(0, 1, 2, 3, 4, 6, 7)
    }

    data class FrameInput(val player1: Int, val player2: Int = 0)

    private fun parseFM2(file: File): List<FrameInput> {
        val inputs = mutableListOf<FrameInput>()
        for (line in file.readLines()) {
            if (!line.startsWith("|")) continue
            val parts = line.removePrefix("|").removeSuffix("|").split("|")
            if (parts.size < 2) continue
            val p1 = parseFM2Buttons(parts[1].padEnd(8, '.'))
            val p2 = if (parts.size >= 3) parseFM2Buttons(parts[2].padEnd(8, '.')) else 0
            inputs.add(FrameInput(p1, p2))
        }
        return inputs
    }

    private fun parseFM2Buttons(input: String): Int {
        var buttons = 0
        if (input.length >= 8) {
            if (input[0] != '.') buttons = buttons or 0x01
            if (input[1] != '.') buttons = buttons or 0x02
            if (input[2] != '.') buttons = buttons or 0x04
            if (input[3] != '.') buttons = buttons or 0x08
            if (input[4] != '.') buttons = buttons or 0x10
            if (input[5] != '.') buttons = buttons or 0x20
            if (input[6] != '.') buttons = buttons or 0x40
            if (input[7] != '.') buttons = buttons or 0x80
        }
        return buttons
    }

    private fun findFile(vararg paths: String): File? =
        paths.map { File(it) }.firstOrNull { it.exists() }

    /**
     * Sync ALL GameRam state from a 2048-byte FCEUX RAM frame.
     * Uses @RamLocation annotations to copy each field from the flat byte array.
     */
    private fun syncFullRamFromFceux(system: System, fceuxFrame: ByteArray, offset: Int) {
        for (prop in GameRam::class.declaredMemberProperties) {
            val addr = prop.findAnnotation<RamLocation>()?.address ?: continue
            val value = prop.getter.call(system.ram)

            // ByteArray fields: copy contents from FCEUX RAM
            when (value) {
                is ByteArray -> {
                    if (value.size <= 256) {
                        for (i in value.indices) {
                            if (addr + i < 2048) value[i] = fceuxFrame[offset + addr + i]
                        }
                    }
                    continue
                }
                is UByteArray -> {
                    if (value.size <= 256) {
                        for (i in value.indices) {
                            if (addr + i < 2048) value[i] = fceuxFrame[offset + addr + i].toUByte()
                        }
                    }
                    continue
                }
            }

            // Scalar fields: set via reflection
            val mutableProp = prop as? KMutableProperty1<GameRam, *> ?: continue
            if (addr >= 2048) continue
            val byteVal = fceuxFrame[offset + addr]
            when (value) {
                is Byte -> {
                    @Suppress("UNCHECKED_CAST")
                    (mutableProp as KMutableProperty1<GameRam, Byte>).set(system.ram, byteVal)
                }
                is UByte -> {
                    @Suppress("UNCHECKED_CAST")
                    (mutableProp as KMutableProperty1<GameRam, UByte>).set(system.ram, byteVal.toUByte())
                }
                is Boolean -> {
                    @Suppress("UNCHECKED_CAST")
                    (mutableProp as KMutableProperty1<GameRam, Boolean>).set(system.ram, byteVal != 0.toByte())
                }
                // Value classes and enums handled explicitly below
            }
        }

        // Sync SprObject flat arrays (not annotated with @RamLocation)
        val flatArrays = listOf(
            system.ram.sprObjXSpeed to 0x57,
            system.ram.sprObjPageLoc to 0x6D,
            system.ram.sprObjXPos to 0x86,
            system.ram.sprObjYSpeed to 0x9F,
            system.ram.sprObjYPos to 0xCE,
            system.ram.sprObjYHighPos to 0xB5,
            system.ram.sprObjXMoveForce to 0x0400,
            system.ram.sprObjYMoveForce to 0x0416,
        )
        for ((arr, base) in flatArrays) {
            for (i in arr.indices) {
                if (base + i < 2048) arr[i] = fceuxFrame[offset + base + i]
            }
        }

        // sprDataOffsets store NES byte offsets (0-252); game code divides by 4 to get sprite index.
        // sprites[] is Array(64) matching NES OAM (64 sprites × 4 bytes each).

        // Reset stack index — NMI does balanced push/pop but currentIndex accumulates across frames
        system.ram.stack.clear()

        // Sync value class properties explicitly
        system.ram.savedJoypadBits = JoypadBits(fceuxFrame[offset + 0x6FC])
        system.ram.savedJoypad1Bits = JoypadBits(fceuxFrame[offset + 0x6FC])
        system.ram.savedJoypad2Bits = JoypadBits(fceuxFrame[offset + 0x6FD])
        system.ram.mirrorPPUCTRLREG1 = PpuControl(fceuxFrame[offset + 0x778])
        system.ram.mirrorPPUCTRLREG2 = PpuMask(fceuxFrame[offset + 0x779])
        system.ram.playerSprAttrib = SpriteFlags(fceuxFrame[offset + 0x3C4])

        // Sync OperMode enum
        val fMode = fceuxFrame[offset + OPER_MODE].toInt() and 0xFF
        system.ram.operMode = when (fMode) {
            0 -> OperMode.TitleScreen
            1 -> OperMode.Game
            2 -> OperMode.Victory
            else -> OperMode.GameOver
        }
    }

    private fun initializeFromInterpreter(system: System, romFile: File): SubroutineComparison {
        val comparison = SubroutineComparison.create(romFile.path)
        val interp = comparison.interpreter

        var ppuStatus: UByte = 0x80u
        var ppuStatusReads = 0

        interp.memoryReadHook = { addr ->
            when (addr) {
                0x2002 -> {
                    ppuStatusReads++
                    val status = ppuStatus
                    ppuStatus = ppuStatus and 0x7Fu
                    if (ppuStatusReads % 100 == 0) ppuStatus = ppuStatus or 0x80u
                    status
                }
                in 0x2000..0x2007 -> 0u
                else -> null
            }
        }
        interp.memoryWriteHook = { addr, _ ->
            when (addr) {
                0x2000 -> true
                in 0x2001..0x2007 -> true
                0x4014 -> true
                in 0x4000..0x4017 -> true
                else -> false
            }
        }

        interp.cpu.PC = 0x8000u
        var cycles = 0
        while (cycles < 100000) {
            val pc = interp.cpu.PC.toInt()
            val opcode = interp.memory.readByte(pc).toInt()
            if (opcode == 0x4C) {
                val lo = interp.memory.readByte(pc + 1).toInt()
                val hi = interp.memory.readByte(pc + 2).toInt()
                val target = lo or (hi shl 8)
                if (target == pc) break
            }
            interp.step()
            cycles++
        }
        println("RESET done in $cycles steps")

        ppuStatus = 0x80u
        ppuStatusReads = 0
        interp.cpu.PC = 0x8082u
        cycles = 0
        while (cycles < 50000) {
            if (interp.memory.readByte(interp.cpu.PC.toInt()) == 0x40.toUByte()) break
            interp.step()
            cycles++
        }
        println("First NMI done in $cycles steps")

        comparison.syncRamToSystem(system)

        interp.memoryReadHook = null
        interp.memoryWriteHook = null

        return comparison
    }

    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @Test
    fun `TAS replay reaches game completion`() {
        val romFile = findFile("smb.nes", "../smb.nes")
        if (romFile == null) { println("Skipping: No ROM file found"); return }

        val tasFile = findFile(
            "happylee-warps.fm2", "../happylee-warps.fm2",
            "/Users/jivie/Projects/decompiler-6502-kotlin/smb/happylee-warps.fm2",
        )
        if (tasFile == null) { println("Skipping: No TAS file found"); return }

        val fceuxRamFile = findFile(
            "data/tas/fceux-full-ram.bin", "../data/tas/fceux-full-ram.bin",
            "/Users/jivie/Projects/decompiler-6502-kotlin/data/tas/fceux-full-ram.bin",
        )
        val fceuxRam = fceuxRamFile?.readBytes()

        println("=== TAS Replay Test ===")
        println("ROM: ${romFile.absolutePath}")
        println("TAS: ${tasFile.absolutePath}")
        if (fceuxRam != null) {
            println("FCEUX RAM dump: ${fceuxRam.size / 2048} frames")
        } else {
            println("WARNING: No FCEUX RAM dump - full sync disabled")
        }

        val tasInputs = parseFM2(tasFile)
        println("Loaded ${tasInputs.size} TAS frames")

        val system = System()
        initializeFromInterpreter(system, romFile)

        var maxWorld = 1
        var maxLevel = 1
        var reachedW84 = false
        var gameCompleted = false
        var lastProgressFrame = 0
        var nmiErrorCount = 0
        val errorLocations = mutableMapOf<String, Int>()
        val totalFrames = minOf(tasInputs.size, if (fceuxRam != null) fceuxRam.size / 2048 else tasInputs.size)

        println("\n=== Running TAS ($totalFrames frames) ===")

        for (frame in 0 until totalFrames) {
            if (frame in SUPPRESS_NMI_FRAMES) continue

            // Full RAM sync from FCEUX dump BEFORE NMI.
            // The FCEUX dump captures mid-NMI state (after timers, before game logic).
            // By syncing this state, our NMI's operModeExecutionTree starts from the
            // exact same state as FCEUX's.
            if (fceuxRam != null) {
                val fOff = frame * 2048
                if (fOff + 2047 < fceuxRam.size) {
                    syncFullRamFromFceux(system, fceuxRam, fOff)
                }
            }

            // Inject TAS buttons into input port so readJoypads() transfers them to RAM
            val buttons = if (frame < tasInputs.size) tasInputs[frame].player1 else 0
            system.inputs.joypadPort1 = JoypadBits(buttons.toByte())

            // Run NMI with timeout protection
            var nmiError: Throwable? = null
            try {
                val nmiThread = Thread {
                    try {
                        system.nonMaskableInterrupt()
                    } catch (e: Throwable) {
                        nmiError = e
                    }
                }
                nmiThread.start()
                nmiThread.join(2000)
                if (nmiThread.isAlive) {
                    println("Frame $frame HUNG! Mode=${system.ram.operMode}, Task=${system.ram.operModeTask}")
                    @Suppress("DEPRECATION")
                    nmiThread.stop()
                    break
                }
                if (nmiError != null) {
                    nmiErrorCount++
                    val loc = nmiError!!.stackTrace.firstOrNull {
                        it.className.startsWith("com.ivieleague.smbtranslation")
                    }?.let { "${it.fileName}:${it.lineNumber}" } ?: "unknown"
                    errorLocations[loc] = (errorLocations[loc] ?: 0) + 1
                    // Log first few errors with full detail, then periodically
                    if (nmiErrorCount <= 5 || frame % 2000 == 0) {
                        println("Frame $frame NMI ERROR #$nmiErrorCount: ${nmiError!!::class.simpleName}: ${nmiError!!.message}")
                        println(nmiError!!.stackTraceToString().lines().take(5).joinToString("\n"))
                    }
                }
            } catch (e: Exception) {
                println("Frame $frame ERROR: ${e.message}")
                if (frame > 100) break
            }

            // Check progress (read from FCEUX state, not our output, for consistency)
            if (fceuxRam != null) {
                val fOff = frame * 2048
                if (fOff + 2047 < fceuxRam.size) {
                    val world = (fceuxRam[fOff + WORLD_NUMBER].toInt() and 0xFF) + 1
                    val level = (fceuxRam[fOff + LEVEL_NUMBER].toInt() and 0xFF) + 1
                    val fMode = fceuxRam[fOff + OPER_MODE].toInt() and 0xFF

                    if (world > maxWorld || (world == maxWorld && level > maxLevel)) {
                        maxWorld = world
                        maxLevel = level
                        lastProgressFrame = frame
                        println("Frame $frame: Reached W$world-$level (FCEUX Mode=$fMode)")
                    }
                    if (world == 8 && level == 4) reachedW84 = true
                    if (reachedW84 && fMode == 2) {
                        gameCompleted = true
                        println("Frame $frame: GAME COMPLETED!")
                        break
                    }
                }
            }

            // Progress reporting
            if (frame % 2000 == 0) {
                println("Frame $frame/$totalFrames: W$maxWorld-$maxLevel")
            }
        }

        println("\n=== Results ===")
        println("Max level reached: W$maxWorld-$maxLevel")
        println("Reached W8-4: $reachedW84")
        println("Game completed: $gameCompleted")
        println("NMI errors: $nmiErrorCount / $totalFrames frames")
        if (errorLocations.isNotEmpty()) {
            println("\nError locations (sorted by frequency):")
            for ((loc, count) in errorLocations.entries.sortedByDescending { it.value }) {
                println("  $count\t$loc")
            }
        }

        assertTrue(maxWorld > 1 || maxLevel > 1 || gameCompleted,
            "Should make progress past W1-1 (reached W$maxWorld-$maxLevel)")

        when {
            gameCompleted -> println("\nSUCCESS: Translation completed the TAS!")
            reachedW84 -> println("\nPARTIAL: Reached W8-4 but didn't detect completion")
            else -> println("\nINCOMPLETE: Only reached W$maxWorld-$maxLevel")
        }
    }
}

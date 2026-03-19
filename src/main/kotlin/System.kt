package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.interpreter.ShadowValidator
import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.nes.AudioProcessingUnit
import com.ivieleague.smbtranslation.nes.Inputs
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit

class System {
    val ram = GameRam()
    val ppu = PictureProcessingUnit()
    val apu = AudioProcessingUnit()
    val inputs = Inputs()
    var shadow: ShadowValidator? = null
    /** Temporary debug flag for tracing enemy processing flow */
    var debugEnemyTrace: Boolean = false

    /**
     * Pending action to be executed during the next NMI.
     * Used by FrameDelay to suspend long-running routines across frames.
     */
    var pendingNmiAction: (() -> Unit)? = null

    /**
     * Reads a byte from the equivalent NES flat RAM address in the SprObject region.
     * Handles cross-array boundary reads that occur in NES hardware when using
     * indexed addressing across adjacent RAM regions.
     *
     * Layout: $57(22)sprObjXSpeed, $6D(25)sprObjPageLoc, $86(25)sprObjXPos,
     *         $9F(22)sprObjYSpeed, $B5(25)sprObjYHighPos, $CE(25)sprObjYPos
     */
    fun readNesRamByte(addr: Int): Byte {
        return when {
            addr in 0x57..0x6C -> ram.sprObjXSpeed[addr - 0x57]     // 22 bytes
            addr in 0x6D..0x85 -> ram.sprObjPageLoc[addr - 0x6D]    // 25 bytes
            addr in 0x86..0x9E -> ram.sprObjXPos[addr - 0x86]       // 25 bytes
            addr in 0x9F..0xB4 -> ram.sprObjYSpeed[addr - 0x9F]     // 22 bytes
            addr in 0xB5..0xCD -> ram.sprObjYHighPos[addr - 0xB5]   // 25 bytes
            addr in 0xCE..0xE6 -> ram.sprObjYPos[addr - 0xCE]       // 25 bytes
            else -> 0 // outside SprObject region — see setupVine for special handling
        }
    }
}
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
}
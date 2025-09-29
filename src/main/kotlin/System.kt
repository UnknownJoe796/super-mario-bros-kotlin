package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.AudioProcessingUnit
import com.ivieleague.smbtranslation.nes.Inputs
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit

class System {
    val ram = GameRam()
    val ppu = PictureProcessingUnit()
    val apu = AudioProcessingUnit()
    val inputs = Inputs()
}
package com.ivieleague.smbtranslation.old

class System() {
    var disableScreenFlag: Boolean = false
    var game: Game = Game()

    //    VRAM_Buffer1_Offset   = $0300
    //    VRAM_Buffer1          = $0301
    val vramBuffer1 = VramBuffer()

    //    VRAM_Buffer2_Offset   = $0340
    //    VRAM_Buffer2          = $0341
    val vramBuffer2 = VramBuffer()

    //    VRAM_Buffer_AddrCtrl  = $0773
    var bufferToWrite: VramBuffer = vramBuffer1

    //    GamePauseStatus       = $0776
    var gamePauseStatus: Byte = 0x0
    //    GamePauseTimer        = $0777
    var gamePauseTimer: Byte = 0x0

    //    IntervalTimerControl  = $077f
    var intervalTimerControl: Byte = 0x0
}
var system: System = System()
package com.ivieleague.smbtranslation.old

class Game() {

    //> OperMode              = $0770
    var operMode: Byte = 0x00
    //> OperMode_Task         = $0772
    var operModeTask: Byte = 0x00
    //> PseudoRandomBitReg    = $07a7
    var psuedoRandomBitReg: ByteArray = byteArrayOf(0x00, 0x00)

    var area: Area = Area()
    //> DisableScreenFlag     = $0774
    var disableScreenFlag: Boolean = false
}


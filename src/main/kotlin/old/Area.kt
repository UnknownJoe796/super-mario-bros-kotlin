package com.ivieleague.smbtranslation.old

class Area() {
    var horizontalScroll: Byte = 0x0 //      = $073f
    var verticalScroll: Byte = 0x0 //        = $0740
    //    TimerControl          = $0747
    var timerControl: Byte = 0x0
    //    FrameCounter          = $09
    var frameCounter: Byte = 0x0
    //    Sprite0HitDetectFlag  = $0722
    var sprite0HitDetectFlag: Boolean = false
}
package com.ivieleague.smbtranslation.nes

import com.ivieleague.smbtranslation.utils.JoypadBits

typealias TwoBits = Byte
typealias ThreeBits = Byte
typealias Nibble = Byte
typealias FiveBits = Byte
typealias SevenBits = Byte
typealias ElevenBits = Byte
typealias VramAddress = Short


class Inputs {
    var joypadPort1: JoypadBits = JoypadBits(0)
    var joypadPort2: JoypadBits = JoypadBits(0)
}

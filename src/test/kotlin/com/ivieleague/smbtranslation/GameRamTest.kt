package com.ivieleague.smbtranslation

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRamTest {
    @Test
    fun testClear() {
        val system = System()
        system.ram.worldNumber = Constants.World8
        system.ram.reset(0x75f..0x75f)
        assertEquals(0, system.ram.worldNumber)
    }
}
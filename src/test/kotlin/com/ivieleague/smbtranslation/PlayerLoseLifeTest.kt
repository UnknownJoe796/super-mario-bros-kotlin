package com.ivieleague.smbtranslation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerLoseLifeTest {

    @Test
    fun gameOverWhenNoLives() {
        val system = System()
        // Pre-set state
        system.ram.numberofLives = 0x00
        system.ram.operMode = OperMode.Game // ensure not already GameOver
        system.ram.operModeTask = 0x12
        system.ram.sprite0HitDetectFlag = true
        system.ram.disableScreenFlag = false
        system.ram.eventMusicQueue = 0

        system.playerLoseLife()

        // disable screen and clear sprite0 hit detect
        assertTrue(system.ram.disableScreenFlag)
        assertFalse(system.ram.sprite0HitDetectFlag)
        // Silence queued
        assertEquals(Constants.Silence, system.ram.eventMusicQueue)
        // Lives underflowed to 0xFF
        assertEquals(0xFF.toByte(), system.ram.numberofLives)
        // Game over mode set and task reset
        assertEquals(OperMode.GameOver, system.ram.operMode)
        assertEquals(0x00, system.ram.operModeTask)
    }

    @Test
    fun stillInGameSetsHalfwayPageBasedOnWorldLevelAndScreen() {
        val system = System()
        system.ram.numberofLives = 2
        system.ram.worldNumber = Constants.World1 // 0
        // Level -1 (0): use high nybble of first byte ($56 >> 4 = 5)
        system.ram.levelNumber = Constants.Level1 // 0
        system.ram.screenLeftPageLoc = 5 // equal -> set
        system.playerLoseLife()
        assertEquals(1, system.ram.numberofLives) // decremented
        assertEquals(5.toByte(), system.ram.halfwayPage)

        // Level -2 (1): use low nybble of first byte ($56 & 0x0F = 6)
        val system2 = System()
        system2.ram.numberofLives = 5
        system2.ram.worldNumber = Constants.World1 // 0
        system2.ram.levelNumber = Constants.Level2 // 1
        system2.ram.screenLeftPageLoc = 4 // 6 > 4 -> start at beginning
        system2.playerLoseLife()
        assertEquals(4, system2.ram.numberofLives)
        assertEquals(0.toByte(), system2.ram.halfwayPage)

        // Level -3 (2): second byte for world 1 is $40 -> high nybble 4
        val system3 = System()
        system3.ram.numberofLives = 9
        system3.ram.worldNumber = Constants.World1 // 0
        system3.ram.levelNumber = Constants.Level3 // 2
        system3.ram.screenLeftPageLoc = 7 // 4 <= 7 -> set 4
        system3.playerLoseLife()
        assertEquals(8, system3.ram.numberofLives)
        assertEquals(4.toByte(), system3.ram.halfwayPage)
    }
}

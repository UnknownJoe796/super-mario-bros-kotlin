package com.ivieleague.smbtranslation

import kotlin.test.Test
import kotlin.test.assertEquals

class SpriteShufflerTest {
    @Test
    fun `spriteShuffler updates SprDataOffset, cycles amount offset, and sets misc offsets`() {
        val system = System()
        val ram = system.ram

        // Initialize SprShuffleAmt table and current offset
        // Use index 1 with amount 0x10
        ram.sprShuffleAmtOffset = 0x01
        ram.sprShuffleAmt[0] = 0x00
        ram.sprShuffleAmt[1] = 0x10
        ram.sprShuffleAmt[2] = 0x08 // arbitrary; not used in this test

        // Prepare sprDataOffsets with targeted values
        // Below threshold (0x27), at threshold (0x28), and high with carry (0xF5)
        ram.sprDataOffsets[0] = 0x27
        ram.sprDataOffsets[1] = 0x28.toByte()
        ram.sprDataOffsets[2] = 0xF5.toByte()

        // Values for misc computation (indices 5..7)
        ram.sprDataOffsets[5] = 0x10
        ram.sprDataOffsets[6] = 0x20
        ram.sprDataOffsets[7] = 0x30

        // Sanity-fill remaining entries but they are not critical
        for (i in 3..4) ram.sprDataOffsets[i] = 0
        for (i in 8..14) ram.sprDataOffsets[i] = 0

        // Execute the routine
        system.spriteShuffler()

        // Validate SprDataOffset modifications
        // 0x27 < 0x28 => unchanged
        assertEquals(0x27, ram.sprDataOffsets[0].toInt() and 0xFF)
        // 0x28 + 0x10 => 0x38
        assertEquals(0x38, ram.sprDataOffsets[1].toInt() and 0xFF)
        // 0xF5 + 0x10 => 0x05 with carry, then +0x28 => 0x2D
        assertEquals(0x2D, ram.sprDataOffsets[2].toInt() and 0xFF)

        // Validate SprShuffleAmtOffset cycles by +1 modulo 3
        assertEquals(0x02, ram.sprShuffleAmtOffset.toInt() and 0xFF)

        // Validate Misc_SprDataOffset layout based on sprDataOffsets[5..7] after shuffle
        // Note sprDataOffsets[7] has been updated by shuffle: 0x30 + 0x10 => 0x40
        val expectedMisc = intArrayOf(
            0x10, 0x18, 0x20, // from index 5
            0x20, 0x28, 0x30, // from index 6
            0x40, 0x48, 0x50  // from index 7 (post-shuffle)
        )
        for (i in expectedMisc.indices) {
            assertEquals(expectedMisc[i], ram.miscSprDataOffsets[i].toInt() and 0xFF, "miscSprDataOffsets[$i]")
        }
    }
}

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.bit
import com.ivieleague.smbtranslation.utils.bitRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitOps2Test {

    @Test
    fun `bit(index) reads correct flags at low and high positions`() {
        val b: Byte = 0b1000_0001.toByte()
        assertTrue(b.bit(0))
        assertTrue(b.bit(7))
        assertFalse(b.bit(1))
        assertFalse(b.bit(6))
    }

    @Test
    fun `bit(index, value) sets and clears without affecting other bits`() {
        var b: Byte = 0b0001_0000.toByte()

        // Set bit 0 -> only bit 0 and 4 should be set
        b = b.bit(0, true)
        assertEquals(0b0001_0001, b)
        assertTrue(b.bit(4))
        assertTrue(b.bit(0))

        // Clear bit 4 -> only bit 0 remains
        b = b.bit(4, false)
        assertEquals(0b0000_0001, b)
        assertFalse(b.bit(4))
        assertTrue(b.bit(0))

        // Set bit 7 -> high bit set alongside bit 0
        b = b.bit(7, true)
        assertEquals(0b1000_0001.toByte(), b)
        assertTrue(b.bit(7))
        assertTrue(b.bit(0))
    }

    @Test
    fun `bitRange(start,end) extracts the expected range`() {
        val b: Byte = 0b1101_0110.toByte()
        // Extract bits [1..3] -> original bits are (bit1..bit3) = 1,1,0 -> numeric 0b011 (3)
        assertEquals(0b011, b.bitRange(1, 3).toInt())
        // Extract bits [4..7] -> 0b1101 (13)
        assertEquals(0b1101, b.bitRange(4, 7).toInt())
        // Single-bit range [0..0]
        assertEquals(0b0, b.bitRange(0, 0).toInt())
        // Single-bit range [7..7]
        assertEquals(0b1, b.bitRange(7, 7).toInt())
    }

    @Test
    fun `bitRange(start,end,value) writes within range and masks overflow`() {
        // Start with 0b1010_1010
        var b: Byte = 0b1010_1010.toByte()

        // Write 0b011 into bits [1..3] -> expect bits 1..3 = 011; others unchanged
        b = b.bitRange(1, 3, 0b011)
        assertEquals(0b1010_0110.toByte(), b)

        // Write overflow value 0b1111 into [4..6] (width=3) -> only low 3 bits (0b111) used
        b = b.bitRange(4, 6, 0b1111)
        assertEquals(0b1111_0110.toByte(), b)

        // Clear a high single bit range [7..7]
        b = b.bitRange(7, 7, 0b0)
        assertEquals(0b0111_0110, b)

        // Set lowest single bit range [0..0]
        b = b.bitRange(0, 0, 0b1)
        assertEquals(0b0111_0111, b)
    }
}

package com.ivieleague.smbtranslation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectSmokeTest {

    @Test
    fun `GameRam Stack push and pop maintain LIFO order`() {
        val ram = GameRam()
        ram.stack.clear()
        ram.stack.push(0x01)
        ram.stack.push(0x7F)
        ram.stack.push(0x00)
        assertEquals(0x00, ram.stack.pop())
        assertEquals(0x7F, ram.stack.pop())
        assertEquals(0x01, ram.stack.pop())
        assertEquals(0, ram.stack.currentIndex)
    }

    @Test
    fun `BitRangeAccess2 correctly sets and gets bit ranges`() {
        // Create a simple ByteAccess backing store
        val backing = object : ByteAccess { override var value: Byte = 0 }

        // Define a delegating holder that exposes a 2-bit field (bits 0..1)
        class Holder(access: ByteAccess) {
            var field: Byte by BitRangeAccess2(access, 0, 1)
        }
        val h = Holder(backing)

        // Set 2-bit value and verify both delegate and backing store reflect it
        h.field = 0b10
        assertEquals(0b10, h.field.toInt())
        assertEquals(0b10, (backing.value.toInt() and 0b11))

        // Overwrite with 0b01 and ensure masking works
        h.field = 0b01
        assertEquals(0b01, h.field.toInt())
        assertEquals(0b01, (backing.value.toInt() and 0b11))

        // Ensure higher bits are unaffected by field writes
        backing.value = (backing.value.toInt() or (1 shl 7)).toByte() // set bit 7 manually
        assertTrue(backing.value.toInt() and (1 shl 7) != 0)
        assertEquals(0b01, h.field.toInt())

        // Changing the field should not clear bit 7
        h.field = 0b10
        assertTrue(backing.value.toInt() and (1 shl 7) != 0)
        assertEquals(0b10, (backing.value.toInt() and 0b11))
    }
}

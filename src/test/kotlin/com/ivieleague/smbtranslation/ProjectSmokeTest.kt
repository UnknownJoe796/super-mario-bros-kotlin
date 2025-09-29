package com.ivieleague.smbtranslation

import kotlin.test.Test
import kotlin.test.assertEquals

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
}

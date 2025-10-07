package com.ivieleague.smbtranslation

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DigitsMathRoutineTest {

    @Test
    fun simpleAdditionNoCarry() {
        val system = System()
        val ram = system.ram
        ram.operMode = OperMode.Game

        val digits = byteArrayOf(0, 0, 0, 0, 0, 0)
        // add 5 to the least significant digit (x=5 corresponds to y=last)
        ram.digitModifier[5] = 5

        system.digitsMathRoutine(digits)

        assertContentEquals(byteArrayOf(0, 0, 0, 0, 0, 5), digits)
        // modifiers are cleared
        for (i in 0..5) assertEquals(0, ram.digitModifier[i].toInt())
    }

    @Test
    fun carryPropagationAcrossDigits() {
        val system = System()
        val ram = system.ram
        ram.operMode = OperMode.Game

        val digits = byteArrayOf(0, 0, 0, 0, 9, 9) // ..99
        ram.digitModifier[5] = 2 // add 2 to ones place -> 11 -> 1 and carry to tens

        system.digitsMathRoutine(digits)

        // ..99 + 02 => ..01 with carry making tens 0 and hundreds +1 => ..001?
        // Step by step: ones 9+2=11 -> 1, carry+1 to tens; tens 9+1=10 -> 0, carry+1 to hundreds
        // hundreds 0+1=1
        assertContentEquals(byteArrayOf(0, 0, 0, 1, 0, 1), digits)
        for (i in 0..5) assertEquals(0, ram.digitModifier[i].toInt())
    }

    @Test
    fun carryPropagationAcrossDigitsShort() {
        val system = System()
        val ram = system.ram
        ram.operMode = OperMode.Game

        val digits = byteArrayOf(0, 9) // ..99
        ram.digitModifier[5] = 2 // add 2 to ones place -> 11 -> 1 and carry to tens

        system.digitsMathRoutine(digits)

        // ..99 + 02 => ..01 with carry making tens 0 and hundreds +1 => ..001?
        // Step by step: ones 9+2=11 -> 1, carry+1 to tens; tens 9+1=10 -> 0, carry+1 to hundreds
        // hundreds 0+1=1
        assertContentEquals(byteArrayOf(1, 1), digits)
    }

    @Test
    fun borrowPropagationAcrossDigits() {
        val system = System()
        val ram = system.ram
        ram.operMode = OperMode.Game

        val digits = byteArrayOf(0, 0, 0, 1, 0, 2) // ..102
        ram.digitModifier[5] = (-3).toByte() // subtract 3 from ones: 2-3 => borrow -> 9 and decrement tens by 1

        system.digitsMathRoutine(digits)

        // ones becomes 9, tens becomes -1 applied then added to 0 => 0 + (-1) => borrow again
        // tens result 9, hundreds decremented by 1: 1-1 => 0
        assertContentEquals(byteArrayOf(0, 0, 0, 0, 9, 9), digits)
        for (i in 0..5) assertEquals(0, ram.digitModifier[i].toInt())
    }
}

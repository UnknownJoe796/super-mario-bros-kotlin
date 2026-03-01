// by Claude
package com.ivieleague.smbtranslation.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinaryInterpreterTest {

    @Test
    fun `simple program loads value, stores to zero page, reads it back`() {
        val interp = BinaryInterpreter6502()

        // Program: LDA #$42, STA $00, LDA $00, BRK
        // Opcodes: A9 42 85 00 A5 00 00
        val program = byteArrayOf(
            0xA9.toByte(), 0x42.toByte(),  // LDA #$42
            0x85.toByte(), 0x00.toByte(),  // STA $00
            0xA5.toByte(), 0x00.toByte(),  // LDA $00
            0x00.toByte()                  // BRK
        )

        // Load program at $8000
        interp.memory.loadProgram(0x8000, program)

        // Set the reset vector at $FFFC/$FFFD to point to $8000
        interp.memory.writeWord(0xFFFC, 0x8000.toUShort())

        // BRK vector: point to a halting address so we don't run forever.
        // BRK jumps to the IRQ vector at $FFFE. We'll point it to a location with
        // another BRK, but we'll use maxCycles to prevent infinite loops.
        // Actually, we just need to run and then check state after the BRK is hit.
        // BRK pushes PC+2 and status, then jumps to [$FFFE]. We need a valid vector.
        // Point IRQ vector to a NOP-BRK sequence so execution stops quickly.
        interp.memory.writeByte(0xFFF0, 0xEA.toUByte())  // NOP at $FFF0
        interp.memory.writeWord(0xFFFE, 0xFFF0.toUShort())  // IRQ vector -> $FFF0

        // Reset and run
        interp.reset()
        assertEquals(0x8000.toUShort(), interp.cpu.PC, "PC should be set to reset vector")

        // Step through the program instruction by instruction
        // Step 1: LDA #$42
        interp.step()
        assertEquals(0x42.toUByte(), interp.cpu.A, "A should be 0x42 after LDA #\$42")

        // Step 2: STA $00
        interp.step()
        assertEquals(0x42.toUByte(), interp.memory.readByte(0x00), "Memory[$00] should be 0x42 after STA \$00")

        // Step 3: LDA $00
        interp.step()
        assertEquals(0x42.toUByte(), interp.cpu.A, "A should still be 0x42 after LDA \$00")

        // Step 4: BRK - this will push PC and status, then jump to IRQ vector
        interp.step()
        assertTrue(interp.cpu.I, "Interrupt disable flag should be set after BRK")

        // Final state verification
        assertEquals(0x42.toUByte(), interp.cpu.A, "A register should contain 0x42")
        assertEquals(0x42.toUByte(), interp.memory.readByte(0x00), "Memory at \$00 should contain 0x42")
    }

    @Test
    fun `run with maxCycles stops execution`() {
        val interp = BinaryInterpreter6502()

        // Infinite loop: JMP $8000
        val program = byteArrayOf(
            0x4C.toByte(), 0x00.toByte(), 0x80.toByte()  // JMP $8000
        )

        interp.memory.loadProgram(0x8000, program)
        interp.memory.writeWord(0xFFFC, 0x8000.toUShort())

        interp.reset()
        val cycles = interp.run(maxCycles = 100)

        // Should have run some cycles and stopped due to maxCycles limit
        assertTrue(cycles >= 100, "Should have consumed at least 100 cycles, got $cycles")
    }

    @Test
    fun `flags are set correctly for zero result`() {
        val interp = BinaryInterpreter6502()

        // LDA #$00, BRK
        val program = byteArrayOf(
            0xA9.toByte(), 0x00.toByte(),  // LDA #$00
            0x00.toByte()                  // BRK
        )

        interp.memory.loadProgram(0x8000, program)
        interp.memory.writeWord(0xFFFC, 0x8000.toUShort())
        interp.memory.writeWord(0xFFFE, 0x8002.toUShort())  // IRQ vector

        interp.reset()
        interp.step()  // LDA #$00

        assertEquals(0x00.toUByte(), interp.cpu.A)
        assertTrue(interp.cpu.Z, "Zero flag should be set when loading 0")
        assertTrue(!interp.cpu.N, "Negative flag should be clear when loading 0")
    }

    @Test
    fun `flags are set correctly for negative result`() {
        val interp = BinaryInterpreter6502()

        // LDA #$80, BRK
        val program = byteArrayOf(
            0xA9.toByte(), 0x80.toByte(),  // LDA #$80
            0x00.toByte()                  // BRK
        )

        interp.memory.loadProgram(0x8000, program)
        interp.memory.writeWord(0xFFFC, 0x8000.toUShort())
        interp.memory.writeWord(0xFFFE, 0x8002.toUShort())

        interp.reset()
        interp.step()  // LDA #$80

        assertEquals(0x80.toUByte(), interp.cpu.A)
        assertTrue(!interp.cpu.Z, "Zero flag should be clear when loading 0x80")
        assertTrue(interp.cpu.N, "Negative flag should be set when loading 0x80")
    }
}

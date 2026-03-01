@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.decompiler6502tokotlin.smb

import com.ivieleague.decompiler6502tokotlin.codegen.*

/**
 * Main entry point for the decompiled Super Mario Bros.
 *
 * This initializes the system and starts the game loop.
 * The game runs in an infinite loop, processing frames until
 * the NMI (vertical blank) interrupt updates the screen.
 */
fun main() {
    // Initialize CPU state
    resetCPU()
    clearMemory()

    // Load ROM data into memory
    initializeRomData()

    // Start the game - this is the reset vector entry point
    // In the original NES, the CPU reads the reset vector from 0xFFFC-0xFFFD
    // and jumps to that address (0x8000 for SMB)
    func_0()  // Entry point at the beginning of ROM
}

/**
 * NMI handler - called every frame during vertical blank.
 * This updates PPU, handles input, and calls the main game loop.
 */
fun nmiHandler() {
    // TODO: Implement NMI handler
    // This should push registers, update PPU, call game logic, pop registers
}

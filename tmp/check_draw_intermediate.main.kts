val rom = java.io.File("smb.nes")
if (!rom.exists()) { println("ROM not found"); System.exit(1) }
val data = rom.readBytes()

fun prgByte(addr: Int): Int = data[16 + (addr - 0x8000)].toInt() and 0xFF

// DrawPlayer_Intermediate at $EFA4
println("DrawPlayer_Intermediate raw bytes at \$EFA4:")
for (i in 0 until 32) {
    print(String.format("%02X ", prgByte(0xEFA4 + i)))
    if ((i + 1) % 16 == 0) println()
}
println()

// Simple 6502 disassembly of the relevant instructions
var pc = 0xEFA4
val opNames = mapOf(
    0xA2 to "LDX", 0xBD to "LDA abs,X", 0x95 to "STA zp,X", 0xCA to "DEX",
    0x10 to "BPL", 0xA0 to "LDY", 0x20 to "JSR", 0xB9 to "LDA abs,Y",
    0x09 to "ORA #", 0x99 to "STA abs,Y", 0x60 to "RTS"
)
println("Disassembly:")
for (line in 0 until 15) {
    val op = prgByte(pc)
    when (op) {
        0xA2 -> { val v = prgByte(pc + 1); println("  \$${String.format("%04X", pc)}: LDX #\$${String.format("%02X", v)}"); pc += 2 }
        0xA0 -> { val v = prgByte(pc + 1); println("  \$${String.format("%04X", pc)}: LDY #\$${String.format("%02X", v)}"); pc += 2 }
        0xBD -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: LDA \$${String.format("%04X", (hi shl 8) or lo)},X"); pc += 3 }
        0xB9 -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: LDA \$${String.format("%04X", (hi shl 8) or lo)},Y"); pc += 3 }
        0x95 -> { val v = prgByte(pc+1); println("  \$${String.format("%04X", pc)}: STA \$${String.format("%02X", v)},X"); pc += 2 }
        0x99 -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: STA \$${String.format("%04X", (hi shl 8) or lo)},Y"); pc += 3 }
        0xCA -> { println("  \$${String.format("%04X", pc)}: DEX"); pc += 1 }
        0x10 -> { val v = prgByte(pc+1); val target = pc + 2 + (if (v > 127) v - 256 else v); println("  \$${String.format("%04X", pc)}: BPL \$${String.format("%04X", target)}"); pc += 2 }
        0x20 -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: JSR \$${String.format("%04X", (hi shl 8) or lo)}"); pc += 3 }
        0x09 -> { val v = prgByte(pc+1); println("  \$${String.format("%04X", pc)}: ORA #\$${String.format("%02X", v)}"); pc += 2 }
        0x60 -> { println("  \$${String.format("%04X", pc)}: RTS"); pc += 1 }
        else -> { println("  \$${String.format("%04X", pc)}: ??? opcode \$${String.format("%02X", op)}"); pc += 1 }
    }
}

// Also check DrawPlayerLoop address and the DrawSpriteObject to see if Y gets modified
println()
println("DrawPlayerLoop at \$EFDC:")
for (i in 0 until 16) {
    print(String.format("%02X ", prgByte(0xEFDC + i)))
}
println()
println("DrawSpriteObject at \$F282:")
for (i in 0 until 50) {
    print(String.format("%02X ", prgByte(0xF282 + i)))
    if ((i + 1) % 16 == 0) println()
}
println()

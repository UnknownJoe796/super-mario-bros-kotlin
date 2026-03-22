val rom = java.io.File("smb.nes")
if (!rom.exists()) { println("ROM not found"); System.exit(1) }
val data = rom.readBytes()

fun prgByte(addr: Int): Int = data[16 + (addr - 0x8000)].toInt() and 0xFF

// ChkForPlayerAttrib at $F0E9
// C_S_IGAtt at $F117
// KilledAtt at $F105
// ExPlyrAt at $F129

println("ChkForPlayerAttrib at \$F0E9:")
for (i in 0 until 0x42) {
    val addr = 0xF0E9 + i
    val b = prgByte(addr)
    if (i % 16 == 0 && i > 0) println()
    print(String.format("%02X ", b))
}
println()

// Manual disassembly of key section
println("\nDisassembly:")
var pc = 0xF0E9
for (line in 0 until 30) {
    if (pc > 0xF12A) break
    val op = prgByte(pc)
    when (op) {
        0xA0 -> { val v = prgByte(pc+1); println("  \$${String.format("%04X", pc)}: LDY #\$${String.format("%02X", v)}"); pc += 2 }
        0xAC -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: LDY \$${String.format("%04X", (hi shl 8) or lo)}"); pc += 3 }
        0xAD -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: LDA \$${String.format("%04X", (hi shl 8) or lo)}"); pc += 3 }
        0xB9 -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: LDA \$${String.format("%04X", (hi shl 8) or lo)},Y"); pc += 3 }
        0x99 -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: STA \$${String.format("%04X", (hi shl 8) or lo)},Y"); pc += 3 }
        0xC9 -> { val v = prgByte(pc+1); println("  \$${String.format("%04X", pc)}: CMP #\$${String.format("%02X", v)}"); pc += 2 }
        0xF0 -> { val v = prgByte(pc+1); val target = pc + 2 + (if (v > 127) v - 256 else v); println("  \$${String.format("%04X", pc)}: BEQ \$${String.format("%04X", target)}"); pc += 2 }
        0xD0 -> { val v = prgByte(pc+1); val target = pc + 2 + (if (v > 127) v - 256 else v); println("  \$${String.format("%04X", pc)}: BNE \$${String.format("%04X", target)}"); pc += 2 }
        0x29 -> { val v = prgByte(pc+1); println("  \$${String.format("%04X", pc)}: AND #\$${String.format("%02X", v)}"); pc += 2 }
        0x09 -> { val v = prgByte(pc+1); println("  \$${String.format("%04X", pc)}: ORA #\$${String.format("%02X", v)}"); pc += 2 }
        0x4C -> { val lo = prgByte(pc+1); val hi = prgByte(pc+2); println("  \$${String.format("%04X", pc)}: JMP \$${String.format("%04X", (hi shl 8) or lo)}"); pc += 3 }
        0x60 -> { println("  \$${String.format("%04X", pc)}: RTS"); pc += 1 }
        else -> { println("  \$${String.format("%04X", pc)}: ??? \$${String.format("%02X", op)}"); pc += 1 }
    }
}

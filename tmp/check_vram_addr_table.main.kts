val rom = java.io.File("smb.nes")
if (!rom.exists()) { println("ROM not found"); System.exit(1) }
val data = rom.readBytes()

// VRAM_AddrTable_Low at PRG $805A (PRG starts at $8000 in CPU, maps to file offset 16 + 0)
// Actually SMB has 2 PRG banks = 32KB, mapped to $8000-$FFFF
// File offset = 16 (header) + (addr - $8000)
fun prgByte(addr: Int): Int = data[16 + (addr - 0x8000)].toInt() and 0xFF

println("VRAM_AddrTable_Low at \$805A:")
for (i in 0 until 12) {
    val lo = prgByte(0x805A + i)
    print(String.format("%02X ", lo))
}
println()

println("VRAM_AddrTable_High at \$806D:")
for (i in 0 until 12) {
    val hi = prgByte(0x806D + i)
    print(String.format("%02X ", hi))
}
println()

println("\nFull address table:")
for (i in 0 until 12) {
    val lo = prgByte(0x805A + i)
    val hi = prgByte(0x806D + i)
    val addr = (hi shl 8) or lo
    println("  Index $i: \$${String.format("%04X", addr)}")
}

// Also check VRAM_Buffer1 address
// VRAM_Buffer1 is defined as $0301 in standard SMB disassembly
println("\nVRAM_Buffer_Offset at \$8080:")
for (i in 0 until 4) {
    val v = prgByte(0x8080 + i)
    print(String.format("%02X ", v))
}
println()

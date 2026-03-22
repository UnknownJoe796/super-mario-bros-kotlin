val rom = java.io.File("smb.nes")
if (!rom.exists()) { println("ROM not found"); System.exit(1) }
val data = rom.readBytes()

fun prgByte(addr: Int): Int = data[16 + (addr - 0x8000)].toInt() and 0xFF

// MushroomIconData address
// From the assembly: MushroomIconData followed by DrawMushroomIcon
// Let me find it by searching for the bytes $07, $22, $49, $83, $CE, $24, $24, $00
// which are the known data values

// DrawMushroomIcon - let me find it in the constants
// Actually let me search for the icon data pattern
println("Searching for MushroomIconData pattern in PRG...")
for (addr in 0x8000..0xFFF0) {
    if (prgByte(addr) == 0x07 && prgByte(addr+1) == 0x22 && prgByte(addr+2) == 0x49 &&
        prgByte(addr+3) == 0x83 && prgByte(addr+4) == 0xCE && prgByte(addr+5) == 0x24 &&
        prgByte(addr+6) == 0x24 && prgByte(addr+7) == 0x00) {
        println("  Found at \$${String.format("%04X", addr)}")
    }
}

// Let me also look at what VRAM_Buffer_Offset values are used
println("\nVRAM_Buffer_Offset data at \$8080:")
for (i in 0 until 4) {
    print(String.format("%02X ", prgByte(0x8080 + i)))
}
println()

// VRAM_Buffer1 = $0301, VRAM_Buffer1_Offset = VRAM_Buffer1 - 1 = $0300
println("\nKey addresses:")
println("  VRAM_Buffer1 = \$0301")
println("  VRAM_Buffer1 - 1 = \$0300")
println("  VRAM_Buffer_AddrTable[0] = \$0301 (index 0)")
println("  VRAM_Buffer_AddrTable[5] = \$0300 (index 5)")

// Now let me check ClearBuffersDrawIcon to understand what exactly gets cleared
// and then how DrawMushroomIcon writes to the buffer
println("\nSearching for ClearBuffersDrawIcon...")
val clearBuffAddr = 0x870A // from the constants if available, or search
// Let me check smb-constants.kt content
println("Looking for ClearBuffersDrawIcon constant...")

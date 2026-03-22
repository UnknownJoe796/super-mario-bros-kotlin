val rom = java.io.File("smb.nes")
if (!rom.exists()) {
    println("ROM not found")
    System.exit(1)
}
val data = rom.readBytes()
val prgSize = data[4].toInt() * 16 * 1024
val chrOffset = 16 + prgSize

val chrData = data.copyOfRange(chrOffset, chrOffset + 8192)
println("CHR offset in file: 0x${chrOffset.toString(16)}")
println("Title screen data starts at CHR 0x1EC0:")
println("First 32 bytes:")
for (i in 0 until 32) {
    print("${String.format("%02X", chrData[0x1EC0 + i].toInt() and 0xFF)} ")
    if ((i + 1) % 16 == 0) println()
}
println()
println("Byte at 0x1EC0 (NES stores at \$0300): 0x${String.format("%02X", chrData[0x1EC0].toInt() and 0xFF)}")
println("Byte at 0x1EC1 (NES stores at \$0301 = VRAM_Buffer1 start): 0x${String.format("%02X", chrData[0x1EC1].toInt() and 0xFF)}")

// Parse as VRAM buffer starting from byte 0 (Kotlin's current behavior)
println("\n--- Parsing from byte 0 (current Kotlin behavior) ---")
var i = 0
var recordNum = 0
val bytes = chrData.copyOfRange(0x1EC0, 0x1EC0 + 0x13A)
while (i < bytes.size && recordNum < 5) {
    val hi = bytes[i].toInt() and 0xFF
    if (hi == 0) { println("  Record $recordNum: TERMINATOR at offset $i"); break }
    if (i + 2 >= bytes.size) break
    val lo = bytes[i + 1].toInt() and 0xFF
    val ctrl = bytes[i + 2].toInt() and 0xFF
    val addr = (hi shl 8) or lo
    val length = ctrl and 0x3F
    val vert = (ctrl and 0x80) != 0
    val repeat = (ctrl and 0x40) != 0
    val dataSize = if (repeat) (if (length > 0) 1 else 0) else length
    println("  Record $recordNum: addr=\$${String.format("%04X", addr)} ctrl=\$${String.format("%02X", ctrl)} len=$length vert=$vert repeat=$repeat dataBytes=$dataSize headerAt=$i")
    i += 3 + dataSize
    recordNum++
}

println("\n--- Parsing from byte 1 (NES actual behavior) ---")
i = 1
recordNum = 0
while (i < bytes.size && recordNum < 5) {
    val hi = bytes[i].toInt() and 0xFF
    if (hi == 0) { println("  Record $recordNum: TERMINATOR at offset $i"); break }
    if (i + 2 >= bytes.size) break
    val lo = bytes[i + 1].toInt() and 0xFF
    val ctrl = bytes[i + 2].toInt() and 0xFF
    val addr = (hi shl 8) or lo
    val length = ctrl and 0x3F
    val vert = (ctrl and 0x80) != 0
    val repeat = (ctrl and 0x40) != 0
    val dataSize = if (repeat) (if (length > 0) 1 else 0) else length
    println("  Record $recordNum: addr=\$${String.format("%04X", addr)} ctrl=\$${String.format("%02X", ctrl)} len=$length vert=$vert repeat=$repeat dataBytes=$dataSize headerAt=$i")
    i += 3 + dataSize
    recordNum++
}

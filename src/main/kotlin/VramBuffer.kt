package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.VramBufferControl

class VramBuffer() {
}

data class VramBufferInstruction(
    val address: Short,
    val control: VramBufferControl,
    val content: List<Byte>
)

class Ppu2 {
    //    $0000-$0FFF 	$1000 	Pattern table 0 	Cartridge
    //    $1000-$1FFF 	$1000 	Pattern table 1 	Cartridge
    val patternTables = Array<PatternTable>(2) { PatternTable() }
    //    $2000-$23BF 	$03c0 	Nametable 0 	Cartridge
    //    $23C0-$23FF 	$0040 	Attribute table 0 	Cartridge
    //    $2400-$27BF 	$03c0 	Nametable 1 	Cartridge
    //    $27C0-$27FF 	$0040 	Attribute table 1 	Cartridge
    //    $2800-$2BBF 	$03c0 	Nametable 2 	Cartridge
    //    $2BC0-$2BFF 	$0040 	Attribute table 2 	Cartridge
    //    $2C00-$2FBF 	$03c0 	Nametable 3 	Cartridge
    //    $2FC0-$2FFF 	$0040 	Attribute table 3 	Cartridge
    val nameTables = Array<NameAndAttributeTable>(4) { NameAndAttributeTable() }
    //    $3000-$3EFF 	$0F00 	Unused 	Cartridge
    //    $3F00-$3F1F 	$0020 	Palette RAM indexes 	Internal to PPU
    val palettes = Array<Palette>(4) { Palette() }
    //    $3F20-$3FFF 	$00E0 	Mirrors of $3F00-$3F1F 	Internal to PPU

    class Palette()
    class PatternTable()
    class NameAndAttributeTable()
}
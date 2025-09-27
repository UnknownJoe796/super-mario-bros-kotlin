package com.ivieleague.smbtranslation


fun System.updateScreen(bufferToWrite: GameRam.VramBytes) {
    //> ;$00 - vram buffer address table low
    //> ;$01 - vram buffer address table high


    // Translate the VRAM update buffer format used by SMB into our PPU operations.
    // Buffer layout is a sequence of segments, terminated by a single 0x00 byte:
    // [highAddr][lowAddr][control/len][data ...] ... [0x00]
    // control/len byte: bit7=1 -> increment by 32 (vertical), bit7=0 -> increment by 1 (horizontal);
    //                    bit6=1 -> repeat the next data byte for "len" times; bit6=0 -> use next "len" bytes;
    //                    bits0-5 = length (0..63)

    // Reset PPU scroll latch via status read is not modeled; proceed to parsing.  //> ldx PPU_STATUS
    val bytes = bufferToWrite.bytes
    if (bytes.isEmpty()) {
        // No updates: initialize scroll registers to zero (A==0 in original)
        initScroll(0x00)        //> InitScroll / sta PPU_SCROLL_REG x2
        return
    }

    var i = 0
    // Process each segment until we hit the implicit 0x00 terminator (not included in bytes array)
    while (i < bytes.size) {
        val high = bytes[i]
        if (high == 0.toByte()) {
            // Terminator reached -> fall through to init scroll like original RTS path
            initScroll(0x00)
            return
        }
        if (i + 2 >= bytes.size) {
            // Malformed/incomplete segment; stop safely
            initScroll(0x00)
            return
        }
        val low = bytes[i + 1]
        val controlLen = bytes[i + 2].toInt() and 0xFF
        i += 3

        //> sta PPU_ADDRESS (high) / sta PPU_ADDRESS (low)
        // Set VRAM address
        val vramAddr = (((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)).toShort()
        ppu.setVramAddress(vramAddr)

        // Configure increment mode by adjusting the control register's drawVertical bit based on bit7
        //> asl (save), use d7 to choose increment step, jsr WritePPUReg1
        val incrementBy32 = (controlLen and 0x80) != 0
        // Use the mirror as base, modify drawVertical, and write to the PPU control register
        val ctrl = ram.mirrorPPUCTRLREG1.copy(drawVertical = incrementBy32)
        ppu.control = ctrl
        // Note: original code did not store back to mirror here; we mirror that behavior as well.

        //> asl / bcc GetLength (repeat flag), ora #%00000010 / iny
        val repeatMode = (controlLen and 0x40) != 0
        val length = controlLen and 0x3F

        if (repeatMode) {
            // Repeat single byte length times
            if (i >= bytes.size) {
                initScroll(0x00)
                return
            }
            val value = bytes[i]
            repeat(length) {
                //> RepeatByte: sta PPU_DATA (same value)
                ppu.writeVram(value)
            }
            // In 6502 code Y was incremented once before the loop for repeat mode, so move past the one data byte
            i += 1
        } else {
            // Write a run of distinct bytes
            var written = 0
            while (written < length) {
                if (i + written >= bytes.size) {
                    initScroll(0x00)
                    return
                }
                //> OutputToVRAM: iny / lda ($00),y / sta PPU_DATA
                ppu.writeVram(bytes[i + written])
                written++
            }
            i += length
        }

        // After each segment, the original code reinitialized the PPU address sequence:
        // set to $3F00, then wrote $00 to PPU_ADDRESS twice more (to reset the latch).
        // We approximate this by explicitly setting the address to $3F00, then to $0000.
        //> lda #$3f / sta PPU_ADDRESS ; lda #$00 / sta PPU_ADDRESS x3
        ppu.setVramAddress(0x3F00)
        ppu.setVramAddress(0x0000)

        // Loop to next segment (i already points to the next segment's first byte)
    }

    // Finished processing: initialize scroll registers to zero, matching trailing code path
    initScroll(0x00)
}
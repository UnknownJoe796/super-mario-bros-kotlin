package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.chr.OriginalRom
import kotlin.collections.listOf
import kotlin.experimental.and
import kotlin.experimental.xor


object GameTexts {
    val TopStatusBarLine = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 3,
            y = 2,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x16],  //M
                OriginalRom.backgrounds[0xa],  //A
                OriginalRom.backgrounds[0x1b],  //R
                OriginalRom.backgrounds[0x12],  //I
                OriginalRom.backgrounds[0x18]   //O
            )
        ),
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 18,
            y = 2,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x20],  //W
                OriginalRom.backgrounds[0x18],  //O
                OriginalRom.backgrounds[0x1b],  //R
                OriginalRom.backgrounds[0x15],  //L
                OriginalRom.backgrounds[0xd],  //D
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x1d],  //T
                OriginalRom.backgrounds[0x12],  //I
                OriginalRom.backgrounds[0x16],  //M
                OriginalRom.backgrounds[0xe]  //E
            )
        ),
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 8,
            y = 3,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x0],  //0
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x2e],  // coin
                OriginalRom.backgrounds[0x29],  // times symbol
            )
        ),
        BufferedPpuUpdate.BackgroundAttributeRepeat(
            nametable = 0,
            ax = 0,
            ay = 0,
            drawVertically = false,
            value = 0xaa.toUByte(),  //ground tile?
            repetitions = 63
        ),
        BufferedPpuUpdate.BackgroundAttributeString(
            nametable = 0,
            ax = 2,
            ay = 0,
            drawVertically = false,
            values = listOf(0xea.toUByte())  // ground tile?
        ),
    )
    val WorldLivesDisplay = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 13,
            y = 14,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x29],  // times symbol
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24]   //
            )
        ),
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 11,
            y = 10,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x20],  //W
                OriginalRom.backgrounds[0x18],  //O
                OriginalRom.backgrounds[0x1b],  //R
                OriginalRom.backgrounds[0x15],  //L
                OriginalRom.backgrounds[0xd],  //D
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x28],  //-
                OriginalRom.backgrounds[0x24],  //
            )
        ),
        BufferedPpuUpdate.BackgroundPatternRepeat(
            nametable = 0,
            x = 12,
            y = 16,
            drawVertically = false,
            pattern = OriginalRom.backgrounds[0x24],
            repetitions = 7
        ),
        BufferedPpuUpdate.BackgroundAttributeString(
            nametable = 0,
            ax = 4,
            ay = 3,
            drawVertically = false,
            values = listOf(0xba.toUByte())
        ),
    )
    val TwoPlayerTimeUp = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 13,
            y = 14,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x16],  //M
                OriginalRom.backgrounds[0xa],  //A
                OriginalRom.backgrounds[0x1b],  //R
                OriginalRom.backgrounds[0x12],  //I
                OriginalRom.backgrounds[0x18],  //O
            )
        ),
    )
    val OnePlayerTimeUp = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 12,
            y = 16,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x1d],  //T
                OriginalRom.backgrounds[0x12],  //I
                OriginalRom.backgrounds[0x16],  //M
                OriginalRom.backgrounds[0xe],  //E
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x1e],  //U
                OriginalRom.backgrounds[0x19]  //P
            )
        ),
    )
    val TwoPlayerGameOver = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 13,
            y = 14,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x16],  //M
                OriginalRom.backgrounds[0xa],  //A
                OriginalRom.backgrounds[0x1b],  //R
                OriginalRom.backgrounds[0x12],  //I
                OriginalRom.backgrounds[0x18],  //O
            )
        ),
    )
    val OnePlayerGameOver = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = 11,
            y = 16,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x10],  //G
                OriginalRom.backgrounds[0xa],  //A
                OriginalRom.backgrounds[0x16],  //M
                OriginalRom.backgrounds[0xe],  //E
                OriginalRom.backgrounds[0x24],  //
                OriginalRom.backgrounds[0x18],  //O
                OriginalRom.backgrounds[0x1f],  //V
                OriginalRom.backgrounds[0xe],  //E
                OriginalRom.backgrounds[0x1b]  //R
            )
        ),
    )
    val WarpZoneWelcome = listOf(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 1,
            x = 4,
            y = 12,
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[0x20],
                OriginalRom.backgrounds[0xe],
                OriginalRom.backgrounds[0x15],
                OriginalRom.backgrounds[0xc],
                OriginalRom.backgrounds[0x18],
                OriginalRom.backgrounds[0x16],
                OriginalRom.backgrounds[0xe],
                OriginalRom.backgrounds[0x24],
                OriginalRom.backgrounds[0x1d],
                OriginalRom.backgrounds[0x18],
                OriginalRom.backgrounds[0x24],
                OriginalRom.backgrounds[0x20],
                OriginalRom.backgrounds[0xa],
                OriginalRom.backgrounds[0x1b],
                OriginalRom.backgrounds[0x19],
                OriginalRom.backgrounds[0x24],
                OriginalRom.backgrounds[0x23],
                OriginalRom.backgrounds[0x18],
                OriginalRom.backgrounds[0x17],
                OriginalRom.backgrounds[0xe],
                OriginalRom.backgrounds[0x2b]
            )
        ),
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 1,
            x = 5,
            y = 17,
            drawVertically = false,
            patterns = listOf(OriginalRom.backgrounds[0x24])
        ),
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 1,
            x = 13,
            y = 17,
            drawVertically = false,
            patterns = listOf(OriginalRom.backgrounds[0x24])
        ),
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 1,
            x = 21,
            y = 17,
            drawVertically = false,
            patterns = listOf(OriginalRom.backgrounds[0x24])
        ),
        BufferedPpuUpdate.BackgroundAttributeRepeat(
            nametable = 1,
            ax = 1,
            ay = 3,
            drawVertically = false,
            value = 0xaa.toUByte(),
            repetitions = 6
        ),
        BufferedPpuUpdate.BackgroundAttributeRepeat(
            nametable = 1,
            ax = 1,
            ay = 4,
            drawVertically = false,
            value = 0xaa.toUByte(),
            repetitions = 5
        ),
    )

    val list = listOf(
        TopStatusBarLine, TopStatusBarLine,
        WorldLivesDisplay, WorldLivesDisplay,
        TwoPlayerTimeUp, OnePlayerTimeUp,
        TwoPlayerGameOver, OnePlayerGameOver,
        WarpZoneWelcome, WarpZoneWelcome,
    )

    val luigiName = listOf(
        OriginalRom.backgrounds[0x15],
        OriginalRom.backgrounds[0x1e],
        OriginalRom.backgrounds[0x12],
        OriginalRom.backgrounds[0x10],
        OriginalRom.backgrounds[0x12],
    )  // "LUIGI"

    // Warp zone numbers
    val warpZoneNumbers = listOf(
        listOf(OriginalRom.backgrounds[0x04], OriginalRom.backgrounds[0x03], OriginalRom.backgrounds[0x02]),
        listOf(OriginalRom.backgrounds[0x24], OriginalRom.backgrounds[0x05], OriginalRom.backgrounds[0x24]),
        listOf(OriginalRom.backgrounds[0x08], OriginalRom.backgrounds[0x07], OriginalRom.backgrounds[0x06])
    )
}

fun System.writeGameText(textNumber: Byte) {
    //> WriteGameText:
    //> pha                      ;save text number to stack
    // Cool.  We'll just leave it under 'textNumber'.

    //> asl
    //> tay                      ;multiply by 2 and use as offset
    var gameTextIndex = textNumber * 2
    //> cpy #$04                 ;if set to do top status bar or world/lives display,
    //> bcc LdGameText           ;branch to use current offset as-is
    if (gameTextIndex >= 0x04) {
        //> cpy #$08                 ;if set to do time-up or game over,
        //> bcc Chk2Players          ;branch to check players
        if (gameTextIndex >= 0x08) {
            //> ldy #$08                 ;otherwise warp zone, therefore set offset
            gameTextIndex = 0x8
        }
        //> Chk2Players:   lda NumberOfPlayers      ;check for number of players
        //> bne LdGameText           ;if there are two, use current offset to also print name
        if(ram.numberOfPlayers == 0.toByte()) {
            //> iny                      ;otherwise increment offset by one to not print name
            gameTextIndex++
        }
    }
    //> LdGameText:    ldx GameTextOffsets,y    ;get offset to message we want to print
    val text = GameTexts.list[gameTextIndex]
    //> ldy #$00

    //> GameTextLoop:  lda GameText,x           ;load message data
    //> cmp #$ff                 ;check for terminator
    //> beq EndGameText          ;branch to end text if found
    //> sta VRAM_Buffer1,y       ;otherwise write data to buffer
    //> inx                      ;and increment increment
    //> iny
    //> bne GameTextLoop         ;do this for 256 bytes if no terminator found
    //> EndGameText:   lda #$00                 ;put null terminator at end
    //> sta VRAM_Buffer1,y
    assert(ram.vRAMBuffer1.isEmpty())
    ram.vRAMBuffer1.addAll(text)

    //> pla                      ;pull original text number from stack
    //> tax
    //> cmp #$04                 ;are we printing warp zone?
    //> bcs PrintWarpZoneNumbers
    if(textNumber >= 0x4.toByte()) return printWarpZoneNumbers(textNumber)

    //> dex                      ;are we printing the world/lives display?
    //> bne CheckPlayerName      ;if not, branch to check player's name
    if(textNumber != 1.toByte()) return checkPlayerName(textNumber)

    // Otherwise, we're doing WorldLivesDisplay
    ram.vRAMBuffer1[0] = (ram.vRAMBuffer1[0] as BufferedPpuUpdate.BackgroundPatternString).let { original ->
        original.copy(patterns = original.patterns.toMutableList().apply {
            //> lda NumberofLives        ;otherwise, check number of lives
            //> clc                      ;and increment by one for display
            //> adc #$01
            var lifeNumber = ram.numberofLives + 1
            //> cmp #10                  ;more than 9 lives?
            //> bcc PutLives
            if(lifeNumber >= 10) {
                //> sbc #10                  ;if so, subtract 10 and put a crown tile
                lifeNumber -= 10
                //> ldy #$9f                 ;next to the difference...strange things happen if
                //> sty VRAM_Buffer1+7       ;the number of lives exceeds 19
                // Offset from original code by three bytes (2 for vram address bytes, 1, for control byte)
                this[7-3] = OriginalRom.backgrounds[0x9f]
            }

            //> PutLives:      sta VRAM_Buffer1+8
            // Offset from original code by three bytes (2 for vram address bytes, 1, for control byte)
            this[8-3] = OriginalRom.backgrounds[lifeNumber]
        })
    }

    ram.vRAMBuffer1[0] = (ram.vRAMBuffer1[0] as BufferedPpuUpdate.BackgroundPatternString).let { original ->
        original.copy(patterns = original.patterns.toMutableList().apply {
            //> ldy WorldNumber          ;write world and level numbers (incremented for display)
            //> iny                      ;to the buffer in the spaces surrounding the dash
            //> sty VRAM_Buffer1+19
            // Offset from original code by 13 bytes (2 for vram address bytes, 1, for control byte, 9 for previous instruction)
            this[19-13] = OriginalRom.backgrounds[ram.worldNumber + 1]

            //> ldy LevelNumber
            //> iny
            //> sty VRAM_Buffer1+21      ;we're done here
            this[21-13] = OriginalRom.backgrounds[ram.levelNumber + 1]
        })
    }
    //> rts
}


private fun System.checkPlayerName(originalTextNumber: Byte) {
    //> CheckPlayerName:
    //> lda NumberOfPlayers    ;check number of players
    //> beq ExitChkName        ;if only 1 player, leave
    if (ram.numberOfPlayers == 0.toByte()) return
    //> lda CurrentPlayer      ;load current player
    var currentPlayer = ram.currentPlayer
    //> dex                    ;check to see if current message number is for time up
    //> bne ChkLuigi
    //> ldy OperMode           ;check for game over mode
    //> cpy #GameOverModeValue
    //> beq ChkLuigi
    if (originalTextNumber == 2.toByte() && ram.operMode != OperMode.GameOver) {
        //> eor #%00000001         ;if not, must be time up, invert d0 to do other player
        currentPlayer = currentPlayer xor 0x01.toByte()
    }
    //> ChkLuigi:    lsr
    //> bcc ExitChkName        ;if mario is current player, do not change the name
    if(currentPlayer and 0x1.toByte() == 0.toByte()) return
    //> ldy #$04
    //> NameLoop:    lda LuigiName,y        ;otherwise, replace "MARIO" with "LUIGI"
    //> sta VRAM_Buffer1+3,y
    //> dey
    //> bpl NameLoop           ;do this until each letter is replaced
    ram.vRAMBuffer1[0] = (ram.vRAMBuffer1[0] as BufferedPpuUpdate.BackgroundPatternString).copy(
        patterns = GameTexts.luigiName
    )
    //> ExitChkName: rts
}

private fun System.printWarpZoneNumbers(originalTextNumber: Byte) {
    //> PrintWarpZoneNumbers:
    //> sbc #$04               ;subtract 4 and then shift to the left
    val warpZoneNumbers = GameTexts.warpZoneNumbers[originalTextNumber - 4]
    //> asl                    ;twice to get proper warp zone number
    //> asl                    ;offset
    //> tax
    //> ldy #$00
    //> WarpNumLoop: lda WarpZoneNumbers,x  ;print warp zone numbers into the
    //> sta VRAM_Buffer1+27,y  ;placeholders from earlier
    //> inx
    //> iny                    ;put a number in every fourth space
    //> iny
    //> iny
    //> iny
    //> cpy #$0c
    //> bcc WarpNumLoop
    //> lda #$2c               ;load new buffer pointer at end of message
    //> jmp SetVRAMOffset
    repeat(3) {
        ram.vRAMBuffer1[it + 1] = (ram.vRAMBuffer1[it + 1] as BufferedPpuUpdate.BackgroundPatternString).copy(
            patterns = listOf(warpZoneNumbers[it])
        )
    }
}

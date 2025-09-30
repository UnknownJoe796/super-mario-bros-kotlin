package com.ivieleague.smbtranslation

// TODO: Need to check ALL of these.
// Screen and intermediate display control tasks translated from SMB disassembly.
// These functions operate over the high-level PPU abstraction and GameRam state.

fun System.screenRoutines() {
    //> ScreenRoutines:
    //>       lda ScreenRoutineTask        ;run one of the following subroutines
    //>       jsr JumpEngine
    when (ram.screenRoutineTask.toInt() and 0xFF) {
        0x00 -> initScreen()
        0x01 -> setupIntermediate()
        0x02 -> writeTopStatusLine()
        0x03 -> writeBottomStatusLine()
        0x04 -> displayTimeUp()
        0x05 -> resetSpritesAndScreenTimer()
        0x06 -> displayIntermediate()
        0x07 -> resetSpritesAndScreenTimer()
        0x08 -> areaParserTaskControl()
        0x09 -> getAreaPalette()
        0x0A -> getBackgroundColor()
        0x0B -> getAlternatePalette1()
        0x0C -> drawTitleScreen()
        0x0D -> clearBuffersDrawIcon()
        0x0E -> writeTopScore()
        else -> Unit
    }
}

//-------------------------------------------------------------------------------------

private fun System.initScreen() {
    //> InitScreen:
    //>       jsr MoveAllSpritesOffscreen ;initialize all sprites including sprite #0
    moveAllSpritesOffscreen()
    //>       jsr InitializeNameTables    ;and erase both name and attribute tables
    initializeNameTables()
    //>       lda OperMode
    //>       beq NextSubtask             ;if mode still 0, do not load
    if (ram.operMode == OperMode.TitleScreen) return nextSubtask()
    //>       ldx #$03                    ;into buffer pointer
    //>       jmp SetVRAMAddr_A
    // We model SetVRAMAddr_A as selecting which VRAM update buffer address control to use.
    // The original sets a buffer pointer/index to 3 here; reflect by storing to vRAMBufferAddrCtrl.
    ram.vRAMBufferAddrCtrl = 0x03
    // In the original, SetVRAMAddr_A would use X to index a table of addresses; here we just advance the task.
    return nextSubtask()
}

//-------------------------------------------------------------------------------------

private fun System.setupIntermediate() {
    //> SetupIntermediate:
    //>       lda BackgroundColorCtrl  ;save current background color control
    //>       pha                      ;and player status to stack
    val savedBackgroundColorCtrl = ram.backgroundColorCtrl
    //>       lda PlayerStatus
    //>       pha
    val savedPlayerStatus = ram.playerStatus
    //>       lda #$00                 ;set background color to black
    //>       sta PlayerStatus         ;and player status to not fiery
    ram.playerStatus = 0x00
    //>       lda #$02                 ;this is the ONLY time background color control
    //>       sta BackgroundColorCtrl  ;is set to less than 4
    ram.backgroundColorCtrl = 0x02
    //>       jsr GetPlayerColors
    getPlayerColors()
    //>       pla                      ;we only execute this routine for
    //>       sta PlayerStatus         ;the intermediate lives display
    ram.playerStatus = savedPlayerStatus
    //>       pla                      ;and once we're done, we return bg
    //>       sta BackgroundColorCtrl  ;color ctrl and player status from stack
    ram.backgroundColorCtrl = savedBackgroundColorCtrl
    //>       jmp IncSubtask           ;then move onto the next task
    incSubtask()
}

//-------------------------------------------------------------------------------------
// Jump table targets referenced by ScreenRoutines that are not part of this issue.
// Keep them minimal no-ops for now to preserve structure and compile.

private fun System.writeTopStatusLine() {
    //> WriteTopStatusLine:
    //>       lda #$00          ;select main status bar
    //>       jsr WriteGameText ;output it
    writeGameText(0)
    //>       jmp IncSubtask    ;onto the next task
    incSubtask()
}

private fun System.writeBottomStatusLine() {
    //> WriteBottomStatusLine:
    //>       jsr GetSBNybbles        ;write player's score and coin tally to screen
    getSBNybbles()
    //>       ldx VRAM_Buffer1_Offset
    // Our high-level model appends directly to vRAMBuffer1; we do not track raw offsets.
    //>       lda #$20                ;write address for world-area number on screen
    //>       sta VRAM_Buffer1,x
    //>       lda #$73
    //>       sta VRAM_Buffer1+1,x
    //>       lda #$03                ;write length for it
    //>       sta VRAM_Buffer1+2,x
    // Compute nametable coordinates for $2073 ($2000 base + row*32 + col).
    val x = 0x73 % 32 // 19
    val y = 0x73 / 32 // 3
    //>       ldy WorldNumber         ;first the world number
    //>       iny
    //>       tya
    val worldDigitTile = ((ram.worldNumber.toInt() and 0xFF) + 1).coerceIn(0, 255)
    //>       lda #$28                ;next the dash
    val dashTile = 0x28
    //>       ldy LevelNumber         ;next the level number
    //>       iny                     ;increment for proper number display
    //>       tya
    val levelDigitTile = ((ram.levelNumber.toInt() and 0xFF) + 1).coerceIn(0, 255)
    // Emit three background patterns at $2073: [world, '-', level]
    ram.vRAMBuffer1.add(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = x.toByte(),
            y = y.toByte(),
            drawVertically = false,
            patterns = listOf(
                ppu.originalRomBackgrounds[worldDigitTile],
                ppu.originalRomBackgrounds[dashTile],
                ppu.originalRomBackgrounds[levelDigitTile],
            )
        )
    )
    //>       lda #$00                ;put null terminator on
    // No-op in high-level buffer model.
    //>       sta VRAM_Buffer1+6,x
    //>       txa                     ;move the buffer offset up by 6 bytes
    //>       clc
    //>       adc #$06
    //>       sta VRAM_Buffer1_Offset
    // Not modeled; subsequent writes will append.
    //>       jmp IncSubtask
    incSubtask()
}

private fun System.displayTimeUp() {
    //> DisplayTimeUp:
    //>           lda GameTimerExpiredFlag  ;if game timer not expired, increment task
    //>           beq NoTimeUp              ;control 2 tasks forward, otherwise, stay here
    if ((ram.gameTimerExpiredFlag.toInt() and 0xFF) != 0) {
        //>           lda #$00
        //>           sta GameTimerExpiredFlag  ;reset timer expiration flag
        ram.gameTimerExpiredFlag = 0
        //>           lda #$02                  ;output time-up screen to buffer
        //>           jmp OutputInter
        outputInter(0x02)
        return
    }
    //> NoTimeUp: inc ScreenRoutineTask     ;increment control task 2 tasks forward
    ram.screenRoutineTask = ((ram.screenRoutineTask.toInt() + 1) and 0xFF).toByte()
    //>           jmp IncSubtask
    incSubtask()
}

private fun System.resetSpritesAndScreenTimer() {
    //> ResetSpritesAndScreenTimer
    // For minimal parity, ensure sprites are cleared when entering screens.
    moveAllSpritesOffscreen()
}

private fun System.displayIntermediate() {
    //> DisplayIntermediate:
    //>                lda OperMode                 ;check primary mode of operation
    //>                beq NoInter                  ;if in title screen mode, skip this
    if (ram.operMode == OperMode.TitleScreen) {
        //> NoInter:       lda #$08                     ;set for specific task and leave
        //>                sta ScreenRoutineTask
        ram.screenRoutineTask = 0x08
        //>                rts
        return
    }
    //>                cmp #GameOverModeValue       ;are we in game over mode?
    //>                beq GameOverInter            ;if so, proceed to display game over screen
    if (ram.operMode == OperMode.GameOver) {
        //> GameOverInter: lda #$12                     ;set screen timer
        //>                sta ScreenTimer
        ram.screenTimer = 0x12
        //>                lda #$03                     ;output game over screen to buffer
        //>                jsr WriteGameText
        writeGameText(0x03)
        //>                jmp IncModeTask_B
        return incModeTask_B()
    }
    //>                lda AltEntranceControl       ;otherwise check for mode of alternate entry
    //>                bne NoInter                  ;and branch if found
    if ((ram.altEntranceControl.toInt() and 0xFF) != 0) {
        ram.screenRoutineTask = 0x08
        return
    }
    //>                ldy AreaType                 ;check if we are on castle level
    //>                cpy #$03                     ;and if so, branch (possibly residual)
    //>                beq PlayerInter
    if ((ram.areaType.toInt() and 0xFF) == 0x03) {
        // fall-through to PlayerInter
    } else {
        //>                lda DisableIntermediate      ;if this flag is set, skip intermediate lives display
        //>                bne NoInter                  ;and jump to specific task, otherwise
        if ((ram.disableIntermediate.toInt() and 0xFF) != 0) {
            ram.screenRoutineTask = 0x08
            return
        }
    }
    //> PlayerInter:   jsr DrawPlayer_Intermediate  ;put player in appropriate place for
    drawPlayerIntermediate()
    //>                lda #$01                     ;lives display, then output lives display to buffer
    //> OutputInter:   jsr WriteGameText
    writeGameText(0x01)
    //>                jsr ResetScreenTimer
    resetScreenTimer()
    //>                lda #$00
    //>                sta DisableScreenFlag        ;reenable screen output
    ram.disableScreenFlag = false
    //>                rts
}

private fun System.areaParserTaskControl() {
    //> AreaParserTaskControl:
    //>            inc DisableScreenFlag     ;turn off screen
    ram.disableScreenFlag = true
    //> TaskLoop:  jsr AreaParserTaskHandler ;render column set of current area
    //>            lda AreaParserTaskNum     ;check number of tasks
    //>            bne TaskLoop              ;if tasks still not all done, do another one
    do {
        areaParserTaskHandler()
    } while ((ram.areaParserTaskNum.toInt() and 0xFF) != 0)
    //>            dec ColumnSets            ;do we need to render more column sets?
    //>            bpl OutputCol
    val colSets = ((ram.columnSets.toInt() and 0xFF) - 1)
    ram.columnSets = colSets.toByte()
    if (colSets < 0x80 && colSets >= 0) {
        //> OutputCol: lda #$06                  ;set vram buffer to output rendered column set
        //>            sta VRAM_Buffer_AddrCtrl  ;on next NMI
        ram.vRAMBufferAddrCtrl = 0x06
        //>            rts
        return
    }
    //>            inc ScreenRoutineTask     ;if not, move on to the next task
    ram.screenRoutineTask = ((ram.screenRoutineTask.toInt() + 1) and 0xFF).toByte()
    //> OutputCol: lda #$06                  ;set vram buffer to output rendered column set
    //>            sta VRAM_Buffer_AddrCtrl  ;on next NMI
    ram.vRAMBufferAddrCtrl = 0x06
    //>            rts
}

// --- Palette selection/data tables translated from disassembly ---
private val AreaPalette = byteArrayOf(
    0x01, 0x02, 0x03, 0x04
)

// Note: used only when BackgroundColorCtrl is set (values 4-7). The original indexes BGColorCtrl_Addr-4,y
private val BGColorCtrl_Addr = byteArrayOf(
    0x00, 0x09, 0x0a, 0x04
)

// First 4: by area type when bg color ctrl not set. Second 4: by background color control when set.
private val BackgroundColors = byteArrayOf(
    0x22, 0x22, 0x0f, 0x0f,
    0x0f, 0x22, 0x0f, 0x0f,
)

// Player palettes (Mario, Luigi, Fiery)
private val PlayerColors = byteArrayOf(
    0x22, 0x16, 0x27, 0x18, // mario
    0x22, 0x30, 0x27, 0x19, // luigi
    0x22, 0x37, 0x27, 0x16, // fiery
)

private fun System.getAreaPalette() {
    //> GetAreaPalette:
    //>                ldy AreaType             ;select appropriate palette to load
    //>                ldx AreaPalette,y        ;based on area type
    val areaType = ram.areaType.toInt() and 0x03
    val indexFromTable = AreaPalette[areaType]
    //> SetVRAMAddr_A: stx VRAM_Buffer_AddrCtrl ;store offset into buffer control
    ram.vRAMBufferAddrCtrl = indexFromTable
    //> NextSubtask:   jmp IncSubtask           ;move onto next task
    incSubtask()
}

private fun System.getBackgroundColor() {
    //> GetBackgroundColor:
    //>            ldy BackgroundColorCtrl   ;check background color control
    //>            beq NoBGColor             ;if not set, increment task and fetch palette
    val bgCtrl = ram.backgroundColorCtrl.toInt() and 0xFF
    if (bgCtrl == 0) return incSubtask() // NoBGColor
    //>            lda BGColorCtrl_Addr-4,y  ;put appropriate palette into vram
    // In the original, valid values for BackgroundColorCtrl are 4..7 when set.
    val idx = (bgCtrl - 4).coerceIn(0, 3)
    val value = BGColorCtrl_Addr[idx]
    //>            sta VRAM_Buffer_AddrCtrl  ;note that if set to 5-7, $0301 will not be read
    ram.vRAMBufferAddrCtrl = value
    //> NoBGColor: inc ScreenRoutineTask     ;increment to next subtask and plod on through
    incSubtask()
}

private fun System.getAlternatePalette1() {
    //> GetAlternatePalette1:
    //>                lda AreaStyle            ;check for mushroom level style
    //>                cmp #$01
    //>                bne NoAltPal
    //>                lda #$0b                 ;if found, load appropriate palette
    //> SetVRAMAddr_B: sta VRAM_Buffer_AddrCtrl
    if ((ram.areaStyle.toInt() and 0xFF) == 0x01) {
        ram.vRAMBufferAddrCtrl = 0x0B
    }
    //> NoAltPal:      jmp IncSubtask           ;now onto the next task
    incSubtask()
}


private fun System.clearBuffersDrawIcon() {
    //> ClearBuffersDrawIcon:
    //>              lda OperMode               ;check game mode
    //>              bne IncModeTask_B          ;if not title screen mode, leave
    if (ram.operMode != OperMode.TitleScreen) return incModeTask_B()
    //>              ldx #$00                   ;otherwise, clear buffer space
    //> TScrClear:   sta VRAM_Buffer1-1,x
    //>              sta VRAM_Buffer1-1+$100,x
    //>              dex
    //>              bne TScrClear
    // In the high-level model, just clear both VRAM buffers.
    ram.vRAMBuffer1.clear()
    ram.vRAMBuffer2.clear()
    //>              jsr DrawMushroomIcon       ;draw player select icon
    drawMushroomIcon()
    //> IncSubtask:  inc ScreenRoutineTask      ;move onto next task
    incSubtask()
}

private fun System.writeTopScore() {
    //> WriteTopScore
    // TODO: implemented elsewhere in the future
}

//-------------------------------------------------------------------------------------
// Helpers corresponding to IncSubtask/NextSubtask and GetPlayerColors.

private fun System.incSubtask() {
    //> IncSubtask:  inc ScreenRoutineTask      ;move onto next task
    ram.screenRoutineTask = ((ram.screenRoutineTask.toInt() + 1) and 0xFF).toByte()
}

private fun System.nextSubtask() {
    //> NextSubtask:   jmp IncSubtask           ;move onto next task
    incSubtask()
}

private fun System.getPlayerColors() {
    //> GetPlayerColors:
    //>                ldx VRAM_Buffer1_Offset  ;get current buffer offset
    // We do not model a raw offset; we append to the high-level vRAMBuffer1 list.
    //>                ldy #$00
    var paletteOffset = 0 // start with Mario
    //>                lda CurrentPlayer        ;check which player is on the screen
    //>                beq ChkFiery
    //>                ldy #$04                 ;load offset for luigi
    if ((ram.currentPlayer.toInt() and 0xFF) != 0) paletteOffset = 4
    //> ChkFiery:      lda PlayerStatus         ;check player status
    //>                cmp #$02
    //>                bne StartClrGet          ;if fiery, load alternate offset for fiery player
    //>                ldy #$08
    if ((ram.playerStatus.toInt() and 0xFF) == 0x02) paletteOffset = 8
    // Build the four player colors from the table
    val p0 = PlayerColors[paletteOffset + 0]
    val p1 = PlayerColors[paletteOffset + 1]
    val p2 = PlayerColors[paletteOffset + 2]
    val p3 = PlayerColors[paletteOffset + 3]

    // Determine background color selection
    //>                ldx VRAM_Buffer1_Offset  ;load original offset from before
    //>                ldy BackgroundColorCtrl  ;if this value is four or greater, it will be set
    //>                bne SetBGColor           ;therefore use it as offset to background color
    //>                ldy AreaType             ;otherwise use area type bits from area offset as offset
    val bgIndex = if ((ram.backgroundColorCtrl.toInt() and 0xFF) != 0) {
        // backgroundColorCtrl uses values 4..7 when set
        ram.backgroundColorCtrl.toInt() and 0xFF
    } else {
        ram.areaType.toInt() and 0x03
    }
    //> SetBGColor:    lda BackgroundColors,y   ;to background color instead
    val bg = BackgroundColors[bgIndex]

    // Compose final sprite palette bytes, with background color as the first entry.
    // The original code overwrites the first of the four player colors with background.
    val colors = listOf(
        com.ivieleague.smbtranslation.nes.Color(bg),
        com.ivieleague.smbtranslation.nes.Color(p1),
        com.ivieleague.smbtranslation.nes.Color(p2),
        com.ivieleague.smbtranslation.nes.Color(p3),
    )

    // Emit a SpriteSetPalette update for sprite palette index 0 (address $3F10).
    ram.vRAMBuffer1.add(
        BufferedPpuUpdate.SpriteSetPalette(
            index = 0,
            colors = colors,
        )
    )

    //>                rts
}

private fun System.incModeTask_B() {
    //> IncModeTask_B: inc OperMode_Task
    ram.operModeTask = ((ram.operModeTask.toInt() + 1) and 0xFF).toByte()
}

private fun System.drawPlayerIntermediate() {
    //> DrawPlayer_Intermediate (high-level stub)
    // Positioning the player sprite for the intermission is not required yet.
}

private fun System.resetScreenTimer() {
    //> ResetScreenTimer (high-level approximation)
    ram.screenTimer = 0
}

private fun System.areaParserTaskHandler() {
    //> AreaParserTaskHandler (high-level stub)
    // Minimal behavior: count down tasks until zero.
    val n = ram.areaParserTaskNum.toInt() and 0xFF
    if (n > 0) ram.areaParserTaskNum = (n - 1).toByte()
}

// --- Minimal helper stubs for text/score/intermission writes ---
private fun System.writeGameText(select: Int) {
    //> WriteGameText (high-level stub)
    // For now, we do not render actual glyphs here. The rest of the code draws via buffered templates.
}

private fun System.getSBNybbles() {
    //> GetSBNybbles (high-level stub)
    // Score/coin nybble packing is not required for current tests; left as a no-op.
}

private fun System.outputInter(code: Int) {
    //> OutputInter (high-level stub)
    // In the original, this selects an intermission/text block and calls WriteGameText.
    // Here we simply route to writeGameText to preserve control flow.
    writeGameText(code)
}

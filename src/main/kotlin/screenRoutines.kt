package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.chr.OriginalRom
import com.ivieleague.smbtranslation.nes.Color
import com.ivieleague.smbtranslation.nes.DirectPalette
import kotlin.collections.listOf
import kotlin.experimental.and

// Screen and intermediate display control tasks translated from SMB disassembly.
// These functions operate over the high-level PPU abstraction and GameRam state.

fun System.screenRoutines() {
    //> ScreenRoutines:
    //> lda ScreenRoutineTask        ;run one of the following subroutines
    //> jsr JumpEngine
    when (ram.screenRoutineTask) {
        //> .dw InitScreen
        0x00.toByte() -> initScreen()
        //> .dw SetupIntermediate
        0x01.toByte() -> setupIntermediate()
        //> .dw WriteTopStatusLine
        0x02.toByte() -> writeTopStatusLine()
        //> .dw WriteBottomStatusLine
        0x03.toByte() -> writeBottomStatusLine()
        //> .dw DisplayTimeUp
        0x04.toByte() -> displayTimeUp()
        //> .dw ResetSpritesAndScreenTimer
        0x05.toByte() -> resetSpritesAndScreenTimer()
        //> .dw DisplayIntermediate
        0x06.toByte() -> displayIntermediate()
        //> .dw ResetSpritesAndScreenTimer
        0x07.toByte() -> resetSpritesAndScreenTimer()
        //> .dw AreaParserTaskControl
        0x08.toByte() -> areaParserTaskControl()
        //> .dw GetAreaPalette
        0x09.toByte() -> getAreaPalette()
        //> .dw GetBackgroundColor
        0x0A.toByte() -> getBackgroundColor()
        //> .dw GetAlternatePalette1
        0x0B.toByte() -> getAlternatePalette1()
        //> .dw DrawTitleScreen
        0x0C.toByte() -> drawTitleScreen()
        //> .dw ClearBuffersDrawIcon
        0x0D.toByte() -> clearBuffersDrawIcon()
        //> .dw WriteTopScore
        0x0E.toByte() -> writeTopScore()
        else -> Unit
    }
}

//-------------------------------------------------------------------------------------

private fun System.initScreen() {
    //> InitScreen:
    //> jsr MoveAllSpritesOffscreen ;initialize all sprites including sprite #0
    moveAllSpritesOffscreen()
    //> jsr InitializeNameTables    ;and erase both name and attribute tables
    initializeNameTables()
    //> lda OperMode
    //> beq NextSubtask             ;if mode still 0, do not load
    if (ram.operMode == OperMode.TitleScreen) return nextSubtask()
    //> ldx #$03                    ;into buffer pointer
    //> jmp SetVRAMAddr_A
    // We model SetVRAMAddr_A as selecting which VRAM update buffer address control to use.
    // The original sets a buffer pointer/index to 3 here; reflect by storing to vRAMBufferAddrCtrl.
    ram.vRAMBufferAddrCtrl = 0x03.toByte()
    // In the original, SetVRAMAddr_A would use X to index a table of addresses; here we just advance the task.
    //> SetVRAMAddr_A: stx VRAM_Buffer_AddrCtrl ;store offset into buffer control
    //> NextSubtask:   jmp IncSubtask           ;move onto next task
    return nextSubtask()
}

//-------------------------------------------------------------------------------------

private fun System.setupIntermediate() {
    //> SetupIntermediate:
    //> lda BackgroundColorCtrl  ;save current background color control
    //> pha                      ;and player status to stack
    val savedBackgroundColorCtrl = ram.backgroundColorCtrl
    //> lda PlayerStatus
    //> pha
    val savedPlayerStatus = ram.playerStatus
    //> lda #$00                 ;set background color to black
    //> sta PlayerStatus         ;and player status to not fiery
    ram.playerStatus = 0x00.toByte()
    //> lda #$02                 ;this is the ONLY time background color control
    //> sta BackgroundColorCtrl  ;is set to less than 4
    ram.backgroundColorCtrl = 0x02.toByte()
    //> jsr GetPlayerColors
    getPlayerColors()
    //> pla                      ;we only execute this routine for
    //> sta PlayerStatus         ;the intermediate lives display
    ram.playerStatus = savedPlayerStatus
    //> pla                      ;and once we're done, we return bg
    //> sta BackgroundColorCtrl  ;color ctrl and player status from stack
    ram.backgroundColorCtrl = savedBackgroundColorCtrl
    //> jmp IncSubtask           ;then move onto the next task
    incSubtask()
}

//-------------------------------------------------------------------------------------
// Jump table targets referenced by ScreenRoutines that are not part of this issue.
// Keep them minimal no-ops for now to preserve structure and compile.

private fun System.writeTopStatusLine() {
    //> WriteTopStatusLine:
    //> lda #$00          ;select main status bar
    //> jsr WriteGameText ;output it
    writeGameText(0)
    //> jmp IncSubtask    ;onto the next task
    incSubtask()
}

private fun System.writeBottomStatusLine() {
    //> WriteBottomStatusLine:
    //> jsr GetSBNybbles        ;write player's score and coin tally to screen
    getSBNybbles()
    //> ldx VRAM_Buffer1_Offset
    // Our high-level model appends directly to vRAMBuffer1; we do not track raw offsets.
    //> lda #$20                ;write address for world-area number on screen
    //> sta VRAM_Buffer1,x
    //> lda #$73
    //> sta VRAM_Buffer1+1,x
    //> lda #$03                ;write length for it
    //> sta VRAM_Buffer1+2,x
    // Compute nametable coordinates for $2073 ($2000 base + row*32 + col).
    val x = 0x73 % 32
    val y = 0x73 / 32
    //> ldy WorldNumber         ;first the world number
    //> iny
    //> tya
    //> sta VRAM_Buffer1+3,x
    val worldDigitTile = ((ram.worldNumber) + 1).coerceIn(0, 255)
    //> lda #$28                ;next the dash
    //> sta VRAM_Buffer1+4,x
    val dashTile = 0x28.toByte()
    //> ldy LevelNumber         ;next the level number
    //> iny                     ;increment for proper number display
    //> tya
    //> sta VRAM_Buffer1+5,x
    //> lda #$00                ;put null terminator on
    //> sta VRAM_Buffer1+6,x
    val levelDigitTile = ((ram.levelNumber) + 1).coerceIn(0, 255)
    // Emit three background patterns at $2073: [world, '-', level]
    ram.vRAMBuffer1.add(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = 0,
            x = x.toByte(),
            y = y.toByte(),
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[worldDigitTile],
                OriginalRom.backgrounds[dashTile],
                OriginalRom.backgrounds[levelDigitTile],
            )
        )
    )
    //> txa                     ;move the buffer offset up by 6 bytes
    //> clc
    //> adc #$06
    //> sta VRAM_Buffer1_Offset
    // This would add 6 bytes to the length of the VRAM_Buffer1.
    // In our stuff, however, we don't need to do that because we model the VRAM Buffer as a list of instructions, rather than just bytes.
    //> jmp IncSubtask
    incSubtask()
}

private fun System.displayTimeUp() {
    //> DisplayTimeUp:
    //> lda GameTimerExpiredFlag  ;if game timer not expired, increment task
    //> beq NoTimeUp              ;control 2 tasks forward, otherwise, stay here
    if (ram.gameTimerExpiredFlag) {
        //> lda #$00
        //> sta GameTimerExpiredFlag  ;reset timer expiration flag
        ram.gameTimerExpiredFlag = false
        //> lda #$02                  ;output time-up screen to buffer
        //> jmp OutputInter
        outputInter(2)
        return
    }
    //> NoTimeUp: inc ScreenRoutineTask     ;increment control task 2 tasks forward
    ram.screenRoutineTask++
    //> jmp IncSubtask
    incSubtask()
}

private fun System.resetSpritesAndScreenTimer() {
    //> lda ScreenTimer             ;check if screen timer has expired
    //> bne NoReset                 ;if not, branch to leave
    if((ram.screenTimer) != 0.toByte())
        //> NoReset: rts
        return
    //> jsr MoveAllSpritesOffscreen ;otherwise reset sprites now
    moveAllSpritesOffscreen()
}

private fun System.displayIntermediate() {
    //> DisplayIntermediate:
    //> lda OperMode                 ;check primary mode of operation
    //> beq NoInter                  ;if in title screen mode, skip this
    if (ram.operMode == OperMode.TitleScreen) return noInter()
    //> cmp #GameOverModeValue       ;are we in game over mode?
    //> beq GameOverInter            ;if so, proceed to display game over screen
    if (ram.operMode == OperMode.GameOver) return gameOverInter()
    //> lda AltEntranceControl       ;otherwise check for mode of alternate entry
    //> bne NoInter                  ;and branch if found
    if ((ram.altEntranceControl) != 0.toByte()) return noInter()
    //> ldy AreaType                 ;check if we are on castle level
    //> cpy #$03                     ;and if so, branch (possibly residual)
    //> beq PlayerInter
    if (ram.areaType != 0x03.toByte()) {
        //> lda DisableIntermediate      ;if this flag is set, skip intermediate lives display
        //> bne NoInter                  ;and jump to specific task, otherwise
        if (ram.disableIntermediate) return noInter()
    }
    //> PlayerInter:   jsr DrawPlayer_Intermediate  ;put player in appropriate place for
    drawPlayerIntermediate()
    //> lda #$01                     ;lives display, then output lives display to buffer
    //> OutputInter:   jsr WriteGameText
    writeGameText(0x01)
    //> jsr ResetScreenTimer
    resetScreenTimer()
    //> lda #$00
    //> sta DisableScreenFlag        ;reenable screen output
    ram.disableScreenFlag = false
    //> rts
}
private fun System.gameOverInter() {
    //> GameOverInter: lda #$12                     ;set screen timer
    //> sta ScreenTimer
    ram.screenTimer = 0x12.toByte()
    //> lda #$03                     ;output game over screen to buffer
    //> jsr WriteGameText
    writeGameText(0x03)
    //> jmp IncModeTask_B
    return incModeTask_B()
}

private fun System.noInter() {
    //> NoInter:       lda #$08                     ;set for specific task and leave
    //> sta ScreenRoutineTask
    ram.screenRoutineTask = 0x08.toByte()
    //> rts
    return
}

private fun System.areaParserTaskControl() {
    //> AreaParserTaskControl:
    //> inc DisableScreenFlag     ;turn off screen
    ram.disableScreenFlag = true
    do {
        //> TaskLoop:  jsr AreaParserTaskHandler ;render column set of current area
        areaParserTaskHandler()
        //> lda AreaParserTaskNum     ;check number of tasks
        //> bne TaskLoop              ;if tasks still not all done, do another one
    } while (ram.areaParserTaskNum != 0.toByte())
    //> dec ColumnSets            ;do we need to render more column sets?
    //> bpl OutputCol
    if (--ram.columnSets < 0) {
        //> inc ScreenRoutineTask     ;if not, move on to the next task
        ram.screenRoutineTask++
    }
    //> OutputCol: lda #$06                  ;set vram buffer to output rendered column set
    //> sta VRAM_Buffer_AddrCtrl  ;on next NMI
    ram.vRAMBufferAddrCtrl = 0x06.toByte()
    //> rts
}

// --- Palette selection/data tables translated from disassembly ---
// Offsets representing indexes in vramAddrTable
private val AreaPalette = byteArrayOf(
    0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte()
)

// Note: used only when BackgroundColorCtrl is set (values 4-7). The original indexes BGColorCtrl_Addr-4,y
private val BGColorCtrl_Addr = byteArrayOf(
    0x00.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x04.toByte()
)

// First 4: by area type when bg color ctrl not set. Second 4: by background color control when set.
private val BackgroundColors = arrayOf(
    Color(0x22.toByte()), Color(0x22.toByte()), Color(0x0f.toByte()), Color(0x0f.toByte()),
    Color(0x0f.toByte()), Color(0x22.toByte()), Color(0x0f.toByte()), Color(0x0f.toByte()),
)

// Player palettes (Mario, Luigi, Fiery)
object PlayerPalettes {
    val mario =
        DirectPalette(arrayOf(Color(0x22.toByte()), Color(0x16.toByte()), Color(0x27.toByte()), Color(0x18.toByte())))
    val luigi =
        DirectPalette(arrayOf(Color(0x22.toByte()), Color(0x30.toByte()), Color(0x27.toByte()), Color(0x19.toByte())))
    val fiery =
        DirectPalette(arrayOf(Color(0x22.toByte()), Color(0x37.toByte()), Color(0x27.toByte()), Color(0x16.toByte())))
}

private fun System.getAreaPalette() {
    //> GetAreaPalette:
    //> ldy AreaType             ;select appropriate palette to load
    //> ldx AreaPalette,y        ;based on area type
    //> SetVRAMAddr_A: stx VRAM_Buffer_AddrCtrl ;store offset into buffer control
    ram.vRAMBufferAddrCtrl = AreaPalette[ram.areaType]
    //> NextSubtask:   jmp IncSubtask           ;move onto next task
    incSubtask()
}

private fun System.getBackgroundColor() {
    //> GetBackgroundColor:
    //> ldy BackgroundColorCtrl   ;check background color control
    val bgCtrl = ram.backgroundColorCtrl
    //> beq NoBGColor             ;if not set, increment task and fetch palette
    if (bgCtrl == 0.toByte()) return incSubtask() // NoBGColor
    //> lda BGColorCtrl_Addr-4,y  ;put appropriate palette into vram
    //> sta VRAM_Buffer_AddrCtrl  ;note that if set to 5-7, $0301 will not be read
    ram.vRAMBufferAddrCtrl = BGColorCtrl_Addr[bgCtrl - 4]
    //> NoBGColor: inc ScreenRoutineTask     ;increment to next subtask and plod on through
    incSubtask()
}

private fun System.getAlternatePalette1() {
    //> GetAlternatePalette1:
    //> lda AreaStyle            ;check for mushroom level style
    //> cmp #$01
    //> bne NoAltPal
    if (ram.areaStyle == 0x01.toByte()) {
        //> lda #$0b                 ;if found, load appropriate palette
        //> SetVRAMAddr_B: sta VRAM_Buffer_AddrCtrl
        ram.vRAMBufferAddrCtrl = 0x0B.toByte()
    }
    //> NoAltPal:      jmp IncSubtask           ;now onto the next task
    incSubtask()
}


private fun System.clearBuffersDrawIcon() {
    //> ClearBuffersDrawIcon:
    //> lda OperMode               ;check game mode
    //> bne IncModeTask_B          ;if not title screen mode, leave
    if (ram.operMode != OperMode.TitleScreen) return incModeTask_B()
    //> ldx #$00                   ;otherwise, clear buffer space
    //> TScrClear:   sta VRAM_Buffer1-1,x
    // Huh?  The below seems like a really really weird place to write to...
    // Ram addresses 0x400+ are random useful game data variables.
    // Is this supposed to somehow be the second VRAM buffer?  I don't think it is...
    //> sta VRAM_Buffer1-1+$100,x
    //> dex
    //> bne TScrClear
    ram.vRAMBuffer1.clear()
    //> jsr DrawMushroomIcon       ;draw player select icon
    drawMushroomIcon()
    //> IncSubtask:  inc ScreenRoutineTask      ;move onto next task
    incSubtask()
    //> rts
}

private fun System.writeTopScore(): Unit {
    //> WriteTopScore:
    //> lda #$fa           ;run display routine to display top score on title
    //> jsr UpdateNumber
    updateNumber(0xFA.toByte())
    //> IncModeTask_B: inc OperMode_Task  ;move onto next mode
    //> rts
    return incModeTask_B()
}

//-------------------------------------------------------------------------------------
// Helpers corresponding to IncSubtask/NextSubtask and GetPlayerColors.

private fun System.incSubtask() {
    //> IncSubtask:  inc ScreenRoutineTask      ;move onto next task
    ram.screenRoutineTask++
}

//> NextSubtask:   jmp IncSubtask           ;move onto next task
private fun System.nextSubtask() = incSubtask()

fun System.getPlayerColors() {
    //> GetPlayerColors:
    //> ldx VRAM_Buffer1_Offset  ;get current buffer offset
    // We're preparing to append to VRAM Buffer 1.
    //> ldy #$00
    var palette = PlayerPalettes.mario // start with Mario
    //> lda CurrentPlayer        ;check which player is on the screen
    //> beq ChkFiery
    if ((ram.currentPlayer) != 0.toByte()) {
        //> ldy #$04                 ;load offset for luigi
        palette = PlayerPalettes.luigi
    }
    //> ChkFiery:      lda PlayerStatus         ;check player status
    //> cmp #$02
    //> bne StartClrGet          ;if fiery, load alternate offset for fiery player
    if ((ram.playerStatus) == 0x02.toByte()) {
        //> ldy #$08
        palette = PlayerPalettes.fiery
    }

    // Determine background color selection
    //> ldx VRAM_Buffer1_Offset  ;load original offset from before
    //> ldy BackgroundColorCtrl  ;if this value is four or greater, it will be set
    //> bne SetBGColor           ;therefore use it as offset to background color
    //> ldy AreaType             ;otherwise use area type bits from area offset as offset
    val bgIndex = if (ram.backgroundColorCtrl != 0.toByte()) {
        // backgroundColorCtrl uses values 4..7 when set
        ram.backgroundColorCtrl
    } else {
        ram.areaType and 0x03.toByte()
    }
    //> SetBGColor:    lda BackgroundColors,y   ;to background color instead
    val bg = BackgroundColors[bgIndex]

    // Emit a SpriteSetPalette update for sprite palette index 0 (address $3F10).
    ram.vRAMBuffer1.add(
        BufferedPpuUpdate.SpriteSetPalette(
            index = 0,
            colors = listOf(bg) + palette.colors.toList().drop(1),
        )
    )

    //> rts
}

private fun System.incModeTask_B() {
    //> IncModeTask_B: inc OperMode_Task  ;move onto next mode
    ram.operModeTask++
    //> rts
}

private fun System.drawPlayerIntermediate(): Unit  { /*TODO*/ }

private fun System.resetScreenTimer() {
    //> ResetScreenTimer:
    //> lda #$07                    ;reset timer again
    //> sta ScreenTimer
    ram.screenTimer = 0x07.toByte()
    //> inc ScreenRoutineTask       ;move onto next task
    ram.screenRoutineTask++
    //> NoReset: rts
}


private fun System.getSBNybbles(): Unit  { /*TODO*/ }
private fun System.updateNumber(a: Byte): Unit  { /*TODO*/ }
private fun System.areaParserTasks(taskNum: Byte): Unit  { /*TODO*/ }

private fun System.outputInter(code: Int): Unit  { /*TODO*/ }
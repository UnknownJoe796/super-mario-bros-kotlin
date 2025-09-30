package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.chr.rawChrData
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit
import com.ivieleague.smbtranslation.utils.JoypadBits
import com.ivieleague.smbtranslation.utils.VramBufferControl
import java.io.File
import kotlin.experimental.or
import kotlin.experimental.xor


fun System.titleScreenMode(): Unit {
    //> TitleScreenMode:
    //> lda OperMode_Task
    //> jsr JumpEngine

    //> .dw InitializeGame
    //> .dw ScreenRoutines
    //> .dw PrimaryGameSetup
    //> .dw GameMenuRoutine
    when(ram.operModeTask.toInt()) {
        0 -> initializeGame()
        1 -> screenRoutines()
        2 -> primaryGameSetup()
        3 -> gameMenuRoutine()
        else -> throw IllegalStateException()
    }
}

fun System.gameMenuRoutine() {
    //> GameMenuRoutine:
    //> ldy #$00
    //> lda SavedJoypad1Bits        ;check to see if either player pressed
    //> ora SavedJoypad2Bits        ;only the start button (either joypad)
    val eitherController = JoypadBits(ram.savedJoypad1Bits.byte or ram.savedJoypad2Bits.byte)
    when {
        //> cmp #Start_Button
        //> beq StartGame
        eitherController == JoypadBits(start = true) ||
                //> cmp #A_Button+Start_Button  ;check to see if A + start was pressed
                //> bne ChkSelect               ;if not, branch to check select button
                //> StartGame:    jmp ChkContinue             ;if either start or A + start, execute here
                eitherController == JoypadBits(start = true, a = true) -> chkContinue(eitherController)

        //> ChkSelect:    cmp #Select_Button          ;check to see if the select button was pressed
        //> beq SelectBLogic            ;if so, branch reset demo timer
        eitherController == JoypadBits(select = true) ||
                //> ldx DemoTimer               ;otherwise check demo timer
                //> bne ChkWorldSel             ;if demo timer not expired, branch to check world selection

                // This section will go down to the else
                //> sta SelectTimer             ;set controller bits here if running demo
                //> jsr DemoEngine              ;run through the demo actions
                //> bcs ResetTitle              ;if carry flag set, demo over, thus branch
                //> jmp RunDemo                 ;otherwise, run game engine for demo

                //> ChkWorldSel:  ldx WorldSelectEnableFlag   ;check to see if world selection has been enabled
                //> beq NullJoypad
                //> cmp #B_Button               ;if so, check to see if the B button was pressed
                //> bne NullJoypad
                // reset demo timer
                //> iny                         ;if so, increment Y and execute same code as select
                ram.demoTimer != 0.toByte() && ram.worldSelectEnableFlag && eitherController == JoypadBits(
            b = true
        ) -> {
            //> SelectBLogic: lda DemoTimer               ;if select or B pressed, check demo timer one last time
            //> beq ResetTitle              ;if demo timer expired, branch to reset title screen mode
            if (ram.demoTimer == 0.toByte()) return resetTitle()
            //> lda #$18                    ;otherwise reset demo timer
            //> sta DemoTimer
            ram.demoTimer = 0x18.toByte()
            //> lda SelectTimer             ;check select/B button timer
            //> bne NullJoypad              ;if not expired, branch
            if (ram.selectTimer != 0.toByte()) return nullJoypad()
            //> lda #$10                    ;otherwise reset select button timer
            //> sta SelectTimer
            ram.selectTimer = 0x10.toByte()
            //> cpy #$01                    ;was the B button pressed earlier?  if so, branch
            //> beq IncWorldSel             ;note this will not be run if world selection is disabled
            if (eitherController == JoypadBits(b = true)) return incWorldSel()
            //> lda NumberOfPlayers         ;if no, must have been the select button, therefore
            //> eor #%00000001              ;change number of players and draw icon accordingly
            //> sta NumberOfPlayers
            ram.numberOfPlayers = (ram.numberOfPlayers xor 0x01).toByte()
            //> jsr DrawMushroomIcon
            drawMushroomIcon()
            //> jmp NullJoypad
            return nullJoypad()
        }

        else -> {
            if (ram.demoTimer == 0.toByte()) {
                // set controller bits here if running demo
                ram.selectTimer = eitherController.byte
                // run through the demo actions; if carry flag set, demo over
                if (demoEngine()) resetTitle() else runDemo()
            } else {
                // DemoTimer != 0 and not B-only world select -> NullJoypad then RunDemo
                nullJoypad()
            }
        }
    }
}

//> WSelectBufferTemplate:
//> .db $04, $20, $73, $01, $00, $00
fun System.wSelectBufferTemplate(worldNumber: Byte) = listOf<BufferedPpuUpdate>(
    BufferedPpuUpdate.BackgroundPatternRepeat(
        nametable = 0,
        x = (0x73 % 0x20).toByte(),
        y = (0x73 / 0x20).toByte(),
        repetitions = 1,
        drawVertically = true,
        pattern = ppu.originalRomBackgrounds[worldNumber.toInt()]
    )
)
private fun System.incWorldSel() {
    //> IncWorldSel:  ldx WorldSelectNumber       ;increment world select number
    //> inx
    var x = ram.worldSelectNumber + 1
    //> txa
    //> and #%00000111              ;mask out higher bits
    //> sta WorldSelectNumber       ;store as current world select number
    ram.worldSelectNumber = (x and 0x7).toByte()
    //> jsr GoContinue
    goContinue(ram.worldSelectNumber)
    //> UpdateShroom: lda WSelectBufferTemplate,x ;write template for world select in vram buffer
    //> sta VRAM_Buffer1-1,x        ;do this until all bytes are written
    //> inx
    //> cpx #$06
    //> bmi UpdateShroom
    // Copies in the template
    //> ldy WorldNumber             ;get world number from variable and increment for
    //> iny                         ;proper display, and put in blank byte before
    //> sty VRAM_Buffer1+3          ;null terminator
    // Updates the template with the world number
    // This code rewrites the world number.
    ram.vRAMBuffer1.clear()
    ram.vRAMBuffer1.addAll(wSelectBufferTemplate(ram.worldNumber))
    return nullJoypad()  // continue on
}
private fun System.nullJoypad() {
    //> NullJoypad:   lda #$00                    ;clear joypad bits for player 1
    //> sta SavedJoypad1Bits
    ram.savedJoypad1Bits = JoypadBits(0)
    return runDemo()
}
private fun System.runDemo() {
    //> RunDemo:      jsr GameCoreRoutine         ;run game engine
    gameCoreRoutine()
    //> lda GameEngineSubroutine    ;check to see if we're running lose life routine
    //> cmp #$06
    //> bne ExitMenu                ;if not, do not do all the resetting below
    if (ram.gameEngineSubroutine != 0x06.toByte()) return
    return resetTitle()
}
private fun System.resetTitle() {
    //> ResetTitle:   lda #$00                    ;reset game modes, disable
    //> sta OperMode                ;sprite 0 check and disable
    ram.operMode = OperMode.TitleScreen
    //> sta OperMode_Task           ;screen output
    ram.operModeTask = 0x00.toByte()
    //> sta Sprite0HitDetectFlag
    ram.sprite0HitDetectFlag = false
    //> inc DisableScreenFlag
    ram.disableScreenFlag = true
    //> rts
}
private fun System.chkContinue(joypadBits: JoypadBits) {
    //> ChkContinue:  ldy DemoTimer               ;if timer for demo has expired, reset modes
    //> beq ResetTitle
    if(ram.demoTimer == 0.toByte()) return resetTitle()
    //> asl                         ;check to see if A button was also pushed
    //> bcc StartWorld1             ;if not, don't load continue function's world number
    if(joypadBits.a) {
        //> lda ContinueWorld           ;load previously saved world number for secret
        //> jsr GoContinue              ;continue function when pressing A + start
        goContinue(ram.continueWorld)
    }
    //> StartWorld1:  jsr LoadAreaPointer
    loadAreaPointer()
    //> inc Hidden1UpFlag           ;set 1-up box flag for both players
    ram.hidden1UpFlag = true
    //> inc OffScr_Hidden1UpFlag
    ram.offScrHidden1UpFlag = true
    //> inc FetchNewGameTimerFlag   ;set fetch new game timer flag
    ram.fetchNewGameTimerFlag = true
    //> inc OperMode                ;set next game mode
    ram.operMode = OperMode.Game
    //> lda WorldSelectEnableFlag   ;if world select flag is on, then primary
    //> sta PrimaryHardMode         ;hard mode must be on as well
    ram.primaryHardMode = ram.worldSelectEnableFlag
    //> lda #$00
    //> sta OperMode_Task           ;set game mode here, and clear demo timer
    ram.operModeTask = 0x00.toByte()
    //> sta DemoTimer
    ram.demoTimer = 0x00.toByte()
    //> ldx #$17
    //> lda #$00
    for(x in 0x17 downTo 0) {
        //> InitScores:   sta ScoreAndCoinDisplay,x   ;clear player scores and coin displays
        ram.scoreAndCoinDisplay[x] = 0x0
        //> dex
        //> bpl InitScores
    }
    //> ExitMenu:     rts
}
private fun System.goContinue(worldNumber: Byte) {
    //> GoContinue:   sta WorldNumber             ;start both players at the first area
    ram.worldNumber = worldNumber
    //> sta OffScr_WorldNumber      ;of the previously saved world number
    ram.offScrWorldNumber = worldNumber
    //> ldx #$00                    ;note that on power-up using this function
    //> stx AreaNumber              ;will make no difference
    ram.areaNumber = 0
    //> stx OffScr_AreaNumber
    ram.offScrAreaNumber = 0
    //> rts
}

fun System.initializeGame(): Unit = TODO()
fun System.primaryGameSetup(): Unit = TODO()

//> MushroomIconData:
//>       .db $07, $22, $49, $83, $ce, $24, $24, $00
val System.mushroomIconData get() = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 0,
        x = (0x249 % 0x20).toByte(),
        y = (0x249 / 0x20).toByte(),
        drawVertically = true,
        patterns = listOf(
            ppu.originalRomBackgrounds[0xce],
            ppu.originalRomBackgrounds[0x24],
            ppu.originalRomBackgrounds[0x24],
        )
    )
)

fun System.drawMushroomIcon() {
    //> DrawMushroomIcon:
    //>           ldy #$07                ;read eight bytes to be read by transfer routine
    //> IconDataRead: lda MushroomIconData,y  ;note that the default position is set for a
    //>           sta VRAM_Buffer1-1,y    ;1-player game
    //>           dey
    //>           bpl IconDataRead
    ram.vRAMBuffer1.addAll(mushroomIconData)

    //>           lda NumberOfPlayers     ;check number of players
    //>           beq ExitIcon            ;if set to 1-player game, we're done
    if (ram.numberOfPlayers != 0.toByte()) {
        ram.vRAMBuffer1[0] = (ram.vRAMBuffer1[0] as BufferedPpuUpdate.BackgroundPatternString).let {
            it.copy(patterns = listOf(
            //>           lda #$24                ;otherwise, load blank tile in 1-player position
            //>           sta VRAM_Buffer1+3
                ppu.originalRomBackgrounds[0x24],
                ppu.originalRomBackgrounds[0x24],
                //>           lda #$ce                ;then load shroom icon tile in 2-player position
                //>           sta VRAM_Buffer1+5
                ppu.originalRomBackgrounds[0xce],
            ))
        }
    }
    //> ExitIcon:     rts
}

private val originalRom = File("smb.nes").readBytes()
fun System.drawTitleScreen() {
    //> DrawTitleScreen:
    //> lda OperMode                 ;are we in title screen mode?
    //> bne IncModeTask_B            ;if not, exit
    //> lda #>TitleScreenDataOffset  ;load address $1ec0 into
    //> sta PPU_ADDRESS              ;the vram address register
    //> lda #<TitleScreenDataOffset
    //> sta PPU_ADDRESS
    //> lda #$03                     ;put address $0300 into
    //> sta $01                      ;the indirect at $00
    //> ldy #$00
    //> sty $00
    //> lda PPU_DATA                 ;do one garbage read
    //> OutputTScr: lda PPU_DATA                 ;get title screen from chr-rom
    //> sta ($00),y                  ;store 256 bytes into buffer
    //> iny
    //> bne ChkHiByte                ;if not past 256 bytes, do not increment
    //> inc $01                      ;otherwise increment high byte of indirect
    //> ChkHiByte:  lda $01                      ;check high byte?
    //> cmp #$04                     ;at $0400?
    //> bne OutputTScr               ;if not, loop back and do another
    //> cpy #$3a                     ;check if offset points past end of data
    //> bcc OutputTScr               ;if not, loop back and do another
    //> lda #$05                     ;set buffer transfer control to $0300,
    //> jmp SetVRAMAddr_B            ;increment task and exit
    if (ram.operMode != OperMode.TitleScreen) {
        // IncModeTask_B
        ram.operModeTask++
        return
    }

    // Title screen pattern data lives in CHR at $1EC0
    val titleScreenDataOffset = 0x1EC0
    val totalBytes = 0x13A

    // Ensure CHR is loaded (rawChrData returns a zeroed array if ROM missing)
    val vramBufferBytes = rawChrData.copyOfRange(0x1EC0, 0x1EC0 + 0x13A)

    ram.vRAMBuffer1.clear()
    ram.vRAMBuffer1.addAll(BufferedPpuUpdate.parseVramBuffer(ppu, vramBufferBytes))
    ram.vRAMBufferAddrCtrl = 5
    ram.operModeTask++
}


//> DemoActionData:
//>       .db $01, $80, $02, $81, $41, $80, $01
//>       .db $42, $c2, $02, $80, $41, $c1, $41, $c1
//>       .db $01, $c1, $01, $02, $80, $00
val demoActionData: List<JoypadBits> = listOf<Byte>(
    0x01,
    0x80.toByte(), 0x02, 0x81.toByte(), 0x41, 0x80.toByte(), 0x01,
    0x42, 0xC2.toByte(), 0x02, 0x80.toByte(), 0x41, 0xC1.toByte(), 0x41, 0xC1.toByte(),
    0x01, 0xC1.toByte(), 0x01, 0x02, 0x80.toByte(), 0x00
).map { JoypadBits(it) }

//> DemoTimingData:
//>       .db $9b, $10, $18, $05, $2c, $20, $24
//>       .db $15, $5a, $10, $20, $28, $30, $20, $10
//>       .db $80, $20, $30, $30, $01, $ff, $00
val demoTimingData: List<Byte> = listOf(
    0x9B.toByte(), 0x10, 0x18, 0x05, 0x2C.toByte(), 0x20, 0x24,
    0x15, 0x5A.toByte(), 0x10, 0x20, 0x28, 0x30, 0x20, 0x10,
    0x80.toByte(), 0x20, 0x30, 0x30, 0x01, 0xFF.toByte(), 0x00
)

/**
 * Kotlin translation of DemoEngine.
 * @return True if the demo is done (carry set in original), false if still running (carry clear)
 */
fun System.demoEngine(): Boolean {
    //> DemoEngine:
    //>           ldx DemoAction         ;load current demo action
    var x = ram.demoAction.toInt()
    //>           lda DemoActionTimer    ;load current action timer
    //>           bne DoAction           ;if timer still counting down, skip
    if (ram.demoActionTimer == 0.toByte()) {
        //>           inx
        //>           inc DemoAction         ;if expired, increment action, X, and
        x += 1
        ram.demoAction = x.toByte()
        //>           sec                    ;set carry by default for demo over
        var carry = true
        //>           lda DemoTimingData-1,x ;get next timer
        val nextTimer = demoTimingData.getOrNull(x - 1) ?: 0.toByte()
        //>           sta DemoActionTimer    ;store as current timer
        ram.demoActionTimer = nextTimer
        //>           beq DemoOver           ;if timer already at zero, skip
        if (nextTimer == 0.toByte()) {
            //> DemoOver: rts
            return true
        }
        // if timer was nonzero, we fall through to DoAction with carry currently set,
        // but the routine will clear it before returning.
    }

    //> DoAction: lda DemoActionData-1,x ;get and perform action (current or next)
    val actionByte = demoActionData.getOrNull(x - 1) ?: JoypadBits(0)
    //>           sta SavedJoypad1Bits
    ram.savedJoypad1Bits = actionByte
    //>           dec DemoActionTimer    ;decrement action timer
    ram.demoActionTimer = (ram.demoActionTimer - 1).toByte()
    //>           clc                    ;clear carry if demo still going
    //> DemoOver: rts
    return false
}
fun System.loadAreaPointer(): Unit = TODO()
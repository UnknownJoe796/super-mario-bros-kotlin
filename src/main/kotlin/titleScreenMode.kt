package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.JoypadBits
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

//> WSelectBufferTemplate:
//> .db $04, $20, $73, $01, $00, $00
val wSelectBufferTemplate = byteArrayOf(0x04, 0x20, 0x73, 0x01, 0x00, 0x00).map { it.toByte() }.toByteArray()

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
    do {
        //> UpdateShroom: lda WSelectBufferTemplate,x ;write template for world select in vram buffer
        //> sta VRAM_Buffer1-1,x        ;do this until all bytes are written
        ram.vRAMBuffer1.wholeBuffer[x - 1] = wSelectBufferTemplate[x]
        //> inx
        x++
        //> cpx #$06
        //> bmi UpdateShroom
    } while(x < 0x06)
    //> ldy WorldNumber             ;get world number from variable and increment for
    //> iny                         ;proper display, and put in blank byte before
    //> sty VRAM_Buffer1+3          ;null terminator
    ram.vRAMBuffer1.wholeBuffer[3] = (ram.worldNumber + 1).toByte()
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
fun System.screenRoutines(): Unit = TODO()
fun System.primaryGameSetup(): Unit = TODO()

//> MushroomIconData:
//>       .db $07, $22, $49, $83, $ce, $24, $24, $00
private object MushroomIconData : GameRam.VramBytes {
    override val offset: Byte = 0x07
    override val bytes: ByteArray = byteArrayOf(
        0x07,
        0x22,
        0x49,
        0x83.toByte(),
        0xCE.toByte(),
        0x24,
        0x24,
        0x00
    )
}

fun System.drawMushroomIcon() {
    //> DrawMushroomIcon:
    //>           ldy #$07                ;read eight bytes to be read by transfer routine
    //> IconDataRead: lda MushroomIconData,y  ;note that the default position is set for a
    //>           sta VRAM_Buffer1-1,y    ;1-player game
    //>           dey
    //>           bpl IconDataRead
    ram.vRAMBuffer1.absorb(MushroomIconData)

    //>           lda NumberOfPlayers     ;check number of players
    //>           beq ExitIcon            ;if set to 1-player game, we're done
    if (ram.numberOfPlayers != 0.toByte()) {
        //>           lda #$24                ;otherwise, load blank tile in 1-player position
        //>           sta VRAM_Buffer1+3
        ram.vRAMBuffer1.wholeBuffer[3] = 0x24
        //>           lda #$ce                ;then load shroom icon tile in 2-player position
        //>           sta VRAM_Buffer1+5
        ram.vRAMBuffer1.wholeBuffer[5] = 0xCE.toByte()
    }
    //> ExitIcon:     rts
}

/**
 * @return True if the demo is done
 */
fun System.demoEngine(): Boolean = TODO()
fun System.loadAreaPointer(): Unit = TODO()
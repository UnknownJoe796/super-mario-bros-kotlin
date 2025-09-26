package com.ivieleague.smbtranslation


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
val wSelectBufferTemplate = byteArrayOf(0x04, 0x20, 0x73, 0x01, 0x00, 0x00)

fun gameMenuRoutine() {
    //> GameMenuRoutine:
    //> ldy #$00
    //> lda SavedJoypad1Bits        ;check to see if either player pressed
    //> ora SavedJoypad2Bits        ;only the start button (either joypad)
    //> cmp #Start_Button
    //> beq StartGame
//    if()
    //> cmp #A_Button+Start_Button  ;check to see if A + start was pressed
    //> bne ChkSelect               ;if not, branch to check select button
    //> StartGame:    jmp ChkContinue             ;if either start or A + start, execute here
    //> ChkSelect:    cmp #Select_Button          ;check to see if the select button was pressed
    //> beq SelectBLogic            ;if so, branch reset demo timer
    //> ldx DemoTimer               ;otherwise check demo timer
    //> bne ChkWorldSel             ;if demo timer not expired, branch to check world selection
    //> sta SelectTimer             ;set controller bits here if running demo
    //> jsr DemoEngine              ;run through the demo actions
    //> bcs ResetTitle              ;if carry flag set, demo over, thus branch
    //> jmp RunDemo                 ;otherwise, run game engine for demo
    //> ChkWorldSel:  ldx WorldSelectEnableFlag   ;check to see if world selection has been enabled
    //> beq NullJoypad
    //> cmp #B_Button               ;if so, check to see if the B button was pressed
    //> bne NullJoypad
    //> iny                         ;if so, increment Y and execute same code as select
    //> SelectBLogic: lda DemoTimer               ;if select or B pressed, check demo timer one last time
    //> beq ResetTitle              ;if demo timer expired, branch to reset title screen mode
    //> lda #$18                    ;otherwise reset demo timer
    //> sta DemoTimer
    //> lda SelectTimer             ;check select/B button timer
    //> bne NullJoypad              ;if not expired, branch
    //> lda #$10                    ;otherwise reset select button timer
    //> sta SelectTimer
    //> cpy #$01                    ;was the B button pressed earlier?  if so, branch
    //> beq IncWorldSel             ;note this will not be run if world selection is disabled
    //> lda NumberOfPlayers         ;if no, must have been the select button, therefore
    //> eor #%00000001              ;change number of players and draw icon accordingly
    //> sta NumberOfPlayers
    //> jsr DrawMushroomIcon
    //> jmp NullJoypad
    //> IncWorldSel:  ldx WorldSelectNumber       ;increment world select number
    //> inx
    //> txa
    //> and #%00000111              ;mask out higher bits
    //> sta WorldSelectNumber       ;store as current world select number
    //> jsr GoContinue
    //> UpdateShroom: lda WSelectBufferTemplate,x ;write template for world select in vram buffer
    //> sta VRAM_Buffer1-1,x        ;do this until all bytes are written
    //> inx
    //> cpx #$06
    //> bmi UpdateShroom
    //> ldy WorldNumber             ;get world number from variable and increment for
    //> iny                         ;proper display, and put in blank byte before
    //> sty VRAM_Buffer1+3          ;null terminator
    //> NullJoypad:   lda #$00                    ;clear joypad bits for player 1
    //> sta SavedJoypad1Bits
    //> RunDemo:      jsr GameCoreRoutine         ;run game engine
    //> lda GameEngineSubroutine    ;check to see if we're running lose life routine
    //> cmp #$06
    //> bne ExitMenu                ;if not, do not do all the resetting below
    //> ResetTitle:   lda #$00                    ;reset game modes, disable
    //> sta OperMode                ;sprite 0 check and disable
    //> sta OperMode_Task           ;screen output
    //> sta Sprite0HitDetectFlag
    //> inc DisableScreenFlag
    //> rts
    //> ChkContinue:  ldy DemoTimer               ;if timer for demo has expired, reset modes
    //> beq ResetTitle
    //> asl                         ;check to see if A button was also pushed
    //> bcc StartWorld1             ;if not, don't load continue function's world number
    //> lda ContinueWorld           ;load previously saved world number for secret
    //> jsr GoContinue              ;continue function when pressing A + start
    //> StartWorld1:  jsr LoadAreaPointer
    //> inc Hidden1UpFlag           ;set 1-up box flag for both players
    //> inc OffScr_Hidden1UpFlag
    //> inc FetchNewGameTimerFlag   ;set fetch new game timer flag
    //> inc OperMode                ;set next game mode
    //> lda WorldSelectEnableFlag   ;if world select flag is on, then primary
    //> sta PrimaryHardMode         ;hard mode must be on as well
    //> lda #$00
    //> sta OperMode_Task           ;set game mode here, and clear demo timer
    //> sta DemoTimer
    //> ldx #$17
    //> lda #$00
    //> InitScores:   sta ScoreAndCoinDisplay,x   ;clear player scores and coin displays
    //> dex
    //> bpl InitScores
    //> ExitMenu:     rts
    //> GoContinue:   sta WorldNumber             ;start both players at the first area
    //> sta OffScr_WorldNumber      ;of the previously saved world number
    //> ldx #$00                    ;note that on power-up using this function
    //> stx AreaNumber              ;will make no difference
    //> stx OffScr_AreaNumber
    //> rts
}

fun initializeGame(): Unit = TODO()
fun screenRoutines(): Unit = TODO()
fun primaryGameSetup(): Unit = TODO()

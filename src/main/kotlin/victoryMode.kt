package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.Constants.EndOfCastleMusic
import com.ivieleague.smbtranslation.Constants.VictoryMusic
import com.ivieleague.smbtranslation.Constants.World8

/**
 * Kotlin translation of VictoryMode and related routines.
 * These are not currently exercised by tests but are provided to advance the port with
 * accurate commented structure and idiomatic control flow.
 */

fun System.victoryMode() {
    //> VictoryMode:
    //> jsr VictoryModeSubroutines  ;run victory mode subroutines
    victoryModeSubroutines()
    //> lda OperMode_Task           ;get current task of victory mode
    //> beq AutoPlayer              ;if on bridge collapse, skip enemy processing
    if (ram.operModeTask != 0.toByte()) {
        //> ldx #$00
        //> stx ObjectOffset            ;otherwise reset enemy object offset
        ram.objectOffset = 0
        //> jsr EnemiesAndLoopsCore     ;and run enemy code
        enemiesAndLoopsCore()
    }
    //> AutoPlayer: jsr RelativePlayerPosition  ;get player's relative coordinates
    relativePlayerPosition()
    //> jmp PlayerGfxHandler        ;draw the player, then leave
    playerGfxHandler()
}

private fun System.victoryModeSubroutines() {
    //> VictoryModeSubroutines:
    //> lda OperMode_Task
    val task = ram.operModeTask.toInt()
    //> jsr JumpEngine
    //>
    //> .dw BridgeCollapse
    //> .dw SetupVictoryMode
    //> .dw PlayerVictoryWalk
    //> .dw PrintVictoryMessages
    //> .dw PlayerEndWorld
    when (task) {
        0 -> bridgeCollapse()
        1 -> setupVictoryMode()
        2 -> playerVictoryWalk()
        3 -> printVictoryMessages()
        4 -> playerEndWorld()
        else -> { /* no-op */ }
    }
}

private fun System.setupVictoryMode() {
    //> SetupVictoryMode:
    //> ldx ScreenRight_PageLoc  ;get page location of right side of screen
    //> inx                      ;increment to next page
    //> stx DestinationPageLoc   ;store here
    ram.destinationPageLoc = (ram.screenRightPageLoc.toInt() + 1).toByte()
    //> lda #EndOfCastleMusic
    //> sta EventMusicQueue      ;play win castle music
    ram.eventMusicQueue = EndOfCastleMusic
    //> jmp IncModeTask_B        ;jump to set next major task in victory mode
    ram.operModeTask++
    return
}

// Placeholders for routines referenced by the victory mode state machine.
// Keep them private to avoid unintended external coupling for now.
private fun System.bridgeCollapse(): Unit = TODO("BridgeCollapse not yet implemented")
private fun System.playerVictoryWalk() {
    //> PlayerVictoryWalk:
    //> ldy #$00                ;set value here to not walk player by default
    //> sty VictoryWalkControl
    ram.victoryWalkControl = 0x00
    //> lda Player_PageLoc      ;get player's page location
    //> cmp DestinationPageLoc  ;compare with destination page location
    //> bne PerformWalk         ;if page locations don't match, branch
    var autoControlPlayerInput = 0.toByte()
    //> lda Player_X_Position   ;otherwise get player's horizontal position
    //> cmp #$60                ;compare with preset horizontal position
    //> bcs DontWalk            ;if still on other page, branch ahead
    if(ram.playerPageLoc == ram.destinationPageLoc || ram.playerXPosition < 0x60u) {

        //> PerformWalk: inc VictoryWalkControl  ;otherwise increment value and Y
        ram.victoryWalkControl++
        //> iny                     ;note Y will be used to walk the player
        autoControlPlayerInput++
    }
    //> DontWalk:    tya                     ;put contents of Y in A and
    //> jsr AutoControlPlayer   ;use A to move player to the right or not
    autoControlPlayer(autoControlPlayerInput)
    //> lda ScreenLeft_PageLoc  ;check page location of left side of screen
    //> cmp DestinationPageLoc  ;against set value here
    //> beq ExitVWalk           ;branch if equal to change modes if necessary
    if(ram.screenLeftPageLoc != ram.destinationPageLoc) {
        //> lda ScrollFractional
        //> clc                     ;do fixed point math on fractional part of scroll
        //> adc #$80
        //> sta ScrollFractional    ;save fractional movement amount
        val fractionalAddResult = ram.scrollFractional.toUByte() + 0x80u
        ram.scrollFractional = fractionalAddResult.toByte()
        //> lda #$01                ;set 1 pixel per frame
        //> adc #$00                ;add carry from previous addition
        //> tay                     ;use as scroll amount
        //> jsr ScrollScreen        ;do sub to scroll the screen
        scrollScreen((0x01 + (if(fractionalAddResult > 0xFFu) 1 else 0)).toByte())
        //> jsr UpdScrollVar        ;do another sub to update screen and scroll variables
        updScrollVar()
        //> inc VictoryWalkControl  ;increment value to stay in this routine
        ram.victoryWalkControl++
    }
    //> ExitVWalk:   lda VictoryWalkControl  ;load value set here
    //> beq IncModeTask_A       ;if zero, branch to change modes
    if(ram.victoryWalkControl == 0.toByte()) ram.operModeTask++
    //> rts                     ;otherwise leave
}


private fun System.printVictoryMessages() {
    //> lda SecondaryMsgCounter   ;load secondary message counter
    //> bne IncMsgCounter         ;if set, branch to increment message counters
    if (ram.secondaryMsgCounter != 0.toByte()) return incMsgCounter()
    //> lda PrimaryMsgCounter     ;otherwise load primary message counter
    //> beq ThankPlayer           ;if set to zero, branch to print first message
    if (ram.primaryMsgCounter == 0.toByte()) thankPlayer(ram.primaryMsgCounter)
    else {
        //> cmp #$09                  ;if at 9 or above, branch elsewhere (this comparison
        //> bcs IncMsgCounter         ;is residual code, counter never reaches 9)
        // According to the disassembly comments, this can't happen.
        if ((ram.primaryMsgCounter.toInt() and 0xFF) >= 9) return incMsgCounter()

        //> ldy WorldNumber           ;check world number
        //> cpy #World8
        //> bne MRetainerMsg          ;if not at world 8, skip to next part
        if(ram.worldNumber != Constants.World8) return mRetainerMsg(ram.primaryMsgCounter)

        //> cmp #$03                  ;check primary message counter again
        //> bcc IncMsgCounter         ;if not at 3 yet (world 8 only), branch to increment
        if(ram.primaryMsgCounter < 3.toByte()) return incMsgCounter()
        //> sbc #$01                  ;otherwise subtract one
        //> jmp ThankPlayer           ;and skip to next part
        return thankPlayer((ram.primaryMsgCounter - 1).toByte())
    }
}
private fun System.mRetainerMsg(primaryMsgCounter: Byte) {
    //> MRetainerMsg:  cmp #$02                  ;check primary message counter
    //> bcc IncMsgCounter         ;if not at 2 yet (world 1-7 only), branch
    if(primaryMsgCounter < 2) return incMsgCounter()
    else return thankPlayer(primaryMsgCounter)
}
private fun System.thankPlayer(primaryMsgCounter: Byte) {
    var y = primaryMsgCounter
    //> ThankPlayer:   tay                       ;put primary message counter into Y
    //> bne SecondPartMsg         ;if counter nonzero, skip this part, do not print first message
    if (primaryMsgCounter == 0.toByte()) {
        //> lda CurrentPlayer         ;otherwise get player currently on the screen
        //> beq EvalForMusic          ;if mario, branch
        if (ram.currentPlayer != 0.toByte()) y++
        //> iny                       ;otherwise increment Y once for luigi and
        //> bne EvalForMusic          ;do an unconditional branch to the same place
    } else {
        //> SecondPartMsg: iny                       ;increment Y to do world 8's message
        y++
        //> lda WorldNumber
        //> cmp #World8               ;check world number
        //> beq EvalForMusic          ;if at world 8, branch to next part
        if (ram.worldNumber != Constants.World8) {
            //> dey                       ;otherwise decrement Y for world 1-7's message
            y--
            //> cpy #$04                  ;if counter at 4 (world 1-7 only)
            //> bcs SetEndTimer           ;branch to set victory end timer
            if (y >= 4) return setEndTimer(true)
            //> cpy #$03                  ;if counter at 3 (world 1-7 only)
            //> bcs IncMsgCounter         ;branch to keep counting
            if (y >= 3) incMsgCounter()
        }
    }


    //> EvalForMusic:  cpy #$03                  ;if counter not yet at 3 (world 8 only), branch
    //> bne PrintMsg              ;to print message only (note world 1-7 will only
    if (y == 3.toByte()) {
        //> lda #VictoryMusic         ;reach this code if counter = 0, and will always branch)
        //> sta EventMusicQueue       ;otherwise load victory music first (world 8 only)
        ram.eventMusicQueue = VictoryMusic
    }
    //> PrintMsg:      tya                       ;put primary message counter in A
    //> clc                       ;add $0c or 12 to counter thus giving an appropriate value,
    //> adc #$0c                  ;($0c-$0d = first), ($0e = world 1-7's), ($0f-$12 = world 8's)
    //> sta VRAM_Buffer_AddrCtrl  ;write message counter to vram address controller
    ram.vRAMBufferAddrCtrl = (y + 0xC).toByte()
    // fall through
    return incMsgCounter()
}
private fun System.incMsgCounter() {
    //> IncMsgCounter: lda SecondaryMsgCounter
    //> clc
    //> adc #$04                      ;add four to secondary message counter
    //> sta SecondaryMsgCounter
    val newSecondaryCounter = ram.secondaryMsgCounter.toUByte() + 4u
    ram.secondaryMsgCounter = newSecondaryCounter.toByte()
    //> lda PrimaryMsgCounter
    //> adc #$00                      ;add carry to primary message counter
    //> sta PrimaryMsgCounter
    ram.primaryMsgCounter = (ram.primaryMsgCounter.toUByte() + if (newSecondaryCounter > 0xFFu) 1u else 0u).toByte()
    //> cmp #$07                      ;check primary counter one more time
    // fallthrough
    return setEndTimer(ram.primaryMsgCounter.toUByte() >= 7.toUByte())
}
private fun System.setEndTimer(carry: Boolean) {
    //> SetEndTimer:   bcc ExitMsgs                  ;if not reached value yet, branch to leave
    if(carry) {
        //> lda #$06
        //> sta WorldEndTimer             ;otherwise set world end timer
        ram.worldEndTimer = 0x06.toByte()
        //> IncModeTask_A: inc OperMode_Task             ;move onto next task in mode
        ram.operModeTask++
    }
    //> ExitMsgs:      rts                           ;leave
}

private fun System.playerEndWorld() {
    //> PlayerEndWorld:
    // Check world end timer
    //> lda WorldEndTimer          ;check to see if world end timer expired
    //> bne EndExitOne             ;branch to leave if not
    if (ram.worldEndTimer != 0.toByte()) return

    // Check world number; if at or beyond World 8, allow B button check
    //> ldy WorldNumber            ;check world number
    //> cpy #World8                ;if on world 8, player is done with game,
    //> bcs EndChkBButton          ;thus branch to read controller
    val world = ram.worldNumber
    if ((world.toInt() and 0xFF) < (World8.toInt() and 0xFF)) {
        // Initialize for next world start at area 1-1
        //> lda #$00
        //> sta AreaNumber             ;otherwise initialize area number used as offset
        //> sta LevelNumber            ;and level number control to start at area 1
        //> sta OperMode_Task          ;initialize secondary mode of operation
        ram.areaNumber = 0x00
        ram.levelNumber = 0x00
        ram.operModeTask = 0x00
        // increment world number
        //> inc WorldNumber            ;increment world number to move onto the next world
        ram.worldNumber = ((world.toInt() and 0xFF) + 1).toByte()
        // load next area's pointer
        //> jsr LoadAreaPointer        ;get area address offset for the next area
        loadAreaPointer()
        // set flag to fetch game timer from header
        //> inc FetchNewGameTimerFlag  ;set flag to load game timer from header
        ram.fetchNewGameTimerFlag = true
        // set game mode
        //> lda #GameModeValue
        //> sta OperMode               ;set mode of operation to game mode
        ram.operMode = OperMode.Game
        //> EndExitOne:    rts                        ;and leave
        return
    }

    // EndChkBButton: check B on either controller
    //> EndChkBButton: lda SavedJoypad1Bits
    //> ora SavedJoypad2Bits       ;check to see if B button was pressed on
    //> and #B_Button              ;either controller
    //> beq EndExitTwo             ;branch to leave if not
    // TODO: leave this as joypadbits, stupid
    val either = (ram.savedJoypad1Bits.byte.toInt() or ram.savedJoypad2Bits.byte.toInt()) and 0xFF
    val bPressed = (either and (Constants.B_Button.toInt() and 0xFF)) != 0
    if (!bPressed) return

    // Set world selection enable flag
    //> lda #$01                   ;otherwise set world selection flag
    //> sta WorldSelectEnableFlag
    ram.worldSelectEnableFlag = true

    // Remove onscreen player's lives (NumberofLives/$ff)
    //> lda #$ff                   ;remove onscreen player's lives
    //> sta NumberofLives
    ram.numberofLives = 0xFF.toByte()

    // Continue other player or end game
    //> jsr TerminateGame          ;do sub to continue other player or end game
    terminateGame()
    //> EndExitTwo:    rts                        ;leave
}

// Continue other player or end game
private fun System.terminateGame(): Unit = TODO("TerminateGame not yet implemented")

// Engine/graphics helpers referenced from assembly. Keep as stubs.
private fun System.enemiesAndLoopsCore(): Unit = TODO("EnemiesAndLoopsCore not yet implemented")
private fun System.relativePlayerPosition(): Unit = TODO("RelativePlayerPosition not yet implemented")
private fun System.playerGfxHandler(): Unit = TODO("PlayerGfxHandler not yet implemented")

// Minimal stubs for routines referenced by victory walk logic; these will be implemented with proper behavior later.
private fun System.autoControlPlayer(amount: Byte) { /* Moves player right by amount or no-op if 0. Placeholder. */ }
private fun System.scrollScreen(amount: Byte) { /* Scrolls screen by amount. Placeholder. */ }
private fun System.updScrollVar() { /* Updates scroll-related variables. Placeholder. */ }

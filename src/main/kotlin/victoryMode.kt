package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.Constants.EndOfCastleMusic
import com.ivieleague.smbtranslation.Constants.VictoryMusic
import com.ivieleague.smbtranslation.Constants.World8
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Kotlin translation of VictoryMode and related routines.
 * SMB1 uses a 5-task dispatch table (unchanged from original).
 * SMB2J uses two dispatch tables: a 6-task table for W1-7/W9, and a
 * 13-task extended table for W8 (and W-D, which maps to W8).
 */

fun System.victoryMode() {
    //> VictoryMode:
    //> jsr VictoryModeSubroutines  ;run victory mode subroutines
    victoryModeSubroutines()
    //> lda OperMode_Task           ;get current task of victory mode
    //> beq AutoPlayer              ;if on bridge collapse, skip enemy processing
    if (ram.operModeTask != 0.toByte()) {
        if (variant == GameVariant.SMB2J && ram.worldNumber == World8) {
            //> (sm2main) cpx #World8; bne NotW8
            //> lda OperMode_Task; cmp #$0c; bcc NotW8
            //> ldx #$00; stx ObjectOffset; jsr EnemiesAndLoopsCore
            // W8 only runs enemy code at task >= 12 (RunMushroomRetainers)
            if (ram.operModeTask.toInt() and 0xFF >= 0x0c) {
                ram.objectOffset = 0
                enemiesAndLoopsCore()
            }
        } else {
            //> ldx #$00
            //> stx ObjectOffset            ;otherwise reset enemy object offset
            ram.objectOffset = 0
            //> jsr EnemiesAndLoopsCore     ;and run enemy code
            enemiesAndLoopsCore()
        }
    }
    //> AutoPlayer: jsr RelativePlayerPosition  ;get player's relative coordinates
    relativePlayerPosition()
    //> jmp PlayerGfxHandler        ;draw the player, then leave
    playerGfxHandler()
}

private fun System.victoryModeSubroutines() {
    val task = ram.operModeTask.toInt()
    if (variant == GameVariant.SMB2J) {
        //> (sm2main) VictoryModeSubroutines:
        //> lda WorldNumber; cmp #World8; beq VictoryModeSubsForW8
        if (ram.worldNumber == World8) {
            //> VictoryModeSubsForW8:
            //> lda OperMode_Task; jsr JumpEngine
            //> .word BridgeCollapse          ;0
            //> .word SetupVictoryMode        ;1
            //> .word PlayerVictoryWalk       ;2
            //> .word StartVMDelay            ;3
            //> .word ContinueVMDelay         ;4
            //> .word VictoryModeDiskRoutines ;5
            //> .word ScreenSubsForFinalRoom  ;6
            //> .word PrintVictoryMsgsForWorld8 ;7
            //> .word EndCastleAward          ;8
            //> .word AwardExtraLives         ;9
            //> .word FadeToBlue              ;10
            //> .word EraseLivesLines         ;11
            //> .word RunMushroomRetainers    ;12
            when (task) {
                0 -> bridgeCollapse()
                1 -> setupVictoryMode()
                2 -> playerVictoryWalk()
                3 -> startVMDelay()
                4 -> continueVMDelay()
                5 -> victoryModeDiskRoutines()
                6 -> screenSubsForFinalRoom()
                7 -> printVictoryMsgsForW8()
                8 -> endCastleAward()
                9 -> awardExtraLives()
                10 -> fadeToBlue()
                11 -> eraseLivesLines()
                12 -> runMushroomRetainers()
                else -> { /* no-op */ }
            }
        } else {
            //> lda OperMode_Task; jsr JumpEngine
            //> .word BridgeCollapse          ;0
            //> .word SetupVictoryMode        ;1
            //> .word PlayerVictoryWalk       ;2
            //> .word PrintVictoryMessages    ;3
            //> .word EndCastleAward          ;4
            //> .word EndWorld1Thru7          ;5
            when (task) {
                0 -> bridgeCollapse()
                1 -> setupVictoryMode()
                2 -> playerVictoryWalk()
                3 -> printVictoryMessages()
                4 -> endCastleAward()
                5 -> endWorld1Thru7()
                else -> { /* no-op */ }
            }
        }
    } else {
        //> VictoryModeSubroutines: (SMB1)
        //> lda OperMode_Task
        //> jsr JumpEngine
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
}

private fun System.setupVictoryMode() {
    //> SetupVictoryMode:
    //> ldx ScreenRight_PageLoc  ;get page location of right side of screen
    //> inx                      ;increment to next page
    //> stx DestinationPageLoc   ;store here
    ram.destinationPageLoc = (ram.screenRightPageLoc.toInt() + 1).toByte()
    if (variant == GameVariant.SMB2J) {
        //> (sm2main) ldy WorldNumber; lda WorldBits,y; ora CompletedWorlds; sta CompletedWorlds
        val bit = 1 shl (ram.worldNumber.toInt() and 7)
        ram.completedWorlds = ((ram.completedWorlds.toInt() and 0xFF) or bit).toByte()
        //> lda HardWorldFlag; beq W1Thru8
        //> lda WorldNumber; cmp #World4; bcc W1Thru8
        //> lda #World8; sta WorldNumber
        if (ram.hardWorldFlag && (ram.worldNumber.toInt() and 0xFF) >= 3) {
            // World D (index 3 in A-D) triggers end game by mapping to World 8
            ram.worldNumber = World8
        }
    }
    //> lda #EndOfCastleMusic
    //> sta EventMusicQueue      ;play win castle music
    ram.eventMusicQueue = EndOfCastleMusic
    //> jmp IncModeTask_B        ;jump to set next major task in victory mode
    ram.operModeTask++
}

// by Claude - bridgeCollapse() moved to victorySubs.kt
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
    if(ram.playerPageLoc != ram.destinationPageLoc || ram.playerXPosition < 0x60u) {

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
        scrollScreen((0x01 + (if(fractionalAddResult > 0xFFu) 1 else 0)))
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
        if ((ram.primaryMsgCounter) >= 9) return incMsgCounter()

        //> ldy WorldNumber           ;check world number
        //> cpy #World8
        //> bne MRetainerMsg          ;if not at world 8, skip to next part
        if(ram.worldNumber != Constants.World8) return mRetainerMsg(ram.primaryMsgCounter)

        //> cmp #$03                  ;check primary message counter again
        //> bcc IncMsgCounter         ;if not at 3 yet (world 8 only), branch to increment
        if(ram.primaryMsgCounter.toUByte() < 3.toUByte()) return incMsgCounter()
        //> sbc #$01                  ;otherwise subtract one
        //> jmp ThankPlayer           ;and skip to next part
        return thankPlayer((ram.primaryMsgCounter - 1).toByte())
    }
}
private fun System.mRetainerMsg(primaryMsgCounter: Byte) {
    //> MRetainerMsg:  cmp #$02                  ;check primary message counter
    //> bcc IncMsgCounter         ;if not at 2 yet (world 1-7 only), branch
    if(primaryMsgCounter.toUByte() < 2u) return incMsgCounter()
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
            if (y >= 3) return incMsgCounter()
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

/**
 * SMB1 combined end-of-world handler. Waits for timer, then either advances to next
 * world (W1-7) or checks for B button to enable world select (W8).
 */
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
    if (world.toUByte() < World8.toUByte()) {
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
        ram.worldNumber = world.inc()
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
    val either = (ram.savedJoypad1Bits.byte or ram.savedJoypad2Bits.byte)
    val bPressed = either and Constants.B_Button != 0.toByte()
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

// ---- SMB2J separate EndCastleAward / EndWorld1Thru7 ----

/**
 * SMB2J EndCastleAward: short timer delay before advancing to next task.
 * Used as task 4 in W1-7 table and task 8 in W8 table.
 */
private fun System.endCastleAward() {
    //> EndCastleAward:
    //> lda WorldEndTimer
    //> bne ExEWA
    if (ram.worldEndTimer != 0.toByte()) return
    //> lda #$04
    //> sta WorldEndTimer              ;another short delay
    ram.worldEndTimer = 0x04
    //> inc OperMode_Task
    ram.operModeTask++
    //> ExEWA: rts
}

/**
 * SMB2J EndWorld1Thru7: advances to next world after castle award.
 * Caps at World 9 for SMB2J. Used as task 5 in W1-7 table.
 */
private fun System.endWorld1Thru7() {
    //> EndWorld1Thru7:
    //> lda WorldEndTimer
    //> bne EndExit
    if (ram.worldEndTimer != 0.toByte()) return
    //> NextWorld: lda #$00
    //> sta AreaNumber
    //> sta LevelNumber
    //> sta OperMode_Task
    ram.areaNumber = 0x00
    ram.levelNumber = 0x00
    ram.operModeTask = 0x00
    //> lda WorldNumber; clc; adc #$01
    var nextWorld = (ram.worldNumber.toInt() and 0xFF) + 1
    //> cmp #World9; bcc NoPast9; lda #World9
    if (nextWorld > Constants.World9.toInt() and 0xFF) {
        nextWorld = Constants.World9.toInt() and 0xFF
    }
    //> NoPast9: sta WorldNumber
    ram.worldNumber = nextWorld.toByte()
    //> jsr LoadAreaPointer
    loadAreaPointer()
    //> inc FetchNewGameTimerFlag
    ram.fetchNewGameTimerFlag = true
    //> lda #$01; sta OperMode
    ram.operMode = OperMode.Game
    //> EndExit: rts
}

// ---- SMB2J W8 extended victory sequence routines ----

/**
 * SMB2J StartVMDelay (W8 task 3): sets a delay timer and advances.
 */
private fun System.startVMDelay() {
    //> StartVMDelay:
    //> lda #$06; sta WorldEndTimer
    ram.worldEndTimer = 0x06
    //> inc OperMode_Task
    ram.operModeTask++
}

/**
 * SMB2J ContinueVMDelay (W8 task 4): waits for timer to expire, then advances.
 */
private fun System.continueVMDelay() {
    //> ContinueVMDelay:
    //> lda WorldEndTimer; bne ExitVMD
    if (ram.worldEndTimer != 0.toByte()) return
    //> inc OperMode_Task
    ram.operModeTask++
    //> ExitVMD: rts
}

/**
 * SMB2J VictoryModeDiskRoutines (W8 task 5): FDS disk load for ending data.
 * Stub -- increments GamesBeatenCount (capped at 24) and advances.
 */
private fun System.victoryModeDiskRoutines() {
    //> VictoryModeDiskRoutines: (FDS disk load stub)
    // Increment GamesBeatenCount, capped at 24
    val count = (ram.gamesBeatenCount.toInt() and 0xFF) + 1
    ram.gamesBeatenCount = count.coerceAtMost(24).toByte()
    ram.operModeTask++
}

/**
 * SMB2J ScreenSubsForFinalRoom (W8 task 6): draws the princess's room.
 * Stub -- advances to next task.
 */
private fun System.screenSubsForFinalRoom() {
    //> ScreenSubsForFinalRoom: (stub - would draw princess's room via VRAM buffer)
    ram.operModeTask++
}

/**
 * SMB2J PrintVictoryMsgsForWorld8 (W8 task 7): prints W8-specific victory messages.
 * Uses secondaryMsgCounter as fractional accumulator and primaryMsgCounter as message index.
 */
private fun System.printVictoryMsgsForW8() {
    //> PrintVictoryMsgsForWorld8:
    //> lda MsgFractional          ; (= SecondaryMsgCounter)
    //> bne IncVMC
    if (ram.secondaryMsgCounter != 0.toByte()) return incVMsgCounter()
    //> ldy MsgCounter             ; (= PrimaryMsgCounter)
    val counter = ram.primaryMsgCounter.toInt() and 0xFF
    //> cpy #$0a
    //> bcs EndVictoryMessages     ;if counter >= 10, done
    if (counter >= 0x0a) {
        //> EndVictoryMessages:
        //> lda #$0c; sta WorldEndTimer
        ram.worldEndTimer = 0x0c
        //> inc OperMode_Task
        ram.operModeTask++
        return
    }
    //> iny; iny; iny              ;add 3
    val y = counter + 3
    //> cpy #$05; bne PrintVM
    if (y == 0x05) {
        //> lda #VictoryMusic; sta EventMusicQueue
        ram.eventMusicQueue = VictoryMusic
    }
    //> PrintVM: tya; clc; adc #$0c; sta VRAM_Buffer_AddrCtrl
    ram.vRAMBufferAddrCtrl = (y + 0x0c).toByte()
    // fall through to increment
    return incVMsgCounter()
}

/**
 * Increment message counters for W8 victory message printing.
 * Fractional part adds 4 per call; carry increments the main counter.
 */
private fun System.incVMsgCounter() {
    //> IncVMC: lda MsgFractional; clc; adc #$04; sta MsgFractional
    val newFrac = ram.secondaryMsgCounter.toUByte() + 4u
    ram.secondaryMsgCounter = newFrac.toByte()
    //> lda MsgCounter; adc #$00; sta MsgCounter
    ram.primaryMsgCounter = (ram.primaryMsgCounter.toUByte() + if (newFrac > 0xFFu) 1u else 0u).toByte()
    //> rts
}

/**
 * SMB2J AwardExtraLives (W8 task 9): awards 100,000 points per remaining life.
 * Decrements lives one at a time with a delay between each, playing the extra life sound.
 */
private fun System.awardExtraLives() {
    //> AwardExtraLives:
    //> lda WorldEndTimer; bne ExAEL
    if (ram.worldEndTimer != 0.toByte()) return
    //> lda NumberofLives; bmi SkipToNext
    if (ram.numberofLives.toInt() and 0xFF >= 0x80) {
        // No lives left (underflowed past 0) -- advance to next task
        ram.operModeTask++
        return
    }
    //> lda SelectTimer; bne ExAEL
    if (ram.selectTimer != 0.toByte()) return
    //> lda #$30; sta SelectTimer
    ram.selectTimer = 0x30
    //> lda #Sfx_ExtraLife; sta Square2SoundQueue
    ram.square2SoundQueue = Constants.Sfx_ExtraLife
    //> dec NumberofLives
    ram.numberofLives = (ram.numberofLives - 1).toByte()
    // Award 100,000 points (digit modifier position 1 = hundred-thousands)
    //> lda #$01; sta DigitModifier+1
    ram.digitModifier[1] = 1
    //> jsr EndAreaPoints
    endAreaPoints()
    //> ExAEL: rts
}

/**
 * SMB2J FadeToBlue (W8 task 10): gradually changes palette to blue tints.
 * Stub -- advances to next task.
 */
private fun System.fadeToBlue() {
    //> FadeToBlue: (stub - visual palette transition effect)
    ram.operModeTask++
}

/**
 * SMB2J EraseLivesLines (W8 task 11): erases bottom two lines of screen.
 * Stub -- advances to next task.
 */
private fun System.eraseLivesLines() {
    //> EraseLivesLines: (stub - writes blank rows to VRAM buffer)
    ram.operModeTask++
}

/**
 * SMB2J RunMushroomRetainers (W8 task 12): draws flashing mushroom retainers.
 * Stub -- advances to next task when complete.
 */
private fun System.runMushroomRetainers() {
    //> RunMushroomRetainers: (stub - waits for victory music to end)
    // In the real FDS game, this loops drawing retainer sprites until
    // the victory music finishes playing. Since we stub the visuals,
    // just advance immediately.
    ram.operModeTask++
}

// by Claude - terminateGame() moved to victorySubs.kt

// Engine/graphics helpers - now defined in shared files (gameMode.kt, etc.)
// enemiesAndLoopsCore, relativePlayerPosition, playerGfxHandler are in their respective files
// autoControlPlayer, scrollScreen are in playerLevelTransitions.kt and scrollHandler.kt

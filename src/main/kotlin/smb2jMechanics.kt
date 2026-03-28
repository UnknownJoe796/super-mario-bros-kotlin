// by Claude - SMB2J-specific game mechanics: wind, world progression
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*

/**
 * BlowPlayerAround: Pushes player rightward when wind is active.
 * Translated from sm2data2.asm BlowPlayerAround (line 179).
 */
fun System.blowPlayerAround() {
    //> BlowPlayerAround:
    //> lda WindFlag            ;if wind is turned off, just exit
    //> beq ExBlow
    if (!ram.windFlag) return
    //> lda AreaType            ;don't blow the player around unless
    //> cmp #$01                ;the area is ground type
    //> bne ExBlow
    if (ram.areaType != AreaType.Ground) return

    //> ldy #$01
    //> lda FrameCounter        ;branch to set d0 if on an odd frame
    //> asl                     ;shift bit 7 into carry
    //> bcs BThr                ;if carry set (bit 7 was 1), use mask $01
    //> ldy #$03                ;otherwise use mask $03 (wind blows 1 in 4 frames)
    val frame = ram.frameCounter.toInt() and 0xFF
    val mask = if ((frame and 0x80) != 0) 0x01 else 0x03
    //> BThr: sty $00
    //> lda FrameCounter        ;throttle wind blowing by using the frame counter
    //> and $00                 ;to mask out certain frames
    //> bne ExBlow
    if ((frame and mask) != 0) return

    //> lda Player_X_Position   ;move player slightly to the right
    //> clc                     ;to simulate the wind moving the player
    //> adc #$01
    //> sta Player_X_Position
    val newX = (ram.playerXPosition.toInt() and 0xFF) + 1
    ram.playerXPosition = newX.toUByte()
    //> lda Player_PageLoc
    //> adc #$00
    //> sta Player_PageLoc
    if (newX > 0xFF) {
        ram.playerPageLoc = ((ram.playerPageLoc.toInt() and 0xFF) + 1).toByte()
    }
    //> inc Player_X_Scroll     ;add one to movement speed for scroll
    ram.playerXScroll = ((ram.playerXScroll.toInt() and 0xFF) + 1).toByte()
    //> ExBlow: rts
}

/**
 * MoveUpsideDownPiranhaP: Movement for upside-down piranha plants (SMB2J enemy ID $04).
 * Unlike regular piranha plants, these always move (no player proximity check) and
 * the up/down positions are swapped. Translated from sm2data2.asm line 140.
 */
fun System.moveUpsideDownPiranhaP() {
    val x = ram.objectOffset.toInt()

    //> MoveUpsideDownPiranhaP:
    //> lda Enemy_State,x           ;check enemy state
    //> bne ExMoveUDPP              ;if set at all, branch to leave
    if (ram.enemyState.getEnemyState(x).isActive) return
    //> lda EnemyFrameTimer,x       ;check enemy's timer here
    //> bne ExMoveUDPP              ;branch to end if not yet expired
    if (ram.timers[0x0a + x] != 0.toByte()) return

    //> lda PiranhaPlant_MoveFlag,x ;check movement flag
    //> bne SetupToMovePPlant       ;if moving, skip to part ahead
    val moveFlag = ram.sprObjYSpeed[1 + x].toInt() and 0xFF
    if (moveFlag == 0) {
        //> lda PiranhaPlant_Y_Speed,x  ;get vertical speed
        //> eor #$ff / clc / adc #$01   ;two's complement to reverse
        //> sta PiranhaPlant_Y_Speed,x
        val speed = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
        ram.sprObjXSpeed[1 + x] = (((speed xor 0xFF) + 1) and 0xFF).toByte()
        //> inc PiranhaPlant_MoveFlag,x
        ram.sprObjYSpeed[1 + x] = ((ram.sprObjYSpeed[1 + x].toInt() and 0xFF) + 1).toByte()
    }

    //> SetupToMovePPlant:
    //> lda PiranhaPlantUpYPos,x    ;get vertical coordinate (swapped for upside-down)
    //> ldy PiranhaPlant_Y_Speed,x
    //> bpl RiseFallPiranhaPlant    ;branch if moving downwards
    //> lda PiranhaPlantDownYPos,x  ;otherwise get other coordinate
    val ySpeed = ram.sprObjXSpeed[1 + x].toInt().toByte().toInt()
    val targetY = if (ySpeed >= 0) {
        ram.sprObjYMFDummy[1 + x].toInt() and 0xFF   // PiranhaPlantUpYPos (lowest for upside-down)
    } else {
        ram.sprObjYMoveForce[1 + x].toInt() and 0xFF // PiranhaPlantDownYPos (highest for upside-down)
    }

    //> RiseFallPiranhaPlant:
    //> lda TimerControl; bne ExMoveUDPP
    if (ram.timerControl != 0.toByte()) return
    //> lda Enemy_Y_Position,x
    //> clc / adc PiranhaPlant_Y_Speed,x
    //> sta Enemy_Y_Position,x
    val curY = ram.sprObjYPos[1 + x].toInt() and 0xFF
    val newY = (curY + ySpeed) and 0xFF
    ram.sprObjYPos[1 + x] = newY.toByte()
    //> cmp $00; bne ExMoveUDPP
    if (newY == targetY) {
        //> lda #$00; sta PiranhaPlant_MoveFlag,x
        ram.sprObjYSpeed[1 + x] = 0
        //> lda #$20; sta EnemyFrameTimer,x
        ram.timers[0x0a + x] = 0x20
    }
    //> ExMoveUDPP: rts
}

/**
 * SMB2J game over handler: continue/retry system.
 * World 9: no continue allowed, go straight to title screen.
 * Otherwise: continue resumes from current world with 3 lives,
 * retry resets completedWorlds and restarts from world 1.
 * Translated from sm2main.asm GameOverMenu / ContinueOrRetry (line 14431).
 */
fun System.smb2jGameOver() {
    //> TerminateGame:
    //> lda #Silence; sta EventMusicQueue
    ram.eventMusicQueue = Constants.Silence

    //> (sm2main) lda WorldNumber; cmp #World9
    //> World 9: no continue allowed
    if (ram.worldNumber == Constants.World9) {
        ram.operModeTask = 0
        ram.screenTimer = 0
        ram.operMode = OperMode.TitleScreen
        return
    }

    //> ContinueOrRetry:
    //> lda ContinueMenuSelect; beq Continue
    if (ram.continueMenuSelect == 0.toByte()) {
        //> Continue:
        //> ldy #$02; sty NumberofLives  ;give 3 lives
        ram.numberofLives = 0x02
        //> sta LevelNumber; sta AreaNumber  ;put at x-1 of current world
        ram.levelNumber = 0x00
        ram.areaNumber = 0x00
        //> sta CoinTally
        ram.coinTally = 0
        //> ldy #$0b; ISCont: sta ScoreAndCoinDisplay,y; dey; bpl ISCont
        ram.playerScoreDisplay.fill(0)
        // Clear remaining bytes of the 12-byte ScoreAndCoinDisplay region
        ram.player2ScoreDisplay.fill(0)
        //> inc Hidden1UpFlag
        ram.hidden1UpFlag = true
        //> jmp ContinueGame
        //> ContinueGame: jsr LoadAreaPointer
        loadAreaPointer()
        //> lda #$01; sta PlayerSize
        ram.playerSize = PlayerSize.Small
        //> inc FetchNewGameTimerFlag
        ram.fetchNewGameTimerFlag = true
        //> lda #$00
        //> sta TimerControl; sta PlayerStatus; sta GameEngineSubroutine; sta OperMode_Task
        ram.timerControl = 0x00
        ram.playerStatus = PlayerStatus.Small
        ram.gameEngineSubroutine = GameEngineRoutine.EntranceGameTimerSetup
        ram.operModeTask = 0x00
        //> lda #$01; sta OperMode
        ram.operMode = OperMode.Game
    } else {
        //> lda #$00; sta CompletedWorlds  ;reset progress
        ram.completedWorlds = 0
        //> jmp TerminateGame
        ram.operModeTask = 0
        ram.screenTimer = 0
        ram.operMode = OperMode.TitleScreen
    }
}

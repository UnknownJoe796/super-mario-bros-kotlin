// by Claude - Game core routine, game engine, and related subroutines
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * GameCoreRoutine: Sets up the current player's joypad bits,
 * dispatches to the appropriate game routine, then runs the main game engine.
 */
fun System.gameCoreRoutine() {
    //> GameCoreRoutine:
    //> ldx CurrentPlayer          ;get which player is on the screen
    //> lda SavedJoypadBits,x      ;use appropriate player's controller bits
    //> sta SavedJoypadBits        ;as the master controller bits
    ram.savedJoypadBits = if (ram.currentPlayer == 0.toByte()) ram.savedJoypad1Bits else ram.savedJoypad2Bits
    //> jsr GameRoutines           ;execute one of many possible subs
    if (debugEnemyTrace) {
        println("[GCR-pre] pX=${ram.playerXPosition.toInt() and 0xFF} pP=${ram.playerPageLoc.toInt() and 0xFF}")
    }
    shadow?.validated("gameroutines", this) { gameRoutines() } ?: gameRoutines()
    // SMB2J: Wind blows player rightward on certain frames
    if (variant == GameVariant.SMB2J) blowPlayerAround()
    if (debugEnemyTrace) {
        println("[GCR-post] pX=${ram.playerXPosition.toInt() and 0xFF} pP=${ram.playerPageLoc.toInt() and 0xFF}")
    }
    //> lda OperMode_Task          ;check major task of operating mode
    //> cmp #$03                   ;if we are supposed to be here,
    //> bcs GameEngine             ;branch to the game engine itself
    if (ram.operModeTask.toUByte() >= 3u) {
        gameEngine()
    }
    //> rts
}

/**
 * GameRoutines: Dispatch table for player state handlers.
 * Selects a subroutine based on GameEngineSubroutine value.
 */
fun System.gameRoutines() {
    //> GameRoutines:
    //> lda GameEngineSubroutine  ;run routine based on number
    //> jsr JumpEngine
    when (ram.gameEngineSubroutine) {
        //> .dw Entrance_GameTimerSetup
        GameEngineRoutine.EntranceGameTimerSetup -> entranceGameTimerSetup()
        //> .dw Vine_AutoClimb
        GameEngineRoutine.VineAutoClimb -> vineAutoClimb()
        //> .dw SideExitPipeEntry
        GameEngineRoutine.SideExitPipeEntry -> sideExitPipeEntry()
        //> .dw VerticalPipeEntry
        GameEngineRoutine.VerticalPipeEntry -> verticalPipeEntry()
        //> .dw FlagpoleSlide
        GameEngineRoutine.FlagpoleSlide -> flagpoleSlide()
        //> .dw PlayerEndLevel
        GameEngineRoutine.PlayerEndLevel -> playerEndLevel()
        //> .dw PlayerLoseLife
        GameEngineRoutine.PlayerLoseLife -> playerLoseLife()
        //> .dw PlayerEntrance
        GameEngineRoutine.PlayerEntrance -> playerEntrance()
        //> .dw PlayerCtrlRoutine
        GameEngineRoutine.PlayerCtrlRoutine -> playerCtrlRoutine()
        //> .dw PlayerChangeSize
        GameEngineRoutine.PlayerChangeSize -> playerChangeSize()
        //> .dw PlayerInjuryBlink
        GameEngineRoutine.PlayerInjuryBlink -> playerInjuryBlink()
        //> .dw PlayerDeath
        GameEngineRoutine.PlayerDeath -> playerDeath()
        //> .dw PlayerFireFlower
        GameEngineRoutine.PlayerFireFlower -> playerFireFlower()
    }
}

/**
 * GameEngine: The main game loop that processes all game objects each frame.
 * Called after GameRoutines when OperMode_Task >= 3.
 */
fun System.gameEngine() {
    if (debugEnemyTrace) {
        println("[GE-start] pX=${ram.playerXPosition.toInt() and 0xFF} pP=${ram.playerPageLoc.toInt() and 0xFF} eX1=${ram.sprObjXPos[2].toInt() and 0xFF} eP1=${ram.sprObjPageLoc[2].toInt() and 0xFF} eState1=${ram.enemyState[1].toInt() and 0xFF} eDir1=${ram.enemyMovingDirs[1]}")
    }
    //> GameEngine:
    //> jsr ProcFireball_Bubble    ;process fireballs and air bubbles
    shadow?.validated("procfireballBubble", this) { procFireballBubble() } ?: procFireballBubble()

    //> ldx #$00
    //> ProcELoop:
    for (x in 0 until 6) {
        //> stx ObjectOffset           ;put incremented offset in X as enemy object offset
        ram.objectOffset = x.toByte()
        //> jsr EnemiesAndLoopsCore    ;process enemy objects
        enemiesAndLoopsCore()
        //> jsr FloateyNumbersRoutine  ;process floatey numbers
        shadow?.validated("floateynumbersroutine", this) { floateyNumbersRoutine(x.toByte()) } ?: floateyNumbersRoutine(x.toByte())
        //> inx
        //> cpx #$06                   ;do these two subroutines until the whole buffer is done
        //> bne ProcELoop
    }

    //> jsr GetPlayerOffscreenBits ;get offscreen bits for player object
    shadow?.validated("getplayeroffscreenbits", this) { getPlayerOffscreenBits() } ?: getPlayerOffscreenBits()
    //> jsr RelativePlayerPosition ;get relative coordinates for player object
    shadow?.validated("relativeplayerposition", this) { relativePlayerPosition() } ?: relativePlayerPosition()
    //> jsr PlayerGfxHandler       ;draw the player
    shadow?.validated("playergfxhandler", this) { playerGfxHandler() } ?: playerGfxHandler()
    //> jsr BlockObjMT_Updater     ;replace block objects with metatiles if necessary
    shadow?.validated("blockobjmtUpdater", this) { blockObjMTUpdater() } ?: blockObjMTUpdater()

    //> ldx #$01
    //> stx ObjectOffset           ;set offset for second
    ram.objectOffset = 1
    //> jsr BlockObjectsCore       ;process second block object
    shadow?.validated("blockobjectscore", this) { blockObjectsCore() } ?: blockObjectsCore()
    //> dex
    //> stx ObjectOffset           ;set offset for first
    ram.objectOffset = 0
    //> jsr BlockObjectsCore       ;process first block object
    shadow?.validated("blockobjectscore", this) { blockObjectsCore() } ?: blockObjectsCore()

    //> jsr MiscObjectsCore        ;process misc objects (hammer, jumping coins)
    shadow?.validated("miscobjectscore", this) { miscObjectsCore() } ?: miscObjectsCore()
    //> jsr ProcessCannons         ;process bullet bill cannons
    shadow?.validated("processcannons", this) { processCannons() } ?: processCannons()
    //> jsr ProcessWhirlpools      ;process whirlpools
    shadow?.validated("processwhirlpools", this) { processWhirlpools() } ?: processWhirlpools()
    //> jsr FlagpoleRoutine        ;process the flagpole
    shadow?.validated("flagpoleroutine", this) { flagpoleRoutine() } ?: flagpoleRoutine()
    //> jsr RunGameTimer           ;count down the game timer
    shadow?.validated("rungametimer", this) { runGameTimer() } ?: runGameTimer()
    //> jsr ColorRotation          ;cycle one of the background colors
    shadow?.validated("colorrotation", this) { colorRotation() } ?: colorRotation()

    //> lda Player_Y_HighPos
    //> cmp #$02                   ;if player is below the screen, don't bother with the music
    //> bpl NoChgMus
    if (ram.playerYHighPos.toUByte() < 2u) {
        //> lda StarInvincibleTimer    ;if star mario invincibility timer at zero,
        //> beq ClrPlrPal              ;skip this part
        if (ram.starInvincibleTimer == 0.toByte()) {
            //> ClrPlrPal:
            //> jsr ResetPalStar           ;do sub to clear player's palette bits in attributes
            resetPalStar()
            // assembly: ClrPlrPal falls through to SaveAB
        } else {
            //> cmp #$04
            //> bne NoChgMus               ;if not yet at a certain point, continue
            if (ram.starInvincibleTimer == 4.toByte()) {
                //> lda IntervalTimerControl   ;if interval timer not yet expired,
                //> bne NoChgMus               ;branch ahead, don't bother with the music
                if (ram.intervalTimerControl == 0.toByte()) {
                    //> jsr GetAreaMusic           ;to re-attain appropriate level music
                    getAreaMusic()
                }
            }
            //> NoChgMus:
            //> ldy StarInvincibleTimer    ;get invincibility timer
            val starTimer = ram.starInvincibleTimer.toInt() and 0xFF
            //> lda FrameCounter           ;get frame counter
            var a = ram.frameCounter.toInt() and 0xFF
            //> cpy #$08                   ;if timer still above certain point,
            //> bcs CycleTwo               ;branch to cycle player's palette quickly
            if (starTimer < 8) {
                //> lsr                        ;otherwise, divide by 8 to cycle every eighth frame
                //> lsr
                a = a shr 2
            }
            //> CycleTwo:
            //> lsr                        ;if branched here, divide by 2 to cycle every other frame
            a = a shr 1
            //> jsr CyclePlayerPalette     ;do sub to cycle the palette (note: shares fire flower code)
            cyclePlayerPalette(a)
            //> jmp SaveAB                 ;then skip this sub to finish up the game engine
        }
    } else {
        //> NoChgMus:
        //> ldy StarInvincibleTimer    ;get invincibility timer
        val starTimer = ram.starInvincibleTimer.toInt() and 0xFF
        //> lda FrameCounter           ;get frame counter
        var a = ram.frameCounter.toInt() and 0xFF
        //> cpy #$08                   ;if timer still above certain point,
        //> bcs CycleTwo               ;branch to cycle player's palette quickly
        if (starTimer < 8) {
            //> lsr                        ;otherwise, divide by 8 to cycle every eighth frame
            //> lsr
            a = a shr 2
        }
        //> CycleTwo:
        //> lsr                        ;if branched here, divide by 2 to cycle every other frame
        a = a shr 1
        //> jsr CyclePlayerPalette     ;do sub to cycle the palette (note: shares fire flower code)
        cyclePlayerPalette(a)
        //> jmp SaveAB                 ;then skip this sub to finish up the game engine
    }

    //> SaveAB:
    //> lda A_B_Buttons            ;save current A and B button
    //> sta PreviousA_B_Buttons    ;into temp variable to be used on next frame
    ram.previousABButtons = ram.aBButtons
    //> lda #$00
    //> sta Left_Right_Buttons     ;nullify left and right buttons temp variable
    ram.leftRightButtons = 0

    //> UpdScrollVar: (fall through from SaveAB)
    shadow?.validated("updscrollvar", this) { updScrollVar() } ?: updScrollVar()
}

/**
 * UpdScrollVar: Updates scroll variables and triggers area parser if needed.
 * Called from GameEngine and also from VictoryMode.
 */
fun System.updScrollVar() {
    //> UpdScrollVar:
    //> lda VRAM_Buffer_AddrCtrl
    //> cmp #$06                   ;if vram address controller set to 6 (one of two $0341s)
    //> beq ExitEng                ;then branch to leave
    if (ram.vRAMBufferAddrCtrl == 6.toByte()) return

    //> lda AreaParserTaskNum      ;otherwise check number of tasks
    //> bne RunParser
    if (ram.areaParserTaskNum == 0.toByte()) {
        //> lda ScrollThirtyTwo        ;get horizontal scroll in 0-31 or $00-$20 range
        //> cmp #$20                   ;check to see if exceeded $21
        //> bmi ExitEng                ;branch to leave if not
        // NES bmi checks N flag after cmp: branches when (A-$20) has bit 7 set.
        // This is a SIGNED comparison, not unsigned: branches for 0x00-0x1F AND 0xA0-0xFF.
        if (ram.scrollThirtyTwo < 0x20.toByte()) return

        //> lda ScrollThirtyTwo
        //> sbc #$20                   ;otherwise subtract $20 to set appropriately
        //> sta ScrollThirtyTwo        ;and store
        // Note: sbc #$20 on NES means (A - $20 - (1 - Carry)). Since bmi followed cmp #$20, 
        // we know A >= $20, so Carry was set. Thus (A - $20).
        ram.scrollThirtyTwo = (ram.scrollThirtyTwo.toInt() - 0x20).toByte()
        //> lda #$00                   ;reset vram buffer offset used in conjunction with
        //> sta VRAM_Buffer2_Offset    ;level graphics buffer at $0341-$035f
        ram.vRAMBuffer2.clear()
    }

    //> RunParser:
    //> jsr AreaParserTaskHandler  ;update the name table with more level graphics
    areaParserTaskHandler()
    //> ExitEng:      rts                        ;and after all that, we're finally done!
}

/**
 * CyclePlayerPalette: Sets player sprite palette bits based on invincibility cycling.
 */
fun System.cyclePlayerPalette(value: Int) {
    //> CyclePlayerPalette:
    //> and #$03              ;mask out all but d1-d0 (previously d3-d2)
    val paletteBits = value and 0x03
    //> sta $00               ;store result here to use as palette bits
    //> lda Player_SprAttrib  ;get player attributes
    //> and #%11111100        ;save any other bits but palette bits
    //> ora $00               ;add palette bits
    //> sta Player_SprAttrib  ;store as new player attributes
    ram.playerSprAttrib = SpriteFlags((ram.playerSprAttrib.byte.toInt() and 0xFC or paletteBits).toByte())
    //> rts
}

/**
 * ResetPalStar: Clears player's palette bits in attributes (forces palette 0).
 */
fun System.resetPalStar() {
    //> ResetPalStar:
    //> lda Player_SprAttrib  ;get player attributes
    //> and #%11111100        ;mask out palette bits to force palette 0
    //> sta Player_SprAttrib  ;store as new player attributes
    ram.playerSprAttrib = SpriteFlags((ram.playerSprAttrib.byte.toInt() and 0xFC).toByte())
    //> rts
}

// by Claude - Stubs moved to dedicated files:
// vineAutoClimb, sideExitPipeEntry, verticalPipeEntry, flagpoleSlide,
// playerEndLevel, playerEntrance → playerLevelTransitions.kt
// playerCtrlRoutine → playerCtrlRoutine.kt
// playerChangeSize, playerInjuryBlink, playerDeath, playerFireFlower → playerStateChanges.kt
// enemiesAndLoopsCore → enemiesAndLoopsCore.kt
// relativePlayerPosition → relativePosition.kt
// playerGfxHandler → playerGfxHandler.kt
// procFireballBubble → procFireballBubble.kt
// getPlayerOffscreenBits → offscreenBits.kt
/**
 * BlockObjMT_Updater: Replaces block objects with their metatiles if the
 * replacement flag is set and the VRAM buffer is free.
 */
fun System.blockObjMTUpdater() {
    //> BlockObjMT_Updater:
    //> ldx #$01                  ;set offset to start with second block object
    //> UpdateLoop:
    for (x in 1 downTo 0) {
        //> stx ObjectOffset          ;set offset here
        ram.objectOffset = x.toByte()
        //> lda VRAM_Buffer1          ;if vram buffer already being used here,
        //> bne NextBUpd              ;branch to move onto next block object
        // In the original, this checks if the first byte of $0300 (buffer control/address) is zero.
        // In our translation, vRAMBuffer1 is a high-level list.
        // The shadow log shows divergences when this is not 0.
        if (ram.vRAMBuffer1.isNotEmpty()) continue
        //> lda Block_RepFlag,x       ;if flag for block object already clear,
        //> beq NextBUpd              ;branch to move onto next block object
        if (ram.blockRepFlags[x] == 0.toByte()) continue
        //> lda Block_BBuf_Low,x      ;get low byte of block buffer
        //> sta $06                   ;store into block buffer address
        val bbLow = ram.blockBBufLow[x].toInt() and 0xFF
        val useBuf2 = bbLow >= 0xD0
        val buffer = if (useBuf2) ram.blockBuffer2 else ram.blockBuffer1
        val col = bbLow and 0x0F
        //> lda Block_Orig_YPos,x     ;get original vertical coordinate of block object
        //> sta $02                   ;store here and use as offset to block buffer
        //> tay
        val vertOfs = ram.blockOrigYPos[x].toInt() and 0xFF
        //> lda Block_Metatile,x      ;get metatile to be written
        //> sta ($06),y               ;write it to the block buffer
        val metatile = ram.blockMetatile[x].toInt() and 0xFF
        val bufIdx = col + vertOfs
        if (bufIdx in buffer.indices) {
            buffer[bufIdx] = metatile.toByte()
        }
        //> jsr ReplaceBlockMetatile  ;do sub to replace metatile where block object is
        replaceBlockMetatile(metatile, bbLow, vertOfs)
        //> lda #$00
        //> sta Block_RepFlag,x       ;clear block object flag
        ram.blockRepFlags[x] = 0
        //> NextBUpd:   dex                       ;decrement block object offset
        //> bpl UpdateLoop            ;do this until both block objects are dealt with
    //> ;$02 - used to store adder to page location
    //> ;$01 - used to store low nybble of horizontal speed
    //> ;$00 - used to store high nybble of horizontal speed as adder
    }
    //> rts
}
// by Claude - Engine subroutine stubs moved to dedicated files:
// blockObjectsCore → blockObjectsCore.kt, miscObjectsCore → miscObjectsCore.kt
// processCannons → processCannons.kt, processWhirlpools → processWhirlpools.kt
// flagpoleRoutine → flagpoleRoutine.kt
/**
 * ForceInjury: Injures or kills the player.
 * If player is small, kills them. If big/fiery, shrinks to small.
 */
fun System.forceInjury() {
    //> ForceInjury:
    //> ldx PlayerStatus          ;check player's status
    //> beq KillPlayer            ;branch if small
    if (ram.playerStatus == PlayerStatus.Small) {
        //> KillPlayer:
        //> stx Player_X_Speed   ;halt player's horizontal movement by initializing speed
        ram.playerXSpeed = 0
        //> inx
        //> stx EventMusicQueue  ;set event music queue to death music
        ram.eventMusicQueue = Constants.DeathMusic
        //> lda #$fc
        //> sta Player_Y_Speed   ;set new vertical speed
        ram.playerYSpeed = 0xFC.toByte()
        //> lda #$0b             ;set subroutine to run on next frame
        //> bne SetKRout         ;branch to set player's state and other things
        ram.gameEngineSubroutine = GameEngineRoutine.PlayerDeath
        ram.playerState = PlayerState.Falling
        ram.timerControl = 0xFF.toByte()
        ram.scrollAmount = 0
        return
    }

    //> sta PlayerStatus          ;otherwise set player's status to small (A=0 from caller or implicit)
    ram.playerStatus = PlayerStatus.Small
    //> lda #$08
    //> sta InjuryTimer           ;set injured invincibility timer
    ram.injuryTimer = 0x08
    //> asl
    //> sta Square1SoundQueue     ;play pipedown/injury sound ($10)
    ram.square1SoundQueue = Constants.Sfx_PipeDown_Injury
    //> jsr GetPlayerColors       ;change player's palette if necessary
    getPlayerColors()
    //> lda #$0a                  ;set subroutine to run on next frame

    //> SetKRout:
    //> ldy #$01                  ;set new player state
    //> SetPRout:
    //> sta GameEngineSubroutine  ;load new value to run subroutine on next frame
    ram.gameEngineSubroutine = GameEngineRoutine.PlayerInjuryBlink
    //> sty Player_State          ;store new player state
    ram.playerState = PlayerState.Falling
    //> ldy #$ff
    //> sty TimerControl          ;set master timer control flag to halt timers
    ram.timerControl = 0xFF.toByte()
    //> iny
    //> sty ScrollAmount          ;initialize scroll speed
    ram.scrollAmount = 0

    //> ExInjColRoutines:
    //> ldx ObjectOffset              ;get enemy offset and leave
    //> rts
}
/**
 * RunGameTimer: Counts down the game timer and handles time-up.
 */
fun System.runGameTimer() {
    //> RunGameTimer:
    //> lda OperMode               ;get primary mode of operation
    //> beq ExGTimer               ;branch to leave if in title screen mode
    if (ram.operMode == OperMode.TitleScreen) return
    //> lda GameEngineSubroutine
    //> cmp #$08                   ;if routine number less than eight running,
    //> bcc ExGTimer               ;branch to leave
    if (ram.gameEngineSubroutine.ordinal < GameEngineRoutine.PlayerCtrlRoutine.ordinal) return
    //> cmp #$0b                   ;if running death routine,
    //> beq ExGTimer               ;branch to leave
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerDeath) return
    //> lda Player_Y_HighPos
    //> cmp #$02                   ;if player below the screen,
    //> bcs ExGTimer               ;branch to leave regardless of level type
    if (ram.playerYHighPos.toUByte() >= 2u) return
    //> lda GameTimerCtrlTimer     ;if game timer control not yet expired,
    //> bne ExGTimer               ;branch to leave
    if (ram.gameTimerCtrlTimer != 0.toByte()) return

    //> lda GameTimerDisplay
    //> ora GameTimerDisplay+1     ;otherwise check game timer digits
    //> ora GameTimerDisplay+2
    //> beq TimeUpOn               ;if game timer digits at 000, branch to time-up code
    val timerZero = ram.gameTimerDisplay[0] == 0.toByte()
            && ram.gameTimerDisplay[1] == 0.toByte()
            && ram.gameTimerDisplay[2] == 0.toByte()

    if (timerZero) {
        //> TimeUpOn:
        //> sta PlayerStatus          ;init player status (note A will always be zero here)
        ram.playerStatus = PlayerStatus.Small
        //> jsr ForceInjury            ;do sub to kill the player (note player is small here)
        forceInjury()
        //> inc GameTimerExpiredFlag   ;set game timer expiration flag
        ram.gameTimerExpiredFlag = true
        //> ExGTimer:  rts
        return
    }

    //> ldy GameTimerDisplay       ;otherwise check first digit
    //> dey                        ;if first digit not on 1,
    //> bne ResGTCtrl              ;branch to reset game timer control
    if (ram.gameTimerDisplay[0] == 1.toByte()) {
        //> lda GameTimerDisplay+1     ;otherwise check second and third digits
        //> ora GameTimerDisplay+2
        //> bne ResGTCtrl              ;if timer not at 100, branch to reset game timer control
        if (ram.gameTimerDisplay[1] == 0.toByte() && ram.gameTimerDisplay[2] == 0.toByte()) {
            //> lda #TimeRunningOutMusic
            //> sta EventMusicQueue        ;otherwise load time running out music
            ram.eventMusicQueue = Constants.TimeRunningOutMusic
        }
    }

    //> ResGTCtrl:
    //> lda #$18                  ;reset game timer control
    //> sta GameTimerCtrlTimer
    ram.gameTimerCtrlTimer = 0x18
    //> ldy #$23                   ;set offset for last digit
    //> lda #$ff                   ;set value to decrement game timer digit
    //> sta DigitModifier+5
    ram.digitModifier[5] = 0xFF.toByte()
    //> jsr DigitsMathRoutine      ;do sub to decrement game timer slowly
    digitsMathRoutine(ram.gameTimerDisplay)
    //> lda #$a4                   ;set status nybbles to update game timer display
    //> jmp PrintStatusBarNumbers  ;do sub to update the display
    printStatusBarNumbers(0xa4.toByte())
    //> ExGTimer:  rts
}

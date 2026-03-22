// by Claude - Player level transition routines: entrance, end-of-level, flagpole, vine, pipes
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

// ---- Data tables ----

//> Hidden1UpCoinAmts:
//> .db $15, $23, $16, $1b, $17, $18, $23, $63
/**
 * Minimum coin amounts (in BCD) required in the third area of each world
 * to activate the hidden 1-up block. Indexed by world number.
 */
private val Hidden1UpCoinAmts: ByteArray = byteArrayOf(
    0x15, 0x23, 0x16, 0x1b, 0x17, 0x18, 0x23, 0x63
)

// ---- Main routines ----

/**
 * Handles the player's entrance into a level. Supports multiple entry modes:
 * - Normal entry: player walks in from left, auto-walks right if above a threshold
 * - Pipe entry (entrance ctrl 6/7): player exits a pipe, then transitions to next area
 * - Vine entry (alt entrance 2 + joypad override): player climbs a vine, then walks right
 * - Pipe rise (alt entrance 2, no override): player rises from below through a pipe
 *
 * Dispatch index 7 from GameRoutines.
 */
fun System.playerEntrance() {
    //> PlayerEntrance:
    //> lda AltEntranceControl    ;check for mode of alternate entry
    //> cmp #$02
    //> beq EntrMode2             ;if found, branch to enter from pipe or with vine
    if (ram.altEntranceControl == 0x02.toByte()) {
        entrMode2()
        return
    }

    //> lda #$00
    //> ldy Player_Y_Position     ;if vertical position above a certain
    //> cpy #$30                  ;point, nullify controller bits and continue
    //> bcc AutoControlPlayer     ;with player movement code, do not return
    if ((ram.playerYPosition.toInt() and 0xFF) < 0x30) {
        autoControlPlayer(0x00)
        return
    }

    //> lda PlayerEntranceCtrl    ;check player entry bits from header
    //> cmp #$06
    //> beq ChkBehPipe            ;if set to 6 or 7, execute pipe intro code
    //> cmp #$07                  ;otherwise branch to normal entry
    //> bne PlayerRdy
    val pec = ram.playerEntranceCtrl.toInt() and 0xFF
    if (pec == 0x06 || pec == 0x07) {
        //> ChkBehPipe: lda Player_SprAttrib      ;check for sprite attributes
        //> bne IntroEntr             ;branch if found
        if (ram.playerSprAttrib.byte != 0.toByte()) {
            //> IntroEntr:  jsr EnterSidePipe         ;execute sub to move player to the right
            enterSidePipe()
            //> dec ChangeAreaTimer       ;decrement timer for change of area
            ram.changeAreaTimer--
            //> bne ExitEntr              ;branch to exit if not yet expired
            if (ram.changeAreaTimer != 0.toByte()) return
            //> inc DisableIntermediate   ;set flag to skip world and lives display
            ram.disableIntermediate = true
            //> jmp NextArea              ;jump to increment to next area and set modes
            nextArea()
            return
        }
        //> lda #$01
        //> jmp AutoControlPlayer     ;force player to walk to the right
        autoControlPlayer(0x01)
        return
    }

    //> PlayerRdy (normal entry):
    playerRdy()
}

/**
 * Alternate entrance mode 2: either rising from a pipe or entering via vine.
 */
private fun System.entrMode2() {
    //> EntrMode2:  lda JoypadOverride        ;if controller override bits set here,
    //> bne VineEntr              ;branch to enter with vine
    if (ram.joypadOverride != 0.toByte()) {
        vineEntr()
        return
    }

    //> lda #$ff                  ;otherwise, set value here then execute sub
    //> jsr MovePlayerYAxis       ;to move player upwards (note $ff = -1)
    movePlayerYAxis(0xFF.toByte())
    //> lda Player_Y_Position     ;check to see if player is at a specific coordinate
    //> cmp #$91                  ;if player risen to a certain point (this requires pipes
    //> bcc PlayerRdy             ;to be at specific height to look/function right) branch
    if ((ram.playerYPosition.toInt() and 0xFF) < 0x91) {
        playerRdy()
        return
    }
    //> rts                       ;to the last part, otherwise leave
}

/**
 * Vine entrance: waits for the vine to reach maximum height, then moves the player
 * off the vine and to the right until reaching position $48.
 */
private fun System.vineEntr() {
    //> VineEntr:   lda VineHeight
    //> cmp #$60                  ;check vine height
    //> bne ExitEntr              ;if vine not yet reached maximum height, branch to leave
    if (ram.vineHeight != 0x60.toByte()) return

    //> lda Player_Y_Position     ;get player's vertical coordinate
    //> cmp #$99                  ;check player's vertical coordinate against preset value
    val playerY = ram.playerYPosition.toInt() and 0xFF

    //> ldy #$00                  ;load default values to be written to
    //> lda #$01                  ;this value moves player to the right off the vine
    //> bcc OffVine               ;if vertical coordinate < preset value, use defaults
    var disableCollision = 0
    var controlA: Byte = 0x01

    if (playerY >= 0x99) {
        //> lda #$03
        //> sta Player_State          ;otherwise set player state to climbing
        ram.playerState = PlayerState.Climbing
        //> iny                       ;increment value in Y
        disableCollision = 1
        //> lda #$08                  ;set block in block buffer to cover hole, then
        //> sta Block_Buffer_1+$b4    ;use same value to force player to climb
        ram.blockBuffer1[0xB4] = 0x08
        controlA = 0x08
    }

    //> OffVine:    sty DisableCollisionDet   ;set collision detection disable flag
    ram.disableCollisionDet = disableCollision.toByte()
    //> jsr AutoControlPlayer     ;use contents of A to move player up or right, execute sub
    autoControlPlayer(controlA)
    //> lda Player_X_Position
    //> cmp #$48                  ;check player's horizontal position
    //> bcc ExitEntr              ;if not far enough to the right, branch to leave
    if ((ram.playerXPosition.toInt() and 0xFF) < 0x48) return

    //> (falls through to PlayerRdy)
    playerRdy()
}

/**
 * Sets the game engine to run the normal player control routine on the next frame.
 * Resets facing direction, alternate entrance control, collision detection, and joypad override.
 */
private fun System.playerRdy() {
    //> PlayerRdy:  lda #$08                  ;set routine to be executed by game engine next frame
    //> sta GameEngineSubroutine
    ram.gameEngineSubroutine = GameEngineRoutine.PlayerCtrlRoutine
    //> lda #$01                  ;set to face player to the right
    //> sta PlayerFacingDir
    ram.playerFacingDir = Direction.Left
    //> lsr                       ;init A (1 >> 1 = 0)
    //> sta AltEntranceControl    ;init mode of entry
    ram.altEntranceControl = 0x00
    //> sta DisableCollisionDet   ;init collision detection disable flag
    ram.disableCollisionDet = 0x00
    //> sta JoypadOverride        ;nullify controller override bits
    ram.joypadOverride = 0x00
    //> ExitEntr:   rts                       ;leave!
}

/**
 * Handles the flagpole slide after the player grabs the flagpole.
 * Transfers any queued flagpole sound to the square 1 sound channel, then
 * forces the player to slide down (climb down) until reaching Y position $9e.
 *
 * Dispatch index 4 from GameRoutines.
 */
fun System.flagpoleSlide() {
    //> FlagpoleSlide:
    //> lda Enemy_ID+5           ;check special use enemy slot
    //> cmp #FlagpoleFlagObject  ;for flagpole flag object
    //> bne NoFPObj              ;if not found, branch to something residual
    if (ram.enemyID[5] != EnemyId.FlagpoleFlagObject.byte) {
        //> NoFPObj:     inc GameEngineSubroutine ;increment to next routine (this may
        ram.gameEngineSubroutine = ram.gameEngineSubroutine.next()
        //> rts                      ;be residual code)
        return
    }

    //> lda FlagpoleSoundQueue   ;load flagpole sound
    //> sta Square1SoundQueue    ;into square 1's sfx queue
    ram.square1SoundQueue = ram.flagpoleSoundQueue
    //> lda #$00
    //> sta FlagpoleSoundQueue   ;init flagpole sound queue
    ram.flagpoleSoundQueue = 0x00

    //> ldy Player_Y_Position
    //> cpy #$9e                 ;check to see if player has slid down
    //> bcs SlidePlayer          ;far enough, and if so, branch with no controller bits set
    val controlBits: Byte = if ((ram.playerYPosition.toInt() and 0xFF) >= 0x9E) {
        //> SlidePlayer: jmp AutoControlPlayer    ;jump to player control routine
        0x00
    } else {
        //> lda #$04                 ;otherwise force player to climb down (to slide)
        0x04
    }
    autoControlPlayer(controlBits)
}

/**
 * Handles the end-of-level sequence after the flagpole. Forces the player to walk
 * right, plays end-of-level music, checks for hidden 1-up eligibility,
 * increments the area, and transitions to the next area.
 *
 * Dispatch index 5 from GameRoutines.
 */
fun System.playerEndLevel() {
    //> PlayerEndLevel:
    //> lda #$01                  ;force player to walk to the right
    //> jsr AutoControlPlayer
    autoControlPlayer(0x01)

    //> lda Player_Y_Position     ;check player's vertical position
    //> cmp #$ae
    //> bcc ChkStop               ;if player is not yet off the flagpole, skip this part
    if ((ram.playerYPosition.toInt() and 0xFF) >= 0xAE) {
        //> lda ScrollLock            ;if scroll lock not set, branch ahead to next part
        //> beq ChkStop               ;because we only need to do this part once
        if (ram.scrollLock != 0.toByte()) {
            //> lda #EndOfLevelMusic
            //> sta EventMusicQueue       ;load win level music in event music queue
            ram.eventMusicQueue = Constants.EndOfLevelMusic
            //> lda #$00
            //> sta ScrollLock            ;turn off scroll lock to skip this part later
            ram.scrollLock = 0x00
        }
    }

    //> ChkStop:  lda Player_CollisionBits  ;get player collision bits
    //> lsr                       ;check for d0 set
    //> bcs RdyNextA              ;if d0 set, skip to next part
    if ((ram.playerCollisionBits.toInt() and 0x01) == 0) {
        //> lda StarFlagTaskControl   ;if star flag task control already set,
        //> bne InCastle              ;go ahead with the rest of the code
        if (ram.starFlagTaskControl == 0.toByte()) {
            //> inc StarFlagTaskControl   ;otherwise set task control now (this gets ball rolling!)
            ram.starFlagTaskControl++
        }
        //> InCastle: lda #%00100000            ;set player's background priority bit to
        //> sta Player_SprAttrib      ;give illusion of being inside the castle
        ram.playerSprAttrib = SpriteFlags(0x20)
    }

    //> RdyNextA: lda StarFlagTaskControl
    //> cmp #$05                  ;if star flag task control not yet set
    //> bne ExitNA                ;beyond last valid task number, branch to leave
    if (ram.starFlagTaskControl != 0x05.toByte()) return

    //> inc LevelNumber           ;increment level number used for game logic
    ram.levelNumber++
    //> lda LevelNumber
    //> cmp #$03                  ;check to see if we have yet reached level -4
    //> bne NextArea              ;and skip this last part here if not
    if (ram.levelNumber == 0x03.toByte()) {
        //> ldy WorldNumber           ;get world number as offset
        //> lda CoinTallyFor1Ups      ;check third area coin tally for bonus 1-ups
        //> cmp Hidden1UpCoinAmts,y   ;against minimum value, if player has not collected
        //> bcc NextArea              ;at least this number of coins, leave flag clear
        val worldIdx = ram.worldNumber.toInt() and 0xFF
        val coinTally = ram.coinTallyFor1Ups.toInt() and 0xFF
        val required = Hidden1UpCoinAmts[worldIdx].toInt() and 0xFF
        if (coinTally >= required) {
            //> inc Hidden1UpFlag         ;otherwise set hidden 1-up box control flag
            ram.hidden1UpFlag = true
        }
    }

    //> NextArea: (falls through)
    nextArea()
}

/**
 * Handles the auto-climb sequence when the player is on a vine at a level exit.
 * Forces the player to climb up until above the status bar, then sets alternate
 * entrance mode and transitions to the next area.
 *
 * Dispatch index 1 from GameRoutines.
 */
fun System.vineAutoClimb() {
    //> Vine_AutoClimb:
    //> lda Player_Y_HighPos   ;check to see whether player reached position
    //> bne AutoClimb          ;above the status bar yet and if so, set modes
    //> lda Player_Y_Position
    //> cmp #$e4
    //> bcc SetEntr
    val highPos = ram.playerYHighPos.toInt() and 0xFF
    val yPos = ram.playerYPosition.toInt() and 0xFF
    if (highPos != 0 || yPos >= 0xE4) {
        //> AutoClimb: lda #%00001000         ;set controller bits override to up
        //> sta JoypadOverride
        ram.joypadOverride = 0x08
        //> ldy #$03               ;set player state to climbing
        //> sty Player_State
        ram.playerState = PlayerState.Climbing
        //> jmp AutoControlPlayer
        autoControlPlayer(0x08)
        return
    }
    //> SetEntr:   lda #$02               ;set starting position to override
    //> sta AltEntranceControl
    ram.altEntranceControl = 0x02
    //> jmp ChgAreaMode        ;set modes
    chgAreaMode()
}

/**
 * Handles the player entering a pipe going down (vertically).
 * Moves the player downward by 1 pixel per frame, scrolls the screen, then
 * transitions to the destination area using the appropriate entry mode.
 *
 * Dispatch index 3 from GameRoutines.
 */
fun System.verticalPipeEntry() {
    //> VerticalPipeEntry:
    //> lda #$01             ;set 1 as movement amount
    //> jsr MovePlayerYAxis  ;do sub to move player downwards
    movePlayerYAxis(0x01)
    //> jsr ScrollHandler    ;do sub to scroll screen with saved force if necessary
    scrollHandler()

    //> ldy #$00             ;load default mode of entry
    //> lda WarpZoneControl  ;check warp zone control variable/flag
    //> bne ChgAreaPipe      ;if set, branch to use mode 0
    var entryMode: Byte = 0x00
    if (ram.warpZoneControl == 0.toByte()) {
        //> iny
        //> lda AreaType         ;check for castle level type
        //> cmp #$03
        //> bne ChgAreaPipe      ;if not castle type level, use mode 1
        entryMode = 0x01
        if (ram.areaType == AreaType.Castle) {
            //> iny
            //> jmp ChgAreaPipe      ;otherwise use mode 2
            entryMode = 0x02
        }
    }
    //> ChgAreaPipe:
    chgAreaPipe(entryMode)
}

/**
 * Handles the player entering a pipe from the side.
 * Moves the player to the right (via [enterSidePipe]) and when the change area
 * timer expires, transitions to the next area with entry mode 2.
 *
 * Dispatch index 2 from GameRoutines.
 */
fun System.sideExitPipeEntry() {
    //> SideExitPipeEntry:
    //> jsr EnterSidePipe         ;execute sub to move player to the right
    enterSidePipe()
    //> ldy #$02
    //> (falls through to ChgAreaPipe)
    chgAreaPipe(0x02)
}

// ---- Shared helpers ----

/**
 * Decrements the change-area timer. When it expires, sets the alternate entrance
 * control mode and disables the screen for the transition.
 */
private fun System.chgAreaPipe(entryMode: Byte) {
    //> ChgAreaPipe: dec ChangeAreaTimer       ;decrement timer for change of area
    ram.changeAreaTimer--
    //> bne ExitCAPipe
    if (ram.changeAreaTimer != 0.toByte()) return
    //> sty AltEntranceControl    ;when timer expires set mode of alternate entry
    ram.altEntranceControl = entryMode
    //> (falls through to ChgAreaMode)
    chgAreaMode()
    //> ExitCAPipe:  rts                       ;leave
}

/**
 * Disables the screen and resets the secondary operation mode and sprite 0 detection.
 * Used during area transitions to blank the screen while loading the new area.
 */
fun System.chgAreaMode() {
    //> ChgAreaMode: inc DisableScreenFlag     ;set flag to disable screen output
    ram.disableScreenFlag = true
    //> lda #$00
    //> sta OperMode_Task         ;set secondary mode of operation
    ram.operModeTask = 0x00
    //> sta Sprite0HitDetectFlag  ;disable sprite 0 check
    ram.sprite0HitDetectFlag = false
    //> ExitCAPipe:  rts                       ;leave
}

/**
 * Moves the player to the right by entering a side pipe. Sets horizontal speed to 8,
 * but if the player's X position lower nybble is 0, stops horizontal speed and nullifies
 * the controller override.
 */
private fun System.enterSidePipe() {
    //> EnterSidePipe:
    //> lda #$08               ;set player's horizontal speed
    //> sta Player_X_Speed
    ram.playerXSpeed = 0x08

    //> ldy #$01               ;set controller right button by default
    //> lda Player_X_Position  ;mask out higher nybble of player's
    //> and #%00001111         ;horizontal position
    //> bne RightPipe
    val lowerNybble = ram.playerXPosition.toInt() and 0x0F
    val controlBits: Byte
    if (lowerNybble != 0) {
        //> RightPipe: tya                    ;use contents of Y to
        controlBits = 0x01
    } else {
        //> sta Player_X_Speed     ;if lower nybble = 0, set as horizontal speed
        ram.playerXSpeed = 0x00
        //> tay                    ;and nullify controller bit override here
        controlBits = 0x00
    }
    //> jsr AutoControlPlayer  ;execute player control routine with ctrl bits nulled
    autoControlPlayer(controlBits)
    //> rts
}

/**
 * Moves the player vertically by adding the given amount to the player's Y position.
 * Positive values move down, negative values ($ff = -1) move up.
 */
private fun System.movePlayerYAxis(amount: Byte) {
    //> MovePlayerYAxis:
    //> clc
    //> adc Player_Y_Position ;add contents of A to player position
    //> sta Player_Y_Position
    ram.playerYPosition = ((ram.playerYPosition.toInt() + (amount.toInt() and 0xFF)) and 0xFF).toUByte()
    //> rts
}

/**
 * Increments to the next area and sets up timers and modes for the area transition.
 * Called at the end of a level or when transitioning through pipes.
 */
private fun System.nextArea() {
    //> NextArea: inc AreaNumber            ;increment area number used for address loader
    ram.areaNumber++
    //> jsr LoadAreaPointer       ;get new level pointer
    loadAreaPointer()
    //> inc FetchNewGameTimerFlag ;set flag to load new game timer
    ram.fetchNewGameTimerFlag = true
    //> jsr ChgAreaMode           ;do sub to set secondary mode, disable screen and sprite 0
    chgAreaMode()
    //> sta HalfwayPage           ;reset halfway page to 0 (beginning)
    ram.halfwayPage = 0x00
    //> lda #Silence
    //> sta EventMusicQueue       ;silence music and leave
    ram.eventMusicQueue = Constants.Silence
    //> ExitNA:   rts
}

/**
 * Stores A in SavedJoypadBits (overriding controller input), then runs the
 * full player control routine. This is the entry point used by automatic player
 * movement sequences (entrance, pipe, flagpole, etc.).
 */
fun System.autoControlPlayer(controlBits: Byte) {
    //> AutoControlPlayer:
    //> sta SavedJoypadBits         ;override controller bits with contents of A if executing here
    ram.savedJoypadBits = JoypadBits(controlBits)
    //> (falls through to PlayerCtrlRoutine)
    playerCtrlRoutine()
}

// ---- Stubs for sub-dependencies not yet translated ----

// scrollHandler() moved to scrollHandler.kt

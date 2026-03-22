// by Claude - Translation of PlayerCtrlRoutine and player physics subroutines from smbdism.asm
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.xor

// ---- Data tables ----

//> JumpMForceData:
//> .db $20, $20, $1e, $28, $28, $0d, $04
private val JumpMForceData: ByteArray = byteArrayOf(
    0x20, 0x20, 0x1e, 0x28, 0x28, 0x0d, 0x04
)

//> FallMForceData:
//> .db $70, $70, $60, $90, $90, $0a, $09
private val FallMForceData: ByteArray = byteArrayOf(
    0x70, 0x70, 0x60, 0x90.toByte(), 0x90.toByte(), 0x0a, 0x09
)

//> PlayerYSpdData:
//> .db $fc, $fc, $fc, $fb, $fb, $fe, $ff
private val PlayerYSpdData: ByteArray = byteArrayOf(
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfb.toByte(), 0xfb.toByte(), 0xfe.toByte(), 0xff.toByte()
)

//> InitMForceData:
//> .db $00, $00, $00, $00, $00, $80, $00
private val InitMForceData: ByteArray = byteArrayOf(
    0x00, 0x00, 0x00, 0x00, 0x00, 0x80.toByte(), 0x00
)

//> MaxLeftXSpdData:
//> .db $d8, $e8, $f0
private val MaxLeftXSpdData: ByteArray = byteArrayOf(
    0xd8.toByte(), 0xe8.toByte(), 0xf0.toByte()
)

//> MaxRightXSpdData:
//> .db $28, $18, $10
//> .db $0c ;used for pipe intros
private val MaxRightXSpdData: ByteArray = byteArrayOf(
    0x28, 0x18, 0x10, 0x0c
)

//> FrictionData:
//> .db $e4, $98, $d0
private val FrictionData: ByteArray = byteArrayOf(
    0xe4.toByte(), 0x98.toByte(), 0xd0.toByte()
)

//> Climb_Y_SpeedData:
//> .db $00, $ff, $01
private val Climb_Y_SpeedData: ByteArray = byteArrayOf(
    0x00, 0xff.toByte(), 0x01
)

//> Climb_Y_MForceData:
//> .db $00, $20, $ff
private val Climb_Y_MForceData: ByteArray = byteArrayOf(
    0x00, 0x20, 0xff.toByte()
)

//> ClimbAdderLow:
//> .db $0e, $04, $fc, $f2
private val ClimbAdderLow: ByteArray = byteArrayOf(
    0x0e, 0x04, 0xfc.toByte(), 0xf2.toByte()
)

//> ClimbAdderHigh:
//> .db $00, $00, $ff, $ff
private val ClimbAdderHigh: ByteArray = byteArrayOf(
    0x00, 0x00, 0xff.toByte(), 0xff.toByte()
)

//> PlayerAnimTmrData:
//> .db $02, $04, $07
private val PlayerAnimTmrData: ByteArray = byteArrayOf(
    0x02, 0x04, 0x07
)

// ---- Stubs for sub-dependencies not yet implemented ----
// These are private to avoid conflicts with placeholder stubs in other files (e.g. victoryMode.kt).
// When those placeholders are removed, these can be made public or replaced with real implementations.

// getPlayerOffscreenBits() in offscreenBits.kt
// relativePlayerPosition() in relativePosition.kt
// movePlayerHorizontally() in movement.kt
// movePlayerVertically() in gravity.kt
// boundingBoxCore() in boundingBox.kt
// scrollHandler() moved to scrollHandler.kt
// playerBGCollision() moved to collisionDetection.kt


// ---- PlayerCtrlRoutine ----

//> PlayerCtrlRoutine:
/**
 * Main player control subroutine called every frame during gameplay.
 * Handles input processing, dispatching to movement/jump/swim physics,
 * calling scroll handler, collision detection, and offscreen bits.
 */
fun System.playerCtrlRoutine() {
    //> lda GameEngineSubroutine    ;check task here
    //> cmp #$0b                    ;if certain value is set, branch to skip controller bit loading
    //> beq SizeChk
    if (ram.gameEngineSubroutine != GameEngineRoutine.PlayerDeath) {
        //> lda AreaType                ;are we in a water type area?
        //> bne SaveJoyp                ;if not, branch
        if (ram.areaType == AreaType.Water) {
            //> ldy Player_Y_HighPos
            //> dey                         ;if not in vertical area between
            //> bne DisJoyp                 ;status bar and bottom, branch
            val yHighMinusOne = (ram.playerYHighPos - 1).toByte()
            if (yHighMinusOne != 0x00.toByte()) {
                //> DisJoyp: lda #$00                    ;disable controller bits
                //> sta SavedJoypadBits
                ram.savedJoypadBits = JoypadBits(0)
            } else {
                //> lda Player_Y_Position
                //> cmp #$d0                    ;if nearing the bottom of the screen or
                //> bcc SaveJoyp                ;not in the vertical area between status bar or bottom,
                if (ram.playerYPosition >= 0xd0u) {
                    //> DisJoyp: lda #$00
                    //> sta SavedJoypadBits
                    ram.savedJoypadBits = JoypadBits(0)
                }
            }
        }

        //> SaveJoyp: lda SavedJoypadBits         ;otherwise store A and B buttons in $0a
        //> and #%11000000
        //> sta A_B_Buttons
        ram.aBButtons = ram.savedJoypadBits.byte and 0b11000000.toByte()
        //> lda SavedJoypadBits         ;store left and right buttons in $0c
        //> and #%00000011
        //> sta Left_Right_Buttons
        ram.leftRightButtons = ram.savedJoypadBits.byte and 0b00000011
        //> lda SavedJoypadBits         ;store up and down buttons in $0b
        //> and #%00001100
        //> sta Up_Down_Buttons
        ram.upDownButtons = ram.savedJoypadBits.byte and 0b00001100
        //> and #%00000100              ;check for pressing down
        //> beq SizeChk                 ;if not, branch
        if (ram.upDownButtons.toInt() and 0b00000100 != 0) {
            //> lda Player_State            ;check player's state
            //> bne SizeChk                 ;if not on the ground, branch
            if (ram.playerState == PlayerState.OnGround) {
                //> ldy Left_Right_Buttons      ;check left and right
                //> beq SizeChk                 ;if neither pressed, branch
                if (ram.leftRightButtons != 0x00.toByte()) {
                    //> lda #$00
                    //> sta Left_Right_Buttons      ;if pressing down while on the ground,
                    //> sta Up_Down_Buttons         ;nullify directional bits
                    ram.leftRightButtons = 0x00
                    ram.upDownButtons = 0x00
                }
            }
        }
    }

    //> SizeChk: jsr PlayerMovementSubs      ;run movement subroutines
    playerMovementSubs()

    //> ldy #$01                    ;is player small?
    //> lda PlayerSize
    //> bne ChkMoveDir
    //> ldy #$00                    ;check for if crouching
    //> lda CrouchingFlag
    //> beq ChkMoveDir              ;if not, branch ahead
    //> ldy #$02                    ;if big and crouching, load y with 2
    //> ChkMoveDir: sty Player_BoundBoxCtrl     ;set contents of Y as player's bounding box size control
    val boundBoxY: Byte = if (ram.playerSize == PlayerSize.Small) {
        0x01 // small
    } else {
        if (ram.crouchingFlag) 0x02 else 0x00 // big crouching or big standing
    }
    ram.playerBoundBoxCtrl = boundBoxY

    //> lda #$01                    ;set moving direction to right by default
    //> ldy Player_X_Speed          ;check player's horizontal speed
    //> beq PlayerSubs              ;if not moving at all horizontally, skip this part
    //> bpl SetMoveDir              ;if moving to the right, use default moving direction
    //> asl                         ;otherwise change to move to the left
    //> SetMoveDir: sta Player_MovingDir        ;set moving direction
    if (ram.playerXSpeed != 0x00.toByte()) {
        ram.playerMovingDir = if (ram.playerXSpeed < 0) Direction.Right else Direction.Left
    }

    //> PlayerSubs: jsr ScrollHandler           ;move the screen if necessary
    scrollHandler()
    //> jsr GetPlayerOffscreenBits  ;get player's offscreen bits
    getPlayerOffscreenBits()
    //> jsr RelativePlayerPosition  ;get coordinates relative to the screen
    relativePlayerPosition()
    //> ldx #$00                    ;set offset for player object
    //> jsr BoundingBoxCore         ;get player's bounding box coordinates
    boundingBoxCore(sprObjOffset = 0, relCoordOffset = 0)
    //> jsr PlayerBGCollision       ;do collision detection and process
    playerBGCollision()

    //> lda Player_Y_Position
    //> cmp #$40                    ;check to see if player is higher than 64th pixel
    //> bcc PlayerHole              ;if so, branch ahead
    val playerYPos = ram.playerYPosition.toInt()
    if (playerYPos >= 0x40) {
        //> lda GameEngineSubroutine
        //> cmp #$05                    ;if running end-of-level routine, branch ahead
        //> beq PlayerHole
        //> cmp #$07                    ;if running player entrance routine, branch ahead
        //> beq PlayerHole
        //> cmp #$04                    ;if running routines $00-$03, branch ahead
        //> bcc PlayerHole
        val ges = ram.gameEngineSubroutine
        if (ges != GameEngineRoutine.PlayerEndLevel && ges != GameEngineRoutine.PlayerEntrance && ges.ordinal >= GameEngineRoutine.FlagpoleSlide.ordinal) {
            //> lda Player_SprAttrib
            //> and #%11011111              ;otherwise nullify player's
            //> sta Player_SprAttrib        ;background priority flag
            ram.playerSprAttrib = SpriteFlags(ram.playerSprAttrib.byte and 0b11011111.toByte())
        }
    }

    //> PlayerHole: lda Player_Y_HighPos        ;check player's vertical high byte
    //> cmp #$02                    ;for below the screen
    //> bmi ExitCtrl                ;branch to leave if not that far down
    if (ram.playerYHighPos < 0x02) return

    //> ldx #$01
    //> stx ScrollLock              ;set scroll lock
    ram.scrollLock = true
    //> ldy #$04
    //> sty $07                     ;set value here
    var hole07: Int = 0x04
    //> ldx #$00                    ;use X as flag, and clear for cloud level
    var dieFlag: Int = 0x00

    //> ldy GameTimerExpiredFlag    ;check game timer expiration flag
    //> bne HoleDie                 ;if set, branch to HoleDie
    //> ldy CloudTypeOverride       ;check for cloud type override
    //> bne ChkHoleX                ;skip to last part if found (cloud level exit, dieFlag stays 0)
    // Fall through to HoleDie if neither condition branches away
    val skipToChkHoleX = !ram.gameTimerExpiredFlag && ram.cloudTypeOverride

    if (!skipToChkHoleX) {
        //> HoleDie: inx                         ;set flag in X for player death
        dieFlag = 0x01
        //> ldy GameEngineSubroutine
        //> cpy #$0b                    ;check for some other routine running
        //> beq ChkHoleX                ;if so, branch ahead
        if (ram.gameEngineSubroutine != GameEngineRoutine.PlayerDeath) {
            //> ldy DeathMusicLoaded        ;check value here
            //> bne HoleBottom              ;if already set, branch to next part
            if (!ram.deathMusicLoaded) {
                //> iny
                //> sty EventMusicQueue         ;otherwise play death music
                //> sty DeathMusicLoaded        ;and set value here
                ram.eventMusicQueue = 0x01
                ram.deathMusicLoaded = true
            }
            //> HoleBottom: ldy #$06
            //> sty $07                     ;change value here
            hole07 = 0x06
        }
    }

    //> ChkHoleX: cmp $07                     ;compare vertical high byte with value set here
    //> bmi ExitCtrl                ;if less, branch to leave
    if (ram.playerYHighPos < hole07.toByte()) return

    //> dex                         ;otherwise decrement flag in X
    //> bmi CloudExit               ;if flag was clear, branch to set modes and other values
    dieFlag--
    if (dieFlag < 0) {
        //> CloudExit:
        //> lda #$00
        //> sta JoypadOverride      ;clear controller override bits if any are set
        ram.joypadOverride = 0x00
        //> jsr SetEntr             ;do sub to set secondary mode
        setEntr()
        //> inc AltEntranceControl  ;set mode of entry to 3
        ram.altEntranceControl++
        return
    }

    //> ldy EventMusicBuffer        ;check to see if music is still playing
    //> bne ExitCtrl                ;branch to leave if so
    if (ram.eventMusicBuffer != 0x00.toByte()) return

    //> lda #$06                    ;otherwise set to run lose life routine
    //> sta GameEngineSubroutine    ;on next frame
    ram.gameEngineSubroutine = GameEngineRoutine.PlayerLoseLife
    //> ExitCtrl: rts
}

// ---- SetEntr stub (referenced by PlayerCtrlRoutine / Vine_AutoClimb) ----

private fun System.setEntr() {
    //> SetEntr: lda #$02               ;set starting position to override
    //> sta AltEntranceControl
    ram.altEntranceControl = 0x02
    //> jmp ChgAreaMode        ;set modes
    chgAreaMode()
}

// chgAreaMode() is in playerLevelTransitions.kt

// ---- PlayerMovementSubs ----

//> PlayerMovementSubs:
/**
 * Movement subroutine dispatcher. Handles crouching, then calls [playerPhysicsSub]
 * for jump/swim physics, then dispatches to the appropriate state sub via jump table.
 */
private fun System.playerMovementSubs() {
    //> lda #$00                  ;set A to init crouch flag by default
    //> ldy PlayerSize            ;is player small?
    //> bne SetCrouch             ;if so, branch
    var crouchVal = false
    if (ram.playerSize == PlayerSize.Big) {
        //> lda Player_State          ;check state of player
        //> bne ProcMove              ;if not on the ground, branch
        if (ram.playerState == PlayerState.OnGround) {
            //> lda Up_Down_Buttons       ;load controller bits for up and down
            //> and #%00000100            ;single out bit for down button
            crouchVal = (ram.upDownButtons.toInt() and 0b00000100) != 0
        }
    }
    //> SetCrouch: sta CrouchingFlag         ;store value in crouch flag
    ram.crouchingFlag = crouchVal

    //> ProcMove: jsr PlayerPhysicsSub      ;run sub related to jumping and swimming
    playerPhysicsSub()

    //> lda PlayerChangeSizeFlag  ;if growing/shrinking flag set,
    //> bne NoMoveSub             ;branch to leave
    if (ram.playerChangeSizeFlag != 0x00.toByte()) return

    //> lda Player_State
    //> cmp #$03                  ;get player state
    //> beq MoveSubs              ;if climbing, branch ahead, leave timer unset
    if (ram.playerState != PlayerState.Climbing) {
        //> ldy #$18
        //> sty ClimbSideTimer        ;otherwise reset timer now
        ram.climbSideTimer = 0x18
    }

    //> MoveSubs: jsr JumpEngine
    //> .dw OnGroundStateSub
    //> .dw JumpSwimSub
    //> .dw FallingSub
    //> .dw ClimbingSub
    when (ram.playerState) {
        PlayerState.OnGround -> onGroundStateSub()
        PlayerState.Falling -> jumpSwimSub()
        PlayerState.FallingAlt -> fallingSub()
        PlayerState.Climbing -> climbingSub()
    }
    //> NoMoveSub: rts
//> ;$00 - used by ClimbingSub to store high vertical adder
}

// ---- OnGroundStateSub ----

//> OnGroundStateSub:
private fun System.onGroundStateSub() {
    //> jsr GetPlayerAnimSpeed     ;do a sub to set animation frame timing
    getPlayerAnimSpeed()
    //> lda Left_Right_Buttons
    //> beq GndMove                ;if left/right controller bits not set, skip instruction
    if (ram.leftRightButtons != 0x00.toByte()) {
        //> sta PlayerFacingDir        ;otherwise set new facing direction
        ram.playerFacingDir = Direction.fromByte(ram.leftRightButtons)
    }
    //> GndMove: jsr ImposeFriction         ;do a sub to impose friction on player's walk/run
    imposeFriction()
    //> jsr MovePlayerHorizontally ;do another sub to move player horizontally
    val scrollVal = movePlayerHorizontally()
    //> sta Player_X_Scroll        ;set returned value as player's movement speed for scroll
    ram.playerXScroll = scrollVal
    //> rts
}

// ---- FallingSub ----

//> FallingSub:
private fun System.fallingSub() {
    //> lda VerticalForceDown
    //> sta VerticalForce      ;dump vertical movement force for falling into main one
    ram.verticalForce = ram.verticalForceDown
    //> jmp LRAir              ;movement force, then skip ahead to process left/right movement
    lrAir()
}

// ---- JumpSwimSub ----

//> JumpSwimSub:
private fun System.jumpSwimSub() {
    //> ldy Player_Y_Speed         ;if player's vertical speed zero
    //> bpl DumpFall               ;or moving downwards, branch to falling
    if (ram.playerYSpeed >= 0) {
        //> DumpFall: lda VerticalForceDown      ;otherwise dump falling into main fractional
        //> sta VerticalForce
        ram.verticalForce = ram.verticalForceDown
    } else {
        //> lda A_B_Buttons
        //> and #A_Button              ;check to see if A button is being pressed
        //> and PreviousA_B_Buttons    ;and was pressed in previous frame
        val aHeld = (ram.aBButtons.toInt() and Constants.A_Button.toInt() and ram.previousABButtons.toInt()) != 0
        if (!aHeld) {
            //> bne ProcSwim               ;if so, branch elsewhere
            //> lda JumpOrigin_Y_Position  ;get vertical position player jumped from
            //> sec
            //> sbc Player_Y_Position      ;subtract current from original vertical coordinate
            //> cmp DiffToHaltJump         ;compare to value set here to see if player is in mid-jump
            //> bcc ProcSwim               ;or just starting to jump, if just starting, skip ahead
            val jumpDist = ((ram.jumpOriginYPosition.toInt() and 0xFF) - (ram.playerYPosition.toInt() and 0xFF)) and 0xFF
            if (jumpDist >= (ram.diffToHaltJump.toInt() and 0xFF)) {
                //> DumpFall: lda VerticalForceDown
                //> sta VerticalForce
                ram.verticalForce = ram.verticalForceDown
            }
        }
    }

    //> ProcSwim: lda SwimmingFlag           ;if swimming flag not set,
    //> beq LRAir                  ;branch ahead to last part
    if (ram.swimmingFlag) {
        //> jsr GetPlayerAnimSpeed     ;do a sub to get animation frame timing
        getPlayerAnimSpeed()
        //> lda Player_Y_Position
        //> cmp #$14                   ;check vertical position against preset value
        //> bcs LRWater                ;if not yet reached a certain position, branch ahead
        if ((ram.playerYPosition.toInt() and 0xFF) < 0x14) {
            //> lda #$18
            //> sta VerticalForce          ;otherwise set fractional
            ram.verticalForce = 0x18
        }
        //> LRWater: lda Left_Right_Buttons     ;check left/right controller bits (check for swimming)
        //> beq LRAir                  ;if not pressing any, skip
        if (ram.leftRightButtons != 0x00.toByte()) {
            //> sta PlayerFacingDir        ;otherwise set facing direction accordingly
            ram.playerFacingDir = Direction.fromByte(ram.leftRightButtons)
        }
    }

    //> LRAir:
    lrAir()
}

/**
 * Shared tail of JumpSwimSub and FallingSub: processes left/right movement in the air,
 * then moves player horizontally and vertically.
 */
private fun System.lrAir() {
    //> LRAir: lda Left_Right_Buttons     ;check left/right controller bits (check for jumping/falling)
    //> beq JSMove                 ;if not pressing any, skip
    if (ram.leftRightButtons != 0x00.toByte()) {
        //> jsr ImposeFriction         ;otherwise process horizontal movement
        imposeFriction()
    }
    //> JSMove: jsr MovePlayerHorizontally ;do a sub to move player horizontally
    val scrollVal = movePlayerHorizontally()
    //> sta Player_X_Scroll        ;set player's speed here, to be used for scroll later
    ram.playerXScroll = scrollVal

    //> lda GameEngineSubroutine
    //> cmp #$0b                   ;check for specific routine selected
    //> bne ExitMov1               ;branch if not set to run
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerDeath) {
        //> lda #$28
        //> sta VerticalForce          ;otherwise set fractional
        ram.verticalForce = 0x28
    }
    //> ExitMov1: jmp MovePlayerVertically   ;jump to move player vertically, then leave
    movePlayerVertically()
}

// ---- ClimbingSub ----

//> ClimbingSub:
private fun System.climbingSub() {
    //> lda Player_YMF_Dummy
    //> clc                      ;add movement force to dummy variable
    //> adc Player_Y_MoveForce   ;save with carry
    //> sta Player_YMF_Dummy
    val dummyAdd = (ram.playerYMFDummy.toInt() and 0xFF) + (ram.playerYMoveForce.toInt() and 0xFF)
    ram.playerYMFDummy = dummyAdd.toByte()
    val carry1 = if (dummyAdd > 0xFF) 1 else 0

    //> ldy #$00                 ;set default adder here
    //> lda Player_Y_Speed       ;get player's vertical speed
    //> bpl MoveOnVine           ;if not moving upwards, branch
    //> dey                      ;otherwise set adder to $ff
    //> MoveOnVine: sty $00                  ;store adder here
    val highAdder: Int = if (ram.playerYSpeed < 0) 0xFF else 0x00

    //> adc Player_Y_Position    ;add carry to player's vertical position
    //> sta Player_Y_Position    ;and store to move player up or down
    val posAdd = (ram.playerYSpeed.toInt() and 0xFF) + carry1 + (ram.playerYPosition.toInt() and 0xFF)
    ram.playerYPosition = posAdd.toUByte()
    val carry2 = if (posAdd > 0xFF) 1 else 0

    //> lda Player_Y_HighPos
    //> adc $00                  ;add carry to player's page location
    //> sta Player_Y_HighPos     ;and store
    ram.playerYHighPos = ((ram.playerYHighPos.toInt() and 0xFF) + highAdder + carry2).toByte()

    //> lda Left_Right_Buttons   ;compare left/right controller bits
    //> and Player_CollisionBits ;to collision flag
    //> beq InitCSTimer          ;if not set, skip to end
    val lrAndCollision = ram.leftRightButtons and ram.playerCollisionBits
    if (lrAndCollision == 0x00.toByte()) {
        //> InitCSTimer: sta ClimbSideTimer       ;initialize timer here
        ram.climbSideTimer = 0x00
        return
    }

    //> ldy ClimbSideTimer       ;otherwise check timer
    //> bne ExitCSub             ;if timer not expired, branch to leave
    if (ram.climbSideTimer != 0x00.toByte()) return

    //> ldy #$18
    //> sty ClimbSideTimer       ;otherwise set timer now
    ram.climbSideTimer = 0x18

    //> ldx #$00                 ;set default offset here
    //> ldy PlayerFacingDir      ;get facing direction
    //> lsr                      ;move right button controller bit to carry
    //> bcs ClimbFD              ;if controller right pressed, branch ahead
    //> inx
    //> inx                      ;otherwise increment offset by 2 bytes
    //> ClimbFD: dey                      ;check to see if facing right
    //> beq CSetFDir             ;if so, branch, do not increment
    //> inx                      ;otherwise increment by 1 byte
    var climbX = 0
    val rightPressed = (lrAndCollision.toInt() and 0x01) != 0
    if (!rightPressed) {
        climbX += 2
    }
    if (ram.playerFacingDir != Direction.Left) {
        climbX += 1
    }

    //> CSetFDir: lda Player_X_Position
    //> clc                      ;add or subtract from player's horizontal position
    //> adc ClimbAdderLow,x      ;using value here as adder and X as offset
    //> sta Player_X_Position
    val xAdd = (ram.playerXPosition.toInt() and 0xFF) + (ClimbAdderLow[climbX].toInt() and 0xFF)
    ram.playerXPosition = xAdd.toUByte()
    val carry3 = if (xAdd > 0xFF) 1 else 0

    //> lda Player_PageLoc       ;add or subtract carry or borrow using value here
    //> adc ClimbAdderHigh,x     ;from the player's page location
    //> sta Player_PageLoc
    ram.playerPageLoc = ((ram.playerPageLoc.toInt() and 0xFF) + (ClimbAdderHigh[climbX].toInt()) + carry3).toByte()

    //> lda Left_Right_Buttons   ;get left/right controller bits again
    //> eor #%00000011           ;invert them and store them while player
    //> sta PlayerFacingDir      ;is on vine to face player in opposite direction
    ram.playerFacingDir = Direction.fromByte((ram.leftRightButtons xor 0b00000011))
    //> ExitCSub: rts
}

// ---- PlayerPhysicsSub ----

//> PlayerPhysicsSub:
/**
 * Core physics subroutine for jumping, swimming, and climbing.
 * Dispatches to climbing physics if player state is climbing,
 * otherwise checks for jump initiation and then processes X physics.
 */
private fun System.playerPhysicsSub() {
    //> lda Player_State          ;check player state
    //> cmp #$03
    //> bne CheckForJumping       ;if not climbing, branch
    if (ram.playerState == PlayerState.Climbing) {
        //> ldy #$00
        //> lda Up_Down_Buttons       ;get controller bits for up/down
        //> and Player_CollisionBits  ;check against player's collision detection bits
        //> beq ProcClimb             ;if not pressing up or down, branch
        var climbY = 0
        val upDownAndCollision = ram.upDownButtons and ram.playerCollisionBits
        if (upDownAndCollision != 0x00.toByte()) {
            //> iny
            climbY = 1
            //> and #%00001000            ;check for pressing up
            //> bne ProcClimb
            if (upDownAndCollision.toInt() and 0b00001000 == 0) {
                //> iny
                climbY = 2
            }
        }
        //> ProcClimb: ldx Climb_Y_MForceData,y  ;load value here
        //> stx Player_Y_MoveForce    ;store as vertical movement force
        ram.playerYMoveForce = Climb_Y_MForceData[climbY]
        //> lda #$08                  ;load default animation timing
        var animTiming: Byte = 0x08
        //> ldx Climb_Y_SpeedData,y   ;load some other value here
        //> stx Player_Y_Speed        ;store as vertical speed
        ram.playerYSpeed = Climb_Y_SpeedData[climbY]
        //> bmi SetCAnim              ;if climbing down, use default animation timing value
        if (ram.playerYSpeed >= 0) {
            //> lsr                       ;otherwise divide timer setting by 2
            animTiming = (animTiming.toInt() ushr 1).toByte()
        }
        //> SetCAnim: sta PlayerAnimTimerSet    ;store animation timer setting and leave
        ram.playerAnimTimerSet = animTiming
        return
    }

    //> CheckForJumping:
    checkForJumping()
}

// ---- CheckForJumping ----

//> CheckForJumping:
private fun System.checkForJumping() {
    //> lda JumpspringAnimCtrl    ;if jumpspring animating,
    //> bne NoJump                ;skip ahead to something else
    if (ram.jumpspringAnimCtrl != 0x00.toByte()) {
        xPhysics()
        return
    }
    //> lda A_B_Buttons           ;check for A button press
    //> and #A_Button
    //> beq NoJump                ;if not, branch to something else
    if (ram.aBButtons.toInt() and Constants.A_Button.toInt() == 0) {
        xPhysics()
        return
    }
    //> and PreviousA_B_Buttons   ;if button not pressed in previous frame, branch
    //> beq ProcJumping
    if (ram.aBButtons.toInt() and Constants.A_Button.toInt() and ram.previousABButtons.toInt() != 0) {
        //> NoJump: jmp X_Physics             ;otherwise, jump to something else
        xPhysics()
        return
    }

    //> ProcJumping:
    //> lda Player_State           ;check player state
    //> beq InitJS                 ;if on the ground, branch
    if (ram.playerState != PlayerState.OnGround) {
        //> lda SwimmingFlag           ;if swimming flag not set, jump to do something else
        //> beq NoJump                 ;to prevent midair jumping, otherwise continue
        if (!ram.swimmingFlag) {
            xPhysics()
            return
        }
        //> lda JumpSwimTimer          ;if jump/swim timer nonzero, branch
        //> bne InitJS
        if (ram.jumpSwimTimer == 0x00.toByte()) {
            //> lda Player_Y_Speed         ;check player's vertical speed
            //> bpl InitJS                 ;if player's vertical speed motionless or down, branch
            if (ram.playerYSpeed < 0) {
                //> jmp X_Physics              ;if timer at zero and player still rising, do not swim
                xPhysics()
                return
            }
        }
    }

    //> InitJS:
    //> lda #$20                   ;set jump/swim timer
    //> sta JumpSwimTimer
    ram.jumpSwimTimer = 0x20
    //> ldy #$00                   ;initialize vertical force and dummy variable
    //> sty Player_YMF_Dummy
    //> sty Player_Y_MoveForce
    ram.playerYMFDummy = 0x00
    ram.playerYMoveForce = 0x00
    //> lda Player_Y_HighPos       ;get vertical high and low bytes of jump origin
    //> sta JumpOrigin_Y_HighPos   ;and store them next to each other here
    ram.jumpOriginYHighPos = ram.playerYHighPos
    //> lda Player_Y_Position
    //> sta JumpOrigin_Y_Position
    ram.jumpOriginYPosition = ram.playerYPosition.toByte()
    //> lda #$01                   ;set player state to jumping/swimming
    //> sta Player_State
    ram.playerState = PlayerState.Falling

    //> lda Player_XSpeedAbsolute  ;check value related to walking/running speed
    //> cmp #$09
    //> bcc ChkWtr                 ;branch if below certain values, increment Y
    //> iny                        ;for each amount equal or exceeded
    //> cmp #$10
    //> bcc ChkWtr
    //> iny
    //> cmp #$19
    //> bcc ChkWtr
    //> iny
    //> cmp #$1c
    //> bcc ChkWtr                 ;note that for jumping, range is 0-4 for Y
    //> iny
    val absSpd = ram.playerXSpeedAbsolute.toInt() and 0xFF
    var jumpY = 0
    if (absSpd >= 0x09) jumpY++
    if (absSpd >= 0x10) jumpY++
    if (absSpd >= 0x19) jumpY++
    if (absSpd >= 0x1c) jumpY++

    //> ChkWtr: lda #$01                   ;set value here (apparently always set to 1)
    //> sta DiffToHaltJump
    ram.diffToHaltJump = 0x01

    //> lda SwimmingFlag           ;if swimming flag disabled, branch
    //> beq GetYPhy
    if (ram.swimmingFlag) {
        //> ldy #$05                   ;otherwise set Y to 5, range is 5-6
        jumpY = 5
        //> lda Whirlpool_Flag         ;if whirlpool flag not set, branch
        //> beq GetYPhy
        if (ram.whirlpoolFlag != 0x00.toByte()) {
            //> iny                        ;otherwise increment to 6
            jumpY = 6
        }
    }

    //> GetYPhy: lda JumpMForceData,y       ;store appropriate jump/swim
    //> sta VerticalForce          ;data here
    ram.verticalForce = JumpMForceData[jumpY]
    //> lda FallMForceData,y
    //> sta VerticalForceDown
    ram.verticalForceDown = FallMForceData[jumpY]
    //> lda InitMForceData,y
    //> sta Player_Y_MoveForce
    ram.playerYMoveForce = InitMForceData[jumpY]
    //> lda PlayerYSpdData,y
    //> sta Player_Y_Speed
    ram.playerYSpeed = PlayerYSpdData[jumpY]

    //> lda SwimmingFlag           ;if swimming flag disabled, branch
    //> beq PJumpSnd
    if (ram.swimmingFlag) {
        //> lda #Sfx_EnemyStomp        ;load swim/goomba stomp sound into
        //> sta Square1SoundQueue      ;square 1's sfx queue
        ram.square1SoundQueue = Constants.Sfx_EnemyStomp
        //> lda Player_Y_Position
        //> cmp #$14                   ;check vertical low byte of player position
        //> bcs X_Physics              ;if below a certain point, branch
        if ((ram.playerYPosition.toInt() and 0xFF) < 0x14) {
            //> lda #$00                   ;otherwise reset player's vertical speed
            //> sta Player_Y_Speed         ;and jump to something else to keep player
            ram.playerYSpeed = 0x00
        }
        //> jmp X_Physics              ;from swimming above water level
        xPhysics()
        return
    }

    //> PJumpSnd: lda #Sfx_BigJump           ;load big mario's jump sound by default
    //> ldy PlayerSize             ;is mario big?
    //> beq SJumpSnd
    //> lda #Sfx_SmallJump         ;if not, load small mario's jump sound
    //> SJumpSnd: sta Square1SoundQueue      ;store appropriate jump sound in square 1 sfx queue
    ram.square1SoundQueue = if (ram.playerSize == PlayerSize.Big) {
        Constants.Sfx_BigJump
    } else {
        Constants.Sfx_SmallJump
    }

    //> X_Physics:
    xPhysics()
}

// ---- X_Physics / GetPlayerAnimSpeed / ImposeFriction ----

//> X_Physics:
/**
 * Processes the player's horizontal physics: determines maximum speeds, friction values,
 * and running speed based on player state, area type, and controller input.
 */
private fun System.xPhysics() {
    //> ldy #$00
    //> sty $00                    ;init value here
    var y = 0
    var frictionOffset = 0

    //> lda Player_State           ;if mario is on the ground, branch
    //> beq ProcPRun
    if (ram.playerState != PlayerState.OnGround) {
        //> lda Player_XSpeedAbsolute  ;check something that seems to be related
        //> cmp #$19                   ;to mario's speed
        //> bcs GetXPhy                ;if =>$19 branch here
        //> bcc ChkRFast               ;if not branch elsewhere
        if ((ram.playerXSpeedAbsolute.toInt() and 0xFF) >= 0x19) {
            // jump to GetXPhy
        } else {
            // jump to ChkRFast
            y++
            frictionOffset++
            if ((ram.runningSpeed.toInt() and 0xFF) != 0) {
                // FastXSp: frictionOffset++, then jump to GetXPhy
                frictionOffset++
            } else {
                if ((ram.playerXSpeedAbsolute.toInt() and 0xFF) >= 0x21) {
                    // FastXSp
                    frictionOffset++
                }
            }
        }
    } else {
        //> ProcPRun: iny                        ;if mario on the ground, increment Y
        y++
        //> lda AreaType               ;check area type
        //> beq ChkRFast               ;if water type, branch
        if (ram.areaType == AreaType.Water) {
            // ChkRFast
            //> ChkRFast: iny                        ;if running timer not set or level type is water,
            //> inc $00                    ;increment Y again and temp variable in memory
            y++
            frictionOffset++
            //> lda RunningSpeed
            //> bne FastXSp                ;if running speed set here, branch
            if ((ram.runningSpeed.toInt() and 0xFF) != 0) {
                //> FastXSp: inc $00                    ;if running speed set or speed => $21 increment $00
                frictionOffset++
            } else {
                //> lda Player_XSpeedAbsolute
                //> cmp #$21                   ;otherwise check player's walking/running speed
                //> bcc GetXPhy                ;if less than a certain amount, branch ahead
                if ((ram.playerXSpeedAbsolute.toInt() and 0xFF) >= 0x21) {
                    //> FastXSp: inc $00
                    frictionOffset++
                }
            }
            //> jmp GetXPhy
        } else {
            //> dey                        ;decrement Y by default for non-water type area
            y--
            //> lda Left_Right_Buttons     ;get left/right controller bits
            //> cmp Player_MovingDir       ;check against moving direction
            //> bne ChkRFast               ;if controller bits <> moving direction, skip this part
            if (ram.leftRightButtons == ram.playerMovingDir.byte) {
                //> lda A_B_Buttons            ;check for b button pressed
                //> and #B_Button
                //> bne SetRTmr                ;if pressed, skip ahead to set timer
                if (ram.aBButtons.toInt() and Constants.B_Button.toInt() != 0) {
                    //> SetRTmr: lda #$0a                   ;if b button pressed, set running timer
                    //> sta RunningTimer
                    ram.runningTimer = 0x0a
                } else {
                    //> lda RunningTimer           ;check for running timer set
                    //> bne GetXPhy                ;if set, branch
                    if (ram.runningTimer == 0x00.toByte()) {
                        //> ChkRFast:
                        y++
                        frictionOffset++
                        if ((ram.runningSpeed.toInt() and 0xFF) != 0) {
                            frictionOffset++
                        } else {
                            if ((ram.playerXSpeedAbsolute.toInt() and 0xFF) >= 0x21) {
                                frictionOffset++
                            }
                        }
                    }
                }
            } else {
                //> ChkRFast:
                y++
                frictionOffset++
                if ((ram.runningSpeed.toInt() and 0xFF) != 0) {
                    frictionOffset++
                } else {
                    if ((ram.playerXSpeedAbsolute.toInt() and 0xFF) >= 0x21) {
                        frictionOffset++
                    }
                }
            }
        }
    }

    //> GetXPhy: lda MaxLeftXSpdData,y      ;get maximum speed to the left
    //> sta MaximumLeftSpeed
    ram.maximumLeftSpeed = MaxLeftXSpdData[y]

    //> lda GameEngineSubroutine   ;check for specific routine running
    //> cmp #$07                   ;(player entrance)
    //> bne GetXPhy2               ;if not running, skip and use old value of Y
    var rightY = y
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerEntrance) {
        //> ldy #$03                   ;otherwise set Y to 3
        rightY = 3
    }
    //> GetXPhy2: lda MaxRightXSpdData,y     ;get maximum speed to the right
    //> sta MaximumRightSpeed
    ram.maximumRightSpeed = MaxRightXSpdData[rightY]

    //> ldy $00                    ;get other value in memory
    //> lda FrictionData,y         ;get value using value in memory as offset
    //> sta FrictionAdderLow
    ram.frictionAdderLow = FrictionData[frictionOffset]
    //> lda #$00
    //> sta FrictionAdderHigh      ;init something here
    ram.frictionAdderHigh = 0x00

    //> lda PlayerFacingDir
    //> cmp Player_MovingDir       ;check facing direction against moving direction
    //> beq ExitPhy                ;if the same, branch to leave
    if (ram.playerFacingDir != ram.playerMovingDir) {
        //> asl FrictionAdderLow       ;otherwise shift d7 of friction adder low into carry
        //> rol FrictionAdderHigh      ;then rotate carry onto d0 of friction adder high
        val shiftResult = (ram.frictionAdderLow.toInt() and 0xFF) shl 1
        ram.frictionAdderLow = shiftResult.toByte()
        ram.frictionAdderHigh = ((ram.frictionAdderHigh.toInt() and 0xFF) shl 1 or (if (shiftResult > 0xFF) 1 else 0)).toByte()
    }
    //> ExitPhy: rts
}

// ---- GetPlayerAnimSpeed ----

//> GetPlayerAnimSpeed:
/**
 * Sets the player's animation timer based on walking/running speed.
 * Also handles skid detection and running speed flag.
 */
private fun System.getPlayerAnimSpeed() {
    //> ldy #$00                   ;initialize offset in Y
    var y = 0
    //> lda Player_XSpeedAbsolute  ;check player's walking/running speed
    //> cmp #$1c                   ;against preset amount
    val absSpd = ram.playerXSpeedAbsolute.toInt() and 0xFF
    //> bcs SetRunSpd              ;if greater than a certain amount, branch ahead
    if (absSpd < 0x1c) {
        //> iny                        ;otherwise increment Y
        y++
        //> cmp #$0e                   ;compare against lower amount
        //> bcs ChkSkid                ;if greater than this but not greater than first, skip increment
        if (absSpd < 0x0e) {
            //> iny                        ;otherwise increment Y again
            y++
        }

        //> ChkSkid: lda SavedJoypadBits        ;get controller bits
        //> and #%01111111             ;mask out A button
        //> beq SetAnimSpd             ;if no other buttons pressed, branch ahead of all this
        val masked = ram.savedJoypadBits.byte.toInt() and 0b01111111
        if (masked != 0) {
            //> and #$03                   ;mask out all others except left and right
            //> cmp Player_MovingDir       ;check against moving direction
            //> bne ProcSkid               ;if left/right controller bits <> moving direction, branch
            if ((masked and 0x03).toByte() != ram.playerMovingDir.byte) {
                //> ProcSkid: lda Player_XSpeedAbsolute  ;check player's walking/running speed
                //> cmp #$0b                   ;against one last amount
                //> bcs SetAnimSpd             ;if greater than this amount, branch
                if (absSpd < 0x0b) {
                    //> lda PlayerFacingDir
                    //> sta Player_MovingDir       ;otherwise use facing direction to set moving direction
                    ram.playerMovingDir = ram.playerFacingDir
                    //> lda #$00
                    //> sta Player_X_Speed         ;nullify player's horizontal speed
                    //> sta Player_X_MoveForce     ;and dummy variable for player
                    ram.playerXSpeed = 0x00
                    ram.playerXMoveForce = 0x00
                }
            } else {
                //> lda #$00                   ;otherwise set zero value here
                //> SetRunSpd: sta RunningSpeed           ;store zero or running speed here
                //> jmp SetAnimSpd
                ram.runningSpeed = 0x00
            }
        }
    } else {
        //> SetRunSpd: sta RunningSpeed
        // Note: A still holds absSpd (as a byte), but the original stores A which is Player_XSpeedAbsolute
        ram.runningSpeed = ram.playerXSpeedAbsolute
    }

    //> SetAnimSpd: lda PlayerAnimTmrData,y    ;get animation timer setting using Y as offset
    //> sta PlayerAnimTimerSet
    ram.playerAnimTimerSet = PlayerAnimTmrData[y]
    //> rts
//> ;$00 - used to store offset to friction data
}

// ---- ImposeFriction ----

//> ImposeFriction:
/**
 * Applies friction or acceleration to the player's horizontal speed
 * based on controller input and collision bits.
 */
private fun System.imposeFriction() {
    //> and Player_CollisionBits  ;perform AND between left/right controller bits and collision flag
    //> cmp #$00                  ;then compare to zero (this instruction is redundant)
    //> bne JoypFrict             ;if any bits set, branch to next part
    val lrAndCollision = ram.leftRightButtons and ram.playerCollisionBits
    if (lrAndCollision == 0x00.toByte()) {
        //> lda Player_X_Speed
        //> beq SetAbsSpd             ;if player has no horizontal speed, branch ahead to last part
        if (ram.playerXSpeed == 0x00.toByte()) {
            //> SetAbsSpd: sta Player_XSpeedAbsolute ;store walking/running speed here and leave
            ram.playerXSpeedAbsolute = 0x00
            return
        }
        //> bpl RghtFrict             ;if player moving to the right, branch to slow
        //> bmi LeftFrict             ;otherwise logic dictates player moving left, branch to slow
        if (ram.playerXSpeed >= 0) {
            applyRightFriction()
        } else {
            applyLeftFriction()
        }
        return
    }

    //> JoypFrict: lsr                       ;put right controller bit into carry
    //> bcc RghtFrict             ;if left button pressed, carry = 0, thus branch
    if (lrAndCollision.toInt() and 0x01 == 0) {
        // Left button pressed (bit 1 set, bit 0 clear) -> apply right friction (decelerate right / accel left)
        applyRightFriction()
    } else {
        // Right button pressed -> apply left friction (accelerate right)
        applyLeftFriction()
    }
}

/**
 * Accelerates player to the left (or decelerates rightward movement).
 * Adds friction to move speed in the negative (left) direction.
 */
private fun System.applyLeftFriction() {
    //> LeftFrict: lda Player_X_MoveForce    ;load value set here
    //> clc
    //> adc FrictionAdderLow      ;add to it another value set here
    //> sta Player_X_MoveForce    ;store here
    val forceAdd = (ram.playerXMoveForce.toInt() and 0xFF) + (ram.frictionAdderLow.toInt() and 0xFF)
    ram.playerXMoveForce = forceAdd.toByte()
    val carry = if (forceAdd > 0xFF) 1 else 0

    //> lda Player_X_Speed
    //> adc FrictionAdderHigh     ;add value plus carry to horizontal speed
    //> sta Player_X_Speed        ;set as new horizontal speed
    val newSpeed = ram.playerXSpeed.toInt() + ram.frictionAdderHigh.toInt() + carry
    ram.playerXSpeed = newSpeed.toByte()

    //> cmp MaximumRightSpeed     ;compare against maximum value for right movement
    //> bmi XSpdSign              ;if horizontal speed greater negatively, branch
    // Signed comparison: if newSpeed (as signed) < maximumRightSpeed (as signed), branch
    if (ram.playerXSpeed >= ram.maximumRightSpeed) {
        //> lda MaximumRightSpeed     ;otherwise set preset value as horizontal speed
        //> sta Player_X_Speed        ;thus slowing the player's left movement down
        ram.playerXSpeed = ram.maximumRightSpeed
        //> jmp SetAbsSpd
        setAbsoluteSpeed()
        return
    }

    //> XSpdSign:
    setAbsoluteSpeedWithSign()
}

/**
 * Accelerates player to the right (or decelerates leftward movement).
 * Subtracts friction from move speed.
 */
private fun System.applyRightFriction() {
    //> RghtFrict: lda Player_X_MoveForce    ;load value set here
    //> sec
    //> sbc FrictionAdderLow      ;subtract from it another value set here
    //> sta Player_X_MoveForce    ;store here
    val forceSub = (ram.playerXMoveForce.toInt() and 0xFF) - (ram.frictionAdderLow.toInt() and 0xFF)
    ram.playerXMoveForce = forceSub.toByte()
    val borrow = if (forceSub < 0) 1 else 0

    //> lda Player_X_Speed
    //> sbc FrictionAdderHigh     ;subtract value plus borrow from horizontal speed
    //> sta Player_X_Speed        ;set as new horizontal speed
    val newSpeed = ram.playerXSpeed.toInt() - ram.frictionAdderHigh.toInt() - borrow
    ram.playerXSpeed = newSpeed.toByte()

    //> cmp MaximumLeftSpeed      ;compare against maximum value for left movement
    //> bpl XSpdSign              ;if horizontal speed greater positively, branch
    // Signed comparison: if newSpeed (as signed) >= maximumLeftSpeed (as signed), branch
    if (ram.playerXSpeed < ram.maximumLeftSpeed) {
        //> lda MaximumLeftSpeed      ;otherwise set preset value as horizontal speed
        //> sta Player_X_Speed        ;thus slowing the player's right movement down
        ram.playerXSpeed = ram.maximumLeftSpeed
    }

    //> XSpdSign:
    setAbsoluteSpeedWithSign()
}

/**
 * Computes the absolute (unsigned) value of horizontal speed and stores it.
 */
private fun System.setAbsoluteSpeedWithSign() {
    //> XSpdSign: cmp #$00                  ;if player not moving or moving to the right,
    //> bpl SetAbsSpd             ;branch and leave horizontal speed value unmodified
    if (ram.playerXSpeed >= 0) {
        ram.playerXSpeedAbsolute = ram.playerXSpeed
    } else {
        //> eor #$ff
        //> clc                       ;otherwise get two's compliment to get absolute
        //> adc #$01                  ;unsigned walking/running speed
        ram.playerXSpeedAbsolute = ((ram.playerXSpeed.toInt() xor 0xFF) + 1).toByte()
    }
    //> SetAbsSpd: sta Player_XSpeedAbsolute ;store walking/running speed here and leave
//> ;$07 - used to store pseudorandom bit in BubbleCheck
//> ;$02 - used to store maximum vertical speed in FireballObjCore
//> ;$00 - used to store downward movement force in FireballObjCore
}

/**
 * Stores the current horizontal speed directly as the absolute speed value.
 * Used when speed has already been capped at a known-positive maximum (MaximumRightSpeed).
 */
private fun System.setAbsoluteSpeed() {
    //> SetAbsSpd: sta Player_XSpeedAbsolute
    // After capping at MaximumRightSpeed (always positive), A is stored directly.
    ram.playerXSpeedAbsolute = ram.playerXSpeed
}

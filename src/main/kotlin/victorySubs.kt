// by Claude - BridgeCollapse and TerminateGame subroutines
// Translates BridgeCollapse (bridge collapse animation after axe hit) and
// TerminateGame (end-of-game routine) from smbdism.asm.
package com.ivieleague.smbtranslation

import kotlin.experimental.and
import kotlin.experimental.xor

//> BridgeCollapseData:
//> .db $1a ;axe
//> .db $58 ;chain
//> .db $98, $96, $94, $92, $90, $8e, $8c ;bridge
//> .db $8a, $88, $86, $84, $82, $80
private val bridgeCollapseData = byteArrayOf(
    0x1a,
    0x58,
    0x98.toByte(), 0x96.toByte(), 0x94.toByte(), 0x92.toByte(), 0x90.toByte(), 0x8e.toByte(), 0x8c.toByte(),
    0x8a.toByte(), 0x88.toByte(), 0x86.toByte(), 0x84.toByte(), 0x82.toByte(), 0x80.toByte(),
)

/**
 * Bowser bridge collapse animation sequence.
 * Called as task 0 of VictoryModeSubroutines after the player hits the axe.
 * Removes bridge metatiles one at a time, animates Bowser's feet,
 * plays sounds, and eventually sends Bowser falling.
 */
fun System.bridgeCollapse() {
    //> BridgeCollapse:
    //> ldx BowserFront_Offset    ;get enemy offset for bowser
    val x = ram.bowserFrontOffset.toInt() and 0xFF
    //> lda Enemy_ID,x            ;check enemy object identifier for bowser
    //> cmp #Bowser               ;if not found, branch ahead,
    //> bne SetM2                 ;metatile removal not necessary
    if (ram.enemyID[x] != EnemyId.Bowser.byte) {
        return setM2()
    }

    //> stx ObjectOffset          ;store as enemy offset here
    ram.objectOffset = x.toByte()
    //> lda Enemy_State,x         ;if bowser in normal state, skip all of this
    //> beq RemoveBridge
    val enemyState = ram.enemyState[x]
    if (enemyState == 0.toByte()) {
        // Bowser in normal state: remove bridge metatiles
        removeBridge(x)
        return
    }

    //> and #%01000000            ;if bowser's state has d6 clear, skip to silence music
    //> beq SetM2
    if (enemyState and 0b01000000 == 0.toByte()) {
        return setM2()
    }

    //> lda Enemy_Y_Position,x    ;check bowser's vertical coordinate
    //> cmp #$e0                  ;if bowser not yet low enough, skip this part ahead
    //> bcc MoveD_Bowser
    if ((ram.sprObjYPos[x + 1].toInt() and 0xFF) < 0xe0) {
        //> MoveD_Bowser:
        //> jsr MoveEnemySlowVert     ;do a sub to move bowser downwards
        moveEnemySlowVert()
        //> jmp BowserGfxHandler      ;jump to draw bowser's front and rear, then leave
        bowserGfxHandler()
        return
    }

    // Bowser has fallen far enough: silence, advance mode, and kill enemies
    return setM2()
}

/**
 * SetM2: Silence music, advance to next victory mode task, and kill all enemies.
 * Shared exit path for BridgeCollapse when bridge removal is complete or Bowser not found.
 */
private fun System.setM2() {
    //> SetM2: lda #Silence              ;silence music
    //> sta EventMusicQueue
    ram.eventMusicQueue = Constants.Silence
    //> inc OperMode_Task         ;move onto next secondary mode in autoctrl mode
    ram.operModeTask++
    //> jmp KillAllEnemies        ;jump to empty all enemy slots and then leave
    killAllEnemies()
}

/**
 * Inner portion of BridgeCollapse: removes bridge metatiles one at a time,
 * animates Bowser's feet, plays collapse sounds, and eventually triggers
 * Bowser's fall when all bridge pieces are removed.
 */
private fun System.removeBridge(x: Int) {
    //> RemoveBridge:
    //> dec BowserFeetCounter     ;decrement timer to control bowser's feet
    ram.bowserFeetCounter = (ram.bowserFeetCounter - 1).toByte()
    //> bne NoBFall               ;if not expired, skip all of this
    if (ram.bowserFeetCounter != 0.toByte()) {
        //> NoBFall: jmp BowserGfxHandler      ;jump to code that draws bowser
        bowserGfxHandler()
        return
    }

    //> lda #$04
    //> sta BowserFeetCounter     ;otherwise, set timer now
    ram.bowserFeetCounter = 0x04
    //> lda BowserBodyControls
    //> eor #$01                  ;invert bit to control bowser's feet
    //> sta BowserBodyControls
    ram.bowserBodyControls = (ram.bowserBodyControls xor 0x01.toByte())

    //> lda #$22                  ;put high byte of name table address here for now
    //> sta $05
    //> ldy BridgeCollapseOffset  ;get bridge collapse offset here
    val collapseOffset = ram.bridgeCollapseOffset.toInt() and 0xFF
    //> lda BridgeCollapseData,y  ;load low byte of name table address and store here
    //> sta $04
    // Assembly stores nametable address in scratch $04/$05 (not currentNTAddr)
    val ntAddr = (0x22 shl 8) or (bridgeCollapseData[collapseOffset].toInt() and 0xFF)

    //> ldy VRAM_Buffer1_Offset   ;increment vram buffer offset
    //> iny
    // (VRAM buffer offset tracking not modeled; we append to buffer directly)

    //> ldx #$0c                  ;set offset for tile data for sub to draw blank metatile
    //> jsr RemBridge             ;do sub here to remove bowser's bridge metatiles
    // $0c byte offset / 4 bytes per entry = index 3 = blank metatile in blockGfxData
    remBridge(3, ntAddr)

    //> ldx ObjectOffset          ;get enemy offset
    //> jsr MoveVOffset           ;set new vram buffer offset
    // (VRAM buffer offset tracking not modeled in this port)

    //> lda #Sfx_Blast            ;load the fireworks/gunfire sound into the square 2 sfx
    //> sta Square2SoundQueue     ;queue while at the same time loading the brick
    ram.square2SoundQueue = Constants.Sfx_Blast
    //> lda #Sfx_BrickShatter     ;shatter sound into the noise sfx queue thus
    //> sta NoiseSoundQueue       ;producing the unique sound of the bridge collapsing
    ram.noiseSoundQueue = Constants.Sfx_BrickShatter

    //> inc BridgeCollapseOffset  ;increment bridge collapse offset
    ram.bridgeCollapseOffset = (ram.bridgeCollapseOffset + 1).toByte()
    //> lda BridgeCollapseOffset
    //> cmp #$0f                  ;if bridge collapse offset has not yet reached
    //> bne NoBFall               ;the end, go ahead and skip this part
    if ((ram.bridgeCollapseOffset.toInt() and 0xFF) != 0x0f) {
        //> NoBFall: jmp BowserGfxHandler
        bowserGfxHandler()
        return
    }

    //> jsr InitVStf              ;initialize whatever vertical speed bowser has
    initVStf(x)
    //> lda #%01000000
    //> sta Enemy_State,x         ;set bowser's state to one of defeated states (d6 set)
    ram.enemyState[x] = 0b01000000
    //> lda #Sfx_BowserFall
    //> sta Square2SoundQueue     ;play bowser defeat sound
    ram.square2SoundQueue = Constants.Sfx_BowserFall
    //> NoBFall: jmp BowserGfxHandler      ;jump to code that draws bowser
    bowserGfxHandler()
}

/**
 * Empties all enemy slots and clears the frenzy buffer.
 * Corresponds to the KillAllEnemies label in the original assembly.
 */
fun System.killAllEnemies() {
    //> KillAllEnemies:
    //> ldx #$04              ;start with last enemy slot
    //> KillLoop: jsr EraseEnemyObject  ;branch to kill enemy objects
    //> dex                   ;move onto next enemy slot
    //> bpl KillLoop          ;do this until all slots are emptied
    for (slot in 4 downTo 0) {
        eraseEnemyObject(slot)
    }
    //> sta EnemyFrenzyBuffer ;empty frenzy buffer
    ram.enemyFrenzyBuffer = 0
    //> ldx ObjectOffset      ;get enemy object offset and leave
    // (ObjectOffset already set by caller)
    //> rts
}

/**
 * Initializes vertical speed and movement force for enemy at offset [x] to zero.
 * Corresponds to the InitVStf label.
 */
private fun System.initVStf(x: Int) {
    //> InitVStf: lda #$00                    ;initialize vertical speed
    //> sta Enemy_Y_Speed,x         ;and movement force
    ram.sprObjYSpeed[x + 1] = 0
    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYMoveForce[x + 1] = 0
    //> rts
}

/**
 * Moves enemy downward slowly (for Bowser falling off bridge).
 * Corresponds to MoveEnemySlowVert: sets downForce=$0f and maxSpeed=$02.
 */
private fun System.moveEnemySlowVert() {
    //> MoveEnemySlowVert:
    //> ldy #$0f         ;set movement amount for bowser/other objects
    //> SetMdMax: lda #$02         ;set maximum speed in A
    //> bne SetXMoveAmt  ;unconditional branch
    //> SetXMoveAmt: sty $00                 ;set movement amount here
    //> inx                     ;increment X for enemy offset
    //> jsr ImposeGravitySprObj ;do a sub to move enemy object downwards
    //> ldx ObjectOffset        ;get enemy object buffer offset and leave
    val x = ram.objectOffset.toInt()
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = 0x0f, maxSpeed = 0x02)
    //> rts
}

/**
 * Silence music, switch to title screen mode, and reset operation tasks.
 * Called when the game ends (after World 8 victory or all lives lost).
 * Also called from GameOver mode (see operModeExecutionTree.kt).
 */
fun System.terminateGame() {
    //> TerminateGame:
    //> lda #Silence          ;silence music
    //> sta EventMusicQueue
    ram.eventMusicQueue = Constants.Silence
    //> jsr TransposePlayers  ;check if other player can keep
    //> bcc ContinueGame      ;going, and do so if possible
    val gameEnds = transposePlayers()
    if (!gameEnds) {
        //> ContinueGame:
        continueGame()
        return
    }
    //> lda WorldNumber       ;otherwise put world number of current
    //> sta ContinueWorld     ;player into secret continue function variable
    ram.continueWorld = ram.worldNumber
    //> lda #$00
    //> asl                   ;residual ASL instruction
    //> sta OperMode_Task     ;reset all modes to title screen and
    ram.operModeTask = 0
    //> sta ScreenTimer       ;leave
    ram.screenTimer = 0
    //> sta OperMode
    ram.operMode = OperMode.TitleScreen
    //> rts
}

// BowserGfxHandler: now implemented in bowserRoutine.kt

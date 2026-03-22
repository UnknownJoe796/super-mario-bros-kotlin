// by Claude - EnemyToBGCollisionDet, PlayerHeadCollision, RemoveCoin_Axe, HandleWarpZone
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*

// ---------------------------------------------------------------------------
// Data tables
// ---------------------------------------------------------------------------

//> WarpZoneNumbers:
//>   .db $04, $03, $02, $00
//>   .db $24, $05, $24, $00
//>   .db $08, $07, $06, $00
private val warpZoneNumbers = byteArrayOf(
    0x04, 0x03, 0x02, 0x00,
    0x24, 0x05, 0x24, 0x00,
    0x08, 0x07, 0x06, 0x00
)

//> WorldAddrOffsets:
//>   .db World1Areas-AreaAddrOffsets, World2Areas-AreaAddrOffsets, ...
private val worldAddrOffsets = byteArrayOf(
    0x00, 0x05, 0x0a, 0x0e,
    0x13, 0x17, 0x1b, 0x20
)

//> AreaAddrOffsets: (all world areas concatenated)
private val areaAddrOffsets = byteArrayOf(
    // World1Areas
    0x25, 0x29, 0xc0.toByte(), 0x26, 0x60,
    // World2Areas
    0x28, 0x29, 0x01, 0x27, 0x62,
    // World3Areas
    0x24, 0x35, 0x20, 0x63,
    // World4Areas
    0x22, 0x29, 0x41, 0x2c, 0x61,
    // World5Areas
    0x2a, 0x31, 0x26, 0x62,
    // World6Areas
    0x2e, 0x23, 0x2d, 0x60,
    // World7Areas
    0x33, 0x29, 0x01, 0x27, 0x64,
    // World8Areas
    0x30, 0x32, 0x21, 0x65
)

//> BrickQBlockMetatiles:
//>   .db $c1, $c0, $5f, $60          ;used by question blocks
//>   .db $55, $56, $57, $58, $59     ;used by ground level types
//>   .db $5a, $5b, $5c, $5d, $5e     ;used by other level types
private val brickQBlockMetatiles = byteArrayOf(
    0xc1.toByte(), 0xc0.toByte(), 0x5f, 0x60,
    0x55, 0x56, 0x57, 0x58, 0x59,
    0x5a, 0x5b, 0x5c, 0x5d, 0x5e
)

//> EnemyBGCStateData:
//>   .db $01, $01, $02, $02, $02, $05
private val enemyBGCStateData2 = byteArrayOf(0x01, 0x01, 0x02, 0x02, 0x02, 0x05)

//> EnemyBGCXSpdData:
//>   .db $10, $f0
private val enemyBGCXSpdData2 = byteArrayOf(0x10, 0xf0.toByte())

//> BlockYPosAdderData:
//>   .db $04, $12
private val blockYPosAdderData2 = byteArrayOf(0x04, 0x12)

//> BlockBuffer_X_Adder: (duplicated from collisionDetection.kt for use in enemy block checks)
private val blockBufferXAdder2 = byteArrayOf(
    0x08, 0x03, 0x0c, 0x02, 0x02, 0x0d, 0x0d, 0x08,
    0x03, 0x0c, 0x02, 0x02, 0x0d, 0x0d, 0x08, 0x03,
    0x0c, 0x02, 0x02, 0x0d, 0x0d, 0x08, 0x00, 0x10,
    0x04, 0x14, 0x04, 0x04
)

//> BlockBuffer_Y_Adder: (duplicated from collisionDetection.kt for use in enemy block checks)
private val blockBufferYAdder2 = byteArrayOf(
    0x04, 0x20, 0x20, 0x08, 0x18, 0x08, 0x18, 0x02,
    0x20, 0x20, 0x08, 0x18, 0x08, 0x18, 0x12, 0x20,
    0x20, 0x18, 0x18, 0x18, 0x18, 0x18, 0x14, 0x14,
    0x06, 0x06, 0x08, 0x10
)

// ---------------------------------------------------------------------------
// Block buffer collision infrastructure (self-contained for enemy checks)
// These duplicate the private routines in collisionDetection.kt because
// those are file-private and cannot be accessed from here.
// ---------------------------------------------------------------------------

/**
 * Calculates a block buffer address from a 5-bit column index.
 * @param columnIndex 5-bit column (0-31)
 * @return Pair(blockBuffer array, base offset within that array)
 */
private fun System.getBlockBufferAddrEnemy(columnIndex: Int): Pair<ByteArray, Int> {
    //> GetBlockBufferAddr:
    val useBuf2 = (columnIndex and 0x10) != 0
    val col = columnIndex and 0x0F
    val buffer = if (useBuf2) ram.blockBuffer2 else ram.blockBuffer1
    val offset = col  // NES uses interleaved layout: buffer[col + row*0x10]
    return Pair(buffer, offset)
}

/**
 * Block buffer collision for enemy objects. Reads block buffer at the object's
 * position (with adder offsets) and returns the metatile found there.
 *
 * @param sprObjOffset SprObject offset (enemy offset + 1)
 * @param adderOffset Y offset into BlockBuffer_X/Y_Adder tables
 * @param returnHorizontal if true, return horizontal low nybble; if false, vertical
 * @return BlockBufferResult with metatile and coordinate data
 */
private fun System.blockBufferCollisionEnemy(sprObjOffset: Int, adderOffset: Int, returnHorizontal: Boolean): BlockBufferResult {
    //> BlockBufferCollision:
    val y = adderOffset

    val objX = ram.sprObjXPos[sprObjOffset].toInt() and 0xFF
    val xAdder = blockBufferXAdder2[y].toInt() and 0xFF
    val modifiedX = (objX + xAdder) and 0xFF
    val xCarry = if (objX + xAdder > 0xFF) 1 else 0

    val pageLoc = ram.sprObjPageLoc[sprObjOffset].toInt() and 0xFF
    val pageWithCarry = (pageLoc + xCarry) and 0x01

    val columnIndex = (pageWithCarry shl 4) or (modifiedX ushr 4)

    val (buffer, bufBase) = getBlockBufferAddrEnemy(columnIndex)

    val objY = ram.sprObjYPos[sprObjOffset].toInt() and 0xFF
    val yAdder = blockBufferYAdder2[y].toInt() and 0xFF
    val modifiedY = (objY + yAdder) and 0xFF

    val vertOffset = ((modifiedY and 0xF0) - 0x20) and 0xFF
    val bufIndex = bufBase + vertOffset
    val metatile = if (bufIndex in buffer.indices) buffer[bufIndex] else 0

    val lowNybble: Int = if (returnHorizontal) {
        objX and 0x0F
    } else {
        objY and 0x0F
    }

    return BlockBufferResult(
        metatile = metatile,
        lowNybble = lowNybble,
        vertOffset = vertOffset,
        blockBuffer = buffer,
        blockBufferBase = bufBase
    )
}

/**
 * Block buffer collision check for enemy objects.
 * @param flag 0 = return vertical coordinate, 1 = return horizontal coordinate
 * @param adderOffset Y offset into adder tables
 */
private fun System.blockBufferChkEnemyLocal(flag: Int, adderOffset: Int): BlockBufferResult {
    //> BlockBufferChk_Enemy:
    //> txa; clc; adc #$01; tax
    val sprObjOffset = (ram.objectOffset.toInt() and 0xFF) + 1
    //> BBChk_E: jsr BlockBufferCollision
    return blockBufferCollisionEnemy(sprObjOffset, adderOffset, returnHorizontal = flag != 0)
}

/**
 * Sets up floatey number display for an enemy.
 * Duplicated from collisionDetection.kt (private there).
 */
private fun System.setupFloateyNumberLocal(pointsControl: Int) {
    //> SetupFloateyNumber:
    val x = ram.objectOffset.toInt() and 0xFF
    //> sta FloateyNum_Control,x
    ram.floateyNumControl[x] = pointsControl.toByte()
    //> lda #$30; sta FloateyNum_Timer,x
    ram.floateyNumTimer[x] = 0x30
    //> lda Enemy_Y_Position,x; sta FloateyNum_Y_Pos,x
    ram.floateyNumYPos[x] = ram.sprObjYPos[x + 1].toUByte()
    //> lda Enemy_Rel_XPos; sta FloateyNum_X_Pos,x
    ram.floateyNumXPos[x] = ram.enemyRelXPos.toUByte()
}

// =====================================================================
// EnemyToBGCollisionDet
// =====================================================================

/**
 * Checks an enemy object against the level background (block buffers) and adjusts
 * position/behavior based on what metatile it's standing on or hitting.
 * Handles floor landing, side collision, special enemy types (hammer bro, paratroopa, spiny).
 */
fun System.enemyToBGCollisionDet() {
    //> EnemyToBGCollisionDet:
    val x = ram.objectOffset.toInt() and 0xFF

    //> lda Enemy_State,x        ;check enemy state for d6 set
    //> and #%00100000
    //> bne ExEBG                ;if set, branch to leave
    if ((ram.enemyState[x].toInt() and 0x20) != 0) return

    //> jsr SubtEnemyYPos        ;otherwise, do a subroutine here
    //> bcc ExEBG                ;if enemy vertical coord + 62 < 68, branch to leave
    if (!subtEnemyYPos(x)) return

    //> ldy Enemy_ID,x
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cpy #Spiny               ;if enemy object is not spiny, branch elsewhere
    //> bne DoIDCheckBGColl
    if (enemyId == Constants.Spiny.toInt() and 0xFF) {
        //> lda Enemy_Y_Position,x
        //> cmp #$25                 ;if enemy vertical coordinate < 36 branch to leave
        //> bcc ExEBG
        if ((ram.sprObjYPos[x + 1].toInt() and 0xFF) < 0x25) return
    }

    //> DoIDCheckBGColl:
    //> cpy #GreenParatroopaJump ;check for some other enemy object
    //> bne HBChk                ;branch if not found
    if (enemyId == Constants.GreenParatroopaJump.toInt() and 0xFF) {
        //> jmp EnemyJump            ;otherwise jump elsewhere
        enemyJump(x)
        return
    }

    //> HBChk: cpy #HammerBro    ;check for hammer bro
    //> bne CInvu                ;branch if not found
    if (enemyId == Constants.HammerBro.toInt() and 0xFF) {
        //> jmp HammerBroBGColl      ;otherwise jump elsewhere
        hammerBroBGColl(x)
        return
    }

    //> CInvu: cpy #Spiny        ;if enemy object is spiny, branch
    //> beq YesIn
    //> cpy #PowerUpObject       ;if special power-up object, branch
    //> beq YesIn
    //> cpy #$07                 ;if enemy object =>$07, branch to leave
    //> bcs ExEBGChk
    if (enemyId != Constants.Spiny.toInt() and 0xFF &&
        enemyId != Constants.PowerUpObject.toInt() and 0xFF &&
        enemyId >= 0x07) return

    //> YesIn: jsr ChkUnderEnemy ;if enemy object < $07, or = $12 or $2e, do this sub
    val underResult = chkUnderEnemyFull(x)
    if (debugEnemyTrace && x == 1) {
        println("[BGColl] slot=$x metatile=${(underResult.metatile.toInt() and 0xFF).toString(16)} lowNybble=${underResult.lowNybble}")
    }
    //> bne HandleEToBGCollision ;if block underneath enemy, branch
    if (underResult.metatile == 0.toByte()) {
        //> NoEToBGCollision:
        //> jmp ChkForRedKoopa   ;otherwise skip and do something else
        chkForRedKoopa(x)
        return
    }

    //> HandleEToBGCollision:
    handleEToBGCollision(x, underResult)
}

/**
 * SubtEnemyYPos: adds 62 to enemy Y position and compares against 68.
 * @return true if (enemy Y + 62) >= 68 (carry set)
 */
private fun System.subtEnemyYPos(x: Int): Boolean {
    //> SubtEnemyYPos:
    //> lda Enemy_Y_Position,x  ;add 62 pixels to enemy object's
    //> clc; adc #$3e
    //> cmp #$44                ;compare against a certain range
    val result = (ram.sprObjYPos[x + 1].toInt() and 0xFF) + 0x3e
    return (result and 0xFF) >= 0x44
}

/**
 * ChkUnderEnemy with full result: checks the block buffer below the enemy at center-bottom.
 */
// by Claude - Stores the last block buffer result for use in HandleEToBGCollision
private var lastEnemyBBResult: BlockBufferResult? = null

internal fun System.chkUnderEnemyFull(x: Int): BlockBufferResult {
    //> ChkUnderEnemy:
    //> lda #$00                  ;set flag in A for save vertical coordinate
    //> ldy #$15                  ;set Y to check the bottom middle (8,18) of enemy object
    //> jmp BlockBufferChk_Enemy
    val result = blockBufferChkEnemyLocal(flag = 0, adderOffset = 0x15)
    lastEnemyBBResult = result
    return result
}

/**
 * Handles what happens when an enemy is on top of a background tile.
 */
private fun System.handleEToBGCollision(x: Int, underResult: BlockBufferResult) {
    //> HandleEToBGCollision:
    //> jsr ChkForNonSolids       ;if something is underneath enemy, find out what
    //> beq NoEToBGCollision      ;if blank $26, coins, or hidden blocks, jump
    val mt = underResult.metatile.toInt() and 0xFF
    if (chkForNonSolidsLocal(mt)) {
        chkForRedKoopa(x)
        return
    }

    //> cmp #$23
    //> bne LandEnemyProperly     ;check for blank metatile $23 and branch if not found
    if (mt == 0x23) {
        //> ldy $02                   ;get vertical coordinate used to find block
        //> lda #$00                  ;store default blank metatile in that spot so we won't
        //> sta ($06),y               ;trigger this routine accidentally again
        val idx = underResult.blockBufferBase + underResult.vertOffset
        if (idx in underResult.blockBuffer.indices) {
            underResult.blockBuffer[idx] = 0
        }

        //> lda Enemy_ID,x
        val enemyId = ram.enemyID[x].toInt() and 0xFF
        //> cmp #$15                  ;if enemy object => $15, branch ahead
        //> bcs ChkToStunEnemies
        if (enemyId >= 0x15) {
            chkToStunEnemiesLocal(x)
            return
        }
        //> cmp #Goomba               ;if enemy object not goomba, branch ahead
        //> bne GiveOEPoints
        if (enemyId == Constants.Goomba.toInt() and 0xFF) {
            //> jsr KillEnemyAboveBlock
            killEnemyAboveBlock(x)
        }
        //> GiveOEPoints:
        //> lda #$01                  ;award 100 points for hitting block beneath enemy
        //> jsr SetupFloateyNumber
        val savedOfs = ram.objectOffset
        ram.objectOffset = x.toByte()
        setupFloateyNumberLocal(0x01)
        ram.objectOffset = savedOfs

        //> (fall through to ChkToStunEnemies)
        chkToStunEnemiesLocal(x)
        return
    }

    //> LandEnemyProperly:
    landEnemyProperly(x)
}

/**
 * Checks for non-solid metatiles (vine blank, coins, hidden blocks).
 */
internal fun chkForNonSolidsLocal(metatile: Int): Boolean {
    //> ChkForNonSolids:
    val mt = metatile and 0xFF
    return mt == 0x26 || mt == 0xc2 || mt == 0xc3 || mt == 0x5f || mt == 0x60
}

/**
 * KillEnemyAboveBlock: defeats the enemy as if hit from below by a block.
 */
private fun System.killEnemyAboveBlock(x: Int) {
    //> KillEnemyAboveBlock:
    //> jsr ShellOrBlockDefeat
    shellOrBlockDefeatLocal(x)
    //> lda #$fc; sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[x + 1] = 0xfc.toByte()
}

/**
 * Shell or block defeat for enemy BG collision context.
 */
private fun System.shellOrBlockDefeatLocal(x: Int) {
    //> ShellOrBlockDefeat:
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #PiranhaPlant; bne StnE
    if (enemyId == Constants.PiranhaPlant.toInt() and 0xFF) {
        val yPos = ram.sprObjYPos[x + 1].toInt() and 0xFF
        ram.sprObjYPos[x + 1] = (yPos + 0x18).toByte()
    }
    //> StnE: jsr ChkToStunEnemies
    chkToStunEnemiesLocal(x)
    //> lda Enemy_State,x; and #%00011111; ora #%00100000; sta Enemy_State,x
    val state = ram.enemyState[x].toInt() and 0x1F
    ram.enemyState[x] = (state or 0x20).toByte()

    //> lda #$02
    var points = 0x02
    if (enemyId == Constants.HammerBro.toInt() and 0xFF) {
        points = 0x06
    }
    if (enemyId == Constants.Goomba.toInt() and 0xFF) {
        points = 0x01
    }

    //> EnemySmackScore:
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumberLocal(points)
    ram.objectOffset = savedOfs
    ram.square1SoundQueue = Constants.Sfx_EnemySmack
}

/**
 * ChkToStunEnemies: demotes certain enemies, applies stunned state and knockback.
 */
private fun System.chkToStunEnemiesLocal(x: Int) {
    //> ChkToStunEnemies:
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cmp #$09; bcc SetStun
    //> cmp #$11; bcs SetStun
    //> cmp #$0a; bcc Demote
    //> cmp #PiranhaPlant; bcc SetStun
    if (enemyId in 0x09 until 0x11) {
        if (enemyId < 0x0a || enemyId >= (Constants.PiranhaPlant.toInt() and 0xFF)) {
            //> Demote: and #%00000001; sta Enemy_ID,x
            ram.enemyID[x] = (enemyId and 0x01).toByte()
        }
        // else: SetStun path (fall through to SetStun below)
    }

    //> SetStun: lda Enemy_State,x; and #%11110000; ora #%00000010; sta Enemy_State,x
    val state = ram.enemyState[x].toInt() and 0xF0
    ram.enemyState[x] = (state or 0x02).toByte()
    //> dec Enemy_Y_Position,x; dec Enemy_Y_Position,x
    ram.sprObjYPos[x + 1] = (ram.sprObjYPos[x + 1] - 2).toByte()

    //> lda Enemy_ID,x; cmp #Bloober; beq SetWYSpd
    val currentId = ram.enemyID[x].toInt() and 0xFF
    val ySpeed: Byte = if (currentId == Constants.Bloober.toInt() and 0xFF) {
        //> SetWYSpd: lda #$ff
        0xff.toByte()
    } else {
        //> lda #$fd; ldy AreaType; bne SetNotW
        if (ram.areaType != AreaType.Water) 0xfd.toByte() else 0xff.toByte()
    }
    //> SetNotW: sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[x + 1] = ySpeed

    //> ldy #$01; jsr PlayerEnemyDiff; bpl ChkBBill; iny
    var dir = 1
    val (_, highDiff) = playerEnemyDiff()
    if (highDiff.toByte() < 0) dir = 2

    //> ChkBBill:
    val stunId = ram.enemyID[x].toInt() and 0xFF
    if (stunId != Constants.BulletBill_CannonVar.toInt() and 0xFF &&
        stunId != Constants.BulletBill_FrenzyVar.toInt() and 0xFF) {
        //> sty Enemy_MovingDir,x
        ram.enemyMovingDirs[x] = dir.toByte()
    }
    //> NoCDirF: dey
    //> lda EnemyBGCXSpdData,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[x + 1] = enemyBGCXSpdData2[dir - 1]
}

// ---------------------------------------------------------------------------
// LandEnemyProperly and related
// ---------------------------------------------------------------------------

/**
 * Lands the enemy on the tile it hit, adjusting position and state.
 */
private fun System.landEnemyProperly(x: Int) {
    //> LandEnemyProperly:
    //> lda $04                 ;check lower nybble of vertical coordinate saved earlier
    //> sec; sbc #$08           ;subtract eight pixels
    //> cmp #$05                ;used to determine whether enemy landed from falling
    //> bcs ChkForRedKoopa
    val bbResult = lastEnemyBBResult
    val lowNybble = bbResult?.lowNybble ?: 0
    val adjusted = (lowNybble - 0x08) and 0xFF
    if (debugEnemyTrace && x == 1) {
        println("[LandEP] slot=$x lowNybble=$lowNybble adjusted=$adjusted (>=5? ${adjusted >= 5}) state=${ram.enemyState[x].toInt() and 0xFF}")
    }
    if (adjusted >= 0x05) {
        chkForRedKoopa(x)
        return
    }

    //> lda Enemy_State,x
    //> and #%01000000          ;branch if d6 in enemy state is set
    //> bne LandEnemyInitState
    val state = ram.enemyState[x].toInt() and 0xFF
    if ((state and 0x40) != 0) {
        landEnemyInitState(x)
        return
    }

    //> lda Enemy_State,x
    //> asl                     ;branch if d7 in enemy state is not set
    //> bcc ChkLandedEnemyState
    if ((state and 0x80) != 0) {
        //> SChkA: jmp DoEnemySideCheck
        doEnemySideCheck(x)
        return
    }

    //> ChkLandedEnemyState:
    //> lda Enemy_State,x       ;if enemy in normal state, branch back
    //> beq SChkA
    if (state == 0) {
        doEnemySideCheck(x)
        return
    }

    //> cmp #$05                ;if in state used by spiny's egg
    //> beq ProcEnemyDirection
    if (state == 0x05) {
        procEnemyDirection(x)
        return
    }

    //> cmp #$03; bcs ExSteChk
    if (state >= 0x03) return

    //> lda Enemy_State,x; cmp #$02; bne ProcEnemyDirection
    if (state != 0x02) {
        procEnemyDirection(x)
        return
    }

    //> lda #$10                ;load default timer here
    var timer = 0x10
    //> ldy Enemy_ID,x; cpy #Spiny; bne SetForStn
    if ((ram.enemyID[x].toInt() and 0xFF) == Constants.Spiny.toInt() and 0xFF) {
        timer = 0x00
    }
    //> SetForStn: sta EnemyIntervalTimer,x
    ram.timers[0x16 + x] = timer.toByte()
    //> lda #$03; sta Enemy_State,x
    ram.enemyState[x] = 0x03
    //> jsr EnemyLanding
    enemyLanding(x)
}

/**
 * ProcEnemyDirection: processes enemy direction after landing.
 */
private fun System.procEnemyDirection(x: Int) {
    //> ProcEnemyDirection:
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cmp #Goomba; beq LandEnemyInitState
    if (enemyId == Constants.Goomba.toInt() and 0xFF) {
        landEnemyInitState(x)
        return
    }

    //> cmp #Spiny; bne InvtD
    if (enemyId == Constants.Spiny.toInt() and 0xFF) {
        //> lda #$01; sta Enemy_MovingDir,x
        ram.enemyMovingDirs[x] = 0x01
        //> lda #$08; sta Enemy_X_Speed,x
        ram.sprObjXSpeed[x + 1] = 0x08
        //> lda FrameCounter; and #%00000111; beq LandEnemyInitState
        if ((ram.frameCounter.toInt() and 0x07) == 0) {
            landEnemyInitState(x)
            return
        }
    }

    //> InvtD: ldy #$01
    var dir = 1
    //> jsr PlayerEnemyDiff; bpl CNwCDir; iny
    val (lowDiff, highDiff) = playerEnemyDiff()
    if (highDiff.toByte() < 0) dir = 2

    //> CNwCDir: tya; cmp Enemy_MovingDir,x; bne LandEnemyInitState
    if (debugEnemyTrace && x == 1) {
        println("[ProcED] slot=$x dir=$dir highDiff=${highDiff.toString(16)} lowDiff=${lowDiff.toString(16)} movingDir=${ram.enemyMovingDirs[x].toInt() and 0xFF} match=${dir.toByte() == ram.enemyMovingDirs[x]}")
    }
    if (dir.toByte() != ram.enemyMovingDirs[x]) {
        landEnemyInitState(x)
        return
    }

    //> jsr ChkForBump_HammerBroJ
    chkForBumpHammerBroJ(x)
    // Assembly fall-through: after JSR returns, execution continues to LandEnemyInitState
    landEnemyInitState(x)
}

/**
 * LandEnemyInitState: lands the enemy and resets state.
 */
private fun System.landEnemyInitState(x: Int) {
    //> LandEnemyInitState:
    //> jsr EnemyLanding
    enemyLanding(x)
    //> lda Enemy_State,x; and #%10000000; bne NMovShellFallBit
    val state = ram.enemyState[x].toInt() and 0xFF
    if ((state and 0x80) != 0) {
        //> NMovShellFallBit:
        //> lda Enemy_State,x; and #%10111111; sta Enemy_State,x
        ram.enemyState[x] = (state and 0xBF).toByte()
    } else {
        //> lda #$00; sta Enemy_State,x
        ram.enemyState[x] = 0
    }
}

/**
 * EnemyLanding: sets enemy vertical speed/force to zero and snaps to tile boundary.
 */
internal fun System.enemyLanding(x: Int) {
    //> EnemyLanding:
    //> jsr InitVStf
    ram.sprObjYSpeed[x + 1] = 0
    ram.sprObjYMoveForce[x + 1] = 0
    //> lda Enemy_Y_Position,x; and #%11110000; ora #%00001000; sta Enemy_Y_Position,x
    val yPos = ram.sprObjYPos[x + 1].toInt() and 0xFF
    ram.sprObjYPos[x + 1] = ((yPos and 0xF0) or 0x08).toByte()
}

// ---------------------------------------------------------------------------
// ChkForRedKoopa and state transitions
// ---------------------------------------------------------------------------

/**
 * ChkForRedKoopa: handles red koopa edge detection and enemy state transitions.
 */
private fun System.chkForRedKoopa(x: Int) {
    //> ChkForRedKoopa:
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #RedKoopa; bne Chk2MSBSt
    if (enemyId == Constants.RedKoopa.toInt() and 0xFF) {
        //> lda Enemy_State,x; beq ChkForBump_HammerBroJ
        if (ram.enemyState[x] == 0.toByte()) {
            chkForBumpHammerBroJ(x)
            return
        }
    }

    //> Chk2MSBSt: lda Enemy_State,x; tay
    val state = ram.enemyState[x].toInt() and 0xFF
    //> asl; bcc GetSteFromD
    if ((state and 0x80) != 0) {
        //> lda Enemy_State,x; ora #%01000000; jmp SetD6Ste
        ram.enemyState[x] = (state or 0x40).toByte()
    } else {
        //> GetSteFromD: lda EnemyBGCStateData,y
        val newState = if (state in enemyBGCStateData2.indices) {
            enemyBGCStateData2[state].toInt() and 0xFF
        } else {
            state
        }
        //> SetD6Ste: sta Enemy_State,x
        ram.enemyState[x] = newState.toByte()
    }

    //> (fall through to DoEnemySideCheck)
    doEnemySideCheck(x)
}

// ---------------------------------------------------------------------------
// DoEnemySideCheck
// ---------------------------------------------------------------------------

/**
 * DoEnemySideCheck: checks for horizontal blockage on the enemy's sides.
 */
internal fun System.doEnemySideCheck(x: Int) {
    //> DoEnemySideCheck:
    //> lda Enemy_Y_Position,x; cmp #$20; bcc ExESdeC
    val yPos = ram.sprObjYPos[x + 1].toInt() and 0xFF
    if (yPos < 0x20) return

    //> ldy #$16; lda #$02; sta $eb
    var adderY = 0x16
    var checkDir = 2

    //> SdeCLoop:
    while (true) {
        //> lda $eb; cmp Enemy_MovingDir,x; bne NextSdeC
        if (checkDir.toByte() == ram.enemyMovingDirs[x]) {
            //> lda #$01; jsr BlockBufferChk_Enemy
            val result = blockBufferChkEnemyLocal(flag = 1, adderOffset = adderY)
            //> beq NextSdeC
            if (result.metatile != 0.toByte()) {
                //> jsr ChkForNonSolids; bne ChkForBump_HammerBroJ
                if (!chkForNonSolidsLocal(result.metatile.toInt() and 0xFF)) {
                    chkForBumpHammerBroJ(x)
                    return
                }
            }
        }
        //> NextSdeC: dec $eb; iny; cpy #$18; bcc SdeCLoop
        checkDir--
        adderY++
        if (adderY >= 0x18) break
    }
}

// ---------------------------------------------------------------------------
// ChkForBump_HammerBroJ
// ---------------------------------------------------------------------------

/**
 * Plays bump sound if applicable, then either makes hammer bro jump
 * or turns the enemy around.
 */
private fun System.chkForBumpHammerBroJ(x: Int) {
    //> ChkForBump_HammerBroJ:
    //> cpx #$05; beq NoBump
    if (x != 0x05) {
        //> lda Enemy_State,x; asl; bcc NoBump
        val state = ram.enemyState[x].toInt() and 0xFF
        if ((state and 0x80) != 0) {
            //> lda #Sfx_Bump; sta Square1SoundQueue
            ram.square1SoundQueue = Constants.Sfx_Bump
        }
    }

    //> NoBump: lda Enemy_ID,x; cmp #$05; bne InvEnemyDir
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    if (enemyId == Constants.HammerBro.toInt() and 0xFF) {
        //> lda #$00; sta $00; ldy #$fa; jmp SetHJ
        setHJ(x, 0x00, 0xfa.toByte())
        return
    }

    //> InvEnemyDir: jmp RXSpd
    // by Claude - fix: InvEnemyDir jumps directly to RXSpd, bypassing the
    // ID checks in EnemyTurnAround. All enemy types (including PowerUpObject)
    // get their speed reversed when hitting a wall from the side check.
    reverseEnemySpeed(x)
}

/**
 * Sets hammer bro to jumping state.
 */
private fun System.setHJ(x: Int, bitmask: Int, ySpeed: Byte) {
    //> SetHJ: sty Enemy_Y_Speed,x
    ram.sprObjYSpeed[x + 1] = ySpeed
    //> lda Enemy_State,x; ora #$01; sta Enemy_State,x
    val state = ram.enemyState[x].toInt() and 0xFF
    ram.enemyState[x] = (state or 0x01).toByte()
    //> lda $00; and PseudoRandomBitReg+2,x; tay
    var offset = bitmask and (ram.pseudoRandomBitReg[(2 + x).coerceIn(0, 7)].toInt() and 0xFF)
    //> lda SecondaryHardMode; bne HJump; tay
    if (ram.secondaryHardMode == 0.toByte()) {
        offset = 0
    }
    //> HJump: lda HammerBroJumpLData,y; sta EnemyFrameTimer,x
    val hammerBroJumpLData = byteArrayOf(0x20, 0x20, 0x20)
    val timerVal = if (offset in hammerBroJumpLData.indices) {
        hammerBroJumpLData[offset]
    } else {
        hammerBroJumpLData[0]
    }
    ram.timers[0x0a + x] = timerVal
    //> lda PseudoRandomBitReg+1,x; ora #%11000000; sta HammerBroJumpTimer,x
    val lsfr = ram.pseudoRandomBitReg[(1 + x).coerceIn(0, 7)].toInt() and 0xFF
    ram.hammerBroJumpTimers[x] = (lsfr or 0xC0).toByte() // by Claude - indexed with x
}

/**
 * Reverses an enemy's horizontal speed and flips its moving direction.
 * This is the RXSpd label in the assembly, called directly from InvEnemyDir
 * (which bypasses EnemyTurnAround's ID filtering).
 */
private fun System.reverseEnemySpeed(x: Int) {
    //> RXSpd: lda Enemy_X_Speed,x; eor #$ff; tay; iny; sty Enemy_X_Speed,x
    val speed = ram.sprObjXSpeed[x + 1]
    ram.sprObjXSpeed[x + 1] = (speed.toInt().inv() + 1).toByte()

    //> lda Enemy_MovingDir,x; eor #%00000011; sta Enemy_MovingDir,x
    val dir = ram.enemyMovingDirs[x].toInt() and 0xFF
    ram.enemyMovingDirs[x] = (dir xor 0x03).toByte()
}

/**
 * Turns an enemy around by negating horizontal speed and flipping direction,
 * but only for enemy types that are allowed to turn around.
 * EnemyTurnAround in the assembly has ID checks before falling through to RXSpd.
 */
private fun System.enemyTurnAroundLocal(x: Int) {
    //> EnemyTurnAround:
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #PiranhaPlant; beq ExTA
    if (enemyId == Constants.PiranhaPlant.toInt() and 0xFF) return
    //> cmp #Lakitu; beq ExTA
    if (enemyId == Constants.Lakitu.toInt() and 0xFF) return
    //> cmp #HammerBro; beq ExTA
    if (enemyId == Constants.HammerBro.toInt() and 0xFF) return

    //> cmp #Spiny; beq RXSpd
    //> cmp #GreenParatroopaJump; beq RXSpd
    //> cmp #$07; bcs ExTA
    if (enemyId != Constants.Spiny.toInt() and 0xFF &&
        enemyId != Constants.GreenParatroopaJump.toInt() and 0xFF &&
        enemyId >= 0x07) return

    reverseEnemySpeed(x)
}

// ---------------------------------------------------------------------------
// EnemyJump (green paratroopa)
// ---------------------------------------------------------------------------

/**
 * EnemyJump: handles green paratroopa jumping behavior.
 */
private fun System.enemyJump(x: Int) {
    //> EnemyJump:
    //> jsr SubtEnemyYPos; bcc DoSide
    if (!subtEnemyYPos(x)) {
        doEnemySideCheck(x)
        return
    }

    //> lda Enemy_Y_Speed,x; clc; adc #$02; cmp #$03; bcc DoSide
    val adjSpeed = ((ram.sprObjYSpeed[x + 1].toInt() and 0xFF) + 0x02) and 0xFF
    if (adjSpeed < 0x03) {
        doEnemySideCheck(x)
        return
    }

    //> jsr ChkUnderEnemy; beq DoSide
    val underResult = chkUnderEnemyFull(x)
    if (underResult.metatile == 0.toByte()) {
        doEnemySideCheck(x)
        return
    }

    //> jsr ChkForNonSolids; beq DoSide
    if (chkForNonSolidsLocal(underResult.metatile.toInt() and 0xFF)) {
        doEnemySideCheck(x)
        return
    }

    //> jsr EnemyLanding
    enemyLanding(x)
    //> lda #$fd; sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[x + 1] = 0xfd.toByte()
    //> DoSide: jmp DoEnemySideCheck
    doEnemySideCheck(x)
}

// ---------------------------------------------------------------------------
// HammerBroBGColl
// ---------------------------------------------------------------------------

/**
 * HammerBroBGColl: background collision for hammer bro.
 */
private fun System.hammerBroBGColl(x: Int) {
    //> HammerBroBGColl:
    //> jsr ChkUnderEnemy
    val underResult = chkUnderEnemyFull(x)
    //> beq NoUnderHammerBro
    if (underResult.metatile == 0.toByte()) {
        //> NoUnderHammerBro:
        //> lda Enemy_State,x; ora #$01; sta Enemy_State,x
        val state = ram.enemyState[x].toInt() and 0xFF
        ram.enemyState[x] = (state or 0x01).toByte()
        return
    }

    //> cmp #$23; bne UnderHammerBro
    val mt = underResult.metatile.toInt() and 0xFF
    if (mt == 0x23) {
        //> KillEnemyAboveBlock:
        killEnemyAboveBlock(x)
        return
    }

    //> UnderHammerBro:
    //> lda EnemyFrameTimer,x; bne NoUnderHammerBro
    val timer = ram.timers[0x0a + x].toInt() and 0xFF
    if (timer != 0) {
        val state = ram.enemyState[x].toInt() and 0xFF
        ram.enemyState[x] = (state or 0x01).toByte()
        return
    }

    //> lda Enemy_State,x; and #%10001000; sta Enemy_State,x
    val state = ram.enemyState[x].toInt() and 0x88
    ram.enemyState[x] = state.toByte()
    //> jsr EnemyLanding
    enemyLanding(x)
    //> jmp DoEnemySideCheck
    doEnemySideCheck(x)
}

// =====================================================================
// PlayerHeadCollision
// =====================================================================

/**
 * Handles what happens when the player's head hits a block from below:
 * brick breaking (if big), coin block activation, question block item dispensing, etc.
 *
 * @param result the block buffer collision result from the head check
 */
fun System.playerHeadCollision(result: BlockBufferResult) {
    //> PlayerHeadCollision:
    val origMetatile = result.metatile.toInt() and 0xFF

    //> pha                      ;store metatile number to stack
    //> lda #$11                 ;load unbreakable block object state by default
    //> ldx SprDataOffset_Ctrl
    val x = ram.sprDataOffsetCtrl.toInt() and 0xFF

    //> ldy PlayerSize; bne DBlockSte
    //> lda #$12                 ;otherwise load breakable block object state
    val blockState = if (ram.playerSize == 0.toByte()) 0x12 else 0x11
    //> DBlockSte: sta Block_State,x
    ram.blockStates[x] = blockState.toByte()

    // Reconstruct NES $06 value from BlockBufferResult
    val bbLow = if (result.blockBuffer === ram.blockBuffer2) (0xD0 + result.blockBufferBase) else result.blockBufferBase

    //> jsr DestroyBlockMetatile
    destroyBlockMetatile(bbLow, result.vertOffset)

    //> ldx SprDataOffset_Ctrl
    //> lda $02; sta Block_Orig_YPos,x
    ram.blockOrigYPos[x] = result.vertOffset.toByte()

    //> tay
    //> lda $06; sta Block_BBuf_Low,x
    ram.blockBBufLow[x] = bbLow.toByte()

    //> lda ($06),y              ;get contents of block buffer at old address
    val bufIdx = result.blockBufferBase + result.vertOffset
    val blockBufferContents = if (bufIdx in result.blockBuffer.indices) {
        result.blockBuffer[bufIdx].toInt() and 0xFF
    } else {
        0
    }

    //> jsr BlockBumpedChk
    val (bumpMatch, _) = blockBumpedChk(blockBufferContents)
    //> sta $00
    val storedMetatile = blockBufferContents

    //> ldy PlayerSize; bne ChkBrick; tya
    var metatileForBlock: Int = if (ram.playerSize == 0.toByte()) 0 else blockBufferContents

    //> ChkBrick: bcc PutMTileB
    if (bumpMatch) {
        //> ldy #$11; sty Block_State,x
        ram.blockStates[x] = 0x11
        //> lda #$c4
        metatileForBlock = 0xc4

        //> ldy $00; cpy #$58; beq StartBTmr; cpy #$5d; bne PutMTileB
        if (storedMetatile == 0x58 || storedMetatile == 0x5d) {
            //> StartBTmr: lda BrickCoinTimerFlag; bne ContBTmr
            if (ram.brickCoinTimerFlag == 0.toByte()) {
                //> lda #$0b; sta BrickCoinTimer; inc BrickCoinTimerFlag
                ram.brickCoinTimer = 0x0b
                ram.brickCoinTimerFlag = (ram.brickCoinTimerFlag + 1).toByte()
            }
            //> ContBTmr: lda BrickCoinTimer; bne PutOldMT
            if (ram.brickCoinTimer != 0.toByte()) {
                //> PutOldMT: tya
                metatileForBlock = storedMetatile
            } else {
                //> ldy #$c4
                metatileForBlock = 0xc4
            }
        }
    }

    //> PutMTileB: sta Block_Metatile,x
    ram.blockMetatile[x] = metatileForBlock.toByte()

    //> jsr InitBlock_XY_Pos
    initBlockXYPos(x)

    //> ldy $02; lda #$23; sta ($06),y
    if (bufIdx in result.blockBuffer.indices) {
        result.blockBuffer[bufIdx] = 0x23
    }

    //> lda #$10; sta BlockBounceTimer
    ram.blockBounceTimer = 0x10

    //> pla; sta $05
    val originalMT = origMetatile

    //> ldy #$00; lda CrouchingFlag; bne SmallBP
    //> lda PlayerSize; beq BigBP; SmallBP: iny
    val sizeOffset = if (ram.crouchingFlag != 0.toByte() || ram.playerSize != 0.toByte()) 1 else 0

    //> BigBP: lda Player_Y_Position; clc; adc BlockYPosAdderData,y
    //> and #$f0; sta Block_Y_Position,x
    val playerY = ram.playerYPosition.toInt() and 0xFF
    val adder = blockYPosAdderData2[sizeOffset].toInt() and 0xFF
    val blockY = (playerY + adder) and 0xF0
    ram.sprObjYPos[9 + x] = blockY.toByte()

    //> ldy Block_State,x; cpy #$11; beq Unbreak
    val currentBlockState = ram.blockStates[x].toInt() and 0xFF
    if (currentBlockState == 0x11) {
        //> Unbreak: jsr BumpBlock
        bumpBlock(x, originalMT)
    } else {
        //> jsr BrickShatter
        brickShatter(x)
    }

    //> InvOBit: lda SprDataOffset_Ctrl; eor #$01; sta SprDataOffset_Ctrl
    ram.sprDataOffsetCtrl = (ram.sprDataOffsetCtrl.toInt() xor 0x01).toByte()
}

// ---------------------------------------------------------------------------
// InitBlock_XY_Pos
// ---------------------------------------------------------------------------

private fun System.initBlockXYPos(x: Int) {
    //> InitBlock_XY_Pos:
    //> lda Player_X_Position; clc; adc #$08; and #$f0; sta Block_X_Position,x
    val playerX = ram.playerXPosition.toInt() and 0xFF
    val blockX = (playerX + 0x08) and 0xF0
    ram.sprObjXPos[9 + x] = blockX.toByte()

    //> lda Player_PageLoc; adc #$00; sta Block_PageLoc,x
    val carry = if ((playerX + 0x08) > 0xFF) 1 else 0
    val pageLoc = (ram.playerPageLoc.toInt() and 0xFF) + carry
    ram.sprObjPageLoc[9 + x] = pageLoc.toByte()
    //> sta Block_PageLoc2,x
    ram.blockPageLoc2[x] = pageLoc.toByte()
    //> lda Player_Y_HighPos; sta Block_Y_HighPos,x
    ram.sprObjYHighPos[9 + x] = ram.playerYHighPos
}

// ---------------------------------------------------------------------------
// BlockBumpedChk
// ---------------------------------------------------------------------------

private fun blockBumpedChk(metatile: Int): Pair<Boolean, Int> {
    //> BlockBumpedChk:
    //> ldy #$0d; BumpChkLoop: cmp BrickQBlockMetatiles,y
    //> beq MatchBump; dey; bpl BumpChkLoop; clc; MatchBump: rts
    val mt = (metatile and 0xFF).toByte()
    for (i in 0x0d downTo 0) {
        if (brickQBlockMetatiles[i] == mt) return Pair(true, i)
    }
    return Pair(false, -1)
}

// ---------------------------------------------------------------------------
// BumpBlock
// ---------------------------------------------------------------------------

private fun System.bumpBlock(x: Int, originalMT: Int) {
    //> BumpBlock:
    //> jsr CheckTopOfBlock
    checkTopOfBlock(x)
    //> lda #Sfx_Bump; sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_Bump
    //> lda #$00; sta Block_X_Speed,x; sta Block_Y_MoveForce,x; sta Player_Y_Speed
    ram.sprObjXSpeed[9 + x] = 0
    ram.sprObjYMoveForce[9 + x] = 0
    ram.playerYSpeed = 0
    //> lda #$fe; sta Block_Y_Speed,x
    ram.sprObjYSpeed[9 + x] = 0xfe.toByte()

    //> lda $05; jsr BlockBumpedChk; bcc ExitBlockChk
    val (found, bumpIdx) = blockBumpedChk(originalMT)
    if (!found) return

    //> tya; cmp #$09; bcc BlockCode; sbc #$05
    val blockCode = if (bumpIdx >= 0x09) bumpIdx - 0x05 else bumpIdx

    //> BlockCode: jsr JumpEngine
    when (blockCode) {
        0 -> mushFlowerBlock(x)
        1 -> coinBlock(x)
        2 -> coinBlock(x)
        3 -> extraLifeMushBlock(x)
        4 -> mushFlowerBlock(x)
        5 -> vineBlock(x)
        6 -> starBlock(x)
        7 -> coinBlock(x)
        8 -> extraLifeMushBlock(x)
    }
}

// ---------------------------------------------------------------------------
// Block item dispensing routines
// ---------------------------------------------------------------------------

private fun System.mushFlowerBlock(x: Int) {
    //> MushFlowerBlock: lda #$00
    ram.powerUpType = 0x00
    setupPowerUp(x)
}

private fun System.starBlock(x: Int) {
    //> StarBlock: lda #$02
    ram.powerUpType = 0x02
    setupPowerUp(x)
}

private fun System.extraLifeMushBlock(x: Int) {
    //> ExtraLifeMushBlock: lda #$03
    ram.powerUpType = 0x03
    setupPowerUp(x)
}

private fun System.vineBlock(x: Int) {
    //> VineBlock:
    //> ldx #$05                ;load last slot for enemy object buffer
    //> ldy SprDataOffset_Ctrl  ;get control bit
    //> jsr Setup_Vine          ;set up vine object
    setupVine(enemySlot = 5, blockSlot = x)
}

private fun System.coinBlock(x: Int) {
    //> CoinBlock:
    //> jsr FindEmptyMiscSlot
    val miscSlot = findEmptyMiscSlot()
    //> lda Block_PageLoc,x; sta Misc_PageLoc,y
    ram.sprObjPageLoc[miscSlot + 13] = ram.sprObjPageLoc[9 + x]
    //> lda Block_X_Position,x; ora #$05; sta Misc_X_Position,y
    ram.sprObjXPos[miscSlot + 13] = ((ram.sprObjXPos[9 + x].toInt() and 0xFF) or 0x05).toByte()
    //> lda Block_Y_Position,x; sbc #$10; sta Misc_Y_Position,y
    val blockY = ram.sprObjYPos[9 + x].toInt() and 0xFF
    ram.sprObjYPos[miscSlot + 13] = (blockY - 0x10).toByte()
    //> jmp JCoinC
    jCoinC(miscSlot, x)
}

private fun System.setupJumpCoin(x: Int, bbResult: BlockBufferResult) {
    //> SetupJumpCoin:
    val miscSlot = findEmptyMiscSlot()
    //> lda Block_PageLoc2,x; sta Misc_PageLoc,y
    ram.sprObjPageLoc[miscSlot + 13] = ram.blockPageLoc2[x]
    //> lda $06; asl; asl; asl; asl; ora #$05; sta Misc_X_Position,y
    val bbLow = bbResult.blockBufferBase and 0x0F
    ram.sprObjXPos[miscSlot + 13] = ((bbLow shl 4) or 0x05).toByte()
    //> lda $02; adc #$20; sta Misc_Y_Position,y
    ram.sprObjYPos[miscSlot + 13] = (bbResult.vertOffset + 0x20).toByte()
    //> JCoinC:
    jCoinC(miscSlot, x)
}

private fun System.jCoinC(miscSlot: Int, x: Int) {
    //> JCoinC: lda #$fb; sta Misc_Y_Speed,y
    ram.sprObjYSpeed[miscSlot + 13] = 0xfb.toByte()
    //> lda #$01; sta Misc_Y_HighPos,y; sta Misc_State,y; sta Square2SoundQueue
    ram.sprObjYHighPos[miscSlot + 13] = 0x01
    ram.miscStates[miscSlot] = 0x01
    ram.square2SoundQueue = Constants.Sfx_CoinGrab
    //> stx ObjectOffset
    ram.objectOffset = x.toByte()
    //> jsr GiveOneCoin
    giveOneCoin()
    //> inc CoinTallyFor1Ups
    ram.coinTallyFor1Ups = (ram.coinTallyFor1Ups + 1).toByte()
}

private fun System.findEmptyMiscSlot(): Int {
    //> FindEmptyMiscSlot:
    for (slot in 8 downTo 6) {
        if (ram.miscStates[slot] == 0.toByte()) {
            ram.jumpCoinMiscOffset = slot.toByte()
            return slot
        }
    }
    ram.jumpCoinMiscOffset = 8
    return 8
}

// setupPowerUp() moved to powerUpVine.kt

// ---------------------------------------------------------------------------
// BrickShatter
// ---------------------------------------------------------------------------

private fun System.brickShatter(x: Int) {
    //> BrickShatter:
    //> jsr CheckTopOfBlock
    checkTopOfBlock(x)
    //> lda #Sfx_BrickShatter; sta Block_RepFlag,x; sta NoiseSoundQueue
    ram.blockRepFlags[x] = Constants.Sfx_BrickShatter
    ram.noiseSoundQueue = Constants.Sfx_BrickShatter
    //> jsr SpawnBrickChunks
    spawnBrickChunks(x)
    //> lda #$fe; sta Player_Y_Speed
    ram.playerYSpeed = 0xfe.toByte()
    //> lda #$05; sta DigitModifier+5; jsr AddToScore
    ram.digitModifier[5] = 0x05
    addToScore()
}

// ---------------------------------------------------------------------------
// CheckTopOfBlock
// ---------------------------------------------------------------------------

private fun System.checkTopOfBlock(x: Int) {
    //> CheckTopOfBlock:
    //> ldx SprDataOffset_Ctrl; ldy $02; beq TopEx
    // Assembly reads $02 (zero-page temp) and $06 (buffer ptr), which correspond to
    // blockOrigYPos[x] and blockBBufLow[x] since they were just written in playerHeadCollision.
    val vertOffset = ram.blockOrigYPos[x].toInt() and 0xFF
    if (vertOffset == 0) return

    //> tya; sec; sbc #$10; sta $02; tay
    val newVertOffset = (vertOffset - 0x10) and 0xFF

    // Determine which block buffer from blockBBufLow
    val bbLow = ram.blockBBufLow[x].toInt() and 0xFF
    val useBuf2 = bbLow >= 0xd0
    val buffer = if (useBuf2) ram.blockBuffer2 else ram.blockBuffer1
    val col = bbLow and 0x0F
    val bufBase = col  // NES interleaved layout: buffer[col + vertOffset]

    //> lda ($06),y; cmp #$c2; bne TopEx
    val idx = bufBase + newVertOffset
    if (idx !in buffer.indices) return
    val aboveMT = buffer[idx].toInt() and 0xFF
    if (aboveMT != 0xc2) return

    //> lda #$00; sta ($06),y
    buffer[idx] = 0
    //> jsr RemoveCoin_Axe
    removeCoinOrAxe(bbLow, newVertOffset)
    //> ldx SprDataOffset_Ctrl; jsr SetupJumpCoin
    val fakeResult = BlockBufferResult(
        metatile = 0,
        lowNybble = 0,
        vertOffset = newVertOffset,
        blockBuffer = buffer,
        blockBufferBase = bufBase
    )
    setupJumpCoin(x, fakeResult)
}

// ---------------------------------------------------------------------------
// SpawnBrickChunks
// ---------------------------------------------------------------------------

private fun System.spawnBrickChunks(x: Int) {
    //> SpawnBrickChunks:
    //> lda Block_X_Position,x; sta Block_Orig_XPos,x
    ram.blockOrigXPos[x] = ram.sprObjXPos[9 + x]
    //> lda #$f0; sta Block_X_Speed,x; sta Block_X_Speed+2,x
    ram.sprObjXSpeed[9 + x] = 0xf0.toByte()
    ram.sprObjXSpeed[9 + x + 2] = 0xf0.toByte()
    //> lda #$fa; sta Block_Y_Speed,x
    ram.sprObjYSpeed[9 + x] = 0xfa.toByte()
    //> lda #$fc; sta Block_Y_Speed+2,x
    ram.sprObjYSpeed[9 + x + 2] = 0xfc.toByte()
    //> lda #$00; sta Block_Y_MoveForce,x; sta Block_Y_MoveForce+2,x
    ram.sprObjYMoveForce[9 + x] = 0
    ram.sprObjYMoveForce[9 + x + 2] = 0
    //> lda Block_PageLoc,x; sta Block_PageLoc+2,x
    ram.sprObjPageLoc[9 + x + 2] = ram.sprObjPageLoc[9 + x]
    //> lda Block_X_Position,x; sta Block_X_Position+2,x
    ram.sprObjXPos[9 + x + 2] = ram.sprObjXPos[9 + x]
    //> lda Block_Y_Position,x; clc; adc #$08; sta Block_Y_Position+2,x
    ram.sprObjYPos[9 + x + 2] = (ram.sprObjYPos[9 + x] + 8).toByte()
    //> lda #$fa; sta Block_Y_Speed,x  ;(redundant, already set above)
    ram.sprObjYSpeed[9 + x] = 0xfa.toByte()
}

// =====================================================================
// HandleWarpZone
// =====================================================================

/**
 * Handles warp zone pipe entry. Sets world number based on which pipe the player
 * entered, loads the appropriate area pointer, and initializes level parameters.
 *
 * @param warpOfs offset into the WarpZoneNumbers table (already adjusted for pipe position)
 */
fun System.handleWarpZone(warpOfs: Int) {
    //> GetWNum: ldy WarpZoneNumbers,x
    val warpNum = warpZoneNumbers[warpOfs.coerceIn(0, warpZoneNumbers.size - 1)].toInt() and 0xFF
    //> dey; sty WorldNumber
    val worldNum = (warpNum - 1) and 0xFF
    ram.worldNumber = worldNum.toByte()
    //> ldx WorldAddrOffsets,y
    val worldOfsIdx = worldNum.coerceIn(0, worldAddrOffsets.size - 1)
    val worldOffset = worldAddrOffsets[worldOfsIdx].toInt() and 0xFF
    //> lda AreaAddrOffsets,x; sta AreaPointer
    val areaOfsIdx = worldOffset.coerceIn(0, areaAddrOffsets.size - 1)
    ram.areaPointer = areaAddrOffsets[areaOfsIdx]
    //> lda #Silence; sta EventMusicQueue
    ram.eventMusicQueue = Constants.Silence
    //> lda #$00; sta EntrancePage; sta AreaNumber; sta LevelNumber; sta AltEntranceControl
    ram.entrancePage = 0
    ram.areaNumber = 0
    ram.levelNumber = 0
    ram.altEntranceControl = 0
    //> inc Hidden1UpFlag; inc FetchNewGameTimerFlag
    ram.hidden1UpFlag = true
    ram.fetchNewGameTimerFlag = true
}

// =====================================================================
// WarpZoneObject
// =====================================================================

/**
 * Handles the warp zone object: when scroll lock is active and the player is
 * on the correct screen position, enables the warp zone pipes and kills this object.
 */
fun System.warpZoneObject() {
    //> WarpZoneObject:
    val x = ram.objectOffset.toInt() and 0xFF

    //> lda ScrollLock; beq ExGTimer
    if (ram.scrollLock == 0.toByte()) return

    //> lda Player_Y_Position; and Player_Y_HighPos; bne ExGTimer
    val yPosCheck = (ram.playerYPosition.toInt() and 0xFF) and (ram.playerYHighPos.toInt() and 0xFF)
    if (yPosCheck != 0) return

    //> sta ScrollLock
    ram.scrollLock = 0
    //> inc WarpZoneControl
    ram.warpZoneControl = (ram.warpZoneControl + 1).toByte()
    //> jmp EraseEnemyObject
    eraseEnemyObject(x)
}

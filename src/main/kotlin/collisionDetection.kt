// by Claude - Collision detection routines
// Translates PlayerBGCollision, FireballBGCollision, PlayerEnemyCollision,
// FireballEnemyCollision, PlayerHammerCollision, and all supporting subroutines.
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or

// ---------------------------------------------------------------------------
// Data tables
// ---------------------------------------------------------------------------

//> BowserIdentities:
//>       .db Goomba, GreenKoopa, BuzzyBeetle, Spiny, Lakitu, Bloober, HammerBro, Bowser
private val bowserIdentities = byteArrayOf(
    EnemyId.Goomba.byte, EnemyId.GreenKoopa.byte, EnemyId.BuzzyBeetle.byte, EnemyId.Spiny.byte,
    EnemyId.Lakitu.byte, EnemyId.Bloober.byte, EnemyId.HammerBro.byte, EnemyId.Bowser.byte
)

//> ResidualXSpdData:
//>       .db $18, $e8
private val residualXSpdData = byteArrayOf(0x18, 0xe8.toByte())

//> KickedShellXSpdData:
//>       .db $30, $d0
private val kickedShellXSpdData = byteArrayOf(0x30, 0xd0.toByte())

//> DemotedKoopaXSpdData:
//>       .db $08, $f8
private val demotedKoopaXSpdData = byteArrayOf(0x08, 0xf8.toByte())

//> KickedShellPtsData:
//>       .db $0a, $06, $04
private val kickedShellPtsData = byteArrayOf(0x0a, 0x06, 0x04)

//> StompedEnemyPtsData:
//>       .db $02, $06, $05, $06
private val stompedEnemyPtsData = byteArrayOf(0x02, 0x06, 0x05, 0x06)

//> RevivalRateData:
//>       .db $10, $0b
private val revivalRateData = byteArrayOf(0x10, 0x0b)

//> SetBitsMask:
//>       .db %10000000, %01000000, %00100000, %00010000, %00001000, %00000100, %00000010
private val setBitsMask = byteArrayOf(
    0x80.toByte(), 0x40, 0x20, 0x10, 0x08, 0x04, 0x02
)

//> ClearBitsMask:
//>       .db %01111111, %10111111, %11011111, %11101111, %11110111, %11111011, %11111101
private val clearBitsMask = byteArrayOf(
    0x7f, 0xbf.toByte(), 0xdf.toByte(), 0xef.toByte(), 0xf7.toByte(), 0xfb.toByte(), 0xfd.toByte()
)

//> PlayerBGUpperExtent:
//>       .db $20, $10
private val playerBGUpperExtent = byteArrayOf(0x20, 0x10)

//> BlockBufferAdderData:
//>       .db $00, $07, $0e
private val blockBufferAdderData = byteArrayOf(0x00, 0x07, 0x0e)

//> SolidMTileUpperExt:
//>       .db $10, $61, $88, $c4
private val solidMTileUpperExt = byteArrayOf(0x10, 0x61, 0x88.toByte(), 0xc4.toByte())

//> ClimbMTileUpperExt:
//>       .db $24, $6d, $8a, $c6
private val climbMTileUpperExt = byteArrayOf(0x24, 0x6d, 0x8a.toByte(), 0xc6.toByte())

//> AreaChangeTimerData:
//>       .db $a0, $34
private val areaChangeTimerData = byteArrayOf(0xa0.toByte(), 0x34)

//> EnemyBGCStateData:
//>       .db $01, $01, $02, $02, $02, $05
private val enemyBGCStateData = byteArrayOf(0x01, 0x01, 0x02, 0x02, 0x02, 0x05)

//> EnemyBGCXSpdData:
//>       .db $10, $f0
private val enemyBGCXSpdData = byteArrayOf(0x10, 0xf0.toByte())

//> PlayerPosSPlatData:
//>       .db $80, $00
private val playerPosSPlatData = byteArrayOf(0x80.toByte(), 0x00)

//> FlagpoleYPosData:
//>       .db $18, $22, $50, $68, $90
private val flagpoleYPosData = byteArrayOf(0x18, 0x22, 0x50, 0x68, 0x90.toByte())

//> ClimbXPosAdder:
//>       .db $f9, $07
private val climbXPosAdder = byteArrayOf(0xf9.toByte(), 0x07)

//> ClimbPLocAdder:
//>       .db $ff, $00
private val climbPLocAdder = byteArrayOf(0xff.toByte(), 0x00)

//> BlockBuffer_X_Adder:
private val blockBufferXAdder = byteArrayOf(
    0x08, 0x03, 0x0c, 0x02, 0x02, 0x0d, 0x0d, 0x08,
    0x03, 0x0c, 0x02, 0x02, 0x0d, 0x0d, 0x08, 0x03,
    0x0c, 0x02, 0x02, 0x0d, 0x0d, 0x08, 0x00, 0x10,
    0x04, 0x14, 0x04, 0x04
)

//> BlockBuffer_Y_Adder:
private val blockBufferYAdder = byteArrayOf(
    0x04, 0x20, 0x20, 0x08, 0x18, 0x08, 0x18, 0x02,
    0x20, 0x20, 0x08, 0x18, 0x08, 0x18, 0x12, 0x20,
    0x20, 0x18, 0x18, 0x18, 0x18, 0x18, 0x14, 0x14,
    0x06, 0x06, 0x08, 0x10
)

//> BlockYPosAdderData:
//>       .db $04, $12
private val blockYPosAdderData = byteArrayOf(0x04, 0x12)

// =====================================================================
// SprObjectCollisionCore / PlayerCollisionCore
// =====================================================================

/**
 * Performs axis-aligned bounding box collision detection between two objects.
 * Checks horizontal overlap first, then vertical overlap.
 * Direct translation of the 6502 SprObjectCollisionCore routine.
 *
 * In the assembly, X indexes the second object's bounding box, Y indexes the first.
 * BoundingBox_UL_Corner is a flat array: [UL_X, UL_Y, DR_X, DR_Y] per object.
 * BoundingBox_LR_Corner = BoundingBox_UL_Corner + 2.
 *
 * @param bbOffsetX bounding box coordinate offset for second object (X register in asm)
 * @param bbOffsetY bounding box coordinate offset for first object (Y register in asm)
 * @return true if collision detected (carry set in assembly)
 */
fun System.sprObjectCollisionCore(bbOffsetX: Int, bbOffsetY: Int): Boolean {
    //> SprObjectCollisionCore:
    //> sty $06      ;save contents of Y here
    //> lda #$01; sta $07  ;counter: check horizontal first, then vertical
    var x = bbOffsetX
    var y = bbOffsetY

    // Loop twice: first horizontal (offset+0), then vertical (offset+1)
    for (counter in 1 downTo 0) {
        // All values are unsigned bytes
        val ulY = ram.boundBoxCoords[y].toInt() and 0xFF       // BoundingBox_UL_Corner,y
        val ulX = ram.boundBoxCoords[x].toInt() and 0xFF       // BoundingBox_UL_Corner,x
        val drX = ram.boundBoxCoords[x + 2].toInt() and 0xFF   // BoundingBox_LR_Corner,x
        val drY = ram.boundBoxCoords[y + 2].toInt() and 0xFF   // BoundingBox_LR_Corner,y

        //> CollisionCoreLoop:
        //> lda BoundingBox_UL_Corner,y  ; A = ulY
        //> cmp BoundingBox_UL_Corner,x  ; compare ulY vs ulX
        //> bcs FirstBoxGreater          ; if ulY >= ulX, branch
        if (ulY >= ulX) {
            //> FirstBoxGreater:
            //> cmp BoundingBox_UL_Corner,x  ; A still = ulY, compare again
            //> beq CollisionFound           ; if ulY == ulX, collision
            if (ulY == ulX) { x++; y++; continue }  // collision on this axis

            //> cmp BoundingBox_LR_Corner,x  ; compare ulY vs drX
            //> bcc CollisionFound           ; if ulY < drX, collision
            //> beq CollisionFound           ; if ulY == drX, collision
            if (ulY <= drX) { x++; y++; continue }  // collision on this axis

            //> cmp BoundingBox_LR_Corner,y  ; compare ulY vs drY (A still = ulY)
            //> bcc NoCollisionFound         ; if ulY < drY, no collision
            //> beq NoCollisionFound         ; if ulY == drY, no collision
            if (ulY <= drY) return false

            //> lda BoundingBox_LR_Corner,y  ; A = drY
            //> cmp BoundingBox_UL_Corner,x  ; compare drY vs ulX
            //> bcs CollisionFound           ; if drY >= ulX, vertical wrap collision
            if (drY >= ulX) { x++; y++; continue }

            //> NoCollisionFound: (falls through from above checks)
            return false
        } else {
            // ulY < ulX
            //> cmp BoundingBox_LR_Corner,x  ; compare ulY vs drX (A = ulY)
            //> bcc SecondBoxVerticalChk     ; if ulY < drX, branch
            //> beq CollisionFound           ; if ulY == drX, collision
            if (ulY == drX) { x++; y++; continue }  // collision
            if (ulY < drX) {
                //> SecondBoxVerticalChk:
                //> lda BoundingBox_LR_Corner,x  ; A = drX
                //> cmp BoundingBox_UL_Corner,x  ; compare drX vs ulX
                //> bcc CollisionFound           ; if drX < ulX, wrap collision
                if (drX < ulX) { x++; y++; continue }

                //> lda BoundingBox_LR_Corner,y  ; A = drY
                //> cmp BoundingBox_UL_Corner,x  ; compare drY vs ulX
                //> bcs CollisionFound           ; if drY >= ulX, collision
                if (drY >= ulX) { x++; y++; continue }

                return false
            }

            // ulY > drX (can happen if drX wrapped around)
            //> lda BoundingBox_LR_Corner,y  ; A = drY
            //> cmp BoundingBox_UL_Corner,y  ; compare drY vs ulY
            //> bcc CollisionFound           ; if drY < ulY, wrap collision
            if (drY < ulY) { x++; y++; continue }

            //> cmp BoundingBox_UL_Corner,x  ; compare drY vs ulX
            //> bcs CollisionFound           ; if drY >= ulX, collision
            if (drY >= ulX) { x++; y++; continue }

            return false
        }
    }
    // Both axes had collision
    return true
}

/**
 * Player collision core - sets X offset to 0 (player) then calls SprObjectCollisionCore.
 *
 * @param bbOffsetY bounding box coordinate offset for the other object
 * @return true if collision detected
 */
fun System.playerCollisionCore(bbOffsetY: Int): Boolean {
    //> PlayerCollisionCore:
    //> ldx #$00     ;initialize X to use player's bounding box for comparison
    return sprObjectCollisionCore(0, bbOffsetY)
}

// =====================================================================
// CheckPlayerVertical
// =====================================================================

/**
 * Checks if the player is vertically offscreen or below a threshold.
 * @return true (carry set) if player is offscreen or too far down (>= $d0)
 */
private fun System.checkPlayerVertical(): Boolean {
    //> CheckPlayerVertical:
    //> lda Player_OffscreenBits  ;if player object is completely offscreen
    //> cmp #$f0                  ;vertically, leave this routine
    //> bcs ExCPV
    val offBits = ram.playerOffscreenBits.toInt() and 0xFF
    if (offBits >= 0xf0) return true
    //> ldy Player_Y_HighPos      ;if player high vertical byte is not
    //> dey                       ;within the screen, leave this routine
    //> bne ExCPV
    val yHigh = ram.playerYHighPos.toInt() and 0xFF
    if (yHigh != 1) return true
    //> lda Player_Y_Position     ;if on the screen, check to see how far down
    //> cmp #$d0                  ;the player is vertically
    val yPos = ram.playerYPosition.toInt() and 0xFF
    //> ExCPV: rts
    return yPos >= 0xd0
}

// =====================================================================
// BlockBufferCollision and related routines
// =====================================================================

/**
 * Calculates a block buffer address from a 5-bit column index.
 * Assembly: GetBlockBufferAddr takes A with bits [d4:d0] representing
 * the column within the current block buffer page.
 *
 * @param columnIndex 5-bit column (0-31)
 * @return Pair(blockBuffer array reference, base offset within that array)
 */
private fun System.getBlockBufferAddr(columnIndex: Int): Pair<ByteArray, Int> {
    //> GetBlockBufferAddr:
    // The assembly computes a 16-bit pointer into $0500 or $05d0 block buffers.
    // columnIndex bit 4 selects which buffer; bits 3-0 select the column (x$0d stride).
    val useBuf2 = (columnIndex and 0x10) != 0
    val col = columnIndex and 0x0F
    val buffer = if (useBuf2) ram.blockBuffer2 else ram.blockBuffer1
    val offset = col  // NES uses interleaved layout: buffer[col + row*0x10]
    return Pair(buffer, offset)
}

/**
 * Core block buffer collision detection for any sprite object.
 * Reads block buffer at the object's position (with adder offsets) and returns
 * the metatile found there plus the low nybble of the relevant coordinate.
 *
 * @param sprObjOffset SprObject offset (0=player, enemy offset+1, fireball offset+7, etc.)
 * @param adderOffset Y offset into BlockBuffer_X/Y_Adder tables
 * @param returnHorizontal if true, return horizontal low nybble in $04; if false, vertical
 * @return the metatile found in the block buffer (0 = nothing)
 */
fun System.blockBufferCollision(sprObjOffset: Int, adderOffset: Int, returnHorizontal: Boolean): BlockBufferResult {
    //> BlockBufferCollision:
    var y = adderOffset

    //> lda BlockBuffer_X_Adder,y   ;add horizontal coordinate
    //> clc                         ;of object to value obtained using Y as offset
    //> adc SprObject_X_Position,x
    //> sta $05                     ;store here
    val objX = ram.sprObjXPos[sprObjOffset].toInt() and 0xFF
    val xAdder = blockBufferXAdder[y].toInt() and 0xFF
    val modifiedX = (objX + xAdder) and 0xFF
    val xCarry = if (objX + xAdder > 0xFF) 1 else 0

    //> lda SprObject_PageLoc,x
    //> adc #$00                    ;add carry to page location
    //> and #$01                    ;get LSB, mask out all other bits
    val pageLoc = ram.sprObjPageLoc[sprObjOffset].toInt() and 0xFF
    val pageWithCarry = (pageLoc + xCarry) and 0x01

    //> lsr                         ;move to carry
    //> ora $05                     ;get stored value
    //> ror                         ;rotate carry to MSB of A
    //> lsr                         ;and effectively move high nybble to
    //> lsr                         ;lower, LSB which became MSB will be
    //> lsr                         ;d4 at this point
    // This computes: (pageWithCarry << 4) | (modifiedX >> 4)
    val columnIndex = (pageWithCarry shl 4) or (modifiedX ushr 4)

    //> jsr GetBlockBufferAddr      ;get address of block buffer into $06, $07
    val (buffer, bufBase) = getBlockBufferAddr(columnIndex)

    //> lda SprObject_Y_Position,x  ;get vertical coordinate of object
    //> clc
    //> adc BlockBuffer_Y_Adder,y   ;add it to value obtained using Y as offset
    val objY = ram.sprObjYPos[sprObjOffset].toInt() and 0xFF
    val yAdder = blockBufferYAdder[y].toInt() and 0xFF
    val modifiedY = (objY + yAdder) and 0xFF

    //> and #%11110000              ;mask out low nybble
    //> sec
    //> sbc #$20                    ;subtract 32 pixels for the status bar
    //> sta $02                     ;store result here
    val vertOffset = ((modifiedY and 0xF0) - 0x20) and 0xFF
    // $02 in the assembly stores the full vertOffset (multiples of 0x10, range 0x00-0xC0).
    // NES uses ($06),Y where Y=vertOffset to index into buffer[col + vertOffset].

    //> tay                         ;use as offset for block buffer
    //> lda ($06),y                 ;check current content of block buffer
    //> sta $03                     ;and store here
    val bufIndex = bufBase + vertOffset
    val metatile = if (bufIndex in buffer.indices) buffer[bufIndex] else 0

    //> pla                         ;pull A from stack
    //> bne RetXC                   ;if A = 1, branch
    val lowNybble: Int
    if (returnHorizontal) {
        //> RetXC: lda SprObject_X_Position,x  ;load horizontal coordinate
        //> and #%00001111              ;and mask out high nybble
        lowNybble = objX and 0x0F
    } else {
        //> lda SprObject_Y_Position,x  ;if A = 0, load vertical coordinate
        //> and #%00001111              ;and mask out high nybble
        lowNybble = objY and 0x0F
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
 * Result from a block buffer collision check.
 */
data class BlockBufferResult(
    val metatile: Byte,          // $03 - the metatile found
    val lowNybble: Int,          // $04 - low nybble of x or y coordinate
    val vertOffset: Int,         // $02 - vertical offset into block buffer (multiples of 0x10)
    val blockBuffer: ByteArray,  // $06/$07 - block buffer reference
    val blockBufferBase: Int     // base offset into blockBuffer (column 0-15)
)

/**
 * Block buffer collision for player head (vertical coordinate returned).
 * Y is NOT incremented before calling (head check).
 */
private fun System.blockBufferColliHead(adderOffset: Int): BlockBufferResult {
    //> BlockBufferColli_Head:
    //> lda #$00       ;set flag to return vertical coordinate
    //> ldx #$00       ;set offset for player object
    return blockBufferCollision(sprObjOffset = 0, adderOffset = adderOffset, returnHorizontal = false)
}

/**
 * Block buffer collision for player feet (vertical coordinate returned).
 * Y is incremented by 1 before calling (feet = head adder + 1).
 */
private fun System.blockBufferColliFeet(adderOffset: Int): BlockBufferResult {
    //> BlockBufferColli_Feet:
    //> iny            ;if branched here, increment to next set of adders
    //> (falls through to BlockBufferColli_Head with flag=0)
    return blockBufferCollision(sprObjOffset = 0, adderOffset = adderOffset + 1, returnHorizontal = false)
}

/**
 * Block buffer collision for player side (horizontal coordinate returned).
 */
private fun System.blockBufferColliSide(adderOffset: Int): BlockBufferResult {
    //> BlockBufferColli_Side:
    //> lda #$01       ;set flag to return horizontal coordinate
    //> ldx #$00       ;set offset for player object
    return blockBufferCollision(sprObjOffset = 0, adderOffset = adderOffset, returnHorizontal = true)
}

/**
 * Block buffer collision check for enemy objects.
 * @param flag 0 = return vertical coordinate, 1 = return horizontal coordinate
 * @param adderOffset Y offset into adder tables
 */
fun System.blockBufferChkEnemy(flag: Int, adderOffset: Int): BlockBufferResult {
    //> BlockBufferChk_Enemy:
    //> txa; clc; adc #$01; tax  ;add 1 to X to run sub with enemy offset in mind
    val sprObjOffset = (ram.objectOffset.toInt() and 0xFF) + 1
    //> BBChk_E: jsr BlockBufferCollision
    val result = blockBufferCollision(sprObjOffset, adderOffset, returnHorizontal = flag != 0)
    return result
}

/**
 * Block buffer collision check for fireball objects.
 */
private fun System.blockBufferChkFBall(): BlockBufferResult {
    //> BlockBufferChk_FBall:
    //> ldy #$1a                  ;set offset for block buffer adder data
    val adderOffset = 0x1a
    //> txa; clc; adc #$07; tax   ;add seven bytes to use fireball's SprObject offset
    val sprObjOffset = (ram.objectOffset.toInt() and 0xFF) + 7
    //> lda #$00                  ;set A to return vertical coordinate
    val result = blockBufferCollision(sprObjOffset, adderOffset, returnHorizontal = false)
    return result
}

// =====================================================================
// Metatile checking helpers
// =====================================================================

/**
 * Gets the metatile attribute offset (2 MSB rotated to LSB positions).
 * @return Pair(originalMetatile, attributeOffset 0-3)
 */
private fun getMTileAttrib(metatile: Int): Pair<Int, Int> {
    //> GetMTileAttrib:
    //> tay            ;save metatile value into Y
    //> and #%11000000 ;mask out all but 2 MSB
    //> asl; rol; rol  ;shift and rotate d7-d6 to d1-d0
    //> tax            ;use as offset for metatile data
    //> tya            ;get original metatile value back
    val offset = (metatile and 0xC0) ushr 6
    return Pair(metatile, offset)
}

/**
 * Checks if the metatile is a solid block.
 * @return true if solid (carry set), the metatile value is compared against threshold
 */
private fun checkForSolidMTiles(metatile: Int): Boolean {
    //> CheckForSolidMTiles:
    //> jsr GetMTileAttrib
    //> cmp SolidMTileUpperExt,x
    val (mt, offset) = getMTileAttrib(metatile)
    val threshold = solidMTileUpperExt[offset].toInt() and 0xFF
    return mt >= threshold  // carry set if metatile >= threshold
}

/**
 * Checks if the metatile is climbable.
 * @return true if climbable (carry set)
 */
private fun checkForClimbMTiles(metatile: Int): Boolean {
    //> CheckForClimbMTiles:
    //> jsr GetMTileAttrib
    //> cmp ClimbMTileUpperExt,x
    val (mt, offset) = getMTileAttrib(metatile)
    val threshold = climbMTileUpperExt[offset].toInt() and 0xFF
    return mt >= threshold
}

/**
 * Checks if the metatile is a coin.
 * @return true if coin (carry set), also queues coin sound
 */
private fun System.checkForCoinMTiles(metatile: Int): Boolean {
    //> CheckForCoinMTiles:
    //> cmp #$c2              ;check for regular coin
    //> beq CoinSd            ;branch if found
    //> cmp #$c3              ;check for underwater coin
    //> beq CoinSd            ;branch if found
    //> clc; rts
    //> CoinSd: lda #Sfx_CoinGrab; sta Square2SoundQueue
    val mt = metatile and 0xFF
    if (mt == 0xc2 || mt == 0xc3) {
        ram.square2SoundQueue = Constants.Sfx_CoinGrab
        return true
    }
    return false
}

/**
 * Checks for hidden coin or 1-up blocks.
 * @return true (zero flag set in asm) if either found, meaning we should skip
 */
private fun chkInvisibleMTiles(metatile: Int): Boolean {
    //> ChkInvisibleMTiles:
    //> cmp #$5f       ;check for hidden coin block
    //> beq ExCInvT    ;branch to leave if found
    //> cmp #$60       ;check for hidden 1-up block
    //> ExCInvT: rts   ;leave with zero flag set if either found
    val mt = metatile and 0xFF
    return mt == 0x5f || mt == 0x60
}

/**
 * Checks for non-solid metatiles (vine blank, coins, hidden blocks).
 * @return true (zero flag set in asm) if non-solid found
 */
private fun chkForNonSolids(metatile: Int): Boolean {
    //> ChkForNonSolids:
    val mt = metatile and 0xFF
    //> beq NSFnd
    //> beq NSFnd
    //> beq NSFnd
    //> beq NSFnd
    //> NSFnd: rts
    return mt == 0x26 || mt == 0xc2 || mt == 0xc3 || mt == 0x5f || mt == 0x60
}

/**
 * Checks for jumpspring metatiles.
 * @return true if jumpspring found (carry set)
 */
private fun chkJumpspringMetatiles(metatile: Int): Boolean {
    //> ChkJumpspringMetatiles:
    //> cmp #$67      ;check for top jumpspring metatile
    //> beq JSFnd
    //> cmp #$68      ;check for bottom jumpspring metatile
    //> bne NoJSFnd   ;branch to use cleared carry if not found
    //> JSFnd:   sec           ;set carry if found
    //> NoJSFnd: rts           ;leave
    val mt = metatile and 0xFF
    return mt == 0x67 || mt == 0x68
}

// =====================================================================
// InitVStf - initialize vertical speed and movement force
// =====================================================================

/**
 * Initializes enemy vertical speed and Y movement force to zero.
 */
private fun System.initVStf(x: Int) {
    //> InitVStf: lda #$00
    //> sta Enemy_Y_Speed,x
    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYSpeed[x + 1] = 0
    ram.sprObjYMoveForce[x + 1] = 0
}

// =====================================================================
// SetupFloateyNumber
// =====================================================================

/**
 * Sets up floatey number display for an enemy.
 * @param pointsControl the points control value to display
 */
fun System.setupFloateyNumber(pointsControl: Int) {
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
// EnemyFacePlayer
// =====================================================================

/**
 * Makes the enemy face toward the player.
 * @return the direction offset (0 = right, 1 = left) for use as data table index
 */
private fun System.enemyFacePlayer(): Int {
    //> EnemyFacePlayer:
    val x = ram.objectOffset.toInt() and 0xFF
    //> ldy #$01               ;set to move right by default
    var dir = 1
    //> jsr PlayerEnemyDiff    ;get horizontal difference between player and enemy
    val (_, highDiff) = playerEnemyDiff()
    //> bpl SFcRt              ;if enemy is to the right of player, do not increment
    if (highDiff.toByte() < 0) {
        //> iny                    ;otherwise, increment to set to move to the left
        dir = 2
    }
    //> SFcRt: sty Enemy_MovingDir,x  ;set moving direction here
    ram.enemyMovingDirs[x] = dir.toByte()
    //> dey                    ;then decrement to use as a proper offset
    return dir - 1
}

// =====================================================================
// EnemyTurnAround
// =====================================================================

/**
 * Turns an enemy around by reversing horizontal speed and moving direction.
 * Only affects certain enemy types.
 */
fun System.enemyTurnAround(x: Int) {
    //> EnemyTurnAround:
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #PiranhaPlant; beq ExTA
    if (enemyId == EnemyId.PiranhaPlant.byte.toInt() and 0xFF) return
    //> cmp #Lakitu; beq ExTA
    if (enemyId == EnemyId.Lakitu.byte.toInt() and 0xFF) return
    //> cmp #HammerBro; beq ExTA
    if (enemyId == EnemyId.HammerBro.byte.toInt() and 0xFF) return
    //> cmp #Spiny; beq RXSpd
    //> cmp #GreenParatroopaJump; beq RXSpd
    //> cmp #$07; bcs ExTA
    if (enemyId != EnemyId.Spiny.byte.toInt() and 0xFF &&
        enemyId != EnemyId.GreenParatroopaJump.byte.toInt() and 0xFF &&
        enemyId >= 0x07) return

    //> RXSpd: lda Enemy_X_Speed,x
    //> eor #$ff; tay; iny
    //> sty Enemy_X_Speed,x
    val speed = ram.sprObjXSpeed[x + 1]
    ram.sprObjXSpeed[x + 1] = (speed.toInt().inv() + 1).toByte()  // negate (two's complement)

    //> lda Enemy_MovingDir,x
    //> eor #%00000011
    //> sta Enemy_MovingDir,x
    val dir = ram.enemyMovingDirs[x].toInt() and 0xFF
    ram.enemyMovingDirs[x] = (dir xor 0x03).toByte()
}

// =====================================================================
// FireballBGCollision
// =====================================================================

/**
 * Fireball vs background collision detection.
 * Handles fireball bouncing off terrain or exploding.
 *
 * NOTE: This function replaces the stub in procFireballBubble.kt.
 * The stub must be removed for compilation.
 */
fun System.fireballBGCollision() {
    //> FireballBGCollision:
    val x = ram.objectOffset.toInt() and 0xFF

    //> lda Fireball_Y_Position,x   ;check fireball's vertical coordinate
    //> cmp #$18
    //> bcc ClearBounceFlag         ;if within the status bar area, branch ahead
    val fbYPos = ram.sprObjYPos[x + 7].toInt() and 0xFF
    if (fbYPos < 0x18) {
        //> ClearBounceFlag:
        ram.fireballBouncingFlags[x] = 0
        return
    }

    //> jsr BlockBufferChk_FBall    ;do fireball to background collision detection on bottom of it
    val result = blockBufferChkFBall()
    //> beq ClearBounceFlag         ;if nothing underneath fireball, branch
    if (result.metatile == 0.toByte()) {
        ram.fireballBouncingFlags[x] = 0
        return
    }

    //> jsr ChkForNonSolids         ;check for non-solid metatiles
    //> beq ClearBounceFlag         ;branch if any found
    if (chkForNonSolids(result.metatile.toInt() and 0xFF)) {
        ram.fireballBouncingFlags[x] = 0
        return
    }

    //> lda Fireball_Y_Speed,x      ;if fireball's vertical speed set to move upwards,
    //> bmi InitFireballExplode     ;branch to set exploding bit in fireball's state
    val fbYSpeed = ram.sprObjYSpeed[x + 7]
    if (fbYSpeed < 0) {
        initFireballExplode(x)
        return
    }

    //> lda FireballBouncingFlag,x  ;if bouncing flag already set,
    //> bne InitFireballExplode     ;branch to set exploding bit
    if (ram.fireballBouncingFlags[x] != 0.toByte()) {
        initFireballExplode(x)
        return
    }

    //> lda #$fd; sta Fireball_Y_Speed,x  ;set vertical speed to bounce upward
    ram.sprObjYSpeed[x + 7] = 0xfd.toByte()
    //> lda #$01; sta FireballBouncingFlag,x  ;set bouncing flag
    ram.fireballBouncingFlags[x] = 1
    //> lda Fireball_Y_Position,x
    //> and #$f8                    ;modify vertical coordinate to land properly
    //> sta Fireball_Y_Position,x
    ram.sprObjYPos[x + 7] = (ram.sprObjYPos[x + 7].toInt() and 0xf8).toByte()
}

private fun System.initFireballExplode(x: Int) {
    //> ;$00 - used to hold one of bitmasks, or offset
    //> ;$01 - used for relative X coordinate, also used to store middle screen page location
    //> ;$02 - used for relative Y coordinate, also used to store middle screen coordinate
    //> InitFireballExplode:
    //> lda #$80; sta Fireball_State,x  ;set exploding flag
    ram.fireballStates[x] = 0x80.toByte()
    //> lda #Sfx_Bump; sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_Bump
}

// =====================================================================
// FireballEnemyCollision
// =====================================================================

/**
 * Fireball vs enemy collision detection.
 * Checks each enemy slot against the current fireball's bounding box.
 *
 * NOTE: This function replaces the stub in procFireballBubble.kt.
 * The stub must be removed for compilation.
 */
fun System.fireballEnemyCollision() {
    //> FireballEnemyCollision:
    val fbObjOfs = ram.objectOffset.toInt() and 0xFF

    //> lda Fireball_State,x  ;check to see if fireball state is set at all
    //> beq ExitFBallEnemy    ;branch to leave if not
    val fbState = ram.fireballStates[fbObjOfs]
    if (fbState == 0.toByte()) return

    //> asl; bcs ExitFBallEnemy  ;branch to leave if d7 in state is set
    if ((fbState.toInt() and 0x80) != 0) return

    //> lda FrameCounter; lsr; bcs ExitFBallEnemy  ;every other frame
    if ((ram.frameCounter.toInt() and 0x01) != 0) return

    //> txa; asl; asl; clc; adc #$1c; tay  ;fireball bounding box offset
    val fbBBOffset = (fbObjOfs * 4) + 0x1c

    //> ldx #$04
    // Loop through enemy slots 4 down to 0
    for (enemyIdx in 4 downTo 0) {
        //> FireballEnemyCDLoop:
        //> stx $01                     ;store enemy object offset
        val savedFbBBOffset = fbBBOffset  // push fireball offset to stack equivalent

        //> lda Enemy_State,x; and #%00100000; bne NoFToECol
        if ((ram.enemyState[enemyIdx].toInt() and 0x20) != 0) continue

        //> lda Enemy_Flag,x; beq NoFToECol
        if (ram.enemyFlags[enemyIdx] == 0.toByte()) continue

        //> lda Enemy_ID,x
        val enemyId = ram.enemyID[enemyIdx].toInt() and 0xFF

        //> cmp #$24; bcc GoombaDie
        //> cmp #$2b; bcc NoFToECol
        if (enemyId >= 0x24 && enemyId < 0x2b) continue

        //> GoombaDie: cmp #Goomba; bne NotGoomba
        if (enemyId == EnemyId.Goomba.byte.toInt() and 0xFF) {
            //> lda Enemy_State,x; cmp #$02; bcs NoFToECol
            if ((ram.enemyState[enemyIdx].toInt() and 0xFF) >= 0x02) continue
        }

        //> NotGoomba: lda EnemyOffscrBitsMasked,x ;if any masked offscreen bits set,
        if (ram.enemyOffscrBitsMaskeds[enemyIdx] != 0.toByte()) continue

        //> txa; asl; asl; clc; adc #$04; tax  ;enemy bounding box offset
        val enemyBBOffset = (enemyIdx * 4) + 4

        //> jsr SprObjectCollisionCore
        val collision = sprObjectCollisionCore(enemyBBOffset, savedFbBBOffset)

        //> ldx ObjectOffset
        //> bcc NoFToECol
        if (!collision) continue

        //> lda #%10000000; sta Fireball_State,x  ;set d7 in fireball state
        ram.fireballStates[fbObjOfs] = 0x80.toByte()

        //> ldx $01  ;get enemy offset
        //> jsr HandleEnemyFBallCol
        handleEnemyFBallCol(enemyIdx)
        //> NoFToECol: pla                         ;pull fireball offset from stack
        //> bpl FireballEnemyCDLoop     ;loop back until collision detection done on all enemies
    }
    //> ExitFBallEnemy: ldx ObjectOffset; rts
}

// =====================================================================
// HandleEnemyFBallCol
// =====================================================================

private fun System.handleEnemyFBallCol(enemyIdx: Int) {
    //> HandleEnemyFBallCol:
    //> jsr RelativeEnemyPosition
    relativeEnemyPosition()

    //> ldx $01  ;get current enemy object offset
    val x = enemyIdx

    //> lda Enemy_Flag,x; bpl ChkBuzzyBeetle  ;check buffer flag for d7 set
    val flag = ram.enemyFlags[x].toInt() and 0xFF
    if ((flag and 0x80) != 0) {
        //> and #%00001111; tax
        val innerIdx = flag and 0x0F
        //> lda Enemy_ID,x; cmp #Bowser; beq HurtBowser
        if ((ram.enemyID[innerIdx].toInt() and 0xFF) == (EnemyId.Bowser.byte.toInt() and 0xFF)) {
            hurtBowser(innerIdx, enemyIdx)
            return
        }
        // ldx $01 - fall through to ChkBuzzyBeetle with original enemy offset
    }

    //> ChkBuzzyBeetle:
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #BuzzyBeetle; beq ExHCF
    //> ExHCF: rts                      ;and now let's leave
    if (enemyId == EnemyId.BuzzyBeetle.byte.toInt() and 0xFF) return
    //> cmp #Bowser; bne ChkOtherEnemies
    if (enemyId == EnemyId.Bowser.byte.toInt() and 0xFF) {
        hurtBowser(x, enemyIdx)
        return
    }

    //> ChkOtherEnemies:
    //> cmp #BulletBill_FrenzyVar; beq ExHCF
    if (enemyId == EnemyId.BulletBillFrenzyVar.byte.toInt() and 0xFF) return
    //> cmp #Podoboo; beq ExHCF
    if (enemyId == EnemyId.Podoboo.byte.toInt() and 0xFF) return
    //> cmp #$15; bcs ExHCF
    if (enemyId >= 0x15) return

    //> ShellOrBlockDefeat:
    shellOrBlockDefeat(x)
}

private fun System.hurtBowser(bowserIdx: Int, enemyIdx: Int) {
    //> HurtBowser:
    //> dec BowserHitPoints
    ram.bowserHitPoints = (ram.bowserHitPoints - 1).toByte()
    //> bne ExHCF
    if (ram.bowserHitPoints != 0.toByte()) return

    //> jsr InitVStf
    initVStf(bowserIdx)
    //> sta Enemy_X_Speed,x
    ram.sprObjXSpeed[bowserIdx + 1] = 0
    //> sta EnemyFrenzyBuffer
    ram.enemyFrenzyBuffer = 0
    //> lda #$fe; sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[bowserIdx + 1] = 0xfe.toByte()
    //> ldy WorldNumber
    val worldNum = ram.worldNumber.toInt() and 0xFF
    //> lda BowserIdentities,y; sta Enemy_ID,x
    ram.enemyID[bowserIdx] = bowserIdentities[worldNum.coerceIn(0, 7)]
    //> lda #$20
    var state = 0x20
    //> cpy #$03; bcs SetDBSte
    if (worldNum < 0x03) {
        //> ora #$03
        state = state or 0x03
    }
    //> SetDBSte: sta Enemy_State,x
    ram.enemyState[bowserIdx] = state.toByte()
    //> lda #Sfx_BowserFall; sta Square2SoundQueue
    ram.square2SoundQueue = Constants.Sfx_BowserFall
    //> ldx $01  ;get enemy offset
    //> lda #$09  ;award 5000 points
    ram.objectOffset = enemyIdx.toByte()
    setupFloateyNumber(0x09)
    //> EnemySmackScore: (falls through)
    ram.square1SoundQueue = Constants.Sfx_EnemySmack
}

/**
 * Defeat an enemy as if hit by shell or from beneath a block.
 */
fun System.shellOrBlockDefeat(x: Int) {
    //> ShellOrBlockDefeat:
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #PiranhaPlant; bne StnE
    if (enemyId == EnemyId.PiranhaPlant.byte.toInt() and 0xFF) {
        //> lda Enemy_Y_Position,x; adc #$18; sta Enemy_Y_Position,x
        // carry=1 from cmp #PiranhaPlant (equal), so adc adds $18 + 1 = $19
        val yPos = ram.sprObjYPos[x + 1].toInt() and 0xFF
        ram.sprObjYPos[x + 1] = (yPos + 0x19).toByte()
    }
    //> StnE: jsr ChkToStunEnemies
    chkToStunEnemies(x)
    //> lda Enemy_State,x; and #%00011111; ora #%00100000; sta Enemy_State,x
    val state = ram.enemyState[x].toInt() and 0x1F
    ram.enemyState[x] = (state or 0x20).toByte()

    //> lda #$02  ;award 200 points by default
    var points = 0x02
    //> ldy Enemy_ID,x; cpy #HammerBro; bne GoombaPoints
    if (enemyId == EnemyId.HammerBro.byte.toInt() and 0xFF) {
        //> lda #$06  ;award 1000 points for hammer bro
        points = 0x06
    }
    //> GoombaPoints: cpy #Goomba; bne EnemySmackScore
    if (enemyId == EnemyId.Goomba.byte.toInt() and 0xFF) {
        //> lda #$01  ;award 100 points for goomba
        points = 0x01
    }

    //> EnemySmackScore:
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumber(points)
    ram.objectOffset = savedOfs
    //> lda #Sfx_EnemySmack; sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_EnemySmack
}

/**
 * Stun enemies: demote koopa variants, set stunned state, apply knockback.
 */
private fun System.chkToStunEnemies(x: Int) {
    //> ChkToStunEnemies:
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cmp #$09; bcc SetStun
    //> cmp #$11; bcs SetStun
    //> cmp #$0a; bcc Demote
    //> cmp #PiranhaPlant; bcc SetStun
    if (enemyId >= 0x09 && enemyId < 0x11) {
        if (enemyId < 0x0a || enemyId >= (EnemyId.PiranhaPlant.byte.toInt() and 0xFF)) {
            // SetStun path
        } else {
            //> Demote: and #%00000001; sta Enemy_ID,x
            ram.enemyID[x] = (enemyId and 0x01).toByte()
        }
    }

    //> SetStun: lda Enemy_State,x; and #%11110000; ora #%00000010; sta Enemy_State,x
    val state = ram.enemyState[x].toInt() and 0xF0
    ram.enemyState[x] = (state or 0x02).toByte()
    //> dec Enemy_Y_Position,x; dec Enemy_Y_Position,x
    ram.sprObjYPos[x + 1] = (ram.sprObjYPos[x + 1] - 2).toByte()

    //> lda Enemy_ID,x; cmp #Bloober; beq SetWYSpd
    val currentId = ram.enemyID[x].toInt() and 0xFF
    val ySpeed: Byte
    if (currentId == EnemyId.Bloober.byte.toInt() and 0xFF) {
        //> SetWYSpd: lda #$ff
        ySpeed = 0xff.toByte()
    } else {
        //> lda #$fd; ldy AreaType; bne SetNotW
        ySpeed = if (ram.areaType != AreaType.Water) 0xfd.toByte() else 0xff.toByte()
    }
    //> SetNotW: sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[x + 1] = ySpeed

    //> ldy #$01; jsr PlayerEnemyDiff; bpl ChkBBill; iny
    var dir = 1
    val (_, highDiff) = playerEnemyDiff()
    if (highDiff.toByte() < 0) dir = 2

    //> ChkBBill:
    val stunId = ram.enemyID[x].toInt() and 0xFF
    if (stunId != EnemyId.BulletBillCannonVar.byte.toInt() and 0xFF &&
        stunId != EnemyId.BulletBillFrenzyVar.byte.toInt() and 0xFF) {
        //> sty Enemy_MovingDir,x
        ram.enemyMovingDirs[x] = dir.toByte()
    }
    //> NoCDirF: dey
    val spdIdx = dir - 1
    //> lda EnemyBGCXSpdData,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[x + 1] = enemyBGCXSpdData[spdIdx]
}

// =====================================================================
// PlayerHammerCollision
// =====================================================================

/**
 * Player vs hammer collision detection.
 * Checks if a hammer (misc object) has collided with the player.
 *
 * NOTE: This function replaces the stub in hammerObj.kt.
 * The stub must be removed for compilation.
 */
fun System.playerHammerCollision() {
    //> PlayerHammerCollision:
    val x = ram.objectOffset.toInt() and 0xFF

    //> ExPHC:  rts
    //> lda FrameCounter; lsr; bcc ExPHC  ;execute every other frame
    if ((ram.frameCounter.toInt() and 0x01) == 0) return

    //> lda TimerControl; ora Misc_OffscreenBits; bne ExPHC
    if (ram.timerControl != 0.toByte() || ram.miscOffscreenBits != 0.toByte()) return

    //> txa; asl; asl; clc; adc #$24; tay  ;misc object bounding box offset
    val miscBBOffset = (x * 4) + 0x24

    //> jsr PlayerCollisionCore
    val collision = playerCollisionCore(miscBBOffset)

    //> ldx ObjectOffset
    //> bcc ClHCol  ;if no collision, branch
    if (!collision) {
        //> ClHCol: lda #$00; sta Misc_Collision_Flag,x
        ram.miscCollisionFlags[x] = 0
        return
    }

    //> lda Misc_Collision_Flag,x; bne ExPHC  ;if already set, leave
    if (ram.miscCollisionFlags[x] != 0.toByte()) return

    //> lda #$01; sta Misc_Collision_Flag,x
    ram.miscCollisionFlags[x] = 1

    //> lda Misc_X_Speed,x; eor #$ff; clc; adc #$01; sta Misc_X_Speed,x
    val miscSprOfs = x + 13  // misc objects start at SprObject offset 13
    val speed = ram.sprObjXSpeed[miscSprOfs]
    ram.sprObjXSpeed[miscSprOfs] = (speed.toInt().inv() + 1).toByte()

    //> lda StarInvincibleTimer; bne ExPHC
    if (ram.starInvincibleTimer != 0.toByte()) return

    //> jmp InjurePlayer
    injurePlayer()
}

// =====================================================================
// PlayerEnemyCollision
// =====================================================================

/**
 * Player vs enemy collision handling. Determines if the player stomps an enemy,
 * gets hurt, or interacts with powerups.
 *
 * NOTE: This function replaces the stub in processCannons.kt.
 * The stub must be removed for compilation.
 */
fun System.playerEnemyCollision() {
    //> PlayerEnemyCollision:
    val x = ram.objectOffset.toInt() and 0xFF

    //> lda FrameCounter; lsr; bcs NoPUp
    if ((ram.frameCounter.toInt() and 0x01) != 0) return

    //> jsr CheckPlayerVertical; bcs NoPECol
    if (checkPlayerVertical()) return

    //> lda EnemyOffscrBitsMasked,x; bne NoPECol
    if (ram.enemyOffscrBitsMaskeds[x] != 0.toByte()) return

    //> lda GameEngineSubroutine; cmp #$08; bne NoPECol
    if (ram.gameEngineSubroutine != GameEngineRoutine.PlayerCtrlRoutine) return

    //> lda Enemy_State,x; and #%00100000; bne NoPECol
    if ((ram.enemyState[x].toInt() and 0x20) != 0) return

    //> jsr GetEnemyBoundBoxOfs
    val (bbOffset, _) = getEnemyBoundBoxOfs()

    //> jsr PlayerCollisionCore
    val collision = playerCollisionCore(bbOffset)

    //> ldx ObjectOffset
    //> bcs CheckForPUpCollision
    if (!collision) {
        //> lda Enemy_CollisionBits,x; and #%11111110; sta Enemy_CollisionBits,x
        ram.enemyCollisionBitsArr[x] = (ram.enemyCollisionBitsArr[x].toInt() and 0xFE).toByte()
        //> NoPECol: rts
        return
    }

    //> CheckForPUpCollision:
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> ldy Enemy_ID,x; cpy #PowerUpObject; bne EColl
    if (enemyId == EnemyId.PowerUpObject.byte.toInt() and 0xFF) {
        //> jmp HandlePowerUpCollision
        handlePowerUpCollision(x)
        return
    }

    //> EColl: lda StarInvincibleTimer; beq HandlePECollisions
    if (ram.starInvincibleTimer != 0.toByte()) {
        //> jmp ShellOrBlockDefeat
        shellOrBlockDefeat(x)
        return
    }

    //> HandlePECollisions:
    handlePECollisions(x, enemyId)
}

private fun System.handlePECollisions(x: Int, enemyId: Int) {
    //> HandlePECollisions:
    //> lda Enemy_CollisionBits,x; and #%00000001
    //> ora EnemyOffscrBitsMasked,x; bne ExPEC
    val colBit = ram.enemyCollisionBitsArr[x].toInt() and 0x01
    if (colBit != 0 || ram.enemyOffscrBitsMaskeds[x] != 0.toByte()) return

    //> lda #$01; ora Enemy_CollisionBits,x; sta Enemy_CollisionBits,x
    ram.enemyCollisionBitsArr[x] = (ram.enemyCollisionBitsArr[x].toInt() or 0x01).toByte()

    //> cpy #Spiny; beq ChkForPlayerInjury
    if (enemyId == EnemyId.Spiny.byte.toInt() and 0xFF) { chkForPlayerInjury(x); return }
    //> cpy #PiranhaPlant; beq InjurePlayer
    if (enemyId == EnemyId.PiranhaPlant.byte.toInt() and 0xFF) { injurePlayer(); return }
    //> cpy #Podoboo; beq InjurePlayer
    if (enemyId == EnemyId.Podoboo.byte.toInt() and 0xFF) { injurePlayer(); return }
    //> cpy #BulletBill_CannonVar; beq ChkForPlayerInjury
    if (enemyId == EnemyId.BulletBillCannonVar.byte.toInt() and 0xFF) { chkForPlayerInjury(x); return }
    //> cpy #$15; bcs InjurePlayer
    if (enemyId >= 0x15) { injurePlayer(); return }
    //> lda AreaType; beq InjurePlayer
    if (ram.areaType == AreaType.Water) { injurePlayer(); return }

    //> lda Enemy_State,x; asl; bcs ChkForPlayerInjury
    val state = ram.enemyState[x].toInt() and 0xFF
    if ((state and 0x80) != 0) { chkForPlayerInjury(x); return }

    //> lda Enemy_State,x; and #%00000111; cmp #$02; bcc ChkForPlayerInjury
    val lowState = state and 0x07
    if (lowState < 0x02) { chkForPlayerInjury(x); return }

    //> lda Enemy_ID,x; cmp #Goomba; beq ExPEC
    if (enemyId == EnemyId.Goomba.byte.toInt() and 0xFF) return

    //> lda #Sfx_EnemySmack; sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_EnemySmack
    //> lda Enemy_State,x; ora #%10000000; sta Enemy_State,x
    ram.enemyState[x] = (state or 0x80).toByte()
    //> jsr EnemyFacePlayer
    val dirOfs = enemyFacePlayer()
    //> lda KickedShellXSpdData,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[x + 1] = kickedShellXSpdData[dirOfs]

    //> lda #$03; clc; adc StompChainCounter
    var pointsOfs = 3 + (ram.stompChainCounter.toInt() and 0xFF)

    //> ldy EnemyIntervalTimer,x; cpy #$03; bcs KSPts
    val timer = ram.timers[0x16 + x].toInt() and 0xFF
    if (timer < 0x03) {
        //> lda KickedShellPtsData,y
        pointsOfs = kickedShellPtsData[timer].toInt() and 0xFF
    }

    //> KSPts: jsr SetupFloateyNumber
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumber(pointsOfs)
    ram.objectOffset = savedOfs
    //> ExPEC: rts
}

private fun System.chkForPlayerInjury(x: Int) {
    //> ChkForPlayerInjury:
    //> lda Player_Y_Speed; bmi ChkInj; bne EnemyStomped
    //> lda Player_Y_Speed; bmi ChkInj; bne EnemyStomped
    val playerYSpeed = ram.playerYSpeed
    if (playerYSpeed > 0) {
        // bne EnemyStomped: positive speed (moving downward) → stomp
        enemyStomped(x)
        return
    }

    // playerYSpeed <= 0: bmi branches to ChkInj if negative,
    // bne falls through if zero (also reaches ChkInj)
    //> ChkInj: lda Enemy_ID,x; cmp #Bloober; bcc ChkETmrs
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    if (enemyId >= EnemyId.Bloober.byte.toInt() and 0xFF) {
        //> lda Player_Y_Position; clc; adc #$0c; cmp Enemy_Y_Position,x
        val playerY = ram.playerYPosition.toInt() and 0xFF
        val enemyY = ram.sprObjYPos[x + 1].toInt() and 0xFF
        //> bcc EnemyStomped
        if ((playerY + 0x0c) and 0xFF < enemyY) {
            enemyStomped(x)
            return
        }
    }

    // Didn't pass the bloober/Y-position check
    //> ChkETmrs: lda StompTimer; bne EnemyStomped
    if (ram.stompTimer != 0.toByte()) { enemyStomped(x); return }
    //> lda InjuryTimer; bne ExInjColRoutines
    if (ram.injuryTimer != 0.toByte()) return

    //> lda Player_Rel_XPos; cmp Enemy_Rel_XPos; bcc TInjE
    val playerRelX = ram.playerRelXPos.toInt() and 0xFF
    val enemyRelX = ram.enemyRelXPos.toInt() and 0xFF
    if (playerRelX < enemyRelX) {
        //> TInjE: lda Enemy_MovingDir,x; cmp #$01; bne InjurePlayer
        if (ram.enemyMovingDirs[x] != 1.toByte()) {
            injurePlayer()
        } else {
            //> jmp LInj
            enemyTurnAround(x)
            injurePlayer()
        }
    } else {
        //> jmp ChkEnemyFaceRight
        chkEnemyFaceRight(x)
    }
}

private fun System.chkEnemyFaceRight(x: Int) {
    //> ChkEnemyFaceRight:
    //> lda Enemy_MovingDir,x; cmp #$01; bne LInj
    if (ram.enemyMovingDirs[x] == 1.toByte()) {
        //> jmp InjurePlayer
        injurePlayer()
    } else {
        //> LInj: jsr EnemyTurnAround; jmp InjurePlayer
        enemyTurnAround(x)
        injurePlayer()
    }
}

fun System.injurePlayer() {
    //> InjurePlayer:
    //> lda InjuryTimer; bne ExInjColRoutines
    if (ram.injuryTimer != 0.toByte()) return

    //> ForceInjury:
    forceInjury()  // defined in gameMode.kt
}

private fun System.enemyStomped(x: Int) {
    //> EnemyStomped:
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cmp #Spiny; beq InjurePlayer
    if (enemyId == EnemyId.Spiny.byte.toInt() and 0xFF) { injurePlayer(); return }

    //> lda #Sfx_EnemyStomp; sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_EnemyStomp

    //> ldy #$00
    var pointsOfs = 0
    //> cmp #FlyingCheepCheep; beq EnemyStompedPts
    //> cmp #BulletBill_FrenzyVar; beq EnemyStompedPts
    //> cmp #BulletBill_CannonVar; beq EnemyStompedPts
    //> cmp #Podoboo; beq EnemyStompedPts
    if (enemyId == EnemyId.FlyingCheepCheep.byte.toInt() and 0xFF ||
        enemyId == EnemyId.BulletBillFrenzyVar.byte.toInt() and 0xFF ||
        enemyId == EnemyId.BulletBillCannonVar.byte.toInt() and 0xFF ||
        enemyId == EnemyId.Podoboo.byte.toInt() and 0xFF) {
        // use pointsOfs = 0
    } else {
        //> iny; cmp #HammerBro; beq EnemyStompedPts
        pointsOfs = 1
        if (enemyId != EnemyId.HammerBro.byte.toInt() and 0xFF) {
            //> iny; cmp #Lakitu; beq EnemyStompedPts
            pointsOfs = 2
            if (enemyId != EnemyId.Lakitu.byte.toInt() and 0xFF) {
                //> iny; cmp #Bloober; bne ChkForDemoteKoopa
                pointsOfs = 3
                if (enemyId != EnemyId.Bloober.byte.toInt() and 0xFF) {
                    chkForDemoteKoopa(x, enemyId)
                    return
                }
            }
        }
    }

    //> EnemyStompedPts:
    val points = stompedEnemyPtsData[pointsOfs].toInt() and 0xFF
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumber(points)
    ram.objectOffset = savedOfs

    //> lda Enemy_MovingDir,x; pha
    val savedDir = ram.enemyMovingDirs[x]
    //> jsr SetStun  (same as the kill enemy logic)
    // SetStun sets enemy state d1 and applies knockback - equivalent to part of ChkToStunEnemies
    val state2 = ram.enemyState[x].toInt() and 0xF0
    ram.enemyState[x] = (state2 or 0x02).toByte()
    ram.sprObjYPos[x + 1] = (ram.sprObjYPos[x + 1] - 2).toByte()
    //> pla; sta Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = savedDir
    //> lda #%00100000; sta Enemy_State,x
    ram.enemyState[x] = 0x20
    //> jsr InitVStf; sta Enemy_X_Speed,x
    initVStf(x)
    ram.sprObjXSpeed[x + 1] = 0
    //> lda #$fd; sta Player_Y_Speed
    ram.playerYSpeed = 0xfd.toByte()
}

private fun System.chkForDemoteKoopa(x: Int, enemyId: Int) {
    //> ChkForDemoteKoopa:
    //> cmp #$09; bcc HandleStompedShellE
    if (enemyId < 0x09) {
        handleStompedShellE(x)
        return
    }
    //> and #%00000001; sta Enemy_ID,x
    ram.enemyID[x] = (enemyId and 0x01).toByte()
    //> ldy #$00; sty Enemy_State,x
    ram.enemyState[x] = 0
    //> lda #$03; jsr SetupFloateyNumber
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumber(0x03)
    ram.objectOffset = savedOfs
    //> jsr InitVStf
    initVStf(x)
    //> jsr EnemyFacePlayer
    val savedOfs2 = ram.objectOffset
    ram.objectOffset = x.toByte()
    val dirOfs = enemyFacePlayer()
    ram.objectOffset = savedOfs2
    //> lda DemotedKoopaXSpdData,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[x + 1] = demotedKoopaXSpdData[dirOfs]
    //> SBnce: lda #$fc; sta Player_Y_Speed
    ram.playerYSpeed = 0xfc.toByte()
}

private fun System.handleStompedShellE(x: Int) {
    //> HandleStompedShellE:
    //> lda #$04; sta Enemy_State,x
    ram.enemyState[x] = 0x04
    //> inc StompChainCounter
    ram.stompChainCounter = (ram.stompChainCounter + 1).toByte()
    //> lda StompChainCounter; clc; adc StompTimer
    val pointsVal = (ram.stompChainCounter.toInt() and 0xFF) + (ram.stompTimer.toInt() and 0xFF)
    //> jsr SetupFloateyNumber
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumber(pointsVal and 0xFF)
    ram.objectOffset = savedOfs
    //> inc StompTimer
    ram.stompTimer = (ram.stompTimer + 1).toByte()
    //> ldy PrimaryHardMode; lda RevivalRateData,y; sta EnemyIntervalTimer,x
    val hardMode = if (ram.primaryHardMode) 1 else 0
    ram.timers[0x16 + x] = revivalRateData[hardMode]
    //> SBnce: lda #$fc; sta Player_Y_Speed
    ram.playerYSpeed = 0xfc.toByte()
}

// =====================================================================
// HandlePowerUpCollision
// =====================================================================

private fun System.handlePowerUpCollision(x: Int) {
    //> HandlePowerUpCollision:
    //> jsr EraseEnemyObject
    eraseEnemyObject(x)
    //> lda #$06; jsr SetupFloateyNumber
    val savedOfs = ram.objectOffset
    ram.objectOffset = x.toByte()
    setupFloateyNumber(0x06)
    ram.objectOffset = savedOfs
    //> lda #Sfx_PowerUpGrab; sta Square2SoundQueue
    ram.square2SoundQueue = Constants.Sfx_PowerUpGrab
    //> lda PowerUpType; cmp #$02; bcc Shroom_Flower_PUp
    val powerUpType = ram.powerUpType.toInt() and 0xFF
    if (powerUpType >= 0x02) {
        //> cmp #$03; beq SetFor1Up
        if (powerUpType == 0x03) {
            //> SetFor1Up: lda #$0b; sta FloateyNum_Control,x
            ram.floateyNumControl[x] = 0x0b
            return
        }
        //> Star power-up
        //> lda #$23; sta StarInvincibleTimer
        ram.starInvincibleTimer = 0x23
        //> lda #StarPowerMusic; sta AreaMusicQueue
        ram.areaMusicQueue = Constants.StarPowerMusic
        return
    }

    //> Shroom_Flower_PUp:
    val playerStatus = ram.playerStatus
    //> beq UpToSuper
    if (playerStatus == PlayerStatus.Small) {
        //> UpToSuper: lda #$01; sta PlayerStatus
        ram.playerStatus = PlayerStatus.Big
        //> lda #$09 (subroutine value for super)
        setPRout(GameEngineRoutine.PlayerChangeSize, PlayerState.OnGround)
    } else if (playerStatus == PlayerStatus.Big) {
        //> bne NoPUp
        //> UpToFiery:
        //> lda #$02; sta PlayerStatus
        ram.playerStatus = PlayerStatus.Fiery
        //> jsr GetPlayerColors
        getPlayerColors()
        //> jmp UpToFiery       ;jump to set values accordingly
        //> jsr SetPRout     ;set values to stop certain things in motion
        //> lda #$0c (subroutine value for fiery)
        setPRout(GameEngineRoutine.PlayerFireFlower, PlayerState.OnGround)
    }
    //> NoPUp: rts
}

private fun System.setPRout(engineSubroutine: GameEngineRoutine, playerState: PlayerState) {
    //> SetPRout:
    //> sta GameEngineSubroutine
    ram.gameEngineSubroutine = engineSubroutine
    //> sty Player_State
    ram.playerState = playerState
    //> ldy #$ff; sty TimerControl
    ram.timerControl = 0xff.toByte()
    //> iny; sty ScrollAmount
    ram.scrollAmount = 0
}

// =====================================================================
// PlayerBGCollision
// =====================================================================

/**
 * Player vs background/terrain collision detection. The critical routine that
 * lets the player stand on ground, hit blocks, and interact with terrain.
 *
 * NOTE: This function replaces the stub in playerCtrlRoutine.kt.
 * The stub must be removed for compilation.
 */
fun System.playerBGCollision() {
    //> PlayerBGCollision:
    //> lda DisableCollisionDet; bne ExPBGCol
    if (ram.disableCollisionDet != 0.toByte()) return
    //> lda GameEngineSubroutine; cmp #$0b; beq ExPBGCol
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerDeath) return
    //> cmp #$04; bcc ExPBGCol
    if (ram.gameEngineSubroutine.ordinal < GameEngineRoutine.FlagpoleSlide.ordinal) return

    //> lda #$01; ldy SwimmingFlag; bne SetPSte
    if (ram.swimmingFlag) {
        ram.playerState = PlayerState.Falling
    } else {
        //> lda Player_State; beq SetFallS; cmp #$03; bne ChkOnScr
        // State 0: beq branches to SetFallS → state = 2
        // State 3: cmp #$03 sets Z flag, bne does NOT branch → falls through to SetFallS → state = 2
        // States 1,2: bne branches to ChkOnScr → no change
        if (ram.playerState == PlayerState.OnGround || ram.playerState == PlayerState.Climbing) {
            //> SetFallS: lda #$02; SetPSte: sta Player_State
            ram.playerState = PlayerState.FallingAlt
        }
    }

    //> ChkOnScr: lda Player_Y_HighPos; cmp #$01; bne ExPBGCol
    if ((ram.playerYHighPos.toInt() and 0xFF) != 0x01) return

    //> lda #$ff; sta Player_CollisionBits
    ram.playerCollisionBits = 0xff.toByte()

    //> lda Player_Y_Position; cmp #$cf; bcc ChkCollSize
    val playerY = ram.playerYPosition.toInt() and 0xFF
    if (playerY >= 0xcf) return

    //> ChkCollSize:
    //> ldy #$02; lda CrouchingFlag; bne GBBAdr
    var adderIdx = 2
    if (ram.crouchingFlag == 0.toByte()) {
        //> lda PlayerSize; bne GBBAdr
        if (ram.playerSize == PlayerSize.Big) {
            //> dey  ;big player not crouching
            adderIdx = 1
            //> lda SwimmingFlag; bne GBBAdr
            if (!ram.swimmingFlag) {
                //> dey
                adderIdx = 0
            }
        }
    }

    //> GBBAdr: lda BlockBufferAdderData,y; sta $eb
    val bbAdderBase = blockBufferAdderData[adderIdx].toInt() and 0xFF
    var adderY = bbAdderBase

    //> ldx PlayerSize; lda CrouchingFlag; beq HeadChk; inx
    var sizeOfs = ram.playerSize.ordinal
    if (ram.crouchingFlag != 0.toByte()) sizeOfs++

    //> HeadChk: lda Player_Y_Position; cmp PlayerBGUpperExtent,x; bcc DoFootCheck
    val upperExtent = playerBGUpperExtent[sizeOfs.coerceIn(0, 1)].toInt() and 0xFF
    if (playerY >= upperExtent) {
        //> jsr BlockBufferColli_Head
        val headResult = blockBufferColliHead(adderY)
        //> beq DoFootCheck  ;branch if nothing above player's head
        if (headResult.metatile != 0.toByte()) {
            val headMT = headResult.metatile.toInt() and 0xFF
            //> jsr CheckForCoinMTiles
            if (checkForCoinMTiles(headMT)) {
                //> bcs AwardTouchedCoin
                handleCoinMetatile(headResult)
                return
            }
            //> ldy Player_Y_Speed; bpl DoFootCheck
            if (ram.playerYSpeed < 0) {
                //> ldy $04; cpy #$04; bcc DoFootCheck
                if (headResult.lowNybble >= 0x04) {
                    //> jsr CheckForSolidMTiles; bcs SolidOrClimb
                    if (checkForSolidMTiles(headMT)) {
                        //> SolidOrClimb:
                        //> cmp #$26; beq NYSpd  ;climbing metatile, no sound
                        if (headMT != 0x26) {
                            //> lda #Sfx_Bump; sta Square1SoundQueue
                            ram.square1SoundQueue = Constants.Sfx_Bump
                        }
                        //> NYSpd: lda #$01; sta Player_Y_Speed
                        ram.playerYSpeed = 1
                    } else {
                        //> ldy AreaType; beq NYSpd  ;water level
                        if (ram.areaType == AreaType.Water) {
                            ram.playerYSpeed = 1
                        } else {
                            //> ldy BlockBounceTimer; bne NYSpd
                            if (ram.blockBounceTimer != 0.toByte()) {
                                ram.playerYSpeed = 1
                            } else {
                                //> jsr PlayerHeadCollision
                                playerHeadCollision(headResult)
                                // jmp DoFootCheck (continue below)
                            }
                        }
                    }
                }
            }
        }
    }

    //> DoFootCheck:
    adderY = bbAdderBase  // reset Y to $eb value
    if (playerY < 0xcf) {
        //> jsr BlockBufferColli_Feet  ;bottom left
        val leftFootResult = blockBufferColliFeet(adderY)
        val leftFootMT = leftFootResult.metatile.toInt() and 0xFF
        //> jsr CheckForCoinMTiles
        if (checkForCoinMTiles(leftFootMT)) {
            handleCoinMetatile(leftFootResult)
            return
        }

        //> pha  ;save bottom left metatile
        //> jsr BlockBufferColli_Feet  ;bottom right
        val rightFootResult = blockBufferColliFeet(adderY + 1)
        val rightFootMT = rightFootResult.metatile.toInt() and 0xFF

        // Assembly stores: $00 = rightFootMT, $01 = leftFootMT
        // temp00 = rightFootMT (used in handlePipeEntry as right foot)
        // temp01 = leftFootMT (used in handlePipeEntry as left foot)

        if (leftFootMT != 0) {
            // bne ChkFootMTile
            if (processFootMetatile(leftFootMT, leftFootResult, rightFootMT, adderY)) return
        } else if (rightFootMT != 0) {
            //> lda $00; beq DoPlayerSideCheck
            //> jsr CheckForCoinMTiles; bcc ChkFootMTile
            if (checkForCoinMTiles(rightFootMT)) {
                handleCoinMetatile(rightFootResult)
                return
            }
            if (processFootMetatile(rightFootMT, rightFootResult, rightFootMT, adderY)) return
        }
        // else: both are 0, fall through to DoPlayerSideCheck
    }

    //> DoPlayerSideCheck:
    doPlayerSideCheck(bbAdderBase)
}

/**
 * @return true if caller should skip DoPlayerSideCheck (NES uses jmp to exit entirely)
 */
private fun System.processFootMetatile(metatile: Int, result: BlockBufferResult, rightFootMT: Int, adderY: Int): Boolean {
    //> ChkFootMTile:
    //> jsr CheckForClimbMTiles; bcs DoPlayerSideCheck
    if (checkForClimbMTiles(metatile)) return false  // falls through to side check

    //> ldy Player_Y_Speed; bmi DoPlayerSideCheck
    if (ram.playerYSpeed < 0) return false

    //> cmp #$c5; bne ContChk
    if (metatile == 0xc5) {
        //> jmp HandleAxeMetatile  — exits PlayerBGCollision entirely
        handleAxeMetatile(result)
        return true
    }

    //> ContChk: jsr ChkInvisibleMTiles; beq DoPlayerSideCheck
    if (chkInvisibleMTiles(metatile)) return false

    //> ldy JumpspringAnimCtrl; bne InitSteP
    if (ram.jumpspringAnimCtrl != 0.toByte()) {
        //> InitSteP: lda #$00; sta Player_State
        ram.playerState = PlayerState.OnGround
        return false  // falls through to DoPlayerSideCheck
    }

    //> ldy $04; cpy #$05; bcc LandPlyr
    if (result.lowNybble < 0x05) {
        //> LandPlyr:
        //> jsr ChkForLandJumpSpring
        chkForLandJumpSpring(metatile)
        //> lda #$f0; and Player_Y_Position; sta Player_Y_Position
        // by Claude - use named scalar, not flat array (coherence fix)
        ram.playerYPosition = (ram.playerYPosition.toInt() and 0xF0).toUByte()
        //> jsr HandlePipeEntry
        handlePipeEntry(rightFootMT, metatile)
        //> lda #$00; sta Player_Y_Speed; sta Player_Y_MoveForce; sta StompChainCounter
        ram.playerYSpeed = 0
        ram.playerYMoveForce = 0
        ram.stompChainCounter = 0
        //> InitSteP: lda #$00; sta Player_State
        ram.playerState = PlayerState.OnGround
        return false  // falls through to DoPlayerSideCheck
    } else {
        //> lda Player_MovingDir; sta $00; jmp ImpedePlayerMove  — exits entirely
        impedePlayerMove(ram.playerMovingDir.byte.toInt() and 0xFF)
        return true
    }
}

private fun System.doPlayerSideCheck(bbAdderBase: Int) {
    //> DoPlayerSideCheck:
    //> ldy $eb; iny; iny  ;increment offset 2 bytes for side collisions
    var adderY = bbAdderBase + 2
    //> lda #$02; sta $00  ;counter
    var counter = 2

    while (counter > 0) {
        //> SideCheckLoop:
        //> iny; sty $eb
        adderY++

        val playerY = ram.playerYPosition.toInt() and 0xFF
        //> lda Player_Y_Position; cmp #$20; bcc BHalf
        if (playerY >= 0x20) {
            //> cmp #$e4; bcs ExSCH
            if (playerY >= 0xe4) return

            //> jsr BlockBufferColli_Side
            val sideResult = blockBufferColliSide(adderY)
            val sideMT = sideResult.metatile.toInt() and 0xFF
            //> beq BHalf
            if (sideMT != 0) {
                //> cmp #$1c; beq BHalf  ;sideways pipe top
                //> cmp #$6b; beq BHalf  ;water pipe top
                if (sideMT != 0x1c && sideMT != 0x6b) {
                    //> jsr CheckForClimbMTiles; bcc CheckSideMTiles
                    if (checkForClimbMTiles(sideMT)) {
                        // climbable: skip to BHalf
                    } else {
                        checkSideMTiles(sideMT, sideResult, counter)
                        return
                    }
                }
            }
        }

        //> BHalf: ldy $eb; iny
        adderY++
        val playerY2 = ram.playerYPosition.toInt() and 0xFF
        //> cmp #$08; bcc ExSCH
        if (playerY2 < 0x08) return
        //> cmp #$d0; bcs ExSCH
        if (playerY2 >= 0xd0) return

        //> jsr BlockBufferColli_Side
        val sideResult2 = blockBufferColliSide(adderY)
        val sideMT2 = sideResult2.metatile.toInt() and 0xFF
        //> bne CheckSideMTiles
        if (sideMT2 != 0) {
            checkSideMTiles(sideMT2, sideResult2, counter)
            return
        }

        //> dec $00; bne SideCheckLoop
        counter--
    }
    //> ExSCH: rts
}

private fun System.checkSideMTiles(metatile: Int, result: BlockBufferResult, sideCounter: Int) {
    //> CheckSideMTiles:
    //> jsr ChkInvisibleMTiles; beq ExCSM
    if (chkInvisibleMTiles(metatile)) return

    //> jsr CheckForClimbMTiles; bcc ContSChk
    if (checkForClimbMTiles(metatile)) {
        //> jmp HandleClimbing
        handleClimbing(metatile, result)
        return
    }

    //> ContSChk: jsr CheckForCoinMTiles; bcs HandleCoinMetatile
    if (checkForCoinMTiles(metatile)) {
        handleCoinMetatile(result)
        return
    }

    //> jsr ChkJumpspringMetatiles; bcc ChkPBtm
    if (chkJumpspringMetatiles(metatile)) {
        //> lda JumpspringAnimCtrl; bne ExCSM
        if (ram.jumpspringAnimCtrl != 0.toByte()) return
        //> jmp StopPlayerMove
        stopPlayerMove(sideCounter)
        return
    }

    //> ChkPBtm: ldy Player_State; cpy #$00; bne StopPlayerMove
    if (ram.playerState != PlayerState.OnGround) {
        stopPlayerMove(sideCounter)
        return
    }

    //> ldy PlayerFacingDir; dey; bne StopPlayerMove
    if (ram.playerFacingDir != Direction.Left) {
        stopPlayerMove(sideCounter)
        return
    }

    //> cmp #$6c; beq PipeDwnS
    //> cmp #$1f; bne StopPlayerMove
    if (metatile != 0x6c && metatile != 0x1f) {
        stopPlayerMove(sideCounter)
        return
    }

    //> PipeDwnS: lda Player_SprAttrib; bne PlyrPipe
    val sprAttr = ram.playerSprAttrib.byte.toInt() and 0xFF
    if (sprAttr == 0) {
        //> ldy #Sfx_PipeDown_Injury; sty Square1SoundQueue
        ram.square1SoundQueue = Constants.Sfx_PipeDown_Injury
    }
    //> PlyrPipe: ora #%00100000; sta Player_SprAttrib
    ram.playerSprAttrib = SpriteFlags((sprAttr or 0x20).toByte())

    //> lda Player_X_Position; and #%00001111; beq ChkGERtn
    val playerXLow = ram.playerXPosition.toInt() and 0x0F
    if (playerXLow != 0) {
        //> ldy #$00; lda ScreenLeft_PageLoc; beq SetCATmr; iny
        val timerIdx = if ((ram.screenLeftPageLoc.toInt() and 0xFF) == 0) 0 else 1
        //> SetCATmr: lda AreaChangeTimerData,y; sta ChangeAreaTimer
        ram.changeAreaTimer = areaChangeTimerData[timerIdx]
    }

    //> ChkGERtn: lda GameEngineSubroutine; cmp #$07; beq ExCSM
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerEntrance) return
    //> cmp #$08; bne ExCSM  — only set pipe entry when currently running PlayerCtrlRoutine
    if (ram.gameEngineSubroutine != GameEngineRoutine.PlayerCtrlRoutine) return

    //> lda #$02; sta GameEngineSubroutine
    ram.gameEngineSubroutine = GameEngineRoutine.SideExitPipeEntry
}

private fun System.stopPlayerMove(sideCounter: Int) {
    //> StopPlayerMove:
    //> jsr ImpedePlayerMove
    // NES reads $00 (the side check loop counter) — NOT playerMovingDir
    impedePlayerMove(sideCounter)
}

// =====================================================================
// ImpedePlayerMove
// =====================================================================

/**
 * Impedes player horizontal movement based on collision side.
 * @param side 1 = left side collision, 2 = right side collision (from $00)
 */
private fun System.impedePlayerMove(side: Int) {
    //> ImpedePlayerMove:
    //> lda #$00
    val displacement: Int
    val xMask: Int
    //> ldy Player_X_Speed
    val speedU = ram.playerXSpeed.toInt() and 0xFF
    //> ldx $00; dex; bne RImpd
    if (side == 1) {
        // Left side collision
        //> inx
        xMask = 1
        //> cpy #$00; bmi ExIPM  ;if player moving to the left, just clear bit
        if (speedU >= 0x80) {
            ram.playerCollisionBits = (ram.playerCollisionBits.toInt() and (xMask xor 0xFF)).toByte()
            return
        }
        //> lda #$ff; jmp NXSpd
        displacement = 0xFF
    } else {
        // Right side collision
        //> RImpd: ldx #$02
        xMask = 2
        //> cpy #$01; bpl ExIPM  ;if player moving to the right, just clear bit
        // NES bpl: branches when (speed - 1) bit 7 is 0, i.e., speed in $01-$80
        if (speedU in 1..0x80) {
            ram.playerCollisionBits = (ram.playerCollisionBits.toInt() and (xMask xor 0xFF)).toByte()
            return
        }
        //> lda #$01
        displacement = 0x01
    }

    //> NXSpd: ldy #$10; sty SideCollisionTimer
    ram.sideCollisionTimer = 0x10
    //> ldy #$00; sty Player_X_Speed
    ram.playerXSpeed = 0
    //> cmp #$00; bpl PlatF; dey
    val highBits = if (displacement >= 0x80) 0xFF else 0x00
    //> PlatF: sty $00
    //> clc; adc Player_X_Position; sta Player_X_Position
    val currentX = ram.playerXPosition.toInt() and 0xFF
    val newX = currentX + displacement
    ram.playerXPosition = (newX and 0xFF).toUByte()
    //> lda Player_PageLoc; adc $00; sta Player_PageLoc
    val carry = if (newX > 0xFF) 1 else 0
    ram.playerPageLoc = ((ram.playerPageLoc.toInt() and 0xFF) + highBits + carry).toByte()

    //> ExIPM: txa; eor #$ff; and Player_CollisionBits; sta Player_CollisionBits
    ram.playerCollisionBits = (ram.playerCollisionBits.toInt() and (xMask xor 0xFF)).toByte()
}

// =====================================================================
// HandleCoinMetatile, HandleAxeMetatile, HandleClimbing
// =====================================================================

private fun System.handleCoinMetatile(result: BlockBufferResult) {
    //> HandleCoinMetatile:
    //> jsr ErACM
    eraseAreaContentsMetatile(result)
    //> inc CoinTallyFor1Ups
    ram.coinTallyFor1Ups = (ram.coinTallyFor1Ups + 1).toByte()
    //> jmp GiveOneCoin
    giveOneCoin()
}

private fun System.eraseAreaContentsMetatile(result: BlockBufferResult) {
    //> ErACM:
    //> ldy $02; lda #$00; sta ($06),y
    val idx = result.blockBufferBase + result.vertOffset
    if (idx in result.blockBuffer.indices) {
        result.blockBuffer[idx] = 0
    }
    //> jmp RemoveCoin_Axe
    // Reconstruct NES $06 value from BlockBufferResult
    val bbLow = if (result.blockBuffer === ram.blockBuffer2) (0xD0 + result.blockBufferBase) else result.blockBufferBase
    removeCoinOrAxe(bbLow, result.vertOffset)
}

private fun System.handleAxeMetatile(result: BlockBufferResult) {
    //> HandleAxeMetatile:
    //> lda #$00; sta OperMode_Task
    ram.operModeTask = 0
    //> lda #$02; sta OperMode
    ram.operMode = OperMode.Victory
    //> lda #$18; sta Player_X_Speed
    ram.playerXSpeed = 0x18
    //> (ErACM)
    eraseAreaContentsMetatile(result)
}

private fun System.handleClimbing(metatile: Int, result: BlockBufferResult) {
    //> HandleClimbing:
    //> ldy $04; cpy #$06; bcc ExHC
    if (result.lowNybble < 0x06) return
    //> cpy #$0a; bcc ChkForFlagpole
    if (result.lowNybble >= 0x0a) return

    //> ChkForFlagpole:
    if (metatile == 0x24 || metatile == 0x25) {
        //> FlagpoleCollision:
        flagpoleCollision(metatile)
        //> jmp PutPlayerOnVine  ;all FlagpoleCollision paths fall through to PutPlayerOnVine
        putPlayerOnVine(result)
        return
    } else if (metatile == 0x26) {
        //> VineCollision:
        val playerY = ram.playerYPosition.toInt() and 0xFF
        if (playerY < 0x20) {
            //> lda #$01; sta GameEngineSubroutine
            ram.gameEngineSubroutine = GameEngineRoutine.VineAutoClimb
        }
        putPlayerOnVine(result)
    } else {
        putPlayerOnVine(result)
    }
}

private fun System.flagpoleCollision(metatile: Int) {
    //> FlagpoleCollision:
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerEndLevel) {
        // putPlayerOnVine path
        return
    }

    //> lda #$01; sta PlayerFacingDir
    ram.playerFacingDir = Direction.Left
    //> inc ScrollLock
    ram.scrollLock = (ram.scrollLock + 1).toByte()

    if (ram.gameEngineSubroutine == GameEngineRoutine.FlagpoleSlide) {
        // already running flagpole slide
        ram.gameEngineSubroutine = GameEngineRoutine.FlagpoleSlide
        return
    }

    //> lda #BulletBill_CannonVar; jsr KillEnemies
    killEnemies(EnemyId.BulletBillCannonVar.byte)
    //> lda #Silence; sta EventMusicQueue
    ram.eventMusicQueue = Constants.Silence
    //> lsr; sta FlagpoleSoundQueue
    ram.flagpoleSoundQueue = 0x40
    //> ldx #$04; lda Player_Y_Position; sta FlagpoleCollisionYPos
    // by Claude - use named scalar, not flat array (coherence fix)
    ram.flagpoleCollisionYPos = ram.playerYPosition.toByte()
    val playerY = ram.playerYPosition.toInt() and 0xFF

    //> ChkFlagpoleYPosLoop:
    var scoreIdx = 4
    while (scoreIdx > 0) {
        if (playerY >= (flagpoleYPosData[scoreIdx].toInt() and 0xFF)) break
        scoreIdx--
    }
    //> MtchF: stx FlagpoleScore
    ram.flagpoleScore = scoreIdx.toByte()
    //> RunFR: lda #$04; sta GameEngineSubroutine
    ram.gameEngineSubroutine = GameEngineRoutine.FlagpoleSlide
}

private fun System.putPlayerOnVine(result: BlockBufferResult) {
    //> PutPlayerOnVine:
    //> lda #$03; sta Player_State
    ram.playerState = PlayerState.Climbing
    //> lda #$00; sta Player_X_Speed; sta Player_X_MoveForce
    ram.playerXSpeed = 0
    ram.playerXMoveForce = 0

    //> lda Player_X_Position; sec; sbc ScreenLeft_X_Pos; cmp #$10
    val playerX = ram.playerXPosition.toInt() and 0xFF
    val screenX = ram.screenLeftXPos.toInt() and 0xFF
    val diff = (playerX - screenX) and 0xFF
    if (diff < 0x10) {
        //> lda #$02; sta PlayerFacingDir
        ram.playerFacingDir = Direction.Right
    }

    //> SetVXPl: ldy PlayerFacingDir
    val facingDir = ram.playerFacingDir.byte.toInt() and 0xFF
    //> lda $06; asl; asl; asl; asl  ;low byte of block buffer address * 16
    val bbLow = result.blockBufferBase and 0xFF  // approximate: $06 in assembly
    // Actually $06 is the low byte of the block buffer pointer.
    // In our implementation, blockBufferBase encodes the column offset.
    // The assembly multiplies it by 16 and adds an adder based on facing direction.
    // This positions the player horizontally on the vine/flagpole.
    val shiftedBBLow = (bbLow shl 4) and 0xFF
    val xPosAdder = climbXPosAdder[facingDir - 1].toInt()
    //> clc; adc ClimbXPosAdder-1,y; sta Player_X_Position
    // by Claude - use named scalar, not flat array (coherence fix)
    ram.playerXPosition = ((shiftedBBLow + xPosAdder) and 0xFF).toUByte()

    //> lda $06; bne ExPVne
    if (bbLow != 0) return

    //> lda ScreenRight_PageLoc; clc; adc ClimbPLocAdder-1,y; sta Player_PageLoc
    val pageLoc = ram.screenRightPageLoc.toInt() and 0xFF
    val pageAdder = climbPLocAdder[facingDir - 1].toInt()
    ram.playerPageLoc = ((pageLoc + pageAdder) and 0xFF).toByte()
}

private fun System.killEnemies(identifier: Byte) {
    //> KillEnemies:
    val id = identifier.toInt() and 0xFF
    for (i in 4 downTo 0) {
        if ((ram.enemyID[i].toInt() and 0xFF) == id) {
            ram.enemyFlags[i] = 0
        }
    }
}

// =====================================================================
// Pipe entry handling
// =====================================================================

private fun System.handlePipeEntry(rightFootMT: Int, leftFootMT: Int) {
    //> HandlePipeEntry:
    //> lda Up_Down_Buttons; and #%00000100; beq ExPipeE
    if ((ram.upDownButtons.toInt() and 0x04) == 0) return

    //> lda $00; cmp #$11; bne ExPipeE  ;check right foot for warp pipe right
    if ((rightFootMT and 0xFF) != 0x11) return
    //> lda $01; cmp #$10; bne ExPipeE  ;check left foot for warp pipe left
    if ((leftFootMT and 0xFF) != 0x10) return

    //> lda #$30; sta ChangeAreaTimer
    ram.changeAreaTimer = 0x30
    //> lda #$03; sta GameEngineSubroutine
    ram.gameEngineSubroutine = GameEngineRoutine.VerticalPipeEntry
    //> lda #Sfx_PipeDown_Injury; sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_PipeDown_Injury
    //> lda #%00100000; sta Player_SprAttrib
    ram.playerSprAttrib = SpriteFlags(0x20)

    //> lda WarpZoneControl; beq ExPipeE
    val warpCtrl = ram.warpZoneControl.toInt() and 0xFF
    if (warpCtrl == 0) return

    //> and #%00000011; asl; asl; tax
    var warpOfs = (warpCtrl and 0x03) shl 2
    //> lda Player_X_Position; cmp #$60; bcc GetWNum
    val playerX = ram.playerXPosition.toInt() and 0xFF
    if (playerX >= 0x60) {
        warpOfs++
        if (playerX >= 0xa0) {
            warpOfs++
        }
    }

    //> GetWNum: ldy WarpZoneNumbers,x; dey; sty WorldNumber
    // WarpZoneNumbers is a ROM data table. We use a stub here.
    handleWarpZone(warpOfs)
}

private fun System.chkForLandJumpSpring(metatile: Int) {
    //> ChkForLandJumpSpring:
    if (!chkJumpspringMetatiles(metatile)) return

    //> lda #$70; sta VerticalForce
    ram.verticalForce = 0x70
    //> lda #$f9; sta JumpspringForce
    ram.jumpspringForce = 0xf9.toByte()
    //> lda #$03; sta JumpspringTimer
    ram.jumpspringTimer = 0x03
    //> lsr; sta JumpspringAnimCtrl
    ram.jumpspringAnimCtrl = 0x01
}

// enemyToBGCollisionDet() moved to enemyBGCollision.kt
// playerHeadCollision() moved to enemyBGCollision.kt
// removeCoinAxe() moved to enemyBGCollision.kt
// handleWarpZone() moved to enemyBGCollision.kt

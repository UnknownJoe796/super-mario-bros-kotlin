// by Claude - Bounding box calculation and collision detection infrastructure
// Translates PlayerEnemyDiff, BoundingBoxCore, GetEnemyBoundBox, GetFireballBoundBox,
// GetMiscBoundBox, SmallPlatformBoundBox, LargePlatformBoundBox, CheckRightScreenBBox,
// CheckLeftScreenBBox, GetEnemyBoundBoxOfs, and related helpers.
package com.ivieleague.smbtranslation

// NOTE: GameRam currently uses scalar fields for bounding box coordinates and some
// array-indexed values. This file adds extension arrays backed by WeakHashMap for:
//   - boundBoxCoords: 4 bytes per object at $4ac (UL_X, UL_Y, DR_X, DR_Y per object)
//   - enemyOffscrBitsMaskeds: 7 bytes at $3d8 (one per enemy slot)
// These should eventually be moved into GameRam proper as:
//   val boundBoxCoords = ByteArray(60) // $4ac - replaces scalar boundingBox* fields
//   val enemyOffscrBitsMaskeds = ByteArray(7) // $3d8 - replaces scalar enemyOffscrBitsMasked

//> ;this data added to relative coordinates of sprite objects
//> ;stored in order: left edge, top edge, right edge, bottom edge
//> BoundBoxCtrlData:
//> .db $02, $08, $0e, $20
//> .db $03, $14, $0d, $20
//> .db $02, $14, $0e, $20
//> .db $02, $09, $0e, $15
//> .db $00, $00, $18, $06
//> .db $00, $00, $20, $0d
//> .db $00, $00, $30, $0d
//> .db $00, $00, $08, $08
//> .db $06, $04, $0a, $08
//> .db $03, $0e, $0d, $14
//> .db $00, $02, $10, $15
//> .db $04, $04, $0c, $1c
private val boundBoxCtrlData = intArrayOf(
    0x02, 0x08, 0x0e, 0x20,
    0x03, 0x14, 0x0d, 0x20,
    0x02, 0x14, 0x0e, 0x20,
    0x02, 0x09, 0x0e, 0x15,
    0x00, 0x00, 0x18, 0x06,
    0x00, 0x00, 0x20, 0x0d,
    0x00, 0x00, 0x30, 0x0d,
    0x00, 0x00, 0x08, 0x08,
    0x06, 0x04, 0x0a, 0x08,
    0x03, 0x0e, 0x0d, 0x14,
    0x00, 0x02, 0x10, 0x15,
    0x04, 0x04, 0x0c, 0x1c,
)

// ---------------------------------------------------------------------------
// Extension arrays for GameRam (temporary backing storage until GameRam is updated)
// ---------------------------------------------------------------------------

// BoundingBox coordinate array: BoundingBox_UL_Corner ($4ac)
// Layout: [UL_X, UL_Y, DR_X, DR_Y] per object, 4 bytes each
// Index 0-3 = player, 4-7 = enemy 0, 8-11 = enemy 1, ..., 28-31 = fireball 0, etc.
// EnemyBoundingBoxCoord ($4b0) = boundBoxCoords + 4
private val _boundBoxCoordsMap = java.util.WeakHashMap<GameRam, ByteArray>()
val GameRam.boundBoxCoords: ByteArray
    get() = _boundBoxCoordsMap.getOrPut(this) { ByteArray(100) }  // 25 sprite objects × 4 bytes each

// EnemyOffscrBitsMasked ($3d8) array, one byte per enemy slot (0-6)
private val _enemyOffscrBitsMaskedMap = java.util.WeakHashMap<GameRam, ByteArray>()
val GameRam.enemyOffscrBitsMaskeds: ByteArray
    get() = _enemyOffscrBitsMaskedMap.getOrPut(this) { ByteArray(7) }

// ---------------------------------------------------------------------------
// Helper: read relative X coordinate by condensed offset
// Condensed offsets: 0=player, 1=enemy, 2=fireball, 3=bubble, 4=block, 6=misc
// ---------------------------------------------------------------------------

private fun GameRam.getRelXPos(condensedOffset: Int): Int {
    return when (condensedOffset) {
        0 -> playerRelXPos.toInt() and 0xFF
        1 -> enemyRelXPos.toInt() and 0xFF
        2 -> fireballRelXPos.toInt() and 0xFF
        3 -> bubbleRelXPos.toInt() and 0xFF
        4 -> blockRelXPos.toInt() and 0xFF
        6 -> miscRelXPos.toInt() and 0xFF
        else -> 0
    }
}

// ---------------------------------------------------------------------------
// Helper: read relative Y coordinate by condensed offset
// ---------------------------------------------------------------------------

private fun GameRam.getRelYPos(condensedOffset: Int): Int {
    return when (condensedOffset) {
        0 -> playerRelYPos.toInt() and 0xFF
        1 -> enemyRelYPos.toInt() and 0xFF
        2 -> fireballRelYPos.toInt() and 0xFF
        3 -> bubbleRelYPos.toInt() and 0xFF
        4 -> blockRelYPos.toInt() and 0xFF
        6 -> miscRelYPos.toInt() and 0xFF
        else -> 0
    }
}

// ---------------------------------------------------------------------------
// Helper: read SprObj_BoundBoxCtrl by SprObject offset
// Assembly layout: $0499=player, $049a=enemy, $04a0=fireball, $04a2=misc
// In the assembly, Enemy_BoundBoxCtrl is indexed by enemy slot (,x) but
// the current GameRam only stores the single "current" value. For the
// bounding box routines, this is always accessed for the current ObjectOffset.
// ---------------------------------------------------------------------------

private fun GameRam.getSprObjBoundBoxCtrl(sprObjOffset: Int): Int {
    return when (sprObjOffset) {
        0 -> playerBoundBoxCtrl.toInt() and 0xFF
        in 1..6 -> enemyBoundBoxCtrl.toInt() and 0xFF
        7, 8 -> fireballBoundBoxCtrl.toInt() and 0xFF
        else -> miscBoundBoxCtrl.toInt() and 0xFF
    }
}

// ---------------------------------------------------------------------------
// Helper: read SprObject_X_Position by SprObject offset
// ---------------------------------------------------------------------------

private fun GameRam.getSprObjXPos(sprObjOffset: Int): Int {
    return when (sprObjOffset) {
        0 -> playerXPosition.toInt() and 0xFF
        in 1..6 -> enemyXPosition.toInt() and 0xFF
        7, 8 -> fireballXPosition.toInt() and 0xFF
        9, 10 -> blockXPosition.toInt() and 0xFF
        in 13..16 -> miscXPosition.toInt() and 0xFF
        else -> 0
    }
}

// ---------------------------------------------------------------------------
// Helper: read SprObject_PageLoc by SprObject offset
// ---------------------------------------------------------------------------

private fun GameRam.getSprObjPageLoc(sprObjOffset: Int): Int {
    return when (sprObjOffset) {
        0 -> playerPageLoc.toInt() and 0xFF
        in 1..6 -> enemyPageLoc.toInt() and 0xFF
        7, 8 -> fireballPageLoc.toInt() and 0xFF
        9, 10 -> blockPageLoc.toInt() and 0xFF
        in 13..16 -> miscPageLoc.toInt() and 0xFF
        else -> 0
    }
}

// ---------------------------------------------------------------------------

/**
 * Calculates horizontal distance between player and enemy object.
 * Subtracts player X position from enemy X position.
 *
 * In the assembly, the low byte of the difference is stored in $00 (a temp variable
 * read by callers), and A holds the page-level difference (with borrow).
 * Callers use the carry/sign of A to determine direction, and $00 for magnitude.
 *
 * @return Pair(lowDiff, highDiff) where lowDiff is the low byte of
 *         (Enemy_X - Player_X) and highDiff is the page difference with borrow
 */
fun System.playerEnemyDiff(): Pair<Int, Int> {
    //> PlayerEnemyDiff:

    //> lda Enemy_X_Position,x  ;get distance between enemy object's
    //> sec                     ;horizontal coordinate and the player's
    //> sbc Player_X_Position   ;horizontal coordinate
    //> sta $00                 ;and store here
    // by Claude - use indexed flat arrays (Enemy_X_Position,x = sprObjXPos[1+x])
    // instead of named scalars which always read enemy 0
    val x = ram.objectOffset.toInt()
    val enemyX = ram.sprObjXPos[1 + x].toInt() and 0xFF
    val playerX = ram.playerXPosition.toInt() and 0xFF
    val lowDiff = (enemyX - playerX) and 0xFF

    //> lda Enemy_PageLoc,x
    //> sbc Player_PageLoc      ;subtract borrow, then leave
    val borrow = if (enemyX < playerX) 1 else 0
    // by Claude - use indexed flat array (Enemy_PageLoc,x = sprObjPageLoc[1+x])
    val enemyPage = ram.sprObjPageLoc[1 + x].toInt() and 0xFF
    val playerPage = ram.playerPageLoc.toInt() and 0xFF
    val highDiff = (enemyPage - playerPage - borrow) and 0xFF

    //> rts
    return Pair(lowDiff, highDiff)
}

// ---------------------------------------------------------------------------

/**
 * Core bounding box calculation. Takes an object's relative screen coordinates
 * and bounding box control value, computes the four corner coordinates of its
 * bounding box, and stores them into the flat BoundingBox coordinate array.
 *
 * In assembly, X = SprObject offset, Y = condensed relative coordinate offset.
 * The routine multiplies X*4 to get the bounding box array offset, reads
 * SprObj_BoundBoxCtrl[X]*4 to index into BoundBoxCtrlData, then adds the
 * data table values to the relative coordinates to produce the four corners.
 *
 * @param sprObjOffset the sprite object offset (index into SprObj arrays):
 *        0=player, 1-6=enemy+1, 7-8=fireball, 9-10=block, 13-16=misc
 * @param relCoordOffset the condensed-array offset for relative coordinates:
 *        0=player, 1=enemy, 2=fireball, 3=bubble, 4-5=block, 6=misc
 */
fun System.boundingBoxCore(sprObjOffset: Int, relCoordOffset: Int) {
    //> BoundingBoxCore:
    //> stx $00                     ;save offset here

    //> lda SprObject_Rel_YPos,y    ;store object coordinates relative to screen
    //> sta $02                     ;vertically and horizontally, respectively
    val relYPos = ram.getRelYPos(relCoordOffset)

    //> lda SprObject_Rel_XPos,y
    //> sta $01
    val relXPos = ram.getRelXPos(relCoordOffset)

    //> txa                         ;multiply offset by four and save to stack
    //> asl
    //> asl
    //> pha
    //> tay                         ;use as offset for Y, X is left alone
    val bbOffset = sprObjOffset * 4  // offset into BoundingBox coordinate array

    //> lda SprObj_BoundBoxCtrl,x   ;load value here to be used as offset for X
    //> asl                         ;multiply that by four and use as X
    //> asl
    //> tax
    val ctrlValue = ram.getSprObjBoundBoxCtrl(sprObjOffset)
    val dataOffset = ctrlValue * 4  // offset into BoundBoxCtrlData

    // by Claude - On the 6502, out-of-range ctrl values just read adjacent ROM bytes.
    // In normal gameplay BoundBoxCtrl is always 0-11 (dataOffset 0-47).
    if (dataOffset + 3 >= boundBoxCtrlData.size) return

    //> lda $01                     ;add the first number in the bounding box data to the
    //> clc                         ;relative horizontal coordinate using enemy object offset
    //> adc BoundBoxCtrlData,x      ;and store somewhere using same offset * 4
    //> sta BoundingBox_UL_Corner,y ;store here (UL_X)
    ram.boundBoxCoords[bbOffset + 0] = ((relXPos + boundBoxCtrlData[dataOffset]) and 0xFF).toByte()

    //> lda $01
    //> clc
    //> adc BoundBoxCtrlData+2,x    ;add the third number in the bounding box data to the
    //> sta BoundingBox_LR_Corner,y ;relative horizontal coordinate and store (DR_X)
    ram.boundBoxCoords[bbOffset + 2] = ((relXPos + boundBoxCtrlData[dataOffset + 2]) and 0xFF).toByte()

    //> inx                         ;increment both offsets
    //> iny
    // Now using dataOffset+1 for Y-axis data, bbOffset+1 for Y-axis storage

    //> lda $02                     ;add the second number to the relative vertical coordinate
    //> clc                         ;using incremented offset and store using the other
    //> adc BoundBoxCtrlData,x      ;incremented offset
    //> sta BoundingBox_UL_Corner,y ;(UL_Y)
    ram.boundBoxCoords[bbOffset + 1] = ((relYPos + boundBoxCtrlData[dataOffset + 1]) and 0xFF).toByte()

    //> lda $02
    //> clc
    //> adc BoundBoxCtrlData+2,x    ;add the fourth number to the relative vertical coordinate
    //> sta BoundingBox_LR_Corner,y ;and store (DR_Y)
    ram.boundBoxCoords[bbOffset + 3] = ((relYPos + boundBoxCtrlData[dataOffset + 3]) and 0xFF).toByte()

    //> pla                         ;get original offset loaded into $00 * y from stack
    //> tay                         ;use as Y
    //> ldx $00                     ;get original offset and use as X again
    //> rts
}

// ---------------------------------------------------------------------------

/**
 * Checks bounding box coordinates against the right side of the screen
 * and clamps offscreen edges to $ff. If the object is on the left half,
 * delegates to checkLeftScreenBBox instead.
 *
 * @param sprObjOffset the sprite object offset in SprObject arrays
 * @param bbOffset the bounding box coordinate array offset (sprObjOffset * 4)
 */
private fun System.checkRightScreenBBox(sprObjOffset: Int, bbOffset: Int) {
    //> CheckRightScreenBBox:
    //> lda ScreenLeft_X_Pos       ;add 128 pixels to left side of screen
    //> clc                        ;and store as horizontal coordinate of middle
    //> adc #$80
    //> sta $02
    val screenLeftX = ram.screenLeftXPos.toInt() and 0xFF
    val midScreenX = (screenLeftX + 0x80) and 0xFF

    //> lda ScreenLeft_PageLoc     ;add carry to page location of left side of screen
    //> adc #$00                   ;and store as page location of middle
    //> sta $01
    val midScreenCarry = if (screenLeftX + 0x80 > 0xFF) 1 else 0
    val midScreenPage = ((ram.screenLeftPageLoc.toInt() and 0xFF) + midScreenCarry) and 0xFF

    //> lda SprObject_X_Position,x ;get horizontal coordinate
    //> cmp $02                    ;compare against middle horizontal coordinate
    //> lda SprObject_PageLoc,x    ;get page location
    //> sbc $01                    ;subtract from middle page location
    val objX = ram.getSprObjXPos(sprObjOffset)
    val objPage = ram.getSprObjPageLoc(sprObjOffset)
    // 6502 cmp sets carry if A >= operand; sbc uses carry as inverse-borrow
    val cmpBorrow = if (objX < midScreenX) 1 else 0
    val pageDiff = (objPage - midScreenPage - cmpBorrow) and 0xFF

    //> bcc CheckLeftScreenBBox    ;if object is on the left side of the screen, branch
    if (pageDiff >= 0x80) {
        // Carry clear after sbc means result was negative -> object on left side
        checkLeftScreenBBox(bbOffset)
        return
    }

    // Object is on the right half of the screen
    //> lda BoundingBox_DR_XPos,y  ;check right-side edge of bounding box for offscreen
    //> bmi NoOfs                  ;coordinates, branch if still on the screen (bit 7 set)
    val drX = ram.boundBoxCoords[bbOffset + 2].toInt() and 0xFF
    if (drX >= 0x80) {
        // Bit 7 set means coordinate is in the upper range (still on screen); skip clamping
        //> NoOfs: ldx ObjectOffset; rts
        return
    }

    // DR_X bit 7 clear means it wrapped around and is offscreen to the right
    //> lda #$ff                   ;load offscreen value to use on one or both sides
    //> ldx BoundingBox_UL_XPos,y  ;check left-side edge of bounding box for offscreen
    //> bmi SORte                  ;coordinates, and branch if still on the screen
    val ulX = ram.boundBoxCoords[bbOffset + 0].toInt() and 0xFF
    if (ulX < 0x80) {
        //> sta BoundingBox_UL_XPos,y  ;store offscreen value for left side
        ram.boundBoxCoords[bbOffset + 0] = 0xFF.toByte()
    }
    //> SORte: sta BoundingBox_DR_XPos,y  ;store offscreen value for right side
    ram.boundBoxCoords[bbOffset + 2] = 0xFF.toByte()

    //> NoOfs: ldx ObjectOffset; rts
}

/**
 * Checks bounding box coordinates against the left side of the screen
 * and clamps offscreen edges to $00.
 */
private fun System.checkLeftScreenBBox(bbOffset: Int) {
    //> CheckLeftScreenBBox:
    //> lda BoundingBox_UL_XPos,y  ;check left-side edge of bounding box for offscreen
    //> bpl NoOfs2                 ;coordinates, and branch if still on the screen
    val ulX = ram.boundBoxCoords[bbOffset + 0].toInt() and 0xFF
    if (ulX < 0x80) {
        // Bit 7 clear = positive/on-screen; nothing to do
        //> NoOfs2: ldx ObjectOffset; rts
        return
    }

    //> cmp #$a0                   ;check to see if left-side edge is in the middle of the
    //> bcc NoOfs2                 ;screen or really offscreen, and branch if still on
    if (ulX < 0xA0) {
        // Between $80 and $9f - could be near screen edge, leave alone
        //> NoOfs2: ldx ObjectOffset; rts
        return
    }

    // Truly offscreen to the left ($a0-$ff range)
    //> lda #$00
    //> ldx BoundingBox_DR_XPos,y  ;check right-side edge of bounding box for offscreen
    //> bpl SOLft                  ;coordinates, branch if still onscreen
    val drX = ram.boundBoxCoords[bbOffset + 2].toInt() and 0xFF
    if (drX >= 0x80) {
        //> sta BoundingBox_DR_XPos,y  ;store offscreen value for right side
        ram.boundBoxCoords[bbOffset + 2] = 0x00
    }
    //> SOLft: sta BoundingBox_UL_XPos,y  ;store offscreen value for left side
    ram.boundBoxCoords[bbOffset + 0] = 0x00

    //> NoOfs2: ldx ObjectOffset; rts
}

// ---------------------------------------------------------------------------

/**
 * Gets bounding box for a fireball object.
 * Adds 7 to the object offset X to address fireball SprObject memory,
 * uses condensed offset 2 for relative coordinates.
 */
fun System.getFireballBoundBox() {
    //> GetFireballBoundBox:
    val x = ram.objectOffset.toInt() and 0xFF

    //> txa         ;add seven bytes to offset
    //> clc         ;to use in routines as offset for fireball
    //> adc #$07
    //> tax
    val sprObjOffset = x + 7

    //> ldy #$02    ;set offset for relative coordinates
    val relCoordOffset = 2

    //> bne FBallB  ;unconditional branch
    //> FBallB: jsr BoundingBoxCore       ;get bounding box coordinates
    boundingBoxCore(sprObjOffset, relCoordOffset)

    //> jmp CheckRightScreenBBox  ;jump to handle any offscreen coordinates
    val bbOffset = sprObjOffset * 4
    checkRightScreenBBox(sprObjOffset, bbOffset)
}

// ---------------------------------------------------------------------------

/**
 * Gets bounding box for a misc object.
 * Adds 9 to the object offset X to address misc SprObject memory,
 * uses condensed offset 6 for relative coordinates.
 */
fun System.getMiscBoundBox() {
    //> GetMiscBoundBox:
    val x = ram.objectOffset.toInt() and 0xFF

    //> txa                       ;add nine bytes to offset
    //> clc                       ;to use in routines as offset for misc object
    //> adc #$09
    //> tax
    val sprObjOffset = x + 9

    //> ldy #$06                  ;set offset for relative coordinates
    val relCoordOffset = 6

    //> FBallB: jsr BoundingBoxCore       ;get bounding box coordinates
    boundingBoxCore(sprObjOffset, relCoordOffset)

    //> jmp CheckRightScreenBBox  ;jump to handle any offscreen coordinates
    val bbOffset = sprObjOffset * 4
    checkRightScreenBBox(sprObjOffset, bbOffset)
}

// ---------------------------------------------------------------------------

/**
 * Gets bounding box for an enemy object.
 * Checks offscreen bits to determine if the enemy should be treated as fully
 * offscreen. If onscreen, calculates bounding box using enemy's SprObject offset.
 */
fun System.getEnemyBoundBox() {
    //> GetEnemyBoundBox:
    val x = ram.objectOffset.toInt() and 0xFF

    //> ldy #$48                 ;store bitmask here for now
    //> sty $00
    val rightMask = 0x48

    //> ldy #$44                 ;store another bitmask here for now and jump
    val leftMask = 0x44

    //> jmp GetMaskedOffScrBits
    getMaskedOffScrBits(x, rightMask, leftMask)
}

// ---------------------------------------------------------------------------

/**
 * Gets bounding box for a small platform enemy.
 * Uses different bitmasks than regular enemies for offscreen detection.
 */
fun System.smallPlatformBoundBox() {
    //> SmallPlatformBoundBox:
    val x = ram.objectOffset.toInt() and 0xFF

    //> ldy #$08                 ;store bitmask here for now
    //> sty $00
    val rightMask = 0x08

    //> ldy #$04                 ;store another bitmask here for now
    val leftMask = 0x04

    //> (falls into GetMaskedOffScrBits)
    getMaskedOffScrBits(x, rightMask, leftMask)
}

// ---------------------------------------------------------------------------

/**
 * Core offscreen-bit masking and enemy bounding box setup.
 * Determines if the enemy is offscreen relative to the left edge, masks
 * offscreen bits accordingly, and either moves the bounding box fully
 * offscreen or computes it normally.
 *
 * @param x the enemy object offset (ObjectOffset value, 0-5)
 * @param rightMask bitmask to use if enemy is to the right of the left screen edge
 * @param leftMask bitmask to use if enemy is on or beyond the left screen edge
 */
private fun System.getMaskedOffScrBits(x: Int, rightMask: Int, leftMask: Int) {
    //> GetMaskedOffScrBits:
    //> lda Enemy_X_Position,x      ;get enemy object position relative
    //> sec                         ;to the left side of the screen
    //> sbc ScreenLeft_X_Pos
    //> sta $01                     ;store here
    val enemyX = ram.enemyXPosition.toInt() and 0xFF
    val screenX = ram.screenLeftXPos.toInt() and 0xFF

    //> lda Enemy_PageLoc,x         ;subtract borrow from current page location
    //> sbc ScreenLeft_PageLoc      ;of left side
    val borrow1 = if (enemyX < screenX) 1 else 0
    val enemyPage = ram.enemyPageLoc.toInt() and 0xFF
    val screenPage = ram.screenLeftPageLoc.toInt() and 0xFF
    val pageDiff = (enemyPage - screenPage - borrow1) and 0xFF

    //> bmi CMBits                  ;if enemy object is beyond left edge, branch
    val maskToUse: Int
    if (pageDiff >= 0x80) {
        // Enemy is beyond the left edge (negative page difference)
        maskToUse = leftMask
    } else {
        //> ora $01
        //> beq CMBits                  ;if precisely at the left edge, branch
        val relX = (enemyX - screenX) and 0xFF
        if (pageDiff == 0 && relX == 0) {
            maskToUse = leftMask
        } else {
            //> ldy $00                     ;if to the right of left edge, use value in $00 for A
            maskToUse = rightMask
        }
    }

    //> CMBits: tya                         ;otherwise use contents of Y
    //> and Enemy_OffscreenBits     ;preserve bitwise whatever's in here
    //> sta EnemyOffscrBitsMasked,x ;save masked offscreen bits here
    val masked = (maskToUse and (ram.enemyOffscreenBits.toInt() and 0xFF)).toByte()
    ram.enemyOffscrBitsMaskeds[x] = masked

    //> bne MoveBoundBoxOffscreen   ;if anything set here, branch
    if (masked != 0.toByte()) {
        moveBoundBoxOffscreen(x)
        return
    }

    //> jmp SetupEOffsetFBBox       ;otherwise, do something else
    setupEOffsetFBBox(x)
}

// ---------------------------------------------------------------------------

/**
 * Gets bounding box for a large platform enemy.
 * Uses GetXOffscreenBits directly to check horizontal offscreen status.
 */
fun System.largePlatformBoundBox() {
    //> LargePlatformBoundBox:
    val x = ram.objectOffset.toInt() and 0xFF

    //> inx                        ;increment X to get the proper offset
    //> jsr GetXOffscreenBits      ;then jump directly to the sub for horizontal offscreen bits
    //> dex                        ;decrement to return to original offset
    // Assembly X is ObjectOffset (enemy index), inx makes it the SprObject offset for enemies.
    val offscreenResult = getXOffscreenBits(x + 1)

    //> cmp #$fe                   ;if completely offscreen, branch to put entire bounding
    //> bcs MoveBoundBoxOffscreen  ;box offscreen, otherwise start getting coordinates
    if ((offscreenResult and 0xFF) >= 0xFE) {
        moveBoundBoxOffscreen(x)
        return
    }

    //> (falls through to SetupEOffsetFBBox)
    setupEOffsetFBBox(x)
}

// ---------------------------------------------------------------------------

/**
 * Sets up the SprObject offset for enemy bounding box, then calculates it.
 * Adds 1 to the enemy object offset to get the SprObject offset (since
 * SprObject index 0 is the player, enemies start at index 1).
 *
 * @param x the enemy object offset (ObjectOffset value, 0-5)
 */
private fun System.setupEOffsetFBBox(x: Int) {
    //> SetupEOffsetFBBox:
    //> txa                        ;add 1 to offset to properly address
    //> clc                        ;the enemy object memory locations
    //> adc #$01
    //> tax
    val sprObjOffset = x + 1

    //> ldy #$01                   ;load 1 as offset here, same reason
    val relCoordOffset = 1

    //> jsr BoundingBoxCore        ;do a sub to get the coordinates of the bounding box
    boundingBoxCore(sprObjOffset, relCoordOffset)

    //> jmp CheckRightScreenBBox   ;jump to handle offscreen coordinates of bounding box
    val bbOffset = sprObjOffset * 4
    checkRightScreenBBox(sprObjOffset, bbOffset)
}

// ---------------------------------------------------------------------------

/**
 * Moves the entire bounding box offscreen by filling all four coordinates with $ff.
 * Used when an enemy is determined to be fully offscreen.
 *
 * @param x the enemy object offset (ObjectOffset value, 0-5)
 */
private fun System.moveBoundBoxOffscreen(x: Int) {
    //> MoveBoundBoxOffscreen:
    //> txa                            ;multiply offset by 4
    //> asl
    //> asl
    //> tay                            ;use as offset here
    val bbOffset = x * 4

    //> lda #$ff
    //> sta EnemyBoundingBoxCoord,y    ;load value into four locations here and leave
    //> sta EnemyBoundingBoxCoord+1,y
    //> sta EnemyBoundingBoxCoord+2,y
    //> sta EnemyBoundingBoxCoord+3,y
    // EnemyBoundingBoxCoord ($4b0) = BoundingBox_UL_Corner ($4ac) + 4
    // So enemy offset 0 starts at global boundBoxCoords index 4.
    val globalOffset = 4 + bbOffset
    ram.boundBoxCoords[globalOffset + 0] = 0xFF.toByte()
    ram.boundBoxCoords[globalOffset + 1] = 0xFF.toByte()
    ram.boundBoxCoords[globalOffset + 2] = 0xFF.toByte()
    ram.boundBoxCoords[globalOffset + 3] = 0xFF.toByte()

    //> rts
}

// ---------------------------------------------------------------------------

/**
 * Gets the bounding box coordinate offset (Y register value) for the current
 * enemy, and checks whether the enemy is fully offscreen.
 *
 * The assembly sets Y to (ObjectOffset * 4) + 4, which is the index into
 * BoundingBox_UL_Corner for this enemy's bounding box. It also checks the
 * low nybble of Enemy_OffscreenBits to see if the enemy is fully offscreen.
 *
 * @return Pair(bbOffset, isFullyOffscreen) where bbOffset is the Y index into
 *         bounding box coordinates, and isFullyOffscreen is true when all low
 *         4 offscreen bits are set (carry is set after cmp #$0f).
 */
fun System.getEnemyBoundBoxOfs(): Pair<Int, Boolean> {
    //> GetEnemyBoundBoxOfs:
    //> lda ObjectOffset         ;get enemy object buffer offset
    val objOffset = ram.objectOffset.toInt() and 0xFF
    return getEnemyBoundBoxOfsArg(objOffset)
}

/**
 * Gets the bounding box coordinate offset for a given enemy index.
 *
 * @param enemyIndex the enemy object index (0-5), typically from ObjectOffset or A register
 * @return Pair(bbOffset, isFullyOffscreen) where bbOffset = (enemyIndex * 4) + 4,
 *         and isFullyOffscreen is true if all low 4 offscreen bits are set
 */
fun System.getEnemyBoundBoxOfsArg(enemyIndex: Int): Pair<Int, Boolean> {
    //> GetEnemyBoundBoxOfsArg:
    //> asl                      ;multiply A by four, then add four
    //> asl                      ;to skip player's bounding box
    //> clc
    //> adc #$04
    //> tay                      ;send to Y
    val bbOffset = (enemyIndex * 4) + 4

    //> lda Enemy_OffscreenBits  ;get offscreen bits for enemy object
    //> and #%00001111           ;save low nybble
    //> cmp #%00001111           ;check for all bits set
    val offBits = ram.enemyOffscreenBits.toInt() and 0x0F
    val isFullyOffscreen = (offBits == 0x0F)

    //> rts
    return Pair(bbOffset, isFullyOffscreen)
}

// getXOffscreenBits() is in offscreenBits.kt

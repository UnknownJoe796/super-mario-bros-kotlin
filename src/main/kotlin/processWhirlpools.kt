// by Claude - ProcessWhirlpools subroutine
// Translates ProcessWhirlpools: water-level whirlpool effect pulling the player.
package com.ivieleague.smbtranslation

/**
 * Processes whirlpool zones in water-type levels.
 * Checks if the player is within a whirlpool's horizontal extent,
 * then pulls the player toward the center and applies downward gravity.
 */
fun System.processWhirlpools() {
    //> ProcessWhirlpools:
    //> lda AreaType                ;check for water type level
    //> bne ExitWh                  ;branch to leave if not found
    if (ram.areaType != AreaType.Water) return
    //> sta Whirlpool_Flag          ;otherwise initialize whirlpool flag
    ram.whirlpoolFlag = 0
    //> lda TimerControl            ;if master timer control set,
    //> bne ExitWh                  ;branch to leave
    if (ram.timerControl != 0.toByte()) return

    //> ldy #$04                    ;otherwise start with last whirlpool data
    //> WhLoop:
    for (y in 4 downTo 0) {
        //> lda Whirlpool_LeftExtent,y  ;get left extent of whirlpool
        //> clc
        //> adc Whirlpool_Length,y      ;add length of whirlpool
        //> sta $02                     ;store result as right extent here
        val leftExtent = ram.cannonXPositions[y].toInt() and 0xFF  // Whirlpool_LeftExtent
        val length = ram.cannonYPositions[y].toInt() and 0xFF      // Whirlpool_Length
        val rightExtentLow = (leftExtent + length) and 0xFF
        val rightCarry = if (leftExtent + length > 0xFF) 1 else 0

        //> lda Whirlpool_PageLoc,y     ;get page location
        //> beq NextWh                  ;if none or page 0, branch to get next data
        val whirlpoolPage = ram.cannonPageLocs[y].toInt() and 0xFF  // Whirlpool_PageLoc
        if (whirlpoolPage == 0) continue
        //> adc #$00                    ;add carry
        //> sta $01                     ;store result as page location of right extent here
        val rightExtentPage = whirlpoolPage + rightCarry

        //> lda Player_X_Position       ;get player's horizontal position
        //> sec
        //> sbc Whirlpool_LeftExtent,y  ;subtract left extent
        val playerX = ram.playerXPosition.toInt() and 0xFF
        val leftSub = playerX - leftExtent
        val leftBorrow = if (leftSub < 0) 1 else 0
        //> lda Player_PageLoc          ;get player's page location
        //> sbc Whirlpool_PageLoc,y     ;subtract borrow
        val playerPage = ram.sprObjPageLoc[0].toInt() and 0xFF
        val leftPageDiff = (playerPage - whirlpoolPage - leftBorrow).toByte()
        //> bmi NextWh                  ;if player too far left, branch to get next data
        if (leftPageDiff < 0) continue

        //> lda $02                     ;otherwise get right extent
        //> sec
        //> sbc Player_X_Position       ;subtract player's horizontal coordinate
        val rightSub = rightExtentLow - playerX
        val rightBorrow = if (rightSub < 0) 1 else 0
        //> lda $01                     ;get right extent's page location
        //> sbc Player_PageLoc          ;subtract borrow
        val rightPageDiff = (rightExtentPage - playerPage - rightBorrow).toByte()
        //> bpl WhirlpoolActivate       ;if player within right extent, branch to whirlpool code
        if (rightPageDiff >= 0) {
            whirlpoolActivate(y)
            return
        }
        //> NextWh: dey                 ;move onto next whirlpool data
        //> bpl WhLoop                  ;do this until all whirlpools are checked
    }
    //> ExitWh: rts                     ;leave
}

/**
 * Activates the whirlpool effect: pulls the player toward the center
 * on alternating frames, then applies downward gravity.
 */
private fun System.whirlpoolActivate(y: Int) {
    //> WhirlpoolActivate:
    //> lda Whirlpool_Length,y      ;get length of whirlpool
    //> lsr                         ;divide by 2
    //> sta $00                     ;save here
    val halfLength = (ram.cannonYPositions[y].toInt() and 0xFF) shr 1
    //> lda Whirlpool_LeftExtent,y  ;get left extent of whirlpool
    //> clc
    //> adc $00                     ;add length divided by 2
    //> sta $01                     ;save as center of whirlpool
    val leftExtent = ram.cannonXPositions[y].toInt() and 0xFF
    val centerX = (leftExtent + halfLength) and 0xFF
    val centerCarry = if (leftExtent + halfLength > 0xFF) 1 else 0
    //> lda Whirlpool_PageLoc,y     ;get page location
    //> adc #$00                    ;add carry
    //> sta $00                     ;save as page location of whirlpool center
    val centerPage = (ram.cannonPageLocs[y].toInt() and 0xFF) + centerCarry

    //> lda FrameCounter            ;get frame counter
    //> lsr                         ;shift d0 into carry (to run on every other frame)
    //> bcc WhPull                  ;if d0 not set, branch to last part of code
    if ((ram.frameCounter.toInt() and 0x01) != 0) {
        //> lda $01                     ;get center
        //> sec
        //> sbc Player_X_Position       ;subtract player's horizontal coordinate
        val playerX = ram.playerXPosition.toInt() and 0xFF
        val centerSub = centerX - playerX
        val centerBorrow = if (centerSub < 0) 1 else 0
        //> lda $00                     ;get page location of center
        //> sbc Player_PageLoc          ;subtract borrow
        val playerPage = ram.sprObjPageLoc[0].toInt() and 0xFF
        val pageDiff = (centerPage - playerPage - centerBorrow).toByte()

        //> bpl LeftWh                  ;if player to the left of center, branch
        if (pageDiff >= 0) {
            //> LeftWh: lda Player_CollisionBits ;get player's collision bits
            //> lsr                         ;shift d0 into carry
            //> bcc WhPull                  ;if d0 not set, branch
            if ((ram.playerCollisionBits.toInt() and 0x01) != 0) {
                //> lda Player_X_Position       ;slowly pull player right, towards the center
                //> clc
                //> adc #$01                    ;add one pixel
                //> sta Player_X_Position
                val newX = (ram.playerXPosition.toInt() and 0xFF) + 1
                ram.playerXPosition = newX.toUByte()
                //> lda Player_PageLoc
                //> adc #$00                    ;add carry
                //> SetPWh: sta Player_PageLoc
                val newPage = (ram.sprObjPageLoc[0].toInt() and 0xFF) + (if (newX > 0xFF) 1 else 0)
                ram.sprObjPageLoc[0] = newPage.toByte()
            }
        } else {
            //> lda Player_X_Position       ;slowly pull player left, towards the center
            //> sec
            //> sbc #$01                    ;subtract one pixel
            //> sta Player_X_Position
            val newX = (ram.playerXPosition.toInt() and 0xFF) - 1
            ram.playerXPosition = (newX and 0xFF).toUByte()
            //> lda Player_PageLoc
            //> sbc #$00                    ;subtract borrow
            //> jmp SetPWh                  ;jump to set player's new page location
            //> SetPWh: sta Player_PageLoc
            val newPage = (ram.sprObjPageLoc[0].toInt() and 0xFF) - (if (newX < 0) 1 else 0)
            ram.sprObjPageLoc[0] = newPage.toByte()
        }
    }

    //> WhPull:
    //> lda #$10
    //> sta $00                     ;set vertical movement force
    //> lda #$01
    //> sta Whirlpool_Flag          ;set whirlpool flag to be used later
    ram.whirlpoolFlag = 1
    //> sta $02                     ;also set maximum vertical speed
    //> lsr
    //> tax                         ;set X for player offset (X=0)
    //> jmp ImposeGravity           ;jump to put whirlpool effect on player vertically
    imposeGravity(
        sprObjOffset = 0,
        downForce = 0x10,
        upForce = 0,
        maxSpeed = 0x01,
        bidirectional = false
    )
}

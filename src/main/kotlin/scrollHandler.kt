// by Claude - ScrollHandler and related scroll management routines
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*

// ---- Data tables ----

//> X_SubtracterData:
//> .db $00, $10
private val X_SubtracterData: ByteArray = byteArrayOf(0x00, 0x10)

//> OffscrJoypadBitsData:
//> .db $01, $02
private val OffscrJoypadBitsData: ByteArray = byteArrayOf(0x01, 0x02)

// ---- ScrollHandler ----

/**
 * Main scroll handler called every frame from PlayerCtrlRoutine.
 * Decides whether and how much to scroll the screen based on the player's
 * horizontal position and movement, then constrains the player if offscreen.
 */
fun System.scrollHandler() {
    //> ScrollHandler:
    //> lda Player_X_Scroll       ;load value saved here
    //> clc
    //> adc Platform_X_Scroll     ;add value used by left/right platforms
    //> sta Player_X_Scroll       ;save as new value here to impose force on scroll
    ram.playerXScroll = ((ram.playerXScroll.toInt() and 0xFF) + (ram.platformXScroll.toInt() and 0xFF)).toByte()

    //> lda ScrollLock            ;check scroll lock flag
    //> bne InitScrlAmt           ;skip a bunch of code here if set
    if (ram.scrollLock != 0.toByte()) {
        // InitScrlAmt
        ram.scrollAmount = 0
        chkPOffscr()
        return
    }

    //> lda Player_Pos_ForScroll
    //> cmp #$50                  ;check player's horizontal screen position
    //> bcc InitScrlAmt           ;if less than 80 pixels to the right, branch
    if ((ram.playerPosForScroll.toInt() and 0xFF) < 0x50) {
        ram.scrollAmount = 0
        chkPOffscr()
        return
    }

    //> lda SideCollisionTimer    ;if timer related to player's side collision
    //> bne InitScrlAmt           ;not expired, branch
    if (ram.sideCollisionTimer != 0.toByte()) {
        ram.scrollAmount = 0
        chkPOffscr()
        return
    }

    //> ldy Player_X_Scroll       ;get value and decrement by one
    //> dey                       ;if value originally set to zero or otherwise
    //> bmi InitScrlAmt           ;negative for left movement, branch
    var y = (ram.playerXScroll.toInt() and 0xFF)
    val yDecremented = (y - 1) and 0xFF
    if (yDecremented >= 0x80) {
        // Value was 0 or the result is negative (bit 7 set) -> branch to InitScrlAmt
        ram.scrollAmount = 0
        chkPOffscr()
        return
    }

    //> iny                       ;restore Y to original value
    //> cpy #$02                  ;if value $01, branch and do not decrement
    //> bcc ChkNearMid
    //> dey                       ;otherwise decrement by one
    if (y >= 0x02) {
        y -= 1 // decrement by one
    }
    // else: y stays at original (which is < 2, i.e. $01) - do not decrement

    //> ChkNearMid: lda Player_Pos_ForScroll
    //> cmp #$70                  ;check player's horizontal screen position
    //> bcc ScrollScreen          ;if less than 112 pixels to the right, branch
    if ((ram.playerPosForScroll.toInt() and 0xFF) >= 0x70) {
        //> ldy Player_X_Scroll       ;otherwise get original value undecremented
        y = ram.playerXScroll.toInt() and 0xFF
    }

    //> ScrollScreen:
    scrollScreen(y)
    chkPOffscr()
}

/**
 * Applies the computed scroll amount: updates ScrollAmount, ScrollThirtyTwo,
 * screen left position, horizontal scroll register, page locations, and PPU nametable bit.
 */
private fun System.scrollScreen(scrollValue: Int) {
    //> ScrollScreen:
    //> tya
    //> sta ScrollAmount          ;save value here
    ram.scrollAmount = scrollValue.toByte()

    //> clc
    //> adc ScrollThirtyTwo       ;add to value already set here
    //> sta ScrollThirtyTwo       ;save as new value here
    ram.scrollThirtyTwo = ((scrollValue + (ram.scrollThirtyTwo.toInt() and 0xFF)) and 0xFF).toByte()

    //> tya
    //> clc
    //> adc ScreenLeft_X_Pos      ;add to left side coordinate
    //> sta ScreenLeft_X_Pos      ;save as new left side coordinate
    //> sta HorizontalScroll      ;save here also
    val leftXResult = scrollValue + (ram.screenLeftXPos.toInt() and 0xFF)
    ram.screenLeftXPos = leftXResult.toByte()
    ram.horizontalScroll = leftXResult.toByte()

    //> lda ScreenLeft_PageLoc
    //> adc #$00                  ;add carry to page location for left
    //> sta ScreenLeft_PageLoc    ;side of the screen
    val carry = if (leftXResult > 0xFF) 1 else 0
    val newPageLoc = (ram.screenLeftPageLoc.toInt() and 0xFF) + carry
    ram.screenLeftPageLoc = newPageLoc.toByte()

    //> and #$01                  ;get LSB of page location
    //> sta $00                   ;save as temp variable for PPU register 1 mirror
    val pageLocLSB = newPageLoc and 0x01

    //> lda Mirror_PPU_CTRL_REG1  ;get PPU register 1 mirror
    //> and #%11111110            ;save all bits except d0
    //> ora $00                   ;get saved bit here and save in PPU register 1
    //> sta Mirror_PPU_CTRL_REG1  ;mirror to be used to set name table later
    val ppuBits = (ram.mirrorPPUCTRLREG1.byte.toInt() and 0xFE) or pageLocLSB
    ram.mirrorPPUCTRLREG1 = PpuControl(ppuBits.toByte())

    //> jsr GetScreenPosition     ;figure out where the right side is
    getScreenPosition()

    //> lda #$08
    //> sta ScrollIntervalTimer   ;set scroll timer (residual, not used elsewhere)
    ram.scrollIntervalTimer = 0x08

    //> jmp ChkPOffscr            ;skip this part (InitScrlAmt is skipped)
    // Control flow returns to caller which calls chkPOffscr()
}

/**
 * Checks whether the player is offscreen and constrains their position
 * to the visible screen edges if so. Also nullifies platform scroll force.
 */
private fun System.chkPOffscr() {
    //> ChkPOffscr:   ldx #$00                  ;set X for player offset
    //> jsr GetXOffscreenBits     ;get horizontal offscreen bits for player
    //> sta $00                   ;save them here
    val offscreenBits = getXOffscreenBits(sprObjOffset = 0)

    //> ldy #$00                  ;load default offset (left side)
    //> asl                       ;if d7 of offscreen bits are set,
    //> bcs KeepOnscr             ;branch with default offset
    val d7Set = (offscreenBits and 0x80) != 0
    if (d7Set) {
        // KeepOnscr with y=0 (left side)
        keepOnscr(side = 0)
        return
    }

    //> iny                         ;otherwise use different offset (right side)
    //> lda $00
    //> and #%00100000              ;check offscreen bits for d5 set
    //> beq InitPlatScrl            ;if not set, branch ahead of this part
    val d5Set = (offscreenBits and 0x20) != 0
    if (!d5Set) {
        // InitPlatScrl: just nullify platform scroll and return
        ram.platformXScroll = 0
        return
    }

    // KeepOnscr with y=1 (right side)
    keepOnscr(side = 1)
}

/**
 * Constrains the player to the visible screen edge.
 * @param side 0 = left edge, 1 = right edge
 */
private fun System.keepOnscr(side: Int) {
    //> KeepOnscr:    lda ScreenEdge_X_Pos,y      ;get left or right side coordinate based on offset
    //> sec
    //> sbc X_SubtracterData,y      ;subtract amount based on offset
    //> sta Player_X_Position       ;store as player position to prevent movement further
    val edgeXPos = if (side == 0) ram.screenLeftXPos else ram.screenRightXPos
    val subtractAmount = X_SubtracterData[side].toInt() and 0xFF
    val xResult = (edgeXPos.toInt() and 0xFF) - subtractAmount
    ram.playerXPosition = (xResult and 0xFF).toUByte()

    //> lda ScreenEdge_PageLoc,y    ;get left or right page location based on offset
    //> sbc #$00                    ;subtract borrow
    //> sta Player_PageLoc          ;save as player's page location
    val edgePageLoc = if (side == 0) ram.screenLeftPageLoc else ram.screenRightPageLoc
    val borrow = if (xResult < 0) 1 else 0
    ram.playerPageLoc = ((edgePageLoc.toInt() and 0xFF) - borrow).toByte()

    //> lda Left_Right_Buttons      ;check saved controller bits
    //> cmp OffscrJoypadBitsData,y  ;against bits based on offset
    //> beq InitPlatScrl            ;if equal, branch (player pressing away from edge)
    val buttons = ram.leftRightButtons.toInt() and 0xFF
    val expectedButtons = OffscrJoypadBitsData[side].toInt() and 0xFF
    if (buttons != expectedButtons) {
        //> lda #$00
        //> sta Player_X_Speed          ;otherwise nullify horizontal speed of player
        ram.playerXSpeed = 0
    }

    //> InitPlatScrl: lda #$00                    ;nullify platform force imposed on scroll
    //> sta Platform_X_Scroll
    ram.platformXScroll = 0
    //> rts
}

// ---- GetScreenPosition ----

/**
 * Computes the right side screen boundary from the left side position.
 * Right edge = left edge + 255 pixels (with carry propagation to page).
 */
fun System.getScreenPosition() {
    //> GetScreenPosition:
    //> lda ScreenLeft_X_Pos    ;get coordinate of screen's left boundary
    //> clc
    //> adc #$ff                ;add 255 pixels
    //> sta ScreenRight_X_Pos   ;store as coordinate of screen's right boundary
    val result = (ram.screenLeftXPos.toInt() and 0xFF) + 0xFF
    ram.screenRightXPos = result.toByte()

    //> lda ScreenLeft_PageLoc  ;get page number where left boundary is
    //> adc #$00                ;add carry from before
    //> sta ScreenRight_PageLoc ;store as page number where right boundary is
    val carry = if (result > 0xFF) 1 else 0
    ram.screenRightPageLoc = ((ram.screenLeftPageLoc.toInt() and 0xFF) + carry).toByte()
    //> rts
}

// ---- Stubs for sub-dependencies not yet in this file ----
// NOTE: titleScreenMode.kt has a private getScreenPosition() stub that should be
// removed or replaced with a call to the public getScreenPosition() defined here.

// getXOffscreenBits() is in offscreenBits.kt

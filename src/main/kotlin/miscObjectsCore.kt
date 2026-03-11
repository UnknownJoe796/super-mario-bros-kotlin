// by Claude - MiscObjectsCore subroutine
// Translates MiscObjectsCore: processes hammer and jumping coin misc objects.
package com.ivieleague.smbtranslation

/**
 * Processes all misc objects (hammers and jumping coins/floatey numbers).
 * Iterates backward through the misc object buffer (indices 8 down to 0).
 */
fun System.miscObjectsCore() {
    //> MiscObjectsCore:
    //> ldx #$08          ;set at end of misc object buffer
    //> MiscLoop:
    for (x in 8 downTo 0) {
        //> stx ObjectOffset  ;store misc object offset here
        ram.objectOffset = x.toByte()
        //> lda Misc_State,x  ;check misc object state
        val state = ram.miscStates[x].toInt() and 0xFF
        //> beq MiscLoopBack  ;branch to check next slot
        if (state == 0) continue

        //> asl               ;otherwise shift d7 into carry
        //> bcc ProcJumpCoin  ;if d7 not set, jumping coin, thus skip to rest of code here
        if ((state and 0x80) != 0) {
            //> jsr ProcHammerObj ;otherwise go to process hammer
            procHammerObj()
            //> jmp MiscLoopBack  ;then check next slot
            continue
        }

        // --- ProcJumpCoin ---
        procJumpCoin(x, state)
        //> MiscLoopBack: dex; bpl MiscLoop
    }
    //> rts
}

/**
 * Processes a jumping coin or floatey number at misc object index [x].
 */
private fun System.procJumpCoin(x: Int, state: Int) {
    //> ProcJumpCoin:
    //> ldy Misc_State,x          ;check misc object state
    //> dey                       ;decrement to see if it's set to 1
    //> beq JCoinRun              ;if so, branch to handle jumping coin
    if (state != 1) {
        //> inc Misc_State,x          ;otherwise increment state to either start off or as timer
        ram.miscStates[x] = (state + 1).toByte()

        //> lda Misc_X_Position,x     ;get horizontal coordinate for misc object
        //> clc
        //> adc ScrollAmount          ;add current scroll speed
        //> sta Misc_X_Position,x
        val miscXOfs = 13 + x  // Misc objects are at SprObject offset 13+
        val newX = (ram.sprObjXPos[miscXOfs].toInt() and 0xFF) + (ram.scrollAmount.toInt() and 0xFF)
        ram.sprObjXPos[miscXOfs] = newX.toByte()
        //> lda Misc_PageLoc,x        ;get page location
        //> adc #$00                  ;add carry
        //> sta Misc_PageLoc,x
        val newPage = (ram.sprObjPageLoc[miscXOfs].toInt() and 0xFF) + (if (newX > 0xFF) 1 else 0)
        ram.sprObjPageLoc[miscXOfs] = newPage.toByte()

        //> lda Misc_State,x
        //> cmp #$30                  ;check state of object for preset value
        //> bne RunJCSubs             ;if not yet reached, branch to subroutines
        if ((state + 1) == 0x30) {
            //> lda #$00
            //> sta Misc_State,x      ;otherwise nullify object state
            ram.miscStates[x] = 0
            //> jmp MiscLoopBack
            return
        }
    } else {
        //> JCoinRun:
        //> txa; clc; adc #$0d; tax   ;add 13 bytes to offset for next subroutine
        val sprObjOfs = x + 13
        //> lda #$50; sta $00          ;set downward movement amount
        //> lda #$06; sta $02          ;set maximum vertical speed
        //> lsr; sta $01               ;divide by 2 and set as upward movement amount
        //> lda #$00                   ;set A to impose gravity on jumping coin
        //> jsr ImposeGravity
        imposeGravity(
            sprObjOffset = sprObjOfs,
            downForce = 0x50,
            upForce = 0x03,
            maxSpeed = 0x06,
            bidirectional = false  // A=0 means down-only
        )
        //> ldx ObjectOffset          ;get original misc object offset
        //> lda Misc_Y_Speed,x        ;check vertical speed
        //> cmp #$05
        val miscYSpeed = ram.sprObjYSpeed[13 + x]
        //> bne RunJCSubs             ;if not moving downward fast enough, keep state as-is
        if (miscYSpeed == 5.toByte()) {
            //> inc Misc_State,x      ;otherwise increment state to change to floatey number
            ram.miscStates[x] = (state + 1).toByte()
        }
    }

    //> RunJCSubs:
    //> jsr RelativeMiscPosition  ;get relative coordinates
    relativeMiscPosition()
    //> jsr GetMiscOffscreenBits  ;get offscreen information
    getMiscOffscreenBits()
    //> jsr GetMiscBoundBox       ;get bounding box coordinates (why?)
    getMiscBoundBox()
    //> jsr JCoinGfxHandler       ;draw the coin or floatey number
    jCoinGfxHandler()
}

// procHammerObj() moved to hammerObj.kt
// getMiscBoundBox() moved to boundingBox.kt
// jCoinGfxHandler() moved to jCoinGfxHandler.kt

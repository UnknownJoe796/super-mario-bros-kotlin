// by Claude - BlockObjectsCore subroutine
// Translates BlockObjectsCore: processes bouncing block and brick chunk objects.
package com.ivieleague.smbtranslation

/**
 * Processes a block object (bouncing brick or brick chunks).
 * State low nybble = 1 means bouncing block, > 1 means brick chunks.
 * Block objects use SprObject offset base 9 (i.e., sprObj[9+ObjectOffset]).
 */
fun System.blockObjectsCore() {
    val x = ram.objectOffset.toInt()

    //> BlockObjectsCore:
    //> lda Block_State,x           ;get state of block object
    //> beq UpdSte                  ;if not set, branch to leave
    val state = ram.blockStates[x].toInt() and 0xFF
    if (state == 0) {
        ram.blockStates[x] = 0
        return
    }

    //> and #$0f                    ;mask out high nybble
    //> pha                         ;push to stack
    //> tay                         ;put in Y for now
    val lowNybble = state and 0x0F

    //> txa
    //> clc
    //> adc #$09                    ;add 9 bytes to offset
    //> tax
    val sprObjOfs = x + 9

    //> dey                         ;decrement Y to check for solid block state
    //> beq BouncingBlockHandler    ;branch if found
    if (lowNybble == 1) {
        bouncingBlockHandler(x, sprObjOfs, lowNybble)
        return
    }

    // --- Brick chunks path ---
    //> jsr ImposeGravityBlock      ;do sub to impose gravity on one block object
    imposeGravityBlock(sprObjOfs)
    //> jsr MoveObjectHorizontally  ;do another sub to move horizontally
    moveObjectHorizontally(sprObjOfs)

    //> txa; clc; adc #$02; tax     ;move onto next block object
    val sprObjOfs2 = sprObjOfs + 2
    //> jsr ImposeGravityBlock      ;do sub to impose gravity on other block object
    imposeGravityBlock(sprObjOfs2)
    //> jsr MoveObjectHorizontally  ;do another sub to move horizontally
    moveObjectHorizontally(sprObjOfs2)

    //> ldx ObjectOffset            ;get block object offset used for both
    //> jsr RelativeBlockPosition   ;get relative coordinates
    relativeBlockPosition()
    //> jsr GetBlockOffscreenBits   ;get offscreen information
    getBlockOffscreenBits()
    //> jsr DrawBrickChunks         ;draw the brick chunks
    drawBrickChunks()

    //> pla                         ;get lower nybble of saved state
    //> ldy Block_Y_HighPos,x       ;check vertical high byte of block object
    val yHighPos = ram.sprObjYHighPos[9 + x].toInt() and 0xFF
    //> beq UpdSte                  ;if above the screen, branch to save state
    // A = lowNybble (from pla), so UpdSte stores lowNybble, not 0
    if (yHighPos == 0) {
        ram.blockStates[x] = lowNybble.toByte()
        return
    }

    //> pha                         ;otherwise save state back into stack
    //> lda #$f0
    //> cmp Block_Y_Position+2,x    ;check to see if bottom block object went to bottom of screen
    val bottomY = ram.sprObjYPos[9 + x + 2].toInt() and 0xFF
    //> bcs ChkTop                  ;and branch if not
    if (0xF0 < bottomY) {
        //> sta Block_Y_Position+2,x ;otherwise set offscreen coordinate
        ram.sprObjYPos[9 + x + 2] = 0xF0.toByte()
    }

    //> ChkTop: lda Block_Y_Position,x ;get top block object's vertical coordinate
    val topY = ram.sprObjYPos[9 + x].toInt() and 0xFF
    //> cmp #$f0                    ;see if it went to the bottom of the screen
    //> pla                         ;pull block object state from stack
    if (topY >= 0xF0) {
        //> bcs KillBlock           ;do unconditional branch to kill it
        //> KillBlock: lda #$00
        ram.blockStates[x] = 0
    } else {
        //> bcc UpdSte              ;if not, branch to save state
        //> UpdSte: sta Block_State,x
        ram.blockStates[x] = lowNybble.toByte()
    }
}

private fun System.bouncingBlockHandler(x: Int, sprObjOfs: Int, lowNybble: Int) {
    //> BouncingBlockHandler:
    //> jsr ImposeGravityBlock     ;do sub to impose gravity on block object
    imposeGravityBlock(sprObjOfs)
    //> ldx ObjectOffset           ;get block object offset
    //> jsr RelativeBlockPosition  ;get relative coordinates
    relativeBlockPosition()
    //> jsr GetBlockOffscreenBits  ;get offscreen information
    getBlockOffscreenBits()
    //> jsr DrawBlock              ;draw the block
    drawBlock()

    //> lda Block_Y_Position,x     ;get vertical coordinate
    //> and #$0f                   ;mask out high nybble
    //> cmp #$05                   ;check to see if low nybble wrapped around
    val yLowNybble = (ram.sprObjYPos[9 + x].toInt() and 0xFF) and 0x0F
    //> pla                        ;pull state from stack
    //> bcs UpdSte                 ;if still above amount, not time to kill block yet
    if (yLowNybble >= 0x05) {
        ram.blockStates[x] = lowNybble.toByte()
    } else {
        //> lda #$01
        //> sta Block_RepFlag,x    ;otherwise set flag to replace metatile
        ram.blockRepFlags[x] = 1
        //> KillBlock: lda #$00
        //> UpdSte: sta Block_State,x
        //> ;$02 - used to store offset to block buffer
        ram.blockStates[x] = 0
    }
}

// drawBrickChunks() moved to drawRoutines.kt
// drawBlock() moved to drawRoutines.kt

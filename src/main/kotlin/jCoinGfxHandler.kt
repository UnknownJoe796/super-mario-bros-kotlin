// by Claude - JCoinGfxHandler and DrawFloateyNumber_Coin subroutines
// Translates jumping coin and floatey number rendering from smbdism.asm (lines ~13414-13464).
// IMPORTANT: The stub in miscObjectsCore.kt (line 113) for jCoinGfxHandler() must be
// removed for this real implementation to be called.
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.SpriteFlags

//> JumpingCoinTiles:
//>       .db $60, $61, $62, $63
private val JumpingCoinTiles = byteArrayOf(0x60, 0x61, 0x62, 0x63)

/**
 * Draws either a jumping coin (state < 2) or a floatey number (state >= 2)
 * for the current misc object.
 */
fun System.jCoinGfxHandler() {
    //> JCoinGfxHandler:
    val x = ram.objectOffset.toInt()
    //> ldy Misc_SprDataOffset,x    ;get coin/floatey number's OAM data offset
    val y = (ram.miscSprDataOffsets[x].toInt() and 0xFF) shr 2
    //> lda Misc_State,x            ;get state of misc object
    val state = ram.miscStates[x].toInt() and 0xFF
    //> cmp #$02                    ;if 2 or greater,
    //> bcs DrawFloateyNumber_Coin  ;branch to draw floatey number
    if (state >= 2) {
        drawFloateyNumberCoin(x, y)
        return
    }

    // --- Draw jumping coin ---
    val miscOfs = 13 + x  // SprObject offset for misc objects
    //> lda Misc_Y_Position,x       ;store vertical coordinate as
    //> sta Sprite_Y_Position,y     ;Y coordinate for first sprite
    val yPos = ram.sprObjYPos[miscOfs].toUByte()
    ram.sprites[y].y = yPos
    //> clc
    //> adc #$08                    ;add eight pixels
    //> sta Sprite_Y_Position+4,y   ;store as Y coordinate for second sprite
    ram.sprites[y + 1].y = (yPos + 8u).toUByte()

    //> lda Misc_Rel_XPos           ;get relative horizontal coordinate
    //> sta Sprite_X_Position,y
    //> sta Sprite_X_Position+4,y   ;store as X coordinate for first and second sprites
    val relX = ram.miscRelXPos.toUByte()
    ram.sprites[y].x = relX
    ram.sprites[y + 1].x = relX

    //> lda FrameCounter            ;get frame counter
    //> lsr                         ;divide by 2 to alter every other frame
    //> and #%00000011              ;mask out d2-d1
    //> tax                         ;use as graphical offset
    val tileIndex = (ram.frameCounter.toInt() ushr 1) and 0x03
    //> lda JumpingCoinTiles,x      ;load tile number
    val tile = JumpingCoinTiles[tileIndex]
    //> iny                         ;increment OAM data offset to write tile numbers
    //> jsr DumpTwoSpr              ;do sub to dump tile number into both sprites
    // DumpTwoSpr writes A to both sprite tile number slots when called after iny
    // In the NES, iny advances Y by 1 byte into the tilenumber field of the sprite,
    // and DumpTwoSpr writes to Sprite_Data,y and Sprite_Data+4,y.
    // In our Kotlin model, we write the tile number directly.
    ram.sprites[y].tilenumber = tile
    ram.sprites[y + 1].tilenumber = tile

    //> dey                         ;decrement to get old offset
    //> lda #$02
    //> sta Sprite_Attributes,y     ;set attribute byte in first sprite
    ram.sprites[y].attributes = SpriteFlags(palette = 2.toByte())
    //> lda #$82
    //> sta Sprite_Attributes+4,y   ;set attribute byte with vertical flip in second sprite
    ram.sprites[y + 1].attributes = SpriteFlags(flipVertical = true, palette = 2.toByte())

    //> ldx ObjectOffset            ;get misc object offset
    //> ExJCGfx: rts
//> ;tiles arranged in top left, right, bottom left, right order
//> ;$07 - counter
//> ;$05 - used to hold X position
//> ;$04 - used to hold sprite attributes
//> ;$03 - used to hold flip control (not used here)
//> ;$02 - used to hold bottom row Y position
//> ;$00-$01 - used to hold tiles for drawing the power-up, $00 also used to hold power-up type
}

/**
 * Draws a floatey number showing "200" for a coin that has finished jumping.
 * The number rises every other frame until it expires.
 */
private fun System.drawFloateyNumberCoin(x: Int, y: Int) {
    //> DrawFloateyNumber_Coin:
    val miscOfs = 13 + x  // SprObject offset for misc objects

    //> lda FrameCounter          ;get frame counter
    //> lsr                       ;divide by 2
    //> bcs NotRsNum              ;branch if d0 not set to raise number every other frame
    if ((ram.frameCounter.toInt() and 1) == 0) {
        //> dec Misc_Y_Position,x     ;otherwise, decrement vertical coordinate
        ram.sprObjYPos[miscOfs] = (ram.sprObjYPos[miscOfs] - 1).toByte()
    }
    //> NotRsNum:
    //> lda Misc_Y_Position,x     ;get vertical coordinate
    //> jsr DumpTwoSpr            ;dump into both sprites
    val yPos = ram.sprObjYPos[miscOfs].toUByte()
    ram.sprites[y].y = yPos
    ram.sprites[y + 1].y = yPos

    //> lda Misc_Rel_XPos         ;get relative horizontal coordinate
    //> sta Sprite_X_Position,y   ;store as X coordinate for first sprite
    val relX = ram.miscRelXPos.toUByte()
    ram.sprites[y].x = relX
    //> clc
    //> adc #$08                  ;add eight pixels
    //> sta Sprite_X_Position+4,y ;store as X coordinate for second sprite
    ram.sprites[y + 1].x = (relX + 8u).toUByte()

    //> lda #$02
    //> sta Sprite_Attributes,y   ;store attribute byte in both sprites
    //> sta Sprite_Attributes+4,y
    ram.sprites[y].attributes = SpriteFlags(palette = 2)
    ram.sprites[y + 1].attributes = SpriteFlags(palette = 2)

    //> lda #$f7
    //> sta Sprite_Tilenumber,y   ;put tile numbers into both sprites
    ram.sprites[y].tilenumber = 0xf7.toByte()
    //> lda #$fb                  ;that resemble "200"
    //> sta Sprite_Tilenumber+4,y
    ram.sprites[y + 1].tilenumber = 0xfb.toByte()

    //> jmp ExJCGfx               ;then jump to leave
}

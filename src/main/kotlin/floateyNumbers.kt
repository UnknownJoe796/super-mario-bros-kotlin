package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.utils.SpriteFlags


//> ;data is used as tiles for numbers
//> ;that appear when you defeat enemies
//> FloateyNumTileData:
//> .db $ff, $ff ;dummy
//> .db $f6, $fb ; "100"
//> .db $f7, $fb ; "200"
//> .db $f8, $fb ; "400"
//> .db $f9, $fb ; "500"
//> .db $fa, $fb ; "800"
//> .db $f6, $50 ; "1000"
//> .db $f7, $50 ; "2000"
//> .db $f8, $50 ; "4000"
//> .db $f9, $50 ; "5000"
//> .db $fa, $50 ; "8000"
//> .db $fd, $fe ; "1-UP"

//> ;high nybble is digit number, low nybble is number to
//> ;add to the digit of the player's score
//> ScoreUpdateData:
//> .db $ff ;dummy
//> .db $41, $42, $44, $45, $48
//> .db $31, $32, $34, $35, $38, $00

data class ComboInfo(val leftTileIndex: Byte, val rightTileIndex: Byte, val digitIndex: Byte, val amount: Byte) {
    companion object {
        val list = listOf(
            ComboInfo(leftTileIndex = 0.toByte(), rightTileIndex = 0.toByte(), digitIndex = -1, amount = -1),
            ComboInfo(leftTileIndex = 0xf6.toByte(), rightTileIndex = 0xfb.toByte(), digitIndex = 4, amount = 1),
            ComboInfo(leftTileIndex = 0xf7.toByte(), rightTileIndex = 0xfb.toByte(), digitIndex = 4, amount = 2),
            ComboInfo(leftTileIndex = 0xf8.toByte(), rightTileIndex = 0xfb.toByte(), digitIndex = 4, amount = 4),
            ComboInfo(leftTileIndex = 0xf9.toByte(), rightTileIndex = 0xfb.toByte(), digitIndex = 4, amount = 5),
            ComboInfo(leftTileIndex = 0xfa.toByte(), rightTileIndex = 0xfb.toByte(), digitIndex = 4, amount = 8),
            ComboInfo(leftTileIndex = 0xf6.toByte(), rightTileIndex = 0x50.toByte(), digitIndex = 3, amount = 1),
            ComboInfo(leftTileIndex = 0xf7.toByte(), rightTileIndex = 0x50.toByte(), digitIndex = 3, amount = 2),
            ComboInfo(leftTileIndex = 0xf8.toByte(), rightTileIndex = 0x50.toByte(), digitIndex = 3, amount = 4),
            ComboInfo(leftTileIndex = 0xf9.toByte(), rightTileIndex = 0x50.toByte(), digitIndex = 3, amount = 5),
            ComboInfo(leftTileIndex = 0xfa.toByte(), rightTileIndex = 0x50.toByte(), digitIndex = 3, amount = 8),
            ComboInfo(leftTileIndex = 0xfd.toByte(), rightTileIndex = 0xfe.toByte(), digitIndex = 0, amount = 0),
        )
    }
}

/**
 * @return enemy object offset
 */
fun System.floateyNumbersRoutine(comboNumber: Byte) {
    //> FloateyNumbersRoutine:
    //> lda FloateyNum_Control,x     ;load control for floatey number
    var control = ram.floateyNumControl[comboNumber]
    //> beq EndExitOne               ;if zero, branch to leave
    if (control == 0.toByte()) return
    //> cmp #$0b                     ;if less than $0b, branch
    //> bcc ChkNumTimer
    if (control >= ComboInfo.list.lastIndex) {
        //> lda #$0b                     ;otherwise set to $0b, thus keeping
        //> sta FloateyNum_Control,x     ;it in range
        control = ComboInfo.list.lastIndex.toByte()
        ram.floateyNumControl[comboNumber] = control
    }
    //> ChkNumTimer:  tay                          ;use as Y
    //> lda FloateyNum_Timer,x       ;check value here
    val currentTimer = ram.floateyNumTimer[comboNumber]
    //> bne DecNumTimer              ;if nonzero, branch ahead
    if(currentTimer == 0.toByte()) {
        //> sta FloateyNum_Control,x     ;initialize floatey number control and leave
        ram.floateyNumControl[comboNumber] = 0
        //> rts
        return
    }
    //> DecNumTimer:  dec FloateyNum_Timer,x       ;decrement value here
    ram.floateyNumTimer[comboNumber]--
    //> cmp #$2b                     ;if not reached a certain point, branch
    //> bne ChkTallEnemy
    if (currentTimer == 0x2b.toByte()) {
        //> cpy #$0b                     ;check offset for $0b
        //> bne LoadNumTiles             ;branch ahead if not found
        if(control == ComboInfo.list.lastIndex.toByte()) {
            //> inc NumberofLives            ;give player one extra life (1-up)
            ram.numberofLives++
            //> lda #Sfx_ExtraLife
            //> sta Square2SoundQueue        ;and play the 1-up sound
            ram.square2SoundQueue = Constants.Sfx_ExtraLife
        }
        //> LoadNumTiles: lda ScoreUpdateData,y        ;load point value here
        //> lsr                          ;move high nybble to low
        //> lsr
        //> lsr
        //> lsr
        //> tax                          ;use as X offset, essentially the digit
        //> lda ScoreUpdateData,y        ;load again and this time
        //> and #%00001111               ;mask out the high nybble
        //> sta DigitModifier,x          ;store as amount to add to the digit
        val data = ComboInfo.list[control]
        ram.digitModifier[data.digitIndex] = data.amount
        //> jsr AddToScore               ;update the score accordingly
        addToScore()
    }
    // wtf? this is uber cursed.  X was just overwritten in LoadNumTiles?
    var x: Int = comboNumber.toInt()
    //> ChkTallEnemy: ldy Enemy_SprDataOffset,x    ;get OAM data offset for enemy object
    var y = ram.enemySprDataOffset[x]
    //> lda Enemy_ID,x               ;get enemy object identifier
    val useAltOffset = when(ram.enemyID[x]) {
        //> cmp #Spiny
        //> beq FloateyPart              ;branch if spiny
        //> cmp #PiranhaPlant
        //> beq FloateyPart              ;branch if piranha plant
        Constants.Spiny, Constants.PiranhaPlant -> false
        //> cmp #HammerBro
        //> beq GetAltOffset             ;branch elsewhere if hammer bro
        Constants.HammerBro -> true
        //> cmp #GreyCheepCheep
        //> beq FloateyPart              ;branch if cheep-cheep of either color
        //> cmp #RedCheepCheep
        //> beq FloateyPart
        Constants.GreyCheepCheep, Constants.RedCheepCheep -> false
        //> cmp #TallEnemy
        //> bcs GetAltOffset             ;branch elsewhere if enemy object => $09
        in Constants.TallEnemy..Byte.MAX_VALUE -> true
        else -> {
            //> lda Enemy_State,x
            //> cmp #$02                     ;if enemy state defeated or otherwise
            //> bcs FloateyPart              ;$02 or greater, branch beyond this part
            ram.enemyState[x] < 0x2
        }
    }

    if(useAltOffset) {
        //> GetAltOffset: ldx SprDataOffset_Ctrl       ;load some kind of control bit
        //> ldy Alt_SprDataOffset,x      ;get alternate OAM data offset
        y = ram.altSprDataOffset[ram.sprDataOffsetCtrl]
        //> ldx ObjectOffset             ;get enemy object offset again
        x = ram.objectOffset.toInt()
    }

    //> FloateyPart:  lda FloateyNum_Y_Pos,x       ;get vertical coordinate for
    val fny = ram.floateyNumYPos[x]
    //> cmp #$18                     ;floatey number, if coordinate in the
    //> bcc SetupNumSpr              ;status bar, branch
    if(fny >= 0x18u) {
        //> sbc #$01
        //> sta FloateyNum_Y_Pos,x       ;otherwise subtract one and store as new
        ram.floateyNumYPos[x] = (fny - 1u).toUByte()
    }
    //> SetupNumSpr:  lda FloateyNum_Y_Pos,x       ;get vertical coordinate
    //> sbc #$08                     ;subtract eight and dump into the
    //> jsr DumpTwoSpr               ;left and right sprite's Y coordinates
    dumpTwoSpr(y.toInt(), (ram.floateyNumYPos[x] - 0x8u).toUByte())
    //> lda FloateyNum_X_Pos,x       ;get horizontal coordinate
    //> sta Sprite_X_Position,y      ;store into X coordinate of left sprite
    ram.sprites[y].x = ram.floateyNumXPos[x]
    //> clc
    //> adc #$08                     ;add eight pixels and store into X
    //> sta Sprite_X_Position+4,y    ;coordinate of right sprite
    ram.sprites[y.toInt()+1].x = (ram.floateyNumXPos[x] + 0x8u).toUByte()
    //> lda #$02
    //> sta Sprite_Attributes,y      ;set palette control in attribute bytes
    //> sta Sprite_Attributes+4,y    ;of left and right sprites
    ram.sprites[y].attributes = SpriteFlags(palette = 2)
    ram.sprites[y+1].attributes = SpriteFlags(palette = 2)
    //> lda FloateyNum_Control,x
    //> asl                          ;multiply our floatey number control by 2
    //> tax                          ;and use as offset for look-up table
    //> lda FloateyNumTileData,x
    //> sta Sprite_Tilenumber,y      ;display first half of number of points
    //> lda FloateyNumTileData+1,x
    //> sta Sprite_Tilenumber+4,y    ;display the second half
    val entry = ComboInfo.list[control]
    ram.sprites[y].tilenumber = entry.leftTileIndex
    ram.sprites[y+1].tilenumber = entry.rightTileIndex
    //> ldx ObjectOffset             ;get enemy object offset and leave
    //> rts
    return
}

fun System.dumpTwoSpr(index: Int, desiredY: UByte): Unit {
    ram.sprites[index].y = desiredY
    ram.sprites[index + 1].y = desiredY
}
fun System.addToScore(): Unit = TODO()

;SMB2J DISASSEMBLY (SM2DATA2 portion)

;-------------------------------------------------------------------------------------
;DEFINES

FrameCounter          = $09
Enemy_State           = $1e
Enemy_Y_Position      = $cf
PiranhaPlantUpYPos    = $0417
PiranhaPlantDownYPos  = $0434
PiranhaPlant_Y_Speed  = $58
PiranhaPlant_MoveFlag = $a0

Player_X_Scroll       = $06ff

Player_PageLoc        = $6d
Player_X_Position     = $86

AreaObjectLength      = $0730
WindFlag              = $07f9
AreaType              = $074e

TimerControl          = $0747
EnemyFrameTimer       = $078a

Sprite_Y_Position     = $0200
Sprite_Tilenumber     = $0201
Sprite_Attributes     = $0202
Sprite_X_Position     = $0203

Alt_SprDataOffset     = $06ec

NoiseSoundQueue       = $fd

MetatileBuffer        = $06a1

; import from other files
.import GetPipeHeight
.import FindEmptyEnemySlot
.import SetupPiranhaPlant
.import VerticalPipeData
.import RenderUnderPart
; export to other files
.export E_CastleArea5
.export E_CastleArea6
.export E_CastleArea7
.export E_CastleArea8
.export E_GroundArea12
.export E_GroundArea13
.export E_GroundArea14
.export E_GroundArea15
.export E_GroundArea16
.export E_GroundArea17
.export E_GroundArea18
.export E_GroundArea19
.export E_GroundArea22
.export E_GroundArea23
.export E_GroundArea24
.export E_GroundArea29
.export E_UndergroundArea4
.export E_UndergroundArea5
.export E_WaterArea2
.export E_WaterArea4
.export E_WaterArea5
.export L_CastleArea5
.export L_CastleArea6
.export L_CastleArea7
.export L_CastleArea8
.export L_GroundArea12
.export L_GroundArea13
.export L_GroundArea14
.export L_GroundArea15
.export L_GroundArea16
.export L_GroundArea17
.export L_GroundArea18
.export L_GroundArea19
.export L_GroundArea22
.export L_GroundArea23
.export L_GroundArea24
.export L_GroundArea29
.export L_UndergroundArea4
.export L_UndergroundArea5
.export L_WaterArea2
.export L_WaterArea4
.export L_WaterArea5

;-------------------------------------------------------------------------------------------------
;$06 - used to store vertical length of pipe
;$07 - starts with adder from area parser, used to store row offset

UpsideDownPipe_High:
       lda #$01                     ;start at second row
       pha
       bne UDP
UpsideDownPipe_Low:
       lda #$04                     ;start at fifth row
       pha
UDP:   jsr GetPipeHeight            ;get pipe height from object byte
       pla
       sta $07                      ;save buffer offset temporarily
       tya
       pha                          ;save pipe height temporarily
       ldy AreaObjectLength,x       ;if on second column of pipe, skip this
       beq NoUDP
       jsr FindEmptyEnemySlot       ;otherwise try to insert upside-down
       bcs NoUDP                    ;piranha plant, if no empty slots, skip this
       lda #$04
       jsr SetupPiranhaPlant        ;set up upside-down piranha plant
       lda $06
       asl
       asl                          ;multiply height of pipe by 16
       asl                          ;and add enemy Y position previously set up
       asl                          ;then subtract 10 pixels, save as new Y position
       clc
       adc Enemy_Y_Position,x
       sec
       sbc #$0a
       sta Enemy_Y_Position,x
       sta PiranhaPlantDownYPos,x   ;set as "down" position
       clc                          ;add 24 pixels, save as "up" position
       adc #$18                     ;note up and down here are reversed
       sta PiranhaPlantUpYPos,x     
       inc PiranhaPlant_MoveFlag,x  ;set movement flag
NoUDP: pla
       tay                          ;return tile offset
       pha
       ldx $07
       lda VerticalPipeData+2,y
       ldy $06                      ;render the pipe shaft
       dey
       jsr RenderUnderPart
       pla
       tay
       lda VerticalPipeData,y       ;and render the pipe end
       sta MetatileBuffer,x
       rts

       rts                        ;unused, nothing jumps here

MoveUpsideDownPiranhaP:
      lda Enemy_State,x           ;check enemy state
      bne ExMoveUDPP              ;if set at all, branch to leave
      lda EnemyFrameTimer,x       ;check enemy's timer here
      bne ExMoveUDPP              ;branch to end if not yet expired
      lda PiranhaPlant_MoveFlag,x ;check movement flag
      bne SetupToMovePPlant       ;if moving, skip to part ahead
      lda PiranhaPlant_Y_Speed,x  ;get vertical speed
      eor #$ff
      clc                         ;change to two's compliment
      adc #$01
      sta PiranhaPlant_Y_Speed,x  ;save as new vertical speed
      inc PiranhaPlant_MoveFlag,x ;increment to set movement flag

SetupToMovePPlant:
      lda PiranhaPlantUpYPos,x    ;get original vertical coordinate (lowest point)
      ldy PiranhaPlant_Y_Speed,x  ;get vertical speed
      bpl RiseFallPiranhaPlant    ;branch if moving downwards
      lda PiranhaPlantDownYPos,x  ;otherwise get other vertical coordinate (highest point)

RiseFallPiranhaPlant:
       sta $00                     ;save vertical coordinate here
       lda TimerControl            ;get master timer control
       bne ExMoveUDPP              ;branch to leave if set (likely not necessary)
       lda Enemy_Y_Position,x      ;get current vertical coordinate
       clc
       adc PiranhaPlant_Y_Speed,x  ;add vertical speed to move up or down
       sta Enemy_Y_Position,x      ;save as new vertical coordinate
       cmp $00                     ;compare against low or high coordinate
       bne ExMoveUDPP              ;branch to leave if not yet reached
       lda #$00
       sta PiranhaPlant_MoveFlag,x ;otherwise clear movement flag
       lda #$20
       sta EnemyFrameTimer,x       ;set timer to delay piranha plant movement
ExMoveUDPP:
       rts

;-------------------------------------------------------------------------------------

BlowPlayerAround:
        lda WindFlag            ;if wind is turned off, just exit
        beq ExBlow
        lda AreaType            ;don't blow the player around unless
        cmp #$01                ;the area is ground type
        bne ExBlow
        ldy #$01
        lda FrameCounter        ;branch to set d0 if on an odd frame
        asl
        bcs BThr                ;otherwise wind will only blow
        ldy #$03                ;one out of every four frames
BThr:   sty $00
        lda FrameCounter        ;throttle wind blowing by using the frame counter
        and $00                 ;to mask out certain frames
        bne ExBlow
        lda Player_X_Position   ;move player slightly to the right
        clc                     ;to simulate the wind moving the player
        adc #$01
        sta Player_X_Position
        lda Player_PageLoc
        adc #$00
        sta Player_PageLoc
        inc Player_X_Scroll     ;add one to movement speed for scroll
ExBlow: rts

;note the position data values are overwritten in RAM
LeavesYPos:
        .byte $30, $70, $b8, $50, $98, $30
        .byte $70, $b8, $50, $98, $30, $70

LeavesXPos:
        .byte $30, $30, $30, $60, $60, $a0
        .byte $a0, $a0, $d0, $d0, $d0, $60

LeavesTile:
        .byte $7b, $7b, $7b, $7b, $7a, $7a
        .byte $7b, $7b, $7b, $7a, $7b, $7a

SimulateWind:
          lda WindFlag             ;if no wind, branch to leave
          beq ExSimW
          lda #$04                 ;play wind sfx
          sta NoiseSoundQueue
          jsr ModifyLeavesPos      ;modify X and Y position data of leaves
          ldx #$00                 ;use mostly unused sprite data offset
          ldy Alt_SprDataOffset-1  ;for first six leaves
DrawLeaf: lda LeavesYPos,x
          sta Sprite_Y_Position,y  ;set up sprite data in OAM memory
          lda LeavesTile,x
          sta Sprite_Tilenumber,y
          lda #$41
          sta Sprite_Attributes,y
          lda LeavesXPos,x
          sta Sprite_X_Position,y
          iny
          iny
          iny
          iny
          inx                      ;if still on first six leaves, continue
          cpx #$06                 ;using the first sprite data offset
          bne DLLoop               ;otherwise use the next one instead
          ldy Alt_SprDataOffset    ;note the next one is also used by blocks
DLLoop:   cpx #$0c                 ;continue until done putting all leaves on the screen
          bne DrawLeaf
ExSimW:   rts

LeavesPosAdder:
   .byte $57, $57, $56, $56, $58, $58, $56, $56, $57, $58, $57, $58
   .byte $59, $59, $58, $58, $5a, $5a, $58, $58, $59, $5a, $59, $5a

ModifyLeavesPos:
         ldx #$0b
MLPLoop: lda LeavesXPos,x      ;add each adder to each X position twice
         clc                   ;and to each Y position once
         adc LeavesPosAdder,x
         adc LeavesPosAdder,x
         sta LeavesXPos,x
         lda LeavesYPos,x
         clc
         adc LeavesPosAdder,x
         sta LeavesYPos,x
         dex
         bpl MLPLoop
         rts

WindOn:
     lda #$01         ;branch to turn the wind on
     bne WOn
WindOff:
     lda #$00         ;turn the wind off
WOn: sta WindFlag
     rts

;some unused bytes
   .byte $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff

;-------------------------------------------------------------------------------------

;level 5-4
E_CastleArea5:
  .byte $2a, $a9, $6b, $0c, $cb, $0c, $15, $9c, $89, $1c, $cc, $1d, $09, $9d, $f5, $1c
  .byte $6b, $a9, $ab, $0c, $db, $29, $48, $9d, $9b, $0c, $5b, $8c, $a5, $1c, $49, $9d
  .byte $79, $1d, $09, $9d, $6b, $0c, $c9, $1f, $3b, $8c, $88, $95, $b9, $1c, $19, $9d
  .byte $30, $cc, $78, $2d, $a6, $28, $90, $b5, $ff

;level 6-4
E_CastleArea6:
  .byte $0f, $02, $09, $1f, $8b, $85, $2b, $8c, $e9, $1b, $25, $9d, $0f, $07, $09, $1d
  .byte $6d, $28, $99, $1b, $b5, $2c, $4b, $8c, $09, $9f, $fb, $15, $9d, $a8, $0f, $0c
  .byte $2b, $0c, $78, $2d, $90, $b5, $ff

;level 7-4
E_CastleArea7:
  .byte $05, $9d, $0d, $a8, $dd, $1d, $07, $ac, $54, $2c, $a2, $2c, $f4, $2c, $42, $ac
  .byte $26, $9d, $d4, $03, $24, $83, $64, $03, $2b, $82, $4b, $02, $7b, $02, $9b, $02
  .byte $5b, $82, $7b, $02, $0b, $82, $2b, $02, $c6, $1b, $28, $82, $48, $02, $a6, $1b
  .byte $7b, $95, $85, $0c, $9d, $9b, $0f, $0e, $78, $2d, $7a, $1d, $90, $b5, $ff

;level 8-4
E_CastleArea8:
  .byte $19, $9b, $99, $1b, $2c, $8c, $59, $1b, $c5, $0f, $0e, $83, $e0, $0f, $06, $2e
  .byte $67, $e7, $0f, $08, $9b, $07, $0e, $83, $e0, $39, $0e, $87, $10, $bd, $28, $59
  .byte $9f, $0f, $0f, $34, $0f, $77, $10, $9e, $67, $f1, $0f, $12, $0e, $67, $e3, $78
  .byte $2d, $0f, $15, $3b, $29, $57, $82, $0f, $18, $55, $1d, $78, $2d, $90, $b5, $ff

;level 5-1
E_GroundArea12:
  .byte $1b, $82, $4b, $02, $7b, $02, $ab, $02, $0f, $03, $f9, $0e, $d0, $be, $8e, $c4
  .byte $86, $f8, $0e, $c0, $ba, $0f, $0d, $3a, $0e, $bb, $02, $30, $b7, $80, $bc, $c0
  .byte $bc, $0f, $12, $24, $0f, $54, $0f, $ce, $3c, $80, $d3, $0f, $cb, $8e, $f9, $0e
  .byte $ff

;level 5-3
E_GroundArea13:
  .byte $0a, $aa, $15, $8f, $44, $0f, $4e, $44, $80, $d8, $07, $57, $90, $0f, $06, $67
  .byte $24, $8b, $17, $b9, $24, $ab, $97, $16, $87, $2a, $28, $84, $0f, $57, $a9, $a5
  .byte $29, $f5, $29, $a7, $a4, $0a, $a4, $ff

;level 6-1
E_GroundArea14:
  .byte $07, $82, $67, $0e, $40, $bd, $e0, $38, $d0, $bc, $6e, $84, $a0, $9b, $05, $0f
  .byte $06, $bb, $05, $0f, $08, $0b, $0e, $4b, $0e, $0f, $0a, $05, $29, $85, $29, $0f
  .byte $0c, $dd, $28, $ff

;level 6-3
E_GroundArea15:
  .byte $0f, $02, $28, $10, $e6, $03, $d8, $90, $0f, $05, $85, $0f, $78, $83, $c8, $10
  .byte $18, $83, $58, $83, $f7, $90, $0f, $0c, $43, $0f, $73, $8f, $ff

;level 7-1
E_GroundArea16:
  .byte $a7, $83, $d7, $03, $0f, $03, $6b, $00, $0f, $06, $e3, $0f, $14, $8f, $3e, $44
  .byte $c3, $0b, $80, $87, $05, $ab, $05, $db, $83, $0f, $0b, $07, $05, $13, $0e, $2b
  .byte $02, $4b, $02, $0f, $10, $0b, $0e, $b0, $37, $90, $bc, $80, $bc, $ae, $44, $c0
  .byte $ff

;level 7-2
E_GroundArea17:
  .byte $0a, $aa, $d5, $8f, $03, $8f, $3e, $44, $c6, $d8, $83, $0f, $06, $a6, $11, $b9
  .byte $0e, $39, $9d, $79, $1b, $a6, $11, $e8, $03, $87, $83, $16, $90, $a6, $11, $b9
  .byte $1d, $05, $8f, $38, $29, $89, $29, $26, $8f, $46, $29, $ff

;level 7-3
E_GroundArea18:
  .byte $0f, $04, $a3, $10, $0f, $09, $e3, $29, $0f, $0d, $55, $24, $a9, $24, $0f, $11
  .byte $59, $1d, $a9, $1b, $23, $8f, $15, $9b, $ff

;level 8-1
E_GroundArea19:
  .byte $0f, $01, $db, $02, $30, $b7, $80, $3b, $1b, $8e, $4a, $0e, $eb, $03, $3b, $82
  .byte $5b, $02, $e5, $0f, $14, $8f, $44, $0f, $5b, $82, $0c, $85, $35, $8f, $06, $85
  .byte $e3, $05, $db, $83, $3e, $84, $e0, $ff

;level 8-2
E_GroundArea22:
  .byte $0f, $02, $0a, $29, $f7, $02, $80, $bc, $6b, $82, $7b, $02, $9b, $02, $ab, $02
  .byte $39, $8e, $0f, $07, $ce, $35, $ec, $f5, $0f, $fb, $85, $fb, $85, $3e, $c4, $e3
  .byte $a7, $02, $ff

;level 8-3
E_GroundArea23:
  .byte $09, $a9, $86, $11, $d5, $10, $a3, $8f, $d5, $29, $86, $91, $2b, $83, $58, $03
  .byte $5b, $85, $eb, $05, $3e, $bc, $e0, $0f, $09, $43, $0f, $74, $0f, $6b, $85, $db
  .byte $05, $c6, $a4, $19, $a4, $12, $8f
;another unused area
E_GroundArea24:
  .byte $ff

;cloud level used with level 5-1
E_GroundArea29:
  .byte $0a, $aa, $2e, $2b, $98, $2e, $36, $e7, $ff

;level 5-2
E_UndergroundArea4:
  .byte $0b, $83, $b7, $03, $d7, $03, $0f, $05, $67, $03, $7b, $02, $9b, $02, $80, $b9
  .byte $3b, $83, $4e, $b4, $80, $86, $2b, $c9, $2c, $16, $ac, $67, $b4, $de, $3b, $81
  .byte $ff

;underground bonus rooms used with worlds 5-8
E_UndergroundArea5:
  .byte $1e, $af, $ca, $1e, $2c, $85, $0f, $04, $1e, $2d, $a7, $1e, $2f, $ce, $1e, $35
  .byte $e5, $0f, $07, $1e, $2b, $87, $1e, $30, $c5, $ff

;level 6-2
E_WaterArea2:
  .byte $0f, $01, $2e, $3b, $a1, $5b, $07, $ab, $07, $69, $87, $ba, $07, $fb, $87, $65
  .byte $a7, $6a, $27, $a6, $a7, $ac, $27, $1b, $87, $88, $07, $2b, $83, $7b, $07, $a7
  .byte $90, $e5, $83, $14, $a7, $19, $27, $77, $07, $f8, $07, $47, $8f, $b9, $07, $ff

;water area used in level 8-4
E_WaterArea4:
  .byte $07, $9b, $0a, $07, $b9, $1b, $66, $9b, $78, $07, $ae, $67, $e5, $ff

;water area used in level 6-1
E_WaterArea5:
  .byte $97, $87, $cb, $00, $ee, $2b, $f8, $fe, $2d, $ad, $75, $87, $d3, $27, $d9, $27
  .byte $0f, $04, $56, $0f, $ff

;level 5-4
L_CastleArea5:
  .byte $9b, $07, $05, $32, $06, $33, $07, $33, $3e, $03, $4c, $50, $4e, $07, $57, $31
  .byte $6e, $03, $7c, $52, $9e, $07, $fe, $0a, $7e, $89, $9e, $0a, $ee, $09, $fe, $0b
  .byte $13, $8e, $1e, $09, $3e, $0a, $6e, $09, $87, $0e, $9e, $02, $c6, $07, $ca, $0e
  .byte $f7, $62, $07, $8e, $08, $61, $17, $62, $1e, $0a, $4e, $06, $5e, $0a, $7e, $06
  .byte $8e, $0a, $ae, $06, $be, $07, $f3, $0e, $1e, $86, $2e, $0a, $84, $37, $93, $36
  .byte $a2, $45, $1e, $89, $46, $0e, $6e, $0a, $a7, $31, $db, $60, $f7, $60, $1b, $e0
  .byte $37, $31, $7e, $09, $8e, $0b, $a3, $0e, $fe, $04, $17, $bb, $47, $0e, $77, $0e
  .byte $be, $02, $ce, $0a, $07, $8e, $17, $31, $63, $31, $a7, $34, $c7, $0e, $13, $b1
  .byte $4e, $09, $1e, $8a, $7e, $02, $97, $34, $b7, $0e, $ce, $0a, $de, $02, $d8, $61
  .byte $f7, $62, $fe, $03, $07, $b4, $17, $0e, $47, $62, $4e, $0a, $5e, $03, $51, $61
  .byte $67, $62, $77, $34, $b7, $62, $c1, $61, $da, $60, $e9, $61, $f8, $62, $fe, $0a
  .byte $0d, $c4, $01, $52, $11, $52, $21, $52, $31, $52, $41, $52, $51, $52, $61, $52
  .byte $cd, $43, $ce, $09, $de, $0b, $dd, $42, $fe, $02, $5d, $c7, $fd

;level 6-4
L_CastleArea6:
  .byte $5b, $09, $05, $32, $06, $33, $4e, $0a, $87, $31, $fe, $02, $88, $f2, $c7, $33
  .byte $0d, $02, $07, $0e, $17, $34, $6e, $0a, $8e, $02, $bf, $67, $ed, $4b, $b7, $b6
  .byte $c3, $35, $1e, $8a, $2e, $02, $33, $3f, $37, $3f, $88, $f2, $c7, $33, $ed, $4b
  .byte $0d, $06, $03, $33, $0f, $74, $47, $73, $67, $73, $7e, $09, $9e, $0a, $ed, $4b
  .byte $f7, $32, $07, $8e, $97, $0e, $ae, $00, $de, $02, $e3, $35, $e7, $35, $3e, $8a
  .byte $4e, $02, $53, $3e, $57, $3e, $07, $8e, $a7, $34, $bf, $63, $ed, $4b, $2e, $8a
  .byte $fe, $06, $2e, $88, $34, $33, $35, $33, $6e, $06, $8e, $0c, $be, $06, $fe, $0a
  .byte $01, $d2, $0d, $44, $11, $52, $21, $52, $31, $52, $41, $52, $42, $0b, $51, $52
  .byte $61, $52, $cd, $43, $ce, $09, $dd, $42, $de, $0b, $fe, $02, $5d, $c7, $fd

;level 7-4
L_CastleArea7:
  .byte $58, $07, $05, $35, $06, $3d, $07, $3d, $be, $06, $de, $0c, $f3, $3d, $03, $8e
  .byte $6e, $43, $ce, $0a, $e1, $67, $f1, $67, $01, $e7, $11, $67, $1e, $05, $28, $39
  .byte $6e, $40, $be, $01, $c7, $06, $db, $0e, $de, $00, $1f, $80, $6f, $00, $bf, $00
  .byte $0f, $80, $5f, $00, $7e, $05, $a8, $37, $fe, $02, $24, $8e, $34, $30, $3e, $0c
  .byte $4e, $43, $ae, $0a, $be, $0c, $ee, $0a, $fe, $0c, $2e, $8a, $3e, $0c, $7e, $02
  .byte $8e, $0e, $98, $36, $b9, $34, $08, $bf, $09, $3f, $0e, $82, $2e, $86, $4e, $0c
  .byte $9e, $09, $c1, $62, $c4, $0e, $ee, $0c, $0e, $86, $5e, $0c, $7e, $09, $a1, $62
  .byte $a4, $0e, $ce, $0c, $fe, $0a, $28, $b4, $a6, $31, $e8, $34, $8b, $b2, $9b, $0e
  .byte $fe, $07, $fe, $8a, $0d, $c4, $cd, $43, $ce, $09, $dd, $42, $de, $0b, $fe, $02
  .byte $5d, $c7, $fd

;level 8-4
L_CastleArea8:
  .byte $5b, $03, $05, $34, $06, $35, $07, $36, $6e, $0a, $ee, $02, $fe, $05, $0d, $01
  .byte $17, $0e, $97, $0e, $9e, $02, $c6, $05, $fa, $30, $fe, $0a, $4e, $82, $57, $0e
  .byte $58, $62, $68, $62, $79, $61, $8a, $60, $8e, $0a, $f5, $31, $f9, $7b, $39, $f3
  .byte $97, $33, $b5, $71, $39, $f3, $4d, $48, $9e, $02, $ae, $05, $cd, $4a, $ed, $4b
  .byte $0e, $81, $17, $06, $39, $73, $5c, $02, $85, $65, $95, $32, $a9, $7b, $cc, $03
  .byte $5e, $8f, $6d, $47, $fe, $02, $0d, $07, $39, $73, $4e, $0a, $ae, $02, $ec, $71
  .byte $07, $81, $17, $02, $39, $73, $e6, $05, $39, $fb, $4e, $0a, $c4, $31, $eb, $61
  .byte $fe, $02, $07, $b0, $1e, $0a, $4e, $06, $57, $0e, $be, $02, $c9, $61, $da, $60
  .byte $ed, $4b, $0e, $85, $0d, $0e, $fe, $0a, $78, $e4, $8e, $06, $b3, $06, $bf, $47
  .byte $ee, $0f, $6d, $c7, $0e, $82, $39, $73, $9a, $60, $a9, $61, $ae, $06, $de, $0a
  .byte $e7, $03, $eb, $79, $f7, $03, $fe, $06, $0d, $14, $fe, $0a, $5e, $82, $7f, $66
  .byte $9e, $0a, $f8, $64, $fe, $0b, $9e, $84, $be, $05, $be, $82, $da, $60, $e9, $61
  .byte $f8, $62, $fe, $0a, $0d, $c4, $11, $64, $51, $62, $cd, $43, $ce, $09, $dd, $42
  .byte $de, $0b, $fe, $02, $5d, $c7, $fd

;level 5-1
L_GroundArea12:
  .byte $52, $b1, $0f, $20, $6e, $75, $cc, $73, $a3, $b3, $bf, $74, $0c, $84, $83, $3f
  .byte $9f, $74, $ef, $71, $ec, $01, $2f, $f1, $2c, $01, $6f, $71, $6c, $01, $a8, $91
  .byte $aa, $10, $77, $fb, $56, $f4, $39, $f1, $bf, $37, $33, $e7, $43, $04, $47, $03
  .byte $6c, $05, $c3, $67, $d3, $67, $e3, $67, $ed, $4c, $fc, $07, $73, $e7, $83, $67
  .byte $93, $67, $a3, $67, $bc, $08, $43, $e7, $53, $67, $dc, $02, $59, $91, $c3, $33
  .byte $d9, $71, $df, $72, $2d, $cd, $5b, $71, $9b, $71, $3b, $f1, $a7, $c2, $db, $71
  .byte $0d, $10, $9b, $71, $0a, $b0, $1c, $04, $67, $63, $76, $64, $85, $65, $94, $66
  .byte $a3, $67, $b3, $67, $cc, $09, $73, $a3, $87, $22, $b3, $09, $d6, $83, $e3, $03
  .byte $fe, $3f, $0d, $15, $de, $31, $ec, $01, $03, $f7, $9d, $41, $df, $26, $0d, $18
  .byte $39, $71, $7f, $37, $f2, $68, $01, $e9, $11, $39, $68, $7a, $de, $3f, $6d, $c5
  .byte $fd

;level 5-3
L_GroundArea13:
  .byte $50, $11, $0f, $26, $df, $32, $fe, $10, $0d, $01, $98, $74, $c8, $13, $52, $e1
  .byte $63, $31, $61, $79, $c6, $61, $06, $e1, $8b, $71, $ab, $71, $e4, $19, $eb, $19
  .byte $60, $86, $c8, $13, $cd, $4b, $39, $f3, $98, $13, $17, $f5, $7c, $15, $7f, $13
  .byte $cf, $15, $d4, $40, $0b, $9a, $23, $16, $32, $44, $a3, $95, $b2, $43, $0d, $0a
  .byte $27, $14, $3d, $4a, $a4, $40, $bc, $16, $bf, $13, $c4, $40, $04, $c0, $1f, $16
  .byte $24, $40, $43, $31, $ce, $11, $dd, $41, $0e, $d2, $3f, $20, $3d, $c7, $fd
 
;level 6-1
L_GroundArea14:
  .byte $52, $a1, $0f, $20, $6e, $40, $d6, $61, $e7, $07, $f7, $21, $16, $e1, $34, $63
  .byte $47, $21, $54, $04, $67, $0a, $74, $63, $dc, $01, $06, $e1, $17, $26, $86, $61
  .byte $66, $c2, $58, $c1, $f7, $03, $04, $f6, $8a, $10, $9c, $04, $e8, $62, $f9, $61
  .byte $0a, $e0, $53, $31, $5f, $73, $7b, $71, $77, $25, $fc, $e2, $17, $aa, $23, $00
  .byte $3c, $67, $b3, $01, $cc, $63, $db, $71, $df, $73, $fc, $00, $4f, $b7, $ca, $7a
  .byte $c5, $31, $ec, $54, $3c, $dc, $5d, $4c, $0f, $b3, $47, $63, $6b, $f1, $8c, $0a
  .byte $39, $f1, $ec, $03, $f0, $33, $0f, $e2, $29, $73, $49, $61, $58, $62, $67, $73
  .byte $85, $65, $94, $66, $a3, $77, $ad, $4d, $4d, $c1, $6f, $26, $5d, $c7, $fd

;level 6-3
L_GroundArea15:
  .byte $50, $11, $0f, $26, $af, $32, $d8, $62, $de, $10, $08, $e4, $5a, $62, $6c, $4c
  .byte $86, $43, $ad, $48, $3a, $e2, $53, $42, $88, $64, $9c, $36, $08, $e4, $4a, $62
  .byte $5c, $4d, $3a, $e2, $9c, $32, $fc, $41, $3c, $b1, $83, $00, $ac, $42, $2a, $e2
  .byte $3c, $46, $aa, $62, $bc, $4e, $c6, $43, $46, $c3, $aa, $62, $bd, $48, $0b, $96
  .byte $47, $07, $c7, $12, $3c, $c2, $9c, $41, $cd, $48, $dc, $32, $4c, $c2, $bc, $32
  .byte $1c, $b1, $5a, $62, $6c, $44, $76, $43, $ba, $62, $dc, $32, $5d, $ca, $73, $12
  .byte $e3, $12, $8e, $91, $9d, $41, $be, $42, $ef, $20, $cd, $c7, $fd

;level 7-1
L_GroundArea16:
  .byte $52, $b1, $0f, $20, $6e, $76, $03, $b1, $09, $71, $0f, $71, $6f, $33, $a7, $63
  .byte $b7, $34, $bc, $0e, $4d, $cc, $03, $a6, $08, $72, $3f, $72, $6d, $4c, $73, $07
  .byte $77, $73, $83, $27, $ac, $00, $bf, $73, $3c, $80, $9a, $30, $ac, $5b, $c6, $3c
  .byte $6a, $b0, $75, $10, $96, $74, $b6, $0a, $da, $30, $e3, $28, $ec, $5b, $ed, $48
  .byte $aa, $b0, $33, $b4, $51, $79, $ad, $4a, $dd, $4d, $e3, $2c, $0c, $fa, $73, $07
  .byte $b3, $04, $cb, $71, $ec, $07, $0d, $0a, $39, $71, $df, $33, $ca, $b0, $d6, $10
  .byte $d7, $30, $dc, $0c, $03, $b1, $ad, $41, $ef, $26, $ed, $c7, $39, $f1, $0d, $10
  .byte $7d, $4c, $0d, $13, $a8, $11, $aa, $10, $1c, $83, $d7, $7b, $f3, $67, $5d, $cd
  .byte $6d, $47, $fd

;level 7-2
L_GroundArea17:
  .byte $56, $11, $0f, $26, $df, $32, $fe, $11, $0d, $01, $0c, $5f, $03, $80, $0c, $52
  .byte $29, $15, $7c, $5b, $23, $b2, $29, $1f, $31, $79, $1c, $de, $48, $3b, $ed, $4b
  .byte $39, $f1, $cf, $b3, $fe, $10, $37, $8e, $77, $0e, $9e, $11, $a8, $34, $a9, $34
  .byte $aa, $34, $f8, $62, $fe, $10, $37, $b6, $de, $11, $e7, $63, $f8, $62, $09, $e1
  .byte $0e, $10, $47, $36, $b7, $0e, $be, $91, $ca, $32, $ee, $10, $1d, $ca, $7e, $11
  .byte $83, $77, $9e, $10, $1e, $91, $2d, $41, $4f, $26, $4d, $c7, $fd

;level 7-3
L_GroundArea18:
  .byte $57, $11, $0f, $26, $fe, $10, $4b, $92, $59, $0f, $ad, $4c, $d3, $93, $0b, $94
  .byte $29, $0f, $7b, $93, $99, $0f, $0d, $06, $27, $12, $35, $0f, $23, $b1, $57, $75
  .byte $a3, $31, $ab, $71, $f7, $75, $23, $b1, $87, $13, $95, $0f, $0d, $0a, $23, $35
  .byte $38, $13, $55, $00, $9b, $16, $0b, $96, $c7, $75, $3b, $92, $49, $0f, $ad, $4c
  .byte $29, $92, $52, $40, $6c, $15, $6f, $11, $72, $40, $bf, $15, $03, $93, $0a, $13
  .byte $12, $41, $8b, $12, $99, $0f, $0d, $10, $47, $16, $46, $45, $b3, $32, $13, $b1
  .byte $57, $0e, $a7, $0e, $d3, $31, $53, $b1, $a6, $31, $03, $b2, $13, $0e, $8d, $4d
  .byte $ae, $11, $bd, $41, $ee, $52, $0f, $a0, $dd, $47, $fd

;level 8-1
L_GroundArea19:
  .byte $52, $a1, $0f, $20, $6e, $65, $57, $f3, $60, $21, $6f, $62, $ac, $75, $07, $80
  .byte $1c, $76, $87, $01, $9c, $70, $b0, $33, $cf, $66, $57, $e3, $6c, $04, $cd, $4c
  .byte $9a, $b0, $ac, $0c, $83, $b1, $8f, $74, $bd, $4d, $f8, $11, $fa, $10, $83, $87
  .byte $93, $22, $9f, $74, $59, $f1, $89, $61, $a9, $61, $bc, $0c, $67, $a0, $eb, $71
  .byte $77, $87, $7a, $10, $86, $51, $95, $52, $a4, $53, $b6, $04, $b3, $24, $26, $85
  .byte $4a, $10, $53, $23, $5c, $00, $6f, $73, $93, $08, $07, $fb, $2c, $04, $33, $30
  .byte $74, $76, $eb, $71, $57, $8b, $6c, $02, $96, $74, $e3, $30, $0c, $86, $7d, $41
  .byte $bf, $26, $bd, $c7, $fd

;level 8-2
L_GroundArea22:
  .byte $50, $61, $0f, $26, $bb, $f1, $dc, $06, $23, $87, $b5, $71, $b7, $31, $d7, $28
  .byte $06, $c5, $67, $08, $0d, $05, $39, $71, $7c, $00, $9e, $62, $b6, $0b, $e6, $08
  .byte $4e, $e0, $5d, $4c, $59, $0f, $6c, $02, $93, $67, $ac, $56, $ad, $4c, $1f, $b1
  .byte $3c, $01, $98, $0a, $9e, $20, $a8, $21, $f3, $09, $0e, $a1, $27, $20, $3e, $62
  .byte $56, $08, $7d, $4d, $c6, $08, $3e, $e0, $9e, $62, $b6, $08, $1e, $e0, $4c, $00
  .byte $6c, $00, $a7, $7b, $de, $2f, $6d, $c7, $fe, $10, $0b, $93, $5b, $15, $b7, $12
  .byte $03, $91, $ab, $1f, $bd, $41, $ef, $26, $ad, $c7, $fd

;level 8-3
L_GroundArea23:
  .byte $50, $50, $0f, $26, $0b, $1f, $57, $92, $8b, $12, $d2, $14, $4b, $92, $59, $0f
  .byte $0b, $95, $bb, $1f, $be, $52, $58, $e2, $9e, $50, $97, $08, $bb, $1f, $ae, $d2
  .byte $b6, $08, $bb, $1f, $dd, $4a, $f6, $07, $26, $89, $8e, $50, $98, $62, $eb, $11
  .byte $07, $f3, $0b, $1d, $2e, $52, $47, $0a, $ce, $50, $eb, $1f, $ee, $52, $5e, $d0
  .byte $d9, $0f, $ab, $9f, $be, $52, $8e, $d0, $ab, $1d, $ae, $52, $36, $8b, $56, $08
  .byte $5e, $50, $dc, $15, $df, $12, $2f, $95, $c3, $31, $5b, $9f, $6d, $41, $8e, $52
  .byte $af, $20, $ad, $c7
;another unused area
L_GroundArea24:
  .byte $fd

;cloud level used with level 5-1
L_GroundArea29:
  .byte $00, $c1, $4c, $00, $f3, $4f, $fa, $c6, $68, $a0, $69, $20, $6a, $20, $7a, $47
  .byte $f8, $20, $f9, $20, $fa, $20, $0a, $cf, $b4, $49, $55, $a0, $56, $20, $73, $47
  .byte $f5, $20, $f6, $20, $22, $a1, $41, $48, $52, $20, $72, $20, $92, $20, $b2, $20
  .byte $fe, $00, $9b, $c2, $ad, $c7, $fd

;level 5-2
L_UndergroundArea4:
  .byte $48, $0f, $1e, $01, $27, $06, $5e, $02, $8f, $63, $8c, $01, $ef, $67, $1c, $81
  .byte $2e, $09, $3c, $63, $73, $01, $8c, $60, $fe, $02, $1e, $8e, $3e, $02, $44, $07
  .byte $45, $52, $4e, $0e, $8e, $02, $99, $71, $b5, $24, $b6, $24, $b7, $24, $fe, $02
  .byte $07, $87, $17, $22, $37, $52, $37, $0b, $47, $52, $4e, $0a, $57, $52, $5e, $02
  .byte $67, $52, $77, $52, $7e, $0a, $87, $52, $8e, $02, $96, $46, $97, $52, $a7, $52
  .byte $b7, $52, $c7, $52, $d7, $52, $e7, $52, $f7, $52, $fe, $04, $07, $a3, $47, $08
  .byte $57, $26, $c7, $0a, $e9, $71, $17, $a7, $97, $08, $9e, $01, $a0, $24, $c6, $74
  .byte $f0, $0c, $fe, $04, $0c, $80, $6f, $32, $98, $62, $a8, $62, $bc, $00, $c7, $73
  .byte $e7, $73, $fe, $02, $7f, $e7, $8e, $01, $9e, $00, $de, $02, $f7, $0b, $fe, $0e
  .byte $4e, $82, $54, $52, $64, $51, $6e, $00, $74, $09, $9f, $00, $df, $00, $2f, $80
  .byte $4e, $02, $59, $47, $ce, $0a, $07, $f5, $68, $54, $7f, $64, $88, $54, $a8, $54
  .byte $ae, $01, $b8, $52, $bf, $47, $c8, $52, $d8, $52, $e8, $52, $ee, $0f, $4d, $c7
  .byte $0d, $0d, $0e, $02, $68, $7a, $be, $01, $ee, $0f, $6d, $c5, $fd

;underground bonus rooms used with worlds 5-8
L_UndergroundArea5:
  .byte $08, $0f, $0e, $01, $2e, $05, $38, $2c, $3a, $4f, $08, $ac, $c7, $0b, $ce, $01
  .byte $df, $4a, $6d, $c7, $0e, $81, $00, $5a, $2e, $02, $b8, $4f, $cf, $65, $0f, $e5
  .byte $4f, $65, $8f, $65, $df, $4a, $6d, $c7, $0e, $81, $00, $5a, $30, $07, $34, $52
  .byte $3e, $02, $42, $47, $44, $47, $46, $27, $c0, $0b, $c4, $52, $df, $4a, $6d, $c7
  .byte $fd

;level 6-2
L_WaterArea2:
  .byte $41, $01, $27, $d3, $79, $51, $c4, $56, $00, $e2, $03, $53, $0c, $0f, $12, $3b
  .byte $1a, $42, $43, $54, $6d, $49, $83, $53, $99, $53, $c3, $54, $da, $52, $0c, $84
  .byte $09, $53, $53, $64, $63, $31, $67, $34, $86, $41, $8c, $01, $a3, $30, $b3, $64
  .byte $cc, $03, $d9, $42, $5c, $84, $a0, $62, $a8, $62, $b0, $62, $b8, $62, $c0, $62
  .byte $c8, $62, $d0, $62, $d8, $62, $e0, $62, $e8, $62, $16, $c2, $58, $52, $8c, $04
  .byte $a7, $55, $d0, $63, $d7, $65, $e2, $61, $e7, $65, $f2, $61, $f7, $65, $13, $b8
  .byte $17, $38, $8c, $03, $1d, $c9, $50, $62, $5c, $0b, $62, $3e, $63, $52, $8a, $52
  .byte $93, $54, $aa, $42, $d3, $51, $ea, $41, $03, $d3, $1c, $04, $1a, $52, $33, $55
  .byte $73, $44, $77, $44, $16, $d2, $19, $31, $1a, $32, $5c, $0f, $9a, $47, $95, $64
  .byte $a5, $64, $b5, $64, $c5, $64, $d5, $64, $e5, $64, $f5, $64, $05, $e4, $40, $61
  .byte $42, $35, $56, $34, $5c, $09, $a2, $61, $a6, $61, $b3, $34, $b7, $34, $fc, $08
  .byte $0c, $87, $28, $54, $59, $53, $9a, $30, $a9, $61, $b8, $62, $be, $0b, $d4, $60
  .byte $d5, $0d, $de, $0f, $0d, $ca, $7d, $47, $fd

;water area used in level 8-4
L_WaterArea4:
  .byte $07, $0f, $0e, $02, $39, $73, $05, $8e, $2e, $0b, $b7, $0e, $64, $8e, $6e, $02
  .byte $ce, $06, $de, $0f, $e6, $0d, $7d, $c7, $fd

;water area used in level 6-1
L_WaterArea5:
  .byte $01, $01, $77, $39, $a3, $43, $00, $bf, $29, $51, $39, $48, $61, $55, $d6, $54
  .byte $d2, $44, $0c, $82, $2e, $02, $31, $66, $44, $47, $47, $32, $4a, $47, $97, $32
  .byte $c1, $66, $ce, $01, $dc, $02, $fe, $0e, $0c, $8f, $08, $4f, $fe, $01, $27, $d3
  .byte $5c, $02, $9a, $60, $a9, $61, $b8, $62, $c7, $63, $ce, $0f, $d5, $0d, $7d, $c7
  .byte $fd

;unused bytes
  .byte $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff
  .byte $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff, $ff
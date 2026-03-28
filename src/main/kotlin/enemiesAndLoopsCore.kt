// by Claude - EnemiesAndLoopsCore, ProcessEnemyData, ProcLoopCommand, InitEnemyObject, CheckpointEnemyID
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.areaparser.EnemyObjByte1
import com.ivieleague.smbtranslation.utils.EnemyState
import com.ivieleague.smbtranslation.utils.getEnemyState

// NES Y register tracking for Setup_Vine: on the NES, the Y register at Setup_Vine
// comes from JumpEngine's dispatch mechanism: Y = enemyID * 2 + 2. This is because
// checkpointEnemyID calls JumpEngine which sets Y = A*2 (ASL;TAY) then INY twice.
// For VineObject (ID=$2F): Y = 0x2F*2 + 2 = 0x60. Setup_Vine then reads
// Block_PageLoc,Y / Block_X_Position,Y / Block_Y_Position,Y using this Y value,
// reading from scratch RAM addresses rather than meaningful block data.
// This value is set by checkpointEnemyID before dispatching to setupVine.
private var setupVineBlockY: Int = 0

//> ;loop command data
// SMB1 loop command ROM data tables (11 entries)
//> LoopCmdWorldNumber:
//>       .db $03, $03, $06, $06, $06, $06, $06, $06, $07, $07, $07
private val smb1LoopCmdWorldNumber = intArrayOf(0x03, 0x03, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x07, 0x07, 0x07)

//> LoopCmdPageNumber:
//>       .db $05, $09, $04, $05, $06, $08, $09, $0a, $06, $0b, $10
private val smb1LoopCmdPageNumber = intArrayOf(0x05, 0x09, 0x04, 0x05, 0x06, 0x08, 0x09, 0x0a, 0x06, 0x0b, 0x10)

//> LoopCmdYPosition:
//>       .db $40, $b0, $b0, $80, $40, $40, $80, $40, $f0, $f0, $f0
private val smb1LoopCmdYPosition = intArrayOf(0x40, 0xb0, 0xb0, 0x80, 0x40, 0x40, 0x80, 0x40, 0xf0, 0xf0, 0xf0)

// SMB1 area data offsets for loop-back positions (11 entries)
//> AreaDataOfsLoopback:
//>       .db $12, $36, $0e, $0e, $0e, $32, $32, $32, $0a, $26, $40
private val smb1AreaDataOfsLoopback = intArrayOf(0x12, 0x36, 0x0e, 0x0e, 0x0e, 0x32, 0x32, 0x32, 0x0a, 0x26, 0x40)

// by Claude - ROM data tables for enemy init routines
//> NormalXSpdData:
//>       .db $f8, $f4
private val normalXSpdData = intArrayOf(0xf8, 0xf4)

//> HBroWalkingTimerData:
//>       .db $80, $50
private val hBroWalkingTimerData = intArrayOf(0x80, 0x50)

//> FirebarSpinSpdData:
//>       .db $28, $38, $28, $38, $28
private val firebarSpinSpdData = intArrayOf(0x28, 0x38, 0x28, 0x38, 0x28)

//> FirebarSpinDirData:
//>       .db $00, $00, $10, $10, $00
private val firebarSpinDirData = intArrayOf(0x00, 0x00, 0x10, 0x10, 0x00)

//> PlatPosDataLow:
//>       .db $08, $0c, $f8
private val platPosDataLow = intArrayOf(0x08, 0x0c, 0xf8)

//> PlatPosDataHigh:
//>       .db $00, $00, $ff
private val platPosDataHigh = intArrayOf(0x00, 0x00, 0xff)

//> PRDiffAdjustData:
//>       .db $26, $2c, $32, $38
//>       .db $20, $22, $24, $26
//>       .db $13, $14, $15, $16
private val prDiffAdjustData = intArrayOf(
    0x26, 0x2c, 0x32, 0x38,
    0x20, 0x22, 0x24, 0x26,
    0x13, 0x14, 0x15, 0x16
)

//> FlyCCXPositionData:
//>       .db $80, $30, $40, $80, $30, $50, $50, $70
//>       .db $20, $40, $80, $a0, $70, $40, $90, $68
private val flyCCXPositionData = intArrayOf(
    0x80, 0x30, 0x40, 0x80, 0x30, 0x50, 0x50, 0x70,
    0x20, 0x40, 0x80, 0xa0, 0x70, 0x40, 0x90, 0x68
)

//> FlyCCXSpeedData:
//>       .db $0e, $05, $06, $0e, $1c, $20, $10, $0c, $1e, $22, $18, $14
private val flyCCXSpeedData = intArrayOf(
    0x0e, 0x05, 0x06, 0x0e, 0x1c, 0x20, 0x10, 0x0c, 0x1e, 0x22, 0x18, 0x14
)

//> FlyCCTimerData:
//>       .db $10, $60, $20, $48
private val flyCCTimerData = intArrayOf(0x10, 0x60, 0x20, 0x48)

//> FlameYPosData:
//>       .db $90, $80, $70, $90
private val flameYPosData = intArrayOf(0x90, 0x80, 0x70, 0x90)

//> FlameYMFAdderData:
//>       .db $ff, $01
private val flameYMFAdderData = intArrayOf(0xff, 0x01)

//> Bitmasks:
//>       .db %00000001, %00000010, %00000100, %00001000, %00010000, %00100000, %01000000, %10000000
private val bitmasks = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80)

//> Enemy17YPosData:
//>       .db $40, $30, $90, $50, $20, $60, $a0, $70
private val enemy17YPosData = intArrayOf(0x40, 0x30, 0x90, 0x50, 0x20, 0x60, 0xa0, 0x70)

//> SwimCC_IDData:
//>       .db $0a, $0b
private val swimCCIDData = intArrayOf(0x0a, 0x0b)

/**
 * Main dispatch for enemy processing and level loop commands.
 * Checks Enemy_Flag MSB to decide whether to run enemy objects,
 * process loop commands, or handle Bowser flag linking.
 */
fun System.enemiesAndLoopsCore() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> EnemiesAndLoopsCore:
    //> lda Enemy_Flag,x         ;check data here for MSB set
    val enemyFlag = ram.enemyFlags[x].toInt() and 0xFF

    if (debugEnemyTrace) println("  [ELC] slot=$x flag=${enemyFlag.toString(16)} id=${(ram.enemyID[x].toInt() and 0xFF).toString(16)} state=${(ram.enemyState[x].toInt() and 0xFF).toString(16)} aPTN=${ram.areaParserTaskNum}")

    //> pha                      ;save in stack
    //> asl
    //> bcs ChkBowserF           ;if MSB set in enemy flag, branch ahead of jumps
    if ((enemyFlag and 0x80) != 0) {
        //> ChkBowserF: pla                      ;get data from stack
        //> and #%00001111           ;mask out high nybble
        val linkedIndex = enemyFlag and 0x0F
        //> tay
        //> lda Enemy_Flag,y         ;use as pointer and load same place with different offset
        //> bne ExitELCore
        if (ram.enemyFlags[linkedIndex].toInt() == 0) {
            //> sta Enemy_Flag,x         ;if second enemy flag not set, also clear first one
            ram.enemyFlags[x] = 0
        }
        //> ExitELCore: rts
        return
    }

    //> pla                      ;get from stack
    //> beq ChkAreaTsk           ;if data zero, branch
    if (enemyFlag != 0) {
        //> jmp RunEnemyObjectsCore  ;otherwise, jump to run enemy subroutines
        runEnemyObjectsCore()
        return
    }

    //> ChkAreaTsk: lda AreaParserTaskNum    ;check number of tasks to perform
    //> and #$07
    //> cmp #$07                 ;if at a specific task, jump and leave
    //> beq ExitELCore
    if ((ram.areaParserTaskNum.toInt() and 0x07) == 0x07) {
        return
    }

    //> jmp ProcLoopCommand      ;otherwise, jump to process loop command/load enemies
    procLoopCommand()
}

/**
 * Dispatches to the correct enemy handler based on Enemy_ID.
 * Enemy IDs $00-$14 all go to runNormalEnemies; higher IDs
 * are dispatched via a jump table with $14 subtracted.
 */
fun System.runEnemyObjectsCore() {
    //> RunEnemyObjectsCore:
    //> ldx ObjectOffset  ;get offset for enemy object buffer
    val x = ram.objectOffset.toInt() and 0xFF
    //> lda #$00          ;load value 0 for jump engine by default
    //> ldy Enemy_ID,x
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cpy #$15          ;if enemy object < $15, use default value
    //> bcc JmpEO
    //> tya               ;otherwise subtract $14 from the value and use
    //> sbc #$14          ;as value for jump engine
    val jumpIndex = if (enemyId < EnemyId.BowserFlame.id) 0 else enemyId - 0x14

    //> JmpEO: jsr JumpEngine
    when (jumpIndex) {
        //> .dw RunNormalEnemies  ;for objects $00-$14
        0 -> runNormalEnemies()
        //> .dw RunBowserFlame    ;for object $15
        1 -> runBowserFlame()
        //> .dw RunFireworks       ;for object $16
        2 -> runFireworks()
        //> .dw NoRunCode          ;for objects $17-$1a
        3 -> {} // NoRunCode
        4 -> {} // NoRunCode
        5 -> {} // NoRunCode
        6 -> {} // NoRunCode
        //> .dw RunFirebarObj      ;for objects $1b-$22
        7 -> runFirebarObj()
        8 -> runFirebarObj()
        9 -> runFirebarObj()
        10 -> runFirebarObj()
        11 -> runFirebarObj()
        12 -> runFirebarObj()
        13 -> runFirebarObj()
        14 -> runFirebarObj()
        //> .dw NoRunCode          ;for object $23
        15 -> {} // NoRunCode
        //> .dw RunLargePlatform   ;for objects $24-$2a
        16 -> runLargePlatform()
        17 -> runLargePlatform()
        18 -> runLargePlatform()
        19 -> runLargePlatform()
        20 -> runLargePlatform()
        21 -> runLargePlatform()
        22 -> runLargePlatform()
        //> .dw RunSmallPlatform   ;for objects $2b-$2c
        23 -> runSmallPlatform()
        24 -> runSmallPlatform()
        //> .dw RunBowser          ;for object $2d
        25 -> runBowser()
        //> .dw PowerUpObjHandler  ;for object $2e
        26 -> powerUpObjHandler()
        //> .dw VineObjectHandler  ;for object $2f
        27 -> vineObjectHandler()
        //> .dw NoRunCode          ;for object $30
        28 -> {} // NoRunCode
        //> .dw RunStarFlagObj     ;for object $31
        29 -> runStarFlagObj()
        //> .dw JumpspringHandler  ;for object $32
        30 -> jumpspringHandler()
        //> .dw NoRunCode          ;for object $33
        31 -> {} // NoRunCode
        //> .dw WarpZoneObject     ;for object $34
        32 -> warpZoneObject()
        //> .dw RunRetainerObj     ;for object $35
        33 -> runRetainerObj()
    }
}

/**
 * Handles level loop commands: checks world number, page number, player Y position,
 * and player state. If the player hasn't met the loop conditions, loops the level back.
 * World 7 has a special multi-part loop counter system requiring 3 correct passes.
 *
 * Falls through to ChkEnemyFrenzy and ProcessEnemyData.
 */
private fun System.procLoopCommand() {
    if (variant == GameVariant.SMB2J) {
        procLoopCommandSmb2j()
    } else {
        procLoopCommandSmb1()
    }

    //> ChkEnemyFrenzy:
    chkEnemyFrenzy()
}

/**
 * SMB2J loop command processing: uses per-entry MultiLoopCount table for all worlds.
 * Every entry can be a multi-part loop (not just World 7 as in SMB1).
 */
private fun System.procLoopCommandSmb2j() {
    val data = Smb2jRomData
    //> lda LoopCommand           ;check if loop command was found
    //> beq ChkEnemyFrenzy
    if (ram.loopCommand == 0.toByte()) return
    //> lda CurrentColumnPos      ;check to see if we're still on the first page
    //> bne ChkEnemyFrenzy
    if (ram.currentColumnPos != 0.toUByte()) return

    //> ldy #$0c
    //> FindLoop: dey
    //> bmi ChkEnemyFrenzy
    var matchIndex = -1
    for (y in (data.loopCmdWorldNumber.size - 1) downTo 0) {
        if ((ram.worldNumber.toInt() and 0xFF) != data.loopCmdWorldNumber[y]) continue
        if ((ram.currentPageLoc.toInt() and 0xFF) != data.loopCmdPageNumber[y]) continue
        matchIndex = y
        break
    }
    if (matchIndex < 0) return

    val y = matchIndex
    val playerY = ram.playerYPosition.toInt() and 0xFF
    val correctY = playerY == data.loopCmdYPosition[y]
    val onGround = ram.playerState == PlayerState.OnGround

    if (correctY && onGround) {
        //> inc MultiLoopCorrectCntr
        ram.multiLoopCorrectCntr++
    }
    //> WrongChk: inc MultiLoopPassCntr
    ram.multiLoopPassCntr++
    val requiredCount = data.multiLoopCount[y]
    //> lda MultiLoopPassCntr; cmp MultiLoopCount,y; bne InitLCmd
    if ((ram.multiLoopPassCntr.toInt() and 0xFF) == requiredCount) {
        //> lda MultiLoopCorrectCntr; cmp MultiLoopCount,y; beq InitMLp
        if ((ram.multiLoopCorrectCntr.toInt() and 0xFF) != requiredCount) {
            execGameLoopback(y)
            killAllEnemies()
        }
        //> InitMLp:
        ram.multiLoopPassCntr = 0
        ram.multiLoopCorrectCntr = 0
    }
    //> InitLCmd:
    ram.loopCommand = 0
}

/** SMB1 loop command processing: hardcoded World 7 multi-part check with count of 3. */
private fun System.procLoopCommandSmb1() {
    //> ProcLoopCommand:
    //> lda LoopCommand           ;check if loop command was found
    //> beq ChkEnemyFrenzy
    if (ram.loopCommand != 0.toByte()) {
        //> lda CurrentColumnPos      ;check to see if we're still on the first page
        //> bne ChkEnemyFrenzy        ;if not, do not loop yet
        if (ram.currentColumnPos == 0.toUByte()) {
            //> ldy #$0b                  ;start at the end of each set of loop data
            //> FindLoop: dey
            //> bmi ChkEnemyFrenzy        ;if all data is checked and not match, do not loop
            var matchIndex = -1
            for (y in (smb1LoopCmdWorldNumber.size - 1) downTo 0) {
                //> lda WorldNumber           ;check to see if one of the world numbers
                //> cmp LoopCmdWorldNumber,y  ;matches our current world number
                //> bne FindLoop
                if ((ram.worldNumber.toInt() and 0xFF) != smb1LoopCmdWorldNumber[y]) continue
                //> lda CurrentPageLoc        ;check to see if one of the page numbers
                //> cmp LoopCmdPageNumber,y   ;matches the page we're currently on
                //> bne FindLoop
                if ((ram.currentPageLoc.toInt() and 0xFF) != smb1LoopCmdPageNumber[y]) continue
                matchIndex = y
                break
            }

            if (matchIndex >= 0) {
                val y = matchIndex
                //> lda Player_Y_Position     ;check to see if the player is at the correct position
                //> cmp LoopCmdYPosition,y    ;if not, branch to check for world 7
                //> bne WrongChk
                val playerY = ram.playerYPosition.toInt() and 0xFF
                val correctY = playerY == smb1LoopCmdYPosition[y]
                //> lda Player_State          ;check to see if the player is
                //> cmp #$00                  ;on solid ground (i.e. not jumping or falling)
                //> bne WrongChk              ;if not, player fails to pass loop, and loopback
                val onGround = ram.playerState == PlayerState.OnGround

                if (correctY && onGround) {
                    //> lda WorldNumber           ;are we in world 7?
                    //> cmp #World7               ;(check performed on correct vertical position and on solid ground)
                    //> bne InitMLp               ;if not, initialize flags used there, otherwise
                    if (ram.worldNumber == Constants.World7) {
                        //> inc MultiLoopCorrectCntr  ;increment counter for correct progression
                        ram.multiLoopCorrectCntr++
                        //> IncMLoop: inc MultiLoopPassCntr     ;increment master multi-part counter
                        ram.multiLoopPassCntr++
                        //> lda MultiLoopPassCntr     ;have we done all three parts?
                        //> cmp #$03
                        //> bne InitLCmd              ;if not, skip this part
                        if (ram.multiLoopPassCntr == 3.toByte()) {
                            //> lda MultiLoopCorrectCntr  ;if so, have we done them all correctly?
                            //> cmp #$03
                            //> beq InitMLp               ;if so, branch past unnecessary check here
                            if (ram.multiLoopCorrectCntr != 3.toByte()) {
                                //> bne DoLpBack              ;unconditional branch if previous branch fails
                                execGameLoopback(y)
                                killAllEnemies()
                            }
                            // InitMLp: reset counters regardless
                            ram.multiLoopPassCntr = 0
                            ram.multiLoopCorrectCntr = 0
                        }
                        // else: InitLCmd - just clear the loop command below
                    } else {
                        //> InitMLp:  lda #$00                  ;initialize counters
                        //> sta MultiLoopPassCntr
                        ram.multiLoopPassCntr = 0
                        //> sta MultiLoopCorrectCntr
                        ram.multiLoopCorrectCntr = 0
                    }
                } else {
                    //> WrongChk: lda WorldNumber           ;are we in world 7?
                    //> cmp #World7               ;(check performed on incorrect vertical position or not on solid ground)
                    //> beq IncMLoop
                    if (ram.worldNumber == Constants.World7) {
                        //> IncMLoop: inc MultiLoopPassCntr     ;increment master multi-part counter
                        ram.multiLoopPassCntr++
                        //> lda MultiLoopPassCntr     ;have we done all three parts?
                        //> cmp #$03
                        //> bne InitLCmd              ;if not, skip this part
                        if (ram.multiLoopPassCntr == 3.toByte()) {
                            //> lda MultiLoopCorrectCntr  ;if so, have we done them all correctly?
                            //> cmp #$03
                            //> beq InitMLp               ;if so, branch past unnecessary check here
                            if (ram.multiLoopCorrectCntr == 3.toByte()) {
                                ram.multiLoopPassCntr = 0
                                ram.multiLoopCorrectCntr = 0
                            } else {
                                //> bne DoLpBack
                                execGameLoopback(y)
                                killAllEnemies()
                                ram.multiLoopPassCntr = 0
                                ram.multiLoopCorrectCntr = 0
                            }
                        }
                        // else: InitLCmd - just clear the loop command below
                    } else {
                        //> DoLpBack: jsr ExecGameLoopback      ;if player is not in right place, loop back
                        execGameLoopback(y)
                        //> jsr KillAllEnemies
                        killAllEnemies()
                        //> InitMLp:  lda #$00
                        //> sta MultiLoopCorrectCntr
                        //> sta MultiLoopPassCntr
                        ram.multiLoopPassCntr = 0
                        ram.multiLoopCorrectCntr = 0
                    }
                }
                //> InitLCmd: lda #$00                  ;initialize loop command flag
                //> sta LoopCommand
                ram.loopCommand = 0
            }
        }
    }
}

/**
 * Checks frenzy queue and processes enemy data from the level.
 * If a frenzy enemy is queued, spawns it directly. Otherwise,
 * parses enemy data bytes to determine what to spawn.
 */
private fun System.chkEnemyFrenzy() {
    //> ;$06 - used to hold page location of extended right boundary
    //> ;$07 - used to hold high nybble of position of extended right boundary
    val x = ram.objectOffset.toInt() and 0xFF

    //> ChkEnemyFrenzy:
    //> lda EnemyFrenzyQueue  ;check for enemy object in frenzy queue
    //> beq ProcessEnemyData  ;if not, skip this part
    val frenzyQueue = ram.enemyFrenzyQueue.toInt() and 0xFF
    if (frenzyQueue != 0) {
        //> sta Enemy_ID,x        ;store as enemy object identifier here
        ram.enemyID[x] = frenzyQueue.toByte()
        //> lda #$01
        //> sta Enemy_Flag,x      ;activate enemy object flag
        ram.enemyFlags[x] = 1
        //> lda #$00
        //> sta Enemy_State,x     ;initialize state and frenzy queue
        ram.enemyState[x] = EnemyState.INACTIVE.byte
        //> sta EnemyFrenzyQueue
        ram.enemyFrenzyQueue = 0
        //> jmp InitEnemyObject   ;and then jump to deal with this enemy
        initEnemyObject()
        return
    }

    //> ProcessEnemyData
    if (debugEnemyTrace) println("  [CEF] → processEnemyData offset=${ram.enemyDataOffset} pageSel=${ram.enemyObjectPageSel} pageLoc=${ram.enemyObjectPageLoc}")
    processEnemyData()
    if (debugEnemyTrace) println("  [CEF] ← processEnemyData offset=${ram.enemyDataOffset} pageSel=${ram.enemyObjectPageSel} pageLoc=${ram.enemyObjectPageLoc} id=${(ram.enemyID[x].toInt() and 0xFF).toString(16)}")
}

/**
 * Resets page locations back four pages when the player loops back in a level.
 * Reinitializes enemy data offset, page selects, and sets area data offset
 * from the loopback table.
 */
private fun System.execGameLoopback(loopDataIndex: Int) {
    //> ExecGameLoopback:
    //> lda Player_PageLoc        ;send player back four pages
    //> sec
    //> sbc #$04
    //> sta Player_PageLoc
    ram.playerPageLoc = (ram.playerPageLoc - 4).toByte()
    //> lda CurrentPageLoc        ;send current page back four pages
    //> sec
    //> sbc #$04
    //> sta CurrentPageLoc
    ram.currentPageLoc = ((ram.currentPageLoc.toInt() and 0xFF) - 4).toUByte()
    //> lda ScreenLeft_PageLoc    ;subtract four from page location
    //> sec                       ;of screen's left border
    //> sbc #$04
    //> sta ScreenLeft_PageLoc
    ram.screenLeftPageLoc = (ram.screenLeftPageLoc - 4).toByte()
    //> lda ScreenRight_PageLoc   ;do the same for the page location
    //> sec                       ;of screen's right border
    //> sbc #$04
    //> sta ScreenRight_PageLoc
    ram.screenRightPageLoc = (ram.screenRightPageLoc - 4).toByte()
    //> lda AreaObjectPageLoc     ;subtract four from page control
    //> sec                       ;for area objects
    //> sbc #$04
    //> sta AreaObjectPageLoc
    ram.areaObjectPageLoc = (ram.areaObjectPageLoc - 4).toByte()
    //> lda #$00                  ;initialize page select for both
    //> sta EnemyObjectPageSel    ;area and enemy objects
    ram.enemyObjectPageSel = 0
    //> sta AreaObjectPageSel
    ram.areaObjectPageSel = 0
    //> sta EnemyDataOffset       ;initialize enemy object data offset
    ram.enemyDataOffset = 0
    //> sta EnemyObjectPageLoc    ;and enemy object page control
    ram.enemyObjectPageLoc = 0
    //> lda AreaDataOfsLoopback,y ;adjust area object offset based on
    //> sta AreaDataOffset        ;which loop command we encountered
    val loopbackTable = if (variant == GameVariant.SMB2J) Smb2jRomData.areaDataOfsLoopback else smb1AreaDataOfsLoopback
    ram.areaDataOffset = loopbackTable[loopDataIndex].toByte()
}

/**
 * Parses enemy data from the level's enemy data stream.
 * Handles special row values ($0e for area pointer, $0f for page control),
 * hard mode filtering, buzzy beetle mutation, and enemy group objects.
 * Positions enemies relative to the screen and spawns them into the object buffer.
 */
private fun System.processEnemyData() {
    // by Claude - enemyDataBytes is the ROM data pointed to by the (EnemyData) indirect pointer.
    // Set by getAreaDataAddrs() via RomData lookup tables.
    // NES always reads from (EnemyData),y regardless of pointer validity.
    // If null (not yet initialized), treat as exhausted data (FF marker) → checkFrenzyBuffer.
    val enemyDataBytes = ram.enemyDataBytes
    if (enemyDataBytes == null) {
        checkFrenzyBuffer()
        return
    }
    val x = ram.objectOffset.toInt() and 0xFF

    //> ProcessEnemyData:
    //> ldy EnemyDataOffset      ;get offset of enemy object data
    var y = ram.enemyDataOffset.toInt() and 0xFF
    //> lda (EnemyData),y        ;load first byte
    val enemyByte1 = EnemyObjByte1(enemyDataBytes.getOrElse(y) { 0xFF.toByte() })
    if (debugEnemyTrace) println("  [PED] slot=$x offset=$y firstByte=${enemyByte1.rawInt.toString(16)} pageSel=${ram.enemyObjectPageSel} pageLoc=${ram.enemyObjectPageLoc}")
    //> cmp #$ff                 ;check for EOD terminator
    //> bne CheckEndofBuffer
    if (enemyByte1.isEndOfData) {
        //> jmp CheckFrenzyBuffer    ;if found, jump to check frenzy buffer
        checkFrenzyBuffer()
        return
    }

    //> CheckEndofBuffer:
    //> and #%00001111           ;check for special row $0e
    //> cmp #$0e
    //> beq CheckRightBounds     ;if found, branch
    if (!enemyByte1.isAreaPointerChange) {
        //> cpx #$05                 ;check for end of buffer
        //> bcc CheckRightBounds     ;if not at end of buffer, branch
        if (x >= 0x05) {
            //> iny
            //> lda (EnemyData),y        ;check for specific value here
            //> and #%00111111           ;not sure what this was intended for, exactly
            //> cmp #$2e                 ;this part is quite possibly residual code
            //> beq CheckRightBounds     ;but it has the effect of keeping enemies out of
            //> rts                      ;the sixth slot
            val nextByte = enemyDataBytes.getOrElse(y + 1) { 0.toByte() }.toInt() and 0x3F
            if (nextByte != 0x2E) return
        }
    }

    //> CheckRightBounds:
    //> lda ScreenRight_X_Pos    ;add 48 to pixel coordinate of right boundary
    //> clc
    //> adc #$30
    val rightXExtended = (ram.screenRightXPos.toInt() and 0xFF) + 0x30
    //> and #%11110000           ;store high nybble
    val extBoundHighNybble = rightXExtended and 0xF0
    //> lda ScreenRight_PageLoc  ;add carry to page location of right boundary
    //> adc #$00
    val extBoundPageLoc = ((ram.screenRightPageLoc.toInt() and 0xFF) + (if (rightXExtended > 0xFF) 1 else 0)) and 0xFF

    //> ldy EnemyDataOffset
    //> iny
    y = (ram.enemyDataOffset.toInt() and 0xFF) + 1
    //> lda (EnemyData),y        ;if MSB of enemy object is clear, branch to check for row $0f
    val secondByte = enemyDataBytes.getOrElse(y) { 0.toByte() }.toInt() and 0xFF
    //> asl
    //> bcc CheckPageCtrlRow
    if ((secondByte and 0x80) != 0) {
        //> lda EnemyObjectPageSel   ;if page select already set, do not set again
        //> bne CheckPageCtrlRow
        if (ram.enemyObjectPageSel == 0.toByte()) {
            //> inc EnemyObjectPageSel   ;otherwise, if MSB is set, set page select
            ram.enemyObjectPageSel++
            //> inc EnemyObjectPageLoc   ;and increment page control
            ram.enemyObjectPageLoc++
        }
    }

    //> CheckPageCtrlRow:
    //> dey
    y--
    //> lda (EnemyData),y        ;reread first byte
    val rereadByte1 = EnemyObjByte1(enemyDataBytes.getOrElse(y) { 0.toByte() })
    //> and #$0f
    //> cmp #$0f                 ;check for special row $0f
    //> bne PositionEnemyObj     ;if not found, branch to position enemy object
    if (rereadByte1.isPageControl) {
        //> lda EnemyObjectPageSel   ;if page select set,
        //> bne PositionEnemyObj     ;branch without reading second byte
        if (ram.enemyObjectPageSel == 0.toByte()) {
            //> iny
            //> lda (EnemyData),y        ;otherwise, get second byte, mask out 2 MSB
            //> and #%00111111
            val pageCtrl = enemyDataBytes.getOrElse(y + 1) { 0.toByte() }.toInt() and 0x3F
            //> sta EnemyObjectPageLoc   ;store as page control for enemy object data
            ram.enemyObjectPageLoc = pageCtrl.toByte()
            //> inc EnemyDataOffset      ;increment enemy object data offset 2 bytes
            //> inc EnemyDataOffset
            ram.enemyDataOffset = (ram.enemyDataOffset + 2).toByte()
            //> inc EnemyObjectPageSel   ;set page select for enemy object data and
            ram.enemyObjectPageSel++
            //> jmp ProcLoopCommand      ;jump back to process loop commands again
            procLoopCommand()
            return
        }
        // fall through to PositionEnemyObj if page select was already set
    }

    //> PositionEnemyObj:
    //> lda EnemyObjectPageLoc   ;store page control as page location
    //> sta Enemy_PageLoc,x      ;for enemy object
    ram.sprObjPageLoc[1 + x] = ram.enemyObjectPageLoc

    //> lda (EnemyData),y        ;get first byte of enemy object
    val positionByte1 = EnemyObjByte1(enemyDataBytes.getOrElse(y) { 0.toByte() })
    //> and #%11110000
    val columnPos = positionByte1.columnBits
    //> sta Enemy_X_Position,x   ;store column position
    ram.sprObjXPos[1 + x] = columnPos.toByte()

    //> cmp ScreenRight_X_Pos    ;check column position against right boundary
    //> lda Enemy_PageLoc,x      ;without subtracting, then subtract borrow
    //> sbc ScreenRight_PageLoc  ;from page location
    val screenRightX = ram.screenRightXPos.toInt() and 0xFF
    val borrow = if (columnPos >= screenRightX) 1 else 0
    val pageDiff = (ram.sprObjPageLoc[1 + x].toInt() and 0xFF) - (ram.screenRightPageLoc.toInt() and 0xFF) - (1 - borrow)

    //> bcs CheckRightExtBounds  ;if enemy object beyond or at boundary, branch
    if (debugEnemyTrace) println("  [PED] boundary: colPos=${columnPos.toString(16)} pageLoc=${(ram.sprObjPageLoc[1+x].toInt() and 0xFF).toString(16)} scrRX=${screenRightX.toString(16)} scrRPg=${(ram.screenRightPageLoc.toInt() and 0xFF).toString(16)} pageDiff=$pageDiff extHigh=${extBoundHighNybble.toString(16)} extPg=${extBoundPageLoc.toString(16)}")
    if (pageDiff < 0) {
        if (debugEnemyTrace) println("  [PED] → behind screen, checkThreeBytes/parseRow0e")
        //> lda (EnemyData),y
        //> and #%00001111           ;check for special row $0e
        //> cmp #$0e                 ;if found, jump elsewhere
        //> beq ParseRow0e
        if (positionByte1.isAreaPointerChange) {
            parseRow0e(y)
        } else {
            //> jmp CheckThreeBytes      ;if not found, unconditional jump
            checkThreeBytes()
        }
        // NES Inc2B/Inc3B ends with RTS (no loop) — single-pass is correct
        return
    }

    //> CheckRightExtBounds:
    //> lda $07                  ;check right boundary + 48 against
    //> cmp Enemy_X_Position,x   ;column position without subtracting,
    //> lda $06                  ;then subtract borrow from page control temp
    //> sbc Enemy_PageLoc,x      ;plus carry
    val extBorrow = if (extBoundHighNybble >= columnPos) 1 else 0
    val extPageDiff = extBoundPageLoc - (ram.sprObjPageLoc[1 + x].toInt() and 0xFF) - (1 - extBorrow)

    //> bcc CheckFrenzyBuffer    ;if enemy object beyond extended boundary, branch
    if (extPageDiff < 0) {
        if (debugEnemyTrace) println("  [PED] → beyond ext boundary, checkFrenzyBuffer")
        checkFrenzyBuffer()
        return
    }
    if (debugEnemyTrace) println("  [PED] → SPAWNING enemy type=${(enemyDataBytes.getOrElse(y+1) { 0.toByte() }.toInt() and 0x3F).toString(16)}")

    //> lda #$01                 ;store value in vertical high byte
    //> sta Enemy_Y_HighPos,x
    ram.sprObjYHighPos[1 + x] = 1

    //> lda (EnemyData),y        ;get first byte again
    //> asl                      ;multiply by four to get the vertical
    //> asl                      ;coordinate
    //> asl
    //> asl
    val verticalCoord = (positionByte1.row shl 4) and 0xFF
    //> sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = verticalCoord.toByte()

    //> cmp #$e0                 ;do one last check for special row $0e
    //> beq ParseRow0e           ;(necessary if branched to $c1cb)
    if (verticalCoord == 0xE0) {
        parseRow0e(y)
        return
    }

    //> iny
    //> lda (EnemyData),y        ;get second byte of object
    val secondDataByte = enemyDataBytes.getOrElse(y + 1) { 0.toByte() }.toInt() and 0xFF
    //> and #%01000000           ;check to see if hard mode bit is set
    //> beq CheckForEnemyGroup   ;if not, branch to check for group enemy objects
    if ((secondDataByte and 0x40) != 0) {
        //> lda SecondaryHardMode    ;if set, check to see if secondary hard mode flag
        //> beq Inc2B                ;is on, and if not, branch to skip this object completely
        if (ram.secondaryHardMode == 0.toByte()) {
            advanceEnemyDataOffset(positionByte1)
            return
        }
    }

    //> CheckForEnemyGroup:
    //> lda (EnemyData),y      ;get second byte and mask out 2 MSB
    //> and #%00111111
    val enemyType = secondDataByte and 0x3F

    //> cmp #$37               ;check for value below $37
    //> bcc BuzzyBeetleMutate
    if (enemyType in 0x37 until 0x3F) {
        //> cmp #$3f               ;if $37 or greater, check for value
        //> bcc DoGroup            ;below $3f, branch if below $3f
        //> DoGroup: jmp HandleGroupEnemies   ;handle enemy group objects
        // HandleGroupEnemies ends with jmp Inc2B → jmp ProcLoopCommand
        handleGroupEnemies(enemyType)
        advanceEnemyDataOffset(positionByte1)
        return
    }

    //> BuzzyBeetleMutate:
    var finalEnemyId = enemyType
    //> cmp #Goomba          ;if below $37, check for goomba
    //> bne StrID            ;value ($3f or more always fails)
    if (finalEnemyId == (EnemyId.Goomba.id)) {
        //> ldy PrimaryHardMode  ;check if primary hard mode flag is set
        //> beq StrID            ;and if so, change goomba to buzzy beetle
        if (ram.primaryHardMode) {
            //> lda #BuzzyBeetle
            finalEnemyId = EnemyId.BuzzyBeetle.id  // BuzzyBeetle = $02
        }
    }

    //> StrID:  sta Enemy_ID,x       ;store enemy object number into buffer
    ram.enemyID[x] = finalEnemyId.toByte()
    //> lda #$01
    //> sta Enemy_Flag,x     ;set flag for enemy in buffer
    ram.enemyFlags[x] = 1
    //> jsr InitEnemyObject
    // NES Y register = enemyDataOffset + 1 here (after iny at line 610/asm 7974)
    setupVineBlockY = (ram.enemyDataOffset.toInt() and 0xFF) + 1
    initEnemyObject()
    //> lda Enemy_Flag,x     ;check to see if flag is set
    //> bne Inc2B            ;if not, leave, otherwise branch
    if (ram.enemyFlags[x].toInt() != 0) {
        advanceEnemyDataOffset(positionByte1)
    }
    //> rts
}

/**
 * Handles special row $0e in enemy data: sets area pointer and entrance page
 * for world-specific area transitions (e.g., underground bonus areas).
 */
private fun System.parseRow0e(dataY: Int) {
    val enemyDataBytes = ram.enemyDataBytes ?: return  // by Claude

    //> ParseRow0e:
    //> iny                      ;increment Y to load third byte of object
    //> iny
    val thirdByteIdx = dataY + 2
    //> lda (EnemyData),y
    val thirdByte = enemyDataBytes.getOrElse(thirdByteIdx) { 0.toByte() }.toInt() and 0xFF
    //> lsr                      ;move 3 MSB to the bottom, effectively
    //> lsr                      ;making %xxx00000 into %00000xxx
    //> lsr
    //> lsr
    //> lsr
    val worldNum = thirdByte ushr 5
    //> (SMB2J) lda WorldNumber; cmp #World9; beq W9Skip
    //> cmp WorldNumber          ;is it the same world number as we're on?
    //> bne NotUse               ;if not, do not use
    val worldMatch = (variant == GameVariant.SMB2J && ram.worldNumber == Constants.World9) ||
            worldNum == (ram.worldNumber.toInt() and 0xFF)
    if (worldMatch) {
        //> dey
        //> lda (EnemyData),y        ;otherwise, get second byte and use as offset
        //> sta AreaPointer          ;to addresses for level and enemy object data
        ram.areaPointer = enemyDataBytes.getOrElse(thirdByteIdx - 1) { 0.toByte() }
        //> iny
        //> lda (EnemyData),y        ;get third byte again, and this time mask out
        //> and #%00011111           ;the 3 MSB from before, save as page number to be
        //> sta EntrancePage         ;used upon entry to area, if area is entered
        ram.entrancePage = (thirdByte and 0x1F).toByte()
    }
    //> NotUse: jmp Inc3B
    //> Inc3B:  inc EnemyDataOffset      ;if row = $0e, increment three bytes
    ram.enemyDataOffset = (ram.enemyDataOffset + 3).toByte()
    //> (falls through from Inc3B to the rest of Inc2B logic)
    ram.enemyObjectPageSel = 0
    // reload X from ObjectOffset (assembly does ldx ObjectOffset here)
}

/**
 * Checks if the first byte of enemy data at current offset has row $0e,
 * and advances the offset by 2 or 3 bytes accordingly.
 */
private fun System.checkThreeBytes() {
    val enemyDataBytes = ram.enemyDataBytes ?: return  // by Claude

    //> CheckThreeBytes:
    //> ldy EnemyDataOffset      ;load current offset for enemy object data
    val y = ram.enemyDataOffset.toInt() and 0xFF
    //> lda (EnemyData),y        ;get first byte
    val firstByte = enemyDataBytes.getOrElse(y) { 0.toByte() }.toInt() and 0xFF
    //> and #%00001111           ;check for special row $0e
    //> cmp #$0e
    //> bne Inc2B
    if ((firstByte and 0x0F) == 0x0E) {
        //> Inc3B:  inc EnemyDataOffset      ;if row = $0e, increment three bytes
        ram.enemyDataOffset++
    }
    //> Inc2B:  inc EnemyDataOffset      ;otherwise increment two bytes
    //> inc EnemyDataOffset
    ram.enemyDataOffset = (ram.enemyDataOffset + 2).toByte()
    //> lda #$00                 ;init page select for enemy objects
    //> sta EnemyObjectPageSel
    ram.enemyObjectPageSel = 0
    //> ldx ObjectOffset         ;reload current offset in enemy buffers
    //> rts
}

/**
 * Advances the enemy data offset by 2 or 3 bytes depending on whether
 * the first byte has special row $0e, and resets page select.
 */
private fun System.advanceEnemyDataOffset(byte1: EnemyObjByte1) {
    //> CheckThreeBytes / Inc2B / Inc3B combined
    if (byte1.isAreaPointerChange) {
        ram.enemyDataOffset++
    }
    ram.enemyDataOffset = (ram.enemyDataOffset + 2).toByte()
    ram.enemyObjectPageSel = 0
}

/**
 * Checks frenzy buffer and vine flag offset when enemy data is exhausted (EOD found).
 * Spawns a frenzy enemy or vine if conditions are met.
 */
private fun System.checkFrenzyBuffer() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> CheckFrenzyBuffer:
    //> lda EnemyFrenzyBuffer    ;if enemy object stored in frenzy buffer
    //> bne StrFre               ;then branch ahead to store in enemy object buffer
    val frenzyBuffer = ram.enemyFrenzyBuffer.toInt() and 0xFF
    if (debugEnemyTrace) println("  [CFB] slot=$x frenzyBuffer=${frenzyBuffer.toString(16)} vineFlagOfs=${ram.vineFlagOffset.toInt() and 0xFF}")
    if (frenzyBuffer != 0) {
        //> StrFre: sta Enemy_ID,x           ;store contents of frenzy buffer into enemy identifier value
        ram.enemyID[x] = frenzyBuffer.toByte()
        if (debugEnemyTrace) println("  [CFB] → initEnemyObject with id=${frenzyBuffer.toString(16)}")
        // NES Y register = enemyDataOffset here (no iny on frenzy path)
        setupVineBlockY = ram.enemyDataOffset.toInt() and 0xFF
        initEnemyObject()
        return
    }

    //> lda VineFlagOffset       ;otherwise check vine flag offset
    //> cmp #$01
    //> bne ExEPar               ;if other value <> 1, leave
    if ((ram.vineFlagOffset.toInt() and 0xFF) != 0x01) {
        //> ExEPar: rts
        return
    }

    //> lda #VineObject          ;otherwise put vine in enemy identifier
    //> StrFre: sta Enemy_ID,x
    ram.enemyID[x] = EnemyId.VineObject.byte
    //> (falls through to InitEnemyObject)
    // NES Y register = enemyDataOffset here (no iny on vine/frenzy path)
    setupVineBlockY = ram.enemyDataOffset.toInt() and 0xFF
    initEnemyObject()
}

/**
 * Initializes enemy state to 0 and dispatches to the enemy-type-specific
 * init routine via checkpointEnemyID.
 */
fun System.initEnemyObject() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitEnemyObject:
    //> lda #$00                 ;initialize enemy state
    //> sta Enemy_State,x
    ram.enemyState[x] = EnemyState.INACTIVE.byte
    //> jsr CheckpointEnemyID    ;jump ahead to run jump engine and subroutines
    checkpointEnemyID()
    //> ExEPar: rts                      ;then leave
}

/**
 * Dispatches to enemy-type-specific initialization routines based on Enemy_ID.
 * For enemy IDs < $15, adds 8 pixels to Y position and sets offscreen masked bit.
 * Then dispatches via a jump engine table to the appropriate init subroutine.
 */
fun System.checkpointEnemyID() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> CheckpointEnemyID:
    //> lda Enemy_ID,x
    val enemyId = ram.enemyID[x].toInt() and 0xFF

    //> cmp #$15                     ;check enemy object identifier for $15 or greater
    //> bcs InitEnemyRoutines        ;and branch straight to the jump engine if found
    if (enemyId < EnemyId.BowserFlame.id) {
        //> tay                          ;save identifier in Y register for now
        //> lda Enemy_Y_Position,x
        //> adc #$08                     ;add eight pixels to what will eventually be the
        //> sta Enemy_Y_Position,x       ;enemy object's vertical coordinate ($00-$14 only)
        val curY = ram.sprObjYPos[1 + x].toInt() and 0xFF
        ram.sprObjYPos[1 + x] = ((curY + 0x08) and 0xFF).toByte()
        //> lda #$01
        //> sta EnemyOffscrBitsMasked,x  ;set offscreen masked bit
        ram.enemyOffscrBitsMaskeds[x] = 1
        //> tya                          ;get identifier back and use as offset for jump engine
    }

    //> InitEnemyRoutines:
    //> ;jump engine table for newly loaded enemy objects
    //> NoInitCode:
    //> jsr JumpEngine
    // JumpEngine sets Y = A*2 + 2 (ASL;TAY;INY;INY), which Setup_Vine uses to read block data
    setupVineBlockY = enemyId * 2 + 2
    when (enemyId) {
        //> .dw InitNormalEnemy  ;for objects $00-$02
        EnemyId.GreenKoopa.id -> initNormalEnemy()
        EnemyId.RedKoopaShell.id -> initNormalEnemy()
        EnemyId.BuzzyBeetle.id -> initNormalEnemy()
        //> .dw InitRedKoopa     ;for object $03
        EnemyId.RedKoopa.id -> initRedKoopa()
        //> .dw NoInitCode        ;for object $04 (SMB1: GreenKoopaVar)
        //> .dw InitPiranhaPlant  ;for object $04 (SMB2J: UpsideDownPiranhaP)
        EnemyId.GreenKoopaVar.id -> if (variant == GameVariant.SMB2J) initPiranhaPlant() else {}
        //> .dw InitHammerBro     ;for object $05
        EnemyId.HammerBro.id -> initHammerBro()
        //> .dw InitGoomba        ;for object $06
        EnemyId.Goomba.id -> initGoomba()
        //> .dw InitBloober       ;for object $07
        EnemyId.Bloober.id -> initBloober()
        //> .dw InitBulletBill    ;for object $08
        EnemyId.BulletBillFrenzyVar.id -> initBulletBill()
        //> .dw NoInitCode        ;for object $09
        EnemyId.TallEnemy.id -> {} // NoInitCode
        //> .dw InitCheepCheep    ;for objects $0a-$0b
        EnemyId.GreyCheepCheep.id -> initCheepCheep()
        EnemyId.RedCheepCheep.id -> initCheepCheep()
        //> .dw InitPodoboo       ;for object $0c
        EnemyId.Podoboo.id -> initPodoboo()
        //> .dw InitPiranhaPlant  ;for object $0d
        EnemyId.PiranhaPlant.id -> initPiranhaPlant()
        //> .dw InitJumpGPTroopa  ;for object $0e
        EnemyId.GreenParatroopaJump.id -> initJumpGPTroopa()
        //> .dw InitRedPTroopa    ;for object $0f
        EnemyId.RedParatroopa.id -> initRedPTroopa()
        //> .dw InitHorizFlySwimEnemy ;for object $10
        EnemyId.GreenParatroopaFly.id -> initHorizFlySwimEnemy()
        //> .dw InitLakitu        ;for object $11
        EnemyId.Lakitu.id -> initLakitu()
        //> .dw InitEnemyFrenzy   ;for object $12
        EnemyId.Spiny.id -> initEnemyFrenzy()
        //> .dw NoInitCode        ;for object $13
        EnemyId.DummyEnemy.id -> {} // NoInitCode
        //> .dw InitEnemyFrenzy   ;for objects $14-$17
        EnemyId.FlyingCheepCheep.id -> initEnemyFrenzy()
        EnemyId.BowserFlame.id -> initEnemyFrenzy()
        EnemyId.Fireworks.id -> initEnemyFrenzy()
        EnemyId.BBillCCheepFrenzy.id -> initEnemyFrenzy()
        //> .dw EndFrenzy         ;for object $18
        EnemyId.StopFrenzy.id -> endFrenzy()
        //> .dw NoInitCode        ;for objects $19-$1a
        0x19 -> {} // NoInitCode
        0x1A -> {} // NoInitCode
        //> .dw InitShortFirebar  ;for objects $1b-$1e
        0x1B -> initShortFirebar()
        0x1C -> initShortFirebar()
        0x1D -> initShortFirebar()
        0x1E -> initShortFirebar()
        //> .dw InitLongFirebar   ;for object $1f
        0x1F -> initLongFirebar()
        //> .dw NoInitCode        ;for objects $20-$23
        0x20 -> {} // NoInitCode
        0x21 -> {} // NoInitCode
        0x22 -> {} // NoInitCode
        0x23 -> {} // NoInitCode
        //> .dw InitBalPlatform   ;for object $24
        0x24 -> initBalPlatform()
        //> .dw InitVertPlatform  ;for object $25
        0x25 -> initVertPlatform()
        //> .dw LargeLiftUp       ;for object $26
        0x26 -> largeLiftUp()
        //> .dw LargeLiftDown     ;for object $27
        0x27 -> largeLiftDown()
        //> .dw InitHoriPlatform  ;for object $28
        0x28 -> initHoriPlatform()
        //> .dw InitDropPlatform  ;for object $29
        0x29 -> initDropPlatform()
        //> .dw InitHoriPlatform  ;for object $2a
        0x2A -> initHoriPlatform()
        //> .dw PlatLiftUp        ;for object $2b
        0x2B -> platLiftUp()
        //> .dw PlatLiftDown      ;for object $2c
        0x2C -> platLiftDown()
        //> .dw InitBowser        ;for object $2d
        EnemyId.Bowser.id -> initBowser()
        //> .dw PwrUpJmp          ;for object $2e (possibly dummy value)
        EnemyId.PowerUpObject.id -> pwrUpJmp()
        //> .dw Setup_Vine        ;for object $2f
        EnemyId.VineObject.id -> setupVine()
        //> .dw NoInitCode        ;for objects $30-$34
        EnemyId.FlagpoleFlagObject.id -> {} // NoInitCode
        EnemyId.StarFlagObject.id -> {} // NoInitCode
        EnemyId.JumpspringObject.id -> {} // NoInitCode
        EnemyId.BulletBillCannonVar.id -> {} // NoInitCode
        0x34 -> {} // NoInitCode
        //> .dw InitRetainerObj   ;for object $35
        EnemyId.RetainerObject.id -> initRetainerObj()
        //> .dw EndOfEnemyInitCode ;for object $36
        0x36 -> endOfEnemyInitCode()
    }
}

// by Claude - Run handler stubs moved to dedicated files:
// runNormalEnemies, runBowserFlame, runFireworks, runFirebarObj,
// runStarFlagObj, jumpspringHandler, runRetainerObj → enemyBehaviors.kt
// runLargePlatform, runSmallPlatform → platformRoutines.kt
// runBowser → bowserRoutine.kt
// powerUpObjHandler, vineObjectHandler → powerUpVine.kt
// warpZoneObject → enemyBGCollision.kt

// --- Enemy init routines (dispatched from CheckpointEnemyID) ---
// by Claude - translated from smbdism.asm InitNormalEnemy through EndOfEnemyInitCode

/**
 * Sets horizontal speed based on hard mode, then falls through to TallBBox.
 * Used by goombas, koopas, and other normal walking enemies ($00-$02).
 */
private fun System.initNormalEnemy() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitNormalEnemy:
    //> ldy #$01              ;load offset of 1 by default
    //> lda PrimaryHardMode   ;check for primary hard mode flag set
    //> bne GetESpd
    //> dey                   ;if not set, decrement offset
    val spdIdx = if (ram.primaryHardMode) 1 else 0

    //> GetESpd: lda NormalXSpdData,y  ;get appropriate horizontal speed
    //> SetESpd: sta Enemy_X_Speed,x   ;store as speed for enemy object
    ram.sprObjXSpeed[1 + x] = normalXSpdData[spdIdx].toByte()

    //> jmp TallBBox          ;branch to set bounding box control and other data
    tallBBox()
}

/**
 * Initializes red koopa: normal enemy init + set enemy state to 1.
 */
private fun System.initRedKoopa() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitRedKoopa:
    //> jsr InitNormalEnemy   ;load appropriate horizontal speed
    initNormalEnemy()

    //> lda #$01              ;set enemy state for red koopa troopa $03
    //> sta Enemy_State,x
    ram.enemyState[x] = EnemyState.NORMAL.byte
}

/**
 * Sets up hammer bro: zero speed, walking timer based on hard mode, bbox $0b.
 */
private fun System.initHammerBro() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitHammerBro:
    //> lda #$00                    ;init horizontal speed and timer used by hammer bro
    //> sta HammerThrowingTimer,x   ;apparently to time hammer throwing
    ram.hammerThrowingTimers[x] = 0 // by Claude - indexed with x

    //> sta Enemy_X_Speed,x
    ram.sprObjXSpeed[1 + x] = 0

    //> ldy SecondaryHardMode       ;get secondary hard mode flag
    //> lda HBroWalkingTimerData,y
    //> sta EnemyIntervalTimer,x    ;set value as delay for hammer bro to walk left
    val hardIdx = if (ram.secondaryHardMode != 0.toByte()) 1 else 0
    ram.timers[0x16 + x] = hBroWalkingTimerData[hardIdx].toByte()

    //> lda #$0b                    ;set specific value for bounding box size control
    //> jmp SetBBox
    setBBox(0x0b)
}

/**
 * Initializes goomba: normal enemy init + small bounding box ($09).
 */
private fun System.initGoomba() {
    //> InitGoomba:
    //> jsr InitNormalEnemy  ;set appropriate horizontal speed
    initNormalEnemy()

    //> jmp SmallBBox        ;set $09 as bounding box control, set other values
    smallBBox()
}

/**
 * Initializes bloober (blooper): zero move speed, small bounding box.
 */
private fun System.initBloober() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitBloober:
    //> lda #$00               ;initialize horizontal speed
    //> sta BlooperMoveSpeed,x
    // by Claude - BlooperMoveSpeed,x = Enemy_X_Speed ($58+x)
    ram.sprObjXSpeed[1 + x] = 0

    //> SmallBBox: lda #$09    ;set specific bounding box size control
    //> bne SetBBox            ;unconditional branch
    smallBBox()
}

/**
 * Initializes bullet bill: moving left, bbox $09 (no speed/force init).
 */
private fun System.initBulletBill() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitBulletBill:
    //> lda #$02                  ;set moving direction for left
    //> sta Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = 2

    //> lda #$09                  ;set bounding box control for $09
    //> sta Enemy_BoundBoxCtrl,x
    ram.enemyBoundBoxCtrls[x] = 0x09
}

/**
 * Initializes cheep-cheep: small bbox, random movement flag, save original Y.
 */
private fun System.initCheepCheep() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitCheepCheep:
    //> jsr SmallBBox              ;set vertical bounding box, speed, init others
    smallBBox()

    //> lda PseudoRandomBitReg,x   ;check one portion of LSFR
    //> and #%00010000             ;get d4 from it
    //> sta CheepCheepMoveMFlag,x  ;save as movement flag of some sort
    val lsfr = ram.pseudoRandomBitReg[x].toInt() and 0xFF
    // by Claude - CheepCheepMoveMFlag,x = Enemy_X_Speed ($58+x)
    ram.sprObjXSpeed[1 + x] = (lsfr and 0x10).toByte()

    //> lda Enemy_Y_Position,x
    //> sta CheepCheepOrigYPos,x   ;save original vertical coordinate here
    // by Claude - CheepCheepOrigYPos,x = Enemy_Y_MoveForce ($434+x)
    ram.sprObjYMoveForce[1 + x] = ram.sprObjYPos[1 + x]
}

/**
 * Initializes podoboo: position below screen, set timer, small bbox.
 */
private fun System.initPodoboo() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitPodoboo:
    //> lda #$02                  ;set enemy position to below
    //> sta Enemy_Y_HighPos,x     ;the bottom of the screen
    ram.sprObjYHighPos[1 + x] = 2

    //> sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = 2

    //> lsr
    //> sta EnemyIntervalTimer,x  ;set timer for enemy (2 >> 1 = 1)
    ram.timers[0x16 + x] = 1

    //> lsr
    //> sta Enemy_State,x         ;initialize enemy state (1 >> 1 = 0)
    ram.enemyState[x] = EnemyState.INACTIVE.byte

    //> jmp SmallBBox             ;$09 as bounding box size and set other things
    smallBBox()
}

/**
 * Initializes piranha plant: set Y speed, store down/up Y positions, bbox $09.
 * SMB2J adds world-dependent patching: worlds 4-8 and A-D get red piranha plants
 * ($22 attribute, $13 range) instead of green ($21 attribute, $21 range).
 * The original assembly self-modifies EnemyAttributeData and ChkPlayerNearPipe.
 */
internal fun System.initPiranhaPlant() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitPiranhaPlant:
    // SMB2J patches EnemyAttributeData[PiranhaPlant] and ChkPlayerNearPipe range based on
    // HardWorldFlag/WorldNumber. These are now computed properties on GameRam (isRedPiranhaPlant,
    // piranhaPlantAttribute, piranhaPlantNearPipeRange) so no explicit patching is needed.

    //> lda #$01                     ;set initial speed
    //> sta PiranhaPlant_Y_Speed,x
    ram.sprObjXSpeed[1 + x] = 1  // PiranhaPlant_Y_Speed,x = Enemy_X_Speed,x ($58+x)

    //> lsr
    //> sta Enemy_State,x            ;initialize enemy state (1 >> 1 = 0)
    ram.enemyState[x] = EnemyState.INACTIVE.byte

    //> sta PiranhaPlant_MoveFlag,x  ;initialize move flag
    ram.sprObjYSpeed[1 + x] = 0  // PiranhaPlant_MoveFlag,x = Enemy_Y_Speed,x ($A0+x)

    //> lda Enemy_Y_Position,x
    //> sta PiranhaPlantDownYPos,x   ;save original vertical coordinate here
    val yPos = ram.sprObjYPos[1 + x].toInt() and 0xFF
    ram.sprObjYMoveForce[1 + x] = yPos.toByte()  // PiranhaPlantDownYPos,x ($434+x)

    //> sec
    //> sbc #$18
    //> sta PiranhaPlantUpYPos,x     ;save original vertical coordinate - 24 pixels here
    ram.sprObjYMFDummy[1 + x] = ((yPos - 0x18) and 0xFF).toByte()  // PiranhaPlantUpYPos,x ($417+x)

    //> lda #$09
    //> jmp SetBBox2                 ;set specific value for bounding box control
    ram.enemyBoundBoxCtrls[x] = 0x09
}

/**
 * Initializes jumping green paratroopa: moving left, speed $f8, bbox $03.
 */
private fun System.initJumpGPTroopa() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitJumpGPTroopa:
    //> lda #$02                  ;set for movement to the left
    //> sta Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = 2

    //> lda #$f8                  ;set horizontal speed
    //> sta Enemy_X_Speed,x
    ram.sprObjXSpeed[1 + x] = 0xf8.toByte()

    //> TallBBox2: lda #$03       ;set specific value for bounding box control
    //> SetBBox2:  sta Enemy_BoundBoxCtrl,x  ;set bounding box control then leave
    ram.enemyBoundBoxCtrls[x] = 0x03
}

/**
 * Initializes red paratroopa: stores original Y and center Y, then tall bbox.
 */
private fun System.initRedPTroopa() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitRedPTroopa:
    //> ldy #$30                    ;load central position adder for 48 pixels down
    //> lda Enemy_Y_Position,x      ;set vertical coordinate into location to
    //> sta RedPTroopaOrigXPos,x    ;be used as original vertical coordinate
    val yPos = ram.sprObjYPos[1 + x].toInt() and 0xFF
    // by Claude - indexed by objectOffset (aliases Enemy_X_MoveForce,x at $0401+x)
    ram.sprObjXMoveForce[1 + x] = yPos.toByte()

    //> bpl GetCent                 ;if vertical coordinate < $80
    //> ldy #$e0                    ;if => $80, load position adder for 32 pixels up
    val centerAdder = if ((yPos and 0x80) != 0) 0xE0 else 0x30

    //> GetCent: tya                ;send central position adder to A
    //> adc Enemy_Y_Position,x      ;add to current vertical coordinate
    //> sta RedPTroopaCenterYPos,x  ;store as central vertical coordinate
    // by Claude - indexed by objectOffset (aliases Enemy_X_Speed,x at $58+x)
    ram.sprObjXSpeed[1 + x] = ((centerAdder + yPos) and 0xFF).toByte()

    //> TallBBox: (falls through)
    tallBBox()
}

/**
 * Initializes horizontally flying/swimming enemy: zero X speed, tall bbox.
 */
private fun System.initHorizFlySwimEnemy() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitHorizFlySwimEnemy:
    //> lda #$00        ;initialize horizontal speed
    //> jmp SetESpd     ;store and go to TallBBox
    ram.sprObjXSpeed[1 + x] = 0
    tallBBox()
}

/**
 * Initializes lakitu: if frenzy buffer active, erase; otherwise set up as flying enemy.
 */
private fun System.initLakitu() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitLakitu:
    //> lda EnemyFrenzyBuffer      ;check to see if an enemy is already in
    //> bne KillLakitu             ;the frenzy buffer, and branch to kill lakitu if so
    if (ram.enemyFrenzyBuffer != 0.toByte()) {
        //> KillLakitu: jmp EraseEnemyObject
        //> ;$01-$03 - used to hold pseudorandom difference adjusters
        eraseEnemyObject(x)
        return
    }

    //> SetupLakitu:
    //> lda #$00                   ;erase counter for lakitu's reappearance
    //> sta LakituReappearTimer
    ram.lakituReappearTimer = 0

    //> jsr InitHorizFlySwimEnemy  ;set $03 as bounding box, set other attributes
    initHorizFlySwimEnemy()

    //> jmp TallBBox2              ;set $03 as bounding box again (not necessary) and leave
    ram.enemyBoundBoxCtrls[x] = 0x03
}

/**
 * Initializes enemy frenzy: stores enemy ID into frenzy buffer, then dispatches
 * to the appropriate frenzy handler via a jump table.
 */
private fun System.initEnemyFrenzy() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitEnemyFrenzy:
    //> lda Enemy_ID,x        ;load enemy identifier
    //> sta EnemyFrenzyBuffer ;save in enemy frenzy buffer
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    ram.enemyFrenzyBuffer = enemyId.toByte()

    //> sec
    //> sbc #$12              ;subtract 18 and use as offset for jump engine
    val offset = enemyId - 0x12

    //> jsr JumpEngine
    //> ;frenzy object jump table
    //> NoFrenzyCode:
    //> .dw LakituAndSpinyHandler   ;$12
    //> .dw NoFrenzyCode            ;$13
    //> .dw InitFlyingCheepCheep    ;$14
    //> .dw InitBowserFlame         ;$15
    //> .dw InitFireworks           ;$16
    //> .dw BulletBillCheepCheep    ;$17
    when (offset) {
        0 -> lakituAndSpinyHandler(x) // LakituAndSpinyHandler
        1 -> {} // NoFrenzyCode
        2 -> initFlyingCheepCheep(x) // InitFlyingCheepCheep
        3 -> initBowserFlame(x) // InitBowserFlame
        4 -> initFireworks(x) // InitFireworks
        5 -> bulletBillCheepCheep(x) // BulletBillCheepCheep
        else -> {} // NoFrenzyCode
    }
}

/**
 * Ends frenzy mode: sets any lakitu enemies to state 1, clears frenzy buffer and flag.
 */
private fun System.endFrenzy() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> EndFrenzy:
    //> ldy #$05               ;start at last slot
    for (y in 5 downTo 0) {
        //> LakituChk: lda Enemy_ID,y         ;check enemy identifiers
        //> cmp #Lakitu            ;for lakitu
        //> bne NextFSlot
        if (ram.enemyID[y] == EnemyId.Lakitu.byte) {
            //> lda #$01               ;if found, set state
            //> sta Enemy_State,y
            ram.enemyState[y] = EnemyState.NORMAL.byte
        }
        //> NextFSlot: dey          ;move onto the next slot
        //> bpl LakituChk           ;do this until all slots are checked
    }

    //> lda #$00
    //> sta EnemyFrenzyBuffer  ;empty enemy frenzy buffer
    ram.enemyFrenzyBuffer = 0

    //> sta Enemy_Flag,x       ;disable enemy buffer flag for this object
    ram.enemyFlags[x] = 0
}

//> FireworksXPosData: .db $00, $30, $60, $60, $00, $20
private val fireworksXPosData = intArrayOf(0x00, 0x30, 0x60, 0x60, 0x00, 0x20)
//> FireworksYPosData: .db $60, $40, $70, $40, $60, $30
private val fireworksYPosData = intArrayOf(0x60, 0x40, 0x70, 0x40, 0x60, 0x30)

/**
 * InitFireworks: spawns firework explosions one at a time, decrementing fireworksCounter each time.
 * Positions each firework relative to the star flag object using lookup tables.
 */
private fun System.initFireworks(x: Int) {
    //> InitFireworks:
    //> lda FrenzyEnemyTimer; bne ExitFWk
    if (ram.frenzyEnemyTimer != 0.toByte()) return

    //> lda #$20; sta FrenzyEnemyTimer
    ram.frenzyEnemyTimer = 0x20
    //> dec FireworksCounter
    ram.fireworksCounter = (ram.fireworksCounter - 1).toByte()

    //> ldy #$06; StarFChk: dey; lda Enemy_ID,y; cmp #StarFlagObject; bne StarFChk
    var starFlagSlot = -1
    for (y in 5 downTo 0) {
        if (ram.enemyID[y] == EnemyId.StarFlagObject.byte) {
            starFlagSlot = y
            break
        }
    }
    if (starFlagSlot < 0) return // safety: no star flag found (NES would infinite-loop)

    //> lda Enemy_X_Position,y; sec; sbc #$30; pha
    val flagX = ram.sprObjXPos[1 + starFlagSlot].toInt() and 0xFF
    val baseX = (flagX - 0x30) and 0xFF
    val borrow = if (flagX < 0x30) 1 else 0
    //> lda Enemy_PageLoc,y; sbc #$00; sta $00
    val basePage = ((ram.sprObjPageLoc[1 + starFlagSlot].toInt() and 0xFF) - borrow) and 0xFF

    //> lda FireworksCounter; clc; adc Enemy_State,y; tay
    val tableIdx = ((ram.fireworksCounter.toInt() and 0xFF) + (ram.enemyState[starFlagSlot].toInt() and 0xFF)) and 0xFF

    //> pla; clc; adc FireworksXPosData,y; sta Enemy_X_Position,x
    val fwXSum = baseX + fireworksXPosData.getOrElse(tableIdx) { 0 }
    ram.sprObjXPos[1 + x] = (fwXSum and 0xFF).toByte()
    //> lda $00; adc #$00; sta Enemy_PageLoc,x
    ram.sprObjPageLoc[1 + x] = ((basePage + (if (fwXSum > 0xFF) 1 else 0)) and 0xFF).toByte()
    //> lda FireworksYPosData,y; sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = fireworksYPosData.getOrElse(tableIdx) { 0x60 }.toByte()
    //> lda #$01; sta Enemy_Y_HighPos,x; sta Enemy_Flag,x
    ram.sprObjYHighPos[1 + x] = 1
    ram.enemyFlags[x] = 1
    //> lsr; sta ExplosionGfxCounter,x  (= sprObjXSpeed[1+x])
    ram.sprObjXSpeed[1 + x] = 0
    //> lda #$08; sta ExplosionTimerCounter,x  (= sprObjYSpeed[1+x])
    ram.sprObjYSpeed[1 + x] = 0x08
}

/**
 * LakituAndSpinyHandler: if lakitu is present, spawns a spiny egg above it;
 * if not, waits for reappear timer then respawns lakitu at the right edge.
 */
private fun System.lakituAndSpinyHandler(x: Int) {
    //> LakituAndSpinyHandler:
    //> lda FrenzyEnemyTimer    ;if timer here not expired, leave
    //> bne ExLSHand
    if (ram.frenzyEnemyTimer != 0.toByte()) return

    //> cpx #$05                ;if we are on the special use slot, leave
    //> bcs ExLSHand
    if (x >= 5) return

    //> lda #$80                ;set timer
    //> sta FrenzyEnemyTimer
    ram.frenzyEnemyTimer = 0x80.toByte()

    //> ldy #$04                ;start with the last enemy slot
    //> ChkLak: lda Enemy_ID,y  ;check all enemy slots to see
    //> cmp #Lakitu             ;if lakitu is on one of them
    //> beq CreateSpiny         ;if so, branch out of this loop
    //> dey; bpl ChkLak
    var lakituSlot = -1
    for (y in 4 downTo 0) {
        if (ram.enemyID[y] == EnemyId.Lakitu.byte) {
            lakituSlot = y
            break
        }
    }

    if (lakituSlot >= 0) {
        //> CreateSpiny:
        //> lda Player_Y_Position      ;if player above a certain point, branch to leave
        //> cmp #$2c; bcc ExLSHand
        if ((ram.playerYPosition.toInt() and 0xFF) < 0x2c) return

        //> lda Enemy_State,y          ;if lakitu is not in normal state, branch to leave
        //> bne ExLSHand
        if (ram.enemyState.getEnemyState(lakituSlot).isActive) return

        //> lda Enemy_PageLoc,y; sta Enemy_PageLoc,x
        ram.sprObjPageLoc[1 + x] = ram.sprObjPageLoc[1 + lakituSlot]
        //> lda Enemy_X_Position,y; sta Enemy_X_Position,x
        ram.sprObjXPos[1 + x] = ram.sprObjXPos[1 + lakituSlot]
        //> lda #$01; sta Enemy_Y_HighPos,x
        ram.sprObjYHighPos[1 + x] = 1
        //> lda Enemy_Y_Position,y; sec; sbc #$08; sta Enemy_Y_Position,x
        val lakituY = ram.sprObjYPos[1 + lakituSlot].toInt() and 0xFF
        ram.sprObjYPos[1 + x] = ((lakituY - 0x08) and 0xFF).toByte()

        //> lda PseudoRandomBitReg,x; and #%00000011; tay
        //> ldx #$02; DifLoop: lda PRDiffAdjustData,y; sta $01,x; iny*4; dex; bpl DifLoop
        // Assembly loads 3 PRDiffAdjustData values into $01-$03 for PlayerLakituDiff.
        // The Kotlin playerLakituDiff uses hardcoded lakituDiffAdj instead of $01-$03,
        // but the result is discarded anyway (SmallBBox destroys A, X_Speed set to 0).

        //> ldx ObjectOffset; jsr PlayerLakituDiff
        playerLakituDiff(x)

        //> ldy Player_X_Speed; cpy #$08; bcs SetSpSpd
        //> beq UsePosv                ;branch if neither bits are set
        //> eor #%11111111             ;otherwise get two's compliment of Y
        //> UsePosv:  tya              ;put value from A in Y back to A (they will be lost anyway)
        //> SetSpSpd: jsr SmallBBox    ;lose contents of A
        smallBBox()
        //> ldy #$02; sta Enemy_X_Speed,x  ;A=0 after SmallBBox, speed zeroed
        ram.sprObjXSpeed[1 + x] = 0
        //> cmp #$00; bmi SpinyRte; dey    ;A=0 so bmi not taken, Y=2->1
        //> SpinyRte: sty Enemy_MovingDir,x
        ram.enemyMovingDirs[x] = 1  // always 1 (A=0 from SmallBBox, bmi never taken)

        //> lda #$fd; sta Enemy_Y_Speed,x
        ram.sprObjYSpeed[1 + x] = 0xfd.toByte()
        //> lda #$01; sta Enemy_Flag,x
        ram.enemyFlags[x] = 1
        //> lda #$05; sta Enemy_State,x  ;put spiny in egg state
        ram.enemyState[x] = EnemyState.SPINY_EGG.byte
        return
    }

    // --- No lakitu found: try to respawn ---
    //> inc LakituReappearTimer
    ram.lakituReappearTimer = (ram.lakituReappearTimer + 1).toByte()
    //> lda LakituReappearTimer; cmp #$07; bcc ExLSHand  (SMB2J: cmp #$03)
    val reappearThreshold = if (variant == GameVariant.SMB2J) 0x03 else 0x07
    if ((ram.lakituReappearTimer.toInt() and 0xFF) < reappearThreshold) return

    //> ldx #$04
    //> ChkNoEn: lda Enemy_Flag,x; beq CreateL; dex; bpl ChkNoEn
    var emptySlot = -1
    for (s in 4 downTo 0) {
        if (ram.enemyFlags[s] == 0.toByte()) {
            emptySlot = s
            break
        }
    }
    //> bmi RetEOfs  ;if no empty slots were found, branch to leave
    if (emptySlot < 0) return

    //> CreateL:
    //> lda #$00; sta Enemy_State,x
    ram.enemyState[emptySlot] = EnemyState.INACTIVE.byte
    //> lda #Lakitu; sta Enemy_ID,x
    ram.enemyID[emptySlot] = EnemyId.Lakitu.byte

    //> jsr SetupLakitu
    val savedOffset = ram.objectOffset
    ram.objectOffset = emptySlot.toByte()
    // SetupLakitu inlined: clear timer, init flying enemy, set bbox
    ram.lakituReappearTimer = 0
    initHorizFlySwimEnemy()
    ram.enemyBoundBoxCtrls[emptySlot] = 0x03

    //> lda #$20
    var lakituY = 0x20
    if (variant == GameVariant.SMB2J) {
        //> ldy HardWorldFlag; bne SetLowLY  (worlds A-D: use $60)
        //> ldy WorldNumber; cpy #$06; bcc SetLakXY  (worlds 1-6: use $20)
        //> SetLowLY: lda #$60  (worlds 7-8 or A-D: lower position)
        if (ram.hardWorldFlag || (ram.worldNumber.toInt() and 0xFF) >= 6) {
            lakituY = 0x60
        }
    }
    //> jsr PutAtRightExtent
    putAtRightExtent(emptySlot, lakituY)

    //> RetEOfs: ldx ObjectOffset
    //> ExLSHand: rts
    ram.objectOffset = savedOffset
}

/**
 * PutAtRightExtent: places enemy at vertical position yPos, 32 pixels beyond right screen edge,
 * then jumps to FinishFlame to set bbox=$08, Y_HighPos=1, Flag=1, X_MoveForce=0, State=0.
 */
private fun System.putAtRightExtent(x: Int, yPos: Int) {
    //> PutAtRightExtent:
    //> sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = yPos.toByte()
    //> lda ScreenRight_X_Pos; clc; adc #$20; sta Enemy_X_Position,x
    val rightX = (ram.screenRightXPos.toInt() and 0xFF) + 0x20
    ram.sprObjXPos[1 + x] = (rightX and 0xFF).toByte()
    //> lda ScreenRight_PageLoc; adc #$00; sta Enemy_PageLoc,x
    ram.sprObjPageLoc[1 + x] = ((ram.screenRightPageLoc.toInt() and 0xFF) + (if (rightX > 0xFF) 1 else 0)).toByte()

    //> jmp FinishFlame
    finishFlame(x)
}

/**
 * FinishFlame: common tail for flame/enemy setup -- bbox=$08, enable flag, zero state.
 */
private fun System.finishFlame(x: Int) {
    //> FinishFlame:
    //> lda #$08; sta Enemy_BoundBoxCtrl,x
    ram.enemyBoundBoxCtrls[x] = 0x08
    //> lda #$01; sta Enemy_Y_HighPos,x; sta Enemy_Flag,x
    ram.sprObjYHighPos[1 + x] = 1
    ram.enemyFlags[x] = 1
    //> lsr; sta Enemy_X_MoveForce,x; sta Enemy_State,x
    ram.sprObjXMoveForce[1 + x] = 0
    ram.enemyState[x] = EnemyState.INACTIVE.byte
}

/**
 * InitFlyingCheepCheep: spawns flying cheep-cheep enemies with pseudo-random positioning
 * and speed, placed relative to the player's position.
 */
private fun System.initFlyingCheepCheep(x: Int) {
    //> InitFlyingCheepCheep:
    //> lda FrenzyEnemyTimer; bne ChpChpEx
    if (ram.frenzyEnemyTimer != 0.toByte()) return

    //> jsr SmallBBox
    smallBBox()

    //> lda PseudoRandomBitReg+1,x; and #%00000011; tay
    val timerOfs = ram.pseudoRandomBitReg[x + 1].toInt() and 0x03
    //> lda FlyCCTimerData,y; sta FrenzyEnemyTimer
    ram.frenzyEnemyTimer = flyCCTimerData[timerOfs].toByte()

    //> ldy #$03; lda SecondaryHardMode; beq MaxCC; iny
    val maxCC = if (ram.secondaryHardMode != 0.toByte()) 4 else 3
    //> MaxCC: sty $00; cpx $00; bcs ChpChpEx
    if (x >= maxCC) return

    //> lda PseudoRandomBitReg,x; and #%00000011; sta $00; sta $01
    val prBits = ram.pseudoRandomBitReg[x].toInt() and 0x03
    var temp00 = prBits
    var temp01 = prBits

    //> lda #$fb; sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[1 + x] = 0xfb.toByte()

    //> lda #$00; ldy Player_X_Speed; beq GSeed
    var seedAdder = 0
    val playerSpeed = ram.playerXSpeed.toInt() and 0xFF
    if (playerSpeed != 0) {
        //> lda #$04; cpy #$19; bcc GSeed; asl
        seedAdder = if (playerSpeed >= 0x19) 0x08 else 0x04
    }

    //> GSeed: pha
    val stackVal = seedAdder
    //> clc; adc $00; sta $00
    temp00 = (seedAdder + temp00) and 0xFF

    //> lda PseudoRandomBitReg+1,x; and #%00000011
    val pr1Bits = ram.pseudoRandomBitReg[x + 1].toInt() and 0x03
    //> beq RSeed
    if (pr1Bits != 0) {
        //> lda PseudoRandomBitReg+2,x; and #%00001111; sta $00
        temp00 = ram.pseudoRandomBitReg[x + 2].toInt() and 0x0F
    }

    //> RSeed: pla; clc; adc $01; tay
    val speedOfs = (stackVal + temp01) and 0xFF

    //> lda FlyCCXSpeedData,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[1 + x] = flyCCXSpeedData.getOrElse(speedOfs) { 0 }.toByte()

    //> lda #$01; sta Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = 1

    //> lda Player_X_Speed; bne D2XPos1
    // NES Y register at D2XPos1 depends on code path:
    // If playerSpeed != 0: Y = speedOfs (from tay after RSeed), skip ldy $00
    // If playerSpeed == 0: Y = temp00 (from ldy $00)
    var posY: Int
    if (playerSpeed == 0) {
        //> ldy $00; tya; and #%00000010; beq D2XPos1
        posY = temp00
        val d1Set = (temp00 and 0x02) != 0
        if (d1Set) {
            //> lda Enemy_X_Speed,x; eor #$ff; clc; adc #$01; sta Enemy_X_Speed,x
            val curSpeed = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
            ram.sprObjXSpeed[1 + x] = (((curSpeed xor 0xFF) + 1) and 0xFF).toByte()
            //> inc Enemy_MovingDir,x
            ram.enemyMovingDirs[x] = 2
        }
    } else {
        posY = speedOfs  // Y retains speedOfs value, ldy $00 was skipped
    }

    //> D2XPos1: tya; and #%00000010; beq D2XPos2
    val posOfs = posY
    if ((posOfs and 0x02) != 0) {
        //> lda Player_X_Position; clc; adc FlyCCXPositionData,y
        val pX = ram.playerXPosition.toInt() and 0xFF
        val addVal = flyCCXPositionData.getOrElse(posOfs) { 0 }
        val sum = pX + addVal
        //> sta Enemy_X_Position,x
        ram.sprObjXPos[1 + x] = (sum and 0xFF).toByte()
        //> lda Player_PageLoc; adc #$00
        //> jmp FinCCSt
        ram.sprObjPageLoc[1 + x] = ((ram.playerPageLoc.toInt() and 0xFF) + (if (sum > 0xFF) 1 else 0)).toByte()
    } else {
        //> D2XPos2: lda Player_X_Position; sec; sbc FlyCCXPositionData,y
        val pX = ram.playerXPosition.toInt() and 0xFF
        val subVal = flyCCXPositionData.getOrElse(posOfs) { 0 }
        val diff = pX - subVal
        //> sta Enemy_X_Position,x
        ram.sprObjXPos[1 + x] = (diff and 0xFF).toByte()
        //> lda Player_PageLoc; sbc #$00
        //> FinCCSt:
        ram.sprObjPageLoc[1 + x] = ((ram.playerPageLoc.toInt() and 0xFF) - (if (diff < 0) 1 else 0)).toByte()
    }

    //> sta Enemy_PageLoc,x  (already stored above)
    //> lda #$01; sta Enemy_Flag,x; sta Enemy_Y_HighPos,x
    ram.enemyFlags[x] = 1
    ram.sprObjYHighPos[1 + x] = 1
    //> lda #$f8; sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = 0xf8.toByte()
}

/**
 * InitBowserFlame: spawns bowser's flame either from bowser's mouth (if bowser exists)
 * or from the right edge of the screen with randomized vertical position.
 */
private fun System.initBowserFlame(x: Int) {
    //> InitBowserFlame:
    //> lda FrenzyEnemyTimer; bne FlmEx
    if (ram.frenzyEnemyTimer != 0.toByte()) return

    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYMoveForce[1 + x] = 0

    //> lda NoiseSoundQueue; ora #Sfx_BowserFlame; sta NoiseSoundQueue
    ram.noiseSoundQueue = (ram.noiseSoundQueue.toInt() or Constants.Sfx_BowserFlame.toInt()).toByte()

    //> ldy BowserFront_Offset
    val bowserOfs = ram.bowserFrontOffset.toInt() and 0xFF
    //> lda Enemy_ID,y; cmp #Bowser; beq SpawnFromMouth
    if (ram.enemyID[bowserOfs] == EnemyId.Bowser.byte) {
        //> SpawnFromMouth:
        //> lda Enemy_X_Position,y; sec; sbc #$0e; sta Enemy_X_Position,x
        val bowserX = ram.sprObjXPos[1 + bowserOfs].toInt() and 0xFF
        ram.sprObjXPos[1 + x] = ((bowserX - 0x0e) and 0xFF).toByte()
        //> lda Enemy_PageLoc,y; sta Enemy_PageLoc,x
        ram.sprObjPageLoc[1 + x] = ram.sprObjPageLoc[1 + bowserOfs]
        //> lda Enemy_Y_Position,y; clc; adc #$08; sta Enemy_Y_Position,x
        val bowserY = ram.sprObjYPos[1 + bowserOfs].toInt() and 0xFF
        ram.sprObjYPos[1 + x] = ((bowserY + 0x08) and 0xFF).toByte()

        //> lda PseudoRandomBitReg,x; and #%00000011; sta Enemy_YMF_Dummy,x
        val prBits = ram.pseudoRandomBitReg[x].toInt() and 0x03
        ram.sprObjYMFDummy[1 + x] = prBits.toByte()
        //> tay; lda FlameYPosData,y
        val flameTargetY = flameYPosData[prBits]
        //> ldy #$00; cmp Enemy_Y_Position,x; bcc SetMF; iny
        val mfOfs = if (flameTargetY >= (ram.sprObjYPos[1 + x].toInt() and 0xFF)) 1 else 0
        //> SetMF: lda FlameYMFAdderData,y; sta Enemy_Y_MoveForce,x
        ram.sprObjYMoveForce[1 + x] = flameYMFAdderData[mfOfs].toByte()

        //> lda #$00; sta EnemyFrenzyBuffer
        ram.enemyFrenzyBuffer = 0

        //> (falls through to FinishFlame)
        finishFlame(x)
    } else {
        // --- Not bowser, spawn from right edge ---
        //> jsr SetFlameTimer; clc; adc #$20
        var timerVal = setFlameTimer() + 0x20
        //> ldy SecondaryHardMode; beq SetFrT
        if (ram.secondaryHardMode != 0.toByte()) {
            //> sec; sbc #$10
            timerVal -= 0x10
        }
        //> SetFrT: sta FrenzyEnemyTimer
        ram.frenzyEnemyTimer = (timerVal and 0xFF).toByte()

        //> lda PseudoRandomBitReg,x; and #%00000011
        val prBits = ram.pseudoRandomBitReg[x].toInt() and 0x03
        //> sta BowserFlamePRandomOfs,x  ($0417+x = sprObjYMFDummy[1+x])
        ram.sprObjYMFDummy[1 + x] = prBits.toByte()
        //> tay; lda FlameYPosData,y
        val yPos = flameYPosData[prBits]

        //> (falls through to PutAtRightExtent which jmp's to FinishFlame)
        putAtRightExtent(x, yPos)
    }
}

/**
 * BulletBillCheepCheep: in water levels, spawns swimming cheep-cheeps using bitmask filtering;
 * in non-water levels, spawns bullet bills as frenzy variant.
 */
private fun System.bulletBillCheepCheep(x: Int) {
    //> ;$00 - used to store Y position of group enemies
    //> ;$01 - used to store enemy ID
    //> ;$02 - used to store page location of right side of screen
    //> ;$03 - used to store X position of right side of screen
    //> BulletBillCheepCheep:
    //> lda FrenzyEnemyTimer; bne ExF17
    if (ram.frenzyEnemyTimer != 0.toByte()) return

    //> lda AreaType; bne DoBulletBills
    if (ram.areaType != AreaType.Water) {
        //> DoBulletBills:
        //> ldy #$ff
        //> BB_SLoop: iny; cpy #$05; bcs FireBulletBill
        //> ExF17:    rts                        ;if found, leave
        for (y in 0 until 5) {
            //> lda Enemy_Flag,y; beq BB_SLoop
            if (ram.enemyFlags[y] == 0.toByte()) continue
            //> lda Enemy_ID,y; cmp #BulletBill_FrenzyVar; bne BB_SLoop
            if (ram.enemyID[y] == EnemyId.BulletBillFrenzyVar.byte) {
                //> rts  ;if found, leave
                return
            }
        }
        //> FireBulletBill:
        //> ;$03 - used to store X position of right side of screen
        //> ;$02 - used to store page location of right side of screen
        //> ;$01 - used to store enemy ID
        //> ;$00 - used to store Y position of group enemies
        //> lda Square2SoundQueue; ora #Sfx_Blast; sta Square2SoundQueue
        ram.square2SoundQueue = (ram.square2SoundQueue.toInt() or Constants.Sfx_Blast.toInt()).toByte()
        //> lda #BulletBill_FrenzyVar; bne Set17ID (unconditional)
        ram.enemyID[x] = EnemyId.BulletBillFrenzyVar.byte
    } else {
        // --- Water level: swimming cheep-cheeps ---
        //> cpx #$03; bcs ExF17
        if (x >= 3) return

        //> ldy #$00; lda PseudoRandomBitReg,x; cmp #$aa; bcc ChkW2; iny
        var idOfs = 0
        if ((ram.pseudoRandomBitReg[x].toInt() and 0xFF) >= 0xaa) idOfs++
        //> ChkW2: lda WorldNumber; cmp #World2; beq Get17ID; iny
        if (ram.worldNumber != Constants.World2) idOfs++
        //> Get17ID: tya; and #%00000001; tay
        idOfs = idOfs and 0x01
        //> lda SwimCC_IDData,y; Set17ID: sta Enemy_ID,x
        ram.enemyID[x] = swimCCIDData[idOfs].toByte()
    }

    //> lda BitMFilter; cmp #$ff; bne GetRBit
    if ((ram.bitMFilter.toInt() and 0xFF) == 0xFF) {
        //> lda #$00; sta BitMFilter
        ram.bitMFilter = 0
    }

    //> GetRBit: lda PseudoRandomBitReg,x; and #%00000111
    var bitOfs = ram.pseudoRandomBitReg[x].toInt() and 0x07

    //> ChkRBit: tay; lda Bitmasks,y; bit BitMFilter; beq AddFBit
    while (true) {
        val mask = bitmasks[bitOfs]
        if ((ram.bitMFilter.toInt() and mask) == 0) break
        //> iny; tya; and #%00000111; jmp ChkRBit
        bitOfs = (bitOfs + 1) and 0x07
    }

    //> AddFBit: ora BitMFilter; sta BitMFilter
    ram.bitMFilter = ((ram.bitMFilter.toInt() and 0xFF) or bitmasks[bitOfs]).toByte()

    //> lda Enemy17YPosData,y; jsr PutAtRightExtent
    putAtRightExtent(x, enemy17YPosData[bitOfs])

    //> sta Enemy_YMF_Dummy,x  (A = 0 after FinishFlame's lsr #$01)
    ram.sprObjYMFDummy[1 + x] = 0

    //> lda #$20; sta FrenzyEnemyTimer
    ram.frenzyEnemyTimer = 0x20

    //> jmp CheckpointEnemyID
    val savedOffset = ram.objectOffset
    ram.objectOffset = x.toByte()
    checkpointEnemyID()
    ram.objectOffset = savedOffset
}

/**
 * Initializes short firebar: set spin state, speed, direction; center on tile (+4px).
 */
private fun System.initShortFirebar() {
    //> ;$00-$01 - used to hold pseudorandom bits
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitShortFirebar:
    //> lda #$00                    ;initialize low byte of spin state
    //> sta FirebarSpinState_Low,x
    // by Claude - FirebarSpinState_Low,x = Enemy_X_Speed ($58+x)
    ram.sprObjXSpeed[1 + x] = 0

    //> lda Enemy_ID,x              ;subtract $1b from enemy identifier
    //> sec                         ;to get proper offset for firebar data
    //> sbc #$1b
    //> tay
    val fbOfs = (ram.enemyID[x].toInt() and 0xFF) - 0x1b

    //> lda FirebarSpinSpdData,y    ;get spinning speed of firebar
    //> sta FirebarSpinSpeed,x
    ram.firebarSpinSpeed[x] = firebarSpinSpdData[fbOfs].toByte()

    //> lda FirebarSpinDirData,y    ;get spinning direction of firebar
    //> sta FirebarSpinDirection,x
    ram.firebarSpinDirection[x] = firebarSpinDirData[fbOfs].toByte()

    //> lda Enemy_Y_Position,x
    //> clc                         ;add four pixels to vertical coordinate
    //> adc #$04
    //> sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = ((ram.sprObjYPos[1 + x].toInt() and 0xFF) + 4).toByte()

    //> lda Enemy_X_Position,x
    //> clc                         ;add four pixels to horizontal coordinate
    //> adc #$04
    //> sta Enemy_X_Position,x
    val newX = (ram.sprObjXPos[1 + x].toInt() and 0xFF) + 4
    ram.sprObjXPos[1 + x] = (newX and 0xFF).toByte()

    //> lda Enemy_PageLoc,x
    //> adc #$00                    ;add carry to page location
    //> sta Enemy_PageLoc,x
    ram.sprObjPageLoc[1 + x] = ((ram.sprObjPageLoc[1 + x].toInt() and 0xFF) + (if (newX > 0xFF) 1 else 0)).toByte()

    //> jmp TallBBox2               ;set bounding box control (not used) and leave
    //> ;$00-$01 - used to hold pseudorandom bits
    ram.enemyBoundBoxCtrls[x] = 0x03
}

/**
 * Initializes long firebar: duplicates enemy object first, then does short firebar init.
 */
private fun System.initLongFirebar() {
    //> InitLongFirebar:
    //> jsr DuplicateEnemyObj       ;create enemy object for long firebar
    duplicateEnemyObj()

    //> (falls through to InitShortFirebar)
    initShortFirebar()
}

/**
 * Initializes balance platform: adjusts Y, positions, sets alignment state.
 */
private fun System.initBalPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitBalPlatform:
    //> dec Enemy_Y_Position,x    ;raise vertical position by two pixels
    //> dec Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = ((ram.sprObjYPos[1 + x].toInt() and 0xFF) - 2).toByte()

    //> ldy SecondaryHardMode     ;if secondary hard mode flag not set,
    //> bne AlignP                ;branch ahead
    if (ram.secondaryHardMode == 0.toByte()) {
        //> ldy #$02                  ;otherwise set value here
        //> jsr PosPlatform           ;do a sub to add or subtract pixels
        posPlatform(2)
    }

    //> AlignP: ldy #$ff          ;set default value here for now
    //> lda BalPlatformAlignment  ;get current balance platform alignment
    //> sta Enemy_State,x         ;set platform alignment to object state here
    val curAlign = ram.balPlatformAlignment.toInt() and 0xFF
    ram.enemyState[x] = curAlign.toByte()

    //> bpl SetBPA                ;if old alignment $ff, put $ff as alignment for negative
    if ((curAlign and 0x80) != 0) {
        //> txa                       ;if old contents already $ff, put
        //> tay                       ;object offset as alignment to make next positive
        //> SetBPA: sty BalPlatformAlignment
        ram.balPlatformAlignment = x.toByte()
    } else {
        //> SetBPA: sty BalPlatformAlignment  ;store $ff here
        ram.balPlatformAlignment = 0xFF.toByte()
    }

    //> lda #$00
    //> sta Enemy_MovingDir,x     ;init moving direction
    ram.enemyMovingDirs[x] = 0

    //> tay                       ;init Y to 0
    //> jsr PosPlatform           ;do a sub to add 8 pixels, then run shared code here
    posPlatform(0)

    //> (falls through to InitDropPlatform)
    initDropPlatform()
}

/**
 * Initializes vertical platform: stores top Y and center Y positions.
 */
private fun System.initVertPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitVertPlatform:
    //> ldy #$40                    ;set default value here
    //> lda Enemy_Y_Position,x      ;check vertical position
    val yPos = ram.sprObjYPos[1 + x].toInt() and 0xFF
    val topY: Int
    val centerAdder: Int

    //> bpl SetYO                   ;if above a certain point, skip this part
    if ((yPos and 0x80) == 0) {
        topY = yPos
        centerAdder = 0x40
    } else {
        //> eor #$ff
        //> clc                         ;otherwise get two's complement
        //> adc #$01
        topY = ((yPos xor 0xFF) + 1) and 0xFF
        //> ldy #$c0                    ;get alternate value to add to vertical position
        centerAdder = 0xC0
    }

    //> SetYO: sta YPlatformTopYPos,x
    ram.sprObjXMoveForce[1 + x] = topY.toByte()  // YPlatformTopYPos,x ($401+x)

    //> tya
    //> eor #%11111111             ;otherwise get two's compliment of Y
    //> clc                         ;load value from earlier, add number of pixels
    //> adc Enemy_Y_Position,x      ;to vertical position
    //> sta YPlatformCenterYPos,x   ;save result as central vertical position
    ram.sprObjXSpeed[1 + x] = ((centerAdder + yPos) and 0xFF).toByte()  // YPlatformCenterYPos,x ($58+x)

    //> (falls through to CommonPlatCode)
    commonPlatCode()
}

/**
 * Initializes large lift going up: platform lift up + overwrite bbox with SPBBox.
 */
private fun System.largeLiftUp() {
    //> LargeLiftUp:
    //> jsr PlatLiftUp       ;execute code for platforms going up
    platLiftUp()

    //> jmp LargeLiftBBox    → jmp SPBBox  ;overwrite bounding box for large platforms
    spBBox()
}

/**
 * Initializes large lift going down: platform lift down + overwrite bbox.
 */
private fun System.largeLiftDown() {
    //> LargeLiftDown:
    //> jsr PlatLiftDown     ;execute code for platforms going down
    platLiftDown()

    //> LargeLiftBBox: jmp SPBBox  ;jump to overwrite bounding box size control
    spBBox()
}

/**
 * Initializes horizontal platform: zero secondary counter, common platform code.
 */
private fun System.initHoriPlatform() {
    //> InitHoriPlatform:
    //> lda #$00
    //> sta XMoveSecondaryCounter,x  ;init one of the moving counters
    // by Claude - XMoveSecondaryCounter,x = Enemy_X_Speed ($58+x)
    val x = ram.objectOffset.toInt() and 0xFF
    ram.sprObjXSpeed[1 + x] = 0

    //> jmp CommonPlatCode           ;jump ahead to execute more code
    commonPlatCode()
}

/**
 * Initializes drop platform: set collision flag to $ff, then common platform code.
 */
private fun System.initDropPlatform() {
    //> InitDropPlatform:
    //> lda #$ff
    //> sta PlatformCollisionFlag,x  ;set some value here
    ram.platformCollisionFlag = 0xFF.toByte()

    //> jmp CommonPlatCode           ;then jump ahead to execute more code
    commonPlatCode()
}

/**
 * Initializes platform going up: Y move force $10, Y speed $ff, position + small lift bbox.
 */
private fun System.platLiftUp() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> PlatLiftUp:
    //> lda #$10                 ;set movement amount here
    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYMoveForce[1 + x] = 0x10

    //> lda #$ff                 ;set moving speed for platforms going up
    //> sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[1 + x] = 0xFF.toByte()

    //> jmp CommonSmallLift      ;skip ahead to part we should be executing
    commonSmallLift()
}

/**
 * Initializes platform going down: Y move force $f0, Y speed $00, position + small lift bbox.
 */
private fun System.platLiftDown() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> PlatLiftDown:
    //> lda #$f0                 ;set movement amount here
    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYMoveForce[1 + x] = 0xF0.toByte()

    //> lda #$00                 ;set moving speed for platforms going down
    //> sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[1 + x] = 0

    //> (falls through to CommonSmallLift)
    commonSmallLift()
}

/**
 * Initializes bowser: duplicates enemy object, sets all bowser-specific state.
 */
private fun System.initBowser() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitBowser:
    //> jsr DuplicateEnemyObj     ;jump to create another bowser object
    duplicateEnemyObj()

    //> stx BowserFront_Offset    ;save offset of first here
    ram.bowserFrontOffset = x.toByte()

    //> lda #$00
    //> sta BowserBodyControls    ;initialize bowser's body controls
    ram.bowserBodyControls = 0

    //> sta BridgeCollapseOffset  ;and bridge collapse offset
    ram.bridgeCollapseOffset = 0

    //> lda Enemy_X_Position,x
    //> sta BowserOrigXPos        ;store original horizontal position here
    ram.bowserOrigXPos = ram.sprObjXPos[1 + x]

    //> lda #$df
    //> sta BowserFireBreathTimer ;store something here
    ram.bowserFireBreathTimer = 0xdf.toByte()

    //> sta Enemy_MovingDir,x     ;and in moving direction
    ram.enemyMovingDirs[x] = 0xdf.toByte()

    //> lda #$20
    //> sta BowserFeetCounter     ;set bowser's feet timer and in enemy timer
    ram.bowserFeetCounter = 0x20

    //> sta EnemyFrameTimer,x
    ram.timers[0x0a + x] = 0x20

    //> lda #$05
    //> ChpChpEx: rts
    //> sta BowserHitPoints       ;give bowser 5 hit points
    ram.bowserHitPoints = 5

    //> lsr
    //> ExitFWk:  rts
    //> sta BowserMovementSpeed   ;set default movement speed here (5 >> 1 = 2)
    ram.bowserMovementSpeed = 2
}

/**
 * Residual jump point for power-up object: sets state, flag, bbox, attributes, sound.
 */
private fun System.pwrUpJmp() {
    //> PwrUpJmp:  lda #$01                  ;this is a residual jump point in enemy object jump table
    //> sta Enemy_State+5         ;set power-up object's state
    ram.enemyState[5] = EnemyState.NORMAL.byte

    //> sta Enemy_Flag+5          ;set buffer flag
    ram.enemyFlags[5] = 1

    //> lda #$03
    //> sta Enemy_BoundBoxCtrl+5  ;set bounding box size control for power-up object
    ram.enemyBoundBoxCtrls[5] = 0x03

    //> lda PowerUpType
    //> cmp #$02                  ;check currently loaded power-up type
    //> bcs PutBehind             ;if star or 1-up, branch ahead
    val powerUpType = ram.powerUpType.toInt() and 0xFF
    if (powerUpType < 0x02) {
        //> lda PlayerStatus          ;otherwise check player's current status
        val playerStatusOrd = ram.playerStatus.ordinal
        //> cmp #$02
        //> bcc StrType               ;if player not fiery, use status as power-up type
        val newType = if (playerStatusOrd < 0x02) {
            playerStatusOrd
        } else {
            //> lsr                       ;otherwise shift right to force fire flower type
            playerStatusOrd ushr 1
        }
        //> StrType: sta PowerUpType
        ram.powerUpType = newType.toByte()
    }

    //> PutBehind: lda #%00100000
    //> sta Enemy_SprAttrib+5     ;set background priority bit
    ram.sprAttrib[1 + 5] = 0x20

    //> lda #Sfx_GrowPowerUp
    //> sta Square2SoundQueue     ;load power-up reveal sound and leave
    ram.square2SoundQueue = (ram.square2SoundQueue.toInt() or Constants.Sfx_GrowPowerUp.toInt()).toByte()
}

/**
 * Sets up vine object from block coordinates. Stores vine slot offset and plays sound.
 * Note: this routine is shared between block-hit code and checkpointEnemyID dispatch.
 * When called from checkpointEnemyID, the vine is already positioned from enemy data.
 */
private fun System.setupVine() {
    //> ;$06-$07 - used as address to block buffer data
    //> ;$02 - used as vertical high nybble of block buffer offset
    val x = ram.objectOffset.toInt() and 0xFF

    //> Setup_Vine:
    //> lda #VineObject          ;load identifier for vine object
    //> sta Enemy_ID,x           ;store in buffer
    ram.enemyID[x] = EnemyId.VineObject.byte

    //> lda #$01
    //> sta Enemy_Flag,x         ;set flag for enemy object buffer
    ram.enemyFlags[x] = 1

    //> lda Block_PageLoc,y      ;copy block coordinates to enemy position
    //> sta Enemy_PageLoc,x
    //> SpawnFromMouth:
    //> lda Block_X_Position,y
    //> sta Enemy_X_Position,x
    //> lda Block_Y_Position,y
    //> sta Enemy_Y_Position,x
    // NES Y register here comes from JumpEngine dispatch: Y = enemyID*2+2 = 0x60
    // for VineObject. Setup_Vine reads Block_PageLoc,Y / Block_X_Position,Y /
    // Block_Y_Position,Y using this Y value. The resulting addresses ($00D6, $00EF,
    // $0137) point to scratch RAM and stack, not meaningful block data.
    // Use GameRamMapper for full flat RAM access including unmapped scratch bytes.
    val y = setupVineBlockY
    val flat = GameRamMapper.toFlat(ram)
    fun readFlat(addr: Int): Byte = flat.getOrElse(addr) { 0 }
    val pageLoc = readFlat(0x76 + y)
    val xPos = readFlat(0x8F + y)
    val yPos = readFlat(0xD7 + y)
    if (debugEnemyTrace) println("[setupVine] y=$y blockIdx=${9+y} nesAddr=\$${(0x76+y).toString(16)}/\$${(0x8F+y).toString(16)}/\$${(0xD7+y).toString(16)} → page=${pageLoc.toInt() and 0xFF} x=${xPos.toInt() and 0xFF} y=${yPos.toInt() and 0xFF}")
    ram.sprObjPageLoc[1 + x] = pageLoc
    ram.sprObjXPos[1 + x] = xPos
    ram.sprObjYPos[1 + x] = yPos

    //> ldy VineFlagOffset       ;load vine flag/offset to next available vine slot
    val vineFlagOfs = ram.vineFlagOffset.toInt() and 0xFF

    //> bne NextVO               ;if set at all, don't bother to store vertical
    if (vineFlagOfs == 0) {
        //> sta VineStart_Y_Position ;otherwise store vertical coordinate here
        ram.vineStartYPosition = ram.sprObjYPos[1 + x]
    }

    //> NextVO: txa              ;store object offset to next available vine slot
    //> sta VineObjOffset,y      ;using vine flag as offset
    ram.vineObjOffsets[vineFlagOfs] = x.toByte()

    //> inc VineFlagOffset       ;increment vine flag offset
    ram.vineFlagOffset = ((vineFlagOfs + 1) and 0xFF).toByte()

    //> lda #Sfx_GrowVine
    //> sta Square2SoundQueue    ;load vine grow sound
    ram.square2SoundQueue = (ram.square2SoundQueue.toInt() or Constants.Sfx_GrowVine.toInt()).toByte()
}

/**
 * Initializes retainer (princess/mushroom retainer): fixed Y position $b8.
 */
private fun System.initRetainerObj() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitRetainerObj:
    //> lda #$b8                ;set fixed vertical position for
    //> sta Enemy_Y_Position,x  ;princess/mushroom retainer object
    ram.sprObjYPos[1 + x] = 0xb8.toByte()
}

/**
 * End-of-init-code sentinel. Does nothing (just an rts in assembly).
 */
private fun System.endOfEnemyInitCode() {
    //> EndOfEnemyInitCode: rts
}

// --- Shared init helpers ---
// by Claude - TallBBox, SmallBBox, SetBBox, InitVStf, CommonPlatCode, etc.

/**
 * TallBBox: sets bounding box control to $03 and falls through to SetBBox.
 */
private fun System.tallBBox() {
    //> TallBBox: lda #$03
    setBBox(0x03)
}

/**
 * SmallBBox: sets bounding box control to $09 and falls through to SetBBox.
 */
private fun System.smallBBox() {
    //> SmallBBox: lda #$09
    //> bne SetBBox            ;unconditional branch
    setBBox(0x09)
}

/**
 * SetBBox: stores bounding box control, sets moving direction to left, inits vertical.
 */
private fun System.setBBox(ctrl: Int) {
    val x = ram.objectOffset.toInt() and 0xFF

    //> SetBBox: sta Enemy_BoundBoxCtrl,x
    ram.enemyBoundBoxCtrls[x] = ctrl.toByte()

    //> lda #$02                    ;set moving direction for left
    //> sta Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = 2

    //> InitVStf:
    initVStf()
}

/**
 * InitVStf: zeroes vertical speed and vertical movement force.
 */
private fun System.initVStf() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> InitVStf: lda #$00
    //> sta Enemy_Y_Speed,x         ;and movement force
    ram.sprObjYSpeed[1 + x] = 0

    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYMoveForce[1 + x] = 0
}

/**
 * CommonPlatCode: inits vertical, then sets bounding box based on area type and hard mode.
 */
private fun System.commonPlatCode() {
    //> CommonPlatCode:
    //> jsr InitVStf              ;do a sub to init certain other values
    initVStf()

    //> SPBBox:
    spBBox()
}

/**
 * SPBBox: sets platform bounding box control - $05 for castle or hard mode, $06 otherwise.
 */
private fun System.spBBox() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> SPBBox: lda #$05           ;set default bounding box size control
    //> ldy AreaType
    //> cpy #$03                  ;check for castle-type level
    //> beq CasPBB                ;use default value if found
    //> ldy SecondaryHardMode     ;otherwise check for secondary hard mode flag
    //> bne CasPBB                ;if set, use default value
    //> lda #$06                  ;use alternate value if not castle or secondary not set
    val bbCtrl = if (ram.areaType == AreaType.Castle || ram.secondaryHardMode != 0.toByte()) 0x05 else 0x06

    //> CasPBB: sta Enemy_BoundBoxCtrl,x
    ram.enemyBoundBoxCtrls[x] = bbCtrl.toByte()
}

/**
 * CommonSmallLift: positions platform (+12px), sets bbox control to $04.
 */
private fun System.commonSmallLift() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> CommonSmallLift:
    //> ldy #$01
    //> jsr PosPlatform           ;do a sub to add 12 pixels due to preset value
    posPlatform(1)

    //> lda #$04
    //> sta Enemy_BoundBoxCtrl,x  ;set bounding box control for small platforms
    ram.enemyBoundBoxCtrls[x] = 0x04
}

/**
 * PosPlatform: adds signed offset to enemy X position and page location.
 */
private fun System.posPlatform(y: Int) {
    val x = ram.objectOffset.toInt() and 0xFF

    //> PosPlatform:
    //> lda Enemy_X_Position,x  ;get horizontal coordinate
    //> clc
    //> adc PlatPosDataLow,y    ;add or subtract pixels depending on offset
    //> sta Enemy_X_Position,x  ;store as new horizontal coordinate
    val curX = ram.sprObjXPos[1 + x].toInt() and 0xFF
    val addend = platPosDataLow[y] and 0xFF  // unsigned: 6502 ADC treats all as unsigned
    val newX = curX + addend
    ram.sprObjXPos[1 + x] = (newX and 0xFF).toByte()

    //> lda Enemy_PageLoc,x
    //> adc PlatPosDataHigh,y   ;add or subtract page location depending on offset
    //> sta Enemy_PageLoc,x     ;store as new page location
    val curPage = ram.sprObjPageLoc[1 + x].toInt() and 0xFF
    val pageAddend = platPosDataHigh[y] and 0xFF  // unsigned: 6502 ADC treats all as unsigned
    val carry = if (newX > 0xFF) 1 else 0  // standard unsigned carry from ADC
    ram.sprObjPageLoc[1 + x] = ((curPage + pageAddend + carry) and 0xFF).toByte()
}

/**
 * DuplicateEnemyObj: finds an empty enemy slot, copies position, links via flag MSB.
 */
private fun System.duplicateEnemyObj() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> DuplicateEnemyObj:
    //> ldy #$ff                ;start at beginning of enemy slots
    //> FSLoop: iny             ;increment one slot
    //> lda Enemy_Flag,y        ;check enemy buffer flag for empty slot
    //> bne FSLoop              ;if set, branch and keep checking
    var y = 0
    while (y < 6 && ram.enemyFlags[y] != 0.toByte()) y++
    if (y >= 6) return  // no empty slot found

    //> sty DuplicateObj_Offset ;otherwise set offset here
    ram.duplicateObjOffset = y.toByte()

    //> txa                     ;transfer original enemy buffer offset
    //> ora #%10000000          ;store with d7 set as flag in new enemy
    //> sta Enemy_Flag,y        ;slot as well as enemy offset
    ram.enemyFlags[y] = ((x and 0xFF) or 0x80).toByte()

    //> lda Enemy_PageLoc,x
    //> sta Enemy_PageLoc,y     ;copy page location
    ram.sprObjPageLoc[1 + y] = ram.sprObjPageLoc[1 + x]

    //> lda Enemy_X_Position,x  ;copy horizontal coordinates
    //> sta Enemy_X_Position,y  ;from original enemy to new enemy
    ram.sprObjXPos[1 + y] = ram.sprObjXPos[1 + x]

    //> lda #$01
    //> sta Enemy_Flag,x        ;set flag as normal for original enemy
    ram.enemyFlags[x] = 1

    //> sta Enemy_Y_HighPos,y   ;set high vertical byte for new enemy
    ram.sprObjYHighPos[1 + y] = 1

    //> lda Enemy_Y_Position,x
    //> sta Enemy_Y_Position,y  ;copy vertical coordinate from original to new
    //> FlmEx:  rts                     ;and then leave
    ram.sprObjYPos[1 + y] = ram.sprObjYPos[1 + x]
}

// by Claude - killAllEnemies() moved to victorySubs.kt

/**
 * HandleGroupEnemies: spawns a group of 2-3 enemies at the right edge of the screen.
 * The groupType parameter is the raw second byte value ($37-$3e range) from enemy data.
 */
private fun System.handleGroupEnemies(groupType: Int) {
    //> HandleGroupEnemies:
    //> ldy #$00                  ;load value for green koopa troopa
    //> sec
    //> sbc #$37                  ;subtract $37 from second byte read
    val adjustedType = groupType - 0x37

    //> pha                       ;save result in stack for now
    //> cmp #$04                  ;was byte in $3b-$3e range?
    //> bcs SnglID                ;if so, branch (single enemy type from Y=$00)
    val enemyIdForGroup: Int
    if (adjustedType < 0x04) {
        //> pha                       ;save another copy to stack
        //> ldy #Goomba               ;load value for goomba enemy
        //> lda PrimaryHardMode       ;if primary hard mode flag not set,
        //> beq PullID                ;branch, otherwise change to value
        //> ldy #BuzzyBeetle          ;for buzzy beetle
        //> PullID: pla               ;get second copy from stack
        enemyIdForGroup = if (ram.primaryHardMode) {
            EnemyId.BuzzyBeetle.id
        } else {
            EnemyId.Goomba.id
        }
    } else {
        //> SnglID: sty $01           ;save enemy id here (Y=0 = green koopa troopa)
        enemyIdForGroup = 0x00  // GreenKoopa
    }

    //> ldy #$b0                  ;load default y coordinate
    //> and #$02                  ;check to see if d1 was set
    //> beq SetYGp                ;if not set, use default $b0
    //> ldy #$70                  ;otherwise use $70
    val yCoord = if ((adjustedType and 0x02) != 0) 0x70 else 0xb0

    //> SetYGp: sty $00           ;save y coordinate here
    //> lda ScreenRight_PageLoc   ;get page number of right edge of screen
    //> sta $02                   ;save here
    var pageStore = ram.screenRightPageLoc.toInt() and 0xFF

    //> lda ScreenRight_X_Pos     ;get pixel coordinate of right edge
    //> sta $03                   ;save here
    var xStore = ram.screenRightXPos.toInt() and 0xFF

    //> ldy #$02                  ;load two enemies by default
    //> pla                       ;get first copy from stack
    //> lsr                       ;check to see if d0 was set
    //> bcc CntGrp                ;if not, use default value
    //> iny                       ;otherwise increment to three enemies
    var numEnemies = if ((adjustedType and 0x01) != 0) 3 else 2

    //> CntGrp: sty NumberofGroupEnemies
    ram.numberofGroupEnemies = numEnemies.toByte()

    //> GrLoop:
    while (numEnemies > 0) {
        //> ldx #$ff                  ;start at beginning of enemy buffers
        //> GSltLp: inx               ;increment and branch if past
        //> cpx #$05                  ;end of buffers
        //> bcs NextED
        var slot = -1
        for (s in 0 until 5) {
            //> lda Enemy_Flag,x      ;check to see if enemy is already
            //> bne GSltLp            ;stored in buffer, and branch if so
            if (ram.enemyFlags[s] == 0.toByte()) {
                slot = s
                break
            }
        }
        if (slot < 0) break  // no empty slot, jump to NextED

        //> lda $01
        //> sta Enemy_ID,x            ;store enemy object identifier
        ram.enemyID[slot] = enemyIdForGroup.toByte()

        //> lda $02
        //> sta Enemy_PageLoc,x       ;store page location for enemy object
        ram.sprObjPageLoc[1 + slot] = pageStore.toByte()

        //> lda $03
        //> sta Enemy_X_Position,x    ;store x coordinate for enemy object
        ram.sprObjXPos[1 + slot] = xStore.toByte()

        //> clc
        //> adc #$18                  ;add 24 pixels for next enemy
        //> sta $03
        val nextX = xStore + 0x18
        xStore = nextX and 0xFF

        //> lda $02                   ;add carry to page location for
        //> adc #$00                  ;next enemy
        //> sta $02
        if (nextX > 0xFF) pageStore = (pageStore + 1) and 0xFF

        //> lda $00                   ;store y coordinate for enemy object
        //> sta Enemy_Y_Position,x
        ram.sprObjYPos[1 + slot] = yCoord.toByte()

        //> lda #$01                  ;activate flag for buffer, and
        //> sta Enemy_Y_HighPos,x     ;put enemy within the screen vertically
        ram.sprObjYHighPos[1 + slot] = 1

        //> sta Enemy_Flag,x
        ram.enemyFlags[slot] = 1

        //> jsr CheckpointEnemyID     ;process each enemy object separately
        val savedOffset = ram.objectOffset
        ram.objectOffset = slot.toByte()
        checkpointEnemyID()
        ram.objectOffset = savedOffset

        //> dec NumberofGroupEnemies  ;do this until we run out of enemy objects
        numEnemies--
        ram.numberofGroupEnemies = numEnemies.toByte()
        //> bne GrLoop
    }

    //> NextED: jmp Inc2B             ;jump to increment data offset and leave
    // Note: Inc2B is handled by the caller (processEnemyData) via advanceEnemyDataOffset
}

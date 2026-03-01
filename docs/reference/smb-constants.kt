@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.decompiler6502tokotlin.smb

import com.ivieleague.decompiler6502tokotlin.codegen.MemoryByte

// Memory address variables from smbdism.asm
// Each variable delegates to a specific memory location using property delegates
// Access: `operMode.toInt()` reads from memory[0x0770]
// Store: `operMode = 5u` writes to memory[0x0770]

const val A_B_Buttons = 0x0A
var aBButtons by MemoryByte(A_B_Buttons)
const val A_Button = 0x80
var aButton by MemoryByte(A_Button)
const val AirBubbleTimer = 0x0792
var airBubbleTimer by MemoryByte(AirBubbleTimer)
const val AltEntranceControl = 0x0752
var altEntranceControl by MemoryByte(AltEntranceControl)
const val AltGameOverMusic = 0x10
var altGameOverMusic by MemoryByte(AltGameOverMusic)
const val AltRegContentFlag = 0x07CA
var altRegContentFlag by MemoryByte(AltRegContentFlag)
const val Alt_SprDataOffset = 0x06EC
var altSprDataOffset by MemoryByte(Alt_SprDataOffset)
const val AreaAddrsLOffset = 0x074F
var areaAddrsLOffset by MemoryByte(AreaAddrsLOffset)
const val AreaData = 0xE7
var areaData by MemoryByte(AreaData)
const val AreaDataHigh = 0xE8
var areaDataHigh by MemoryByte(AreaDataHigh)
const val AreaDataLow = 0xE7
var areaDataLow by MemoryByte(AreaDataLow)
const val AreaDataOffset = 0x072C
var areaDataOffset by MemoryByte(AreaDataOffset)
const val AreaMusicBuffer = 0xF4
var areaMusicBuffer by MemoryByte(AreaMusicBuffer)
const val AreaMusicBuffer_Alt = 0x07C5
var areaMusicBufferAlt by MemoryByte(AreaMusicBuffer_Alt)
const val AreaMusicQueue = 0xFB
var areaMusicQueue by MemoryByte(AreaMusicQueue)
const val AreaNumber = 0x0760
var areaNumber by MemoryByte(AreaNumber)
const val AreaObjOffsetBuffer = 0x072D
var areaObjOffsetBuffer by MemoryByte(AreaObjOffsetBuffer)
const val AreaObjectHeight = 0x0735
var areaObjectHeight by MemoryByte(AreaObjectHeight)
const val AreaObjectLength = 0x0730
var areaObjectLength by MemoryByte(AreaObjectLength)
const val AreaObjectPageLoc = 0x072A
var areaObjectPageLoc by MemoryByte(AreaObjectPageLoc)
const val AreaObjectPageSel = 0x072B
var areaObjectPageSel by MemoryByte(AreaObjectPageSel)
const val AreaParserTaskNum = 0x071F
var areaParserTaskNum by MemoryByte(AreaParserTaskNum)
const val AreaPointer = 0x0750
var areaPointer by MemoryByte(AreaPointer)
const val AreaStyle = 0x0733
var areaStyle by MemoryByte(AreaStyle)
const val AreaType = 0x074E
var areaType by MemoryByte(AreaType)
const val AttributeBuffer = 0x03F9
var attributeBuffer by MemoryByte(AttributeBuffer)
const val BBill_CCheep_Frenzy = 0x17
var bBillCCheepFrenzy by MemoryByte(BBill_CCheep_Frenzy)
const val B_Button = 0x40
var bButton by MemoryByte(B_Button)
const val BackgroundColorCtrl = 0x0744
var backgroundColorCtrl by MemoryByte(BackgroundColorCtrl)
const val BackgroundScenery = 0x0742
var backgroundScenery by MemoryByte(BackgroundScenery)
const val BackloadingFlag = 0x0728
var backloadingFlag by MemoryByte(BackloadingFlag)
const val BalPlatformAlignment = 0x03A0
var balPlatformAlignment by MemoryByte(BalPlatformAlignment)
const val BehindAreaParserFlag = 0x0729
var behindAreaParserFlag by MemoryByte(BehindAreaParserFlag)
const val BitMFilter = 0x06DD
var bitMFilter by MemoryByte(BitMFilter)
const val BlockBounceTimer = 0x0784
var blockBounceTimer by MemoryByte(BlockBounceTimer)
const val BlockBufferColumnPos = 0x06A0
var blockBufferColumnPos by MemoryByte(BlockBufferColumnPos)
const val Block_BBuf_Low = 0x03E6
var blockBBufLow by MemoryByte(Block_BBuf_Low)
const val Block_Buffer_1 = 0x0500
var blockBuffer1 by MemoryByte(Block_Buffer_1)
const val Block_Buffer_2 = 0x05D0
var blockBuffer2 by MemoryByte(Block_Buffer_2)
const val Block_Metatile = 0x03E8
var blockMetatile by MemoryByte(Block_Metatile)
const val Block_OffscreenBits = 0x03D4
var blockOffscreenBits by MemoryByte(Block_OffscreenBits)
const val Block_Orig_XPos = 0x03F1
var blockOrigXPos by MemoryByte(Block_Orig_XPos)
const val Block_Orig_YPos = 0x03E4
var blockOrigYPos by MemoryByte(Block_Orig_YPos)
const val Block_PageLoc = 0x76
var blockPageLoc by MemoryByte(Block_PageLoc)
const val Block_PageLoc2 = 0x03EA
var blockPageLoc2 by MemoryByte(Block_PageLoc2)
const val Block_Rel_XPos = 0x03B1
var blockRelXPos by MemoryByte(Block_Rel_XPos)
const val Block_Rel_YPos = 0x03BC
var blockRelYPos by MemoryByte(Block_Rel_YPos)
const val Block_RepFlag = 0x03EC
var blockRepFlag by MemoryByte(Block_RepFlag)
const val Block_ResidualCounter = 0x03F0
var blockResidualCounter by MemoryByte(Block_ResidualCounter)
const val Block_SprDataOffset = 0x06EC
var blockSprDataOffset by MemoryByte(Block_SprDataOffset)
const val Block_State = 0x26
var blockState by MemoryByte(Block_State)
const val Block_X_Position = 0x8F
var blockXPosition by MemoryByte(Block_X_Position)
const val Block_X_Speed = 0x60
var blockXSpeed by MemoryByte(Block_X_Speed)
const val Block_Y_HighPos = 0xBE
var blockYHighPos by MemoryByte(Block_Y_HighPos)
const val Block_Y_MoveForce = 0x043C
var blockYMoveForce by MemoryByte(Block_Y_MoveForce)
const val Block_Y_Position = 0xD7
var blockYPosition by MemoryByte(Block_Y_Position)
const val Block_Y_Speed = 0xA8
var blockYSpeed by MemoryByte(Block_Y_Speed)
const val Bloober = 0x07
var bloober by MemoryByte(Bloober)
const val BlooperMoveCounter = 0xA0
var blooperMoveCounter by MemoryByte(BlooperMoveCounter)
const val BlooperMoveSpeed = 0x58
var blooperMoveSpeed by MemoryByte(BlooperMoveSpeed)
const val BoundingBox_DR_XPos = 0x04AE
var boundingBoxDRXPos by MemoryByte(BoundingBox_DR_XPos)
const val BoundingBox_DR_YPos = 0x04AF
var boundingBoxDRYPos by MemoryByte(BoundingBox_DR_YPos)
const val BoundingBox_LR_Corner = 0x04AE
var boundingBoxLRCorner by MemoryByte(BoundingBox_LR_Corner)
const val BoundingBox_UL_Corner = 0x04AC
var boundingBoxULCorner by MemoryByte(BoundingBox_UL_Corner)
const val BoundingBox_UL_XPos = 0x04AC
var boundingBoxULXPos by MemoryByte(BoundingBox_UL_XPos)
const val BoundingBox_UL_YPos = 0x04AD
var boundingBoxULYPos by MemoryByte(BoundingBox_UL_YPos)
const val Bowser = 0x2D
var bowser by MemoryByte(Bowser)
const val BowserBodyControls = 0x0363
var bowserBodyControls by MemoryByte(BowserBodyControls)
const val BowserFeetCounter = 0x0364
var bowserFeetCounter by MemoryByte(BowserFeetCounter)
const val BowserFireBreathTimer = 0x0790
var bowserFireBreathTimer by MemoryByte(BowserFireBreathTimer)
const val BowserFlame = 0x15
var bowserFlame by MemoryByte(BowserFlame)
const val BowserFlamePRandomOfs = 0x0417
var bowserFlamePRandomOfs by MemoryByte(BowserFlamePRandomOfs)
const val BowserFlameTimerCtrl = 0x0367
var bowserFlameTimerCtrl by MemoryByte(BowserFlameTimerCtrl)
const val BowserFront_Offset = 0x0368
var bowserFrontOffset by MemoryByte(BowserFront_Offset)
const val BowserGfxFlag = 0x036A
var bowserGfxFlag by MemoryByte(BowserGfxFlag)
const val BowserHitPoints = 0x0483
var bowserHitPoints by MemoryByte(BowserHitPoints)
const val BowserMovementSpeed = 0x0365
var bowserMovementSpeed by MemoryByte(BowserMovementSpeed)
const val BowserOrigXPos = 0x0366
var bowserOrigXPos by MemoryByte(BowserOrigXPos)
const val BrickCoinTimer = 0x079D
var brickCoinTimer by MemoryByte(BrickCoinTimer)
const val BrickCoinTimerFlag = 0x06BC
var brickCoinTimerFlag by MemoryByte(BrickCoinTimerFlag)
const val BridgeCollapseOffset = 0x0369
var bridgeCollapseOffset by MemoryByte(BridgeCollapseOffset)
const val Bubble_OffscreenBits = 0x03D3
var bubbleOffscreenBits by MemoryByte(Bubble_OffscreenBits)
const val Bubble_PageLoc = 0x83
var bubblePageLoc by MemoryByte(Bubble_PageLoc)
const val Bubble_Rel_XPos = 0x03B0
var bubbleRelXPos by MemoryByte(Bubble_Rel_XPos)
const val Bubble_Rel_YPos = 0x03BB
var bubbleRelYPos by MemoryByte(Bubble_Rel_YPos)
const val Bubble_SprDataOffset = 0x06EE
var bubbleSprDataOffset by MemoryByte(Bubble_SprDataOffset)
const val Bubble_X_Position = 0x9C
var bubbleXPosition by MemoryByte(Bubble_X_Position)
const val Bubble_YMF_Dummy = 0x042C
var bubbleYMFDummy by MemoryByte(Bubble_YMF_Dummy)
const val Bubble_Y_HighPos = 0xCB
var bubbleYHighPos by MemoryByte(Bubble_Y_HighPos)
const val Bubble_Y_Position = 0xE4
var bubbleYPosition by MemoryByte(Bubble_Y_Position)
const val BulletBill_CannonVar = 0x33
var bulletBillCannonVar by MemoryByte(BulletBill_CannonVar)
const val BulletBill_FrenzyVar = 0x08
var bulletBillFrenzyVar by MemoryByte(BulletBill_FrenzyVar)
const val BuzzyBeetle = 0x02
var buzzyBeetle by MemoryByte(BuzzyBeetle)
const val Cannon_Offset = 0x046A
var cannonOffset by MemoryByte(Cannon_Offset)
const val Cannon_PageLoc = 0x046B
var cannonPageLoc by MemoryByte(Cannon_PageLoc)
const val Cannon_Timer = 0x047D
var cannonTimer by MemoryByte(Cannon_Timer)
const val Cannon_X_Position = 0x0471
var cannonXPosition by MemoryByte(Cannon_X_Position)
const val Cannon_Y_Position = 0x0477
var cannonYPosition by MemoryByte(Cannon_Y_Position)
const val CastleMusic = 0x08
var castleMusic by MemoryByte(CastleMusic)
const val ChangeAreaTimer = 0x06DE
var changeAreaTimer by MemoryByte(ChangeAreaTimer)
const val CheepCheepMoveMFlag = 0x58
var cheepCheepMoveMFlag by MemoryByte(CheepCheepMoveMFlag)
const val CheepCheepOrigYPos = 0x0434
var cheepCheepOrigYPos by MemoryByte(CheepCheepOrigYPos)
const val ClimbSideTimer = 0x0789
var climbSideTimer by MemoryByte(ClimbSideTimer)
const val CloudMusic = 0x10
var cloudMusic by MemoryByte(CloudMusic)
const val CloudTypeOverride = 0x0743
var cloudTypeOverride by MemoryByte(CloudTypeOverride)
const val CoinTally = 0x075E
var coinTally by MemoryByte(CoinTally)
const val CoinTallyFor1Ups = 0x0748
var coinTallyFor1Ups by MemoryByte(CoinTallyFor1Ups)
const val ColorRotateOffset = 0x06D4
var colorRotateOffset by MemoryByte(ColorRotateOffset)
const val ColumnSets = 0x071E
var columnSets by MemoryByte(ColumnSets)
const val ContinueWorld = 0x07FD
var continueWorld by MemoryByte(ContinueWorld)
const val CrouchingFlag = 0x0714
var crouchingFlag by MemoryByte(CrouchingFlag)
const val CurrentColumnPos = 0x0726
var currentColumnPos by MemoryByte(CurrentColumnPos)
const val CurrentNTAddr_High = 0x0720
var currentNTAddrHigh by MemoryByte(CurrentNTAddr_High)
const val CurrentNTAddr_Low = 0x0721
var currentNTAddrLow by MemoryByte(CurrentNTAddr_Low)
const val CurrentPageLoc = 0x0725
var currentPageLoc by MemoryByte(CurrentPageLoc)
const val CurrentPlayer = 0x0753
var currentPlayer by MemoryByte(CurrentPlayer)
const val DAC_Counter = 0x07C0
var dACCounter by MemoryByte(DAC_Counter)
const val DeathMusic = 0x01
var deathMusic by MemoryByte(DeathMusic)
const val DeathMusicLoaded = 0x0712
var deathMusicLoaded by MemoryByte(DeathMusicLoaded)
const val DemoAction = 0x0717
var demoAction by MemoryByte(DemoAction)
const val DemoActionTimer = 0x0718
var demoActionTimer by MemoryByte(DemoActionTimer)
const val DemoTimer = 0x07A2
var demoTimer by MemoryByte(DemoTimer)
const val DestinationPageLoc = 0x34
var destinationPageLoc by MemoryByte(DestinationPageLoc)
const val DiffToHaltJump = 0x0706
var diffToHaltJump by MemoryByte(DiffToHaltJump)
const val DigitModifier = 0x0134
var digitModifier by MemoryByte(DigitModifier)
const val DisableCollisionDet = 0x0716
var disableCollisionDet by MemoryByte(DisableCollisionDet)
const val DisableIntermediate = 0x0769
var disableIntermediate by MemoryByte(DisableIntermediate)
const val DisableScreenFlag = 0x0774
var disableScreenFlag by MemoryByte(DisableScreenFlag)
const val DisplayDigits = 0x07D7
var displayDigits by MemoryByte(DisplayDigits)
const val Down_Dir = 0x04
var downDir by MemoryByte(Down_Dir)
const val DuplicateObj_Offset = 0x06CF
var duplicateObjOffset by MemoryByte(DuplicateObj_Offset)
const val EndOfCastleMusic = 0x08
var endOfCastleMusic by MemoryByte(EndOfCastleMusic)
const val EndOfLevelMusic = 0x20
var endOfLevelMusic by MemoryByte(EndOfLevelMusic)
const val EnemyBoundingBoxCoord = 0x04B0
var enemyBoundingBoxCoord by MemoryByte(EnemyBoundingBoxCoord)
const val EnemyData = 0xE9
var enemyData by MemoryByte(EnemyData)
const val EnemyDataHigh = 0xEA
var enemyDataHigh by MemoryByte(EnemyDataHigh)
const val EnemyDataLow = 0xE9
var enemyDataLow by MemoryByte(EnemyDataLow)
const val EnemyDataOffset = 0x0739
var enemyDataOffset by MemoryByte(EnemyDataOffset)
const val EnemyFrameTimer = 0x078A
var enemyFrameTimer by MemoryByte(EnemyFrameTimer)
const val EnemyFrenzyBuffer = 0x06CB
var enemyFrenzyBuffer by MemoryByte(EnemyFrenzyBuffer)
const val EnemyFrenzyQueue = 0x06CD
var enemyFrenzyQueue by MemoryByte(EnemyFrenzyQueue)
const val EnemyIntervalTimer = 0x0796
var enemyIntervalTimer by MemoryByte(EnemyIntervalTimer)
const val EnemyObjectPageLoc = 0x073A
var enemyObjectPageLoc by MemoryByte(EnemyObjectPageLoc)
const val EnemyObjectPageSel = 0x073B
var enemyObjectPageSel by MemoryByte(EnemyObjectPageSel)
const val EnemyOffscrBitsMasked = 0x03D8
var enemyOffscrBitsMasked by MemoryByte(EnemyOffscrBitsMasked)
const val Enemy_BoundBoxCtrl = 0x049A
var enemyBoundBoxCtrl by MemoryByte(Enemy_BoundBoxCtrl)
const val Enemy_CollisionBits = 0x0491
var enemyCollisionBits by MemoryByte(Enemy_CollisionBits)
const val Enemy_Flag = 0x0F
var enemyFlag by MemoryByte(Enemy_Flag)
const val Enemy_ID = 0x16
var enemyID by MemoryByte(Enemy_ID)
const val Enemy_MovingDir = 0x46
var enemyMovingDir by MemoryByte(Enemy_MovingDir)
const val Enemy_OffscreenBits = 0x03D1
var enemyOffscreenBits by MemoryByte(Enemy_OffscreenBits)
const val Enemy_PageLoc = 0x6E
var enemyPageLoc by MemoryByte(Enemy_PageLoc)
const val Enemy_Rel_XPos = 0x03AE
var enemyRelXPos by MemoryByte(Enemy_Rel_XPos)
const val Enemy_Rel_YPos = 0x03B9
var enemyRelYPos by MemoryByte(Enemy_Rel_YPos)
const val Enemy_SprAttrib = 0x03C5
var enemySprAttrib by MemoryByte(Enemy_SprAttrib)
const val Enemy_SprDataOffset = 0x06E5
var enemySprDataOffset by MemoryByte(Enemy_SprDataOffset)
const val Enemy_State = 0x1E
var enemyState by MemoryByte(Enemy_State)
const val Enemy_X_MoveForce = 0x0401
var enemyXMoveForce by MemoryByte(Enemy_X_MoveForce)
const val Enemy_X_Position = 0x87
var enemyXPosition by MemoryByte(Enemy_X_Position)
const val Enemy_X_Speed = 0x58
var enemyXSpeed by MemoryByte(Enemy_X_Speed)
const val Enemy_YMF_Dummy = 0x0417
var enemyYMFDummy by MemoryByte(Enemy_YMF_Dummy)
const val Enemy_Y_HighPos = 0xB6
var enemyYHighPos by MemoryByte(Enemy_Y_HighPos)
const val Enemy_Y_MoveForce = 0x0434
var enemyYMoveForce by MemoryByte(Enemy_Y_MoveForce)
const val Enemy_Y_Position = 0xCF
var enemyYPosition by MemoryByte(Enemy_Y_Position)
const val Enemy_Y_Speed = 0xA0
var enemyYSpeed by MemoryByte(Enemy_Y_Speed)
const val EntrancePage = 0x0751
var entrancePage by MemoryByte(EntrancePage)
const val EventMusicBuffer = 0x07B1
var eventMusicBuffer by MemoryByte(EventMusicBuffer)
const val EventMusicQueue = 0xFC
var eventMusicQueue by MemoryByte(EventMusicQueue)
const val ExplosionGfxCounter = 0x58
var explosionGfxCounter by MemoryByte(ExplosionGfxCounter)
const val ExplosionTimerCounter = 0xA0
var explosionTimerCounter by MemoryByte(ExplosionTimerCounter)
const val FBall_OffscreenBits = 0x03D2
var fBallOffscreenBits by MemoryByte(FBall_OffscreenBits)
const val FBall_SprDataOffset = 0x06F1
var fBallSprDataOffset by MemoryByte(FBall_SprDataOffset)
const val FetchNewGameTimerFlag = 0x0757
var fetchNewGameTimerFlag by MemoryByte(FetchNewGameTimerFlag)
const val FireballBouncingFlag = 0x3A
var fireballBouncingFlag by MemoryByte(FireballBouncingFlag)
const val FireballCounter = 0x06CE
var fireballCounter by MemoryByte(FireballCounter)
const val FireballThrowingTimer = 0x0711
var fireballThrowingTimer by MemoryByte(FireballThrowingTimer)
const val Fireball_BoundBoxCtrl = 0x04A0
var fireballBoundBoxCtrl by MemoryByte(Fireball_BoundBoxCtrl)
const val Fireball_PageLoc = 0x74
var fireballPageLoc by MemoryByte(Fireball_PageLoc)
const val Fireball_Rel_XPos = 0x03AF
var fireballRelXPos by MemoryByte(Fireball_Rel_XPos)
const val Fireball_Rel_YPos = 0x03BA
var fireballRelYPos by MemoryByte(Fireball_Rel_YPos)
const val Fireball_State = 0x24
var fireballState by MemoryByte(Fireball_State)
const val Fireball_X_Position = 0x8D
var fireballXPosition by MemoryByte(Fireball_X_Position)
const val Fireball_X_Speed = 0x5E
var fireballXSpeed by MemoryByte(Fireball_X_Speed)
const val Fireball_Y_HighPos = 0xBC
var fireballYHighPos by MemoryByte(Fireball_Y_HighPos)
const val Fireball_Y_Position = 0xD5
var fireballYPosition by MemoryByte(Fireball_Y_Position)
const val Fireball_Y_Speed = 0xA6
var fireballYSpeed by MemoryByte(Fireball_Y_Speed)
const val FirebarSpinDirection = 0x34
var firebarSpinDirection by MemoryByte(FirebarSpinDirection)
const val FirebarSpinSpeed = 0x0388
var firebarSpinSpeed by MemoryByte(FirebarSpinSpeed)
const val FirebarSpinState_High = 0xA0
var firebarSpinStateHigh by MemoryByte(FirebarSpinState_High)
const val FirebarSpinState_Low = 0x58
var firebarSpinStateLow by MemoryByte(FirebarSpinState_Low)
const val Fireworks = 0x16
var fireworks by MemoryByte(Fireworks)
const val FireworksCounter = 0x06D7
var fireworksCounter by MemoryByte(FireworksCounter)
const val FlagpoleCollisionYPos = 0x070F
var flagpoleCollisionYPos by MemoryByte(FlagpoleCollisionYPos)
const val FlagpoleFNum_YMFDummy = 0x010E
var flagpoleFNumYMFDummy by MemoryByte(FlagpoleFNum_YMFDummy)
const val FlagpoleFNum_Y_Pos = 0x010D
var flagpoleFNumYPos by MemoryByte(FlagpoleFNum_Y_Pos)
const val FlagpoleFlagObject = 0x30
var flagpoleFlagObject by MemoryByte(FlagpoleFlagObject)
const val FlagpoleScore = 0x010F
var flagpoleScore by MemoryByte(FlagpoleScore)
const val FlagpoleSoundQueue = 0x0713
var flagpoleSoundQueue by MemoryByte(FlagpoleSoundQueue)
const val FloateyNum_Control = 0x0110
var floateyNumControl by MemoryByte(FloateyNum_Control)
const val FloateyNum_Timer = 0x012C
var floateyNumTimer by MemoryByte(FloateyNum_Timer)
const val FloateyNum_X_Pos = 0x0117
var floateyNumXPos by MemoryByte(FloateyNum_X_Pos)
const val FloateyNum_Y_Pos = 0x011E
var floateyNumYPos by MemoryByte(FloateyNum_Y_Pos)
const val FlyCheepCheepFrenzy = 0x14
var flyCheepCheepFrenzy by MemoryByte(FlyCheepCheepFrenzy)
const val FlyingCheepCheep = 0x14
var flyingCheepCheep by MemoryByte(FlyingCheepCheep)
const val ForegroundScenery = 0x0741
var foregroundScenery by MemoryByte(ForegroundScenery)
const val FrameCounter = 0x09
var frameCounter by MemoryByte(FrameCounter)
const val FrenzyEnemyTimer = 0x078F
var frenzyEnemyTimer by MemoryByte(FrenzyEnemyTimer)
const val FrictionAdderHigh = 0x0701
var frictionAdderHigh by MemoryByte(FrictionAdderHigh)
const val FrictionAdderLow = 0x0702
var frictionAdderLow by MemoryByte(FrictionAdderLow)
const val GameEngineSubroutine = 0x0E
var gameEngineSubroutine by MemoryByte(GameEngineSubroutine)
const val GameModeValue = 0x01
var gameModeValue by MemoryByte(GameModeValue)
const val GameOverModeValue = 0x03
var gameOverModeValue by MemoryByte(GameOverModeValue)
const val GameOverMusic = 0x02
var gameOverMusic by MemoryByte(GameOverMusic)
const val GamePauseStatus = 0x0776
var gamePauseStatus by MemoryByte(GamePauseStatus)
const val GamePauseTimer = 0x0777
var gamePauseTimer by MemoryByte(GamePauseTimer)
const val GameTimerCtrlTimer = 0x0787
var gameTimerCtrlTimer by MemoryByte(GameTimerCtrlTimer)
const val GameTimerDisplay = 0x07F8
var gameTimerDisplay by MemoryByte(GameTimerDisplay)
const val GameTimerExpiredFlag = 0x0759
var gameTimerExpiredFlag by MemoryByte(GameTimerExpiredFlag)
const val GameTimerSetting = 0x0715
var gameTimerSetting by MemoryByte(GameTimerSetting)
const val Goomba = 0x06
var goomba by MemoryByte(Goomba)
const val GreenKoopa = 0x00
var greenKoopa by MemoryByte(GreenKoopa)
const val GreenParatroopaFly = 0x10
var greenParatroopaFly by MemoryByte(GreenParatroopaFly)
const val GreenParatroopaJump = 0x0E
var greenParatroopaJump by MemoryByte(GreenParatroopaJump)
const val GreyCheepCheep = 0x0A
var greyCheepCheep by MemoryByte(GreyCheepCheep)
const val GroundMusic = 0x01
var groundMusic by MemoryByte(GroundMusic)
const val GroundMusicHeaderOfs = 0x07C7
var groundMusicHeaderOfs by MemoryByte(GroundMusicHeaderOfs)
const val HalfwayPage = 0x075B
var halfwayPage by MemoryByte(HalfwayPage)
const val HammerBro = 0x05
var hammerBro by MemoryByte(HammerBro)
const val HammerBroJumpTimer = 0x3C
var hammerBroJumpTimer by MemoryByte(HammerBroJumpTimer)
const val HammerEnemyOffset = 0x06AE
var hammerEnemyOffset by MemoryByte(HammerEnemyOffset)
const val HammerThrowingTimer = 0x03A2
var hammerThrowingTimer by MemoryByte(HammerThrowingTimer)
const val Hidden1UpFlag = 0x075D
var hidden1UpFlag by MemoryByte(Hidden1UpFlag)
const val HorizontalScroll = 0x073F
var horizontalScroll by MemoryByte(HorizontalScroll)
const val InjuryTimer = 0x079E
var injuryTimer by MemoryByte(InjuryTimer)
const val IntervalTimerControl = 0x077F
var intervalTimerControl by MemoryByte(IntervalTimerControl)
const val JOYPAD_PORT = 0x4016
var jOYPADPORT by MemoryByte(JOYPAD_PORT)
const val JOYPAD_PORT1 = 0x4016
var jOYPADPORT1 by MemoryByte(JOYPAD_PORT1)
const val JOYPAD_PORT2 = 0x4017
var jOYPADPORT2 by MemoryByte(JOYPAD_PORT2)
const val JoypadBitMask = 0x074A
var joypadBitMask by MemoryByte(JoypadBitMask)
const val JoypadOverride = 0x0758
var joypadOverride by MemoryByte(JoypadOverride)
const val JumpCoinMiscOffset = 0x06B7
var jumpCoinMiscOffset by MemoryByte(JumpCoinMiscOffset)
const val JumpOrigin_Y_HighPos = 0x0707
var jumpOriginYHighPos by MemoryByte(JumpOrigin_Y_HighPos)
const val JumpOrigin_Y_Position = 0x0708
var jumpOriginYPosition by MemoryByte(JumpOrigin_Y_Position)
const val JumpSwimTimer = 0x0782
var jumpSwimTimer by MemoryByte(JumpSwimTimer)
const val JumpspringAnimCtrl = 0x070E
var jumpspringAnimCtrl by MemoryByte(JumpspringAnimCtrl)
const val JumpspringForce = 0x06DB
var jumpspringForce by MemoryByte(JumpspringForce)
const val JumpspringObject = 0x32
var jumpspringObject by MemoryByte(JumpspringObject)
const val JumpspringTimer = 0x0786
var jumpspringTimer by MemoryByte(JumpspringTimer)
const val Jumpspring_FixedYPos = 0x58
var jumpspringFixedYPos by MemoryByte(Jumpspring_FixedYPos)
const val Lakitu = 0x11
var lakitu by MemoryByte(Lakitu)
const val LakituMoveDirection = 0xA0
var lakituMoveDirection by MemoryByte(LakituMoveDirection)
const val LakituMoveSpeed = 0x58
var lakituMoveSpeed by MemoryByte(LakituMoveSpeed)
const val LakituReappearTimer = 0x06D1
var lakituReappearTimer by MemoryByte(LakituReappearTimer)
const val Left_Dir = 0x02
var leftDir by MemoryByte(Left_Dir)
const val Left_Right_Buttons = 0x0C
var leftRightButtons by MemoryByte(Left_Right_Buttons)
const val Level1 = 0x00
var level1 by MemoryByte(Level1)
const val Level2 = 0x01
var level2 by MemoryByte(Level2)
const val Level3 = 0x02
var level3 by MemoryByte(Level3)
const val Level4 = 0x03
var level4 by MemoryByte(Level4)
const val LevelNumber = 0x075C
var levelNumber by MemoryByte(LevelNumber)
const val LoopCommand = 0x0745
var loopCommand by MemoryByte(LoopCommand)
const val MaxRangeFromOrigin = 0x06DC
var maxRangeFromOrigin by MemoryByte(MaxRangeFromOrigin)
const val MaximumLeftSpeed = 0x0450
var maximumLeftSpeed by MemoryByte(MaximumLeftSpeed)
const val MaximumRightSpeed = 0x0456
var maximumRightSpeed by MemoryByte(MaximumRightSpeed)
const val MetatileBuffer = 0x06A1
var metatileBuffer by MemoryByte(MetatileBuffer)
const val Mirror_PPU_CTRL_REG1 = 0x0778
var mirrorPPUCTRLREG1 by MemoryByte(Mirror_PPU_CTRL_REG1)
const val Mirror_PPU_CTRL_REG2 = 0x0779
var mirrorPPUCTRLREG2 by MemoryByte(Mirror_PPU_CTRL_REG2)
const val Misc_BoundBoxCtrl = 0x04A2
var miscBoundBoxCtrl by MemoryByte(Misc_BoundBoxCtrl)
const val Misc_Collision_Flag = 0x06BE
var miscCollisionFlag by MemoryByte(Misc_Collision_Flag)
const val Misc_OffscreenBits = 0x03D6
var miscOffscreenBits by MemoryByte(Misc_OffscreenBits)
const val Misc_PageLoc = 0x7A
var miscPageLoc by MemoryByte(Misc_PageLoc)
const val Misc_Rel_XPos = 0x03B3
var miscRelXPos by MemoryByte(Misc_Rel_XPos)
const val Misc_Rel_YPos = 0x03BE
var miscRelYPos by MemoryByte(Misc_Rel_YPos)
const val Misc_SprDataOffset = 0x06F3
var miscSprDataOffset by MemoryByte(Misc_SprDataOffset)
const val Misc_State = 0x2A
var miscState by MemoryByte(Misc_State)
const val Misc_X_Position = 0x93
var miscXPosition by MemoryByte(Misc_X_Position)
const val Misc_X_Speed = 0x64
var miscXSpeed by MemoryByte(Misc_X_Speed)
const val Misc_Y_HighPos = 0xC2
var miscYHighPos by MemoryByte(Misc_Y_HighPos)
const val Misc_Y_Position = 0xDB
var miscYPosition by MemoryByte(Misc_Y_Position)
const val Misc_Y_Speed = 0xAC
var miscYSpeed by MemoryByte(Misc_Y_Speed)
const val MultiLoopCorrectCntr = 0x06D9
var multiLoopCorrectCntr by MemoryByte(MultiLoopCorrectCntr)
const val MultiLoopPassCntr = 0x06DA
var multiLoopPassCntr by MemoryByte(MultiLoopPassCntr)
const val MushroomLedgeHalfLen = 0x0736
var mushroomLedgeHalfLen by MemoryByte(MushroomLedgeHalfLen)
const val MusicData = 0xF5
var musicData by MemoryByte(MusicData)
const val MusicDataHigh = 0xF6
var musicDataHigh by MemoryByte(MusicDataHigh)
const val MusicDataLow = 0xF5
var musicDataLow by MemoryByte(MusicDataLow)
const val MusicOffset_Noise = 0x07B0
var musicOffsetNoise by MemoryByte(MusicOffset_Noise)
const val MusicOffset_Square1 = 0xF8
var musicOffsetSquare1 by MemoryByte(MusicOffset_Square1)
const val MusicOffset_Square2 = 0xF7
var musicOffsetSquare2 by MemoryByte(MusicOffset_Square2)
const val MusicOffset_Triangle = 0xF9
var musicOffsetTriangle by MemoryByte(MusicOffset_Triangle)
const val NoiseDataLoopbackOfs = 0x07C1
var noiseDataLoopbackOfs by MemoryByte(NoiseDataLoopbackOfs)
const val NoiseSoundBuffer = 0xF3
var noiseSoundBuffer by MemoryByte(NoiseSoundBuffer)
const val NoiseSoundQueue = 0xFD
var noiseSoundQueue by MemoryByte(NoiseSoundQueue)
const val Noise_BeatLenCounter = 0x07BA
var noiseBeatLenCounter by MemoryByte(Noise_BeatLenCounter)
const val Noise_SfxLenCounter = 0x07BF
var noiseSfxLenCounter by MemoryByte(Noise_SfxLenCounter)
const val NoteLenLookupTblOfs = 0xF0
var noteLenLookupTblOfs by MemoryByte(NoteLenLookupTblOfs)
const val NoteLengthTblAdder = 0x07C4
var noteLengthTblAdder by MemoryByte(NoteLengthTblAdder)
const val NumberOfPlayers = 0x077A
var numberOfPlayers by MemoryByte(NumberOfPlayers)
const val NumberofGroupEnemies = 0x06D3
var numberofGroupEnemies by MemoryByte(NumberofGroupEnemies)
const val NumberofLives = 0x075A
var numberofLives by MemoryByte(NumberofLives)
const val ObjectOffset = 0x08
var objectOffset by MemoryByte(ObjectOffset)
const val OffScr_AreaNumber = 0x0767
var offScrAreaNumber by MemoryByte(OffScr_AreaNumber)
const val OffScr_CoinTally = 0x0765
var offScrCoinTally by MemoryByte(OffScr_CoinTally)
const val OffScr_HalfwayPage = 0x0762
var offScrHalfwayPage by MemoryByte(OffScr_HalfwayPage)
const val OffScr_Hidden1UpFlag = 0x0764
var offScrHidden1UpFlag by MemoryByte(OffScr_Hidden1UpFlag)
const val OffScr_LevelNumber = 0x0763
var offScrLevelNumber by MemoryByte(OffScr_LevelNumber)
const val OffScr_NumberofLives = 0x0761
var offScrNumberofLives by MemoryByte(OffScr_NumberofLives)
const val OffScr_WorldNumber = 0x0766
var offScrWorldNumber by MemoryByte(OffScr_WorldNumber)
const val OffscreenPlayerInfo = 0x0761
var offscreenPlayerInfo by MemoryByte(OffscreenPlayerInfo)
const val OnscreenPlayerInfo = 0x075A
var onscreenPlayerInfo by MemoryByte(OnscreenPlayerInfo)
const val OperMode = 0x0770
var operMode by MemoryByte(OperMode)
const val OperMode_Task = 0x0772
var operModeTask by MemoryByte(OperMode_Task)
const val PPU_ADDRESS = 0x2006
var pPUADDRESS by MemoryByte(PPU_ADDRESS)
const val PPU_CTRL_REG1 = 0x2000
var pPUCTRLREG1 by MemoryByte(PPU_CTRL_REG1)
const val PPU_CTRL_REG2 = 0x2001
var pPUCTRLREG2 by MemoryByte(PPU_CTRL_REG2)
const val PPU_DATA = 0x2007
var pPUDATA by MemoryByte(PPU_DATA)
const val PPU_SCROLL_REG = 0x2005
var pPUSCROLLREG by MemoryByte(PPU_SCROLL_REG)
const val PPU_SPR_ADDR = 0x2003
var pPUSPRADDR by MemoryByte(PPU_SPR_ADDR)
const val PPU_SPR_DATA = 0x2004
var pPUSPRDATA by MemoryByte(PPU_SPR_DATA)
const val PPU_STATUS = 0x2002
var pPUSTATUS by MemoryByte(PPU_STATUS)
const val PauseModeFlag = 0x07C6
var pauseModeFlag by MemoryByte(PauseModeFlag)
const val PauseSoundBuffer = 0x07B2
var pauseSoundBuffer by MemoryByte(PauseSoundBuffer)
const val PauseSoundQueue = 0xFA
var pauseSoundQueue by MemoryByte(PauseSoundQueue)
const val PipeIntroMusic = 0x20
var pipeIntroMusic by MemoryByte(PipeIntroMusic)
const val PiranhaPlant = 0x0D
var piranhaPlant by MemoryByte(PiranhaPlant)
const val PiranhaPlantDownYPos = 0x0434
var piranhaPlantDownYPos by MemoryByte(PiranhaPlantDownYPos)
const val PiranhaPlantUpYPos = 0x0417
var piranhaPlantUpYPos by MemoryByte(PiranhaPlantUpYPos)
const val PiranhaPlant_MoveFlag = 0xA0
var piranhaPlantMoveFlag by MemoryByte(PiranhaPlant_MoveFlag)
const val PiranhaPlant_Y_Speed = 0x58
var piranhaPlantYSpeed by MemoryByte(PiranhaPlant_Y_Speed)
const val PlatformCollisionFlag = 0x03A2
var platformCollisionFlag by MemoryByte(PlatformCollisionFlag)
const val Platform_X_Scroll = 0x03A1
var platformXScroll by MemoryByte(Platform_X_Scroll)
const val PlayerAnimCtrl = 0x070D
var playerAnimCtrl by MemoryByte(PlayerAnimCtrl)
const val PlayerAnimTimer = 0x0781
var playerAnimTimer by MemoryByte(PlayerAnimTimer)
const val PlayerAnimTimerSet = 0x070C
var playerAnimTimerSet by MemoryByte(PlayerAnimTimerSet)
const val PlayerChangeSizeFlag = 0x070B
var playerChangeSizeFlag by MemoryByte(PlayerChangeSizeFlag)
const val PlayerEntranceCtrl = 0x0710
var playerEntranceCtrl by MemoryByte(PlayerEntranceCtrl)
const val PlayerFacingDir = 0x33
var playerFacingDir by MemoryByte(PlayerFacingDir)
const val PlayerGfxOffset = 0x06D5
var playerGfxOffset by MemoryByte(PlayerGfxOffset)
const val PlayerScoreDisplay = 0x07DD
var playerScoreDisplay by MemoryByte(PlayerScoreDisplay)
const val PlayerSize = 0x0754
var playerSize by MemoryByte(PlayerSize)
const val PlayerStatus = 0x0756
var playerStatus by MemoryByte(PlayerStatus)
const val Player_BoundBoxCtrl = 0x0499
var playerBoundBoxCtrl by MemoryByte(Player_BoundBoxCtrl)
const val Player_CollisionBits = 0x0490
var playerCollisionBits by MemoryByte(Player_CollisionBits)
const val Player_MovingDir = 0x45
var playerMovingDir by MemoryByte(Player_MovingDir)
const val Player_OffscreenBits = 0x03D0
var playerOffscreenBits by MemoryByte(Player_OffscreenBits)
const val Player_PageLoc = 0x6D
var playerPageLoc by MemoryByte(Player_PageLoc)
const val Player_Pos_ForScroll = 0x0755
var playerPosForScroll by MemoryByte(Player_Pos_ForScroll)
const val Player_Rel_XPos = 0x03AD
var playerRelXPos by MemoryByte(Player_Rel_XPos)
const val Player_Rel_YPos = 0x03B8
var playerRelYPos by MemoryByte(Player_Rel_YPos)
const val Player_SprAttrib = 0x03C4
var playerSprAttrib by MemoryByte(Player_SprAttrib)
const val Player_SprDataOffset = 0x06E4
var playerSprDataOffset by MemoryByte(Player_SprDataOffset)
const val Player_State = 0x1D
var playerState by MemoryByte(Player_State)
const val Player_XSpeedAbsolute = 0x0700
var playerXSpeedAbsolute by MemoryByte(Player_XSpeedAbsolute)
const val Player_X_MoveForce = 0x0705
var playerXMoveForce by MemoryByte(Player_X_MoveForce)
const val Player_X_Position = 0x86
var playerXPosition by MemoryByte(Player_X_Position)
const val Player_X_Scroll = 0x06FF
var playerXScroll by MemoryByte(Player_X_Scroll)
const val Player_X_Speed = 0x57
var playerXSpeed by MemoryByte(Player_X_Speed)
const val Player_YMF_Dummy = 0x0416
var playerYMFDummy by MemoryByte(Player_YMF_Dummy)
const val Player_Y_HighPos = 0xB5
var playerYHighPos by MemoryByte(Player_Y_HighPos)
const val Player_Y_MoveForce = 0x0433
var playerYMoveForce by MemoryByte(Player_Y_MoveForce)
const val Player_Y_Position = 0xCE
var playerYPosition by MemoryByte(Player_Y_Position)
const val Player_Y_Speed = 0x9F
var playerYSpeed by MemoryByte(Player_Y_Speed)
const val Podoboo = 0x0C
var podoboo by MemoryByte(Podoboo)
const val PowerUpObject = 0x2E
var powerUpObject by MemoryByte(PowerUpObject)
const val PowerUpType = 0x39
var powerUpType by MemoryByte(PowerUpType)
const val PreviousA_B_Buttons = 0x0D
var previousABButtons by MemoryByte(PreviousA_B_Buttons)
const val PrimaryHardMode = 0x076A
var primaryHardMode by MemoryByte(PrimaryHardMode)
const val PrimaryMsgCounter = 0x0719
var primaryMsgCounter by MemoryByte(PrimaryMsgCounter)
const val PseudoRandomBitReg = 0x07A7
var pseudoRandomBitReg by MemoryByte(PseudoRandomBitReg)
const val RedCheepCheep = 0x0B
var redCheepCheep by MemoryByte(RedCheepCheep)
const val RedKoopa = 0x03
var redKoopa by MemoryByte(RedKoopa)
const val RedPTroopaCenterYPos = 0x58
var redPTroopaCenterYPos by MemoryByte(RedPTroopaCenterYPos)
const val RedPTroopaOrigXPos = 0x0401
var redPTroopaOrigXPos by MemoryByte(RedPTroopaOrigXPos)
const val RedParatroopa = 0x0F
var redParatroopa by MemoryByte(RedParatroopa)
const val RetainerObject = 0x35
var retainerObject by MemoryByte(RetainerObject)
const val Right_Dir = 0x01
var rightDir by MemoryByte(Right_Dir)
const val RunningSpeed = 0x0703
var runningSpeed by MemoryByte(RunningSpeed)
const val RunningTimer = 0x0783
var runningTimer by MemoryByte(RunningTimer)
const val SND_DELTA_REG = 0x4010
var sNDDELTAREG by MemoryByte(SND_DELTA_REG)
const val SND_MASTERCTRL_REG = 0x4015
var sNDMASTERCTRLREG by MemoryByte(SND_MASTERCTRL_REG)
const val SND_NOISE_REG = 0x400C
var sNDNOISEREG by MemoryByte(SND_NOISE_REG)
const val SND_REGISTER = 0x4000
var sNDREGISTER by MemoryByte(SND_REGISTER)
const val SND_SQUARE1_REG = 0x4000
var sNDSQUARE1REG by MemoryByte(SND_SQUARE1_REG)
const val SND_SQUARE2_REG = 0x4004
var sNDSQUARE2REG by MemoryByte(SND_SQUARE2_REG)
const val SND_TRIANGLE_REG = 0x4008
var sNDTRIANGLEREG by MemoryByte(SND_TRIANGLE_REG)
const val SPR_DMA = 0x4014
var sPRDMA by MemoryByte(SPR_DMA)
const val SavedJoypad1Bits = 0x06FC
var savedJoypad1Bits by MemoryByte(SavedJoypad1Bits)
const val SavedJoypad2Bits = 0x06FD
var savedJoypad2Bits by MemoryByte(SavedJoypad2Bits)
const val SavedJoypadBits = 0x06FC
var savedJoypadBits by MemoryByte(SavedJoypadBits)
const val ScoreAndCoinDisplay = 0x07DD
var scoreAndCoinDisplay by MemoryByte(ScoreAndCoinDisplay)
const val ScreenEdge_PageLoc = 0x071A
var screenEdgePageLoc by MemoryByte(ScreenEdge_PageLoc)
const val ScreenEdge_X_Pos = 0x071C
var screenEdgeXPos by MemoryByte(ScreenEdge_X_Pos)
const val ScreenLeft_PageLoc = 0x071A
var screenLeftPageLoc by MemoryByte(ScreenLeft_PageLoc)
const val ScreenLeft_X_Pos = 0x071C
var screenLeftXPos by MemoryByte(ScreenLeft_X_Pos)
const val ScreenRight_PageLoc = 0x071B
var screenRightPageLoc by MemoryByte(ScreenRight_PageLoc)
const val ScreenRight_X_Pos = 0x071D
var screenRightXPos by MemoryByte(ScreenRight_X_Pos)
const val ScreenRoutineTask = 0x073C
var screenRoutineTask by MemoryByte(ScreenRoutineTask)
const val ScreenTimer = 0x07A0
var screenTimer by MemoryByte(ScreenTimer)
const val ScrollAmount = 0x0775
var scrollAmount by MemoryByte(ScrollAmount)
const val ScrollFractional = 0x0768
var scrollFractional by MemoryByte(ScrollFractional)
const val ScrollIntervalTimer = 0x0795
var scrollIntervalTimer by MemoryByte(ScrollIntervalTimer)
const val ScrollLock = 0x0723
var scrollLock by MemoryByte(ScrollLock)
const val ScrollThirtyTwo = 0x073D
var scrollThirtyTwo by MemoryByte(ScrollThirtyTwo)
const val SecondaryHardMode = 0x06CC
var secondaryHardMode by MemoryByte(SecondaryHardMode)
const val SecondaryMsgCounter = 0x0749
var secondaryMsgCounter by MemoryByte(SecondaryMsgCounter)
const val SelectTimer = 0x0780
var selectTimer by MemoryByte(SelectTimer)
const val Select_Button = 0x20
var selectButton by MemoryByte(Select_Button)
const val Sfx_BigJump = 0x01
var sfxBigJump by MemoryByte(Sfx_BigJump)
const val Sfx_Blast = 0x08
var sfxBlast by MemoryByte(Sfx_Blast)
const val Sfx_BowserFall = 0x80
var sfxBowserFall by MemoryByte(Sfx_BowserFall)
const val Sfx_BowserFlame = 0x02
var sfxBowserFlame by MemoryByte(Sfx_BowserFlame)
const val Sfx_BrickShatter = 0x01
var sfxBrickShatter by MemoryByte(Sfx_BrickShatter)
const val Sfx_Bump = 0x02
var sfxBump by MemoryByte(Sfx_Bump)
const val Sfx_CoinGrab = 0x01
var sfxCoinGrab by MemoryByte(Sfx_CoinGrab)
const val Sfx_EnemySmack = 0x08
var sfxEnemySmack by MemoryByte(Sfx_EnemySmack)
const val Sfx_EnemyStomp = 0x04
var sfxEnemyStomp by MemoryByte(Sfx_EnemyStomp)
const val Sfx_ExtraLife = 0x40
var sfxExtraLife by MemoryByte(Sfx_ExtraLife)
const val Sfx_Fireball = 0x20
var sfxFireball by MemoryByte(Sfx_Fireball)
const val Sfx_Flagpole = 0x40
var sfxFlagpole by MemoryByte(Sfx_Flagpole)
const val Sfx_GrowPowerUp = 0x02
var sfxGrowPowerUp by MemoryByte(Sfx_GrowPowerUp)
const val Sfx_GrowVine = 0x04
var sfxGrowVine by MemoryByte(Sfx_GrowVine)
const val Sfx_PipeDown_Injury = 0x10
var sfxPipeDownInjury by MemoryByte(Sfx_PipeDown_Injury)
const val Sfx_PowerUpGrab = 0x20
var sfxPowerUpGrab by MemoryByte(Sfx_PowerUpGrab)
const val Sfx_SecondaryCounter = 0x07BE
var sfxSecondaryCounter by MemoryByte(Sfx_SecondaryCounter)
const val Sfx_SmallJump = 0x80
var sfxSmallJump by MemoryByte(Sfx_SmallJump)
const val Sfx_TimerTick = 0x10
var sfxTimerTick by MemoryByte(Sfx_TimerTick)
const val ShellChainCounter = 0x0125
var shellChainCounter by MemoryByte(ShellChainCounter)
const val SideCollisionTimer = 0x0785
var sideCollisionTimer by MemoryByte(SideCollisionTimer)
const val Silence = 0x80
var silence by MemoryByte(Silence)
const val SoundMemory = 0x07B0
var soundMemory by MemoryByte(SoundMemory)
const val Spiny = 0x12
var spiny by MemoryByte(Spiny)
const val SprDataOffset = 0x06E4
var sprDataOffset by MemoryByte(SprDataOffset)
const val SprDataOffset_Ctrl = 0x03EE
var sprDataOffsetCtrl by MemoryByte(SprDataOffset_Ctrl)
const val SprObj_BoundBoxCtrl = 0x0499
var sprObjBoundBoxCtrl by MemoryByte(SprObj_BoundBoxCtrl)
const val SprObject_OffscrBits = 0x03D0
var sprObjectOffscrBits by MemoryByte(SprObject_OffscrBits)
const val SprObject_PageLoc = 0x6D
var sprObjectPageLoc by MemoryByte(SprObject_PageLoc)
const val SprObject_Rel_XPos = 0x03AD
var sprObjectRelXPos by MemoryByte(SprObject_Rel_XPos)
const val SprObject_Rel_YPos = 0x03B8
var sprObjectRelYPos by MemoryByte(SprObject_Rel_YPos)
const val SprObject_SprAttrib = 0x03C4
var sprObjectSprAttrib by MemoryByte(SprObject_SprAttrib)
const val SprObject_X_MoveForce = 0x0400
var sprObjectXMoveForce by MemoryByte(SprObject_X_MoveForce)
const val SprObject_X_Position = 0x86
var sprObjectXPosition by MemoryByte(SprObject_X_Position)
const val SprObject_X_Speed = 0x57
var sprObjectXSpeed by MemoryByte(SprObject_X_Speed)
const val SprObject_YMF_Dummy = 0x0416
var sprObjectYMFDummy by MemoryByte(SprObject_YMF_Dummy)
const val SprObject_Y_HighPos = 0xB5
var sprObjectYHighPos by MemoryByte(SprObject_Y_HighPos)
const val SprObject_Y_MoveForce = 0x0433
var sprObjectYMoveForce by MemoryByte(SprObject_Y_MoveForce)
const val SprObject_Y_Position = 0xCE
var sprObjectYPosition by MemoryByte(SprObject_Y_Position)
const val SprObject_Y_Speed = 0x9F
var sprObjectYSpeed by MemoryByte(SprObject_Y_Speed)
const val SprShuffleAmt = 0x06E1
var sprShuffleAmt by MemoryByte(SprShuffleAmt)
const val SprShuffleAmtOffset = 0x06E0
var sprShuffleAmtOffset by MemoryByte(SprShuffleAmtOffset)
const val Sprite0HitDetectFlag = 0x0722
var sprite0HitDetectFlag by MemoryByte(Sprite0HitDetectFlag)
const val Sprite_Attributes = 0x0202
var spriteAttributes by MemoryByte(Sprite_Attributes)
const val Sprite_Data = 0x0200
var spriteData by MemoryByte(Sprite_Data)
const val Sprite_Tilenumber = 0x0201
var spriteTilenumber by MemoryByte(Sprite_Tilenumber)
const val Sprite_X_Position = 0x0203
var spriteXPosition by MemoryByte(Sprite_X_Position)
const val Sprite_Y_Position = 0x0200
var spriteYPosition by MemoryByte(Sprite_Y_Position)
const val Squ1_EnvelopeDataCtrl = 0x07B7
var squ1EnvelopeDataCtrl by MemoryByte(Squ1_EnvelopeDataCtrl)
const val Squ1_NoteLenCounter = 0x07B6
var squ1NoteLenCounter by MemoryByte(Squ1_NoteLenCounter)
const val Squ1_SfxLenCounter = 0x07BB
var squ1SfxLenCounter by MemoryByte(Squ1_SfxLenCounter)
const val Squ2_EnvelopeDataCtrl = 0x07B5
var squ2EnvelopeDataCtrl by MemoryByte(Squ2_EnvelopeDataCtrl)
const val Squ2_NoteLenBuffer = 0x07B3
var squ2NoteLenBuffer by MemoryByte(Squ2_NoteLenBuffer)
const val Squ2_NoteLenCounter = 0x07B4
var squ2NoteLenCounter by MemoryByte(Squ2_NoteLenCounter)
const val Squ2_SfxLenCounter = 0x07BD
var squ2SfxLenCounter by MemoryByte(Squ2_SfxLenCounter)
const val Square1SoundBuffer = 0xF1
var square1SoundBuffer by MemoryByte(Square1SoundBuffer)
const val Square1SoundQueue = 0xFF
var square1SoundQueue by MemoryByte(Square1SoundQueue)
const val Square2SoundBuffer = 0xF2
var square2SoundBuffer by MemoryByte(Square2SoundBuffer)
const val Square2SoundQueue = 0xFE
var square2SoundQueue by MemoryByte(Square2SoundQueue)
const val StaircaseControl = 0x0734
var staircaseControl by MemoryByte(StaircaseControl)
const val StarFlagObject = 0x31
var starFlagObject by MemoryByte(StarFlagObject)
const val StarFlagTaskControl = 0x0746
var starFlagTaskControl by MemoryByte(StarFlagTaskControl)
const val StarInvincibleTimer = 0x079F
var starInvincibleTimer by MemoryByte(StarInvincibleTimer)
const val StarPowerMusic = 0x40
var starPowerMusic by MemoryByte(StarPowerMusic)
const val Start_Button = 0x10
var startButton by MemoryByte(Start_Button)
const val StompChainCounter = 0x0484
var stompChainCounter by MemoryByte(StompChainCounter)
const val StompTimer = 0x0791
var stompTimer by MemoryByte(StompTimer)
const val Stop_Frenzy = 0x18
var stopFrenzy by MemoryByte(Stop_Frenzy)
const val SwimmingFlag = 0x0704
var swimmingFlag by MemoryByte(SwimmingFlag)
const val TallEnemy = 0x09
var tallEnemy by MemoryByte(TallEnemy)
const val TerrainControl = 0x0727
var terrainControl by MemoryByte(TerrainControl)
const val TimeRunningOutMusic = 0x40
var timeRunningOutMusic by MemoryByte(TimeRunningOutMusic)
const val TimerControl = 0x0747
var timerControl by MemoryByte(TimerControl)
const val Timers = 0x0780
var timers by MemoryByte(Timers)
const val TitleScreenDataOffset = 0x1EC0
var titleScreenDataOffset by MemoryByte(TitleScreenDataOffset)
const val TitleScreenModeValue = 0x00
var titleScreenModeValue by MemoryByte(TitleScreenModeValue)
const val TopScoreDisplay = 0x07D7
var topScoreDisplay by MemoryByte(TopScoreDisplay)
const val Tri_NoteLenBuffer = 0x07B8
var triNoteLenBuffer by MemoryByte(Tri_NoteLenBuffer)
const val Tri_NoteLenCounter = 0x07B9
var triNoteLenCounter by MemoryByte(Tri_NoteLenCounter)
const val UndergroundMusic = 0x04
var undergroundMusic by MemoryByte(UndergroundMusic)
const val Up_Dir = 0x08
var upDir by MemoryByte(Up_Dir)
const val Up_Down_Buttons = 0x0B
var upDownButtons by MemoryByte(Up_Down_Buttons)
const val VRAM_Buffer1 = 0x0301
var vRAMBuffer1 by MemoryByte(VRAM_Buffer1)
const val VRAM_Buffer1_Offset = 0x0300
var vRAMBuffer1Offset by MemoryByte(VRAM_Buffer1_Offset)
const val VRAM_Buffer2 = 0x0341
var vRAMBuffer2 by MemoryByte(VRAM_Buffer2)
const val VRAM_Buffer2_Offset = 0x0340
var vRAMBuffer2Offset by MemoryByte(VRAM_Buffer2_Offset)
const val VRAM_Buffer_AddrCtrl = 0x0773
var vRAMBufferAddrCtrl by MemoryByte(VRAM_Buffer_AddrCtrl)
const val VerticalFlipFlag = 0x0109
var verticalFlipFlag by MemoryByte(VerticalFlipFlag)
const val VerticalForce = 0x0709
var verticalForce by MemoryByte(VerticalForce)
const val VerticalForceDown = 0x070A
var verticalForceDown by MemoryByte(VerticalForceDown)
const val VerticalScroll = 0x0740
var verticalScroll by MemoryByte(VerticalScroll)
const val VictoryModeValue = 0x02
var victoryModeValue by MemoryByte(VictoryModeValue)
const val VictoryMusic = 0x04
var victoryMusic by MemoryByte(VictoryMusic)
const val VictoryWalkControl = 0x35
var victoryWalkControl by MemoryByte(VictoryWalkControl)
const val VineFlagOffset = 0x0398
var vineFlagOffset by MemoryByte(VineFlagOffset)
const val VineHeight = 0x0399
var vineHeight by MemoryByte(VineHeight)
const val VineObjOffset = 0x039A
var vineObjOffset by MemoryByte(VineObjOffset)
const val VineObject = 0x2F
var vineObject by MemoryByte(VineObject)
const val VineStart_Y_Position = 0x039D
var vineStartYPosition by MemoryByte(VineStart_Y_Position)
const val WarmBootValidation = 0x07FF
var warmBootValidation by MemoryByte(WarmBootValidation)
const val WarpZoneControl = 0x06D6
var warpZoneControl by MemoryByte(WarpZoneControl)
const val WaterMusic = 0x02
var waterMusic by MemoryByte(WaterMusic)
const val Whirlpool_Flag = 0x047D
var whirlpoolFlag by MemoryByte(Whirlpool_Flag)
const val Whirlpool_LeftExtent = 0x0471
var whirlpoolLeftExtent by MemoryByte(Whirlpool_LeftExtent)
const val Whirlpool_Length = 0x0477
var whirlpoolLength by MemoryByte(Whirlpool_Length)
const val Whirlpool_Offset = 0x046A
var whirlpoolOffset by MemoryByte(Whirlpool_Offset)
const val Whirlpool_PageLoc = 0x046B
var whirlpoolPageLoc by MemoryByte(Whirlpool_PageLoc)
const val World1 = 0x00
var world1 by MemoryByte(World1)
const val World2 = 0x01
var world2 by MemoryByte(World2)
const val World3 = 0x02
var world3 by MemoryByte(World3)
const val World4 = 0x03
var world4 by MemoryByte(World4)
const val World5 = 0x04
var world5 by MemoryByte(World5)
const val World6 = 0x05
var world6 by MemoryByte(World6)
const val World7 = 0x06
var world7 by MemoryByte(World7)
const val World8 = 0x07
var world8 by MemoryByte(World8)
const val WorldEndTimer = 0x07A1
var worldEndTimer by MemoryByte(WorldEndTimer)
const val WorldNumber = 0x075F
var worldNumber by MemoryByte(WorldNumber)
const val WorldSelectEnableFlag = 0x07FC
var worldSelectEnableFlag by MemoryByte(WorldSelectEnableFlag)
const val WorldSelectNumber = 0x076B
var worldSelectNumber by MemoryByte(WorldSelectNumber)
const val XMovePrimaryCounter = 0xA0
var xMovePrimaryCounter by MemoryByte(XMovePrimaryCounter)
const val XMoveSecondaryCounter = 0x58
var xMoveSecondaryCounter by MemoryByte(XMoveSecondaryCounter)
const val YPlatformCenterYPos = 0x58
var yPlatformCenterYPos by MemoryByte(YPlatformCenterYPos)
const val YPlatformTopYPos = 0x0401
var yPlatformTopYPos by MemoryByte(YPlatformTopYPos)

// ROM code label constants (data table addresses)
// These are addresses in ROM where data tables and code are located

const val ActionClimbing = 0xF044
const val ActionFalling = 0xF034
const val ActionSwimming = 0xF050
const val ActionWalkRun = 0xF03C
const val AddCCF = 0xCF0C
const val AddFBit = 0xC6E6
const val AddHA = 0xCDCB
const val AddHS = 0xCABB
const val AddModLoop = 0x8F68
const val AddToScore = 0xBC27
const val AddVA = 0xCDFF
const val AdjSm = 0xCE28
const val AlignP = 0xC7ED
const val AllMus = 0xF8EF
const val AllRowC = 0xEB93
const val AllUnder = 0x97AA
const val AltYPosOffset = 0x911A
const val Alter2 = 0x96E2
const val AlterAreaAttributes = 0x96C5
const val AlterYP = 0xBFE9
const val AlternateLengthHandler = 0xF8C5
const val AnimationControl = 0xF06F
const val AreaAddrOffsets = 0x9CBC
const val AreaChangeTimerData = 0xDE03
const val AreaDataAddrHigh = 0x9D4E
const val AreaDataAddrLow = 0x9D2C
const val AreaDataHOffsets = 0x9D28
const val AreaDataOfsLoopback = 0x9BF8
const val AreaFrenzy = 0x972B
const val AreaMusicEnvData = 0xFF9A
const val AreaPalette = 0x85BB
const val AreaParserCore = 0x93FC
const val AreaParserTaskControl = 0x86E6
const val AreaParserTaskHandler = 0x92B0
const val AreaParserTasks = 0x92C8
const val AreaStyleObject = 0x9740
const val AttribLoop = 0x8990
const val AutoClimb = 0xB1D1
const val AutoControlPlayer = 0xB0E6
const val AutoPlayer = 0x839A
const val AwardGameTimerPoints = 0xD312
const val AwardTouchedCoin = 0xDD1A
const val AxeObj = 0x9A09
const val BBChk_E = 0xE3A5
const val BBFly = 0xBA73
const val BB_SLoop = 0xC6FF
const val BGColorCtrl_Addr = 0x85CB
const val BHalf = 0xDD85
const val BPGet = 0xCF1E
const val BSceneDataOffsets = 0x92F7
const val BSwimE = 0xCC03
const val BWithL = 0x9B28
const val B_FaceP = 0xD0B0
const val BackColC = 0x9646
const val BackSceneryData = 0x92FA
const val BackSceneryMetatiles = 0x938A
const val BackgroundColors = 0x85CF
const val BalancePlatRope = 0x99D7
const val BalancePlatform = 0xD432
const val BigBP = 0xBD62
const val BigJp = 0xCE31
const val BigKTS = 0xEF2D
const val Bitmasks = 0xC68A
const val BlankPalette = 0x89C9
const val BlkOffscr = 0xEC35
const val BlockBuffLowBounds = 0x9504
const val BlockBufferAdderData = 0xE3AD
const val BlockBufferAddr = 0x9BDD
const val BlockBufferChk_Enemy = 0xE388
const val BlockBufferChk_FBall = 0xE39C
const val BlockBufferColli_Feet = 0xE3E8
const val BlockBufferColli_Head = 0xE3E9
const val BlockBufferColli_Side = 0xE3EC
const val BlockBufferCollision = 0xE3F0
const val BlockBuffer_X_Adder = 0xE3B0
const val BlockBuffer_Y_Adder = 0xE3CC
const val BlockBumpedChk = 0xBDF6
const val BlockCode = 0xBDBD
const val BlockGfxData = 0x8A39
const val BlockObjMT_Updater = 0xBED4
const val BlockObjectsCore = 0xBE70
const val BlockYPosAdderData = 0xBCEB
const val BlooberBitmasks = 0xCB87
const val BlooberSwim = 0xCBAC
const val BlstSJp = 0xF5D1
const val BorrowOne = 0x8F87
const val BotSP = 0xEDAA
const val BounceJS = 0xB8F4
const val BouncingBlockHandler = 0xBEB3
const val BoundBoxCtrlData = 0xE1FD
const val BoundingBoxCore = 0xE29C
const val BowserControl = 0xD07F
const val BowserFlameEnvData = 0xFFCA
const val BowserGfxHandler = 0xD17B
const val BowserIdentities = 0xD736
const val BowserPaletteData = 0x8D4C
const val BranchToDecLength1 = 0xF47B
const val BrickMetatiles = 0x9A29
const val BrickQBlockMetatiles = 0xBDE8
const val BrickShatter = 0xBE02
const val BrickShatterEnvData = 0xFFEA
const val BrickShatterFreqData = 0xF62B
const val BrickWithCoins = 0x9B14
const val BrickWithItem = 0x9B19
const val BridgeCollapse = 0xCFEC
const val BridgeCollapseData = 0xCFDD
const val Bridge_High = 0x9979
const val Bridge_Low = 0x997F
const val Bridge_Middle = 0x997C
const val BubbleCheck = 0xB6F9
const val BubbleTimerData = 0xB74D
const val Bubble_MForceData = 0xB74B
const val BublExit = 0xB686
const val BublLoop = 0xB675
const val BulletBillCannon = 0x9A69
const val BulletBillCheepCheep = 0xC69C
const val BulletBillHandler = 0xBA33
const val BulletBillXSpdData = 0xBA31
const val BumpBlock = 0xBD9B
const val BumpChkLoop = 0xBDF8
const val BuzzyBeetleMutate = 0xC1FD
const val CCSwim = 0xCC53
const val CCSwimUpwards = 0xCC99
const val CGrab_TTickRegL = 0xF522
const val CInvu = 0xDFE6
const val CMBits = 0xE267
const val CNwCDir = 0xE0C5
const val CRendLoop = 0x981B
const val CSetFDir = 0xB40A
const val CSzNext = 0xF0C3
const val C_ObjectMetatile = 0x99FE
const val C_ObjectRow = 0x99FB
const val C_S_IGAtt = 0xF117
const val CannonBitmasks = 0xB9BA
const val CarryOne = 0x8F8E
const val CasPBB = 0xC83B
const val CastleBridgeObj = 0x9A01
const val CastleMetatiles = 0x97CF
const val CastleMusData = 0xFBA4
const val CastleMusHdr = 0xF95C
const val CastleObject = 0x9806
const val CastlePaletteData = 0x8D10
const val ChainObj = 0x9A0E
const val ChangeSizeOffsetAdder = 0xF09C
const val CheckAnimationStop = 0xEA29
const val CheckBalPlatform = 0xD440
const val CheckBowserFront = 0xE923
const val CheckBowserGfxFlag = 0xE8F2
const val CheckBowserRear = 0xE94C
const val CheckDefeatedState = 0xEA37
const val CheckEndofBuffer = 0xC150
const val CheckForBloober = 0xE9DE
const val CheckForBulletBillCV = 0xE8BE
const val CheckForClimbMTiles = 0xDF9A
const val CheckForCoinMTiles = 0xDFA1
const val CheckForDefdGoomba = 0xE9BA
const val CheckForESymmetry = 0xEAA6
const val CheckForEnemyGroup = 0xC1F1
const val CheckForGoomba = 0xE900
const val CheckForHammerBro = 0xE9CA
const val CheckForJumping = 0xB479
const val CheckForJumpspring = 0xE8D5
const val CheckForLakitu = 0xE97A
const val CheckForPUpCollision = 0xD881
const val CheckForPodoboo = 0xE8E1
const val CheckForRetainerObj = 0xE8A9
const val CheckForSecondFrame = 0xEA22
const val CheckForSolidMTiles = 0xDF8F
const val CheckForSpiny = 0xE965
const val CheckForVerticalFlip = 0xEA64
const val CheckFrenzyBuffer = 0xC216
const val CheckHalfway = 0x904A
const val CheckLeftScreenBBox = 0xE30C
const val CheckNoiseBuffer = 0xF675
const val CheckPageCtrlRow = 0xC189
const val CheckPlayerName = 0x885F
const val CheckPlayerVertical = 0xDC41
const val CheckRear = 0x955D
const val CheckRightBounds = 0xC164
const val CheckRightExtBounds = 0xC1CB
const val CheckRightScreenBBox = 0xE2DE
const val CheckRightSideUpShell = 0xE9A6
const val CheckSfx1Buffer = 0xF43F
const val CheckSfx2Buffer = 0xF5A6
const val CheckSideMTiles = 0xDD9C
const val CheckThreeBytes = 0xC250
const val CheckToAnimateEnemy = 0xE9FA
const val CheckToMirrorJSpring = 0xEB4E
const val CheckToMirrorLakitu = 0xEB12
const val CheckTopOfBlock = 0xBE1F
const val CheckUpsideDownShell = 0xE990
const val CheckpointEnemyID = 0xC26C
const val ChgAreaMode = 0xB213
const val ChgAreaPipe = 0xB20B
const val ChgSDir = 0xCE6F
const val Chk1Row13 = 0x9530
const val Chk1Row14 = 0x9554
const val Chk1stB = 0x959D
const val Chk2MSBSt = 0xE0EC
const val Chk2Ofs = 0xCE5C
const val Chk2Players = 0x8815
const val ChkAreaTsk = 0xC053
const val ChkAreaType = 0x9106
const val ChkBBill = 0xE054
const val ChkBehPipe = 0xB083
const val ChkBowserF = 0xC05F
const val ChkBrick = 0xBD1A
const val ChkBuzzyBeetle = 0xD752
const val ChkCFloor = 0x982D
const val ChkCollSize = 0xDC98
const val ChkContinue = 0x82D8
const val ChkDSte = 0xBA6A
const val ChkETmrs = 0xD90E
const val ChkEmySpd = 0xCFCB
const val ChkEnemyFaceRight = 0xD9F6
const val ChkEnemyFrenzy = 0xC12F
const val ChkFBCl = 0xCE58
const val ChkFOfs = 0xCDE6
const val ChkFTop = 0xE492
const val ChkFiery = 0x85FD
const val ChkFireB = 0xD149
const val ChkFlagOffscreen = 0xE5A7
const val ChkFlagpoleYPosLoop = 0xDE68
const val ChkFootMTile = 0xDD1D
const val ChkForBump_HammerBroJ = 0xE124
const val ChkForDemoteKoopa = 0xD9B3
const val ChkForFall = 0xD44D
const val ChkForFlagpole = 0xDE39
const val ChkForFloatdown = 0xCC1C
const val ChkForLandJumpSpring = 0xDEC4
const val ChkForNonSolids = 0xE1B5
const val ChkForPlayerAttrib = 0xF0E9
const val ChkForPlayerC_LargeP = 0xDB5F
const val ChkForPlayerInjury = 0xD8F9
const val ChkForRedKoopa = 0xE0E2
const val ChkForTopCollision = 0xDBCF
const val ChkFrontSte = 0xE940
const val ChkGERtn = 0xDDF0
const val ChkHiByte = 0x8723
const val ChkHoleX = 0xB1AA
const val ChkInj = 0xD8FF
const val ChkInvisibleMTiles = 0xDEBD
const val ChkJH = 0xC9E1
const val ChkJumpspringMetatiles = 0xDEDD
const val ChkKillGoomba = 0xCAEB
const val ChkLS = 0xCF31
const val ChkLak = 0xC3B4
const val ChkLakDif = 0xCF7D
const val ChkLandedEnemyState = 0xE07E
const val ChkLeftCo = 0xEC46
const val ChkLength = 0x9571
const val ChkLrgObjFixedLength = 0x9BAF
const val ChkLrgObjLength = 0x9BAC
const val ChkLuigi = 0x8873
const val ChkMTLow = 0x94E0
const val ChkMouth = 0xD08C
const val ChkMoveDir = 0xB13E
const val ChkNearMid = 0xAFBA
const val ChkNearPlayer = 0xCC29
const val ChkNoEn = 0xC3CA
const val ChkNumTimer = 0x84D1
const val ChkOnScr = 0xDC86
const val ChkOtherEnemies = 0xD789
const val ChkOtherForFall = 0xD462
const val ChkOverR = 0x9197
const val ChkPBtm = 0xDDBB
const val ChkPOffscr = 0xB000
const val ChkPSpeed = 0xCF9F
const val ChkPUSte = 0xBCD2
const val ChkPauseTimer = 0x8194
const val ChkPlayerNearPipe = 0xD3CF
const val ChkRBit = 0xC6D6
const val ChkRFast = 0xB545
const val ChkRearSte = 0xE955
const val ChkRep = 0xEC0A
const val ChkRow13 = 0x95C3
const val ChkRow14 = 0x95B3
const val ChkSRows = 0x95E2
const val ChkSelect = 0x8258
const val ChkSkid = 0xB59E
const val ChkSmallPlatCollision = 0xD671
const val ChkSmallPlatLoop = 0xDB8C
const val ChkSpinyO = 0xCFC1
const val ChkStPos = 0x9153
const val ChkStart = 0x819D
const val ChkStop = 0xB2E3
const val ChkSwimE = 0x91B0
const val ChkSwimYPos = 0xCCAC
const val ChkTallEnemy = 0x8500
const val ChkToMoveBalPlat = 0xD474
const val ChkToStunEnemies = 0xE01B
const val ChkTop = 0xBEAA
const val ChkUnderEnemy = 0xE1AE
const val ChkUpM = 0xC018
const val ChkVFBD = 0xCE3C
const val ChkW2 = 0xC6B4
const val ChkWorldSel = 0x826C
const val ChkWtr = 0xB4D2
const val ChkYCenterPos = 0xD5EF
const val ChkYPCollision = 0xD5FE
const val Chk_BB = 0xBA1A
const val ChnkOfs = 0xECC9
const val ChpChpEx = 0xC44E
const val ClHCol = 0xD7FA
const val ClearBitsMask = 0xDA2C
const val ClearBounceFlag = 0xE1EF
const val ClearBuffersDrawIcon = 0x8732
const val ClearVRLoop = 0x9077
const val ClimbAdderHigh = 0xB3CB
const val ClimbAdderLow = 0xB3C7
const val ClimbFD = 0xB406
const val ClimbMTileUpperExt = 0xDF96
const val ClimbPLocAdder = 0xDE27
const val ClimbXPosAdder = 0xDE25
const val Climb_Y_MForceData = 0xB44D
const val Climb_Y_SpeedData = 0xB44A
const val ClimbingSub = 0xB3CF
const val CloudExit = 0xB1BB
const val ClrGetLoop = 0x860A
const val ClrMTBuf = 0x9408
const val ClrPauseTimer = 0x81BD
const val ClrPlrPal = 0xAF64
const val ClrSndLoop = 0x8FD6
const val ClrTimersLoop = 0x8FED
const val CntGrp = 0xC74D
const val CntPl = 0xEEF3
const val CoinBlock = 0xBB38
const val CoinMetatileData = 0x99EE
const val CoinPoints = 0xBC22
const val CoinSd = 0xDFAB
const val CoinTallyOffsets = 0xBBF8
const val ColFlg = 0xD494
const val ColObj = 0x9A20
const val ColdBoot = 0x802B
const val CollisionCoreLoop = 0xE32D
const val CollisionFound = 0xE37E
const val ColorRotatePalette = 0x89C3
const val ColorRotation = 0x89E1
const val ColumnOfBricks = 0x9A50
const val ColumnOfSolidBlocks = 0x9A59
const val CommonPlatCode = 0xC828
const val CommonSmallLift = 0xC860
const val CompDToO = 0xD107
const val ContBTmr = 0xBD39
const val ContChk = 0xDD2D
const val ContES = 0xEAB6
const val ContPau = 0xF316
const val ContSChk = 0xDDA9
const val ContVMove = 0xBF6D
const val Cont_CGrab_TTick = 0xF5C2
const val ContinueBlast = 0xF545
const val ContinueBowserFall = 0xF5D3
const val ContinueBowserFlame = 0xF685
const val ContinueBrickShatter = 0xF640
const val ContinueBumpThrow = 0xF40D
const val ContinueCGrabTTick = 0xF52C
const val ContinueExtraLife = 0xF5E7
const val ContinueGame = 0x9264
const val ContinueGrowItems = 0xF60F
const val ContinueMusic = 0xF691
const val ContinuePipeDownInj = 0xF4BB
const val ContinuePowerUpGrab = 0xF557
const val ContinueSmackEnemy = 0xF48D
const val ContinueSndJump = 0xF3DF
const val ContinueSwimStomp = 0xF469
const val CopyFToR = 0xD187
const val CopyScore = 0x8FAF
const val CreateL = 0xC3D3
const val CreateSpiny = 0xC3E6
const val CyclePlayerPalette = 0xB288
const val CycleTwo = 0xAF5D
const val D2XPos1 = 0xC51C
const val D2XPos2 = 0xC530
const val DBlkLoop = 0xEBE7
const val DBlockSte = 0xBCFA
const val DChunks = 0xEC65
const val DSFLoop = 0xD36D
const val DaySnowPaletteData = 0x8D34
const val DeathMAltReg = 0xF810
const val DeathMusData = 0xFB72
const val DeathMusHdr = 0xF9B2
const val DecHT = 0xCA0A
const val DecJpFPS = 0xF419
const val DecNumTimer = 0x84DB
const val DecPauC = 0xF32E
const val DecSeXM = 0xCB60
const val DecTimers = 0x8100
const val DecTimersLoop = 0x810E
const val DecodeAreaData = 0x9595
const val DecrementSfx1Length = 0xF4A2
const val DecrementSfx2Length = 0xF568
const val DecrementSfx3Length = 0xF658
const val DefaultBlockObjTiles = 0xEBCD
const val DefaultSprOffsets = 0x8FBC
const val DefaultXOnscreenOfs = 0xF1F3
const val DefaultYOnscreenOfs = 0xF234
const val DelayToAreaEnd = 0xD3A2
const val DemoActionData = 0x8340
const val DemoEngine = 0x836B
const val DemoOver = 0x838A
const val DemoTimingData = 0x8355
const val Demote = 0xE02B
const val DemotedKoopaXSpdData = 0xD851
const val DestroyBlockMetatile = 0x8A6B
const val DifLoop = 0xC40F
const val DigitPLoop = 0x8F47
const val DigitsMathRoutine = 0x8F5F
const val DisJoyp = 0xB0FF
const val DisplayIntermediate = 0x86A8
const val DisplayTimeUp = 0x8693
const val DivLLoop = 0xF5EC
const val DividePDiff = 0xF26D
const val DmpJpFPS = 0xF3F4
const val DoAPTasks = 0x92BA
const val DoAction = 0x8380
const val DoAltLoad = 0xF817
const val DoBPl = 0xD43B
const val DoBulletBills = 0xC6FD
const val DoChangeSize = 0xEF3A
const val DoEnemySideCheck = 0xE0FE
const val DoFootCheck = 0xDCF6
const val DoGroup = 0xC22E
const val DoIDCheckBGColl = 0xDFD8
const val DoLpBack = 0xC11C
const val DoNothing1 = 0x92AA
const val DoNothing2 = 0x92AF
const val DoOtherPlatform = 0xD4A7
const val DoPlayerSideCheck = 0xDD5E
const val DoSide = 0xE182
const val DoneInitArea = 0x9054
const val DonePlayerTask = 0xB273
const val DontWalk = 0x83D0
const val DownJSpr = 0xB8D5
const val DrawBlock = 0xEBD1
const val DrawBowser = 0xE949
const val DrawBrickChunks = 0xEC53
const val DrawBricks = 0x9A38
const val DrawBubble = 0xEDE1
const val DrawEnemyObjRow = 0xEBAA
const val DrawEnemyObject = 0xEA4B
const val DrawEraseRope = 0xD4BD
const val DrawExplosion_Fireball = 0xED09
const val DrawExplosion_Fireworks = 0xED17
const val DrawFbar = 0xCD9C
const val DrawFireball = 0xECDE
const val DrawFirebar = 0xECED
const val DrawFirebar_Collision = 0xCDBB
const val DrawFlagSetTimer = 0xD396
const val DrawFlameLoop = 0xD23C
const val DrawFloateyNumber_Coin = 0xE655
const val DrawHammer = 0xE4DC
const val DrawJSpr = 0xB902
const val DrawLargePlatform = 0xE5C8
const val DrawMTLoop = 0x88D0
const val DrawMushroomIcon = 0x8325
const val DrawOneSpriteRow = 0xEBB2
const val DrawPipe = 0x9925
const val DrawPlayerLoop = 0xEFDC
const val DrawPlayer_Intermediate = 0xEFA4
const val DrawPowerUp = 0xE6D2
const val DrawQBlk = 0x9B2C
const val DrawRope = 0x99E9
const val DrawRow = 0x9A48
const val DrawSidePart = 0x98CE
const val DrawSmallPlatform = 0xED66
const val DrawSpriteObject = 0xF282
const val DrawStarFlag = 0xD365
const val DrawThisRow = 0x9B9D
const val DrawTitleScreen = 0x86FF
const val DrawVine = 0xE435
const val DropPlatform = 0xD631
const val DumpFall = 0xB38D
const val DumpFourSpr = 0xE5BB
const val DumpSixSpr = 0xE5B5
const val DumpThreeSpr = 0xE5BE
const val DumpTwoSpr = 0xE5C1
const val Dump_Freq_Regs = 0xF38D
const val Dump_Sq2_Regs = 0xF39F
const val Dump_Squ1_Regs = 0xF381
const val DuplicateEnemyObj = 0xC575
const val ECLoop = 0xDA56
const val EColl = 0xD88A
const val ELPGive = 0xD33F
const val EL_LRegs = 0xF5E0
const val ESRtnr = 0xEACA
const val E_CastleArea1 = 0x9D70
const val E_CastleArea2 = 0x9D97
const val E_CastleArea3 = 0x9DB0
const val E_CastleArea4 = 0x9DDF
const val E_CastleArea5 = 0x9E0A
const val E_CastleArea6 = 0x9E1F
const val E_GroundArea1 = 0x9E59
const val E_GroundArea10 = 0x9F7B
const val E_GroundArea11 = 0x9F7C
const val E_GroundArea12 = 0x9FA0
const val E_GroundArea13 = 0x9FA9
const val E_GroundArea14 = 0x9FCE
const val E_GroundArea15 = 0x9FF1
const val E_GroundArea16 = 0x9FFA
const val E_GroundArea17 = 0x9FFB
const val E_GroundArea18 = 0xA035
const val E_GroundArea19 = 0xA060
const val E_GroundArea2 = 0x9E7E
const val E_GroundArea20 = 0xA08E
const val E_GroundArea21 = 0xA0AA
const val E_GroundArea22 = 0xA0B3
const val E_GroundArea3 = 0x9E9B
const val E_GroundArea4 = 0x9EA9
const val E_GroundArea5 = 0x9ED0
const val E_GroundArea6 = 0x9F01
const val E_GroundArea7 = 0x9F1F
const val E_GroundArea8 = 0x9F3C
const val E_GroundArea9 = 0x9F51
const val E_UndergroundArea1 = 0xA0D8
const val E_UndergroundArea2 = 0xA105
const val E_UndergroundArea3 = 0xA133
const val E_WaterArea1 = 0xA160
const val E_WaterArea2 = 0xA171
const val E_WaterArea3 = 0xA19B
const val EggExc = 0xEAF2
const val EmptyBlock = 0x9A19
const val EmptyChkLoop = 0x994C
const val EmptySfx2Buffer = 0xF56D
const val EndAParse = 0x9588
const val EndAreaPoints = 0xD336
const val EndChgSize = 0xB23D
const val EndChkBButton = 0x8487
const val EndExitOne = 0x8486
const val EndExitTwo = 0x849E
const val EndFrenzy = 0xC7B8
const val EndGameText = 0x882E
const val EndMushL = 0x978B
const val EndOfCastleMusData = 0xFE51
const val EndOfCastleMusicEnvData = 0xFF96
const val EndOfEnemyInitCode = 0xC881
const val EndOfLevelMusHdr = 0xF949
const val EndOfMusicData = 0xF74B
const val EndRp = 0xD530
const val EndTreeL = 0x9773
const val EndUChk = 0x94CA
const val EndlessLoop = 0x8057
const val EndlessRope = 0x99D0
const val EnemiesAndLoopsCore = 0xC047
const val EnemiesCollision = 0xDA33
const val Enemy17YPosData = 0xC692
const val EnemyAddrHOffsets = 0x9CE0
const val EnemyAnimTimingBMask = 0xE876
const val EnemyAttributeData = 0xE85B
const val EnemyBGCStateData = 0xDFB9
const val EnemyBGCXSpdData = 0xDFBF
const val EnemyDataAddrHigh = 0x9D06
const val EnemyDataAddrLow = 0x9CE4
const val EnemyFacePlayer = 0xDA05
const val EnemyGfxHandler = 0xE87D
const val EnemyGfxTableOffsets = 0xE840
const val EnemyGraphicsTable = 0xE73E
const val EnemyJump = 0xE163
const val EnemyLanding = 0xE14F
const val EnemyMovementSubs = 0xC905
const val EnemySmackScore = 0xD7BC
const val EnemyStomped = 0xD969
const val EnemyStompedPts = 0xD996
const val EnemyToBGCollisionDet = 0xDFC1
const val EnemyTurnAround = 0xDB1C
const val EnterSidePipe = 0xB21F
const val EntrMode2 = 0xB09B
const val Entrance_GameTimerSetup = 0x9131
const val ErACM = 0xDE1C
const val EraseDMods = 0x8F7C
const val EraseEnemyObject = 0xC998
const val EraseFB = 0xB6EE
const val EraseMLoop = 0x8F80
const val EraseR1 = 0xD4F7
const val EraseR2 = 0xD528
const val EvalForMusic = 0x8434
const val ExAnimC = 0xF08F
const val ExBCDr = 0xECDD
const val ExBGfxH = 0xD1BB
const val ExCInvT = 0xDEC3
const val ExCJSp = 0xDEDC
const val ExCPV = 0xDC51
const val ExCSM = 0xDE02
const val ExCannon = 0xBA30
const val ExDBlk = 0xEC52
const val ExDBub = 0xEE06
const val ExDLPl = 0xE654
const val ExDPl = 0xD63C
const val ExDivPD = 0xF281
const val ExEBG = 0xDFB8
const val ExEBGChk = 0xE066
const val ExEGHandler = 0xEBA9
const val ExEPar = 0xC22D
const val ExESdeC = 0xE123
const val ExF17 = 0xC710
const val ExFl = 0xD1EA
const val ExFlmeD = 0xD294
const val ExGTimer = 0xB7A3
const val ExHC = 0xDE38
const val ExHCF = 0xD7C3
const val ExIPM = 0xDF81
const val ExInjColRoutines = 0xD955
const val ExJCGfx = 0xE6BD
const val ExJSpring = 0xB91D
const val ExLPC = 0xDB78
const val ExLSHand = 0xC3E5
const val ExLiftP = 0xD679
const val ExMoveLak = 0xCFDC
const val ExNH = 0xF67F
const val ExPBGCol = 0xDC97
const val ExPEC = 0xD8F8
const val ExPF = 0xD5D0
const val ExPGH = 0xEF33
const val ExPHC = 0xD7FF
const val ExPRp = 0xD597
const val ExPVne = 0xDEBC
const val ExPipeE = 0xDF4A
const val ExPlPos = 0xDC40
const val ExPlyrAt = 0xF129
const val ExRPl = 0xD64E
const val ExS1H = 0xF45A
const val ExS2H = 0xF5C1
const val ExSCH = 0xDD9B
const val ExSFN = 0xDA24
const val ExSPC = 0xDBB7
const val ExSPl = 0xEDDE
const val ExScrnBd = 0xD6D5
const val ExSfx1 = 0xF4B5
const val ExSfx2 = 0xF57B
const val ExSfx3 = 0xF666
const val ExSteChk = 0xE0A4
const val ExSwCC = 0xCCC6
const val ExTA = 0xDB44
const val ExTrans = 0x92A9
const val ExVMove = 0xC046
const val ExXMP = 0xD630
const val ExXMove = 0xBF4C
const val ExXOfsBS = 0xF22A
const val ExYOfsBS = 0xF26C
const val ExYPl = 0xD606
const val ExecGameLoopback = 0xC08C
const val ExitAFrenzy = 0x973C
const val ExitBlink = 0xB253
const val ExitBlockChk = 0xBDE7
const val ExitBoth = 0xB268
const val ExitBubl = 0xB74A
const val ExitCAPipe = 0xB21E
const val ExitCSub = 0xB41F
const val ExitCastle = 0x986E
const val ExitChgSize = 0xB244
const val ExitChkName = 0x8881
const val ExitColorRot = 0x8A38
const val ExitCtrl = 0xB1BA
const val ExitDeath = 0xB2A3
const val ExitDecBlock = 0x9B3C
const val ExitDrawM = 0x8967
const val ExitDumpSpr = 0xE5C7
const val ExitECRoutine = 0xDAB1
const val ExitELCore = 0xC06A
const val ExitEmptyChk = 0x9956
const val ExitEng = 0xAF92
const val ExitEntr = 0xB0E5
const val ExitFBallEnemy = 0xD733
const val ExitFWk = 0xC689
const val ExitFlagP = 0xB8B5
const val ExitGetM = 0x9115
const val ExitIcon = 0x833F
const val ExitMenu = 0x830D
const val ExitMov1 = 0xB3C4
const val ExitMsgs = 0x8460
const val ExitMusicHandler = 0xF8C4
const val ExitNA = 0xB328
const val ExitOutputN = 0x8F5E
const val ExitPUp = 0xBCEA
const val ExitPause = 0x81C5
const val ExitPhy = 0xB58B
const val ExitPipe = 0x98AB
const val ExitProcessEColl = 0xDAF0
const val ExitRp = 0xD53E
const val ExitUPartR = 0x9BAB
const val ExitVH = 0xB9B7
const val ExitVWalk = 0x83F1
const val ExitWh = 0xB7F4
const val ExplosionTiles = 0xED06
const val ExtendLB = 0xD68F
const val ExtraLifeFreqData = 0xF4D4
const val ExtraLifeMushBlock = 0xBDD8
const val FBCLoop = 0xCE32
const val FBLeft = 0xCBA2
const val FBallB = 0xE23D
const val FMiscLoop = 0xBB86
const val FPGfx = 0xB8AC
const val FPS2nd = 0xF3F2
const val FSLoop = 0xC577
const val FSceneDataOffsets = 0x93AE
const val FallE = 0xCA98
const val FallMForceData = 0xB42B
const val FallingSub = 0xB36D
const val FastXSp = 0xB554
const val FeetTmr = 0xD094
const val FetchNoiseBeatData = 0xF878
const val FetchSqu1MusicData = 0xF7C5
const val FinCCSt = 0xC53C
const val FindAreaMusicHeader = 0xF6ED
const val FindAreaPointer = 0x9C13
const val FindEmptyEnemySlot = 0x994A
const val FindEmptyMiscSlot = 0xBB84
const val FindEventMusicHeader = 0xF6F1
const val FindLoop = 0xC0D8
const val FindPlayerAction = 0xEF34
const val FinishFlame = 0xC61F
const val FireA = 0xED02
const val FireBulletBill = 0xC711
const val FireCannon = 0xB9E9
const val FireballBGCollision = 0xE1C8
const val FireballEnemyCDLoop = 0xD6EE
const val FireballEnemyCollision = 0xD6D9
const val FireballExplosion = 0xB6F3
const val FireballObjCore = 0xB689
const val FireballXSpdData = 0xB687
const val FirebarCollision = 0xCE08
const val FirebarMirrorData = 0xCD2A
const val FirebarPosLookupTbl = 0xCCC7
const val FirebarSpin = 0xD410
const val FirebarSpinDirData = 0xC454
const val FirebarSpinSpdData = 0xC44F
const val FirebarTblOffsets = 0xCD2E
const val FirebarYPos = 0xCD3A
const val FireworksSoundScore = 0xD2BD
const val FireworksXPosData = 0xC631
const val FireworksYPosData = 0xC637
const val FirstBoxGreater = 0xE35F
const val FirstSprTilenum = 0xE4D0
const val FirstSprXPos = 0xE4C0
const val FirstSprYPos = 0xE4C4
const val FlagBalls_Residual = 0x9994
const val FlagpoleCollision = 0xDE41
const val FlagpoleGfxHandler = 0xE54B
const val FlagpoleObject = 0x999E
const val FlagpoleRoutine = 0xB855
const val FlagpoleScoreDigits = 0xB850
const val FlagpoleScoreMods = 0xB84B
const val FlagpoleScoreNumTiles = 0xE541
const val FlagpoleSlide = 0xB2A4
const val FlagpoleYPosData = 0xDE29
const val FlameTimerData = 0xD1D1
const val FlameYMFAdderData = 0xC5A1
const val FlameYPosData = 0xC59D
const val FlipBowserOver = 0xE946
const val FlipEnemyVertically = 0xEA8A
const val FlipPUpRightSide = 0xE72B
const val FlmEx = 0xC59C
const val FlmeAt = 0xD235
const val Floatdown = 0xCC21
const val FloateyNumTileData = 0x849F
const val FloateyNumbersRoutine = 0x84C3
const val FloateyPart = 0x852B
const val FlyCC = 0xCEED
const val FlyCCBPriority = 0xCEDA
const val FlyCCTimerData = 0xC4A4
const val FlyCCXPositionData = 0xC488
const val FlyCCXSpeedData = 0xC498
const val ForceHPose = 0xE4EC
const val ForceInjury = 0xD931
const val ForeSceneryData = 0x93B1
const val FourFrameExtent = 0xF068
const val Fr12S = 0xCF40
const val FreCompLoop = 0x9732
const val FrenzyIDData = 0x9728
const val FreqRegLookupTbl = 0xFF00
const val FrictionData = 0xB447
const val Fthrow = 0xF403
const val GBBAdr = 0xDCAB
const val GMLoopB = 0xF6D1
const val GSeed = 0xC4E4
const val GSltLp = 0xC752
const val GameCoreRoutine = 0xAEEA
const val GameEngine = 0xAEFE
const val GameIsOn = 0x9281
const val GameMenuRoutine = 0x8245
const val GameMode = 0xAEDC
const val GameOverInter = 0x86D3
const val GameOverMode = 0x9218
const val GameOverMusData = 0xFC45
const val GameOverMusHdr = 0xF966
const val GameRoutines = 0xB04A
const val GameText = 0x8752
const val GameTextLoop = 0x8820
const val GameTextOffsets = 0x87FE
const val GameTimerData = 0x912D
const val GameTimerFireworks = 0xD2F2
const val Get17ID = 0xC6BC
const val GetAltOffset = 0x8523
const val GetAlternatePalette1 = 0x8643
const val GetAreaDataAddrs = 0x9C22
const val GetAreaMusic = 0x90ED
const val GetAreaObjXPosition = 0x9BCB
const val GetAreaObjYPosition = 0x9BD3
const val GetAreaObjectID = 0x9B36
const val GetAreaPal = 0x8A08
const val GetAreaPalette = 0x85BF
const val GetAreaType = 0x9C09
const val GetBackgroundColor = 0x85E3
const val GetBlankPal = 0x89EF
const val GetBlockBufferAddr = 0x9BE1
const val GetBlockOffscreenBits = 0xF1B6
const val GetBubbleOffscreenBits = 0xF191
const val GetCent = 0xC355
const val GetCurrentAnimOffset = 0xF062
const val GetDToO = 0xD0EA
const val GetESpd = 0xC316
const val GetEnemyBoundBox = 0xE243
const val GetEnemyBoundBoxOfs = 0xDC52
const val GetEnemyBoundBoxOfsArg = 0xDC54
const val GetEnemyOffscreenBits = 0xF1AF
const val GetFireballBoundBox = 0xE22D
const val GetFireballOffscreenBits = 0xF187
const val GetFirebarPosition = 0xCE8E
const val GetGfxOffsetAdder = 0xF091
const val GetHAdder = 0xCE9A
const val GetHPose = 0xE4F0
const val GetHRp = 0xD56A
const val GetHalfway = 0x91F6
const val GetLRp = 0xD550
const val GetLength = 0x8EB3
const val GetLrgObjAttrib = 0x9BBB
const val GetMTileAttrib = 0xDFB0
const val GetMaskedOffScrBits = 0xE252
const val GetMiscBoundBox = 0xE236
const val GetMiscOffscreenBits = 0xF19B
const val GetObjRelativePosition = 0xF171
const val GetOffScreenBitsSet = 0xF1C0
const val GetOffsetFromAnimCtrl = 0xF0D0
const val GetPRCmp = 0xD0D1
const val GetPipeHeight = 0x9939
const val GetPlayerAnimSpeed = 0xB58F
const val GetPlayerColors = 0x85F1
const val GetPlayerOffscreenBits = 0xF180
const val GetProperObjOffset = 0xF1A8
const val GetRBit = 0xC6D1
const val GetRow = 0x9A44
const val GetRow2 = 0x9A5F
const val GetSBNybbles = 0xBC30
const val GetScoreDiff = 0x8FA1
const val GetScreenPosition = 0xB038
const val GetSteFromD = 0xE0F9
const val GetVAdder = 0xCEBA
const val GetWNum = 0xDF22
const val GetXOffscreenBits = 0xF1F6
const val GetXPhy = 0xB55E
const val GetXPhy2 = 0xB56C
const val GetYOffscreenBits = 0xF239
const val GetYPhy = 0xB4E4
const val GiveFPScr = 0xB899
const val GiveOEPoints = 0xE016
const val GiveOneCoin = 0xBBFE
const val GmbaAnim = 0xE910
const val GndMove = 0xB363
const val GoContinue = 0x830E
const val GoombaDie = 0xD706
const val GoombaPoints = 0xD7B6
const val GorSLog = 0xF0C6
const val GrLoop = 0xC750
const val GroundLevelLeadInHdr = 0xF99A
const val GroundLevelPart1Hdr = 0xF976
const val GroundLevelPart2AHdr = 0xF97C
const val GroundLevelPart2BHdr = 0xF982
const val GroundLevelPart2CHdr = 0xF988
const val GroundLevelPart3AHdr = 0xF98E
const val GroundLevelPart3BHdr = 0xF994
const val GroundLevelPart4AHdr = 0xF9A0
const val GroundLevelPart4BHdr = 0xF9A6
const val GroundLevelPart4CHdr = 0xF9AC
const val GroundMLdInData = 0xFAF9
const val GroundM_P1Data = 0xFA01
const val GroundM_P2AData = 0xFA49
const val GroundM_P2BData = 0xFA75
const val GroundM_P2CData = 0xFA9D
const val GroundM_P3AData = 0xFAC2
const val GroundM_P3BData = 0xFADB
const val GroundM_P4AData = 0xFB25
const val GroundM_P4BData = 0xFB4B
const val GroundM_P4CData = 0xFB74
const val GroundPaletteData = 0x8CC8
const val GrowItemRegs = 0xF602
const val GrowThePowerUp = 0xBCB3
const val HBChk = 0xDFDF
const val HBlankDelay = 0x8159
const val HBroWalkingTimerData = 0xC326
const val HJump = 0xCA4B
const val HalfwayPageNybbles = 0x91BD
const val HammerBroBGColl = 0xE185
const val HammerBroJumpCode = 0xCA12
const val HammerBroJumpLData = 0xCA10
const val HammerChk = 0xD10F
const val HammerEnemyOfsData = 0xBA89
const val HammerSprAttrib = 0xE4D8
const val HammerThrowTmrData = 0xC9CE
const val HammerXSpdData = 0xBA92
const val HandleAreaMusicLoopB = 0xF6D4
const val HandleAxeMetatile = 0xDE0E
const val HandleChangeSize = 0xF0B0
const val HandleClimbing = 0xDE2E
const val HandleCoinMetatile = 0xDE05
const val HandleEToBGCollision = 0xDFFA
const val HandleEnemyFBallCol = 0xD73E
const val HandleGroupEnemies = 0xC71B
const val HandleNoiseMusic = 0xF86D
const val HandlePECollisions = 0xD895
const val HandlePipeEntry = 0xDEE8
const val HandlePowerUpCollision = 0xD800
const val HandleSquare1Music = 0xF7BC
const val HandleSquare2Music = 0xF73A
const val HandleStompedShellE = 0xD9D4
const val HandleTriangleMusic = 0xF81A
const val HeadChk = 0xDCBA
const val Hidden1UpBlock = 0x9B01
const val Hidden1UpCoinAmts = 0xB2C2
const val HighPosUnitData = 0xF237
const val HoleBottom = 0xB1A6
const val HoleDie = 0xB194
const val HoleMetatiles = 0x9B3D
const val Hole_Empty = 0x9B41
const val Hole_Water = 0x9957
const val HurtBowser = 0xD75C
const val ISpr0Loop = 0x90B6
const val IconDataRead = 0x8327
const val ImpedePlayerMove = 0xDF4B
const val ImposeFriction = 0xB5CC
const val ImposeGravity = 0xBFD7
const val ImposeGravityBlock = 0xBFA4
const val ImposeGravitySprObj = 0xBFAD
const val InCastle = 0xB2F1
const val InPause = 0xF2EE
const val Inc2B = 0xC25E
const val Inc3B = 0xC25B
const val IncAreaObjOffset = 0x9589
const val IncMLoop = 0xC102
const val IncModeTask_A = 0x845D
const val IncModeTask_B = 0x874E
const val IncMsgCounter = 0x8443
const val IncPXM = 0xCB5D
const val IncSubtask = 0x8745
const val IncWorldSel = 0x829C
const val IncrementColumnPos = 0x92DB
const val IncrementSFTask1 = 0xD30E
const val IncrementSFTask2 = 0xD39E
const val InitATLoop = 0x8E4D
const val InitBalPlatform = 0xC7DF
const val InitBlock_XY_Pos = 0xBD84
const val InitBloober = 0xC342
const val InitBowser = 0xC549
const val InitBowserFlame = 0xC5A3
const val InitBuffer = 0x80D0
const val InitBulletBill = 0xC36B
const val InitByte = 0x90DC
const val InitByteLoop = 0x90D4
const val InitCSTimer = 0xB420
const val InitChangeSize = 0xB255
const val InitCheepCheep = 0xC375
const val InitDropPlatform = 0xC803
const val InitEnemyFrenzy = 0xC7A0
const val InitEnemyObject = 0xC226
const val InitEnemyRoutines = 0xC27F
const val InitFireballExplode = 0xE1F4
const val InitFireworks = 0xC63D
const val InitFlyingCheepCheep = 0xC4A8
const val InitGoomba = 0xC2F1
const val InitHammerBro = 0xC328
const val InitHoriPlatform = 0xC80B
const val InitHorizFlySwimEnemy = 0xC33D
const val InitJS = 0xB4A0
const val InitJumpGPTroopa = 0xC7D1
const val InitLCmd = 0xC12A
const val InitLakitu = 0xC385
const val InitLongFirebar = 0xC459
const val InitMForceData = 0xB439
const val InitMLp = 0xC122
const val InitNTLoop = 0x8E3B
const val InitNormalEnemy = 0xC30E
const val InitPageLoop = 0x90D2
const val InitPiranhaPlant = 0xC787
const val InitPlatScrl = 0xB02E
const val InitPlatformFall = 0xD598
const val InitPodoboo = 0xC2F7
const val InitRear = 0x9636
const val InitRedKoopa = 0xC31E
const val InitRedPTroopa = 0xC34A
const val InitRetainerObj = 0xC307
const val InitScores = 0x8307
const val InitScreen = 0x858B
const val InitScrlAmt = 0xAFFB
const val InitScroll = 0x8EE6
const val InitShortFirebar = 0xC45C
const val InitSteP = 0xDD5A
const val InitVStf = 0xC363
const val InitVertPlatform = 0xC812
const val InitializeArea = 0x8FE4
const val InitializeGame = 0x8FCF
const val InitializeMemory = 0x90CC
const val InitializeNameTables = 0x8E19
const val InjurePlayer = 0xD92C
const val IntermediatePlayerData = 0xEF9E
const val IntroEntr = 0xB08D
const val IntroPipe = 0x9882
const val InvEnemyDir = 0xE140
const val InvOBit = 0xBD7B
const val InvtD = 0xE0BD
const val JCoinC = 0xBB6C
const val JCoinGfxHandler = 0xE686
const val JCoinRun = 0xBBC9
const val JSFnd = 0xDEE6
const val JSMove = 0xB3B3
const val JmpEO = 0xC88F
const val JoypFrict = 0xB5DB
const val JumpEngine = 0x8E04
const val JumpMForceData = 0xB424
const val JumpRegContents = 0xF3D3
const val JumpSwimSub = 0xB376
const val JumpToDecLength2 = 0xF5C5
const val JumpingCoinTiles = 0xE682
const val Jumpspring = 0x9AD3
const val JumpspringFrameOffsets = 0xE878
const val JumpspringHandler = 0xB8BA
const val Jumpspring_Y_PosData = 0xB8B6
const val KSPts = 0xD8F5
const val KeepOnscr = 0xB013
const val KickedShellPtsData = 0xD892
const val KickedShellXSpdData = 0xD84F
const val KillAllEnemies = 0xD071
const val KillBB = 0xBA85
const val KillBlock = 0xBECF
const val KillELoop = 0x971C
const val KillEnemies = 0x9716
const val KillEnemyAboveBlock = 0xE18E
const val KillFireBall = 0xED61
const val KillLakitu = 0xC395
const val KillLoop = 0xD073
const val KillPlayer = 0xD958
const val KillVine = 0xB98A
const val KilledAtt = 0xF105
const val LInj = 0xD9FF
const val LLeft = 0x892A
const val LRAir = 0xB3AC
const val LRWater = 0xB3A6
const val L_CastleArea1 = 0xA1AF
const val L_CastleArea2 = 0xA210
const val L_CastleArea3 = 0xA28F
const val L_CastleArea4 = 0xA302
const val L_CastleArea5 = 0xA36F
const val L_CastleArea6 = 0xA3FA
const val L_GroundArea1 = 0xA46B
const val L_GroundArea10 = 0xA832
const val L_GroundArea11 = 0xA83B
const val L_GroundArea12 = 0xA87A
const val L_GroundArea13 = 0xA88F
const val L_GroundArea14 = 0xA8F6
const val L_GroundArea15 = 0xA95B
const val L_GroundArea16 = 0xA9CE
const val L_GroundArea17 = 0xA9FF
const val L_GroundArea18 = 0xAA92
const val L_GroundArea19 = 0xAB05
const val L_GroundArea2 = 0xA4CE
const val L_GroundArea20 = 0xAB7E
const val L_GroundArea21 = 0xABD7
const val L_GroundArea22 = 0xAC02
const val L_GroundArea3 = 0xA537
const val L_GroundArea4 = 0xA58A
const val L_GroundArea5 = 0xA619
const val L_GroundArea6 = 0xA68E
const val L_GroundArea7 = 0xA6F3
const val L_GroundArea8 = 0xA748
const val L_GroundArea9 = 0xA7CD
const val L_UndergroundArea1 = 0xAC35
const val L_UndergroundArea2 = 0xACD8
const val L_UndergroundArea3 = 0xAD79
const val L_WaterArea1 = 0xAE06
const val L_WaterArea2 = 0xAE45
const val L_WaterArea3 = 0xAEC0
const val LakituAndSpinyHandler = 0xC3A4
const val LakituChk = 0xC7BA
const val LakituDiffAdj = 0xCF25
const val LandEnemyInitState = 0xE0CD
const val LandEnemyProperly = 0xE067
const val LandPlyr = 0xDD44
const val LargeLiftBBox = 0xC848
const val LargeLiftDown = 0xC845
const val LargeLiftUp = 0xC83F
const val LargePlatformBoundBox = 0xE273
const val LargePlatformCollision = 0xDB45
const val LargePlatformSubroutines = 0xC982
const val LcChk = 0xEB74
const val LdGameText = 0x881B
const val LdLDa = 0xCF47
const val LeavePar = 0x9635
const val LeftFrict = 0xB5DE
const val LeftSwim = 0xCBCE
const val LeftWh = 0xB828
const val LenSet = 0x9BBA
const val LimitB = 0xD68D
const val LoadAreaMusic = 0xF6C8
const val LoadAreaPointer = 0x9C03
const val LoadControlRegs = 0xF8D8
const val LoadEnvelopeData = 0xF8F4
const val LoadEventMusic = 0xF6A4
const val LoadHeader = 0xF6F5
const val LoadNumTiles = 0x84ED
const val LoadSqu2Regs = 0xF565
const val LoadTriCtrlReg = 0xF86A
const val LoadUsualEnvData = 0xF8FF
const val LoadWaterEventMusEnvData = 0xF909
const val LongBeat = 0xF8B1
const val LongN = 0xF868
const val LoopCmdE = 0x9645
const val LoopCmdPageNumber = 0xC076
const val LoopCmdWorldNumber = 0xC06B
const val LoopCmdYPosition = 0xC081
const val LrgObj = 0x95F8
const val LuigiName = 0x87ED
const val LuigiThanksMessage = 0x8D68
const val M1FOfs = 0xD28B
const val M2FOfs = 0xD281
const val M3FOfs = 0xD277
const val MEHor = 0xCAAF
const val MRetainerMsg = 0x8414
const val MakeBJump = 0xD13C
const val MakePlatformFall = 0xD45F
const val MarioThanksMessage = 0x8D54
const val Mask2MSB = 0x95DD
const val MaskHPNyb = 0x9204
const val MatchBump = 0xBE01
const val MaxCC = 0xC4C4
const val MaxLeftXSpdData = 0xB440
const val MaxRightXSpdData = 0xB443
const val MaxSpdBlockData = 0xBF9F
const val MediN = 0xF864
const val MetatileGraphics_High = 0x8B0C
const val MetatileGraphics_Low = 0x8B08
const val MidTreeL = 0x9767
const val MirrorEnemyGfx = 0xEAD7
const val MiscLoop = 0xBB98
const val MiscLoopBack = 0xBBF4
const val MiscObjectsCore = 0xBB96
const val MiscSqu1MusicTasks = 0xF7F7
const val MiscSqu2MusicTasks = 0xF79E
const val MovPTDwn = 0xCB22
const val MoveAOId = 0x9612
const val MoveAllSpritesOffscreen = 0x8220
const val MoveBloober = 0xCB89
const val MoveBoundBox = 0xDBA1
const val MoveBoundBoxOffscreen = 0xE289
const val MoveBubl = 0xB732
const val MoveBulletBill = 0xCC36
const val MoveColOffscreen = 0xEC4A
const val MoveD_Bowser = 0xD00F
const val MoveD_EnemyVertically = 0xBF63
const val MoveDefeatedBloober = 0xCBDC
const val MoveDefeatedEnemy = 0xCAE5
const val MoveDropPlatform = 0xBF88
const val MoveEOfs = 0xDB15
const val MoveESprColOffscreen = 0xEBC1
const val MoveESprRowOffscreen = 0xEBB7
const val MoveEnemyHorizontally = 0xBF02
const val MoveEnemySlowVert = 0xBF8C
const val MoveFallingPlatform = 0xBF6B
const val MoveFlyGreenPTroopa = 0xCB25
const val MoveFlyingCheepCheep = 0xCEDF
const val MoveHammerBroXDir = 0xCA58
const val MoveJ_EnemyVertically = 0xBF92
const val MoveJumpingEnemy = 0xCAF9
const val MoveLakitu = 0xCF28
const val MoveLargeLiftPlat = 0xD64F
const val MoveLiftPlatforms = 0xD65B
const val MoveNormalEnemy = 0xCA77
const val MoveObjectHorizontally = 0xBF0F
const val MoveOnVine = 0xB3E0
const val MovePiranhaPlant = 0xD3B0
const val MovePlatformDown = 0xBFB4
const val MovePlatformUp = 0xBFB7
const val MovePlayerHorizontally = 0xBF09
const val MovePlayerVertically = 0xBF4D
const val MovePlayerYAxis = 0xB200
const val MovePodoboo = 0xC9B0
const val MoveRedPTUpOrDown = 0xCB19
const val MoveRedPTroopa = 0xBF77
const val MoveRedPTroopaDown = 0xBF70
const val MoveRedPTroopaUp = 0xBF75
const val MoveSixSpritesOffscreen = 0xE5B3
const val MoveSmallPlatform = 0xD655
const val MoveSpritesOffscreen = 0x8223
const val MoveSubs = 0xB34E
const val MoveSwimmingCheepCheep = 0xCC4A
const val MoveVOffset = 0x8A8F
const val MoveWithXMCntrs = 0xCB66
const val MtchF = 0xDE70
const val MushFlowerBlock = 0xBDD2
const val MushLExit = 0x97CE
const val MushroomIconData = 0x831D
const val MushroomLedge = 0x9778
const val MushroomPaletteData = 0x8D44
const val MushroomRetainerSaved = 0x8D7C
const val MusicHandler = 0xF694
const val MusicHeaderData = 0xF90D
const val MusicLengthLookupTbl = 0xFF66
const val MusicLoopBack = 0xF774
const val MusicSelectData = 0x90E7
const val N2Prt = 0xF3EC
const val N2Tone = 0xF538
const val NKGmba = 0xCAF8
const val NMovShellFallBit = 0xE0DB
const val NPROffscr = 0xEF95
const val NSFnd = 0xE1C7
const val NVFLak = 0xEB3E
const val NXSpd = 0xDF66
const val NYSpd = 0xDCF2
const val NameLoop = 0x8878
const val Next3Slt = 0xBA2D
const val NextAObj = 0x956E
const val NextArea = 0xB315
const val NextBUpd = 0xBEFE
const val NextED = 0xC784
const val NextFSlot = 0xC7C6
const val NextFbar = 0xCDB2
const val NextMTRow = 0x892E
const val NextSdeC = 0xE11C
const val NextSprOffset = 0x81E5
const val NextStair = 0x9AC1
const val NextSubtask = 0x85C8
const val NextTBit = 0x94B6
const val NextVO = 0xB93D
const val NextVSp = 0xE4A2
const val NextWh = 0xB7F1
const val NightSnowPaletteData = 0x8D3C
const val NoAltPal = 0x864F
const val NoBFall = 0xD05E
const val NoBGColor = 0x85EE
const val NoBlankP = 0x989E
const val NoBump = 0xE131
const val NoCDirF = 0xE060
const val NoChgMus = 0xAF52
const val NoCloud2 = 0x94A8
const val NoColFB = 0xCE85
const val NoColWrap = 0x92EB
const val NoCollisionFound = 0xE37A
const val NoDecEnv1 = 0xF7B1
const val NoDecEnv2 = 0xF80A
const val NoDecTimers = 0x8119
const val NoEToBGCollision = 0xDFF7
const val NoEnemyCollision = 0xDAA1
const val NoFBall = 0xB6F2
const val NoFD = 0xCC28
const val NoFPObj = 0xB2BF
const val NoFToECol = 0xD72C
const val NoFore = 0x9467
const val NoFrenzyCode = 0xC7B7
const val NoHFlip = 0xF296
const val NoHOffscr = 0xE540
const val NoHammer = 0xBABF
const val NoIncDAC = 0xF377
const val NoIncPT = 0xCB18
const val NoIncXM = 0xCB5C
const val NoInitCode = 0xC2F0
const val NoInter = 0x86E0
const val NoJSChk = 0xBF59
const val NoJSFnd = 0xDEE7
const val NoJump = 0xB488
const val NoKillE = 0x9724
const val NoLAFr = 0xE98D
const val NoMGPT = 0xCB44
const val NoMoveCode = 0xC934
const val NoMoveSub = 0xB359
const val NoOfs = 0xE309
const val NoOfs2 = 0xE322
const val NoPDwnL = 0xF4D1
const val NoPECol = 0xD880
const val NoPUp = 0xD84C
const val NoReset = 0x88AD
const val NoRunCode = 0xC8D6
const val NoSSw = 0xCC1B
const val NoSideC = 0xDC14
const val NoStop1 = 0xF6CF
const val NoStopSfx = 0xF6B1
const val NoTTick = 0xD327
const val NoTimeUp = 0x86A2
const val NoTone = 0xF39E
const val NoTopSc = 0x8FBB
const val NoUnder = 0x97B0
const val NoUnderHammerBro = 0xE1A7
const val NoWhirlP = 0x9B73
const val NoZSup = 0xBC46
const val NoiseBeatHandler = 0xF88A
const val NoiseSfxHandler = 0xF667
const val NonAnimatedActs = 0xF028
const val NonMaskableInterrupt = 0x8082
const val NormObj = 0x9616
const val NormalXSpdData = 0xC30C
const val NotDOrD4 = 0xF854
const val NotDefB = 0xCC3F
const val NotECstlM = 0xF8E3
const val NotEgg = 0xE977
const val NotGoomba = 0xD710
const val NotRsNum = 0xE65C
const val NotTRO = 0xF757
const val NotTall = 0x9847
const val NotUse = 0xC24D
const val NotWPipe = 0x9608
const val NullJoypad = 0x82BB
const val ObjOffsetData = 0xF1A5
const val OffVine = 0xB0C7
const val OffscrJoypadBitsData = 0xB036
const val OffscreenBoundsCheck = 0xD67A
const val OnGroundStateSub = 0xB35A
const val OnePlayerGameOver = 0x87B3
const val OnePlayerTimeUp = 0x87A0
const val OperModeExecutionTree = 0x8212
const val OtherRope = 0xD4FF
const val OutputCol = 0x86F9
const val OutputInter = 0x86C7
const val OutputNumbers = 0x8F11
const val OutputTScr = 0x8719
const val OutputToVRAM = 0x8EB6
const val PBFRegs = 0xF5DE
const val PIntLoop = 0xEFA6
const val PJumpSnd = 0xB511
const val PPHSubt = 0xD626
const val PRDiffAdjustData = 0xC398
const val PROfsLoop = 0xEF8C
const val PRandomRange = 0xD061
const val PRandomSubtracter = 0xCED5
const val PTRegC = 0xF327
const val PTone1F = 0xF312
const val PTone2F = 0xF325
const val PUpDrawLoop = 0xE6F7
const val PUpOfs = 0xE73B
const val PUp_VGrow_FreqData = 0xF4F8
const val Palette0_MTiles = 0x8B10
const val Palette1_MTiles = 0x8BAC
const val Palette2_MTiles = 0x8C64
const val Palette3Data = 0x89D1
const val Palette3_MTiles = 0x8C8C
const val ParseRow0e = 0xC231
const val PauseRoutine = 0x8182
const val PauseSkip = 0x811B
const val PdbM = 0xC9CB
const val PerformWalk = 0x83CD
const val PipeDwnS = 0xDDCE
const val PlatDn = 0xD4A4
const val PlatF = 0xDF74
const val PlatLiftDown = 0xC857
const val PlatLiftUp = 0xC84B
const val PlatPosDataHigh = 0xC86E
const val PlatPosDataLow = 0xC86B
const val PlatSt = 0xD49E
const val PlatUp = 0xD498
const val PlatformFall = 0xD5BB
const val PlatformSideCollisions = 0xDBF5
const val PlayBeat = 0xF8BB
const val PlayBigJump = 0xF3D1
const val PlayBlast = 0xF53A
const val PlayBowserFall = 0xF5C8
const val PlayBowserFlame = 0xF680
const val PlayBrickShatter = 0xF63B
const val PlayBump = 0xF3FF
const val PlayCoinGrab = 0xF518
const val PlayExtraLife = 0xF5E2
const val PlayFireballThrow = 0xF3F9
const val PlayFlagpoleSlide = 0xF3BF
const val PlayGrowPowerUp = 0xF5FC
const val PlayGrowVine = 0xF600
const val PlayNoiseSfx = 0xF64D
const val PlayPipeDownInj = 0xF4B6
const val PlayPowerUpGrab = 0xF552
const val PlaySmackEnemy = 0xF47D
const val PlaySmallJump = 0xF3CD
const val PlaySqu1Sfx = 0xF388
const val PlaySqu2Sfx = 0xF3A6
const val PlaySwimStomp = 0xF45B
const val PlayTimerTick = 0xF51E
const val PlayerAnimTmrData = 0xB58C
const val PlayerBGCollision = 0xDC64
const val PlayerBGPriorityData = 0x9125
const val PlayerBGUpperExtent = 0xDC62
const val PlayerChangeSize = 0xB233
const val PlayerCollisionCore = 0xE325
const val PlayerColors = 0x85D7
const val PlayerCtrlRoutine = 0xB0E9
const val PlayerDeath = 0xB269
const val PlayerEndLevel = 0xB2CA
const val PlayerEndWorld = 0x8461
const val PlayerEnemyCollision = 0xD853
const val PlayerEnemyDiff = 0xE143
const val PlayerEntrance = 0xB069
const val PlayerFireFlower = 0xB27D
const val PlayerGfxHandler = 0xEEE9
const val PlayerGfxProcessing = 0xEF45
const val PlayerGfxTblOffsets = 0xEE07
const val PlayerGraphicsTable = 0xEE17
const val PlayerHammerCollision = 0xD7C4
const val PlayerHeadCollision = 0xBCED
const val PlayerHole = 0xB179
const val PlayerInjuryBlink = 0xB245
const val PlayerInter = 0x86C2
const val PlayerKilled = 0xEF40
const val PlayerLakituDiff = 0xCF6C
const val PlayerLoseLife = 0x91CD
const val PlayerMovementSubs = 0xB329
const val PlayerOffscreenChk = 0xEF7A
const val PlayerPhysicsSub = 0xB450
const val PlayerPosSPlatData = 0xDC17
const val PlayerRdy = 0xB0D3
const val PlayerStarting_X_Pos = 0x9116
const val PlayerStarting_Y_Pos = 0x911C
const val PlayerStop = 0x9869
const val PlayerSubs = 0xB14C
const val PlayerVictoryWalk = 0x83BD
const val PlayerYSpdData = 0xB432
const val PlyrPipe = 0xDDD7
const val PortLoop = 0x8E6C
const val PosBubl = 0xB714
const val PosJSpr = 0xB8D9
const val PosPlatform = 0xC871
const val PositionEnemyObj = 0xC1AB
const val PositionPlayerOnHPlat = 0xD614
const val PositionPlayerOnS_Plat = 0xDC19
const val PositionPlayerOnVPlat = 0xDC21
const val PowerUpAttributes = 0xE6CE
const val PowerUpGfxTable = 0xE6BE
const val PowerUpGrabFreqData = 0xF4DA
const val PowerUpObjHandler = 0xBC85
const val PrimaryGameSetup = 0x9061
const val PrincessSaved1 = 0x8DA8
const val PrincessSaved2 = 0x8DBF
const val PrintMsg = 0x843C
const val PrintStatusBarNumbers = 0x8F06
const val PrintVictoryMessages = 0x83F6
const val PrintWarpZoneNumbers = 0x8882
const val ProcADLoop = 0x950A
const val ProcAirBubbles = 0xB66E
const val ProcBowserFlame = 0xD1EB
const val ProcClimb = 0xB465
const val ProcELoop = 0xAF03
const val ProcEnemyCollisions = 0xDAB4
const val ProcEnemyDirection = 0xE0A5
const val ProcFireball_Bubble = 0xB624
const val ProcFireballs = 0xB664
const val ProcFirebar = 0xCD3C
const val ProcHammerBro = 0xC9D8
const val ProcHammerObj = 0xBAC3
const val ProcJumpCoin = 0xBBA7
const val ProcJumping = 0xB48B
const val ProcLPlatCollisions = 0xDBBC
const val ProcLoopCommand = 0xC0CC
const val ProcLoopb = 0x957B
const val ProcMove = 0xB33B
const val ProcMoveRedPTroopa = 0xCAFF
const val ProcOnGroundActs = 0xF00B
const val ProcPRun = 0xB52D
const val ProcSPlatCollisions = 0xDBBA
const val ProcSecondEnemyColl = 0xDAF1
const val ProcSkid = 0xB5B3
const val ProcSwim = 0xB393
const val ProcSwimmingB = 0xCBDF
const val ProcessAreaData = 0x9508
const val ProcessBowserHalf = 0xD1BC
const val ProcessCannons = 0xB9BC
const val ProcessEnemyData = 0xC144
const val ProcessLengthData = 0xF8CB
const val ProcessPlayerAction = 0xEFEC
const val ProcessWhirlpools = 0xB7B8
const val PullID = 0xC72F
const val PullOfsB = 0xEC45
const val PulleyRopeMetatiles = 0x97B7
const val PulleyRopeObject = 0x97BA
const val PutAtRightExtent = 0xC5D8
const val PutBehind = 0xBC7B
const val PutBlockMetatile = 0x8A97
const val PutLives = 0x884D
const val PutMTileB = 0xBD41
const val PutOldMT = 0xBD40
const val PutPlayerOnVine = 0xDE88
const val PutinPipe = 0xD40A
const val PwrUpJmp = 0xBC60
const val QuestionBlock = 0x9B0E
const val QuestionBlockRow_High = 0x9968
const val QuestionBlockRow_Low = 0x996B
const val RImpd = 0xDF5E
const val RSeed = 0xC4F8
const val RXSpd = 0xDB36
const val RaiseFlagSetoffFWorks = 0xD34E
const val RdyDecode = 0x9565
const val RdyNextA = 0xB2F6
const val ReadJoypads = 0x8E5C
const val ReadPortBits = 0x8E6A
const val ReadyNextEnemy = 0xDAAA
const val RedPTroopaGrav = 0xBFD1
const val RelWOfs = 0xF142
const val RelativeBlockPosition = 0xF159
const val RelativeBubblePosition = 0xF131
const val RelativeEnemyPosition = 0xF152
const val RelativeFireballPosition = 0xF13B
const val RelativeMiscPosition = 0xF148
const val RelativePlayerPosition = 0xF12A
const val RemBridge = 0x8ACD
const val RemoveBridge = 0xD015
const val RemoveCoin_Axe = 0x8A4D
const val RendBBuf = 0x94D3
const val RendBack = 0x941F
const val RendFore = 0x9455
const val RendTerr = 0x946D
const val RenderAreaGraphics = 0x88AE
const val RenderAttributeTables = 0x896A
const val RenderH = 0xE4F7
const val RenderPlayerSub = 0xEFBE
const val RenderPul = 0x97C8
const val RenderSceneryTerrain = 0x9404
const val RenderSidewaysPipe = 0x98B3
const val RenderUnderPart = 0x9B7D
const val RepeatByte = 0x8EB9
const val ReplaceBlockMetatile = 0x8A61
const val ResGTCtrl = 0xB786
const val ResJmpM = 0xE3A3
const val ResetMDr = 0xD0A6
const val ResetPalFireFlower = 0xB297
const val ResetPalStar = 0xB29A
const val ResetScreenTimer = 0x88A5
const val ResetSpritesAndScreenTimer = 0x889D
const val ResetTitle = 0x82C9
const val ResidualGravityCode = 0xBFA1
const val ResidualHeaderData = 0xF94E
const val ResidualMiscObjectCode = 0xE392
const val ResidualXSpdData = 0xD84D
const val Rest = 0xF792
const val RetEOfs = 0xC3E3
const val RetXC = 0xE429
const val RetYC = 0xE42B
const val ReversePlantSpeed = 0xD3D5
const val RevivalRateData = 0xD9D2
const val ReviveStunned = 0xCAC8
const val RevivedXSpeed = 0xC9D4
const val RghtFrict = 0xB5FC
const val RightCheck = 0x891A
const val RightPipe = 0xB22E
const val RightPlatform = 0xD63D
const val RiseFallPiranhaPlant = 0xD3EA
const val RotPRandomBit = 0x8131
const val Row23C = 0xEB89
const val Row3C = 0xEB7E
const val RowOfBricks = 0x9A2E
const val RowOfCoins = 0x99F2
const val RowOfSolidBlocks = 0x9A3E
const val RunAObj = 0x965F
const val RunAllH = 0xBB28
const val RunBBSubs = 0xBA76
const val RunBowser = 0xD065
const val RunBowserFlame = 0xC935
const val RunDemo = 0x82C0
const val RunEnemyObjectsCore = 0xC882
const val RunFB = 0xB6BE
const val RunFR = 0xDE73
const val RunFirebarObj = 0xC947
const val RunFireworks = 0xD295
const val RunGameOver = 0x9237
const val RunGameTimer = 0xB74F
const val RunHSubs = 0xBB2B
const val RunJCSubs = 0xBBE8
const val RunLargePlatform = 0xC965
const val RunNormalEnemies = 0xC8E0
const val RunOffscrBitsSubs = 0xF1D7
const val RunPUSubs = 0xBCD8
const val RunParser = 0xAF8F
const val RunRetainerObj = 0xC8D7
const val RunSmallPlatform = 0xC94D
const val RunSoundSubroutines = 0xF34B
const val RunStarFlagObj = 0xD2D9
const val RunVSubs = 0xB96A
const val SBBAt = 0xE8CD
const val SBMDir = 0xCBAA
const val SBlasJ = 0xF550
const val SBnce = 0xD9F1
const val SBwsrGfxOfs = 0xE8FE
const val SChk2 = 0xE61A
const val SChk3 = 0xE624
const val SChk4 = 0xE62E
const val SChk5 = 0xE638
const val SChk6 = 0xE642
const val SChkA = 0xE07B
const val SFcRt = 0xDA0D
const val SFlmX = 0xD1F9
const val SJumpSnd = 0xB51A
const val SLChk = 0xE64B
const val SOLft = 0xE31F
const val SORte = 0xE306
const val SOfs = 0xEDC3
const val SOfs2 = 0xEDD1
const val SPBBox = 0xC82B
const val SPixelLak = 0xCFD6
const val SUpdR = 0xEF76
const val Save8Bits = 0x8E8D
const val SaveAB = 0xAF67
const val SaveHAdder = 0x8AA8
const val SaveJoyp = 0xB104
const val SaveXSpd = 0xBF23
const val SceLoop1 = 0x9445
const val SceLoop2 = 0x945F
const val ScoreOffsets = 0xBBFA
const val ScoreUpdateData = 0x84B7
const val ScreenOff = 0x809E
const val ScreenRoutines = 0x8567
const val ScrollHandler = 0xAF93
const val ScrollLockObject = 0x970D
const val ScrollLockObject_Warp = 0x96F2
const val ScrollScreen = 0xAFC4
const val SdeCLoop = 0xE10A
const val SecondBoxVerticalChk = 0xE34C
const val SecondPartMsg = 0x8423
const val SecondSprTilenum = 0xE4D4
const val SecondSprXPos = 0xE4C8
const val SecondSprYPos = 0xE4CC
const val SecondaryGameSetup = 0x9071
const val SelectBLogic = 0x8276
const val Set17ID = 0xC6C3
const val SetATHigh = 0x897D
const val SetAbsSpd = 0xB620
const val SetAmtOffset = 0x81F2
const val SetAnimC = 0xF08C
const val SetAnimSpd = 0xB5C5
const val SetAttrib = 0x8930
const val SetBBox = 0xC35C
const val SetBBox2 = 0xC7DB
const val SetBFlip = 0xEC21
const val SetBGColor = 0x8621
const val SetBPA = 0xC7F8
const val SetBehind = 0x956B
const val SetBitsMask = 0xDA25
const val SetCATmr = 0xDDEA
const val SetCAnim = 0xB475
const val SetCollisionFlag = 0xDBEB
const val SetCrouch = 0xB338
const val SetD6Ste = 0xE0FC
const val SetDBSte = 0xD77D
const val SetDplSpd = 0xBFC5
const val SetESpd = 0xC319
const val SetEndTimer = 0x8456
const val SetEntr = 0xB1DD
const val SetFBTmr = 0xD173
const val SetFWC = 0xD309
const val SetFallS = 0xDC82
const val SetFlameTimer = 0xD1D9
const val SetFor1Up = 0xD83A
const val SetForStn = 0xE09A
const val SetFore = 0x96EE
const val SetFrT = 0xC5C9
const val SetFreq_Squ1 = 0xF38B
const val SetFreq_Squ2 = 0xF3A9
const val SetFreq_Tri = 0xF3AD
const val SetGfxF = 0xD220
const val SetHFAt = 0xF2A0
const val SetHJ = 0xCA37
const val SetHPos = 0xBB09
const val SetHSpd = 0xBAF3
const val SetHalfway = 0x920F
const val SetHiMax = 0xBF94
const val SetHmrTmr = 0xD127
const val SetInitNTHigh = 0x9012
const val SetKRout = 0xD946
const val SetLMov = 0xCF67
const val SetLMovD = 0xCF9C
const val SetLSpd = 0xCF53
const val SetLast2Platform = 0xE5EB
const val SetM2 = 0xD005
const val SetMF = 0xC614
const val SetMFbar = 0xCD96
const val SetMOfs = 0xBAA0
const val SetMdMax = 0xBF8E
const val SetMiscOffset = 0x81F9
const val SetMoveDir = 0xB14A
const val SetNotW = 0xE04A
const val SetOffscrBitsOffset = 0xF1BA
const val SetOscrO = 0xF280
const val SetPESub = 0x91B8
const val SetPRout = 0xD948
const val SetPSte = 0xDC84
const val SetPVar = 0xD628
const val SetPWh = 0xB839
const val SetPause = 0x81C2
const val SetPlatformTilenum = 0xE5FD
const val SetRSpd = 0xCADF
const val SetRTmr = 0xB559
const val SetRunSpd = 0xB5AD
const val SetSDir = 0xCE78
const val SetSecHard = 0x9047
const val SetShim = 0xCA75
const val SetSpSpd = 0xC434
const val SetStPos = 0x9165
const val SetStun = 0xE02F
const val SetVFbr = 0xCE03
const val SetVRAMAddr_A = 0x85C5
const val SetVRAMAddr_B = 0x864C
const val SetVRAMCtrl = 0x89BD
const val SetVRAMOffset = 0x863F
const val SetVXPl = 0xDEA1
const val SetWYSpd = 0xE048
const val SetXMoveAmt = 0xBF96
const val SetYGp = 0xC73A
const val SetYO = 0xC81F
const val SetoffF = 0xD359
const val SetupBB = 0xBA4D
const val SetupBubble = 0xB70B
const val SetupCannon = 0x9A85
const val SetupEOffsetFBBox = 0xE27C
const val SetupExpl = 0xD2A5
const val SetupFloateyNumber = 0xDA11
const val SetupGFB = 0xCD6A
const val SetupGameOver = 0x9224
const val SetupIntermediate = 0x859B
const val SetupJumpCoin = 0xBB51
const val SetupLakitu = 0xC38A
const val SetupNumSpr = 0x8537
const val SetupNums = 0x8F28
const val SetupPlatformRope = 0xD541
const val SetupPowerUp = 0xBC49
const val SetupToMovePPlant = 0xD3E0
const val SetupVictoryMode = 0x83B0
const val SetupWrites = 0x8EA9
const val Setup_Vine = 0xB91E
const val ShellCollisions = 0xDAD9
const val ShellOrBlockDefeat = 0xD795
const val Shimmy = 0xCA62
const val ShrPlF = 0xF0E5
const val ShrinkPlatform = 0xE5E9
const val ShrinkPlayer = 0xF0D7
const val ShroomM = 0xBCAA
const val Shroom_Flower_PUp = 0xD820
const val ShufAmtLoop = 0x90AB
const val ShuffleLoop = 0x81CF
const val SideC = 0xDC11
const val SideCheckLoop = 0xDD66
const val SideExitPipeEntry = 0xB206
const val SidePipeBottomPart = 0x98A7
const val SidePipeShaftData = 0x989F
const val SidePipeTopPart = 0x98A3
const val SilenceData = 0xFA1C
const val SilenceHdr = 0xF958
const val SilentBeat = 0xF8B9
const val SixSpriteStacker = 0xE4AE
const val SizeChk = 0xB12B
const val SkipATRender = 0x92C7
const val SkipByte = 0x90DE
const val SkipCtrlL = 0xF7F1
const val SkipExpTimer = 0x8116
const val SkipFBar = 0xCDBA
const val SkipFqL1 = 0xF798
const val SkipIY = 0xD5EC
const val SkipMainOper = 0x8178
const val SkipMove = 0xC902
const val SkipPIn = 0xF344
const val SkipPT = 0xC979
const val SkipScore = 0xB896
const val SkipSoundSubroutines = 0xF35D
const val SkipSprite0 = 0x815C
const val SkipToFB = 0xD139
const val SkipToOffScrChk = 0xEA61
const val SkpFSte = 0xCD65
const val SkpVTop = 0xE490
const val SlidePlayer = 0xB2BC
const val SlowM = 0xCAB2
const val SlowSwim = 0xCC04
const val SmSpc = 0xF49D
const val SmTick = 0xF49F
const val SmallBBox = 0xC346
const val SmallBP = 0xBD61
const val SmallPlatformBoundBox = 0xE24C
const val SmallPlatformCollision = 0xDB7B
const val SndOn = 0xF2D9
const val SnglID = 0xC730
const val SolidBlockMetatiles = 0x9A25
const val SolidMTileUpperExt = 0xDF8B
const val SolidOrClimb = 0xDCEA
const val SoundEngine = 0xF2D0
const val SpawnBrickChunks = 0xBE41
const val SpawnFBr = 0xD154
const val SpawnFromMouth = 0xC5EC
const val SpawnHammerObj = 0xBA94
const val SpecObj = 0x960D
const val SpinCounterClockwise = 0xD424
const val SpinyRte = 0xC440
const val SpnySC = 0xEAD3
const val SprInitLoop = 0x8227
const val SprObjectCollisionCore = 0xE327
const val SprObjectOffscrChk = 0xEB64
const val Sprite0Clr = 0x813D
const val Sprite0Data = 0x8FCB
const val Sprite0Hit = 0x8150
const val SpriteShuffler = 0x81C6
const val Squ1NoteHandler = 0xF7DC
const val Squ2LengthHandler = 0xF77A
const val Squ2NoteHandler = 0xF786
const val Square1SfxHandler = 0xF41B
const val Square2SfxHandler = 0xF57C
const val StaircaseHeightData = 0x9AA5
const val StaircaseObject = 0x9AB7
const val StaircaseRowData = 0x9AAE
const val StarBlock = 0xBDD5
const val StarFChk = 0xC64C
const val StarFlagExit = 0xD311
const val StarFlagExit2 = 0xD3AF
const val StarFlagTileData = 0xD2D5
const val StarFlagXPosAdder = 0xD2D1
const val StarFlagYPosAdder = 0xD2CD
const val Star_CloudHdr = 0xF943
const val Star_CloudMData = 0xF9B8
const val Start = 0x8000
const val StartBTmr = 0xBD2C
const val StartClrGet = 0x8606
const val StartGame = 0x8255
const val StartPage = 0x8FFE
const val StartWorld1 = 0x82E6
const val StatusBarData = 0x8EF4
const val StatusBarNybbles = 0xBBFC
const val StatusBarOffset = 0x8F00
const val SteadM = 0xCAB4
const val StillInGame = 0x91E9
const val StkLp = 0xE4B0
const val StnE = 0xD7A1
const val StompedEnemyPtsData = 0xD965
const val StopGrowItems = 0xF628
const val StopPlatforms = 0xD5B1
const val StopPlayerMove = 0xDDFF
const val StopSquare1Sfx = 0xF4A7
const val StopSquare2Sfx = 0xF571
const val StoreFore = 0x9C68
const val StoreMT = 0x9488
const val StoreMusic = 0x9110
const val StoreNewD = 0x8F75
const val StoreStyle = 0x9CA3
const val StrAObj = 0x9656
const val StrBlock = 0x94F5
const val StrCOffset = 0x9AA1
const val StrFre = 0xC224
const val StrID = 0xC208
const val StrSprOffset = 0x81E2
const val StrType = 0xBC79
const val StrWOffset = 0x9B70
const val StrWave = 0xF37D
const val StrongBeat = 0xF8A9
const val SubDifAdj = 0xCFD1
const val SubtEnemyYPos = 0xE15B
const val SubtR1 = 0xCDE2
const val SusFbar = 0xCD55
const val SwimCCXMoveData = 0xCC46
const val SwimCC_IDData = 0xC69A
const val SwimKT = 0xEF1F
const val SwimKickTileNum = 0xEEE7
const val SwimStompEnvelopeData = 0xF3B1
const val SwimX = 0xCBBB
const val SzOfs = 0xF09B
const val TInjE = 0xD923
const val TScrClear = 0x8739
const val TallBBox = 0xC35A
const val TallBBox2 = 0xC7D9
const val TaskLoop = 0x86E9
const val TerMTile = 0x947E
const val TerminateGame = 0x9248
const val TerrBChk = 0x94AA
const val TerrLoop = 0x9491
const val TerrainMetatiles = 0x93D8
const val TerrainRenderBits = 0x93DC
const val ThankPlayer = 0x8418
const val ThirdP = 0x9416
const val ThreeFrameExtent = 0xF06D
const val ThreeSChk = 0xB9C3
const val TimeRunOutMusData = 0xFC72
const val TimeRunningOutHdr = 0xF93E
const val TimeUpOn = 0xB79A
const val TitleScreenMode = 0x8231
const val TooFar = 0xD6D2
const val TopEx = 0xBE40
const val TopSP = 0xED9C
const val TopScoreCheck = 0x8F9E
const val TopStatusBarLine = 0x8752
const val TransLoop = 0x9297
const val TransposePlayers = 0x9282
const val TreeLedge = 0x974C
const val TriNoteHandler = 0xF83E
const val TwoPlayerGameOver = 0x87AB
const val TwoPlayerTimeUp = 0x8798
const val Unbreak = 0xBD78
const val UnderHammerBro = 0xE196
const val UndergroundMusData = 0xFD11
const val UndergroundMusHdr = 0xF953
const val UndergroundPaletteData = 0x8CEC
const val UpToFiery = 0xD847
const val UpToSuper = 0xD840
const val UpdScrollVar = 0xAF6F
const val UpdSte = 0xBED1
const val UpdateLoop = 0xBED6
const val UpdateNumber = 0xBC36
const val UpdateScreen = 0x8EDD
const val UpdateShroom = 0x82A9
const val UpdateTopScore = 0x8F97
const val UseAdder = 0xBF2C
const val UseBOffset = 0x8A87
const val UseMiscS = 0xBB92
const val UsePosv = 0xC433
const val VAHandl = 0xCDEE
const val VBlank1 = 0x800A
const val VBlank2 = 0x800F
const val VDrawLoop = 0xB979
const val VPipeSectLoop = 0x9890
const val VRAM_AddrTable_High = 0x806D
const val VRAM_AddrTable_Low = 0x805A
const val VRAM_Buffer_Offset = 0x8080
const val VariableObjOfsRelPos = 0xF165
const val VerticalPipe = 0x98E5
const val VerticalPipeData = 0x98DD
const val VerticalPipeEntry = 0xB1E5
const val VictoryMLoopBack = 0xF777
const val VictoryMode = 0x838B
const val VictoryModeSubroutines = 0x83A0
const val VictoryMusData = 0xFEC8
const val VictoryMusHdr = 0xF961
const val VineBlock = 0xBDDF
const val VineCollision = 0xDE7A
const val VineEntr = 0xB0AC
const val VineHeightData = 0xB949
const val VineObjectHandler = 0xB94B
const val VineTL = 0xE479
const val VineYPosAdder = 0xE433
const val Vine_AutoClimb = 0xB1C7
const val WBootCheck = 0x8018
const val WSelectBufferTemplate = 0x823F
const val WaitOneRow = 0x9BA0
const val WarpNum = 0x9701
const val WarpNumLoop = 0x8889
const val WarpPipe = 0x98F0
const val WarpZoneNumbers = 0x87F2
const val WarpZoneObject = 0xB7A4
const val WarpZoneWelcome = 0x87C0
const val WaterEventMusEnvData = 0xFFA2
const val WaterMus = 0xF8ED
const val WaterMusData = 0xFD52
const val WaterMusHdr = 0xF96B
const val WaterPaletteData = 0x8CA4
const val WaterPipe = 0x986F
const val WhLoop = 0xB7C7
const val WhPull = 0xB83B
const val WhirlpoolActivate = 0xB7F5
const val WinCastleMusHdr = 0xF971
const val WinLevelMusData = 0xFCB0
const val World1Areas = 0x9CBC
const val World2Areas = 0x9CC1
const val World3Areas = 0x9CC6
const val World4Areas = 0x9CCA
const val World5Areas = 0x9CCF
const val World6Areas = 0x9CD3
const val World7Areas = 0x9CD7
const val World8Areas = 0x9CDC
const val WorldAddrOffsets = 0x9CB4
const val WorldLivesDisplay = 0x8779
const val WorldSelectMessage1 = 0x8DDE
const val WorldSelectMessage2 = 0x8DEF
const val WrCMTile = 0xB999
const val WriteBlankMT = 0x8A58
const val WriteBlockMetatile = 0x8A6D
const val WriteBottomStatusLine = 0x865A
const val WriteBufferToScreen = 0x8E92
const val WriteGameText = 0x8808
const val WriteNTAddr = 0x8E2D
const val WritePPUReg1 = 0x8EED
const val WriteTopScore = 0x8749
const val WriteTopStatusLine = 0x8652
const val WrongChk = 0xC115
const val XLdBData = 0xF21E
const val XMRight = 0xCB7C
const val XMoveCntr_GreenPTroopa = 0xCB45
const val XMoveCntr_Platform = 0xCB47
const val XMovingPlatform = 0xD607
const val XOffscreenBitsData = 0xF1E3
const val XOfsLoop = 0xF1FA
const val XSpdSign = 0xB617
const val XSpeedAdderData = 0xC9D0
const val X_Physics = 0xB51C
const val X_SubtracterData = 0xB034
const val YLdBData = 0xF260
const val YMDown = 0xD5FB
const val YMovingPlatform = 0xD5D3
const val YOffscreenBitsData = 0xF22B
const val YOfsLoop = 0xF23D
const val YPDiff = 0xCCBF
const val YSway = 0xCB3B
const val Y_Bubl = 0xB748
const val YesEC = 0xDA9B
const val YesIn = 0xDFF2

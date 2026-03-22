@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.utils.ByteAccess
import com.ivieleague.smbtranslation.utils.ByteArrayAccess
import com.ivieleague.smbtranslation.utils.JoypadBits
import com.ivieleague.smbtranslation.utils.PpuControl
import com.ivieleague.smbtranslation.utils.PpuMask
import com.ivieleague.smbtranslation.utils.SpriteFlags
import com.ivieleague.smbtranslation.utils.access
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

annotation class RamLocation(val address: Int, val size: Int = -1)

class GameRam {
    companion object {
        val clean = GameRam()
        val props by lazy {
            GameRam::class.declaredMemberProperties.mapNotNull {
                val addr = it.findAnnotation<RamLocation>()?.address ?: return@mapNotNull null
                val prop = it as? KMutableProperty1<GameRam, *> ?: return@mapNotNull null
                addr to prop
            }
        }
        /** val ByteArray/UByteArray fields with @RamLocation — reset() must zero their contents */
        val arrayProps by lazy {
            GameRam::class.declaredMemberProperties.mapNotNull { prop ->
                val addr = prop.findAnnotation<RamLocation>()?.address ?: return@mapNotNull null
                val getter = prop.getter
                // Only include val array properties (excluded from props because they're not KMutableProperty1)
                if (prop is KMutableProperty1<*, *>) return@mapNotNull null
                val sample = getter.call(clean)
                val size = when (sample) {
                    is ByteArray -> sample.size
                    is UByteArray -> sample.size
                    else -> return@mapNotNull null
                }
                Triple(addr, size, getter)
            }
        }
    }
    fun reset(range: IntRange) {
        // Reset var scalar properties
        props.forEach { (addr, prop) ->
            prop as KMutableProperty1<GameRam, Any?>
            if(addr in range) prop.set(this, prop.get(clean))
        }
        // Zero val ByteArray/UByteArray contents whose NES base address overlaps the cleared range
        for ((base, size, getter) in arrayProps) {
            val arr = getter.call(this)
            for (i in 0 until size) {
                if ((base + i) in range) when (arr) {
                    is ByteArray -> arr[i] = 0
                    is UByteArray -> arr[i] = 0u
                }
            }
        }
    }

    @RamLocation(0x100)
    val stack = Stack()

    inner class Stack {
        val data = ByteArray(0xFF)
        var currentIndex: Int = 0
        fun push(byte: Byte) {
            data[currentIndex++] = byte
        }

        fun pop(): Byte = data[--currentIndex]
        fun clear() {
            currentIndex = 0
        }
    }

    @RamLocation(0x8) var objectOffset: Byte = 0
    @RamLocation(0x9) var frameCounter: Byte = 0
    @RamLocation(0x6FC) var savedJoypadBits: JoypadBits = JoypadBits(0)
    var savedJoypad1Bits: JoypadBits // alias for savedJoypadBits (same NES address $6FC)
        get() = savedJoypadBits
        set(v) { savedJoypadBits = v }
    @RamLocation(0x6FD) var savedJoypad2Bits: JoypadBits = JoypadBits(0)
    @RamLocation(0x74a) var joypadBitMask: Byte = 0
    @RamLocation(0x758) var joypadOverride: Byte = 0
    @RamLocation(0xa) var aBButtons: Byte = 0
    @RamLocation(0xd) var previousABButtons: Byte = 0
    @RamLocation(0xb) var upDownButtons: Byte = 0
    @RamLocation(0xc) var leftRightButtons: Byte = 0
    @RamLocation(0xe, size = 0) var gameEngineSubroutine: GameEngineRoutine = GameEngineRoutine.EntranceGameTimerSetup
    @RamLocation(0x778) var mirrorPPUCTRLREG1: PpuControl = PpuControl(0)
    @RamLocation(0x779) var mirrorPPUCTRLREG2: PpuMask = PpuMask(0)
    @RamLocation(0x770) var operMode: OperMode = OperMode.TitleScreen
    @RamLocation(0x772) var operModeTask: Byte = 0
    @RamLocation(0x73c) var screenRoutineTask: Byte = 0
    @RamLocation(0x776) var gamePauseStatus: Byte = 0
    @RamLocation(0x777) var gamePauseTimer: Byte = 0
    @RamLocation(0x717) var demoAction: Byte = 0
    @RamLocation(0x718) var demoActionTimer: Byte = 0
    @RamLocation(0x747) var timerControl: Byte = 0
    @RamLocation(0x77f) var intervalTimerControl: Byte = 0
    @RamLocation(0x780) val timers = ByteArray(0x24) // by Claude - indices 0..0x23, accessed via ldx #$23
    // by Claude - named timer scalars backed by timers[] array for coherence with decTimers
    var selectTimer: Byte get() = timers[0x00]; set(v) { timers[0x00] = v }
    var playerAnimTimer: Byte get() = timers[0x01]; set(v) { timers[0x01] = v }
    var jumpSwimTimer: Byte get() = timers[0x02]; set(v) { timers[0x02] = v }
    var runningTimer: Byte get() = timers[0x03]; set(v) { timers[0x03] = v }
    var blockBounceTimer: Byte get() = timers[0x04]; set(v) { timers[0x04] = v }
    var sideCollisionTimer: Byte get() = timers[0x05]; set(v) { timers[0x05] = v }
    var jumpspringTimer: Byte get() = timers[0x06]; set(v) { timers[0x06] = v }
    var gameTimerCtrlTimer: Byte get() = timers[0x07]; set(v) { timers[0x07] = v }
    var climbSideTimer: Byte get() = timers[0x09]; set(v) { timers[0x09] = v }
    var enemyFrameTimer: Byte get() = timers[0x0a]; set(v) { timers[0x0a] = v }
    var frenzyEnemyTimer: Byte get() = timers[0x0f]; set(v) { timers[0x0f] = v }
    var bowserFireBreathTimer: Byte get() = timers[0x10]; set(v) { timers[0x10] = v }
    var stompTimer: Byte get() = timers[0x11]; set(v) { timers[0x11] = v }
    var airBubbleTimer: Byte get() = timers[0x12]; set(v) { timers[0x12] = v }
    var scrollIntervalTimer: Byte get() = timers[0x15]; set(v) { timers[0x15] = v }
    var enemyIntervalTimer: Byte get() = timers[0x16]; set(v) { timers[0x16] = v }
    var brickCoinTimer: Byte get() = timers[0x1d]; set(v) { timers[0x1d] = v }
    var injuryTimer: Byte get() = timers[0x1e]; set(v) { timers[0x1e] = v }
    var starInvincibleTimer: Byte get() = timers[0x1f]; set(v) { timers[0x1f] = v }
    var screenTimer: Byte get() = timers[0x20]; set(v) { timers[0x20] = v }
    var worldEndTimer: Byte get() = timers[0x21]; set(v) { timers[0x21] = v }
    var demoTimer: Byte get() = timers[0x22]; set(v) { timers[0x22] = v }

    class Sprite(
        var y: UByte = 0u,  // offset
        var tilenumber: Byte = 0,  // offset + 1
        var attributes: SpriteFlags = SpriteFlags(0),  // offset + 2
        var x: UByte = 0u,  // offset + 3
    ) {
        fun set(other: Sprite) {
            this.y = other.y
            this.tilenumber = other.tilenumber
            this.attributes = other.attributes
            this.x = other.x
        }
    }
    // NES OAM is 256 bytes (64 sprites × 4 bytes each).
    // sprDataOffsets stores NES byte offsets (0-252); divide by 4 to get sprite index.
    @RamLocation(0x200) val sprites = Array(64) { Sprite() }

    @RamLocation(0x71a) var screenEdgePageLoc: Byte = 0
    @RamLocation(0x71c) var screenEdgeXPos: Byte = 0
    var screenLeftPageLoc: Byte // alias for screenEdgePageLoc (same NES address $71A)
        get() = screenEdgePageLoc
        set(v) { screenEdgePageLoc = v }
    @RamLocation(0x71b) var screenRightPageLoc: Byte = 0
    var screenLeftXPos: Byte // alias for screenEdgeXPos (same NES address $71C)
        get() = screenEdgeXPos
        set(v) { screenEdgeXPos = v }
    @RamLocation(0x71d) var screenRightXPos: Byte = 0
    @RamLocation(0x33) var playerFacingDir: Byte = 0
    var destinationPageLoc: Byte // alias for firebarSpinDirection[0] (same NES address $34)
        get() = firebarSpinDirection[0]
        set(v) { firebarSpinDirection[0] = v }
    var victoryWalkControl: Byte // alias for firebarSpinDirection[1] (same NES address $35)
        get() = firebarSpinDirection[1]
        set(v) { firebarSpinDirection[1] = v }
    @RamLocation(0x768) var scrollFractional: Byte = 0
    @RamLocation(0x719) var primaryMsgCounter: Byte = 0
    @RamLocation(0x749) var secondaryMsgCounter: Byte = 0
    @RamLocation(0x73f) var horizontalScroll: Byte = 0
    @RamLocation(0x740) var verticalScroll: Byte = 0
    @RamLocation(0x723) var scrollLock: Byte = 0
    @RamLocation(0x73d) var scrollThirtyTwo: Byte = 0
    @RamLocation(0x6ff) var playerXScroll: Byte = 0
    @RamLocation(0x755) var playerPosForScroll: Byte = 0
    @RamLocation(0x775) var scrollAmount: Byte = 0

    var areaData: ByteArray? = null  // Indirect: pointer at 0xe7
    // Zero-page scratch bytes $EB-$EF: used as temp variables by various routines.
    // Mapped here so NES flat RAM reads (e.g., Setup_Vine with JumpEngine Y offset)
    // can access them via GameRamMapper.
    @RamLocation(0xeb) val zeroPageScratch: ByteArray = ByteArray(5) // $EB-$EF

    @RamLocation(0xe7) var areaDataLow: Byte = 0
    @RamLocation(0xe8) var areaDataHigh: Byte = 0
    var enemyDataBytes: ByteArray? = null  // by Claude - Indirect: pointer at 0xe9

    @RamLocation(0xe9) var enemyData: Byte = 0
    var enemyDataLow: Byte // alias for enemyData (same NES address $E9)
        get() = enemyData
        set(v) { enemyData = v }
    @RamLocation(0xea) var enemyDataHigh: Byte = 0
    @RamLocation(0x71f) var areaParserTaskNum: Byte = 0
    @RamLocation(0x71e) var columnSets: Byte = 0
    @RamLocation(0x725) var currentPageLoc: UByte = 0u
    @RamLocation(0x726) var currentColumnPos: UByte = 0u
    @RamLocation(0x728) var backloadingFlag: Boolean = false
    @RamLocation(0x729) var behindAreaParserFlag: Boolean = false
    @RamLocation(0x72a) var areaObjectPageLoc: Byte = 0
    @RamLocation(0x72b) var areaObjectPageSel: Byte = 0
    @RamLocation(0x72c) var areaDataOffset: Byte = 0
    @RamLocation(0x72d) val areaObjOffsetBuffer = ByteArray(3)
    @RamLocation(0x730) val areaObjectLength = ByteArray(3)
    @RamLocation(0x734) var staircaseControl: Byte = 0
    @RamLocation(0x735) var areaObjectHeight: Byte = 0
    @RamLocation(0x736) var mushroomLedgeHalfLen: Byte = 0
    @RamLocation(0x739) var enemyDataOffset: Byte = 0
    @RamLocation(0x73a) var enemyObjectPageLoc: Byte = 0
    @RamLocation(0x73b) var enemyObjectPageSel: Byte = 0
    @RamLocation(0x6a1) val metatileBuffer: UByteArray = UByteArray(0x0d)
    @RamLocation(0x6a0) var blockBufferColumnPos: Byte = 0
    @RamLocation(0x721) var currentNTAddrLow: Byte = 0
    @RamLocation(0x720) var currentNTAddrHigh: Byte = 0
    // Attribute buffer used during rendering; modeled as a small array of rows.
    @RamLocation(0x3f9, size = 7) val attributeBuffer: UByteArray = UByteArray(0x20)
    @RamLocation(0x745) var loopCommand: Byte = 0

    @RamLocation(0x7d7) val topScoreDisplay = ByteArray(6)
//    @RamLocation(0x7dd) val scoreAndCoinDisplay = ByteArray(0x18)
    @RamLocation(0x7dd) val playerScoreDisplay = ByteArray(6)
    @RamLocation(0x7e3) val player2ScoreDisplay = ByteArray(6)
    @RamLocation(0x7ED) val coinDisplay = ByteArray(2)
    @RamLocation(0x7f3) val coin2Display = ByteArray(2)
    @RamLocation(0x7f8) val gameTimerDisplay = ByteArray(3)
    @RamLocation(0x134, size = 6) val digitModifier: ByteArray = ByteArray(999)
    // Placeholder bookkeeping for score updates triggered by floatey numbers
    var lastScoreDigitIndex: Byte = 0
    var lastScoreDigitAdd: Byte = 0
    @RamLocation(0x109) var verticalFlipFlag: Byte = 0
    @RamLocation(0x110, size = 7) val floateyNumControl: ByteArray = ByteArray(ComboInfo.list.size)
    @RamLocation(0x117, size = 7) var floateyNumXPos: UByteArray = UByteArray(ComboInfo.list.size)
    @RamLocation(0x11e, size = 7) var floateyNumYPos: UByteArray = UByteArray(ComboInfo.list.size)
    @RamLocation(0x12c, size = 7) val floateyNumTimer: ByteArray = ByteArray(ComboInfo.list.size)
    var shellChainCounter: Byte
        get() = shellChainCounters[0]
        set(value) { shellChainCounters[0] = value }

    @RamLocation(0x10d) var flagpoleFNumYPos: Byte = 0
    @RamLocation(0x10e) var flagpoleFNumYMFDummy: Byte = 0
    @RamLocation(0x10f) var flagpoleScore: Byte = 0
    @RamLocation(0x70f) var flagpoleCollisionYPos: Byte = 0
    @RamLocation(0x484) var stompChainCounter: Byte = 0

    @RamLocation(0x300) val vRAMBuffer1 = MutableVBuffer()
    @RamLocation(0x340) val vRAMBuffer2 = MutableVBuffer()

    @RamLocation(0x773) var vRAMBufferAddrCtrl: Byte = 0

    @RamLocation(0x722) var sprite0HitDetectFlag: Boolean = false
    @RamLocation(0x774) var disableScreenFlag: Boolean = false
    @RamLocation(0x769) var disableIntermediate: Boolean = false
    @RamLocation(0x6d4) var colorRotateOffset: Byte = 0
    @RamLocation(0x727) var terrainControl: Byte = 0
    @RamLocation(0x733) var areaStyle: Byte = 0
    @RamLocation(0x741) var foregroundScenery: Byte = 0
    @RamLocation(0x742) var backgroundScenery: Byte = 0
    @RamLocation(0x743) var cloudTypeOverride: Boolean = false
    @RamLocation(0x744) var backgroundColorCtrl: Byte = 0
    @RamLocation(0x74e) var areaType: Byte = 0  // TODO: This should be an enum, I think
    @RamLocation(0x74f) var areaAddrsLOffset: Byte = 0
    @RamLocation(0x750) var areaPointer: Byte = 0
    @RamLocation(0x710) var playerEntranceCtrl: Byte = 0
    @RamLocation(0x715) var gameTimerSetting: Byte = 0
    @RamLocation(0x752) var altEntranceControl: Byte = 0
    @RamLocation(0x751) var entrancePage: Byte = 0
    @RamLocation(0x77a) var numberOfPlayers: Byte = 0
    @RamLocation(0x6d6) var warpZoneControl: Byte = 0
    @RamLocation(0x6de) var changeAreaTimer: Byte = 0
    @RamLocation(0x6d9) var multiLoopCorrectCntr: Byte = 0
    @RamLocation(0x6da) var multiLoopPassCntr: Byte = 0
    @RamLocation(0x757) var fetchNewGameTimerFlag: Boolean = false
    @RamLocation(0x759) var gameTimerExpiredFlag: Boolean = false
    @RamLocation(0x76a) var primaryHardMode: Boolean = false
    @RamLocation(0x6cc) var secondaryHardMode: Byte = 0
    @RamLocation(0x76b) var worldSelectNumber: Byte = 0
    @RamLocation(0x7fc) var worldSelectEnableFlag: Boolean = false
    @RamLocation(0x7fd) var continueWorld: Byte = 0
    @RamLocation(0x753) var currentPlayer: Byte = 0
    @RamLocation(0x754) var playerSize: Byte = 0
    @RamLocation(0x756) var playerStatus: Byte = 0
    @RamLocation(0x75a) var onscreenPlayerInfo: Byte = 0
    var numberofLives: Byte // alias for onscreenPlayerInfo (same NES address $75A)
        get() = onscreenPlayerInfo
        set(v) { onscreenPlayerInfo = v }
    @RamLocation(0x75b) var halfwayPage: Byte = 0
    @RamLocation(0x75c) var levelNumber: Byte = 0
    @RamLocation(0x75d) var hidden1UpFlag: Boolean = false
    @RamLocation(0x75e) var coinTally: Byte = 0
    @RamLocation(0x75f) var worldNumber: Byte = 0
    @RamLocation(0x760) var areaNumber: Byte = 0
    @RamLocation(0x748) var coinTallyFor1Ups: Byte = 0
    @RamLocation(0x761) var offscreenPlayerInfo: Byte = 0
    var offScrNumberofLives: Byte // alias for offscreenPlayerInfo (same NES address $761)
        get() = offscreenPlayerInfo
        set(v) { offscreenPlayerInfo = v }
    @RamLocation(0x762) var offScrHalfwayPage: Byte = 0
    @RamLocation(0x763) var offScrLevelNumber: Byte = 0
    @RamLocation(0x764) var offScrHidden1UpFlag: Boolean = false
    @RamLocation(0x765) var offScrCoinTally: Byte = 0
    @RamLocation(0x766) var offScrWorldNumber: Byte = 0
    @RamLocation(0x767) var offScrAreaNumber: Byte = 0
    @RamLocation(0x3a0) var balPlatformAlignment: Byte = 0
    @RamLocation(0x3a1) var platformXScroll: Byte = 0
    var platformCollisionFlag: Byte // alias for hammerThrowingTimers[0] (same NES address $3A2)
        get() = hammerThrowingTimers[0]
        set(v) { hammerThrowingTimers[0] = v }
    @RamLocation(0x6bc) var brickCoinTimerFlag: Byte = 0
    @RamLocation(0x746) var starFlagTaskControl: Byte = 0
    @RamLocation(0x7a7) val pseudoRandomBitReg = ByteArray(8)
    @RamLocation(0x7ff) var warmBootValidation: Boolean = false // by BooleanAccess(, trueValue = 0xa5.toByte())
    @RamLocation(0x6e0) var sprShuffleAmtOffset: Byte = 0
    @RamLocation(0x6e1, size = 0) val sprShuffleAmt = ByteArray(9999)  // size=0: excluded from sync

    // SprDataOffset is a contiguous table of 15 bytes starting at $6E4 used by SpriteShuffler
    @RamLocation(0x6e4) val sprDataOffsets = ByteArray(15)

    val playerSprDataOffset: ByteAccess = sprDataOffsets.access(0)  // RamLocation: 0x6e4
    val enemySprDataOffset: ByteArrayAccess = sprDataOffsets.access(1..7)  // RamLocation: 0x6e5
    val blockSprDataOffset: ByteArrayAccess = sprDataOffsets.access(8..9)  // RamLocation: 0x6ec
    val bubbleSprDataOffset: ByteArrayAccess = sprDataOffsets.access(10..11)  // RamLocation: 0x6ee
    val fBallSprDataOffset: ByteArrayAccess = sprDataOffsets.access(12..14)  // RamLocation: 0x6f1
    val altSprDataOffset: ByteArrayAccess = sprDataOffsets.access(8..14)  // RamLocation: 0x6ec

    // Misc_SprDataOffset is a contiguous table of 9 bytes starting at $6F3
    @RamLocation(0x6f3) val miscSprDataOffsets = ByteArray(9)

    @RamLocation(0x3ee) var sprDataOffsetCtrl: Byte = 0
    @RamLocation(0x1d) var playerState: Byte = 0
    @RamLocation(0x1e, size = 6) val enemyState = ByteArray(999)
    var fireballState: Byte
        get() = fireballStates[0]
        set(value) { fireballStates[0] = value }
    var blockState: Byte
        get() = blockStates[0]
        set(value) { blockStates[0] = value }
    var miscState: Byte
        get() = miscStates[0]
        set(value) { miscStates[0] = value }
    @RamLocation(0x45) var playerMovingDir: Byte = 0
    var enemyMovingDir: Byte
        get() = enemyMovingDirs[0]
        set(value) { enemyMovingDirs[0] = value }
    // by Claude - delegates to sprObjXSpeed[0] for scalar/array coherence (same NES address $57)
    var playerXSpeed: Byte
        get() = sprObjXSpeed[0]
        set(value) { sprObjXSpeed[0] = value }
    @RamLocation(0x70e) var jumpspringAnimCtrl: Byte = 0
    @RamLocation(0x6db) var jumpspringForce: Byte = 0
    // by Claude - delegates to sprObjPageLoc[] for scalar/array coherence
    var playerPageLoc: Byte
        get() = sprObjPageLoc[0]
        set(value) { sprObjPageLoc[0] = value }
    var enemyPageLoc: Byte // $6E = sprObjPageLoc[1]
        get() = sprObjPageLoc[1]
        set(value) { sprObjPageLoc[1] = value }
    var fireballPageLoc: Byte // $74 = sprObjPageLoc[7]
        get() = sprObjPageLoc[7]
        set(value) { sprObjPageLoc[7] = value }
    var blockPageLoc: Byte // $76 = sprObjPageLoc[9]
        get() = sprObjPageLoc[9]
        set(value) { sprObjPageLoc[9] = value }
    var miscPageLoc: Byte // $7A = sprObjPageLoc[13]
        get() = sprObjPageLoc[13]
        set(value) { sprObjPageLoc[13] = value }
    // by Claude - delegates to sprObjXPos[] for scalar/array coherence
    var playerXPosition: UByte
        get() = sprObjXPos[0].toUByte()
        set(value) { sprObjXPos[0] = value.toByte() }
    var enemyXPosition: Byte // $87 = sprObjXPos[1]
        get() = sprObjXPos[1]
        set(value) { sprObjXPos[1] = value }
    var fireballXPosition: Byte // $8D = sprObjXPos[7]
        get() = sprObjXPos[7]
        set(value) { sprObjXPos[7] = value }
    var blockXPosition: Byte // $8F = sprObjXPos[9]
        get() = sprObjXPos[9]
        set(value) { sprObjXPos[9] = value }
    var miscXPosition: Byte // $93 = sprObjXPos[13]
        get() = sprObjXPos[13]
        set(value) { sprObjXPos[13] = value }
    // by Claude - delegates to sprObjYSpeed[0] for scalar/array coherence (same NES address $9F)
    var playerYSpeed: Byte
        get() = sprObjYSpeed[0]
        set(value) { sprObjYSpeed[0] = value }
    // by Claude - delegates to sprObjYHighPos[0] for scalar/array coherence (same NES address $B5)
    var playerYHighPos: Byte
        get() = sprObjYHighPos[0]
        set(value) { sprObjYHighPos[0] = value }
    // by Claude - delegates to sprObjYPos[] for scalar/array coherence
    var playerYPosition: UByte
        get() = sprObjYPos[0].toUByte()
        set(value) { sprObjYPos[0] = value.toByte() }
    var blockYPosition: Byte // $D7 = sprObjYPos[9]
        get() = sprObjYPos[9]
        set(value) { sprObjYPos[9] = value }
    // by Claude - delegates to relXPos[] for scalar/array coherence
    var playerRelXPos: Byte
        get() = relXPos[0]
        set(value) { relXPos[0] = value }
    var enemyRelXPos: Byte
        get() = relXPos[1]
        set(value) { relXPos[1] = value }
    var fireballRelXPos: Byte
        get() = relXPos[2]
        set(value) { relXPos[2] = value }
    var bubbleRelXPos: Byte
        get() = relXPos[3]
        set(value) { relXPos[3] = value }
    var blockRelXPos: Byte
        get() = relXPos[4]
        set(value) { relXPos[4] = value }
    var miscRelXPos: Byte
        get() = relXPos[6]
        set(value) { relXPos[6] = value }
    // by Claude - delegates to relYPos[] for scalar/array coherence
    var playerRelYPos: Byte
        get() = relYPos[0]
        set(value) { relYPos[0] = value }
    var enemyRelYPos: Byte
        get() = relYPos[1]
        set(value) { relYPos[1] = value }
    var fireballRelYPos: Byte
        get() = relYPos[2]
        set(value) { relYPos[2] = value }
    var bubbleRelYPos: Byte
        get() = relYPos[3]
        set(value) { relYPos[3] = value }
    var blockRelYPos: Byte
        get() = relYPos[4]
        set(value) { relYPos[4] = value }
    var miscRelYPos: Byte
        get() = relYPos[6]
        set(value) { relYPos[6] = value }
    // by Claude - delegates to sprAttrib[0] for scalar/array coherence (same NES address $3C4)
    var playerSprAttrib: SpriteFlags
        get() = SpriteFlags(sprAttrib[0])
        set(value) { sprAttrib[0] = value.byte }
    // by Claude - delegates to sprObjYMFDummy[0] for scalar/array coherence (same NES address $416)
    var playerYMFDummy: Byte
        get() = sprObjYMFDummy[0]
        set(value) { sprObjYMFDummy[0] = value }
    // by Claude - delegates to sprObjYMoveForce[0] for scalar/array coherence (same NES address $433)
    var playerYMoveForce: Byte
        get() = sprObjYMoveForce[0]
        set(value) { sprObjYMoveForce[0] = value }
    @RamLocation(0x716) var disableCollisionDet: Byte = 0
    @RamLocation(0x490) var playerCollisionBits: Byte = 0
    var enemyCollisionBits: Byte
        get() = enemyCollisionBitsArr[0]
        set(value) { enemyCollisionBitsArr[0] = value }
    // by Claude - removed duplicate sprObjBoundBoxCtrl (same address $0499 as playerBoundBoxCtrl,
    // caused snapshot to capture stale value when two @RamLocation properties shared an address)
    @RamLocation(0x499) var playerBoundBoxCtrl: Byte = 0
    var enemyBoundBoxCtrl: Byte
        get() = enemyBoundBoxCtrls[0]
        set(value) { enemyBoundBoxCtrls[0] = value }
    var fireballBoundBoxCtrl: Byte
        get() = fireballBoundBoxCtrls[0]
        set(value) { fireballBoundBoxCtrls[0] = value }
    @RamLocation(0x4a2) val miscBoundBoxCtrls = ByteArray(9) // Misc_BoundBoxCtrl, indexed by misc slot
    var miscBoundBoxCtrl: Byte
        get() = miscBoundBoxCtrls[0]
        set(v) { miscBoundBoxCtrls[0] = v }
    @RamLocation(0x6cb) var enemyFrenzyBuffer: Byte = 0
    @RamLocation(0x6cd) var enemyFrenzyQueue: Byte = 0
    var enemyFlag: Byte
        get() = enemyFlags[0]
        set(value) { enemyFlags[0] = value }
    @RamLocation(0x16) val enemyID: ByteArray = ByteArray(6)  // Enemy_ID, $16-$1B
    @RamLocation(0x6d5) var playerGfxOffset: Byte = 0
    @RamLocation(0x700) var playerXSpeedAbsolute: Byte = 0
    @RamLocation(0x701) var frictionAdderHigh: Byte = 0
    @RamLocation(0x702) var frictionAdderLow: Byte = 0
    @RamLocation(0x703) var runningSpeed: Byte = 0
    @RamLocation(0x704) var swimmingFlag: Boolean = false
    @RamLocation(0x705) var playerXMoveForce: Byte = 0
    @RamLocation(0x706) var diffToHaltJump: Byte = 0
    @RamLocation(0x707) var jumpOriginYHighPos: Byte = 0
    @RamLocation(0x708) var jumpOriginYPosition: Byte = 0
    @RamLocation(0x709) var verticalForce: Byte = 0
    @RamLocation(0x70a) var verticalForceDown: Byte = 0
    @RamLocation(0x70b) var playerChangeSizeFlag: Byte = 0
    @RamLocation(0x70c) var playerAnimTimerSet: Byte = 0
    @RamLocation(0x70d) var playerAnimCtrl: Byte = 0
    @RamLocation(0x712) var deathMusicLoaded: Byte = 0
    @RamLocation(0x713) var flagpoleSoundQueue: Byte = 0
    @RamLocation(0x714) var crouchingFlag: Byte = 0
    @RamLocation(0x450) var maximumLeftSpeed: Byte = 0
    @RamLocation(0x456) var maximumRightSpeed: Byte = 0
    // by Claude - delegates to offscrBits[0] for scalar/array coherence (same NES address $3D0)
    var playerOffscreenBits: Byte
        get() = offscrBits[0]
        set(value) { offscrBits[0] = value }
    // by Claude - all offscreen bit scalars delegate to offscrBits[] for coherence
    var enemyOffscreenBits: Byte
        get() = offscrBits[1]
        set(value) { offscrBits[1] = value }
    var fBallOffscreenBits: Byte
        get() = offscrBits[2]
        set(value) { offscrBits[2] = value }
    var bubbleOffscreenBits: Byte
        get() = offscrBits[3]
        set(value) { offscrBits[3] = value }
    var blockOffscreenBits: Byte
        get() = offscrBits[4]
        set(value) { offscrBits[4] = value }
    var miscOffscreenBits: Byte
        get() = offscrBits[6]
        set(value) { offscrBits[6] = value }
    var enemyOffscrBitsMasked: Byte
        get() = enemyOffscrBitsMaskeds[0]
        set(value) { enemyOffscrBitsMaskeds[0] = value }
    @RamLocation(0x46a) var cannonOffset: Byte = 0
    var cannonPageLoc: Byte
        get() = cannonPageLocs[0]
        set(value) { cannonPageLocs[0] = value }
    var cannonXPosition: Byte
        get() = cannonXPositions[0]
        set(value) { cannonXPositions[0] = value }
    var cannonYPosition: Byte
        get() = cannonYPositions[0]
        set(value) { cannonYPositions[0] = value }
    var cannonTimer: Byte
        get() = cannonTimers[0]
        set(value) { cannonTimers[0] = value }
    var whirlpoolOffset: Byte // alias for cannonOffset (same NES address $46A)
        get() = cannonOffset
        set(v) { cannonOffset = v }
    var whirlpoolFlag: Byte // alias for cannonTimer (same NES address $47D)
        get() = cannonTimer
        set(v) { cannonTimer = v }
    @RamLocation(0x398) var vineFlagOffset: Byte = 0
    @RamLocation(0x399) var vineHeight: Byte = 0
    var vineObjOffset: Byte // alias for vineObjOffsets[0] (same NES address $39A)
        get() = vineObjOffsets[0]
        set(v) { vineObjOffsets[0] = v }
    @RamLocation(0x39d) var vineStartYPosition: Byte = 0
    // by Claude - Block object indexed fields (2 entries each, indexed by SprDataOffset_Ctrl 0/1)
    @RamLocation(0x3e4) val blockOrigYPos = ByteArray(2)    // Block_Orig_YPos, $03E4+x
    @RamLocation(0x3e6) val blockBBufLow = ByteArray(2)     // Block_BBuf_Low, $03E6+x
    @RamLocation(0x3e8) val blockMetatile = ByteArray(2)    // Block_Metatile, $03E8+x
    @RamLocation(0x3ea) val blockPageLoc2 = ByteArray(2)    // Block_PageLoc2, $03EA+x
    var blockRepFlag: Byte // alias for blockRepFlags[0] (same NES address $3EC)
        get() = blockRepFlags[0]
        set(v) { blockRepFlags[0] = v }
    @RamLocation(0x3f0) var blockResidualCounter: Byte = 0
    @RamLocation(0x3f1) val blockOrigXPos = ByteArray(2)    // Block_Orig_XPos, $03F1+x
    @RamLocation(0x4AC, size = 60) val boundBoxCoords = ByteArray(100) // 25 objects × 4 bytes
    var boundingBoxULXPos: Byte
        get() = boundBoxCoords[0]
        set(v) { boundBoxCoords[0] = v }
    var boundingBoxULYPos: Byte
        get() = boundBoxCoords[1]
        set(v) { boundBoxCoords[1] = v }
    var boundingBoxDRXPos: Byte
        get() = boundBoxCoords[2]
        set(v) { boundBoxCoords[2] = v }
    var boundingBoxDRYPos: Byte
        get() = boundBoxCoords[3]
        set(v) { boundBoxCoords[3] = v }
    var enemyBoundingBoxCoord: Byte
        get() = boundBoxCoords[4]
        set(v) { boundBoxCoords[4] = v }
    var powerUpType: Byte // alias for firebarSpinDirection[5] (same NES address $39)
        get() = firebarSpinDirection[5]
        set(v) { firebarSpinDirection[5] = v }
    var fireballBouncingFlag: Byte // alias for fireballBouncingFlags[0] (same NES address $3A)
        get() = fireballBouncingFlags[0]
        set(v) { fireballBouncingFlags[0] = v }
    @RamLocation(0x6ce) var fireballCounter: Byte = 0
    @RamLocation(0x711) var fireballThrowingTimer: Byte = 0
    @RamLocation(0x6ae) val hammerEnemyOffsets: ByteArray = ByteArray(9)
    @RamLocation(0x6b7) var jumpCoinMiscOffset: Byte = 0
    @RamLocation(0x500) val blockBuffer1: ByteArray = ByteArray(0xd0)
    @RamLocation(0x5d0) val blockBuffer2: ByteArray = ByteArray(0xd0)
    // by Claude - converted to arrays; assembly indexes with ,x (enemy index 0-5)
    @RamLocation(0x3a2) val hammerThrowingTimers: ByteArray = ByteArray(6)
    @RamLocation(0x3c) val hammerBroJumpTimers: ByteArray = ByteArray(6)
    var miscCollisionFlag: Byte // alias for miscCollisionFlags[0] (same NES address $6BE)
        get() = miscCollisionFlags[0]
        set(v) { miscCollisionFlags[0] = v }
    @RamLocation(0x6dd) var bitMFilter: Byte = 0
    @RamLocation(0x6d1) var lakituReappearTimer: Byte = 0
    @RamLocation(0x388) val firebarSpinSpeed = ByteArray(6) // FirebarSpinSpeed, indexed by enemy slot
    @RamLocation(0x34) val firebarSpinDirection = ByteArray(6) // FirebarSpinDirection, indexed by enemy slot (overlaps destinationPageLoc/victoryWalkControl)
    @RamLocation(0x6cf) var duplicateObjOffset: Byte = 0
    @RamLocation(0x6d3) var numberofGroupEnemies: Byte = 0
    @RamLocation(0x363) var bowserBodyControls: Byte = 0
    @RamLocation(0x364) var bowserFeetCounter: Byte = 0
    @RamLocation(0x365) var bowserMovementSpeed: Byte = 0
    @RamLocation(0x366) var bowserOrigXPos: Byte = 0
    @RamLocation(0x367) var bowserFlameTimerCtrl: Byte = 0
    @RamLocation(0x368) var bowserFrontOffset: Byte = 0
    @RamLocation(0x369) var bridgeCollapseOffset: Byte = 0
    @RamLocation(0x36a) var bowserGfxFlag: Byte = 0
    @RamLocation(0x483) var bowserHitPoints: Byte = 0
    @RamLocation(0x6dc) var maxRangeFromOrigin: Byte = 0
    @RamLocation(0x6d7) var fireworksCounter: Byte = 0

    // by Claude - SprObject flat indexed arrays
    // These provide indexed access across all object types using the SprObject offset:
    // 0=player, 1-6=enemies, 7-8=fireballs, 9-10=blocks, 13-17=misc, 22-24=bubbles
    @RamLocation(0x57, size = 22) val sprObjXSpeed = ByteArray(25)
    @RamLocation(0x6D) val sprObjPageLoc = ByteArray(25)
    @RamLocation(0x86) val sprObjXPos = ByteArray(25)
    @RamLocation(0x9F, size = 22) val sprObjYSpeed = ByteArray(25)
    @RamLocation(0xB5) val sprObjYHighPos = ByteArray(25)
    @RamLocation(0xCE) val sprObjYPos = ByteArray(25)
    @RamLocation(0x3C4, size = 12) val sprAttrib = ByteArray(25)
    @RamLocation(0x400, size = 22) val sprObjXMoveForce = ByteArray(25)
    @RamLocation(0x416) val sprObjYMFDummy = ByteArray(25)
    @RamLocation(0x433) val sprObjYMoveForce = ByteArray(25)

    // by Claude - Condensed offset arrays (9 entries)
    // 0=player, 1=enemy, 2=fireball, 3=bubble, 4-5=block, 6-8=misc
    @RamLocation(0x3AD) val relXPos = ByteArray(9)
    @RamLocation(0x3B8) val relYPos = ByteArray(9)
    @RamLocation(0x3D0, size = 7) val offscrBits = ByteArray(9)

    // by Claude - Object-type-specific indexed arrays
    @RamLocation(0x46) val enemyMovingDirs = ByteArray(6)
    @RamLocation(0x0F) val enemyFlags = ByteArray(6)
    @RamLocation(0x49A) val enemyBoundBoxCtrls = ByteArray(6)
    @RamLocation(0x24) val fireballStates = ByteArray(2)
    @RamLocation(0x26) val blockStates = ByteArray(2)
    // by Claude - Misc_State: 9 entries ($2A-$32). Indices 0-4 are pure misc objects (hammers, coins).
    // Indices 5-8 overlap with object variables: [3]=$2D Bowser, [4]=$2E PowerUpObject,
    // [5]=$2F VineObject, [6]=$30 FlagpoleFlagObject, [7]=$31 StarFlagObject, [8]=$32 JumpspringObject.
    @RamLocation(0x2A) val miscStates = ByteArray(9)
    @RamLocation(0x3ec) val blockRepFlags = ByteArray(2)       // Block_RepFlag, base $3EC
    @RamLocation(0x125) val shellChainCounters = ByteArray(6)
    @RamLocation(0x4A0) val fireballBoundBoxCtrls = ByteArray(2)

    // by Claude - Cannon arrays (6 entries each)
    @RamLocation(0x46B) val cannonPageLocs = ByteArray(6)
    @RamLocation(0x471) val cannonXPositions = ByteArray(6)
    @RamLocation(0x477) val cannonYPositions = ByteArray(6)
    @RamLocation(0x47D) val cannonTimers = ByteArray(6)

    // by Claude - Enemy collision/offscreen indexed arrays
    @RamLocation(0x491) val enemyCollisionBitsArr = ByteArray(6)
    @RamLocation(0x3D8) val enemyOffscrBitsMaskeds = ByteArray(6)

    // by Claude - Collision flag arrays (previously WeakHashMap extensions, now proper members)
    @RamLocation(0x3a) val fireballBouncingFlags = ByteArray(2)    // FireballBouncingFlag, base $3A
    @RamLocation(0x6be) val miscCollisionFlags = ByteArray(9)       // Misc_Collision_Flag, base $6BE
    @RamLocation(0x39a) val vineObjOffsets = ByteArray(3)           // VineObjOffset, base $39A
    val platformCollisionFlags: ByteArray // alias for hammerThrowingTimers (same NES address $3A2)
        get() = hammerThrowingTimers

    @RamLocation(0x7b0) var musicOffsetNoise: Byte = 0
    @RamLocation(0x7b1) var eventMusicBuffer: Byte = 0
    @RamLocation(0x7b2) var pauseSoundBuffer: Byte = 0
    @RamLocation(0x7b3) var squ2NoteLenBuffer: Byte = 0
    @RamLocation(0x7b4) var squ2NoteLenCounter: Byte = 0
    @RamLocation(0x7b5) var squ2EnvelopeDataCtrl: Byte = 0
    @RamLocation(0x7b6) var squ1NoteLenCounter: Byte = 0
    @RamLocation(0x7b7) var squ1EnvelopeDataCtrl: Byte = 0
    @RamLocation(0x7b8) var triNoteLenBuffer: Byte = 0
    @RamLocation(0x7b9) var triNoteLenCounter: Byte = 0
    @RamLocation(0x7ba) var noiseBeatLenCounter: Byte = 0
    @RamLocation(0x7bb) var squ1SfxLenCounter: Byte = 0
    @RamLocation(0x7bd) var squ2SfxLenCounter: Byte = 0
    @RamLocation(0x7be) var sfxSecondaryCounter: Byte = 0
    @RamLocation(0x7bf) var noiseSfxLenCounter: Byte = 0
    @RamLocation(0x7c0) var dACCounter: Byte = 0
    @RamLocation(0x7c1) var noiseDataLoopbackOfs: Byte = 0
    @RamLocation(0x7c4) var noteLengthTblAdder: Byte = 0
    @RamLocation(0x7c5) var areaMusicBufferAlt: Byte = 0
    @RamLocation(0x7c6) var pauseModeFlag: Byte = 0
    @RamLocation(0x7c7) var groundMusicHeaderOfs: Byte = 0
    @RamLocation(0x7ca) var altRegContentFlag: Byte = 0

    @RamLocation(0xfa) var pauseSoundQueue: Byte = 0
    @RamLocation(0xff) var square1SoundQueue: Byte = 0
    @RamLocation(0xfe) var square2SoundQueue: Byte = 0
    @RamLocation(0xfd) var noiseSoundQueue: Byte = 0
    @RamLocation(0xfb) var areaMusicQueue: Byte = 0
    @RamLocation(0xfc) var eventMusicQueue: Byte = 0
    @RamLocation(0xf1) var square1SoundBuffer: Byte = 0
    @RamLocation(0xf2) var square2SoundBuffer: Byte = 0
    @RamLocation(0xf3) var noiseSoundBuffer: Byte = 0
    @RamLocation(0xf4) var areaMusicBuffer: Byte = 0
    @RamLocation(0xf5) var musicData: Byte = 0
    @RamLocation(0xf6) var musicDataHigh: Byte = 0
    @RamLocation(0xf7) var musicOffsetSquare2: Byte = 0
    @RamLocation(0xf8) var musicOffsetSquare1: Byte = 0
    @RamLocation(0xf9) var musicOffsetTriangle: Byte = 0
    @RamLocation(0xf0) var noteLenLookupTblOfs: Byte = 0

}
package com.ivieleague.smbtranslation.old

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Timers() {
    companion object {
        var current = Timers()
    }
    val bytes = ByteArray(0x23)

    private infix fun ByteArray.index(i: Int) = object: ReadWriteProperty<Any?, Byte> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Byte {
            return this@index[i]
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Byte) {
            this@index[i] = value
        }
    }

    //> IntervalTimerControl  = $077f
    var intervalTimerControl = 0x0

    //> SelectTimer           = $0780
    var selectTimer: Byte by bytes index (0x0780 - 0x0780)
    //> PlayerAnimTimer       = $0781
    var playerAnimTimer: Byte by bytes index (0x0781 - 0x0780)
    //> JumpSwimTimer         = $0782
    var jumpSwimTimer: Byte by bytes index (0x0782 - 0x0780)
    //> RunningTimer          = $0783
    var runningTimer: Byte by bytes index (0x0783 - 0x0780)
    //> BlockBounceTimer      = $0784
    var blockBounceTimer: Byte by bytes index (0x0784 - 0x0780)
    //> SideCollisionTimer    = $0785
    var sideCollisionTimer: Byte by bytes index (0x0785 - 0x0780)
    //> JumpspringTimer       = $0786
    var jumpspringTimer: Byte by bytes index (0x0786 - 0x0780)
    //> GameTimerCtrlTimer    = $0787
    var gameTimerCtrlTimer: Byte by bytes index (0x0787 - 0x0780)
    //> ClimbSideTimer        = $0789
    var climbSideTimer: Byte by bytes index (0x0789 - 0x0780)
    //> EnemyFrameTimer       = $078a
    var enemyFrameTimer: Byte by bytes index (0x078a - 0x0780)
    //> FrenzyEnemyTimer      = $078f
    var frenzyEnemyTimer: Byte by bytes index (0x078f - 0x0780)
    //> BowserFireBreathTimer = $0790
    var bowserFireBreathTimer: Byte by bytes index (0x0790 - 0x0780)
    //> StompTimer            = $0791
    var stompTimer: Byte by bytes index (0x0791 - 0x0780)

    //> AirBubbleTimer        = $0792
    var airBubbleTimer: Byte by bytes index (0x0792 - 0x0780)
    //> ScrollIntervalTimer   = $0795
    var scrollIntervalTimer: Byte by bytes index (0x0795 - 0x0780)
    //> EnemyIntervalTimer    = $0796
    var enemyIntervalTimer: Byte by bytes index (0x0796 - 0x0780)
    //> BrickCoinTimer        = $079d
    var brickCoinTimer: Byte by bytes index (0x079d - 0x0780)
    //> InjuryTimer           = $079e
    var injuryTimer: Byte by bytes index (0x079e - 0x0780)
    //> StarInvincibleTimer   = $079f
    var starInvincibleTimer: Byte by bytes index (0x079f - 0x0780)
    //> ScreenTimer           = $07a0
    var screenTimer: Byte by bytes index (0x07a0 - 0x0780)
    //> WorldEndTimer         = $07a1
    var worldEndTimer: Byte by bytes index (0x07a1 - 0x0780)
    //> DemoTimer             = $07a2
    var demoTimer: Byte by bytes index (0x07a2 - 0x0780)
}
// by Claude - Named constants for metatile IDs used in block buffers and collision detection
package com.ivieleague.smbtranslation

/**
 * Named constants for NES metatile IDs (0x00-0xEB).
 * Metatiles are 16x16 pixel background tiles stored in the block buffer.
 * Values are Int because metatile comparisons use `toInt() and 0xFF` throughout.
 */
object MetatileId {
    // Pipe metatiles (vertical)
    const val WARP_PIPE_TOP_LEFT: Int = 0x10   // pipe top that leads somewhere (left half)
    const val WARP_PIPE_TOP_RIGHT: Int = 0x11  // pipe top that leads somewhere (right half)

    // Sideways pipe joints (where horizontal meets vertical)
    const val SIDE_PIPE_JOINT_TOP: Int = 0x1c  // sideways pipe joint, top row
    const val SIDE_PIPE_JOINT_BOTTOM: Int = 0x1f // sideways pipe joint, bottom row

    // Used/bumped block (visually blank after being hit from below)
    const val USED_BLOCK: Int = 0x23

    // Flagpole
    const val FLAGPOLE_BALL: Int = 0x24
    const val FLAGPOLE_SHAFT: Int = 0x25

    // Vine/climbable blank
    const val VINE_METATILE: Int = 0x26

    // Brick variants — with visible line (area style 1)
    const val BREAKABLE_BRICK_WITH_LINE: Int = 0x51
    const val BREAKABLE_BRICK_WITHOUT_LINE: Int = 0x52

    // Brick with coins — two visual styles
    const val BRICK_WITH_COINS_WITH_LINE: Int = 0x58
    const val BRICK_WITH_COINS_WITHOUT_LINE: Int = 0x5d

    // Hidden blocks (invisible until hit from below)
    const val HIDDEN_COIN_BLOCK: Int = 0x5f
    const val HIDDEN_1UP_BLOCK: Int = 0x60  // SMB2J: also used as hidden poison mushroom block

    // Jumpspring
    const val JUMPSPRING_TOP: Int = 0x67
    const val JUMPSPRING_BOTTOM: Int = 0x68

    // Water pipe (vertical, in water areas)
    const val WATER_PIPE_TOP: Int = 0x6b
    const val WATER_PIPE_BOTTOM: Int = 0x6c

    // Coins
    const val COIN: Int = 0xc2
    const val UNDERWATER_COIN: Int = 0xc3

    // Empty block (question block or brick after contents removed)
    const val EMPTY_BLOCK: Int = 0xc4

    // Axe (end of Bowser's bridge)
    const val AXE: Int = 0xc5
}

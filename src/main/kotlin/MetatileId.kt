// by Claude - Named constants for metatile IDs used in block buffers and collision detection
package com.ivieleague.smbtranslation

/**
 * Named constants for NES metatile IDs.
 * Metatile IDs encode palette in the top 2 bits and index within the palette group in the lower 6 bits.
 * SMB2J has different metatile tables (shifted entries) so many IDs differ from SMB1.
 */
class MetatileId private constructor(val variant: GameVariant) {
    // Pipe metatiles (vertical) -- same in both games
    val WARP_PIPE_TOP_LEFT: Int = 0x10   // pipe top that leads somewhere (left half)
    val WARP_PIPE_TOP_RIGHT: Int = 0x11  // pipe top that leads somewhere (right half)

    // Sideways pipe joints (where horizontal meets vertical)
    val SIDE_PIPE_JOINT_TOP: Int = if (variant == GameVariant.SMB2J) 0x19 else 0x1c
    val SIDE_PIPE_JOINT_BOTTOM: Int = if (variant == GameVariant.SMB2J) 0x1c else 0x1f

    // Used/bumped block (visually blank after being hit from below)
    val USED_BLOCK: Int = if (variant == GameVariant.SMB2J) 0x20 else 0x23

    // Flagpole
    val FLAGPOLE_BALL: Int = if (variant == GameVariant.SMB2J) 0x21 else 0x24
    val FLAGPOLE_SHAFT: Int = if (variant == GameVariant.SMB2J) 0x22 else 0x25

    // Vine/climbable blank
    val VINE_METATILE: Int = if (variant == GameVariant.SMB2J) 0x23 else 0x26

    // Brick variants — with visible line (area style 1)
    val BREAKABLE_BRICK_WITH_LINE: Int = if (variant == GameVariant.SMB2J) 0x4f else 0x51
    val BREAKABLE_BRICK_WITHOUT_LINE: Int = if (variant == GameVariant.SMB2J) 0x50 else 0x52

    // Brick with coins — two visual styles
    val BRICK_WITH_COINS_WITH_LINE: Int = if (variant == GameVariant.SMB2J) 0x56 else 0x58
    val BRICK_WITH_COINS_WITHOUT_LINE: Int = if (variant == GameVariant.SMB2J) 0x5c else 0x5d

    // Hidden blocks (invisible until hit from below)
    val HIDDEN_COIN_BLOCK: Int = if (variant == GameVariant.SMB2J) 0x5e else 0x5f
    val HIDDEN_1UP_BLOCK: Int = if (variant == GameVariant.SMB2J) 0x5f else 0x60
    // SMB2J-only hidden blocks (not present in SMB1)
    val HIDDEN_POISON_BLOCK: Int = if (variant == GameVariant.SMB2J) 0x60 else -1
    val HIDDEN_POWERUP_BLOCK: Int = if (variant == GameVariant.SMB2J) 0x61 else -1

    // Solid block (3-D block used in stairs, flagpole base, etc.)
    val SOLID_BLOCK: Int = if (variant == GameVariant.SMB2J) 0x62 else 0x61

    // Bridge body (horizontal bridge platform)
    val BRIDGE_BODY: Int = if (variant == GameVariant.SMB2J) 0x64 else 0x63

    // Bullet Bill cannon parts (top, middle, bottom)
    val CANNON_TOP: Int = if (variant == GameVariant.SMB2J) 0x65 else 0x64
    val CANNON_MIDDLE: Int = if (variant == GameVariant.SMB2J) 0x66 else 0x65
    val CANNON_BOTTOM: Int = if (variant == GameVariant.SMB2J) 0x67 else 0x66

    // Jumpspring (SMB2J uses $68/$69 per sm2main lines 4054 and 11306)
    val JUMPSPRING_TOP: Int = if (variant == GameVariant.SMB2J) 0x68 else 0x67
    val JUMPSPRING_BOTTOM: Int = if (variant == GameVariant.SMB2J) 0x69 else 0x68

    // Flag balls (development leftover object)
    val FLAG_BALLS: Int = if (variant == GameVariant.SMB2J) 0x6f else 0x6d

    // Water pipe (vertical, in water areas)
    val WATER_PIPE_TOP: Int = if (variant == GameVariant.SMB2J) 0x6d else 0x6b
    val WATER_PIPE_BOTTOM: Int = if (variant == GameVariant.SMB2J) 0x6e else 0x6c

    // Coins
    val COIN: Int = if (variant == GameVariant.SMB2J) 0xc3 else 0xc2
    val UNDERWATER_COIN: Int = if (variant == GameVariant.SMB2J) 0xc4 else 0xc3

    // Empty block (question block or brick after contents removed)
    val EMPTY_BLOCK: Int = if (variant == GameVariant.SMB2J) 0xc5 else 0xc4

    // Axe (end of Bowser's bridge)
    val AXE: Int = if (variant == GameVariant.SMB2J) 0xc6 else 0xc5

    companion object {
        private val SMB1 = MetatileId(GameVariant.SMB1)
        private val SMB2J = MetatileId(GameVariant.SMB2J)

        fun forVariant(variant: GameVariant): MetatileId = when (variant) {
            GameVariant.SMB1 -> SMB1
            GameVariant.SMB2J -> SMB2J
        }
    }
}

/** Convenience accessor for the variant-appropriate MetatileId from a System context. */
val System.metatileId: MetatileId get() = MetatileId.forVariant(variant)

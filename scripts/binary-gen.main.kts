import kotlin.Boolean

data class BinaryType(
    val name: String,
    val fields: List<Field>
) {
    val code: String get() = buildString {
        println("@JvmInline")
        println("value class ${name}(val byte: Byte) {")
        for(field in fields) {
            field.codeComment?.let {
                println("    $it")
            }
            println("    ${field.codeField}")
        }
        println("    constructor(")
        for(field in fields) {
            println("        ${field.codeParameter} = ${field.codeDefault},")
        }
        println("    ): this((")
        for(field in fields) {
            if(field == fields.last())
                println("        (${field.codeSumPart})")
            else
                println("        (${field.codeSumPart}) +")
        }
        println("    ).toByte())")
        println("    fun copy(")
        for(field in fields) {
            println("        ${field.codeParameter} = this.${field.name},")
        }
        println("    ) = $name(${fields.joinToString(", ") { it.name }})")
        println("}")
    }
}

sealed class Field {
    abstract val name: String
    abstract val codeComment: String?
    abstract val codeField: String
    abstract val codeParameter: String
    abstract val codeDefault: String
    abstract val codeSumPart: String
}

data class BitField(override val name: String, val index: Int, val comment: String? = null) : Field() {
    override val codeComment = comment?.let { "/** $it **/" }
    override val codeField: String = "val $name: Boolean get() = byte.bit($index)"
    override val codeParameter: String = "$name: Boolean"
    override val codeDefault: String = "false"
    override val codeSumPart: String = "if ($name) 0x1 shl $index else 0"
}
data class BitRangeField(override val name: String, val start: Int, val endInclusive: Int, val comment: String? = null) : Field() {
    override val codeComment = comment?.let { "/** $it **/" }
    override val codeField: String = "val $name: Byte get() = byte.bitRange($start, $endInclusive)"
    override val codeParameter: String = "$name: Byte"
    override val codeDefault: String = "0"
    override val codeSumPart: String = "$name.toInt() shr $start and 1.shl(${endInclusive - start}).minus(1)"
}

val types = listOf(
    BinaryType("JoypadBits", listOf(
        BitField("a", 7),
        BitField("b", 6),
        BitField("select", 5),
        BitField("start", 4),
        BitField("up", 3),
        BitField("down", 2),
        BitField("left", 1),
        BitField("right", 0),
    )),
    BinaryType("SpriteFlags", listOf(
        BitField("flipVertical", 7),
        BitField("flipHorizontal", 6),
        BitField("behindBackground", 5),
        BitRangeField("palette", 0, 1)
    )),
    BinaryType("PpuControl", listOf(
        BitField("nmiEnabled", 7, "Vblank NMI enable (0: off, 1: on)"),
        BitField("extWrite", 6, "PPU master/slave select (0: read backdrop from EXT pins; 1: output color on EXT pins)"),
        BitField("tallSpriteMode", 5, "Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)"),
        BitField("backgroundTableOffset", 4, "Background pattern table address (0: $0000; 1: $1000)"),
        BitField("spritePatternTableOffset", 3, "Sprite pattern table address for 8x8 sprites (0: $0000; 1: $1000; ignored in 8x16 mode)"),
        BitField("drawVertical", 2, "VRAM address increment per CPU read/write of PPUDATA (0: add 1, going across; 1: add 32, going down)"),
        BitRangeField("baseNametableAddress", 0, 1, "Base nametable address (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)"),
    )),
    BinaryType("PpuMask", listOf(
        BitField("greyscale", 0, "Greyscale (0: normal color, 1: greyscale)"),
        BitField("showLeftSprites", 1, "1: Show background in leftmost 8 pixels of screen, 0: Hide"),
        BitField("showLeftBackground", 2, "1: Show sprites in leftmost 8 pixels of screen, 0: Hide"),
        BitField("backgroundEnabled", 3, "1: Enable background rendering"),
        BitField("spriteEnabled", 4, "1: Enable sprite rendering"),
        BitField("emphasizeRed", 5, "Emphasize red (green on PAL/Dendy)"),
        BitField("emphasizeGreen", 6, "Emphasize green (red on PAL/Dendy)"),
        BitField("emphasizeBlue", 7, "Emphasize blue"),
    )),
    BinaryType("PpuStatus", listOf(
//        VSO- ---- 	R 	vblank (V), sprite 0 hit (S), sprite overflow (O); read resets write pair for $2005/$2006
        BitField("vblank", 7),
        BitField("sprite0Hit", 6),
        BitField("spriteOverflow", 5),
    )),
    BinaryType("VramBufferControl", listOf(
        // control/len byte: bit7=1 -> increment by 32 (vertical), bit7=0 -> increment by 1 (horizontal);
        //                    bit6=1 -> repeat the next data byte for "len" times; bit6=0 -> use next "len" bytes;
        //                    bits0-5 = length (0..63)
        BitField("drawVertically", 7),
        BitField("repeat", 6),
        BitField("length", 5),
    )),
)

types.forEach { type ->
    println(type.code)
}

package com.ivieleague.smbtranslation.utils

import com.ivieleague.smbtranslation.utils.KotlinPart.Calculation
import kotlin.collections.plusAssign
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class AssemblyFlag {
    N, V, Z, C
}

enum class AssemblyOp(
    val description: String,
    val affectedFlags: Set<AssemblyFlag> = setOf(),
    val consumedFlag: AssemblyFlag? = null,
    val isBranch: Boolean = false,
    val flagPositive: Boolean = false
) {
    LDA(description = "Load Accumulator", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    LDX(description = "Load X Register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    LDY(description = "Load Y Register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    STA(description = "Store Accumulator"),
    STX(description = "Store X Register"),
    STY(description = "Store Y Register"),
    TAX(description = "Transfer accumulator to X", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    TAY(description = "Transfer accumulator to Y", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    TXA(description = "Transfer X to accumulator", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    TYA(description = "Transfer Y to accumulator", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    TSX(description = "Transfer stack pointer to X", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    TXS(description = "Transfer X to stack pointer"),
    PHA(description = "Push accumulator on stack"),
    PHP(description = "Push processor status on stack"),
    PLA(description = "Pull accumulator from stack", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    PLP(description = "Pull processor status from stack"),
    AND(description = "Logical AND", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    EOR(description = "Exclusive OR", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    ORA(description = "Logical Inclusive OR", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    BIT(description = "Bit Test", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.V, AssemblyFlag.Z)),
    ADC(
        description = "Add with Carry",
        affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.V, AssemblyFlag.Z, AssemblyFlag.C),
        consumedFlag = AssemblyFlag.C
    ),
    SBC(
        description = "Subtract with Carry",
        affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.V, AssemblyFlag.Z, AssemblyFlag.C),
        consumedFlag = AssemblyFlag.C
    ),
    CMP(description = "Compare accumulator", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    CPX(description = "Compare X register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    CPY(description = "Compare Y register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    INC(description = "Increment a memory location", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    INX(description = "Increment the X register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    INY(description = "Increment the Y register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    DEC(description = "Decrement a memory location", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    DEX(description = "Decrement the X register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    DEY(description = "Decrement the Y register", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z)),
    ASL(description = "Arithmetic Shift Left", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    LSR(description = "Logical Shift Right", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    ROL(description = "Rotate Left", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    ROR(description = "Rotate Right", affectedFlags = setOf(AssemblyFlag.N, AssemblyFlag.Z, AssemblyFlag.C)),
    JMP(description = "Jump to another location"),
    JSR(description = "Jump to a subroutine"),
    RTS(description = "Return from subroutine"),
    BCC(
        description = "Branch if carry flag clear",
        consumedFlag = AssemblyFlag.C,
        isBranch = true,
        flagPositive = false
    ),
    BCS(description = "Branch if carry flag set", consumedFlag = AssemblyFlag.C, isBranch = true, flagPositive = true),
    BEQ(description = "Branch if zero flag set", consumedFlag = AssemblyFlag.Z, isBranch = true, flagPositive = true),
    BMI(
        description = "Branch if negative flag set",
        consumedFlag = AssemblyFlag.N,
        isBranch = true,
        flagPositive = true
    ),
    BNE(
        description = "Branch if zero flag clear",
        consumedFlag = AssemblyFlag.Z,
        isBranch = true,
        flagPositive = false
    ),
    BPL(
        description = "Branch if negative flag clear",
        consumedFlag = AssemblyFlag.N,
        isBranch = true,
        flagPositive = false
    ),
    BVC(
        description = "Branch if overflow flag clear",
        consumedFlag = AssemblyFlag.V,
        isBranch = true,
        flagPositive = false
    ),
    BVS(
        description = "Branch if overflow flag set",
        consumedFlag = AssemblyFlag.V,
        isBranch = true,
        flagPositive = true
    ),
    CLC(description = "Clear carry flag", affectedFlags = setOf(AssemblyFlag.C)),
    CLD(description = "Clear decimal mode flag"),
    CLI(description = "Clear interrupt disable flag"),
    CLV(description = "Clear overflow flag", affectedFlags = setOf(AssemblyFlag.V)),
    SEC(description = "Set carry flag", affectedFlags = setOf(AssemblyFlag.C)),
    SED(description = "Set decimal mode flag"),
    SEI(description = "Set interrupt disable flag"),
    BRK(description = "Force an interrupt"),
    NOP(description = "No Operation"),
    RTI(description = "Return from Interrupt"),
    ;

    companion object {
        fun parse(text: String): AssemblyOp = AssemblyOp.valueOf(text.uppercase())
    }
}

data class AssemblyLine(
    val label: String? = null,
    val instruction: AssemblyInstruction? = null,
    val comment: String? = null,
    val originalLine: String? = null,
) {
    override fun toString(): String = buildString {
        if (label != null) {
            append(label)
            append(": ")
        }
        if (instruction != null) {
            append(instruction)
        }
        if (comment != null) {
            while (length < 32) append(' ')
            append("; ")
            append(comment)
        }
    }
}

data class AssemblyInstruction(
    val op: AssemblyOp,
    val address: AssemblyAddressing? = null,
) {
    val addressAsLabel get() = address as AssemblyAddressing.Label
    override fun toString(): String = "$op ${address ?: ""}"
}

sealed class AssemblyAddressing {
    data class ValueHex(val value: Byte) : AssemblyAddressing() {
        override fun toString(): String = "#$" + value.toString(16).padStart(2, '0')
    }

    data class ValueBinary(val value: Byte) : AssemblyAddressing() {
        override fun toString(): String = "#%" + value.toString(2).padStart(8, '0')
    }

    data class ValueDecimal(val value: Byte) : AssemblyAddressing() {
        override fun toString(): String = "#$value"
    }

    data class ValueReference(val name: String) : AssemblyAddressing() {
        override fun toString(): String = "#$name"
    }

    data class Label(val label: String) : AssemblyAddressing() {
        override fun toString(): String = label
    }

    data class DirectX(val label: String) : AssemblyAddressing() {
        override fun toString(): String = "$label,X"
    }

    data class DirectY(val label: String) : AssemblyAddressing() {
        override fun toString(): String = "$label,Y"
    }

    data class IndirectX(val label: String) : AssemblyAddressing() {
        override fun toString(): String = "($label,X)"
    }

    data class IndirectY(val label: String) : AssemblyAddressing() {
        override fun toString(): String = "($label),Y"
    }

    companion object {
        fun parse(text: String): AssemblyAddressing {
            if (text.startsWith("#$")) {
                return ValueHex(text.substring(2).toInt(16).toByte())
            }
            if (text.startsWith("#%")) {
                return ValueBinary(text.substring(2).toInt(2).toByte())
            }
            if (text.startsWith("#")) {
                if (text[1].isLetter()) {
                    return ValueReference(text.drop(1))
                }
                return ValueDecimal(text.substring(2).toInt(2).toByte())
            }
            if (text.filter { !it.isWhitespace() }.endsWith(",X)", ignoreCase = true)) {
                return IndirectX(text.substringAfter('(').substringBefore(','))
            }
            if (text.filter { !it.isWhitespace() }.endsWith("),Y", ignoreCase = true)) {
                return IndirectY(text.substringAfter('(').substringBefore(')'))
            }
            if (text.filter { !it.isWhitespace() }.endsWith(",X", ignoreCase = true)) {
                return DirectX(text.substringBefore(',').trim())
            }
            if (text.filter { !it.isWhitespace() }.endsWith(",Y", ignoreCase = true)) {
                return DirectY(text.substringBefore(',').trim())
            }
            return Label(text.trim())
        }
    }
}


sealed class AssemblyGrouping {
    class Direct(val lines: List<AssemblyLine>) : AssemblyGrouping()
    class If(
        val branchInstruction: AssemblyLine,
        val then: List<AssemblyGrouping>,
    ) : AssemblyGrouping()

    class IfElse(
        val branchInstruction: AssemblyLine,
        val then: List<AssemblyGrouping>,
        val otherwise: List<AssemblyGrouping>,
    ) : AssemblyGrouping()

    class DoWhile(
        val contents: List<AssemblyGrouping>,
        val branchInstruction: AssemblyLine,
    ) : AssemblyGrouping()

    class EarlyExit(
        val branchInstruction: AssemblyLine,
    ) : AssemblyGrouping()

    /**
     * Conditionally calling an external subroutine as a returning call.
     */
    class TailCall(
        val branchInstruction: AssemblyLine,
    ) : AssemblyGrouping()
}

fun List<AssemblyLine>.groupify(): List<AssemblyGrouping> {
    // Heuristic grouping for basic control structures: If (forward branch over a block)
    // and Do-While (backward branch to a label above). Falls back to Direct for all else.
    if (this.isEmpty()) return emptyList()

    val lines = this
    val labelIndex = mutableMapOf<String, Int>()
    for (idx in lines.indices) {
        val lab = lines[idx].label
        if (lab != null) labelIndex[lab] = idx
    }

    fun isBranch(op: AssemblyOp): Boolean = when (op) {
        // 6502 conditional branches
        AssemblyOp.BPL,
        AssemblyOp.BMI,
        AssemblyOp.BEQ,
        AssemblyOp.BNE,
        AssemblyOp.BCC,
        AssemblyOp.BCS,
        AssemblyOp.BVS,
        AssemblyOp.BVC -> true

        else -> false
    }

    val result = mutableListOf<AssemblyGrouping>()

    fun addDirect(line: AssemblyLine) {
        val last = result.lastOrNull()
        if (last is AssemblyGrouping.Direct) {
            result[result.lastIndex] = AssemblyGrouping.Direct(last.lines + line)
        } else {
            result += AssemblyGrouping.Direct(listOf(line))
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val instr = line.instruction
        if (instr != null && isBranch(instr.op) && instr.address is AssemblyAddressing.Label) {
            val target = instr.address.label
            val targetIdx = labelIndex[target]
            if (targetIdx != null) {
                if (targetIdx > i) {
                    // Forward branch: treat as If. The then-block is the lines between branch and target label/instruction.
                    val thenSlice = if (i + 1 < targetIdx) lines.subList(i + 1, targetIdx) else emptyList()
                    val thenGroups = thenSlice.groupify()
                    result += AssemblyGrouping.If(branchInstruction = line, then = thenGroups)
                    i = targetIdx
                    continue
                } else if (targetIdx < i) {
                    // Backward branch: treat as Do-While.
                    val start = targetIdx + 1
                    val end = i // exclusive of branch line
                    val bodySlice = if (start < end) lines.subList(start, end) else emptyList()
                    val bodyGroups = bodySlice.groupify()
                    result += AssemblyGrouping.DoWhile(contents = bodyGroups, branchInstruction = line)
                    i++
                    continue
                }
            }
        }
        // Default: keep as direct line
        addDirect(line)
        i++
    }

    return result
}

data class TranslatedAssemblyLine(
    val assembly: AssemblyLine? = null,
    val translation: List<KotlinPart>? = null
) {
    init {
        translation?.forEach {
            (it as? Calculation)?.addUse(this)
        }
    }
    class Emitter(val to: Appendable) {
        fun emit(line: TranslatedAssemblyLine) {
            if (line.assembly != null) to.appendLine("//> ${line.assembly.originalLine?.trim()}")
            line.translation?.let { emitLine(it) }
        }
        val names = HashMap<KotlinPart.Calculation, String>()
        var nameIndex = 0
        fun emitLine(translation: List<KotlinPart>) {
            fun ensureReady(part: KotlinPart) {
                if(part is KotlinPart.Calculation) {
                    part.parts.forEach { ensureReady(it) }
                    if(part.useCount > 1 && part !in names) {
                        val name = "temp${nameIndex++}"
                        names[part] = name
                        emitLine(listOf(KotlinPart.PlainText("val $name = ")) + part.parts)
                    }
                }
            }
            translation.forEach { ensureReady(it) }
            fun print(it: KotlinPart): Unit {
                when (it) {
                    is KotlinPart.PlainText -> to.append(it.text)
                    is KotlinPart.Calculation -> if(it.useCount == 1) it.parts.forEach { print(it) }
                    else to.append(names[it]!!)
                }
            }
            translation.forEach { print(it) }
            to.appendLine()
        }
    }
}

sealed class KotlinPart {
    data class PlainText(val text: String) : KotlinPart() {
        override fun toString(): String = text
    }
    class Calculation(
        val parts: List<KotlinPart>
    ) : KotlinPart() {
        var useCount = 0

        fun addUse(from: Any?) {
            useCount++
            parts.forEach {
                (it as? Calculation)?.addUse(this)
            }
        }

        override fun toString(): String = parts.joinToString("") { it.toString() }
    }
}

sealed class KotlinFlagTranslation {
    data class Literal(val boolean: Boolean) : KotlinFlagTranslation()
    data class Direct(
        val part: KotlinPart,
        val reverse: KotlinPart = KotlinPart.Calculation(listOf(KotlinPart.PlainText("!")) + listOf(part)),
    ) : KotlinFlagTranslation()

    data class FromValue(val part: KotlinPart) : KotlinFlagTranslation()
}

class AutoTranslate {
    var comparison: Pair<String, String>? = null
    var previousLine: AssemblyLine? = null

    var result: () -> KotlinPart = { KotlinPart.PlainText("Unit") }

    private class UseBeforeSetRecorder<T>(val gen: ()->T): ReadWriteProperty<Any?, T> {
        var underlying: T? = null
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return underlying ?: run {
                val v = gen()
                underlying = v
                v
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            underlying = value
        }
    }

    val inputs = ArrayList<String>()

    var a: KotlinPart by UseBeforeSetRecorder {
        inputs += "inputA: Byte"
        KotlinPart.PlainText("inputA")
    }
    var x: KotlinPart by UseBeforeSetRecorder {
        inputs += "inputX: Byte"
        KotlinPart.PlainText("inputX")
    }
    var y: KotlinPart by UseBeforeSetRecorder {
        inputs += "inputY: Byte"
        KotlinPart.PlainText("inputY")
    }

    var n: KotlinFlagTranslation by UseBeforeSetRecorder {
        inputs += "inputN: Boolean"
        KotlinFlagTranslation.Direct(KotlinPart.PlainText("inputN"))
    }
    var c: KotlinFlagTranslation by UseBeforeSetRecorder {
        inputs += "inputC: Boolean"
        KotlinFlagTranslation.Direct(KotlinPart.PlainText("inputC"))
    }
    var z: KotlinFlagTranslation by UseBeforeSetRecorder {
        inputs += "inputZ: Boolean"
        KotlinFlagTranslation.Direct(KotlinPart.PlainText("inputZ"))
    }
    var v: KotlinFlagTranslation by UseBeforeSetRecorder {
        inputs += "inputV: Boolean"
        KotlinFlagTranslation.Direct(KotlinPart.PlainText("inputV"))
    }


    fun get(flag: AssemblyFlag) = when (flag) {
        AssemblyFlag.N -> n
        AssemblyFlag.C -> c
        AssemblyFlag.Z -> z
        AssemblyFlag.V -> v
    }

    val output = ArrayList<TranslatedAssemblyLine>()

    fun AssemblyAddressing.toKotlin(): KotlinPart = when (this) {
        is AssemblyAddressing.DirectX -> KotlinPart.Calculation(listOf(KotlinPart.PlainText("ram.${label.decapitalize()}["), x, KotlinPart.PlainText("]")))
        is AssemblyAddressing.DirectY -> KotlinPart.Calculation(listOf(KotlinPart.PlainText("ram.${label.decapitalize()}["), y, KotlinPart.PlainText("]")))
        is AssemblyAddressing.IndirectX -> KotlinPart.Calculation(listOf(KotlinPart.PlainText("ram.${label.decapitalize()}["), x, KotlinPart.PlainText("]")))
        is AssemblyAddressing.IndirectY -> KotlinPart.Calculation(listOf(KotlinPart.PlainText("ram.${label.decapitalize()}["), y, KotlinPart.PlainText("]")))
        is AssemblyAddressing.Label -> KotlinPart.PlainText("ram.${label.decapitalize()}")
        is AssemblyAddressing.ValueBinary -> KotlinPart.PlainText("0b${value.toString(2).padStart(8, '0')}")
        is AssemblyAddressing.ValueHex -> KotlinPart.PlainText("0x${value.toString(16).padStart(2, '0')}")
        is AssemblyAddressing.ValueDecimal -> KotlinPart.PlainText("$value")
        is AssemblyAddressing.ValueReference -> KotlinPart.PlainText("Constants.$name")
        else -> TODO()
    }

    private fun pt(text: String) = listOf(KotlinPart.PlainText(text))
    fun translate(grouping: AssemblyGrouping): Unit = when (grouping) {
        is AssemblyGrouping.Direct -> {
            for (line in grouping.lines) {
                if (line.label == null && line.comment == null && line.instruction == null) {
                    continue
                }
                output += TranslatedAssemblyLine(
                    assembly = line,
                    translation = line.instruction?.let { instruction ->
                        when (instruction.op) {
                            AssemblyOp.JSR -> pt((instruction.address as AssemblyAddressing.Label).label.decapitalize() + "()")
                            AssemblyOp.JMP -> pt("return ${(instruction.address as AssemblyAddressing.Label).label.decapitalize()}()")
                            AssemblyOp.CLC -> {
                                c = KotlinFlagTranslation.Literal(false)
                                null
                            }

                            AssemblyOp.SEC -> {
                                c = KotlinFlagTranslation.Literal(true)
                                null
                            }

                            AssemblyOp.TYA -> {
                                a = y
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.TAY -> {
                                y = a
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.TXA -> {
                                a = x
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.TAX -> {
                                x = a
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.LDA -> {
                                a = instruction.address!!.toKotlin()
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.LDX -> {
                                x = instruction.address!!.toKotlin()
                                n = KotlinFlagTranslation.FromValue(x)
                                z = KotlinFlagTranslation.FromValue(x)
                                null
                            }

                            AssemblyOp.LDY -> {
                                y = instruction.address!!.toKotlin()
                                n = KotlinFlagTranslation.FromValue(y)
                                z = KotlinFlagTranslation.FromValue(y)
                                null
                            }

                            AssemblyOp.STA -> listOf(instruction.address!!.toKotlin(), KotlinPart.PlainText(" = "), a)
                            AssemblyOp.STX -> listOf(instruction.address!!.toKotlin(), KotlinPart.PlainText(" = "), x)
                            AssemblyOp.STY -> listOf(instruction.address!!.toKotlin(), KotlinPart.PlainText(" = "), y)

                            AssemblyOp.AND -> {
                                a = KotlinPart.Calculation(listOf(a) + pt(" and ") + instruction.address!!.toKotlin())
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.EOR -> {
                                a = KotlinPart.Calculation(listOf(a) + pt(" xor ") + instruction.address!!.toKotlin())
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.ORA -> {
                                a = KotlinPart.Calculation(listOf(a) + pt(" or ") + instruction.address!!.toKotlin())
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.ADC -> {
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(a) + pt(" byteAddResultsInOverflow ") + instruction.address!!.toKotlin())
                                )
                                a =
                                    KotlinPart.Calculation(listOf(a) + pt(" byteAdd ") + instruction.address!!.toKotlin())
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.SBC -> {
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(a) + pt(" byteMinusResultsInOverflow ") + instruction.address!!.toKotlin())
                                )
                                a =
                                    KotlinPart.Calculation(listOf(a) + pt(" byteMinus ") + instruction.address!!.toKotlin())
                                n = KotlinFlagTranslation.FromValue(a)
                                z = KotlinFlagTranslation.FromValue(a)
                                null
                            }

                            AssemblyOp.CMP -> {
                                n = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(a) + pt(" - ") + instruction.address!!.toKotlin() + pt(" < 0")),
                                    reverse = KotlinPart.Calculation(listOf(a) + pt(" - ") + instruction.address!!.toKotlin() + pt(" >= 0")),
                                )
                                z = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(a) + pt(" == ") + instruction.address!!.toKotlin()),
                                    reverse = KotlinPart.Calculation(listOf(a) + pt(" != ") + instruction.address!!.toKotlin()),
                                )
                                c = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(a) + pt(" >= ") + instruction.address!!.toKotlin()),
                                    reverse = KotlinPart.Calculation(listOf(a) + pt(" < ") + instruction.address!!.toKotlin()),
                                )
                                null
                            }

                            AssemblyOp.CPX -> {
                                n = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(x) + pt(" - ") + instruction.address!!.toKotlin() + pt(" < 0")),
                                    reverse = KotlinPart.Calculation(listOf(x) + pt(" - ") + instruction.address!!.toKotlin() + pt(" >= 0")),
                                )
                                z = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(x) + pt(" == ") + instruction.address!!.toKotlin()),
                                    reverse = KotlinPart.Calculation(listOf(x) + pt(" != ") + instruction.address!!.toKotlin()),
                                )
                                c = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(x) + pt(" >= ") + instruction.address!!.toKotlin()),
                                    reverse = KotlinPart.Calculation(listOf(x) + pt(" < ") + instruction.address!!.toKotlin()),
                                )
                                null
                            }

                            AssemblyOp.CPY -> {
                                n = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(y) + pt(" - ") + instruction.address!!.toKotlin() + pt(" < 0")),
                                    reverse = KotlinPart.Calculation(listOf(y) + pt(" - ") + instruction.address!!.toKotlin() + pt(" >= 0")),
                                )
                                z = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(y) + pt(" == " + instruction.address!!.toKotlin())),
                                    reverse = KotlinPart.Calculation(listOf(y) + pt(" != " + instruction.address!!.toKotlin())),
                                )
                                c = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(listOf(y) + pt(" >= " + instruction.address!!.toKotlin())),
                                    reverse = KotlinPart.Calculation(listOf(y) + pt(" < " + instruction.address!!.toKotlin())),
                                )
                                null
                            }

                            AssemblyOp.RTS -> pt("return ") + result()

                            AssemblyOp.INC -> {
                                val out = KotlinPart.Calculation(listOf(KotlinPart.PlainText("++"), instruction.address!!.toKotlin()))
                                n = KotlinFlagTranslation.FromValue(out)
                                z = KotlinFlagTranslation.FromValue(out)
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(
                                        listOf(
                                            out,
                                            KotlinPart.PlainText(" == Byte.MIN_VALUE")
                                        )
                                    ),
                                )
                                listOf(out)
                            }

                            AssemblyOp.DEC -> {
                                val out = KotlinPart.Calculation(listOf(KotlinPart.PlainText("--"), instruction.address!!.toKotlin()))
                                n = KotlinFlagTranslation.FromValue(out)
                                z = KotlinFlagTranslation.FromValue(out)
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(
                                        listOf(
                                            out,
                                            KotlinPart.PlainText(" == Byte.MAX_VALUE")
                                        )
                                    ),
                                )
                                listOf(out)
                            }

                            AssemblyOp.INX -> {
                                x = KotlinPart.Calculation(listOf(x) + pt(".inc()"))
                                n = KotlinFlagTranslation.FromValue(x)
                                z = KotlinFlagTranslation.FromValue(x)
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(
                                        listOf(
                                            x,
                                            KotlinPart.PlainText(" == Byte.MIN_VALUE")
                                        )
                                    ),
                                )
                                null
                            }

                            AssemblyOp.INY -> {
                                y = KotlinPart.Calculation(listOf(y) + pt(".inc()"))
                                n = KotlinFlagTranslation.FromValue(y)
                                z = KotlinFlagTranslation.FromValue(y)
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(
                                        listOf(
                                            y,
                                            KotlinPart.PlainText(" == Byte.MIN_VALUE")
                                        )
                                    ),
                                )
                                null
                            }

                            AssemblyOp.DEX -> {
                                x = KotlinPart.Calculation(listOf(x) + pt(".dec()"))
                                n = KotlinFlagTranslation.FromValue(x)
                                z = KotlinFlagTranslation.FromValue(x)
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(
                                        listOf(
                                            x,
                                            KotlinPart.PlainText(" == Byte.MAX_VALUE")
                                        )
                                    ),
                                )
                                null
                            }

                            AssemblyOp.DEY -> {
                                y = KotlinPart.Calculation(listOf(y) + pt(".dec()"))
                                n = KotlinFlagTranslation.FromValue(y)
                                z = KotlinFlagTranslation.FromValue(y)
                                v = KotlinFlagTranslation.Direct(
                                    part = KotlinPart.Calculation(
                                        listOf(
                                            y,
                                            KotlinPart.PlainText(" == Byte.MAX_VALUE")
                                        )
                                    ),
                                )
                                null
                            }

                            else -> pt("// TODO: $instruction")
                        }
                    }
                )
                previousLine = line
            }
        }

        is AssemblyGrouping.If -> {
            val branch = grouping.branchInstruction
            output += TranslatedAssemblyLine(
                assembly = branch,
                translation = listOf(
                    KotlinPart.PlainText("if ("),
                    condition(branch.instruction!!, invert = true),
                    KotlinPart.PlainText(") {"),
                )
            )
            grouping.then.forEach(::translate)
            output += TranslatedAssemblyLine(
                translation = listOf(
                    KotlinPart.PlainText("}"),
                )
            )
        }

        is AssemblyGrouping.IfElse -> {
            val branch = grouping.branchInstruction
            output += TranslatedAssemblyLine(
                assembly = branch,
                translation = listOf(
                    KotlinPart.PlainText("if ("),
                    condition(branch.instruction!!, invert = true),
                    KotlinPart.PlainText(") {"),
                )
            )
            grouping.then.forEach(::translate)
            output += TranslatedAssemblyLine(
                translation = listOf(
                    KotlinPart.PlainText("} else {"),
                )
            )
            grouping.otherwise.forEach(::translate)
            output += TranslatedAssemblyLine(
                translation = listOf(
                    KotlinPart.PlainText("}"),
                )
            )
        }

        is AssemblyGrouping.DoWhile -> {
            val branch = grouping.branchInstruction
            output += TranslatedAssemblyLine(
                translation = listOf(
                    KotlinPart.PlainText("do {"),
                )
            )
            grouping.contents.forEach(::translate)
            output += TranslatedAssemblyLine(
                assembly = branch,
                translation = listOf(
                    KotlinPart.PlainText("} while ("),
                    condition(branch.instruction!!, invert = false),
                    KotlinPart.PlainText(")"),
                )
            )
        }

        is AssemblyGrouping.EarlyExit -> {
            val branch = grouping.branchInstruction
            output += TranslatedAssemblyLine(
                assembly = branch,
                translation = listOf(
                    KotlinPart.PlainText("if ("),
                    condition(branch.instruction!!, invert = false),
                    KotlinPart.PlainText(") return"),
                )
            )
        }

        is AssemblyGrouping.TailCall -> {
            val branch = grouping.branchInstruction
            output += TranslatedAssemblyLine(
                assembly = branch,
                translation = listOf(
                    KotlinPart.PlainText("if ("),
                    condition(branch.instruction!!, invert = false),
                    KotlinPart.PlainText(") return ${branch.instruction!!.addressAsLabel.label}()"),
                )
            )
        }
    }

    fun condition(branch: AssemblyInstruction, invert: Boolean): KotlinPart {
        val op = branch.op
        val flagState = get(op.consumedFlag!!)
        val flagPositive = op.flagPositive xor invert
        return when (flagState) {
            is KotlinFlagTranslation.Direct -> if (flagPositive)
                flagState.part
            else
                flagState.reverse

            is KotlinFlagTranslation.FromValue -> when (op.consumedFlag) {
                AssemblyFlag.N -> if (flagPositive) {
                    KotlinPart.Calculation(listOf(flagState.part) + pt(" < 0.toByte()"))
                } else {
                    KotlinPart.Calculation(listOf(flagState.part) + pt(" >= 0.toByte()"))
                }

                AssemblyFlag.Z -> if (flagPositive) {
                    KotlinPart.Calculation(listOf(flagState.part) + pt(" == 0.toByte()"))
                } else {
                    KotlinPart.Calculation(listOf(flagState.part) + pt(" != 0.toByte()"))
                }

                else -> throw IllegalStateException()
            }

            is KotlinFlagTranslation.Literal -> KotlinPart.PlainText(flagState.boolean.xor(flagPositive).toString())
        }
    }
}

fun String.parseAssemblyLines(): List<AssemblyLine> {
    return this.split('\n')
        .map {
            AssemblyLine(
                label = it.substringBefore(':', "").trim().takeIf { it.isNotBlank() },
                instruction = it.substringAfter(":").substringBefore(";").trim().takeIf { it.isNotBlank() }?.let {
                    AssemblyInstruction(
                        op = AssemblyOp.parse(it.substringBefore(' ').trim()),
                        address = it.substringAfter(' ', "").trim().takeUnless { it.isBlank() }
                            ?.let { AssemblyAddressing.parse(it) }
                    )
                },
                comment = it.substringAfter(';', "").trim().takeIf { it.isNotBlank() },
                originalLine = it
            )
        }
}

fun main(vararg args: String) {
    testContent.parseAssemblyLines()
        .let {
            val grouped = it.groupify()
            val translator = AutoTranslate()
//            translator.result = { translator.y }
            for(item in grouped) translator.translate(item)
            val output = translator.output
            val builder = StringBuilder()
            builder.appendLine("fun System.${it.firstNotNullOfOrNull { it.label }?.decapitalize()}(${translator.inputs.joinToString(", ")}): Unit {")
            val em = TranslatedAssemblyLine.Emitter(builder)
            output.forEach(em::emit)
            builder.appendLine("}")
            println(builder)
        }
}

val testContent = """
    
VerticalPipe:
          jsr GetPipeHeight
          lda $00                  ;check to see if value was nullified earlier
          beq WarpPipe             ;(if d3, the usage control bit of second byte, was set)
          iny
          iny
          iny
          iny                      ;add four if usage control bit was not set
WarpPipe: tya                      ;save value in stack
          pha
          lda AreaNumber
          ora WorldNumber          ;if at world 1-1, do not add piranha plant ever
          beq DrawPipe
          ldy AreaObjectLength,x   ;if on second column of pipe, branch
          beq DrawPipe             ;(because we only need to do this once)
          jsr FindEmptyEnemySlot   ;check for an empty moving data buffer space
          bcs DrawPipe             ;if not found, too many enemies, thus skip
          jsr GetAreaObjXPosition  ;get horizontal pixel coordinate
          clc
          adc #$08                 ;add eight to put the piranha plant in the center
          sta Enemy_X_Position,x   ;store as enemy's horizontal coordinate
          lda CurrentPageLoc       ;add carry to current page number
          adc #$00
          sta Enemy_PageLoc,x      ;store as enemy's page coordinate
          lda #$01
          sta Enemy_Y_HighPos,x
          sta Enemy_Flag,x         ;activate enemy flag
          jsr GetAreaObjYPosition  ;get piranha plant's vertical coordinate and store here
          sta Enemy_Y_Position,x
          lda #PiranhaPlant        ;write piranha plant's value into buffer
          sta Enemy_ID,x
          jsr InitPiranhaPlant
DrawPipe: pla                      ;get value saved earlier and use as Y
          tay
          ldx $07                  ;get buffer offset
          lda VerticalPipeData,y   ;draw the appropriate pipe with the Y we loaded earlier
          sta MetatileBuffer,x     ;render the top of the pipe
          inx
          lda VerticalPipeData+2,y ;render the rest of the pipe
          ldy $06                  ;subtract one from length and render the part underneath
          dey
          jmp RenderUnderPart
""".trimIndent()
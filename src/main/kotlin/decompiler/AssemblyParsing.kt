package com.ivieleague.smbtranslation.decompiler

import com.ivieleague.smbtranslation.utils.AssemblyInstruction
import com.ivieleague.smbtranslation.utils.AssemblyLine


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

fun String.parseAssemblyLines(): List<AssemblyLine> {
    return this.split('\n')
        .map {
            AssemblyLine(
                label = it.substringBefore(':', "").trim().takeIf { it.isNotBlank() },
                instruction = it.substringAfter(":").substringBefore(";").trim().takeIf { it.isNotBlank() }?.let {
                    AssemblyInstruction(
                        op = com.ivieleague.smbtranslation.utils.AssemblyOp.parse(it.substringBefore(' ').trim()),
                        address = it.substringAfter(' ', "").trim().takeUnless { it.isBlank() }
                            ?.let { com.ivieleague.smbtranslation.utils.AssemblyAddressing.parse(it) }
                    )
                },
                comment = it.substringAfter(';', "").trim().takeIf { it.isNotBlank() },
                originalLine = it
            )
        }
}
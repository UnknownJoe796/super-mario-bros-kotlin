// Ported from decompiler-6502-kotlin by Claude
package com.ivieleague.smbtranslation.interpreter

/**
 * Binary 6502 interpreter that executes raw machine code from memory.
 *
 * Unlike Interpreter6502 which works with parsed AssemblyInstruction objects,
 * this interpreter fetches, decodes, and executes raw opcodes directly from memory.
 * This is needed for running actual NES ROMs with TAS input.
 */
class BinaryInterpreter6502(
    val cpu: CPU6502 = CPU6502(),
    val memory: Memory6502 = Memory6502()
) {
    var halted: Boolean = false
    var nmiPending: Boolean = false
    var irqPending: Boolean = false
    var inNmiHandler: Boolean = false  // Track if we're inside NMI handler to prevent re-entry

    // Frame debt system: certain subroutines take multiple frames worth of cycles
    // When called, they set frame debt which causes subsequent NMIs to be skipped
    var frameDebt: Int = 0  // Number of frames to skip NMI
    var frameDebtMap: Map<Int, Int> = emptyMap()  // subroutine address -> frames of debt

    /**
     * Check if NMI should be skipped due to frame debt from a heavy subroutine.
     * Call this before triggering NMI each frame.
     * @return true if NMI should be skipped this frame
     */
    fun shouldSkipNmiDueToDebt(): Boolean {
        if (frameDebt > 0) {
            frameDebt--
            return true
        }
        return false
    }

    // Hooks for PPU/APU/Controller interception
    var memoryReadHook: ((Int) -> UByte?)? = null
    var memoryWriteHook: ((Int, UByte) -> Boolean)? = null

    // Hooks for subroutine tracing
    var jsrHook: ((targetAddress: Int, callerAddress: Int) -> Unit)? = null
    var rtsHook: (() -> Unit)? = null
    var rtiHook: (() -> Unit)? = null
    var nmiHook: (() -> Unit)? = null  // Called when NMI is about to be serviced

    /**
     * Read a byte from memory, using hook if available
     */
    private fun read(addr: Int): UByte {
        return memoryReadHook?.invoke(addr and 0xFFFF) ?: memory.readByte(addr and 0xFFFF)
    }

    /**
     * Write a byte to memory, using hook if available
     */
    private fun write(addr: Int, value: UByte) {
        if (memoryWriteHook?.invoke(addr and 0xFFFF, value) != true) {
            memory.writeByte(addr and 0xFFFF, value)
        }
    }

    /**
     * Read a 16-bit word from memory (little-endian)
     */
    private fun readWord(addr: Int): Int {
        val lo = read(addr).toInt()
        val hi = read(addr + 1).toInt()
        return lo or (hi shl 8)
    }

    // Track which addresses we've already set debt for (to avoid re-triggering)
    private val debtTriggeredAddresses = mutableSetOf<Int>()

    /**
     * Clear the debt triggered set. Call this at the start of each NMI to allow
     * re-triggering if the same routine is entered again in a new frame.
     */
    fun clearDebtTriggered() {
        debtTriggeredAddresses.clear()
    }

    /**
     * Execute one instruction and return the number of cycles used.
     * Also checks for pending interrupts at the end (proper 6502 behavior).
     */
    fun step(): Int {
        if (halted) return 0

        val pc = cpu.PC.toInt()

        // Check if we're entering a multi-frame routine (regardless of how we got here)
        // Only trigger once per NMI to avoid stacking debt from loops
        if (pc !in debtTriggeredAddresses) {
            frameDebtMap[pc]?.let { framesConsumed ->
                val skipCount = framesConsumed - 1
                if (skipCount > 0) {
                    val oldDebt = frameDebt
                    frameDebt = maxOf(frameDebt, skipCount)
                    debtTriggeredAddresses.add(pc)
                    if (frameDebt > oldDebt) {
                        println("  [DEBT] PC=0x${pc.toString(16)} triggered $framesConsumed frames consumed, debt now $frameDebt")
                    }
                }
            }
        }

        val opcode = read(pc).toInt()
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()

        val cycles = executeOpcode(opcode)

        // Check for pending interrupts at end of instruction (6502 behavior)
        // NMI is edge-triggered so only service once per trigger
        // Don't allow nested NMI (real NES PPU clears NMI line between frames)
        if (nmiPending && !inNmiHandler) {
            nmiPending = false
            inNmiHandler = true
            nmiHook?.invoke()  // Notify tracer of NMI entry
            pushWord(cpu.PC.toInt())
            // For NMI/IRQ: B flag (bit 4) is CLEAR, bit 5 is always SET when pushed
            pushByte((cpu.getStatusByte().toInt() and 0xEF) or 0x20)
            cpu.I = true
            cpu.PC = readWord(0xFFFA).toUShort()
        } else if (irqPending && !cpu.I) {
            irqPending = false
            pushWord(cpu.PC.toInt())
            // For NMI/IRQ: B flag (bit 4) is CLEAR, bit 5 is always SET when pushed
            pushByte((cpu.getStatusByte().toInt() and 0xEF) or 0x20)
            cpu.I = true
            cpu.PC = readWord(0xFFFE).toUShort()
        }

        return cycles
    }

    /**
     * Execute until halted or maxCycles reached
     */
    fun run(maxCycles: Long = Long.MAX_VALUE): Long {
        var totalCycles = 0L
        while (!halted && totalCycles < maxCycles) {
            totalCycles += step()
        }
        return totalCycles
    }

    /**
     * Trigger NMI interrupt
     */
    fun triggerNmi() {
        nmiPending = true
    }

    /**
     * Handle pending interrupts (call this at appropriate times)
     * This is the external entry point for triggering NMI at frame boundaries.
     * Unlike step() which prevents re-entry during the same NMI handler,
     * this always services NMI because each frame should start a new NMI.
     */
    fun handleInterrupts() {
        if (nmiPending) {
            nmiPending = false
            // Reset inNmiHandler since this is a new frame's NMI
            inNmiHandler = true
            nmiHook?.invoke()  // Notify tracer of NMI entry
            pushWord(cpu.PC.toInt())
            // For NMI/IRQ: B flag (bit 4) is CLEAR, bit 5 is always SET when pushed
            pushByte((cpu.getStatusByte().toInt() and 0xEF) or 0x20)
            cpu.I = true
            cpu.PC = readWord(0xFFFA).toUShort()
        } else if (irqPending && !cpu.I) {
            irqPending = false
            pushWord(cpu.PC.toInt())
            // For NMI/IRQ: B flag (bit 4) is CLEAR, bit 5 is always SET when pushed
            pushByte((cpu.getStatusByte().toInt() and 0xEF) or 0x20)
            cpu.I = true
            cpu.PC = readWord(0xFFFE).toUShort()
        }
    }

    /**
     * Reset the CPU and memory - initialize RAM and jump to reset vector
     */
    fun reset() {
        cpu.reset()
        memory.reset()  // Initialize RAM to match real NES power-on state
        cpu.PC = readWord(0xFFFC).toUShort()
        halted = false
        nmiPending = false
        irqPending = false
        inNmiHandler = false
    }

    // =====================================================================
    // Opcode execution
    // =====================================================================

    private fun executeOpcode(opcode: Int): Int {
        return when (opcode) {
            // BRK
            0x00 -> { brk(); 7 }

            // ORA
            0x01 -> { ora(indirectX()); 6 }
            0x05 -> { ora(zeroPage()); 3 }
            0x09 -> { ora(immediate()); 2 }
            0x0D -> { ora(absolute()); 4 }
            0x11 -> { ora(indirectY()); 5 }
            0x15 -> { ora(zeroPageX()); 4 }
            0x19 -> { ora(absoluteY()); 4 }
            0x1D -> { ora(absoluteX()); 4 }

            // ASL
            0x06 -> { aslMem(zeroPageAddr()); 5 }
            0x0A -> { aslAcc(); 2 }
            0x0E -> { aslMem(absoluteAddr()); 6 }
            0x16 -> { aslMem(zeroPageXAddr()); 6 }
            0x1E -> { aslMem(absoluteXAddr()); 7 }

            // PHP, PLP, PHA, PLA
            0x08 -> { php(); 3 }
            0x28 -> { plp(); 4 }
            0x48 -> { pha(); 3 }
            0x68 -> { pla(); 4 }

            // Branches
            0x10 -> { branch(!cpu.N); 2 }  // BPL
            0x30 -> { branch(cpu.N); 2 }   // BMI
            0x50 -> { branch(!cpu.V); 2 }  // BVC
            0x70 -> { branch(cpu.V); 2 }   // BVS
            0x90 -> { branch(!cpu.C); 2 }  // BCC
            0xB0 -> { branch(cpu.C); 2 }   // BCS
            0xD0 -> { branch(!cpu.Z); 2 }  // BNE
            0xF0 -> { branch(cpu.Z); 2 }   // BEQ

            // CLC, SEC, CLI, SEI, CLV, CLD, SED
            0x18 -> { cpu.C = false; 2 }
            0x38 -> { cpu.C = true; 2 }
            0x58 -> { cpu.I = false; 2 }
            0x78 -> { cpu.I = true; 2 }
            0xB8 -> { cpu.V = false; 2 }
            0xD8 -> { cpu.D = false; 2 }
            0xF8 -> { cpu.D = true; 2 }

            // JSR
            0x20 -> { jsr(); 6 }

            // AND
            0x21 -> { and(indirectX()); 6 }
            0x25 -> { and(zeroPage()); 3 }
            0x29 -> { and(immediate()); 2 }
            0x2D -> { and(absolute()); 4 }
            0x31 -> { and(indirectY()); 5 }
            0x35 -> { and(zeroPageX()); 4 }
            0x39 -> { and(absoluteY()); 4 }
            0x3D -> { and(absoluteX()); 4 }

            // BIT
            0x24 -> { bit(zeroPage()); 3 }
            0x2C -> { bit(absolute()); 4 }

            // ROL
            0x26 -> { rolMem(zeroPageAddr()); 5 }
            0x2A -> { rolAcc(); 2 }
            0x2E -> { rolMem(absoluteAddr()); 6 }
            0x36 -> { rolMem(zeroPageXAddr()); 6 }
            0x3E -> { rolMem(absoluteXAddr()); 7 }

            // RTI
            0x40 -> { rti(); 6 }

            // EOR
            0x41 -> { eor(indirectX()); 6 }
            0x45 -> { eor(zeroPage()); 3 }
            0x49 -> { eor(immediate()); 2 }
            0x4D -> { eor(absolute()); 4 }
            0x51 -> { eor(indirectY()); 5 }
            0x55 -> { eor(zeroPageX()); 4 }
            0x59 -> { eor(absoluteY()); 4 }
            0x5D -> { eor(absoluteX()); 4 }

            // LSR
            0x46 -> { lsrMem(zeroPageAddr()); 5 }
            0x4A -> { lsrAcc(); 2 }
            0x4E -> { lsrMem(absoluteAddr()); 6 }
            0x56 -> { lsrMem(zeroPageXAddr()); 6 }
            0x5E -> { lsrMem(absoluteXAddr()); 7 }

            // JMP
            0x4C -> { jmpAbs(); 3 }
            0x6C -> { jmpInd(); 5 }

            // RTS
            0x60 -> { rts(); 6 }

            // ADC
            0x61 -> { adc(indirectX()); 6 }
            0x65 -> { adc(zeroPage()); 3 }
            0x69 -> { adc(immediate()); 2 }
            0x6D -> { adc(absolute()); 4 }
            0x71 -> { adc(indirectY()); 5 }
            0x75 -> { adc(zeroPageX()); 4 }
            0x79 -> { adc(absoluteY()); 4 }
            0x7D -> { adc(absoluteX()); 4 }

            // ROR
            0x66 -> { rorMem(zeroPageAddr()); 5 }
            0x6A -> { rorAcc(); 2 }
            0x6E -> { rorMem(absoluteAddr()); 6 }
            0x76 -> { rorMem(zeroPageXAddr()); 6 }
            0x7E -> { rorMem(absoluteXAddr()); 7 }

            // STA
            0x81 -> { sta(indirectXAddr()); 6 }
            0x85 -> { sta(zeroPageAddr()); 3 }
            0x8D -> { sta(absoluteAddr()); 4 }
            0x91 -> { sta(indirectYAddr()); 6 }
            0x95 -> { sta(zeroPageXAddr()); 4 }
            0x99 -> { sta(absoluteYAddr()); 5 }
            0x9D -> { sta(absoluteXAddr()); 5 }

            // STX
            0x86 -> { stx(zeroPageAddr()); 3 }
            0x8E -> { stx(absoluteAddr()); 4 }
            0x96 -> { stx(zeroPageYAddr()); 4 }

            // STY
            0x84 -> { sty(zeroPageAddr()); 3 }
            0x8C -> { sty(absoluteAddr()); 4 }
            0x94 -> { sty(zeroPageXAddr()); 4 }

            // Transfers
            0x8A -> { cpu.A = cpu.X; cpu.updateZN(cpu.A); 2 }  // TXA
            0x98 -> { cpu.A = cpu.Y; cpu.updateZN(cpu.A); 2 }  // TYA
            0x9A -> { cpu.SP = cpu.X; 2 }  // TXS
            0xA8 -> { cpu.Y = cpu.A; cpu.updateZN(cpu.Y); 2 }  // TAY
            0xAA -> { cpu.X = cpu.A; cpu.updateZN(cpu.X); 2 }  // TAX
            0xBA -> { cpu.X = cpu.SP; cpu.updateZN(cpu.X); 2 }  // TSX

            // LDA
            0xA1 -> { lda(indirectX()); 6 }
            0xA5 -> { lda(zeroPage()); 3 }
            0xA9 -> { lda(immediate()); 2 }
            0xAD -> { lda(absolute()); 4 }
            0xB1 -> { lda(indirectY()); 5 }
            0xB5 -> { lda(zeroPageX()); 4 }
            0xB9 -> { lda(absoluteY()); 4 }
            0xBD -> { lda(absoluteX()); 4 }

            // LDX
            0xA2 -> { ldx(immediate()); 2 }
            0xA6 -> { ldx(zeroPage()); 3 }
            0xAE -> { ldx(absolute()); 4 }
            0xB6 -> { ldx(zeroPageY()); 4 }
            0xBE -> { ldx(absoluteY()); 4 }

            // LDY
            0xA0 -> { ldy(immediate()); 2 }
            0xA4 -> { ldy(zeroPage()); 3 }
            0xAC -> { ldy(absolute()); 4 }
            0xB4 -> { ldy(zeroPageX()); 4 }
            0xBC -> { ldy(absoluteX()); 4 }

            // CMP
            0xC1 -> { cmp(indirectX()); 6 }
            0xC5 -> { cmp(zeroPage()); 3 }
            0xC9 -> { cmp(immediate()); 2 }
            0xCD -> { cmp(absolute()); 4 }
            0xD1 -> { cmp(indirectY()); 5 }
            0xD5 -> { cmp(zeroPageX()); 4 }
            0xD9 -> { cmp(absoluteY()); 4 }
            0xDD -> { cmp(absoluteX()); 4 }

            // CPX
            0xE0 -> { cpx(immediate()); 2 }
            0xE4 -> { cpx(zeroPage()); 3 }
            0xEC -> { cpx(absolute()); 4 }

            // CPY
            0xC0 -> { cpy(immediate()); 2 }
            0xC4 -> { cpy(zeroPage()); 3 }
            0xCC -> { cpy(absolute()); 4 }

            // DEC
            0xC6 -> { dec(zeroPageAddr()); 5 }
            0xCE -> { dec(absoluteAddr()); 6 }
            0xD6 -> { dec(zeroPageXAddr()); 6 }
            0xDE -> { dec(absoluteXAddr()); 7 }

            // DEX, DEY
            0xCA -> { cpu.X = (cpu.X.toInt() - 1 and 0xFF).toUByte(); cpu.updateZN(cpu.X); 2 }
            0x88 -> { cpu.Y = (cpu.Y.toInt() - 1 and 0xFF).toUByte(); cpu.updateZN(cpu.Y); 2 }

            // INC
            0xE6 -> { inc(zeroPageAddr()); 5 }
            0xEE -> { inc(absoluteAddr()); 6 }
            0xF6 -> { inc(zeroPageXAddr()); 6 }
            0xFE -> { inc(absoluteXAddr()); 7 }

            // INX, INY
            0xE8 -> { cpu.X = (cpu.X.toInt() + 1 and 0xFF).toUByte(); cpu.updateZN(cpu.X); 2 }
            0xC8 -> { cpu.Y = (cpu.Y.toInt() + 1 and 0xFF).toUByte(); cpu.updateZN(cpu.Y); 2 }

            // SBC
            0xE1 -> { sbc(indirectX()); 6 }
            0xE5 -> { sbc(zeroPage()); 3 }
            0xE9 -> { sbc(immediate()); 2 }
            0xED -> { sbc(absolute()); 4 }
            0xF1 -> { sbc(indirectY()); 5 }
            0xF5 -> { sbc(zeroPageX()); 4 }
            0xF9 -> { sbc(absoluteY()); 4 }
            0xFD -> { sbc(absoluteX()); 4 }

            // NOP
            0xEA -> { 2 }

            // Illegal/undefined opcodes - treat as NOP
            else -> { 2 }
        }
    }

    // =====================================================================
    // Addressing modes - return value
    // =====================================================================

    private fun immediate(): UByte {
        val value = read(cpu.PC.toInt())
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return value
    }

    private fun zeroPage(): UByte {
        val addr = read(cpu.PC.toInt()).toInt()
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return read(addr)
    }

    private fun zeroPageX(): UByte {
        val addr = (read(cpu.PC.toInt()).toInt() + cpu.X.toInt()) and 0xFF
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return read(addr)
    }

    private fun zeroPageY(): UByte {
        val addr = (read(cpu.PC.toInt()).toInt() + cpu.Y.toInt()) and 0xFF
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return read(addr)
    }

    private fun absolute(): UByte {
        val addr = readWord(cpu.PC.toInt())
        cpu.PC = (cpu.PC.toInt() + 2).toUShort()
        return read(addr)
    }

    private fun absoluteX(): UByte {
        val addr = (readWord(cpu.PC.toInt()) + cpu.X.toInt()) and 0xFFFF
        cpu.PC = (cpu.PC.toInt() + 2).toUShort()
        return read(addr)
    }

    private fun absoluteY(): UByte {
        val addr = (readWord(cpu.PC.toInt()) + cpu.Y.toInt()) and 0xFFFF
        cpu.PC = (cpu.PC.toInt() + 2).toUShort()
        return read(addr)
    }

    private fun indirectX(): UByte {
        val zpAddr = (read(cpu.PC.toInt()).toInt() + cpu.X.toInt()) and 0xFF
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        val addr = read(zpAddr).toInt() or (read((zpAddr + 1) and 0xFF).toInt() shl 8)
        return read(addr)
    }

    private fun indirectY(): UByte {
        val zpAddr = read(cpu.PC.toInt()).toInt()
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        val baseAddr = read(zpAddr).toInt() or (read((zpAddr + 1) and 0xFF).toInt() shl 8)
        val addr = (baseAddr + cpu.Y.toInt()) and 0xFFFF
        return read(addr)
    }

    // =====================================================================
    // Addressing modes - return address
    // =====================================================================

    private fun zeroPageAddr(): Int {
        val addr = read(cpu.PC.toInt()).toInt()
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return addr
    }

    private fun zeroPageXAddr(): Int {
        val addr = (read(cpu.PC.toInt()).toInt() + cpu.X.toInt()) and 0xFF
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return addr
    }

    private fun zeroPageYAddr(): Int {
        val addr = (read(cpu.PC.toInt()).toInt() + cpu.Y.toInt()) and 0xFF
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return addr
    }

    private fun absoluteAddr(): Int {
        val addr = readWord(cpu.PC.toInt())
        cpu.PC = (cpu.PC.toInt() + 2).toUShort()
        return addr
    }

    private fun absoluteXAddr(): Int {
        val addr = (readWord(cpu.PC.toInt()) + cpu.X.toInt()) and 0xFFFF
        cpu.PC = (cpu.PC.toInt() + 2).toUShort()
        return addr
    }

    private fun absoluteYAddr(): Int {
        val addr = (readWord(cpu.PC.toInt()) + cpu.Y.toInt()) and 0xFFFF
        cpu.PC = (cpu.PC.toInt() + 2).toUShort()
        return addr
    }

    private fun indirectXAddr(): Int {
        val zpAddr = (read(cpu.PC.toInt()).toInt() + cpu.X.toInt()) and 0xFF
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        return read(zpAddr).toInt() or (read((zpAddr + 1) and 0xFF).toInt() shl 8)
    }

    private fun indirectYAddr(): Int {
        val zpAddr = read(cpu.PC.toInt()).toInt()
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        val baseAddr = read(zpAddr).toInt() or (read((zpAddr + 1) and 0xFF).toInt() shl 8)
        return (baseAddr + cpu.Y.toInt()) and 0xFFFF
    }

    // =====================================================================
    // Instruction implementations
    // =====================================================================

    private fun lda(value: UByte) { cpu.A = value; cpu.updateZN(cpu.A) }
    private fun ldx(value: UByte) { cpu.X = value; cpu.updateZN(cpu.X) }
    private fun ldy(value: UByte) { cpu.Y = value; cpu.updateZN(cpu.Y) }

    private fun sta(addr: Int) { write(addr, cpu.A) }
    private fun stx(addr: Int) { write(addr, cpu.X) }
    private fun sty(addr: Int) { write(addr, cpu.Y) }

    private fun ora(value: UByte) {
        cpu.A = (cpu.A.toInt() or value.toInt()).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun and(value: UByte) {
        cpu.A = (cpu.A.toInt() and value.toInt()).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun eor(value: UByte) {
        cpu.A = (cpu.A.toInt() xor value.toInt()).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun adc(value: UByte) {
        val a = cpu.A.toInt()
        val v = value.toInt()
        val c = if (cpu.C) 1 else 0
        val result = a + v + c
        cpu.C = result > 0xFF
        val r = (result and 0xFF).toUByte()
        cpu.V = ((a xor r.toInt()) and (v xor r.toInt()) and 0x80) != 0
        cpu.A = r
        cpu.updateZN(cpu.A)
    }

    private fun sbc(value: UByte) {
        val a = cpu.A.toInt()
        val v = value.toInt()
        val c = if (cpu.C) 0 else 1
        val result = a - v - c
        cpu.C = result >= 0
        val r = (result and 0xFF).toUByte()
        cpu.V = ((a xor v) and (a xor r.toInt()) and 0x80) != 0
        cpu.A = r
        cpu.updateZN(cpu.A)
    }

    private fun cmp(value: UByte) = compare(cpu.A, value)
    private fun cpx(value: UByte) = compare(cpu.X, value)
    private fun cpy(value: UByte) = compare(cpu.Y, value)

    private fun compare(reg: UByte, value: UByte) {
        val result = reg.toInt() - value.toInt()
        cpu.C = result >= 0
        cpu.Z = result == 0
        cpu.N = (result and 0x80) != 0
    }

    private fun bit(value: UByte) {
        cpu.Z = (cpu.A.toInt() and value.toInt()) == 0
        cpu.N = (value.toInt() and 0x80) != 0
        cpu.V = (value.toInt() and 0x40) != 0
    }

    private fun inc(addr: Int) {
        val value = (read(addr).toInt() + 1 and 0xFF).toUByte()
        write(addr, value)
        cpu.updateZN(value)
    }

    private fun dec(addr: Int) {
        val value = (read(addr).toInt() - 1 and 0xFF).toUByte()
        write(addr, value)
        cpu.updateZN(value)
    }

    private fun aslAcc() {
        cpu.C = (cpu.A.toInt() and 0x80) != 0
        cpu.A = ((cpu.A.toInt() shl 1) and 0xFF).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun aslMem(addr: Int) {
        val value = read(addr).toInt()
        cpu.C = (value and 0x80) != 0
        val result = ((value shl 1) and 0xFF).toUByte()
        write(addr, result)
        cpu.updateZN(result)
    }

    private fun lsrAcc() {
        cpu.C = (cpu.A.toInt() and 0x01) != 0
        cpu.A = (cpu.A.toInt() ushr 1).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun lsrMem(addr: Int) {
        val value = read(addr).toInt()
        cpu.C = (value and 0x01) != 0
        val result = (value ushr 1).toUByte()
        write(addr, result)
        cpu.updateZN(result)
    }

    private fun rolAcc() {
        val oldC = if (cpu.C) 1 else 0
        cpu.C = (cpu.A.toInt() and 0x80) != 0
        cpu.A = (((cpu.A.toInt() shl 1) or oldC) and 0xFF).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun rolMem(addr: Int) {
        val value = read(addr).toInt()
        val oldC = if (cpu.C) 1 else 0
        cpu.C = (value and 0x80) != 0
        val result = (((value shl 1) or oldC) and 0xFF).toUByte()
        write(addr, result)
        cpu.updateZN(result)
    }

    private fun rorAcc() {
        val oldC = if (cpu.C) 0x80 else 0
        cpu.C = (cpu.A.toInt() and 0x01) != 0
        cpu.A = ((cpu.A.toInt() ushr 1) or oldC).toUByte()
        cpu.updateZN(cpu.A)
    }

    private fun rorMem(addr: Int) {
        val value = read(addr).toInt()
        val oldC = if (cpu.C) 0x80 else 0
        cpu.C = (value and 0x01) != 0
        val result = ((value ushr 1) or oldC).toUByte()
        write(addr, result)
        cpu.updateZN(result)
    }

    private fun branch(condition: Boolean) {
        val offset = read(cpu.PC.toInt()).toByte().toInt()  // Signed
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()
        if (condition) {
            cpu.PC = (cpu.PC.toInt() + offset).toUShort()
        }
    }

    private fun jmpAbs() {
        cpu.PC = readWord(cpu.PC.toInt()).toUShort()
    }

    private fun jmpInd() {
        val addr = readWord(cpu.PC.toInt())
        // 6502 indirect jump bug: if low byte is $FF, high byte wraps within same page
        val lo = read(addr).toInt()
        val hi = read((addr and 0xFF00) or ((addr + 1) and 0xFF)).toInt()
        cpu.PC = (lo or (hi shl 8)).toUShort()
    }

    private fun jsr() {
        val callerPc = cpu.PC.toInt() - 1  // PC of JSR instruction (already incremented)
        val target = readWord(cpu.PC.toInt())
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()  // Point to last byte of instruction
        pushWord(cpu.PC.toInt())
        jsrHook?.invoke(target, callerPc)
        // Check if this subroutine causes frame debt (takes multiple frames to complete)
        // @FRAMES_CONSUMED: N means N frames total, so skip N-1 subsequent NMIs
        frameDebtMap[target]?.let { framesConsumed ->
            val skipCount = framesConsumed - 1  // Skip N-1 frames after this one
            frameDebt = maxOf(frameDebt, skipCount)
        }
        cpu.PC = target.toUShort()
    }

    private fun rts() {
        rtsHook?.invoke()
        cpu.PC = (pullWord() + 1).toUShort()
    }

    private fun rti() {
        rtiHook?.invoke()
        cpu.setStatusByte(pullByte().toUByte())
        cpu.PC = pullWord().toUShort()
        inNmiHandler = false  // Allow next NMI after handler completes
    }

    private fun brk() {
        cpu.PC = (cpu.PC.toInt() + 1).toUShort()  // Skip padding byte
        pushWord(cpu.PC.toInt())
        pushByte(cpu.getStatusByte().toInt() or 0x10)  // Set B flag
        cpu.I = true
        cpu.PC = readWord(0xFFFE).toUShort()
    }

    private fun php() {
        pushByte(cpu.getStatusByte().toInt() or 0x10)  // B flag set when pushed
    }

    private fun plp() {
        cpu.setStatusByte(pullByte().toUByte())
    }

    private fun pha() {
        pushByte(cpu.A.toInt())
    }

    private fun pla() {
        cpu.A = pullByte().toUByte()
        cpu.updateZN(cpu.A)
    }

    // =====================================================================
    // Stack operations
    // =====================================================================

    private fun pushByte(value: Int) {
        write(0x100 + cpu.SP.toInt(), (value and 0xFF).toUByte())
        cpu.SP = (cpu.SP.toInt() - 1 and 0xFF).toUByte()
    }

    private fun pullByte(): Int {
        cpu.SP = (cpu.SP.toInt() + 1 and 0xFF).toUByte()
        return read(0x100 + cpu.SP.toInt()).toInt()
    }

    private fun pushWord(value: Int) {
        pushByte((value shr 8) and 0xFF)
        pushByte(value and 0xFF)
    }

    private fun pullWord(): Int {
        val lo = pullByte()
        val hi = pullByte()
        return lo or (hi shl 8)
    }
}

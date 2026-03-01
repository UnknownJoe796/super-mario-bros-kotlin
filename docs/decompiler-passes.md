# 6502 Decompiler Pass Pipeline

Here's a comprehensive enumeration of passes a 6502 decompiler should use, in dependency order:

## Phase 1: Parsing & Initial Analysis

### Pass 1: **Assembly Parsing**
- Parse assembly text into structured `AssemblyLine` objects
- Extract labels, instructions, operands, and comments
- Build symbol table of all labels

### Pass 2: **Address Resolution**
- Assign absolute addresses to all instructions
- Resolve label references to addresses
- Identify data sections vs code sections

### Pass 3: **Disassembly Validation**
- Verify all instructions are valid 6502 opcodes
- Check addressing mode consistency
- Flag potential data embedded in code

## Phase 2: Function & Block Discovery

### Pass 4: **Entry Point Discovery**
- Identify all function entry points:
    - JSR targets
    - Interrupt vectors (NMI, RESET, IRQ)
    - Exported/public labels
    - Jump table targets

### Pass 5: **Reachability Analysis**
- Trace all reachable code from entry points
- Mark dead/unreachable code
- Identify code vs data regions

### Pass 6: **Basic Block Construction**
- Identify block leaders:
    - First instruction
    - Branch/jump targets
    - Instructions immediately after branches/calls
- Split code into basic blocks (straight-line sequences)
- Handle fall-through between blocks

### Pass 7: **Control Flow Graph (CFG) Construction**
- Link basic blocks with edges:
    - Conditional branches (true/false edges)
    - Unconditional jumps
    - Fall-through edges
    - Return edges
- Build per-function CFGs

### Pass 8: **Function Boundary Detection**
- Group basic blocks into functions
- Identify function boundaries:
    - Entry: JSR targets, labels referenced externally
    - Exit: RTS, RTI, tail calls (JMP to another function)
- Handle overlapping functions (rare but possible)
- Detect unreachable code within functions

## Phase 3: Data Flow Analysis

### Pass 9: **Dominator Tree Construction**
- Build dominator tree for each function
- Identify natural loops (back edges in dominator tree)
- Calculate dominance frontiers for SSA construction

### Pass 10: **Liveness Analysis**
- Compute live-in/live-out sets for each basic block
- Track register liveness (A, X, Y)
- Track flag liveness (N, V, Z, C)
- Track memory location liveness

### Pass 11: **Use-Def Chain Construction**
- Build def-use chains (where values are defined and used)
- Identify reaching definitions
- Track value flow through registers and memory

### Pass 12: **Function Input/Output Analysis**
- **Inputs**: Registers/flags/memory read before written
- **Outputs**: Registers/flags/memory written and live at exit
- **Clobbers**: Registers/flags modified but not returned
- **Side effects**: Memory modifications visible externally

### Pass 13: **Call Graph Construction**
- Build graph of function call relationships
- Identify recursive functions
- Detect indirect calls (jump tables)

## Phase 4: Structural Analysis

### Pass 14: **Loop Detection**
- Identify natural loops using dominator analysis
- Classify loop types:
    - Do-while (branch at bottom)
    - While (branch at top)
    - Counted loops (using index register)
- Detect loop induction variables
- Find loop exit conditions

### Pass 15: **Conditional Structure Detection**
- Identify if/then/else patterns
- Detect switch statements (jump tables)
- Recognize short-circuit boolean evaluation
- Find nested conditionals

### Pass 16: **Region Formation**
- Group blocks into high-level regions:
    - Sequence (straight-line code)
    - If-then, If-then-else
    - Loops (while, do-while, for-like)
    - Switch
- Build Abstract Syntax Tree (AST) structure

### Pass 17: **Goto Elimination**
- Convert structured jumps to high-level control flow
- Identify remaining gotos that can't be eliminated
- Flag irreducible control flow

## Phase 5: Type & Value Analysis

### Pass 18: **Constant Propagation**
- Track constant values through execution
- Fold constant expressions
- Simplify branches with known conditions

### Pass 19: **Memory Access Pattern Analysis**
- Detect array accesses (base + index)
- Identify structure field accesses
- Recognize pointer indirection patterns
- Distinguish scalars from arrays

### Pass 20: **Type Inference**
- Infer types from usage:
    - Boolean (used in branches only)
    - Counter (increment/decrement/compare)
    - Index (used with offset addressing)
    - Pointer (used with indirect addressing)
    - Enum (limited set of constant values)
    - Signed vs unsigned (based on comparisons)
- Propagate type constraints

### Pass 21: **Stack Frame Analysis**
- Match PHA/PLA pairs
- Identify saved registers
- Detect parameter passing via stack
- Calculate stack frame layout

## Phase 6: Expression Reconstruction

### Pass 22: **SSA Construction** (Optional but helpful)
- Convert to Static Single Assignment form
- Makes data flow explicit
- Simplifies many optimizations

### Pass 23: **Expression Tree Building**
- Reconstruct complex expressions from instruction sequences
- Recognize arithmetic/logical operations
- Build expression trees bottom-up

### Pass 24: **Idiom Recognition**
- Recognize 6502-specific patterns:
    - 16-bit arithmetic (multi-byte operations)
    - Multiplication/division sequences
    - Bit manipulation patterns
    - Common library function patterns
- Replace with high-level equivalents

### Pass 25: **Flag Simplification**
- Eliminate redundant flag operations
- Merge compare with subsequent branch
- Simplify flag-to-boolean conversions

### Pass 26: **Common Subexpression Elimination**
- Identify duplicate computations
- Introduce temporary variables
- Reuse computed values

## Phase 7: Variable & Naming

### Pass 27: **Variable Identification**
- Group memory accesses to same location
- Identify distinct variables vs array elements
- Determine variable scope (local, global, parameter)

### Pass 28: **Variable Naming**
- Use original assembly labels where available
- Generate meaningful names based on usage:
    - `counter`, `index`, `flag`, `temp`
    - Function-specific prefixes
- Apply naming conventions

### Pass 29: **Parameter Recovery**
- Identify function parameters from inputs
- Determine parameter passing mechanism:
    - Registers (most common)
    - Memory locations
    - Stack
- Order and name parameters

## Phase 8: Optimization & Cleanup

### Pass 30: **Dead Code Elimination**
- Remove unused assignments
- Eliminate unreachable code
- Prune unused variables

### Pass 31: **Copy Propagation**
- Replace variable copies with original
- Simplify register transfers (TXA, TAY, etc.)

### Pass 32: **Algebraic Simplification**
- Simplify arithmetic expressions
- Fold constants
- Apply algebraic identities

### Pass 33: **Control Flow Simplification**
- Merge adjacent blocks
- Eliminate empty blocks
- Simplify trivial conditions

### Pass 34: **Variable Lifetime Analysis**
- Determine variable scopes
- Promote variables to narrower scopes when possible
- Identify variables that can be reused

## Phase 9: Code Generation

### Pass 35: **AST to Kotlin Conversion**
- Generate Kotlin code from AST
- Apply language-specific idioms
- Handle Kotlin type system constraints

### Pass 36: **Comment Generation**
- Preserve original assembly as comments
- Add explanatory comments for complex logic
- Document function signatures

### Pass 37: **Code Formatting**
- Apply consistent indentation
- Format expressions for readability
- Add blank lines between logical sections

### Pass 38: **Final Validation**
- Verify generated code compiles
- Check type safety
- Validate against original behavior (if tests available)

## Optional Advanced Passes

### Pass 39: **Deobfuscation** (if needed)
- Detect obfuscation patterns
- Simplify intentionally complex code
- Resolve indirect jumps

### Pass 40: **Documentation Generation**
- Extract function purposes from comments
- Generate API documentation
- Create call flow diagrams

### Pass 41: **Test Generation**
- Create unit tests for pure functions
- Generate property-based tests
- Validate against original binary

---

## Pass Dependencies

Some passes have strict ordering requirements:
- CFG construction must precede all flow analysis
- Dominator analysis required for SSA and loop detection
- Type inference benefits from constant propagation
- Expression building needs use-def chains
- Code generation is last

Others can be reordered or iterated:
- Some optimization passes can run multiple times
- Type inference and constant propagation can iterate
- Pattern recognition can occur at different levels

## Implementation Strategy

For your specific use case (6502 → Kotlin with preserved comments), I'd recommend:

**Minimal viable pipeline:**
1. Passes 1-8 (through function boundary detection)
2. Passes 10-12 (liveness and I/O analysis)
3. Passes 14-16 (structural analysis)
4. Pass 20 (basic type inference)
5. Passes 23-25 (expression reconstruction)
6. Passes 35-37 (code generation with comments)

**Full-featured pipeline:**
All passes, with emphasis on idiom recognition (Pass 24) to recognize Super Mario Bros specific patterns.

Would you like detailed algorithms for any specific passes?
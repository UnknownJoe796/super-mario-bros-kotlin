package com.ivieleague.smbtranslation

//> JumpEngine:
//> asl          ;shift bit from contents of A
//> tay
//> pla          ;pull saved return address from stack
//> sta $04      ;save to indirect
//> pla
//> sta $05
//> iny
//> lda ($04),y  ;load pointer from indirect
//> sta $06      ;note that if an RTS is performed in next routine
//> iny          ;it will return to the execution before the sub
//> lda ($04),y  ;that called this routine
//> sta $07
//> jmp ($06)    ;jump to the address we loaded
// This whole thing really exists because indirection was quite difficult in NES assembly.
// For us, an enum that calls the correct function is trivial.
// Thus, this is unimplemented in Kotlin.
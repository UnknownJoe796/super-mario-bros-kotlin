package com.ivieleague.smbtranslation.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupifyTest {

    @Test
    fun forwardBranchGroupsAsIf_thenBodyBetweenBranchAndTarget() {
        // Lines:
        // 0: Start:
        // 1: bne Skip
        // 2: lda #$01
        // 3: Skip:
        // 4: rts
        val lines = listOf(
            AssemblyLine(label = "Start"),
            AssemblyLine(instruction = AssemblyInstruction("bne", AssemblyAddressing.Label("Skip"))),
            AssemblyLine(instruction = AssemblyInstruction("lda", AssemblyAddressing.ValueHex(0x01))),
            AssemblyLine(label = "Skip"),
            AssemblyLine(instruction = AssemblyInstruction("rts")),
        )

        val groups = lines.groupify()

        // Expect: [ If(bne -> then [Direct(lda)]), Direct(label Skip, rts) ]
        assertEquals(3, groups.size, "Expected three top-level groups: leading Direct label, If, and trailing Direct")

        val leading = assertIs<AssemblyGrouping.Direct>(groups[0])
        assertEquals(1, leading.lines.size)
        assertEquals("Start", leading.lines[0].label)

        val ifGroup = assertIs<AssemblyGrouping.If>(groups[1])
        assertEquals("bne", ifGroup.branchInstruction.instruction?.op, "Branch should be the bne line")
        assertEquals(1, ifGroup.then.size, "Then body should have a single Direct group")
        val thenDirect = assertIs<AssemblyGrouping.Direct>(ifGroup.then[0])
        assertEquals(1, thenDirect.lines.size, "Then Direct should contain the lda line only")
        assertEquals("lda", thenDirect.lines[0].instruction?.op)

        val trailing = assertIs<AssemblyGrouping.Direct>(groups[2])
        assertEquals(2, trailing.lines.size, "Trailing Direct should include the Skip label and rts")
        assertEquals("Skip", trailing.lines[0].label)
        assertEquals("rts", trailing.lines[1].instruction?.op)
    }

    @Test
    fun backwardBranchGroupsAsDoWhile_bodyBetweenLabelAndBranch() {
        // Lines:
        // 0: Loop:
        // 1: lda #$01
        // 2: bne Loop
        val lines = listOf(
            AssemblyLine(label = "Loop"),
            AssemblyLine(instruction = AssemblyInstruction("lda", AssemblyAddressing.ValueHex(0x01))),
            AssemblyLine(instruction = AssemblyInstruction("bne", AssemblyAddressing.Label("Loop"))),
        )

        val groups = lines.groupify()

        // Expect: [ Direct(Loop:), DoWhile([Direct(lda)], bne Loop) ]
        assertEquals(2, groups.size, "Expected Direct for the label then DoWhile for the backward branch")

        val directLabel = assertIs<AssemblyGrouping.Direct>(groups[0])
        assertEquals(2, directLabel.lines.size, "Leading Direct should merge label and following non-branch line")
        assertEquals("Loop", directLabel.lines[0].label)
        assertEquals("lda", directLabel.lines[1].instruction?.op)

        val doWhile = assertIs<AssemblyGrouping.DoWhile>(groups[1])
        assertEquals("bne", doWhile.branchInstruction.instruction?.op)
        assertEquals(1, doWhile.contents.size)
        val bodyDirect = assertIs<AssemblyGrouping.Direct>(doWhile.contents[0])
        assertEquals(1, bodyDirect.lines.size)
        assertEquals("lda", bodyDirect.lines[0].instruction?.op)
    }
}
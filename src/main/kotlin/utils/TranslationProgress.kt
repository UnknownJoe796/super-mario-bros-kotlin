// by Claude
package com.ivieleague.smbtranslation.utils

import java.io.File

/**
 * Translation progress tracker for the SMB disassembly-to-Kotlin translation project.
 *
 * Parses smbdism.asm to find all subroutines in the code section (after .org $8000),
 * then scans existing Kotlin source files to determine which have been translated.
 * Outputs a detailed progress report grouped by functional category.
 */

data class AsmSubroutine(
    val name: String,
    val lineNumber: Int,
    val nearestComment: String,
)

data class TranslationStatus(
    val subroutine: AsmSubroutine,
    val isTranslated: Boolean,
    val translationKind: TranslationKind,
    val kotlinFile: String?,
)

enum class TranslationKind {
    /** Fully translated with real logic */
    FULL,
    /** Has a function stub with TODO or empty body */
    STUB,
    /** Not translated at all */
    NONE,
}

enum class SubroutineCategory(val displayName: String) {
    INIT("Initialization & Boot"),
    NMI("NMI & Frame Processing"),
    MODE_CONTROL("Mode Control & Execution"),
    TITLE_SCREEN("Title Screen & Menus"),
    VICTORY("Victory Mode"),
    GAME_OVER("Game Over"),
    SCREEN_ROUTINES("Screen Routines & Display"),
    AREA_PARSER("Area Parser & Level Objects"),
    AREA_ENTRANCE("Area Entrance & Setup"),
    PLAYER_MOVEMENT("Player Movement & Physics"),
    PLAYER_STATE("Player State & Actions"),
    PLAYER_GFX("Player Graphics"),
    ENEMY_INIT("Enemy Initialization"),
    ENEMY_MOVEMENT("Enemy Movement"),
    ENEMY_AI("Enemy AI & Behavior"),
    ENEMY_GFX("Enemy Graphics"),
    COLLISION("Collision Detection"),
    BLOCK_INTERACTION("Block & Metatile Interaction"),
    OBJECTS("Misc Game Objects"),
    PLATFORMS("Platforms"),
    SCORING("Scoring & Status Bar"),
    SCROLLING("Scrolling & Camera"),
    SOUND("Sound Engine & Music"),
    PPU("PPU & VRAM"),
    DATA_TABLE("Data Tables"),
    MISC("Miscellaneous"),
}

fun main() {
    val projectRoot = File(".")
    val asmFile = projectRoot.resolve("smbdism.asm")
    val kotlinSrcDir = projectRoot.resolve("src/main/kotlin")

    if (!asmFile.exists()) {
        System.err.println("ERROR: smbdism.asm not found at ${asmFile.absolutePath}")
        System.err.println("Run this from the project root directory.")
        return
    }

    val asmLines = asmFile.readLines()
    val subroutines = parseSubroutines(asmLines)
    val kotlinFiles = kotlinSrcDir.walkTopDown().filter { it.extension == "kt" }.toList()
    val kotlinContent = kotlinFiles.associate { it.relativeTo(projectRoot).path to it.readText() }

    val statuses = analyzeTranslationStatus(subroutines, kotlinContent)

    printReport(statuses)
}

/**
 * Parses the assembly file to find subroutine-level labels in the code section.
 *
 * Identifies subroutines by finding labels that are targets of JSR instructions
 * or appear in .dw jump engine tables. Data-only labels (followed exclusively by
 * .db/.dw directives) are categorized separately.
 */
fun parseSubroutines(lines: List<String>): List<AsmSubroutine> {
    // Find the code section start
    val orgLineIndex = lines.indexOfFirst { it.trim().startsWith(".org \$8000") }
    if (orgLineIndex < 0) {
        System.err.println("ERROR: Could not find .org \$8000 directive")
        return emptyList()
    }

    // Collect all JSR targets and .dw table entries (these are subroutine references)
    val jsrTargets = mutableSetOf<String>()
    val dwTargets = mutableSetOf<String>()

    val jsrPattern = Regex("""jsr\s+([A-Z][A-Za-z0-9_]+)""")
    val dwPattern = Regex("""\.dw\s+([A-Z][A-Za-z0-9_]+)""")

    for (i in orgLineIndex until lines.size) {
        val line = lines[i]
        jsrPattern.find(line)?.let { jsrTargets.add(it.groupValues[1]) }
        dwPattern.find(line)?.let { dwTargets.add(it.groupValues[1]) }
    }

    val subroutineTargets = jsrTargets + dwTargets

    // Parse labels at column 0
    val labelPattern = Regex("""^([A-Z][A-Za-z0-9_]+):""")
    val results = mutableListOf<AsmSubroutine>()

    for (i in orgLineIndex until lines.size) {
        val line = lines[i]
        val match = labelPattern.find(line) ?: continue
        val labelName = match.groupValues[1]

        // Check if this is a data-only label by looking at the next non-blank line
        val isDataTable = isDataTableLabel(lines, i)

        // Only include labels that are JSR/.dw targets or are after section dividers
        val isAfterDivider = isLabelAfterSectionDivider(lines, i)

        if (labelName in subroutineTargets || (isAfterDivider && !isDataTable)) {
            val comment = findNearestComment(lines, i)
            results.add(AsmSubroutine(labelName, i + 1, comment))
        } else if (isDataTable && isAfterDivider) {
            // Track data tables separately - we still want to count them
            val comment = findNearestComment(lines, i)
            results.add(AsmSubroutine(labelName, i + 1, comment))
        }
    }

    return results.distinctBy { it.name }
}

/**
 * Checks if a label is followed by .db/.dw data directives (making it a data table, not a subroutine).
 */
private fun isDataTableLabel(lines: List<String>, labelLineIndex: Int): Boolean {
    val labelLine = lines[labelLineIndex]
    // If the label line itself has an instruction after the colon, it's not a pure data table
    val afterColon = labelLine.substringAfter(":", "").trim()
    if (afterColon.isNotEmpty() && !afterColon.startsWith(";") && !afterColon.startsWith(".d")) {
        return false
    }
    if (afterColon.startsWith(".db") || afterColon.startsWith(".dw")) {
        return true
    }

    // Check following lines
    for (j in (labelLineIndex + 1) until minOf(labelLineIndex + 5, lines.size)) {
        val nextLine = lines[j].trim()
        if (nextLine.isEmpty() || nextLine.startsWith(";")) continue
        return nextLine.startsWith(".db") || nextLine.startsWith(".dw")
    }
    return false
}

/**
 * Checks if a label appears right after a section divider (;---...---).
 */
private fun isLabelAfterSectionDivider(lines: List<String>, labelLineIndex: Int): Boolean {
    for (j in (labelLineIndex - 1) downTo maxOf(0, labelLineIndex - 10)) {
        val prevLine = lines[j].trim()
        if (prevLine.isEmpty()) continue
        if (prevLine.startsWith(";---")) return true
        if (prevLine.startsWith(";")) continue // Skip comment lines between divider and label
        return false // Hit a non-comment, non-blank line
    }
    return false
}

/**
 * Finds the nearest descriptive comment above a label.
 */
private fun findNearestComment(lines: List<String>, labelLineIndex: Int): String {
    // Check the label line itself for an inline comment
    val inlineComment = lines[labelLineIndex].substringAfter(";", "").trim()
    if (inlineComment.isNotEmpty()) return inlineComment

    // Look at preceding lines for comments
    for (j in (labelLineIndex - 1) downTo maxOf(0, labelLineIndex - 5)) {
        val prevLine = lines[j].trim()
        if (prevLine.isEmpty()) continue
        if (prevLine.startsWith(";---")) return ""
        if (prevLine.startsWith(";")) {
            val comment = prevLine.removePrefix(";").trim()
            if (comment.isNotEmpty() && !comment.all { it == '-' }) return comment
        }
        break
    }
    return ""
}

/**
 * Scans existing Kotlin source files for translated subroutines.
 * Matches by: fun System.xxx() patterns and //> LabelName: comments.
 */
fun analyzeTranslationStatus(
    subroutines: List<AsmSubroutine>,
    kotlinContent: Map<String, String>,
): List<TranslationStatus> {
    // Build lookup of translated function names and their source files
    val funPattern = Regex("""fun\s+System\.(\w+)\s*\(""")
    val commentPattern = Regex("""//>\s*(\w+):""")
    val todoPattern = Regex("""\{\s*/\*\s*TODO\s*\*/\s*\}""")
    val todoCallPattern = Regex("""=\s*TODO\(""")

    data class KotlinFunctionInfo(
        val file: String,
        val isStub: Boolean,
    )

    // Map from camelCase kotlin name -> info
    val translatedFunctions = mutableMapOf<String, KotlinFunctionInfo>()
    // Map from PascalCase asm label -> file where //> comment appears
    val commentedLabels = mutableMapOf<String, String>()

    for ((filePath, content) in kotlinContent) {
        val contentLines = content.lines()
        for ((lineIdx, line) in contentLines.withIndex()) {
            funPattern.find(line)?.let { match ->
                val funcName = match.groupValues[1]
                // Check if this is a stub (TODO or empty body)
                val nextFewLines = contentLines.drop(lineIdx).take(3).joinToString(" ")
                val isStub = todoPattern.containsMatchIn(nextFewLines) ||
                    todoCallPattern.containsMatchIn(nextFewLines)
                translatedFunctions[funcName] = KotlinFunctionInfo(filePath, isStub)
            }
            commentPattern.findAll(line).forEach { match ->
                commentedLabels[match.groupValues[1]] = filePath
            }
        }
    }

    return subroutines.map { sub ->
        val camelName = asmLabelToCamelCase(sub.name)
        val funcInfo = translatedFunctions[camelName]
        val commentFile = commentedLabels[sub.name]
        val kotlinFile = funcInfo?.file ?: commentFile

        val kind = when {
            funcInfo != null && !funcInfo.isStub -> TranslationKind.FULL
            funcInfo != null && funcInfo.isStub -> TranslationKind.STUB
            commentFile != null -> TranslationKind.FULL // Referenced via //> comment means it's part of a translated block
            else -> TranslationKind.NONE
        }

        TranslationStatus(
            subroutine = sub,
            isTranslated = kind != TranslationKind.NONE,
            translationKind = kind,
            kotlinFile = kotlinFile,
        )
    }
}

/**
 * Converts an assembly PascalCase label name to Kotlin camelCase.
 * Examples: "GameCoreRoutine" -> "gameCoreRoutine", "NonMaskableInterrupt" -> "nonMaskableInterrupt"
 *
 * Handles underscore-separated labels like "Bridge_High" -> "bridge_High" and
 * numeric prefixes like "Hidden1UpBlock" -> "hidden1UpBlock".
 */
fun asmLabelToCamelCase(label: String): String {
    if (label.isEmpty()) return label
    return label[0].lowercase() + label.substring(1)
}

fun categorizeSubroutine(name: String, @Suppress("UNUSED_PARAMETER") comment: String, isDataTable: Boolean): SubroutineCategory {
    val lower = name.lowercase()

    // Data tables
    if (isDataTable) return SubroutineCategory.DATA_TABLE

    // Sound
    if (lower.contains("sound") || lower.contains("music") || lower.contains("sfx") ||
        lower.contains("noise") || lower.contains("square1") || lower.contains("square2") ||
        lower.contains("squ1") || lower.contains("squ2") || lower.contains("freq") ||
        lower.contains("triangle") || lower.contains("envelope") || lower.contains("delta") ||
        lower.contains("loadcontrolregs") || lower.contains("dump_sq") || lower.contains("dump_squ")
    ) return SubroutineCategory.SOUND

    // Init/Boot
    if (lower == "start" || lower.contains("initializememory") || lower.contains("initscroll") ||
        lower.contains("initializegame") || lower.contains("secondarygamesetup") ||
        lower.contains("primarygamesetup") || lower.contains("initializenametables") ||
        lower.contains("writeppureg1") || lower.contains("moveallspritesoffscreen") ||
        lower.contains("movespritesoffscreen")
    ) return SubroutineCategory.INIT

    // NMI / frame
    if (lower.contains("nonmaskableinterrupt") || lower.contains("dectimers") ||
        lower.contains("pauseroutine") || lower.contains("spriteshuffler") ||
        lower.contains("readjoypad") || lower.contains("readportbits")
    ) return SubroutineCategory.NMI

    // Mode control
    if (lower.contains("opermodeexecutiontree") || lower.contains("jumpengine") ||
        lower.contains("gamecoreroutine") || lower.contains("gameroutines") ||
        lower.contains("execgameloopback") || lower.contains("donothing")
    ) return SubroutineCategory.MODE_CONTROL

    // Title screen
    if (lower.contains("titlescreen") || lower.contains("gamemenu") || lower.contains("demo") ||
        lower.contains("drawmushroom") || lower.contains("drawtitlescreen") ||
        lower.contains("continuegame") || lower.contains("gocontinue") ||
        lower.contains("gametext")
    ) return SubroutineCategory.TITLE_SCREEN

    // Victory mode
    if (lower.contains("victory") || lower.contains("bridgecollapse") || lower.contains("retainer") ||
        lower.contains("thankplayer") || lower.contains("endworld") || lower.contains("playerend") ||
        lower.contains("endtimer") || lower.contains("incmsgcounter") || lower.contains("printmsg") ||
        lower.contains("terminategame")
    ) return SubroutineCategory.VICTORY

    // Game over
    if (lower.contains("gameover") || lower.contains("rungameover") || lower.contains("setupgameover") ||
        lower.contains("gamemode")
    ) return SubroutineCategory.GAME_OVER

    // Area parser & level objects
    if (lower.contains("areaparser") || lower.contains("decode") || lower.contains("areaobj") ||
        lower.contains("incrementcolumn") || lower.contains("incareaobj") ||
        lower.contains("processareadata") || lower.contains("blockbufferaddr") ||
        lower.contains("areastyleobject") || lower.contains("rowof") || lower.contains("columnof") ||
        lower.contains("hole_") || lower.contains("bridge_") || lower.contains("pulleyrope") ||
        lower.contains("questionblock") || lower.contains("endlessrope") ||
        lower.contains("balanceplatrope") || lower.contains("castleobject") ||
        lower.contains("staircaseobject") || lower.contains("exitpipe") ||
        lower.contains("flagball") || lower.contains("hidden1up") || lower.contains("brickwith") ||
        lower.contains("waterpipe") || lower.contains("emptyblock") || lower.contains("jumpspring") ||
        lower.contains("intropipe") || lower.contains("flagpoleobject") ||
        lower.contains("axeobj") || lower.contains("chainobj") || lower.contains("castlebridgeobj") ||
        lower.contains("scrolllockobject") || lower.contains("areafrenzy") || lower.contains("loopcmd") ||
        lower.contains("alterareaattributes") || lower.contains("areatype") ||
        lower.contains("chklrgobjlength") || lower.contains("chklrgobjfixed") ||
        lower.contains("getlrgobjattrib") || lower.contains("processlengthdata") ||
        lower.contains("verticalpipe") || lower.contains("rendersidewayspipe") ||
        lower.contains("renderunderpart") || lower.contains("setupplatformrope") ||
        lower.contains("getpipeheight") || lower.contains("findemptyenemy") ||
        lower.contains("getchkobjlen") || lower.contains("coinblock") ||
        lower.contains("starblock") || lower.contains("mushflower") || lower.contains("extralife") ||
        lower.contains("vineblock") || lower.contains("warpzone") || lower.contains("treeled") ||
        lower.contains("mushroomledge") || lower.contains("initblock") ||
        lower.contains("castlemetatile") || lower.contains("c_objectrow") ||
        lower.contains("holemetatile") || lower.contains("getareaobjectid") ||
        lower.contains("getareaobjx") || lower.contains("getareaobjy")
    ) return SubroutineCategory.AREA_PARSER

    // Area entrance
    if (lower.contains("entrance") || lower.contains("getareamusic") || lower.contains("loadarea") ||
        lower.contains("findarea") || lower.contains("initializearea") || lower.contains("chgarea") ||
        lower.contains("getareadataaddr") || lower.contains("getareatype") ||
        lower.contains("setpipe") || lower.contains("sidewayspipe") || lower.contains("sideexit") ||
        lower.contains("verticalpipeentry") || lower.contains("setprout") || lower.contains("setentr")
    ) return SubroutineCategory.AREA_ENTRANCE

    // Player movement/physics
    if (lower.contains("playermovement") || lower.contains("playerhorizontal") ||
        lower.contains("playeryaxis") || lower.contains("playerphysics") ||
        lower.contains("imposefriction") || lower.contains("imposegravity") ||
        lower.contains("playervertical") || lower.contains("moveplayervertically") ||
        lower.contains("moveplayerhorizontally") || lower.contains("climbingsub") ||
        lower.contains("jumpswim") || lower.contains("ongroundstate") ||
        lower.contains("x_physics") || lower.contains("impedeplayer") ||
        lower.contains("stopplayermove") || lower.contains("fallingsub") ||
        lower.contains("climbadder") || lower.contains("moveplayeryaxis")
    ) return SubroutineCategory.PLAYER_MOVEMENT

    // Player state/actions
    if (lower.contains("playerctrl") || lower.contains("processplayer") ||
        lower.contains("findplayeraction") || lower.contains("doneplayertask") ||
        lower.contains("injureplayer") || lower.contains("playerinjury") ||
        lower.contains("handlechangesize") || lower.contains("playerchangesize") ||
        lower.contains("playerfireflower") || lower.contains("playerdeath") ||
        lower.contains("playerendlevel") || lower.contains("playerlose") ||
        lower.contains("transposeplayer") || lower.contains("playerentrance") ||
        lower.contains("flagpoleslide") || lower.contains("raiseflag") ||
        lower.contains("playerhammercollis") || lower.contains("handleclimbing") ||
        lower.contains("cycleplayerpalette") || lower.contains("handleanimate") ||
        lower.contains("vine_auto") || lower.contains("forceinjury") ||
        lower.contains("chkforplayerattrib") || lower.contains("getplayeranim") ||
        lower.contains("getplayercolors") || lower.contains("autocontrolplayer") ||
        lower.contains("enterside") || lower.contains("handlepipeentry") ||
        lower.contains("putonvine") || lower.contains("putplayeronvine") ||
        lower.contains("bgcolorctrl")
    ) return SubroutineCategory.PLAYER_STATE

    // Player graphics
    if (lower.contains("playergfx") || lower.contains("renderplayersub") ||
        lower.contains("drawplayer") || lower.contains("getanimoffset") ||
        lower.contains("getcurrentanimoffset") || lower.contains("animationcontrol") ||
        lower.contains("getoffsetfromanim")
    ) return SubroutineCategory.PLAYER_GFX

    // Enemy initialization
    if (lower.contains("inithammer") || lower.contains("initgoomba") || lower.contains("initbowser") ||
        lower.contains("initbloober") || lower.contains("initcheep") || lower.contains("initlakitu") ||
        lower.contains("initbullet") || lower.contains("initpiranhapl") || lower.contains("initpodoboo") ||
        lower.contains("initredkoopa") || lower.contains("initredptroopa") || lower.contains("initenemyobj") ||
        lower.contains("initnormalenemy") || lower.contains("initenemyfrenzy") ||
        lower.contains("initfireworks") || lower.contains("initflyingcheep") ||
        lower.contains("initdropplat") || lower.contains("initvertplat") ||
        lower.contains("inithoriplat") || lower.contains("initbalplat") ||
        lower.contains("initlongfirebar") || lower.contains("initshortfirebar") ||
        lower.contains("initretainerobj") || lower.contains("initjumpgpt") ||
        lower.contains("inithorizfly") || lower.contains("initbowserflame") ||
        lower.contains("endofenemyinitcode") || lower.contains("noinitcode") ||
        lower.contains("checkpointenemyid") || lower.contains("checkfrenzy") ||
        lower.contains("duplicateenemyobj") || lower.contains("chkenemyfrenzy") ||
        lower.contains("initvstf")
    ) return SubroutineCategory.ENEMY_INIT

    // Enemy movement
    if (lower.contains("movenormalenemy") || lower.contains("movebloober") ||
        lower.contains("movejumpingenemy") || lower.contains("movebulletbill") ||
        lower.contains("movelakitu") || lower.contains("movepiranhapl") ||
        lower.contains("movepodoboo") || lower.contains("moveswimmingcheep") ||
        lower.contains("moveflyingcheep") || lower.contains("moveflygreenp") ||
        lower.contains("moveredptroopa") || lower.contains("moved_enemy") ||
        lower.contains("movej_enemy") || lower.contains("moveenemyhorizontally") ||
        lower.contains("moveenemyslowvert") || lower.contains("enemymovement") ||
        lower.contains("xmovcntr") || lower.contains("xmovecntr") ||
        lower.contains("moveobjecthorizontally") || lower.contains("nomovecode") ||
        lower.contains("noruncode") || lower.contains("noruncod") ||
        lower.contains("procmoveredpt") || lower.contains("enemyturn") ||
        lower.contains("movewithxm")
    ) return SubroutineCategory.ENEMY_MOVEMENT

    // Enemy AI & behavior
    if (lower.contains("enemiesandloopscore") || lower.contains("runnormalenemi") ||
        lower.contains("runbowser") || lower.contains("bowserflame") || lower.contains("procbowser") ||
        lower.contains("lakituandspiny") || lower.contains("bulletbillhandl") || lower.contains("bulletbillcan") ||
        lower.contains("bulletbillcheep") || lower.contains("powerupobjhandl") ||
        lower.contains("prochammerbro") || lower.contains("prochammer") ||
        lower.contains("miscobjectscore") || lower.contains("firebarobjcore") ||
        lower.contains("firebarspin") || lower.contains("fireballobj") ||
        lower.contains("procfireball") || lower.contains("procfirebar") ||
        lower.contains("drawfirebar") || lower.contains("drawfireball") ||
        lower.contains("whirlpool") || lower.contains("vineobject") ||
        lower.contains("setup_vine") || lower.contains("runfirebar") ||
        lower.contains("runfireworks") || lower.contains("largeplatformsubr") ||
        lower.contains("runretainerobj") || lower.contains("runenemyobj") ||
        lower.contains("runlargeplatform") || lower.contains("runsmallplat") ||
        lower.contains("runstarflag") || lower.contains("runbowserflame") ||
        lower.contains("endarea") || lower.contains("delaytoareaend") ||
        lower.contains("gametimerfire") || lower.contains("awardgametimer") ||
        lower.contains("rungametimer") || lower.contains("starflagexit") ||
        lower.contains("killallenemi") || lower.contains("killenemi") ||
        lower.contains("shellorblocklefeat") || lower.contains("shellorblockdefeat") ||
        lower.contains("eraseenemyobj") || lower.contains("enemyjump") ||
        lower.contains("enemylanding") || lower.contains("enemyfaceplayer") ||
        lower.contains("setstun") || lower.contains("chktostunenemies") ||
        lower.contains("setxmoveamt") || lower.contains("nofrenzycode") ||
        lower.contains("spawnhammerobj") || lower.contains("drawhamm") ||
        lower.contains("flagpoleroutine") || lower.contains("flagpolegfx") ||
        lower.contains("jumpspringhandler") ||
        lower.contains("bubblecheck") || lower.contains("setupbubble") || lower.contains("drawbubble") ||
        lower.contains("procswimming") || lower.contains("setupfloateynumber") ||
        lower.contains("setupjumpcoin") || lower.contains("jcoingfx") ||
        lower.contains("setuplakitu") || lower.contains("brickshatter") ||
        lower.contains("spawnbrickchunk") || lower.contains("drawbrickchunk") ||
        lower.contains("handleenemyfball") || lower.contains("handlepower") ||
        lower.contains("processcannon") || lower.contains("bulletbillhandler") ||
        lower.contains("setuppowerup") || lower.contains("pwrupjmp") ||
        lower.contains("createspiny") || lower.contains("putatrightextent") ||
        lower.contains("handlegroup") || lower.contains("endfrenzy") ||
        lower.contains("procjumpcoin") || lower.contains("findemptymiscslot") ||
        lower.contains("getfirebarpos") || lower.contains("processbowserhalf") ||
        lower.contains("setflametimer") || lower.contains("alternatelength") ||
        lower.contains("proccannon") || lower.contains("bulletbillhandl")
    ) return SubroutineCategory.ENEMY_AI

    // Enemy graphics
    if (lower.contains("enemygfxhandler") || lower.contains("bowsergfxhandl") ||
        lower.contains("drawenemyobj") || lower.contains("drawspriteobj") ||
        lower.contains("drawonespriterow") || lower.contains("sixspritestacker") ||
        lower.contains("drawexplosion") || lower.contains("drawstarflag") ||
        lower.contains("drawpowerup") || lower.contains("drawvine") ||
        lower.contains("drawlargeplatform") || lower.contains("drawsmallplatform") ||
        lower.contains("drawrope") || lower.contains("drawblock") ||
        lower.contains("movesixsprites") || lower.contains("movesprrowoff") ||
        lower.contains("moveesprrow") || lower.contains("moveesprcol") ||
        lower.contains("movecoloffscreen") || lower.contains("getenemyboundbox") ||
        lower.contains("getgfxoffsetadder") || lower.contains("getproperobj") ||
        lower.contains("dumpfourspr") || lower.contains("dumpsixspr") ||
        lower.contains("dumpthreespr") || lower.contains("dumptwospr")
    ) return SubroutineCategory.ENEMY_GFX

    // Collision - detect before block interaction since some overlap
    if (lower.contains("collision") || lower.contains("playerbgcoll") ||
        lower.contains("playerheadcoll") || lower.contains("playerenemycoll") ||
        lower.contains("playerenemydiff") || lower.contains("playerlakitudiff") ||
        lower.contains("boundingboxcore") || lower.contains("enemytobgcoll") ||
        lower.contains("sprobjcoll") || lower.contains("chksmallplatcoll") ||
        lower.contains("largeplatformcoll") || lower.contains("smallplatformcoll") ||
        lower.contains("largeplatformboundbox") || lower.contains("smallplatformboundbox") ||
        lower.contains("getfirebarboundbox") ||
        lower.contains("getfireballboundbox") || lower.contains("getmiscboundbox") ||
        lower.contains("getplayeroffscreenbits") || lower.contains("getenemyoffscreenbits") ||
        lower.contains("getfireballoffscreenbits") || lower.contains("getmiscoffscreenbits") ||
        lower.contains("getbubbleoffscreenbits") || lower.contains("getblockoffscreenbits") ||
        lower.contains("offscreenboundscheck") || lower.contains("runoffscr") ||
        lower.contains("variableobjofs") || lower.contains("relativeblock") ||
        lower.contains("relativeenemy") || lower.contains("relativeplayer") ||
        lower.contains("relativefire") || lower.contains("relativemisc") ||
        lower.contains("relativebubble") || lower.contains("getobjelative") ||
        lower.contains("getobjrelative") || lower.contains("chkunderenemy") ||
        lower.contains("chkfornonsolid") || lower.contains("chkinvisible") ||
        lower.contains("chkjumpspringmeta") || lower.contains("chkforcoinmtile") ||
        lower.contains("chkforclimbmtile") || lower.contains("chkforsolidmtile") ||
        lower.contains("getmtileattrib") || lower.contains("blockbufferchk") ||
        lower.contains("blockbuffercoll") || lower.contains("positionplayeron") ||
        lower.contains("subtenemyypos") || lower.contains("dividep") ||
        lower.contains("checkplayervertical") || lower.contains("chkfortopofblock") ||
        lower.contains("checktopofblock") || lower.contains("getxoffscreenbits") ||
        lower.contains("smallbbox") || lower.contains("firebarenemycoll") ||
        lower.contains("fireballbgcoll") || lower.contains("fireballenemycoll") ||
        lower.contains("firebarcoll") || lower.contains("getenemyboundboxofs") ||
        lower.contains("getscreenposition") || lower.contains("chkforredkoopa") ||
        lower.contains("doenemysidecheck") || lower.contains("hammerbrobgcoll") ||
        lower.contains("landenemlyproperly") || lower.contains("landenemyproperly") ||
        lower.contains("playerbupper") || lower.contains("setbitsmask") ||
        lower.contains("chkforplayerc_largep") || lower.contains("eracm") ||
        lower.contains("solidmtileupper") || lower.contains("checkforsolidm") ||
        lower.contains("checkforclimbm") || lower.contains("checkforcoinm") ||
        lower.contains("chkleftco")
    ) return SubroutineCategory.COLLISION

    // Block/metatile interaction
    if (lower.contains("blockmetatile") || lower.contains("putblockmetatile") ||
        lower.contains("writeblockmetatile") || lower.contains("destroyblock") ||
        lower.contains("replaceblock") || lower.contains("removecoin") ||
        lower.contains("rembridge") || lower.contains("bumpblock") ||
        lower.contains("blockbumpedchk") || lower.contains("blockobjectscore") ||
        lower.contains("blockobjmt") || lower.contains("killenemyaboveblock") ||
        lower.contains("chkforbump") || lower.contains("chkforland") ||
        lower.contains("handleaxemetatile") || lower.contains("handlecoinmetatile")
    ) return SubroutineCategory.BLOCK_INTERACTION

    // Platforms
    if (lower.contains("platform") && !lower.contains("init") && !lower.contains("draw") &&
        !lower.contains("bbox") && !lower.contains("coll") && !lower.contains("run") &&
        !lower.contains("rope")
    ) return SubroutineCategory.PLATFORMS
    if (lower.contains("platlift") || lower.contains("movedropplat") || lower.contains("movefallingplat") ||
        lower.contains("moveliftplat") || lower.contains("movesmallplat") ||
        lower.contains("commonplat") || lower.contains("commonsmall") ||
        lower.contains("posplatform") || lower.contains("stopplatform") ||
        lower.contains("largelift") || lower.contains("initplatform") ||
        lower.contains("platformfall") || lower.contains("balanceplatform") ||
        lower.contains("xmovingplat") || lower.contains("ymovingplat") ||
        lower.contains("rightplat")
    ) return SubroutineCategory.PLATFORMS

    // Screen routines
    if (lower.contains("screenroutine") || lower.contains("initscreen") ||
        lower.contains("setupintermediate") || lower.contains("areapalette") ||
        lower.contains("getbackgroundcolor") || lower.contains("getalternatepalette") ||
        lower.contains("getareapalette") || lower.contains("writetopstatus") ||
        lower.contains("writebottomstatus") || lower.contains("displaytimeup") ||
        lower.contains("displayintermediate") || lower.contains("areaparseraskcontrol") ||
        lower.contains("areaparsertskcntrl") || lower.contains("clearbuffersdraw") ||
        lower.contains("incsubtask") || lower.contains("writetopscore") ||
        lower.contains("renderattributetables") || lower.contains("renderareagraphics") ||
        lower.contains("updatescreen") || lower.contains("writegametext") ||
        lower.contains("colorrotation") || lower.contains("resetscreentimer") ||
        lower.contains("resetspriteandscreen") || lower.contains("areaparsertraskcontrol") ||
        lower.contains("areaparsertskcntrl") || lower.contains("resetpalstar") ||
        lower.contains("drawplayer_inter") || lower.contains("writeaddr") ||
        lower.contains("writentaddr")
    ) return SubroutineCategory.SCREEN_ROUTINES

    // Scoring
    if (lower.contains("score") || lower.contains("floateynumber") ||
        lower.contains("printstatusbarnumber") || lower.contains("digitsmathrou") ||
        lower.contains("outputnumber") || lower.contains("updatenumber") ||
        lower.contains("updtopscore") || lower.contains("updatetopscore") ||
        lower.contains("topscorecheck") || lower.contains("giveonecoin") ||
        lower.contains("getsbnybb") || lower.contains("addtoscore") ||
        lower.contains("endareapoin")
    ) return SubroutineCategory.SCORING

    // Scrolling
    if (lower.contains("scroll") || lower.contains("updscrollvar") ||
        lower.contains("movevoffset") || lower.contains("chkpoffscr")
    ) return SubroutineCategory.SCROLLING

    // PPU/VRAM
    if (lower.contains("vram") || lower.contains("writeaddr") || lower.contains("setvramctrl") ||
        lower.contains("setvramoffset") || lower.contains("setvramaddr") ||
        lower.contains("initializenametable")
    ) return SubroutineCategory.PPU

    return SubroutineCategory.MISC
}

fun printReport(statuses: List<TranslationStatus>) {
    val total = statuses.size
    val fullTranslated = statuses.count { it.translationKind == TranslationKind.FULL }
    val stubs = statuses.count { it.translationKind == TranslationKind.STUB }
    val untranslated = statuses.count { it.translationKind == TranslationKind.NONE }
    val translatedOrStubbed = fullTranslated + stubs

    println("=" .repeat(80))
    println("  SUPER MARIO BROS. DISASSEMBLY -> KOTLIN TRANSLATION PROGRESS")
    println("=".repeat(80))
    println()
    println("  Total subroutines/labels found: $total")
    println("  Fully translated:               $fullTranslated (${pct(fullTranslated, total)})")
    println("  Stubbed (TODO):                  $stubs (${pct(stubs, total)})")
    println("  Untranslated:                    $untranslated (${pct(untranslated, total)})")
    println()

    val progressBar = buildProgressBar(fullTranslated, stubs, untranslated, 60)
    println("  $progressBar")
    println("  [### = translated] [... = stubbed] [    = remaining]")
    println()

    // Group by category
    val categorized = statuses.map { status ->
        val isDataTable = isDataTableByName(status.subroutine.name)
        val category = categorizeSubroutine(status.subroutine.name, status.subroutine.nearestComment, isDataTable)
        category to status
    }.groupBy({ it.first }, { it.second })

    println("-".repeat(80))
    println("  PROGRESS BY CATEGORY")
    println("-".repeat(80))
    println()
    println("  %-40s %6s %6s %6s %7s".format("Category", "Total", "Done", "Stub", "Pct"))
    println("  " + "-".repeat(72))

    for (category in SubroutineCategory.entries) {
        val items = categorized[category] ?: continue
        val catTotal = items.size
        val catFull = items.count { it.translationKind == TranslationKind.FULL }
        val catStub = items.count { it.translationKind == TranslationKind.STUB }
        val catDone = catFull + catStub
        println("  %-40s %6d %6d %6d %6s".format(
            category.displayName,
            catTotal,
            catFull,
            catStub,
            pct(catDone, catTotal)
        ))
    }

    println()
    println("-".repeat(80))
    println("  UNTRANSLATED SUBROUTINES (by category)")
    println("-".repeat(80))

    for (category in SubroutineCategory.entries) {
        val items = categorized[category] ?: continue
        val remaining = items.filter { it.translationKind == TranslationKind.NONE }
        if (remaining.isEmpty()) continue

        println()
        println("  ${category.displayName} (${remaining.size} remaining):")
        for (status in remaining.sortedBy { it.subroutine.lineNumber }) {
            val comment = if (status.subroutine.nearestComment.isNotEmpty()) {
                "  ; ${status.subroutine.nearestComment.take(50)}"
            } else ""
            println("    - %-35s (line %5d)%s".format(status.subroutine.name, status.subroutine.lineNumber, comment))
        }
    }

    // Stubbed subroutines section
    val stubbedItems = statuses.filter { it.translationKind == TranslationKind.STUB }
    if (stubbedItems.isNotEmpty()) {
        println()
        println("-".repeat(80))
        println("  STUBBED SUBROUTINES (need implementation)")
        println("-".repeat(80))
        for (status in stubbedItems.sortedBy { it.subroutine.lineNumber }) {
            println("    - %-35s in %s".format(status.subroutine.name, status.kotlinFile ?: "?"))
        }
    }

    println()
    println("=".repeat(80))
    println("  Overall: $translatedOrStubbed / $total subroutines addressed (${pct(translatedOrStubbed, total)})")
    println("           $fullTranslated fully translated, $stubs stubbed, $untranslated remaining")
    println("=".repeat(80))
}

private fun isDataTableByName(name: String): Boolean {
    val lower = name.lowercase()
    return lower.contains("data") || lower.contains("table") || lower.contains("offset") ||
        lower.contains("template") || lower.contains("palette") ||
        (lower.contains("metatile") && lower.contains("graphic")) ||
        lower.endsWith("_low") || lower.endsWith("_high")
}

private fun pct(n: Int, total: Int): String {
    if (total == 0) return "0.0%"
    return "%.1f%%".format(n.toDouble() / total * 100)
}

private fun buildProgressBar(full: Int, stubs: Int, remaining: Int, width: Int): String {
    val total = full + stubs + remaining
    if (total == 0) return "[]"
    val fullWidth = (full.toDouble() / total * width).toInt()
    val stubWidth = (stubs.toDouble() / total * width).toInt()
    val remWidth = width - fullWidth - stubWidth
    return "[" + "#".repeat(fullWidth) + ".".repeat(stubWidth) + " ".repeat(remWidth) + "]"
}

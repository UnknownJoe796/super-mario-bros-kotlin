// by Claude - Sound engine data tables
package com.ivieleague.smbtranslation

/**
 * Music header: describes a song section's data layout.
 * @param lengthOffset offset into MusicLengthLookupTbl (selects tempo)
 * @param dataAddrLow low byte of NES ROM address for song data
 * @param dataAddrHigh high byte of NES ROM address for song data
 * @param triangleOffset byte offset from data address to triangle channel data
 * @param square1Offset byte offset from data address to square 1 channel data
 * @param noiseOffset byte offset from data address to noise channel data (0 = no noise)
 */
data class MusicHeader(
    val lengthOffset: Int,
    val dataAddrLow: Int,
    val dataAddrHigh: Int,
    val triangleOffset: Int,
    val square1Offset: Int,
    val noiseOffset: Int = 0
)

/**
 * ROM-resident sound engine data tables extracted from smbdism.asm.
 *
 * The NES sound engine addresses music data via 16-bit pointers stored in
 * ram.musicData / ram.musicDataHigh ($F5/$F6). Each music header provides a
 * base address and per-channel offsets into a contiguous block of song data.
 *
 * Song data is stored here as a flat ByteArray mapped by NES address.
 * Use [readMusicData] to look up bytes the same way the NES engine does.
 */
object SoundData {

    // -----------------------------------------------------------------------
    // Music header offset table (49 bytes)
    // Indexes into musicHeaders array. The NES stores byte offsets into the
    // flattened MusicHeaderData; here we store direct indices.
    //
    // Layout: [0..7] = event music, [8..15] = area music,
    //         [16..48] = ground level music section sequence
    // -----------------------------------------------------------------------

    //> MusicHeaderData:
    //> .db DeathMusHdr-MHD            ;event music
    //> .db GameOverMusHdr-MHD
    //> .db VictoryMusHdr-MHD
    //> .db WinCastleMusHdr-MHD
    //> .db GameOverMusHdr-MHD
    //> .db EndOfLevelMusHdr-MHD
    //> .db TimeRunningOutHdr-MHD
    //> .db SilenceHdr-MHD
    //>
    //> .db GroundLevelPart1Hdr-MHD    ;area music
    //> .db WaterMusHdr-MHD
    //> .db UndergroundMusHdr-MHD
    //> .db CastleMusHdr-MHD
    //> .db Star_CloudHdr-MHD
    //> .db GroundLevelLeadInHdr-MHD
    //> .db Star_CloudHdr-MHD
    //> .db SilenceHdr-MHD
    //>
    //> .db GroundLevelLeadInHdr-MHD   ;ground level music layout
    //> .db GroundLevelPart1Hdr-MHD, GroundLevelPart1Hdr-MHD
    //> .db GroundLevelPart2AHdr-MHD, GroundLevelPart2BHdr-MHD, GroundLevelPart2AHdr-MHD, GroundLevelPart2CHdr-MHD
    //> .db GroundLevelPart2AHdr-MHD, GroundLevelPart2BHdr-MHD, GroundLevelPart2AHdr-MHD, GroundLevelPart2CHdr-MHD
    //> .db GroundLevelPart3AHdr-MHD, GroundLevelPart3BHdr-MHD, GroundLevelPart3AHdr-MHD, GroundLevelLeadInHdr-MHD
    //> .db GroundLevelPart4AHdr-MHD, GroundLevelPart4BHdr-MHD, GroundLevelPart4AHdr-MHD, GroundLevelPart4CHdr-MHD
    //> .db GroundLevelPart4AHdr-MHD, GroundLevelPart4BHdr-MHD, GroundLevelPart4AHdr-MHD, GroundLevelPart4CHdr-MHD
    //> .db GroundLevelPart3AHdr-MHD, GroundLevelPart3BHdr-MHD, GroundLevelPart3AHdr-MHD, GroundLevelLeadInHdr-MHD
    //> .db GroundLevelPart4AHdr-MHD, GroundLevelPart4BHdr-MHD, GroundLevelPart4AHdr-MHD, GroundLevelPart4CHdr-MHD
    private val musicHeaderOffsetData = intArrayOf(
        0xa5, 0x59, 0x54, 0x64, 0x59, 0x3c, 0x31, 0x4b,
        0x69, 0x5e, 0x46, 0x4f, 0x36, 0x8d, 0x36, 0x4b,
        0x8d, 0x69, 0x69, 0x6f, 0x75, 0x6f, 0x7b, 0x6f,
        0x75, 0x6f, 0x7b, 0x81, 0x87, 0x81, 0x8d, 0x69,
        0x69, 0x93, 0x99, 0x93, 0x9f, 0x93, 0x99, 0x93,
        0x9f, 0x81, 0x87, 0x81, 0x8d, 0x93, 0x99, 0x93,
        0x9f
    )

    // -----------------------------------------------------------------------
    // Music headers
    // -----------------------------------------------------------------------

    // MHD base address = $F90D (start of MusicHeaderData in NES ROM)
    // Header byte offset = musicHeaderOffsetData[i]
    // Each header is 5 bytes (secondary music) or 6 bytes (area music with noise)

    // Map from byte offset (as stored in musicHeaderOffsetData) to MusicHeader
    private val headersByOffset: Map<Int, MusicHeader> = mapOf(
        // NES always reads 6 bytes per header regardless of declared size.
        // Short headers overflow into the next header's bytes.
        //> TimeRunningOutHdr: .db $08, <TimeRunOutMusData, >TimeRunOutMusData, $27, $18
        0x31 to MusicHeader(0x08, 0x72, 0xFC, 0x27, 0x18, 0x20),  // byte 5 = Star_CloudHdr[0]
        //> Star_CloudHdr: .db $20, <Star_CloudMData, >Star_CloudMData, $2e, $1a, $40
        0x36 to MusicHeader(0x20, 0xB8, 0xF9, 0x2E, 0x1A, 0x40),
        //> EndOfLevelMusHdr: .db $20, <WinLevelMusData, >WinLevelMusData, $3d, $21
        0x3c to MusicHeader(0x20, 0xB0, 0xFC, 0x3D, 0x21, 0x20),  // byte 5 = ResidualHeaderData[0]
        //> ResidualHeaderData: .db $20, $c4, $fc, $3f, $1d
        0x41 to MusicHeader(0x20, 0xC4, 0xFC, 0x3F, 0x1D, 0x18),  // byte 5 = UndergroundMusHdr[0]
        //> UndergroundMusHdr: .db $18, <UndergroundMusData, >UndergroundMusData, $00, $00
        0x46 to MusicHeader(0x18, 0x11, 0xFD, 0x00, 0x00, 0x08),  // byte 5 = SilenceHdr[0]
        //> SilenceHdr: .db $08, <SilenceData, >SilenceData, $00 (only 4 bytes!)
        0x4b to MusicHeader(0x08, 0x1C, 0xFA, 0x00, 0x00, 0xA4),  // bytes 4-5 = CastleMusHdr[0-1]
        //> CastleMusHdr: .db $00, <CastleMusData, >CastleMusData, $93, $62
        0x4f to MusicHeader(0x00, 0xA4, 0xFB, 0x93, 0x62, 0x10),  // byte 5 = VictoryMusHdr[0]
        //> VictoryMusHdr: .db $10, <VictoryMusData, >VictoryMusData, $24, $14
        0x54 to MusicHeader(0x10, 0xC8, 0xFE, 0x24, 0x14, 0x18),  // byte 5 = GameOverMusHdr[0]
        //> GameOverMusHdr: .db $18, <GameOverMusData, >GameOverMusData, $1e, $14
        0x59 to MusicHeader(0x18, 0x45, 0xFC, 0x1E, 0x14, 0x08),  // byte 5 = WaterMusHdr[0]
        //> WaterMusHdr: .db $08, <WaterMusData, >WaterMusData, $a0, $70, $68
        0x5e to MusicHeader(0x08, 0x52, 0xFD, 0xA0, 0x70, 0x68),
        //> WinCastleMusHdr: .db $08, <EndOfCastleMusData, >EndOfCastleMusData, $4c, $24
        0x64 to MusicHeader(0x08, 0x51, 0xFE, 0x4C, 0x24, 0x18),  // byte 5 = GroundLevelPart1Hdr[0]
        //> GroundLevelPart1Hdr: .db $18, <GroundM_P1Data, >GroundM_P1Data, $2d, $1c, $b8
        0x69 to MusicHeader(0x18, 0x01, 0xFA, 0x2D, 0x1C, 0xB8),
        //> GroundLevelPart2AHdr: .db $18, <GroundM_P2AData, >GroundM_P2AData, $20, $12, $70
        0x6f to MusicHeader(0x18, 0x49, 0xFA, 0x20, 0x12, 0x70),
        //> GroundLevelPart2BHdr: .db $18, <GroundM_P2BData, >GroundM_P2BData, $1b, $10, $44
        0x75 to MusicHeader(0x18, 0x75, 0xFA, 0x1B, 0x10, 0x44),
        //> GroundLevelPart2CHdr: .db $18, <GroundM_P2CData, >GroundM_P2CData, $11, $0a, $1c
        0x7b to MusicHeader(0x18, 0x9D, 0xFA, 0x11, 0x0A, 0x1C),
        //> GroundLevelPart3AHdr: .db $18, <GroundM_P3AData, >GroundM_P3AData, $2d, $10, $58
        0x81 to MusicHeader(0x18, 0xC2, 0xFA, 0x2D, 0x10, 0x58),
        //> GroundLevelPart3BHdr: .db $18, <GroundM_P3BData, >GroundM_P3BData, $14, $0d, $3f
        0x87 to MusicHeader(0x18, 0xDB, 0xFA, 0x14, 0x0D, 0x3F),
        //> GroundLevelLeadInHdr: .db $18, <GroundMLdInData, >GroundMLdInData, $15, $0d, $21
        0x8d to MusicHeader(0x18, 0xF9, 0xFA, 0x15, 0x0D, 0x21),
        //> GroundLevelPart4AHdr: .db $18, <GroundM_P4AData, >GroundM_P4AData, $18, $10, $7a
        0x93 to MusicHeader(0x18, 0x25, 0xFB, 0x18, 0x10, 0x7A),
        //> GroundLevelPart4BHdr: .db $18, <GroundM_P4BData, >GroundM_P4BData, $19, $0f, $54
        0x99 to MusicHeader(0x18, 0x4B, 0xFB, 0x19, 0x0F, 0x54),
        //> GroundLevelPart4CHdr: .db $18, <GroundM_P4CData, >GroundM_P4CData, $1e, $12, $2b
        0x9f to MusicHeader(0x18, 0x74, 0xFB, 0x1E, 0x12, 0x2B),
        //> DeathMusHdr: .db $18, <DeathMusData, >DeathMusData, $1e, $0f, $2d
        0xa5 to MusicHeader(0x18, 0x72, 0xFB, 0x1E, 0x0F, 0x2D),
    )
    //> ;header format is as follows:
    //> ;1 byte - length byte offset
    //> ;2 bytes -  music data address
    //> ;1 byte - triangle data offset
    //> ;1 byte - square 1 data offset
    //> ;1 byte - noise data offset (not used by secondary music)

    // SMB2J header offset table: differs from SMB1 at index 2 (GameOver instead of Victory)
    // and ground music layout at indices 32-33 (GroundLevelPart1 x2 instead of Part4A repeat)
    //> (sm2main) MusicHeaderData:
    private val musicHeaderOffsetData_SMB2J = intArrayOf(
        0xa5, 0x59, 0x59, 0x64, 0x59, 0x3c, 0x31, 0x4b,  // event: [2]=GameOver not Victory
        0x69, 0x5e, 0x46, 0x4f, 0x36, 0x8d, 0x36, 0x4b,  // area: same
        0x8d, 0x69, 0x69, 0x6f, 0x75, 0x6f, 0x7b, 0x6f,  // ground layout: same up to index 31
        0x75, 0x6f, 0x7b, 0x81, 0x87, 0x81, 0x8d,
        0x69, 0x69,                                         // indices 32-33: Part1 x2 (SMB2J specific)
        0x93, 0x99, 0x93, 0x9f, 0x93, 0x99, 0x93,
        0x9f, 0x81, 0x87, 0x81, 0x8d, 0x93, 0x99, 0x93,
        0x9f
    )

    // SMB2J headers: same structure, addresses shifted by -$1FC9
    private val ADDR_DELTA = MUSIC_DATA_BASE_SMB1 - MUSIC_DATA_BASE_SMB2J  // $1FC9
    private val headersByOffset_SMB2J: Map<Int, MusicHeader> = headersByOffset.mapValues { (_, h) ->
        val origAddr = ((h.dataAddrHigh and 0xFF) shl 8) or (h.dataAddrLow and 0xFF)
        val smb2jAddr = origAddr - ADDR_DELTA
        h.copy(dataAddrLow = smb2jAddr and 0xFF, dataAddrHigh = (smb2jAddr shr 8) and 0xFF)
    }

    /** Look up a music header by index into the offset table. */
    fun getMusicHeader(index: Int, smb2j: Boolean = false): MusicHeader {
        val table = if (smb2j) musicHeaderOffsetData_SMB2J else musicHeaderOffsetData
        val headers = if (smb2j) headersByOffset_SMB2J else headersByOffset
        val offset = table[index]
        return headers[offset]
            ?: throw IllegalArgumentException("No music header at offset 0x${offset.toString(16)}")
    }

    // -----------------------------------------------------------------------
    // Music song data ($F9B8 - $FEFF, 1352 bytes)
    // All song data stored contiguously as it appears in ROM.
    // -----------------------------------------------------------------------

    private const val MUSIC_DATA_BASE_SMB1 = 0xF9B8
    // SMB2J/FDS loads SM2MAIN at $6000. Music data within SM2MAIN is at the same
    // relative offset but at a different absolute address: $D9EF instead of $F9B8.
    private const val MUSIC_DATA_BASE_SMB2J = 0xD9EF

    //> ;MUSIC DATA
    //> ;square 2/triangle format
    //> ;d7 - length byte flag (0-note, 1-length)
    //> ;if d7 is set to 0 and d6-d0 is nonzero:
    //> ;d6-d0 - note offset in frequency look-up table (must be even)
    //> ;if d7 is set to 1:
    //> ;d6-d3 - unused
    //> ;d2-d0 - length offset in length look-up table
    //> ;value of $00 in square 2 data is used as null terminator, affects all sound channels
    //> ;value of $00 in triangle data causes routine to skip note
    //>
    //> ;square 1 format
    //> ;d7-d6, d0 - length offset in length look-up table (bit order is d0,d7,d6)
    //> ;d5-d1 - note offset in frequency look-up table
    //> ;value of $00 in square 1 data is flag alternate control reg data to be loaded
    //>
    //> ;noise format
    //> ;d7-d6, d0 - length offset in length look-up table (bit order is d0,d7,d6)
    //> ;d5-d4 - beat type (0 - rest, 1 - short, 2 - strong, 3 - long)
    //> ;d3-d1 - unused
    //> ;value of $00 in noise data is used as null terminator, affects only noise
    //>
    //> ;all music data is organized into sections (unless otherwise stated):
    //> ;square 2, square 1, triangle, noise
    private val musicDataBytes = byteArrayOf(
        //> Star_CloudMData:
        //> Star_CloudMData ($F9B8):
        0x84.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x82.toByte(), 0x04.toByte(), 0x2C.toByte(), 0x04.toByte(),
        0x85.toByte(), 0x2C.toByte(), 0x84.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x2A.toByte(), 0x2A.toByte(), 0x2A.toByte(),
        0x82.toByte(), 0x04.toByte(), 0x2A.toByte(), 0x04.toByte(), 0x85.toByte(), 0x2A.toByte(), 0x84.toByte(), 0x2A.toByte(),
        0x2A.toByte(), 0x00.toByte(),
        // sq1
        0x1F.toByte(), 0x1F.toByte(), 0x1F.toByte(), 0x98.toByte(), 0x1F.toByte(), 0x1F.toByte(),
        0x98.toByte(), 0x9E.toByte(), 0x98.toByte(), 0x1F.toByte(), 0x1D.toByte(), 0x1D.toByte(), 0x1D.toByte(), 0x94.toByte(),
        0x1D.toByte(), 0x1D.toByte(), 0x94.toByte(), 0x9C.toByte(), 0x94.toByte(), 0x1D.toByte(),
        // tri
        0x86.toByte(), 0x18.toByte(),
        0x85.toByte(), 0x26.toByte(), 0x30.toByte(), 0x84.toByte(), 0x04.toByte(), 0x26.toByte(), 0x30.toByte(), 0x86.toByte(),
        0x14.toByte(), 0x85.toByte(), 0x22.toByte(), 0x2C.toByte(), 0x84.toByte(), 0x04.toByte(), 0x22.toByte(), 0x2C.toByte(),
        // noise
        0x21.toByte(), 0xD0.toByte(), 0xC4.toByte(), 0xD0.toByte(), 0x31.toByte(), 0xD0.toByte(), 0xC4.toByte(), 0xD0.toByte(),
        0x00.toByte(),

        //> GroundM_P1Data ($FA01):
        0x85.toByte(), 0x2C.toByte(), 0x22.toByte(), 0x1C.toByte(), 0x84.toByte(), 0x26.toByte(), 0x2A.toByte(), 0x82.toByte(),
        0x28.toByte(), 0x26.toByte(), 0x04.toByte(), 0x87.toByte(), 0x22.toByte(), 0x34.toByte(), 0x3A.toByte(), 0x82.toByte(),
        0x40.toByte(), 0x04.toByte(), 0x36.toByte(), 0x84.toByte(), 0x3A.toByte(), 0x34.toByte(), 0x82.toByte(), 0x2C.toByte(),
        0x30.toByte(), 0x85.toByte(), 0x2A.toByte(),

        //> SilenceData ($FA1C):
        0x00.toByte(),

        // sq1 data for GroundM_P1
        0x5D.toByte(), 0x55.toByte(), 0x4D.toByte(), 0x15.toByte(), 0x19.toByte(), 0x96.toByte(), 0x15.toByte(), 0xD5.toByte(),
        0xE3.toByte(), 0xEB.toByte(), 0x2D.toByte(), 0xA6.toByte(), 0x2B.toByte(), 0x27.toByte(), 0x9C.toByte(), 0x9E.toByte(),
        0x59.toByte(),
        // tri data for GroundM_P1
        0x85.toByte(), 0x22.toByte(), 0x1C.toByte(), 0x14.toByte(), 0x84.toByte(), 0x1E.toByte(), 0x22.toByte(), 0x82.toByte(),
        0x20.toByte(), 0x1E.toByte(), 0x04.toByte(), 0x87.toByte(), 0x1C.toByte(), 0x2C.toByte(), 0x34.toByte(), 0x82.toByte(),
        0x36.toByte(), 0x04.toByte(), 0x30.toByte(), 0x34.toByte(), 0x04.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x26.toByte(),
        0x2A.toByte(), 0x85.toByte(), 0x22.toByte(),

        //> GroundM_P2AData ($FA49):
        0x84.toByte(), 0x04.toByte(), 0x82.toByte(), 0x3A.toByte(), 0x38.toByte(), 0x36.toByte(), 0x32.toByte(), 0x04.toByte(),
        0x34.toByte(), 0x04.toByte(), 0x24.toByte(), 0x26.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x26.toByte(), 0x2C.toByte(),
        0x30.toByte(), 0x00.toByte(),
        // sq1
        0x05.toByte(), 0xB4.toByte(), 0xB2.toByte(), 0xB0.toByte(), 0x2B.toByte(), 0xAC.toByte(), 0x84.toByte(),
        0x9C.toByte(), 0x9E.toByte(), 0xA2.toByte(), 0x84.toByte(), 0x94.toByte(), 0x9C.toByte(), 0x9E.toByte(),
        // tri
        0x85.toByte(), 0x14.toByte(), 0x22.toByte(), 0x84.toByte(), 0x2C.toByte(), 0x85.toByte(), 0x1E.toByte(),
        0x82.toByte(), 0x2C.toByte(), 0x84.toByte(), 0x2C.toByte(), 0x1E.toByte(),

        //> GroundM_P2BData:
        //> GroundM_P2BData ($FA75):
        0x84.toByte(), 0x04.toByte(), 0x82.toByte(), 0x3A.toByte(), 0x38.toByte(), 0x36.toByte(), 0x32.toByte(), 0x04.toByte(),
        0x34.toByte(), 0x04.toByte(), 0x64.toByte(), 0x04.toByte(), 0x64.toByte(), 0x86.toByte(), 0x64.toByte(), 0x00.toByte(),
        // sq1
        0x05.toByte(), 0xB4.toByte(), 0xB2.toByte(), 0xB0.toByte(), 0x2B.toByte(), 0xAC.toByte(), 0x84.toByte(),
        0x37.toByte(), 0xB6.toByte(), 0xB6.toByte(), 0x45.toByte(),
        // tri
        0x85.toByte(), 0x14.toByte(), 0x1C.toByte(), 0x82.toByte(), 0x22.toByte(), 0x84.toByte(), 0x2C.toByte(),
        0x4E.toByte(), 0x82.toByte(), 0x4E.toByte(), 0x84.toByte(), 0x4E.toByte(), 0x22.toByte(),

        //> GroundM_P2CData:
        //> GroundM_P2CData ($FA9D):
        0x84.toByte(), 0x04.toByte(), 0x85.toByte(), 0x32.toByte(), 0x85.toByte(), 0x30.toByte(), 0x86.toByte(), 0x2C.toByte(),
        0x04.toByte(), 0x00.toByte(),
        // sq1
        0x05.toByte(), 0xA4.toByte(), 0x05.toByte(), 0x9E.toByte(), 0x05.toByte(), 0x9D.toByte(), 0x85.toByte(),
        // tri
        0x84.toByte(), 0x14.toByte(), 0x85.toByte(), 0x24.toByte(), 0x28.toByte(), 0x2C.toByte(), 0x82.toByte(),
        0x22.toByte(), 0x84.toByte(), 0x22.toByte(), 0x14.toByte(),
        // noise (shared with Star_Cloud)
        0x21.toByte(), 0xD0.toByte(), 0xC4.toByte(), 0xD0.toByte(), 0x31.toByte(), 0xD0.toByte(), 0xC4.toByte(), 0xD0.toByte(),
        0x00.toByte(),

        //> GroundM_P3AData:
        //> GroundM_P3AData ($FAC2):
        0x82.toByte(), 0x2C.toByte(), 0x84.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x82.toByte(), 0x2C.toByte(), 0x30.toByte(),
        0x04.toByte(), 0x34.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x26.toByte(), 0x86.toByte(), 0x22.toByte(), 0x00.toByte(),
        // sq1
        0xA4.toByte(), 0x25.toByte(), 0x25.toByte(), 0xA4.toByte(), 0x29.toByte(), 0xA2.toByte(), 0x1D.toByte(), 0x9C.toByte(),
        0x95.toByte(),

        //> GroundM_P3BData:
        //> GroundM_P3BData ($FADB):
        0x82.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x2C.toByte(), 0x30.toByte(),
        0x85.toByte(), 0x34.toByte(), 0x04.toByte(), 0x04.toByte(), 0x00.toByte(),
        // sq1
        0xA4.toByte(), 0x25.toByte(), 0x25.toByte(), 0xA4.toByte(), 0xA8.toByte(), 0x63.toByte(), 0x04.toByte(),
        //> ;triangle data used by both sections of third part
        // tri (shared by P3A and P3B)
        0x85.toByte(), 0x0E.toByte(), 0x1A.toByte(), 0x84.toByte(), 0x24.toByte(), 0x85.toByte(), 0x22.toByte(), 0x14.toByte(),
        0x84.toByte(), 0x0C.toByte(),

        //> GroundMLdInData:
        //> GroundMLdInData ($FAF9):
        0x82.toByte(), 0x34.toByte(), 0x84.toByte(), 0x34.toByte(), 0x34.toByte(), 0x82.toByte(), 0x2C.toByte(), 0x84.toByte(),
        0x34.toByte(), 0x86.toByte(), 0x3A.toByte(), 0x04.toByte(), 0x00.toByte(),
        // sq1
        0xA0.toByte(), 0x21.toByte(), 0x21.toByte(), 0xA0.toByte(), 0x21.toByte(), 0x2B.toByte(), 0x05.toByte(), 0xA3.toByte(),
        // tri
        0x82.toByte(), 0x18.toByte(), 0x84.toByte(), 0x18.toByte(), 0x18.toByte(), 0x82.toByte(), 0x18.toByte(), 0x18.toByte(),
        0x04.toByte(), 0x86.toByte(), 0x3A.toByte(), 0x22.toByte(),
        //> ;noise data used by lead-in and third part sections
        // noise (shared by lead-in and P3)
        0x31.toByte(), 0x90.toByte(), 0x31.toByte(), 0x90.toByte(), 0x31.toByte(), 0x71.toByte(), 0x31.toByte(), 0x90.toByte(),
        0x90.toByte(), 0x90.toByte(), 0x00.toByte(),

        //> GroundM_P4AData:
        //> GroundM_P4AData ($FB25):
        0x82.toByte(), 0x34.toByte(), 0x84.toByte(), 0x2C.toByte(), 0x85.toByte(), 0x22.toByte(), 0x84.toByte(), 0x24.toByte(),
        0x82.toByte(), 0x26.toByte(), 0x36.toByte(), 0x04.toByte(), 0x36.toByte(), 0x86.toByte(), 0x26.toByte(), 0x00.toByte(),
        // sq1
        0xAC.toByte(), 0x27.toByte(), 0x5D.toByte(), 0x1D.toByte(), 0x9E.toByte(), 0x2D.toByte(), 0xAC.toByte(), 0x9F.toByte(),
        // tri
        0x85.toByte(), 0x14.toByte(), 0x82.toByte(), 0x20.toByte(), 0x84.toByte(), 0x22.toByte(), 0x2C.toByte(),
        0x1E.toByte(), 0x1E.toByte(), 0x82.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x1E.toByte(), 0x04.toByte(),

        //> GroundM_P4BData:
        //> GroundM_P4BData ($FB4B):
        0x87.toByte(), 0x2A.toByte(), 0x40.toByte(), 0x40.toByte(), 0x40.toByte(), 0x3A.toByte(), 0x36.toByte(),
        0x82.toByte(), 0x34.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x26.toByte(), 0x86.toByte(), 0x22.toByte(), 0x00.toByte(),
        // sq1
        0xE3.toByte(), 0xF7.toByte(), 0xF7.toByte(), 0xF7.toByte(), 0xF5.toByte(), 0xF1.toByte(), 0xAC.toByte(), 0x27.toByte(),
        0x9E.toByte(), 0x9D.toByte(),
        // tri
        0x85.toByte(), 0x18.toByte(), 0x82.toByte(), 0x1E.toByte(), 0x84.toByte(), 0x22.toByte(), 0x2A.toByte(),
        0x22.toByte(), 0x22.toByte(), 0x82.toByte(), 0x2C.toByte(), 0x2C.toByte(), 0x22.toByte(), 0x04.toByte(),

        //> DeathMusData:
        //> DeathMusData ($FB72):
        //> .db $86, $04 ;death music share data with fourth part c of ground level music
        0x86.toByte(), 0x04.toByte(),

        //> GroundM_P4CData:
        //> GroundM_P4CData ($FB74):
        0x82.toByte(), 0x2A.toByte(), 0x36.toByte(), 0x04.toByte(), 0x36.toByte(), 0x87.toByte(), 0x36.toByte(), 0x34.toByte(),
        0x30.toByte(), 0x86.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x00.toByte(),
        // death music sq1 only
        0x00.toByte(), 0x68.toByte(), 0x6A.toByte(), 0x6C.toByte(), 0x45.toByte(),
        // sq1 for P4C/death
        0xA2.toByte(), 0x31.toByte(), 0xB0.toByte(), 0xF1.toByte(), 0xED.toByte(), 0xEB.toByte(), 0xA2.toByte(), 0x1D.toByte(),
        0x9C.toByte(), 0x95.toByte(),
        //> .db $86, $04 ;death music only
        // death music tri preamble
        0x86.toByte(), 0x04.toByte(),
        // tri for P4C/death
        0x85.toByte(), 0x22.toByte(), 0x82.toByte(), 0x22.toByte(), 0x87.toByte(), 0x22.toByte(), 0x26.toByte(), 0x2A.toByte(),
        0x84.toByte(), 0x2C.toByte(), 0x22.toByte(), 0x86.toByte(), 0x14.toByte(),
        //> ;noise data used by fourth part sections
        // noise (shared by P4 sections)
        0x51.toByte(), 0x90.toByte(), 0x31.toByte(), 0x11.toByte(), 0x00.toByte(),

        //> CastleMusData:
        //> CastleMusData ($FBA4):
        0x80.toByte(), 0x22.toByte(), 0x28.toByte(), 0x22.toByte(), 0x26.toByte(), 0x22.toByte(), 0x24.toByte(), 0x22.toByte(),
        0x26.toByte(), 0x22.toByte(), 0x28.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x22.toByte(), 0x28.toByte(), 0x22.toByte(),
        0x26.toByte(), 0x22.toByte(), 0x28.toByte(), 0x22.toByte(), 0x26.toByte(), 0x22.toByte(), 0x24.toByte(), 0x22.toByte(),
        0x26.toByte(), 0x22.toByte(), 0x28.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x22.toByte(), 0x28.toByte(), 0x22.toByte(),
        0x26.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(), 0x24.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(),
        0x28.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(), 0x28.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(),
        0x24.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(), 0x24.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(),
        0x28.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(), 0x28.toByte(), 0x20.toByte(), 0x26.toByte(), 0x20.toByte(),
        0x24.toByte(), 0x28.toByte(), 0x30.toByte(), 0x28.toByte(), 0x32.toByte(), 0x28.toByte(), 0x30.toByte(), 0x28.toByte(),
        0x2E.toByte(), 0x28.toByte(), 0x30.toByte(), 0x28.toByte(), 0x2E.toByte(), 0x28.toByte(), 0x2C.toByte(), 0x28.toByte(),
        0x2E.toByte(), 0x28.toByte(), 0x30.toByte(), 0x28.toByte(), 0x32.toByte(), 0x28.toByte(), 0x30.toByte(), 0x28.toByte(),
        0x2E.toByte(), 0x28.toByte(), 0x30.toByte(), 0x28.toByte(), 0x2E.toByte(), 0x28.toByte(), 0x2C.toByte(), 0x28.toByte(),
        0x2E.toByte(), 0x00.toByte(),
        // sq1
        0x04.toByte(), 0x70.toByte(), 0x6E.toByte(), 0x6C.toByte(), 0x6E.toByte(), 0x70.toByte(), 0x72.toByte(), 0x70.toByte(),
        0x6E.toByte(), 0x70.toByte(), 0x6E.toByte(), 0x6C.toByte(), 0x6E.toByte(), 0x70.toByte(), 0x72.toByte(), 0x70.toByte(),
        0x6E.toByte(), 0x6E.toByte(), 0x6C.toByte(), 0x6E.toByte(), 0x70.toByte(), 0x6E.toByte(), 0x70.toByte(), 0x6E.toByte(),
        0x6C.toByte(), 0x6E.toByte(), 0x6C.toByte(), 0x6E.toByte(), 0x70.toByte(), 0x6E.toByte(), 0x70.toByte(), 0x6E.toByte(),
        0x6C.toByte(), 0x76.toByte(), 0x78.toByte(), 0x76.toByte(), 0x74.toByte(), 0x76.toByte(), 0x74.toByte(), 0x72.toByte(),
        0x74.toByte(), 0x76.toByte(), 0x78.toByte(), 0x76.toByte(), 0x74.toByte(), 0x76.toByte(), 0x74.toByte(), 0x72.toByte(),
        0x74.toByte(),
        // tri
        0x84.toByte(), 0x1A.toByte(), 0x83.toByte(), 0x18.toByte(), 0x20.toByte(), 0x84.toByte(), 0x1E.toByte(), 0x83.toByte(),
        0x1C.toByte(), 0x28.toByte(), 0x26.toByte(), 0x1C.toByte(), 0x1A.toByte(), 0x1C.toByte(),

        //> GameOverMusData:
        //> GameOverMusData ($FC45):
        0x82.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x04.toByte(), 0x22.toByte(), 0x04.toByte(), 0x04.toByte(), 0x84.toByte(),
        0x1C.toByte(), 0x87.toByte(), 0x26.toByte(), 0x2A.toByte(), 0x26.toByte(), 0x84.toByte(), 0x24.toByte(), 0x28.toByte(),
        0x24.toByte(), 0x80.toByte(), 0x22.toByte(), 0x00.toByte(),
        // sq1
        0x9C.toByte(), 0x05.toByte(), 0x94.toByte(), 0x05.toByte(), 0x0D.toByte(), 0x9F.toByte(), 0x1E.toByte(), 0x9C.toByte(),
        0x98.toByte(), 0x9D.toByte(),
        // tri
        0x82.toByte(), 0x22.toByte(), 0x04.toByte(), 0x04.toByte(), 0x1C.toByte(), 0x04.toByte(), 0x04.toByte(), 0x84.toByte(),
        0x14.toByte(), 0x86.toByte(), 0x1E.toByte(), 0x80.toByte(), 0x16.toByte(), 0x80.toByte(), 0x14.toByte(),

        //> TimeRunOutMusData:
        //> TimeRunOutMusData ($FC72):
        0x81.toByte(), 0x1C.toByte(), 0x30.toByte(), 0x04.toByte(), 0x30.toByte(), 0x30.toByte(), 0x04.toByte(), 0x1E.toByte(),
        0x32.toByte(), 0x04.toByte(), 0x32.toByte(), 0x32.toByte(), 0x04.toByte(), 0x20.toByte(), 0x34.toByte(), 0x04.toByte(),
        0x34.toByte(), 0x34.toByte(), 0x04.toByte(), 0x36.toByte(), 0x04.toByte(), 0x84.toByte(), 0x36.toByte(), 0x00.toByte(),
        // sq1
        0x46.toByte(), 0xA4.toByte(), 0x64.toByte(), 0xA4.toByte(), 0x48.toByte(), 0xA6.toByte(), 0x66.toByte(), 0xA6.toByte(),
        0x4A.toByte(), 0xA8.toByte(), 0x68.toByte(), 0xA8.toByte(), 0x6A.toByte(), 0x44.toByte(), 0x2B.toByte(),
        // tri
        0x81.toByte(), 0x2A.toByte(), 0x42.toByte(), 0x04.toByte(), 0x42.toByte(), 0x42.toByte(), 0x04.toByte(), 0x2C.toByte(),
        0x64.toByte(), 0x04.toByte(), 0x64.toByte(), 0x64.toByte(), 0x04.toByte(), 0x2E.toByte(), 0x46.toByte(), 0x04.toByte(),
        0x46.toByte(), 0x46.toByte(), 0x04.toByte(), 0x22.toByte(), 0x04.toByte(), 0x84.toByte(), 0x22.toByte(),

        //> WinLevelMusData:
        //> WinLevelMusData ($FCB0):
        0x87.toByte(), 0x04.toByte(), 0x06.toByte(), 0x0C.toByte(), 0x14.toByte(), 0x1C.toByte(), 0x22.toByte(), 0x86.toByte(),
        0x2C.toByte(), 0x22.toByte(), 0x87.toByte(), 0x04.toByte(), 0x60.toByte(), 0x0E.toByte(), 0x14.toByte(), 0x1A.toByte(),
        0x24.toByte(), 0x86.toByte(), 0x2C.toByte(), 0x24.toByte(), 0x87.toByte(), 0x04.toByte(), 0x08.toByte(), 0x10.toByte(),
        0x18.toByte(), 0x1E.toByte(), 0x28.toByte(), 0x86.toByte(), 0x30.toByte(), 0x30.toByte(), 0x80.toByte(), 0x64.toByte(),
        0x00.toByte(),
        // sq1
        0xCD.toByte(), 0xD5.toByte(), 0xDD.toByte(), 0xE3.toByte(), 0xED.toByte(), 0xF5.toByte(), 0xBB.toByte(),
        0xB5.toByte(), 0xCF.toByte(), 0xD5.toByte(), 0xDB.toByte(), 0xE5.toByte(), 0xED.toByte(), 0xF3.toByte(), 0xBD.toByte(),
        0xB3.toByte(), 0xD1.toByte(), 0xD9.toByte(), 0xDF.toByte(), 0xE9.toByte(), 0xF1.toByte(), 0xF7.toByte(), 0xBF.toByte(),
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x34.toByte(),
        0x00.toByte(), // unused byte
        // tri
        0x86.toByte(), 0x04.toByte(), 0x87.toByte(), 0x14.toByte(), 0x1C.toByte(), 0x22.toByte(), 0x86.toByte(), 0x34.toByte(),
        0x84.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x04.toByte(), 0x04.toByte(), 0x87.toByte(), 0x14.toByte(), 0x1A.toByte(),
        0x24.toByte(), 0x86.toByte(), 0x32.toByte(), 0x84.toByte(), 0x2C.toByte(), 0x04.toByte(), 0x86.toByte(), 0x04.toByte(),
        0x87.toByte(), 0x18.toByte(), 0x1E.toByte(), 0x28.toByte(), 0x86.toByte(), 0x36.toByte(), 0x87.toByte(), 0x30.toByte(),
        0x30.toByte(), 0x30.toByte(), 0x80.toByte(), 0x2C.toByte(),

        //> ;square 2 and triangle use the same data, square 1 is unused
        //> UndergroundMusData:
        //> UndergroundMusData ($FD11):
        0x82.toByte(), 0x14.toByte(), 0x2C.toByte(), 0x62.toByte(), 0x26.toByte(), 0x10.toByte(), 0x28.toByte(), 0x80.toByte(),
        0x04.toByte(), 0x82.toByte(), 0x14.toByte(), 0x2C.toByte(), 0x62.toByte(), 0x26.toByte(), 0x10.toByte(), 0x28.toByte(),
        0x80.toByte(), 0x04.toByte(), 0x82.toByte(), 0x08.toByte(), 0x1E.toByte(), 0x5E.toByte(), 0x18.toByte(), 0x60.toByte(),
        0x1A.toByte(), 0x80.toByte(), 0x04.toByte(), 0x82.toByte(), 0x08.toByte(), 0x1E.toByte(), 0x5E.toByte(), 0x18.toByte(),
        0x60.toByte(), 0x1A.toByte(), 0x86.toByte(), 0x04.toByte(), 0x83.toByte(), 0x1A.toByte(), 0x18.toByte(), 0x16.toByte(),
        0x84.toByte(), 0x14.toByte(), 0x1A.toByte(), 0x18.toByte(), 0x0E.toByte(), 0x0C.toByte(), 0x16.toByte(), 0x83.toByte(),
        0x14.toByte(), 0x20.toByte(), 0x1E.toByte(), 0x1C.toByte(), 0x28.toByte(), 0x26.toByte(), 0x87.toByte(), 0x24.toByte(),
        0x1A.toByte(), 0x12.toByte(), 0x10.toByte(), 0x62.toByte(), 0x0E.toByte(), 0x80.toByte(), 0x04.toByte(), 0x04.toByte(),
        0x00.toByte(),

        //> WaterMusData:
        //> ;noise data directly follows square 2 here unlike in other songs
        //> WaterMusData ($FD52):
        // noise data directly follows sq2 here
        0x82.toByte(), 0x18.toByte(), 0x1C.toByte(), 0x20.toByte(), 0x22.toByte(), 0x26.toByte(), 0x28.toByte(),
        0x81.toByte(), 0x2A.toByte(), 0x2A.toByte(), 0x2A.toByte(), 0x04.toByte(), 0x2A.toByte(), 0x04.toByte(), 0x83.toByte(),
        0x2A.toByte(), 0x82.toByte(), 0x22.toByte(), 0x86.toByte(), 0x34.toByte(), 0x32.toByte(), 0x34.toByte(), 0x81.toByte(),
        0x04.toByte(), 0x22.toByte(), 0x26.toByte(), 0x2A.toByte(), 0x2C.toByte(), 0x30.toByte(), 0x86.toByte(), 0x34.toByte(),
        0x83.toByte(), 0x32.toByte(), 0x82.toByte(), 0x36.toByte(), 0x84.toByte(), 0x34.toByte(), 0x85.toByte(), 0x04.toByte(),
        0x81.toByte(), 0x22.toByte(), 0x86.toByte(), 0x30.toByte(), 0x2E.toByte(), 0x30.toByte(), 0x81.toByte(), 0x04.toByte(),
        0x22.toByte(), 0x26.toByte(), 0x2A.toByte(), 0x2C.toByte(), 0x2E.toByte(), 0x86.toByte(), 0x30.toByte(), 0x83.toByte(),
        0x22.toByte(), 0x82.toByte(), 0x36.toByte(), 0x84.toByte(), 0x34.toByte(), 0x85.toByte(), 0x04.toByte(), 0x81.toByte(),
        0x22.toByte(), 0x86.toByte(), 0x3A.toByte(), 0x3A.toByte(), 0x3A.toByte(), 0x82.toByte(), 0x3A.toByte(), 0x81.toByte(),
        0x40.toByte(), 0x82.toByte(), 0x04.toByte(), 0x81.toByte(), 0x3A.toByte(), 0x86.toByte(), 0x36.toByte(), 0x36.toByte(),
        0x36.toByte(), 0x82.toByte(), 0x36.toByte(), 0x81.toByte(), 0x3A.toByte(), 0x82.toByte(), 0x04.toByte(), 0x81.toByte(),
        0x36.toByte(), 0x86.toByte(), 0x34.toByte(), 0x82.toByte(), 0x26.toByte(), 0x2A.toByte(), 0x36.toByte(), 0x81.toByte(),
        0x34.toByte(), 0x34.toByte(), 0x85.toByte(), 0x34.toByte(), 0x81.toByte(), 0x2A.toByte(), 0x86.toByte(), 0x2C.toByte(),
        0x00.toByte(),
        // noise (water)
        0x84.toByte(), 0x90.toByte(), 0xB0.toByte(), 0x84.toByte(), 0x50.toByte(), 0x50.toByte(), 0xB0.toByte(), 0x00.toByte(),
        // sq1
        0x98.toByte(), 0x96.toByte(), 0x94.toByte(), 0x92.toByte(), 0x94.toByte(), 0x96.toByte(), 0x58.toByte(), 0x58.toByte(),
        0x58.toByte(), 0x44.toByte(), 0x5C.toByte(), 0x44.toByte(), 0x9F.toByte(), 0xA3.toByte(), 0xA1.toByte(), 0xA3.toByte(),
        0x85.toByte(), 0xA3.toByte(), 0xE0.toByte(), 0xA6.toByte(), 0x23.toByte(), 0xC4.toByte(), 0x9F.toByte(), 0x9D.toByte(),
        0x9F.toByte(), 0x85.toByte(), 0x9F.toByte(), 0xD2.toByte(), 0xA6.toByte(), 0x23.toByte(), 0xC4.toByte(), 0xB5.toByte(),
        0xB1.toByte(), 0xAF.toByte(), 0x85.toByte(), 0xB1.toByte(), 0xAF.toByte(), 0xAD.toByte(), 0x85.toByte(), 0x95.toByte(),
        0x9E.toByte(), 0xA2.toByte(), 0xAA.toByte(), 0x6A.toByte(), 0x6A.toByte(), 0x6B.toByte(), 0x5E.toByte(), 0x9D.toByte(),
        // tri
        0x84.toByte(), 0x04.toByte(), 0x04.toByte(), 0x82.toByte(), 0x22.toByte(), 0x86.toByte(), 0x22.toByte(),
        0x82.toByte(), 0x14.toByte(), 0x22.toByte(), 0x2C.toByte(), 0x12.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x14.toByte(),
        0x22.toByte(), 0x2C.toByte(), 0x1C.toByte(), 0x22.toByte(), 0x2C.toByte(), 0x14.toByte(), 0x22.toByte(), 0x2C.toByte(),
        0x12.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x14.toByte(), 0x22.toByte(), 0x2C.toByte(), 0x1C.toByte(), 0x22.toByte(),
        0x2C.toByte(), 0x18.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x16.toByte(), 0x20.toByte(), 0x28.toByte(), 0x18.toByte(),
        0x22.toByte(), 0x2A.toByte(), 0x12.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x18.toByte(), 0x22.toByte(), 0x2A.toByte(),
        0x12.toByte(), 0x22.toByte(), 0x2A.toByte(), 0x14.toByte(), 0x22.toByte(), 0x2C.toByte(), 0x0C.toByte(), 0x22.toByte(),
        0x2C.toByte(), 0x14.toByte(), 0x22.toByte(), 0x34.toByte(), 0x12.toByte(), 0x22.toByte(), 0x30.toByte(), 0x10.toByte(),
        0x22.toByte(), 0x2E.toByte(), 0x16.toByte(), 0x22.toByte(), 0x34.toByte(), 0x18.toByte(), 0x26.toByte(), 0x36.toByte(),
        0x16.toByte(), 0x26.toByte(), 0x36.toByte(), 0x14.toByte(), 0x26.toByte(), 0x36.toByte(), 0x12.toByte(), 0x22.toByte(),
        0x36.toByte(), 0x5C.toByte(), 0x22.toByte(), 0x34.toByte(), 0x0C.toByte(), 0x22.toByte(), 0x22.toByte(), 0x81.toByte(),
        0x1E.toByte(), 0x1E.toByte(), 0x85.toByte(), 0x1E.toByte(), 0x81.toByte(), 0x12.toByte(), 0x86.toByte(), 0x14.toByte(),

        //> EndOfCastleMusData:
        //> EndOfCastleMusData ($FE51):
        0x81.toByte(), 0x2C.toByte(), 0x22.toByte(), 0x1C.toByte(), 0x2C.toByte(), 0x22.toByte(), 0x1C.toByte(), 0x85.toByte(),
        0x2C.toByte(), 0x04.toByte(), 0x81.toByte(), 0x2E.toByte(), 0x24.toByte(), 0x1E.toByte(), 0x2E.toByte(), 0x24.toByte(),
        0x1E.toByte(), 0x85.toByte(), 0x2E.toByte(), 0x04.toByte(), 0x81.toByte(), 0x32.toByte(), 0x28.toByte(), 0x22.toByte(),
        0x32.toByte(), 0x28.toByte(), 0x22.toByte(), 0x85.toByte(), 0x32.toByte(), 0x87.toByte(), 0x36.toByte(), 0x36.toByte(),
        0x36.toByte(), 0x84.toByte(), 0x3A.toByte(), 0x00.toByte(),
        // sq1
        0x5C.toByte(), 0x54.toByte(), 0x4C.toByte(), 0x5C.toByte(), 0x54.toByte(), 0x4C.toByte(),
        0x5C.toByte(), 0x1C.toByte(), 0x1C.toByte(), 0x5C.toByte(), 0x5C.toByte(), 0x5C.toByte(), 0x5C.toByte(),
        0x5E.toByte(), 0x56.toByte(), 0x4E.toByte(), 0x5E.toByte(), 0x56.toByte(), 0x4E.toByte(),
        0x5E.toByte(), 0x1E.toByte(), 0x1E.toByte(), 0x5E.toByte(), 0x5E.toByte(), 0x5E.toByte(), 0x5E.toByte(),
        0x62.toByte(), 0x5A.toByte(), 0x50.toByte(), 0x62.toByte(), 0x5A.toByte(), 0x50.toByte(),
        0x62.toByte(), 0x22.toByte(), 0x22.toByte(), 0x62.toByte(), 0xE7.toByte(), 0xE7.toByte(), 0xE7.toByte(), 0x2B.toByte(),
        // tri
        0x86.toByte(), 0x14.toByte(), 0x81.toByte(), 0x14.toByte(), 0x80.toByte(), 0x14.toByte(), 0x14.toByte(), 0x81.toByte(),
        0x14.toByte(), 0x14.toByte(), 0x14.toByte(), 0x14.toByte(),
        0x86.toByte(), 0x16.toByte(), 0x81.toByte(), 0x16.toByte(), 0x80.toByte(), 0x16.toByte(), 0x16.toByte(), 0x81.toByte(),
        0x16.toByte(), 0x16.toByte(), 0x16.toByte(), 0x16.toByte(),
        0x81.toByte(), 0x28.toByte(), 0x22.toByte(), 0x1A.toByte(), 0x28.toByte(), 0x22.toByte(), 0x1A.toByte(), 0x28.toByte(),
        0x80.toByte(), 0x28.toByte(), 0x28.toByte(), 0x81.toByte(), 0x28.toByte(), 0x87.toByte(), 0x2C.toByte(), 0x2C.toByte(),
        0x2C.toByte(), 0x84.toByte(), 0x30.toByte(),

        //> VictoryMusData:
        //> VictoryMusData ($FEC8):
        0x83.toByte(), 0x04.toByte(), 0x84.toByte(), 0x0C.toByte(), 0x83.toByte(), 0x62.toByte(), 0x10.toByte(), 0x84.toByte(),
        0x12.toByte(), 0x83.toByte(), 0x1C.toByte(), 0x22.toByte(), 0x1E.toByte(), 0x22.toByte(), 0x26.toByte(), 0x18.toByte(),
        0x1E.toByte(), 0x04.toByte(), 0x1C.toByte(), 0x00.toByte(),
        // sq1
        0xE3.toByte(), 0xE1.toByte(), 0xE3.toByte(), 0x1D.toByte(), 0xDE.toByte(), 0xE0.toByte(), 0x23.toByte(),
        0xEC.toByte(), 0x75.toByte(), 0x74.toByte(), 0xF0.toByte(), 0xF4.toByte(), 0xF6.toByte(), 0xEA.toByte(), 0x31.toByte(),
        0x2D.toByte(),
        // tri
        0x83.toByte(), 0x12.toByte(), 0x14.toByte(), 0x04.toByte(), 0x18.toByte(), 0x1A.toByte(), 0x1C.toByte(), 0x14.toByte(),
        0x26.toByte(), 0x22.toByte(), 0x1E.toByte(), 0x1C.toByte(), 0x18.toByte(), 0x1E.toByte(), 0x22.toByte(), 0x0C.toByte(),
        0x14.toByte(),
        //> ;unused space
        // unused
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
    )

    /**
     * Read a byte of music data at the given NES address.
     * The NES sound engine loads (musicDataLow, musicDataHigh) as a base pointer,
     * then reads bytes at base+offset for each channel.
     */
    fun readMusicData(nesAddr: Int): Int {
        // Try SMB1 address range first, then SMB2J
        var index = nesAddr - MUSIC_DATA_BASE_SMB1
        if (index < 0 || index >= musicDataBytes.size) {
            index = nesAddr - MUSIC_DATA_BASE_SMB2J
        }
        if (index < 0 || index >= musicDataBytes.size) return 0
        return musicDataBytes[index].toInt() and 0xFF
    }

    /**
     * Read a byte of music data using the base address (lo/hi) and an offset,
     * matching how the NES sound engine works.
     */
    fun readMusicData(addrLow: Int, addrHigh: Int, offset: Int): Int {
        val baseAddr = ((addrHigh and 0xFF) shl 8) or (addrLow and 0xFF)
        return readMusicData(baseAddr + offset)
    }

    // -----------------------------------------------------------------------
    // Frequency register lookup table (102 bytes, pairs of hi/lo timer values)
    // Indexed by note value from music data (even offsets = freq pairs)
    // -----------------------------------------------------------------------

    //> FreqRegLookupTbl:
    val freqRegLookupTbl = intArrayOf(
        0x00, 0x88, 0x00, 0x2F, 0x00, 0x00,
        0x02, 0xA6, 0x02, 0x80, 0x02, 0x5C, 0x02, 0x3A,
        0x02, 0x1A, 0x01, 0xDF, 0x01, 0xC4, 0x01, 0xAB,
        0x01, 0x93, 0x01, 0x7C, 0x01, 0x67, 0x01, 0x53,
        0x01, 0x40, 0x01, 0x2E, 0x01, 0x1D, 0x01, 0x0D,
        0x00, 0xFE, 0x00, 0xEF, 0x00, 0xE2, 0x00, 0xD5,
        0x00, 0xC9, 0x00, 0xBE, 0x00, 0xB3, 0x00, 0xA9,
        0x00, 0xA0, 0x00, 0x97, 0x00, 0x8E, 0x00, 0x86,
        0x00, 0x77, 0x00, 0x7E, 0x00, 0x71, 0x00, 0x54,
        0x00, 0x64, 0x00, 0x5F, 0x00, 0x59, 0x00, 0x50,
        0x00, 0x47, 0x00, 0x43, 0x00, 0x3B, 0x00, 0x35,
        0x00, 0x2A, 0x00, 0x23, 0x04, 0x75, 0x03, 0x57,
        0x02, 0xF9, 0x02, 0xCF, 0x01, 0xFC, 0x00, 0x6A
    )

    // -----------------------------------------------------------------------
    // Music length lookup table (48 bytes, 6 rows of 8)
    // Indexed by length byte bits to determine note duration in frames
    // -----------------------------------------------------------------------

    //> MusicLengthLookupTbl:
    val musicLengthLookupTbl = intArrayOf(
        0x05, 0x0A, 0x14, 0x28, 0x50, 0x1E, 0x3C, 0x02,
        0x04, 0x08, 0x10, 0x20, 0x40, 0x18, 0x30, 0x0C,
        0x03, 0x06, 0x0C, 0x18, 0x30, 0x12, 0x24, 0x08,
        0x36, 0x03, 0x09, 0x06, 0x12, 0x1B, 0x24, 0x0C,
        0x24, 0x02, 0x06, 0x04, 0x0C, 0x12, 0x18, 0x08,
        0x12, 0x01, 0x03, 0x02, 0x06, 0x09, 0x0C, 0x04
    )

    // -----------------------------------------------------------------------
    // Envelope data tables (used by sound engine for volume/duty cycling)
    // -----------------------------------------------------------------------

    // These three envelope tables are contiguous in ROM ($FF96).
    // On the NES, overflow reads from one table bleed into the next.
    // A single backing array ensures overflow reads produce correct ROM bytes.
    private val musicEnvDataRom = intArrayOf(
        //> EndOfCastleMusicEnvData: (offset 0)
        0x98, 0x99, 0x9A, 0x9B,
        //> AreaMusicEnvData: (offset 4)
        0x90, 0x94, 0x94, 0x95, 0x95, 0x96, 0x97, 0x98,
        //> WaterEventMusEnvData: (offset 12)
        0x90, 0x91, 0x92, 0x92, 0x93, 0x93, 0x93, 0x94,
        0x94, 0x94, 0x94, 0x94, 0x94, 0x95, 0x95, 0x95,
        0x95, 0x95, 0x95, 0x96, 0x96, 0x96, 0x96, 0x96,
        0x96, 0x96, 0x96, 0x96, 0x96, 0x96, 0x96, 0x96,
        0x96, 0x96, 0x96, 0x96, 0x95, 0x95, 0x94, 0x93
    )

    //> EndOfCastleMusicEnvData:
    val endOfCastleMusicEnvData = musicEnvDataRom  // overflow reads into AreaMusic/Water data

    //> AreaMusicEnvData:
    val areaMusicEnvData = IntArray(8) { musicEnvDataRom[4 + it] }

    //> WaterEventMusEnvData:
    val waterEventMusEnvData = IntArray(40) { musicEnvDataRom[12 + it] }

    //> BowserFlameEnvData:
    val bowserFlameEnvData = intArrayOf(
        0x15, 0x16, 0x16, 0x17, 0x17, 0x18, 0x19, 0x19,
        0x1A, 0x1A, 0x1C, 0x1D, 0x1D, 0x1E, 0x1E, 0x1F,
        0x1F, 0x1F, 0x1F, 0x1E, 0x1D, 0x1C, 0x1E, 0x1F,
        0x1F, 0x1E, 0x1D, 0x1C, 0x1A, 0x18, 0x16, 0x14
    )

    //> BrickShatterEnvData:
    //> .dw $fff0  ;unused
    //> ;INTERRUPT VECTORS
    val brickShatterEnvData = intArrayOf(
        0x15, 0x16, 0x16, 0x17, 0x17, 0x18, 0x19, 0x19,
        0x1A, 0x1A, 0x1C, 0x1D, 0x1D, 0x1E, 0x1E, 0x1F
    )

    // -----------------------------------------------------------------------
    // Sound effect frequency/envelope data tables
    // -----------------------------------------------------------------------

    //> BrickShatterFreqData:
    val brickShatterFreqData = intArrayOf(
        0x01, 0x0E, 0x0E, 0x0D, 0x0B, 0x06, 0x0C, 0x0F,
        0x0A, 0x09, 0x03, 0x0D, 0x08, 0x0D, 0x06, 0x0C
    )

    //> SkidSfxFreqData: (SMB2J only)
    val skidSfxFreqData = intArrayOf(
        0x47, 0x49, 0x42, 0x4A, 0x43, 0x4B
    )

    //> WindFreqEnvData: (SMB2J only) upper nybble = envelope, lower nybble = frequency
    val windFreqEnvData = intArrayOf(
        0x37, 0x46, 0x55, 0x64, 0x74, 0x83, 0x93, 0xA2,
        0xB1, 0xC0, 0xD0, 0xE0, 0xF1, 0xF1, 0xF2, 0xE2,
        0xE2, 0xC3, 0xA3, 0x84, 0x64, 0x44, 0x35, 0x25
    )

    //> SwimStompEnvelopeData:
    val swimStompEnvelopeData = intArrayOf(
        0x9F, 0x9B, 0x98, 0x96, 0x95, 0x94, 0x92, 0x90,
        0x90, 0x9A, 0x97, 0x95, 0x93, 0x92
    )

    //> ExtraLifeFreqData:
    val extraLifeFreqData = intArrayOf(0x58, 0x02, 0x54, 0x56, 0x4E, 0x44)

    //> PowerUpGrabFreqData:
    val powerUpGrabFreqData = intArrayOf(
        0x4C, 0x52, 0x4C, 0x48, 0x3E, 0x36, 0x3E, 0x36, 0x30,
        0x28, 0x4A, 0x50, 0x4A, 0x64, 0x3C, 0x32, 0x3C, 0x32,
        0x2C, 0x24, 0x3A, 0x64, 0x3A, 0x34, 0x2C, 0x22, 0x2C,
        // residual frequency data
        0x22, 0x1C, 0x14
    )

    //> PUp_VGrow_FreqData:
    val pUpVGrowFreqData = intArrayOf(
        0x14, 0x04, 0x22, 0x24, 0x16, 0x04, 0x24, 0x26,  // used by both powerup and vinegrow
        0x18, 0x04, 0x26, 0x28, 0x1A, 0x04, 0x28, 0x2A,
        0x1C, 0x04, 0x2A, 0x2C, 0x1E, 0x04, 0x2C, 0x2E,  // used by vinegrow only
        0x20, 0x04, 0x2E, 0x30, 0x22, 0x04, 0x30, 0x32
    )

    //> ;INTERRUPT VECTORS
    //> .dw $fff0  ;unused
}

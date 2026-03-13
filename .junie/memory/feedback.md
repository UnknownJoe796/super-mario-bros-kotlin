[2026-03-12 20:43] - Updated by Junie
{
    "TYPE": "preference",
    "CATEGORY": "workflow preference",
    "EXPECTATION": "Investigate both documented potential translation issues and divergences; prefer using subagents in parallel, otherwise do it personally.",
    "NEW INSTRUCTION": "WHEN multiple analyses are needed and subagents are available THEN delegate tasks to subagents in parallel"
}

[2026-03-12 20:48] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "validation methodology",
    "EXPECTATION": "Do not rely solely on TAS divergence to judge correctness; cross-reference issues 4/5/7 with their disassembly subroutines and assess translation accuracy, asking the user if unsure.",
    "NEW INSTRUCTION": "WHEN investigating potential translation issues or divergences THEN cross-reference authoritative disassembly and prioritize correctness over TAS pass rate"
}

[2026-03-12 23:33] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "runtime and viewport",
    "EXPECTATION": "Handle FrameDelay in the normal application loop and limit rendering to a single visible nametable-sized viewport.",
    "NEW INSTRUCTION": "WHEN implementing emulator-wide timing or rendering changes THEN update both test and normal application modes"
}

[2026-03-12 23:36] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "runtime and viewport",
    "EXPECTATION": "Normal app mode must handle FrameDelay and render only a single 256x240 nametable viewport.",
    "NEW INSTRUCTION": "WHEN running normal application mode THEN catch/resume FrameDelay and render one 256x240 viewport"
}

[2026-03-12 23:48] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "normal app rendering",
    "EXPECTATION": "Normal application mode should display the NES gameplay viewport, not a black box.",
    "NEW INSTRUCTION": "WHEN changing rendering or timing logic THEN run normal app and confirm non-black 256x240 viewport"
}

[2026-03-12 23:51] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "normal app rendering",
    "EXPECTATION": "Normal app should show a single 256x240 NES viewport (not a tiny top-left black box) and progress past initialization instead of looping on frame delays.",
    "NEW INSTRUCTION": "WHEN normal app logs repeated FrameDelay during init THEN ensure disableScreenFlag clears and render one 256x240 viewport"
}

[2026-03-12 23:52] - Updated by Junie
{
    "TYPE": "correction",
    "CATEGORY": "normal app rendering viewport",
    "EXPECTATION": "Normal application should display a single 256x240 NES viewport (not a tiny top-left black box) and show actual gameplay once initialization advances.",
    "NEW INSTRUCTION": "WHEN normal app shows top-left black rectangle THEN render one 256x240 viewport and verify non-black frames post-init"
}


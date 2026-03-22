#!/usr/bin/env python3
"""
Verify that all original disassembly code from smbdism.asm is present
as //> comments in the Kotlin translation files.

Approach:
1. Extract all assembly labels from the code section of smbdism.asm
2. Extract all //> comment lines from Kotlin files
3. Match labels and instruction lines
4. Report coverage and gaps
"""

import os
import re
import sys
from collections import defaultdict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASM_FILE = os.path.join(PROJECT_ROOT, "smbdism.asm")
KOTLIN_DIR = os.path.join(PROJECT_ROOT, "src", "main", "kotlin")

# The code section starts after the last data area (L_WaterArea3) and the
# "unused space" marker. GameMode: is the first real routine label.
CODE_SECTION_START_LABEL = "GameMode"

# ─── Parse ASM file ───────────────────────────────────────────────────────────

def parse_asm_file(path):
    """Parse smbdism.asm and return structured data."""
    with open(path, 'r') as f:
        lines = f.readlines()

    all_labels = {}  # label_name -> line_number
    code_start_line = None

    for i, raw_line in enumerate(lines, 1):
        line = raw_line.rstrip('\n')
        # Extract labels
        label_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', line)
        if label_match:
            label_name = label_match.group(1)
            all_labels[label_name] = i
            if label_name == CODE_SECTION_START_LABEL and code_start_line is None:
                code_start_line = i

    return lines, all_labels, code_start_line


def categorize_asm_line(stripped):
    """Categorize an assembly line."""
    if not stripped:
        return "empty"
    if stripped.startswith(';---'):
        return "separator"
    if stripped.startswith(';'):
        return "comment"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*\s*=\s*', stripped):
        return "define"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*:', stripped):
        return "label"
    if re.match(r'^\s*\.(db|dw|org|ds)\b', stripped, re.IGNORECASE):
        return "directive"
    instr_re = (r'^\s*(?:[A-Za-z_]\w*:\s+)?'
                r'(lda|ldx|ldy|sta|stx|sty|adc|sbc|and|ora|eor|cmp|cpx|cpy|'
                r'inc|inx|iny|dec|dex|dey|asl|lsr|rol|ror|'
                r'jmp|jsr|rts|rti|'
                r'beq|bne|bcc|bcs|bpl|bmi|bvc|bvs|'
                r'clc|sec|cli|sei|cld|sed|clv|'
                r'pha|pla|php|plp|'
                r'tax|tay|txa|tya|tsx|txs|'
                r'nop|brk|bit)\b')
    if re.match(instr_re, stripped, re.IGNORECASE):
        return "instruction"
    return "other"


# ─── Parse Kotlin files ──────────────────────────────────────────────────────

def parse_kotlin_files(kotlin_dir):
    """Extract all //> comments from Kotlin files."""
    asm_comments = []      # (file_path, line_number, comment_text)
    all_kotlin_text = []   # all //> comment texts, normalized
    asm_labels_found = set()

    for root, dirs, files in os.walk(kotlin_dir):
        for fname in sorted(files):
            if not fname.endswith('.kt'):
                continue
            fpath = os.path.join(root, fname)
            rel_path = os.path.relpath(fpath, kotlin_dir)
            with open(fpath, 'r') as f:
                for line_num, line in enumerate(f, 1):
                    match = re.search(r'//>\s?(.*)', line)
                    if match:
                        comment = match.group(1).rstrip()
                        asm_comments.append((rel_path, line_num, comment))
                        all_kotlin_text.append(comment)

                        # Extract labels
                        label_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', comment)
                        if label_match:
                            asm_labels_found.add(label_match.group(1))
                        # Also inline labels: "LabelName:  instruction"
                        inline_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):\s+\w', comment)
                        if inline_match:
                            asm_labels_found.add(inline_match.group(1))

    # Build normalized set for fast lookup
    normalized_set = set()
    for c in all_kotlin_text:
        normalized_set.add(normalize(c))

    return asm_comments, all_kotlin_text, normalized_set, asm_labels_found


def normalize(text):
    """Normalize for matching: lowercase, collapse whitespace, strip comments."""
    s = text.strip()
    s = re.sub(r'\s+', ' ', s)
    return s.lower()


def normalize_no_comment(text):
    """Normalize and strip trailing asm comments."""
    s = re.sub(r'\s*;.*$', '', text).strip()
    return normalize(s)


# ─── Matching logic ──────────────────────────────────────────────────────────

def is_line_found(stripped, category, norm_set, kotlin_labels, kotlin_texts_lower):
    """Check if an assembly line appears in Kotlin //> comments."""
    norm = normalize(stripped)

    # Direct match
    if norm in norm_set:
        return True

    # Match without trailing comment
    norm_nc = normalize_no_comment(stripped)
    if norm_nc and norm_nc in norm_set:
        return True

    # For labels: check if the label name exists
    if category == "label":
        m = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', stripped)
        if m and m.group(1) in kotlin_labels:
            return True

    # For instructions: check if the instruction (without comment) is a substring
    # of any Kotlin //> comment (handles cases where formatting differs slightly)
    if category == "instruction" and norm_nc:
        # Also handle inline label:instruction format
        # e.g., "ProcELoop:    stx ObjectOffset" -> check "stx objectoffset"
        instr_only = re.sub(r'^[A-Za-z_]\w*:\s+', '', stripped).strip()
        instr_norm = normalize_no_comment(instr_only)
        if instr_norm in norm_set:
            return True
        # Substring check (expensive but catches reformatted lines)
        for kt in kotlin_texts_lower:
            if instr_norm and instr_norm in kt:
                return True

    # For directives (.db): check if data values match (ignoring formatting)
    if category == "directive":
        # Extract hex values
        hex_vals = re.findall(r'\$[0-9a-fA-F]+', stripped)
        if hex_vals and len(hex_vals) >= 2:
            hex_pattern = '.*'.join(re.escape(h.lower()) for h in hex_vals[:4])
            for kt in kotlin_texts_lower:
                if re.search(hex_pattern, kt):
                    return True

    # For comments: check if the comment text appears as substring
    if category == "comment":
        comment_text = stripped.lstrip(';').strip().lower()
        if len(comment_text) > 8:
            for kt in kotlin_texts_lower:
                if comment_text in kt:
                    return True

    return False


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    lines_raw, all_labels, code_start = parse_asm_file(ASM_FILE)
    asm_comments, kotlin_texts, norm_set, kotlin_labels = parse_kotlin_files(KOTLIN_DIR)

    # Pre-compute lowercase kotlin texts for substring matching
    kotlin_texts_lower = [normalize(t) for t in kotlin_texts]

    print("=" * 80)
    print("ASM COMMENT VERIFICATION REPORT")
    print("=" * 80)
    print()
    print(f"ASM file: {len(lines_raw)} lines, {len(all_labels)} labels")
    print(f"Kotlin //> comments: {len(asm_comments)} lines, {len(kotlin_labels)} distinct labels")
    print(f"Code section starts at line {code_start} ({CODE_SECTION_START_LABEL}:)")
    print()

    # Process code section only
    code_lines = []  # (line_num, stripped, category)
    for i in range(code_start - 1, len(lines_raw)):
        raw = lines_raw[i].rstrip('\n')
        stripped = raw.strip()
        if not stripped:
            continue
        cat = categorize_asm_line(stripped)
        if cat in ("empty", "separator", "define"):
            continue
        code_lines.append((i + 1, stripped, cat))

    # Check each line
    found_lines = []
    missing_lines = []
    stats = defaultdict(lambda: {"total": 0, "found": 0})

    for line_num, stripped, cat in code_lines:
        stats[cat]["total"] += 1
        if is_line_found(stripped, cat, norm_set, kotlin_labels, kotlin_texts_lower):
            stats[cat]["found"] += 1
            found_lines.append((line_num, stripped, cat))
        else:
            missing_lines.append((line_num, stripped, cat))

    total = len(code_lines)
    total_found = len(found_lines)
    total_missing = len(missing_lines)

    # ─── Label report ────────────────────────────────────────────────────────

    print("=" * 80)
    print("LABEL COVERAGE")
    print("=" * 80)
    print()

    code_label_names = {name for name, ln in all_labels.items() if ln >= code_start}
    missing_label_names = code_label_names - kotlin_labels
    # Also check by searching all kotlin text
    still_missing = set()
    for label in missing_label_names:
        found_any = False
        for kt in kotlin_texts:
            if label in kt:
                found_any = True
                break
        if not found_any:
            still_missing.add(label)

    found_label_count = len(code_label_names) - len(still_missing)
    print(f"Code labels: {found_label_count}/{len(code_label_names)} "
          f"({found_label_count/len(code_label_names)*100:.1f}%)")
    if still_missing:
        print(f"\nMissing labels ({len(still_missing)}):")
        for label in sorted(still_missing, key=lambda l: all_labels.get(l, 0)):
            ln = all_labels[label]
            # Show a few lines of context
            ctx = lines_raw[ln-1].rstrip('\n')[:90]
            print(f"  {ln:5d}: {ctx}")

    # ─── Line coverage ───────────────────────────────────────────────────────

    print()
    print("=" * 80)
    print("LINE-BY-LINE COVERAGE")
    print("=" * 80)
    print()
    print(f"Total code lines: {total}")
    print(f"Found:   {total_found} ({total_found/total*100:.1f}%)")
    print(f"Missing: {total_missing} ({total_missing/total*100:.1f}%)")
    print()
    for cat in sorted(stats.keys()):
        s = stats[cat]
        pct = s["found"]/s["total"]*100 if s["total"] else 0
        missing_n = s["total"] - s["found"]
        print(f"  {cat:12s}: {s['found']:4d}/{s['total']:4d} ({pct:5.1f}%) - {missing_n} missing")

    # ─── Gap analysis ────────────────────────────────────────────────────────

    print()
    print("=" * 80)
    print("CONTIGUOUS GAPS (>10 consecutive missing lines)")
    print("=" * 80)
    print()

    missing_set = {ln for ln, _, _ in missing_lines}
    if missing_lines:
        sorted_missing = sorted(missing_set)
        gaps = []
        current_gap = [sorted_missing[0]]
        for ln in sorted_missing[1:]:
            if ln - current_gap[-1] <= 3:  # allow small gaps (empty/separator lines)
                current_gap.append(ln)
            else:
                if len(current_gap) > 10:
                    gaps.append(current_gap[:])
                current_gap = [ln]
        if len(current_gap) > 10:
            gaps.append(current_gap)

        if gaps:
            for gap in gaps:
                first, last = gap[0], gap[-1]
                # Find nearest label
                nearest = None
                for name, ln in sorted(all_labels.items(), key=lambda x: x[1]):
                    if ln <= first:
                        nearest = (name, ln)
                    else:
                        break
                label_info = f" (near {nearest[0]})" if nearest else ""
                print(f"Lines {first}-{last} ({len(gap)} lines){label_info}:")
                for ln in gap[:8]:
                    print(f"  {ln:5d}: {lines_raw[ln-1].rstrip()[:100]}")
                if len(gap) > 8:
                    print(f"  ... ({len(gap)-8} more)")
                print()
        else:
            print("No large contiguous gaps found.")

    # ─── All missing lines (grouped by nearest label) ────────────────────────

    print()
    print("=" * 80)
    print("ALL MISSING LINES (grouped by nearest routine)")
    print("=" * 80)
    print()

    # Build label-to-line-range map
    sorted_labels = sorted(all_labels.items(), key=lambda x: x[1])
    label_ranges = []
    for idx, (name, start_ln) in enumerate(sorted_labels):
        if start_ln < code_start:
            continue
        end_ln = sorted_labels[idx+1][1] if idx+1 < len(sorted_labels) else len(lines_raw)
        label_ranges.append((name, start_ln, end_ln))

    # Group missing lines by their containing routine
    routine_missing = defaultdict(list)
    for ln, text, cat in missing_lines:
        # Find containing routine
        routine_name = "unknown"
        for name, start, end in label_ranges:
            if start <= ln < end:
                routine_name = name
                break
        routine_missing[routine_name].append((ln, text, cat))

    # Sort routines by line number
    routine_order = {name: start for name, start, end in label_ranges}
    for routine_name in sorted(routine_missing.keys(),
                                key=lambda r: routine_order.get(r, 0)):
        items = routine_missing[routine_name]
        print(f"\n{routine_name} ({len(items)} missing):")
        for ln, text, cat in items[:10]:
            print(f"  {ln:5d} [{cat:11s}]: {text[:90]}")
        if len(items) > 10:
            print(f"  ... ({len(items)-10} more)")

    # ─── Summary ─────────────────────────────────────────────────────────────

    print()
    print("=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print()
    print(f"Labels:  {found_label_count}/{len(code_label_names)} "
          f"({found_label_count/len(code_label_names)*100:.1f}%)")
    print(f"Lines:   {total_found}/{total} ({total_found/total*100:.1f}%)")
    print()

    if total_missing > 0:
        # Categorize what's missing
        missing_cats = defaultdict(int)
        for _, _, cat in missing_lines:
            missing_cats[cat] += 1
        print(f"Missing breakdown: {dict(missing_cats)}")

    return 0 if total_missing == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

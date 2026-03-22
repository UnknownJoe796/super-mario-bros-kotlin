#!/usr/bin/env python3
"""
Verify that all original disassembly code from smbdism.asm is present
as //> comments in the Kotlin translation files.
"""

import os
import re
import sys
from collections import defaultdict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASM_FILE = os.path.join(PROJECT_ROOT, "smbdism.asm")
KOTLIN_DIR = os.path.join(PROJECT_ROOT, "src", "main", "kotlin")

# ─── Parse ASM file ───────────────────────────────────────────────────────────

def parse_asm_file(path):
    """Parse smbdism.asm and return structured data about all labels and lines."""
    with open(path, 'r') as f:
        lines = f.readlines()

    # Track all labels and their line numbers
    all_labels = {}  # label_name -> line_number
    # Track all meaningful assembly lines (instructions, data, labels)
    all_lines = []  # (line_number, raw_line, normalized_line, section)

    current_section = "defines"  # switches to "code" after defines end
    code_start_line = None

    for i, raw_line in enumerate(lines, 1):
        line = raw_line.rstrip('\n')
        stripped = line.strip()

        # Skip empty lines
        if not stripped:
            continue

        # Detect transition from defines to code section
        # The defines section has lines like "LABEL = $XXXX"
        # Code section starts with actual labels and instructions
        if current_section == "defines":
            # Look for the first routine label (code section start)
            # The code starts around "GameMode:" after the data tables
            if re.match(r'^[A-Za-z_][A-Za-z0-9_]*:', stripped) and i > 5000:
                current_section = "code"
                code_start_line = i

        # Extract labels (identifiers followed by colon at start of line)
        label_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', line)
        if label_match:
            label_name = label_match.group(1)
            all_labels[label_name] = i

        # Also match labels that are on the same line as instructions
        # e.g., "ProcELoop:    stx ObjectOffset"
        inline_label_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):\s+\w', line)
        if inline_label_match:
            label_name = inline_label_match.group(1)
            all_labels[label_name] = i

        all_lines.append((i, line, stripped, current_section))

    return all_labels, all_lines, code_start_line


def categorize_asm_line(stripped):
    """Categorize an assembly line."""
    if not stripped:
        return "empty"
    if stripped.startswith(';'):
        return "comment"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*\s*=\s*\$', stripped):
        return "define"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*:', stripped):
        return "label"
    if stripped.startswith('.'):
        return "directive"  # .db, .dw, .org, etc.
    if re.match(r'^\s*(lda|ldx|ldy|sta|stx|sty|adc|sbc|and|ora|eor|cmp|cpx|cpy|'
                r'inc|inx|iny|dec|dex|dey|asl|lsr|rol|ror|'
                r'jmp|jsr|rts|rti|'
                r'beq|bne|bcc|bcs|bpl|bmi|bvc|bvs|'
                r'clc|sec|cli|sei|cld|sed|clv|'
                r'pha|pla|php|plp|'
                r'tax|tay|txa|tya|tsx|txs|'
                r'nop|brk|bit)\b', stripped, re.IGNORECASE):
        return "instruction"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*:', stripped):
        return "label"
    return "other"


# ─── Parse Kotlin files ──────────────────────────────────────────────────────

def parse_kotlin_files(kotlin_dir):
    """Extract all //> comments from Kotlin files."""
    asm_comments = []  # (file_path, line_number, raw_comment, normalized)
    asm_comment_set = set()  # normalized comments for fast lookup
    asm_labels_found = set()  # labels found in //> comments

    for root, dirs, files in os.walk(kotlin_dir):
        for fname in sorted(files):
            if not fname.endswith('.kt'):
                continue
            fpath = os.path.join(root, fname)
            rel_path = os.path.relpath(fpath, kotlin_dir)
            with open(fpath, 'r') as f:
                for line_num, line in enumerate(f, 1):
                    # Match //> comments (possibly indented)
                    match = re.search(r'//>\s?(.*)', line)
                    if match:
                        comment = match.group(1).strip()
                        asm_comments.append((rel_path, line_num, comment, comment))
                        # Normalize for matching: lowercase, collapse whitespace
                        normalized = re.sub(r'\s+', ' ', comment.lower()).strip()
                        asm_comment_set.add(normalized)

                        # Extract label if this is a label line
                        label_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', comment)
                        if label_match:
                            asm_labels_found.add(label_match.group(1))

    return asm_comments, asm_comment_set, asm_labels_found


def normalize_asm_for_matching(line):
    """Normalize an assembly line for fuzzy matching against Kotlin //> comments."""
    # Remove leading whitespace
    stripped = line.strip()
    # Collapse multiple spaces
    normalized = re.sub(r'\s+', ' ', stripped).lower()
    return normalized


# ─── Main verification ───────────────────────────────────────────────────────

def main():
    print("=" * 80)
    print("ASM COMMENT VERIFICATION REPORT")
    print("=" * 80)
    print()

    # Parse both sources
    all_labels, all_lines, code_start = parse_asm_file(ASM_FILE)
    asm_comments, asm_comment_set, kotlin_labels = parse_kotlin_files(KOTLIN_DIR)

    print(f"ASM file: {len(all_lines)} non-empty lines, {len(all_labels)} labels")
    print(f"Kotlin files: {len(asm_comments)} //> comment lines, {len(kotlin_labels)} labels")
    print(f"Code section starts at line {code_start}")
    print()

    # ─── 1. Label Verification ──────────────────────────────────────────────

    print("=" * 80)
    print("SECTION 1: LABEL VERIFICATION")
    print("=" * 80)
    print()

    # Separate labels into defines (before code) and code (after)
    define_labels = {}
    code_labels = {}
    # Also separate data labels (L_* area data, enemy data) from code routine labels
    data_labels = {}
    routine_labels = {}

    for label, line_num in all_labels.items():
        if code_start and line_num >= code_start:
            code_labels[label] = line_num
            routine_labels[label] = line_num
        else:
            define_labels[label] = line_num
            if label.startswith('L_') or label.startswith('E_'):
                data_labels[label] = line_num

    # Check which code labels appear in Kotlin //> comments
    missing_code_labels = []
    found_code_labels = []
    for label in sorted(code_labels.keys()):
        if label in kotlin_labels:
            found_code_labels.append(label)
        else:
            # Also check if the label appears anywhere in //> comments (not just as a label definition)
            label_lower = label.lower()
            found_in_comment = False
            for _, _, comment, _ in asm_comments:
                if label in comment or label_lower in comment.lower():
                    found_in_comment = True
                    break
            if found_in_comment:
                found_code_labels.append(label)
            else:
                missing_code_labels.append((label, code_labels[label]))

    print(f"Code section labels: {len(code_labels)} total")
    print(f"  Found in Kotlin: {len(found_code_labels)}")
    print(f"  Missing from Kotlin: {len(missing_code_labels)}")
    print()

    if missing_code_labels:
        print("MISSING CODE LABELS:")
        # Group by approximate location in asm file
        for label, line_num in sorted(missing_code_labels, key=lambda x: x[1]):
            # Show context: what's around this label in the asm
            print(f"  Line {line_num:5d}: {label}")
        print()

    # ─── 2. Line-by-line coverage ────────────────────────────────────────────

    print("=" * 80)
    print("SECTION 2: LINE-BY-LINE COVERAGE (Code Section)")
    print("=" * 80)
    print()

    # Extract code section lines
    code_section_lines = [(ln, raw, stripped, sec) for ln, raw, stripped, sec in all_lines
                          if sec == "code"]

    # Categorize and check each line
    stats = defaultdict(lambda: {"total": 0, "found": 0, "missing": []})
    total_meaningful = 0
    total_found = 0

    for line_num, raw_line, stripped, section in code_section_lines:
        category = categorize_asm_line(stripped)

        # Skip pure comments and empty lines for matching purposes
        # (though we DO want to check that comment-as-section-headers are present)
        if category in ("empty",):
            continue

        # Skip separator lines
        if stripped.startswith(';---'):
            continue

        stats[category]["total"] += 1

        # Normalize for matching
        normalized = normalize_asm_for_matching(stripped)

        # Check if this line appears in Kotlin //> comments
        found = normalized in asm_comment_set

        # If not found by exact match, try partial matching
        if not found:
            # For labels, check if the label name appears
            if category == "label":
                label_match = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', stripped)
                if label_match and label_match.group(1) in kotlin_labels:
                    found = True

            # For instructions, try matching just the instruction + operand (without comment)
            if not found and category == "instruction":
                # Strip trailing comment
                no_comment = re.sub(r'\s*;.*$', '', stripped).strip()
                norm_no_comment = normalize_asm_for_matching(no_comment)
                if norm_no_comment in asm_comment_set:
                    found = True
                # Also try with the comment
                if not found:
                    for _, _, comment, _ in asm_comments:
                        norm_comment = normalize_asm_for_matching(comment)
                        if norm_no_comment in norm_comment or normalized in norm_comment:
                            found = True
                            break

            # For directives (.db, .dw), try matching the data content
            if not found and category == "directive":
                no_comment = re.sub(r'\s*;.*$', '', stripped).strip()
                norm_no_comment = normalize_asm_for_matching(no_comment)
                for _, _, comment, _ in asm_comments:
                    norm_comment = normalize_asm_for_matching(comment)
                    if norm_no_comment in norm_comment or norm_comment in norm_no_comment:
                        found = True
                        break

            # For comments, try substring matching
            if not found and category == "comment":
                comment_text = stripped.lstrip(';').strip().lower()
                if len(comment_text) > 5:  # skip very short comments
                    for _, _, kt_comment, _ in asm_comments:
                        if comment_text in kt_comment.lower():
                            found = True
                            break

        if found:
            stats[category]["found"] += 1
            total_found += 1
        else:
            stats[category]["missing"].append((line_num, stripped))

        total_meaningful += 1

    print(f"Total meaningful code lines: {total_meaningful}")
    print(f"Found in Kotlin //> comments: {total_found}")
    print(f"Missing: {total_meaningful - total_found}")
    print(f"Coverage: {total_found/total_meaningful*100:.1f}%")
    print()

    for category in sorted(stats.keys()):
        s = stats[category]
        pct = s["found"]/s["total"]*100 if s["total"] else 0
        print(f"  {category:12s}: {s['found']:4d}/{s['total']:4d} ({pct:.1f}%)")
    print()

    # ─── 3. Detailed missing lines ──────────────────────────────────────────

    print("=" * 80)
    print("SECTION 3: MISSING LINES DETAIL")
    print("=" * 80)
    print()

    for category in ["label", "instruction", "directive", "comment", "other"]:
        if category not in stats or not stats[category]["missing"]:
            continue
        missing = stats[category]["missing"]
        print(f"\n--- Missing {category}s ({len(missing)}) ---")
        # Group by contiguous ranges
        current_range_start = None
        current_range = []
        ranges = []
        for line_num, text in missing:
            if current_range and line_num - current_range[-1][0] > 5:
                ranges.append(current_range[:])
                current_range = []
            current_range.append((line_num, text))
        if current_range:
            ranges.append(current_range)

        for range_group in ranges:
            first_line = range_group[0][0]
            last_line = range_group[-1][0]
            if len(range_group) <= 3:
                for ln, txt in range_group:
                    print(f"  Line {ln:5d}: {txt[:100]}")
            else:
                print(f"  Lines {first_line}-{last_line} ({len(range_group)} lines):")
                for ln, txt in range_group[:3]:
                    print(f"    {ln:5d}: {txt[:100]}")
                print(f"    ... ({len(range_group)-3} more)")
        print()

    # ─── 4. Contiguous gap analysis ─────────────────────────────────────────

    print("=" * 80)
    print("SECTION 4: CONTIGUOUS GAPS (runs of >5 consecutive missing lines)")
    print("=" * 80)
    print()

    # Build set of all missing line numbers
    all_missing_lines = set()
    for category in stats:
        for line_num, _ in stats[category]["missing"]:
            all_missing_lines.add(line_num)

    # Find contiguous gaps
    if all_missing_lines:
        sorted_missing = sorted(all_missing_lines)
        gaps = []
        current_gap = [sorted_missing[0]]
        for ln in sorted_missing[1:]:
            if ln - current_gap[-1] <= 2:  # allow 1-line gaps (empty lines)
                current_gap.append(ln)
            else:
                if len(current_gap) > 5:
                    gaps.append(current_gap[:])
                current_gap = [ln]
        if len(current_gap) > 5:
            gaps.append(current_gap)

        if gaps:
            print(f"Found {len(gaps)} significant gaps:")
            for gap in gaps:
                first = gap[0]
                last = gap[-1]
                print(f"\n  ASM lines {first}-{last} ({len(gap)} missing lines):")
                # Show what's at the start and end of the gap from the asm file
                with open(ASM_FILE, 'r') as f:
                    asm_lines = f.readlines()
                # Find the nearest label before this gap
                nearest_label = None
                for label, label_line in sorted(all_labels.items(), key=lambda x: x[1]):
                    if label_line <= first:
                        nearest_label = (label, label_line)
                    else:
                        break
                if nearest_label:
                    print(f"    Near label: {nearest_label[0]} (line {nearest_label[1]})")
                # Show first few lines
                for ln in gap[:5]:
                    if ln <= len(asm_lines):
                        print(f"    {ln:5d}: {asm_lines[ln-1].rstrip()[:100]}")
                if len(gap) > 5:
                    print(f"    ... ({len(gap)-5} more lines)")
        else:
            print("No significant gaps found.")
    else:
        print("No missing lines at all!")

    print()
    print("=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print()
    print(f"Code labels: {len(found_code_labels)}/{len(code_labels)} "
          f"({len(found_code_labels)/len(code_labels)*100:.1f}%)")
    print(f"Code lines:  {total_found}/{total_meaningful} "
          f"({total_found/total_meaningful*100:.1f}%)")
    if missing_code_labels:
        print(f"\n⚠️  {len(missing_code_labels)} labels missing from Kotlin files")
    if total_meaningful - total_found > 0:
        print(f"⚠️  {total_meaningful - total_found} assembly lines missing from Kotlin //> comments")

    # Return exit code
    return 0 if not missing_code_labels and total_found == total_meaningful else 1


if __name__ == "__main__":
    sys.exit(main())

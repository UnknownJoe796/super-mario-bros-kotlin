#!/usr/bin/env python3
"""
Automatically insert missing ASM comments into Kotlin files.

Strategy:
For each missing line, find the best insertion point by:
1. Finding existing //> comments from the same ASM routine
2. Inserting the missing line near those existing comments, based on ASM line ordering
"""

import os
import re
from collections import defaultdict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASM_FILE = os.path.join(PROJECT_ROOT, "smbdism.asm")
KOTLIN_DIR = os.path.join(PROJECT_ROOT, "src", "main", "kotlin")

# Read ASM file for label positions
with open(ASM_FILE) as f:
    asm_lines_raw = f.readlines()

asm_labels = {}
for i, line in enumerate(asm_lines_raw, 1):
    m = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', line)
    if m:
        asm_labels[m.group(1)] = i

# Build sorted label list for finding containing routines
sorted_asm_labels = sorted(asm_labels.items(), key=lambda x: x[1])

def find_asm_routine(asm_line_num):
    """Find the ASM routine label containing this line number."""
    routine = None
    for name, ln in sorted_asm_labels:
        if ln <= asm_line_num:
            routine = name
        else:
            break
    return routine

# Parse Kotlin files
def parse_kotlin_file(filepath):
    """Parse a Kotlin file and build a map of ASM references to line positions."""
    with open(filepath) as f:
        lines = f.readlines()

    # Map: asm_label -> [(kotlin_line_num, comment_text)]
    label_positions = defaultdict(list)
    # Map: asm_line_content_normalized -> kotlin_line_num
    asm_comment_map = {}

    for i, line in enumerate(lines, 1):
        match = re.search(r'//>\s?(.*)', line)
        if match:
            comment = match.group(1).strip()
            # Check for label
            lm = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', comment)
            if lm:
                label_positions[lm.group(1)].append((i, comment))
            # Also check for inline label
            lm2 = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):\s+\w', comment)
            if lm2:
                label_positions[lm2.group(1)].append((i, comment))
            # Store for duplicate detection
            norm = re.sub(r'\s+', ' ', comment.lower()).strip()
            asm_comment_map[norm] = i

    return lines, label_positions, asm_comment_map

def is_duplicate(text, asm_comment_map):
    """Check if this text already exists in the file."""
    norm = re.sub(r'\s+', ' ', text.lower()).strip()
    if norm in asm_comment_map:
        return True
    # Also check without comment
    no_comment = re.sub(r'\s*;.*$', '', text).strip()
    norm_nc = re.sub(r'\s+', ' ', no_comment.lower()).strip()
    if norm_nc and norm_nc in asm_comment_map:
        return True
    return False

def find_insertion_point(missing_asm_line, missing_text, missing_cat, routine_name,
                         lines, label_positions, asm_labels_dict):
    """Find the best line in the Kotlin file to insert this missing comment.
    Returns (kotlin_line_num, indent) where the comment should go BEFORE this line."""

    # Strategy 1: Find existing //> comments for the same routine or nearby routines
    # Get the ASM line range for this routine
    routine_asm_line = asm_labels_dict.get(routine_name, 0)

    # Look for existing //> labels from nearby routines
    best_pos = None
    best_distance = float('inf')

    for label_name, positions in label_positions.items():
        label_asm_line = asm_labels_dict.get(label_name, 0)
        for kt_line, _ in positions:
            dist = abs(label_asm_line - missing_asm_line)
            if dist < best_distance:
                best_distance = dist
                best_pos = kt_line
                # If this label is BEFORE the missing line in ASM, insert AFTER it in Kotlin
                # If AFTER, insert BEFORE it
                if label_asm_line < missing_asm_line:
                    # The missing line comes after this label in ASM,
                    # so we want to insert after this Kotlin line
                    pass

    if best_pos is None:
        return None, "    "

    # Now find the precise position:
    # Walk forward/backward from best_pos to find a good spot

    # Find the closest existing //> comment whose ASM line is just before or just after
    # our missing line's ASM line
    before_pos = None  # Kotlin line of the //> comment just before our ASM line
    after_pos = None   # Kotlin line of the //> comment just after our ASM line

    # Collect all //> comments with their estimated ASM line numbers
    asm_to_kt = []
    for label_name, positions in label_positions.items():
        label_asm = asm_labels_dict.get(label_name, 0)
        for kt_line, comment_text in positions:
            asm_to_kt.append((label_asm, kt_line, comment_text))

    # Also scan for //> comments that contain instruction text we can match to ASM lines
    for i, line in enumerate(lines, 1):
        match = re.search(r'//>\s?(.*)', line)
        if match:
            comment = match.group(1).strip()
            # Try to find this in the ASM file by looking for matching instructions
            norm = re.sub(r'\s+', ' ', comment.lower()).strip()
            # Check if it's a known instruction from near our target
            for asm_ln in range(max(1, missing_asm_line - 50), min(len(asm_lines_raw), missing_asm_line + 50)):
                asm_text = asm_lines_raw[asm_ln - 1].strip().lower()
                asm_norm = re.sub(r'\s+', ' ', asm_text)
                if norm and norm == asm_norm:
                    asm_to_kt.append((asm_ln, i, comment))
                    break

    asm_to_kt.sort(key=lambda x: x[0])

    # Find the before and after positions
    for asm_ln, kt_ln, _ in asm_to_kt:
        if asm_ln <= missing_asm_line:
            before_pos = kt_ln
        elif after_pos is None:
            after_pos = kt_ln
            break

    # Decide insertion point
    if before_pos is not None:
        insert_at = before_pos + 1  # Insert right after the preceding //> comment
        # But skip forward past any non-blank non-comment lines that are part of the same statement
        while insert_at <= len(lines):
            test_line = lines[insert_at - 1].strip() if insert_at <= len(lines) else ""
            if not test_line or test_line.startswith('//'):
                break
            # If next line is code, insert before it
            break
    elif after_pos is not None:
        insert_at = after_pos  # Insert just before the following //> comment
    else:
        return None, "    "

    # Determine indentation from surrounding lines
    if 1 <= insert_at <= len(lines):
        surrounding = lines[insert_at - 1]
        indent_match = re.match(r'^(\s*)', surrounding)
        indent = indent_match.group(1) if indent_match else "    "
    else:
        indent = "    "

    return insert_at, indent


def process_file(kt_filepath, missing_items):
    """Add missing ASM comments to a Kotlin file.
    missing_items: [(asm_line_num, text, category, routine_name)]
    """
    lines, label_positions, asm_comment_map = parse_kotlin_file(kt_filepath)

    # Filter out duplicates
    to_insert = []
    for asm_ln, text, cat, routine in missing_items:
        if not is_duplicate(text, asm_comment_map):
            to_insert.append((asm_ln, text, cat, routine))

    if not to_insert:
        return 0

    # Find insertion points for each
    insertions = []  # [(kotlin_line, asm_text, indent)]
    for asm_ln, text, cat, routine in to_insert:
        kt_line, indent = find_insertion_point(asm_ln, text, cat, routine,
                                                lines, label_positions, asm_labels)
        if kt_line is not None:
            # Format the comment
            if cat == "comment":
                # ASM comments start with ; - keep them as-is after //>
                comment_line = f"{indent}//> {text}\n"
            else:
                comment_line = f"{indent}//> {text}\n"
            insertions.append((kt_line, comment_line, asm_ln))

    if not insertions:
        return 0

    # Sort by kotlin line (descending) to insert from bottom to top
    insertions.sort(key=lambda x: (-x[0], x[2]))

    # Insert lines (from bottom to top to preserve line numbers)
    for kt_line, comment_line, _ in insertions:
        idx = kt_line - 1  # 0-based
        if idx < 0:
            idx = 0
        if idx > len(lines):
            idx = len(lines)
        lines.insert(idx, comment_line)

    # Write back
    with open(kt_filepath, 'w') as f:
        f.writelines(lines)

    return len(insertions)


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    # Parse the missing_lines.txt to get all missing items
    with open(os.path.join(PROJECT_ROOT, "tmp", "missing_lines.txt")) as f:
        content = f.read()

    missing_by_routine = {}
    current_routine = None
    for line in content.split('\n'):
        m = re.match(r'^=== (\w+) \(asm line (\d+)\) ===$', line)
        if m:
            current_routine = m.group(1)
            missing_by_routine[current_routine] = []
            continue
        m2 = re.match(r'^\s+(\d+) \[(\w+\s*)\]: (.+)$', line)
        if m2 and current_routine:
            ln = int(m2.group(1))
            cat = m2.group(2).strip()
            text = m2.group(3)
            missing_by_routine[current_routine].append((ln, cat, text))

    # Map routines to Kotlin files
    kt_files = {}
    for root, dirs, files in os.walk(KOTLIN_DIR):
        for fname in sorted(files):
            if not fname.endswith('.kt'):
                continue
            fpath = os.path.join(root, fname)
            rel = os.path.relpath(fpath, KOTLIN_DIR)
            with open(fpath) as f:
                kt_files[rel] = (fpath, f.read())

    routine_to_file = {}
    for routine in missing_by_routine:
        for rel, (fpath, content) in kt_files.items():
            if f'//> {routine}:' in content or f'//> {routine} ' in content or f'//> {routine}\n' in content:
                routine_to_file[routine] = fpath
                break
        if routine not in routine_to_file:
            for rel, (fpath, content) in kt_files.items():
                # Case-insensitive search but only in //> comments
                pattern = re.compile(r'//>\s*' + re.escape(routine), re.IGNORECASE)
                if pattern.search(content):
                    routine_to_file[routine] = fpath
                    break
        if routine not in routine_to_file:
            # Search in all content
            for rel, (fpath, content) in kt_files.items():
                if routine in content:
                    routine_to_file[routine] = fpath
                    break

    # Group missing items by Kotlin file
    file_to_missing = defaultdict(list)
    unmapped = []
    for routine, items in missing_by_routine.items():
        if routine in routine_to_file:
            fpath = routine_to_file[routine]
            for ln, cat, text in items:
                file_to_missing[fpath].append((ln, text, cat, routine))
        else:
            for ln, cat, text in items:
                unmapped.append((routine, ln, cat, text))

    # Sort within each file by ASM line
    for fpath in file_to_missing:
        file_to_missing[fpath].sort(key=lambda x: x[0])

    # Process each file
    total_inserted = 0
    for fpath in sorted(file_to_missing.keys()):
        items = file_to_missing[fpath]
        rel = os.path.relpath(fpath, KOTLIN_DIR)
        count = process_file(fpath, items)
        print(f"{rel}: inserted {count}/{len(items)} comments")
        total_inserted += count

    print(f"\nTotal inserted: {total_inserted}")
    if unmapped:
        print(f"\nUnmapped routines ({len(unmapped)} items):")
        for routine, ln, cat, text in unmapped[:20]:
            print(f"  {routine} ASM {ln}: {text[:70]}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Extract missing ASM lines organized by target Kotlin file, with full context."""

import os
import re
from collections import defaultdict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASM_FILE = os.path.join(PROJECT_ROOT, "smbdism.asm")
KOTLIN_DIR = os.path.join(PROJECT_ROOT, "src", "main", "kotlin")

# Read the ASM file
with open(ASM_FILE) as f:
    asm_lines = f.readlines()

# Read missing_lines.txt and parse
with open(os.path.join(PROJECT_ROOT, "tmp", "missing_lines.txt")) as f:
    missing_content = f.read()

# Parse into structures: routine -> [(line_num, text, category)]
missing_by_routine = {}
current_routine = None
for line in missing_content.split('\n'):
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

# Routine -> Kotlin file mapping (from the map output)
# We'll search for each routine in Kotlin files
kt_files = {}
for root, dirs, files in os.walk(KOTLIN_DIR):
    for fname in sorted(files):
        if not fname.endswith('.kt'):
            continue
        fpath = os.path.join(root, fname)
        rel = os.path.relpath(fpath, KOTLIN_DIR)
        with open(fpath) as f:
            kt_files[rel] = f.read()

routine_to_file = {}
for routine in missing_by_routine:
    for rel, content in kt_files.items():
        if f'//> {routine}:' in content or f'//> {routine} ' in content or f'//> {routine}\n' in content:
            routine_to_file[routine] = rel
            break
    if routine not in routine_to_file:
        # case-insensitive search
        for rel, content in kt_files.items():
            if routine.lower() in content.lower():
                routine_to_file[routine] = rel
                break
    if routine not in routine_to_file:
        routine_to_file[routine] = "NOT_FOUND"

# Group by Kotlin file
file_to_missing = defaultdict(list)
for routine, items in missing_by_routine.items():
    kt_file = routine_to_file.get(routine, "NOT_FOUND")
    for ln, cat, text in items:
        file_to_missing[kt_file].append((routine, ln, cat, text))

# Sort within each file by asm line number
for kt_file in file_to_missing:
    file_to_missing[kt_file].sort(key=lambda x: x[1])

# Output one file per Kotlin file
out_dir = os.path.join(PROJECT_ROOT, "tmp", "missing_per_file")
os.makedirs(out_dir, exist_ok=True)

for kt_file, items in sorted(file_to_missing.items()):
    safe_name = kt_file.replace('/', '_').replace('.kt', '')
    with open(os.path.join(out_dir, f"{safe_name}.txt"), 'w') as f:
        f.write(f"# Missing ASM lines for {kt_file}\n")
        f.write(f"# {len(items)} lines to add\n\n")
        current_routine = None
        for routine, ln, cat, text in items:
            if routine != current_routine:
                f.write(f"\n--- Near {routine} ---\n")
                current_routine = routine
            f.write(f"  ASM {ln:5d} [{cat:14s}]: {text}\n")
    print(f"{kt_file}: {len(items)} missing lines -> {safe_name}.txt")

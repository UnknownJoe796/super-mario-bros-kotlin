#!/usr/bin/env python3
"""Map each missing ASM routine to its Kotlin file by searching for the routine name."""

import os
import re
from collections import defaultdict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KOTLIN_DIR = os.path.join(PROJECT_ROOT, "src", "main", "kotlin")

# Read the missing lines file
with open(os.path.join(PROJECT_ROOT, "tmp", "missing_lines.txt")) as f:
    content = f.read()

# Extract routine names
routines = re.findall(r'^=== (\w+) \(asm line (\d+)\) ===$', content, re.MULTILINE)

# Build a map of Kotlin file -> content
kt_files = {}
for root, dirs, files in os.walk(KOTLIN_DIR):
    for fname in sorted(files):
        if not fname.endswith('.kt'):
            continue
        fpath = os.path.join(root, fname)
        rel = os.path.relpath(fpath, KOTLIN_DIR)
        with open(fpath) as f:
            kt_files[rel] = f.read()

# For each routine, find which Kotlin file(s) contain it
routine_to_file = {}
for routine_name, asm_line in routines:
    found_in = []
    # Search for the routine name in //> comments or function names
    for rel, content in kt_files.items():
        # Check //> comments for the label
        if f'//> {routine_name}:' in content or f'//> {routine_name} ' in content:
            found_in.append(rel)
        # Check function definitions
        elif f'fun System.{routine_name[0].lower() + routine_name[1:]}' in content:
            found_in.append(rel)
        elif f'fun System.{re.sub(r"([A-Z])", lambda m: m.group(1).lower(), routine_name[0].lower() + routine_name[1:])}' in content:
            found_in.append(rel)

    if not found_in:
        # Try case-insensitive search
        for rel, content in kt_files.items():
            if routine_name.lower() in content.lower():
                found_in.append(rel)

    routine_to_file[routine_name] = found_in

# Group by Kotlin file
file_to_routines = defaultdict(list)
for routine, files in routine_to_file.items():
    if files:
        for f in files[:1]:  # Use first match
            file_to_routines[f].append(routine)
    else:
        file_to_routines["NOT_FOUND"].append(routine)

# Output
for kt_file in sorted(file_to_routines.keys()):
    routines_list = file_to_routines[kt_file]
    print(f"\n{kt_file} ({len(routines_list)} routines):")
    for r in routines_list:
        print(f"  {r}")

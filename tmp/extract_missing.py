#!/usr/bin/env python3
"""Extract all missing assembly lines grouped by Kotlin file they should be in."""

import os
import re
import sys
from collections import defaultdict

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASM_FILE = os.path.join(PROJECT_ROOT, "smbdism.asm")
KOTLIN_DIR = os.path.join(PROJECT_ROOT, "src", "main", "kotlin")

CODE_SECTION_START_LABEL = "GameMode"

def parse_asm_file(path):
    with open(path, 'r') as f:
        lines = f.readlines()
    all_labels = {}
    code_start_line = None
    for i, raw_line in enumerate(lines, 1):
        line = raw_line.rstrip('\n')
        m = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', line)
        if m:
            all_labels[m.group(1)] = i
            if m.group(1) == CODE_SECTION_START_LABEL and code_start_line is None:
                code_start_line = i
    return lines, all_labels, code_start_line

def categorize_asm_line(stripped):
    if not stripped:
        return "empty"
    if stripped.startswith(';---'):
        return "separator"
    if stripped.startswith(';'):
        return "comment"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*\s*=\s*', stripped):
        return "define"
    if re.match(r'^\s*\.dw\b', stripped, re.IGNORECASE):
        return "dw_directive"
    if re.match(r'^\s*\.(db|org|ds)\b', stripped, re.IGNORECASE):
        return "db_directive"
    if re.match(r'^[A-Za-z_][A-Za-z0-9_]*:', stripped):
        return "label"
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

def parse_kotlin_files(kotlin_dir):
    asm_comments = []
    all_text = []
    label_set = set()
    all_kotlin_content = ""
    for root, dirs, files in os.walk(kotlin_dir):
        for fname in sorted(files):
            if not fname.endswith('.kt'):
                continue
            fpath = os.path.join(root, fname)
            rel_path = os.path.relpath(fpath, kotlin_dir)
            with open(fpath, 'r') as f:
                content = f.read()
                all_kotlin_content += content + "\n"
                for line_num, line in enumerate(content.split('\n'), 1):
                    match = re.search(r'//>\s?(.*)', line)
                    if match:
                        comment = match.group(1).rstrip()
                        asm_comments.append((rel_path, line_num, comment))
                        all_text.append(comment)
                        lm = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', comment)
                        if lm:
                            label_set.add(lm.group(1))
                        lm2 = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):\s+\w', comment)
                        if lm2:
                            label_set.add(lm2.group(1))
    norm_set = set()
    for c in all_text:
        norm_set.add(re.sub(r'\s+', ' ', c.strip().lower()))
    texts_lower = [re.sub(r'\s+', ' ', t.strip().lower()) for t in all_text]
    return asm_comments, all_text, norm_set, texts_lower, label_set, all_kotlin_content

def normalize(text):
    return re.sub(r'\s+', ' ', text.strip().lower())

def normalize_no_comment(text):
    s = re.sub(r'\s*;.*$', '', text).strip()
    return normalize(s)

def check_line(stripped, cat, norm_set, texts_lower, kotlin_labels, kotlin_content):
    norm = normalize(stripped)
    if norm in norm_set:
        return 'found'
    norm_nc = normalize_no_comment(stripped)
    if norm_nc and norm_nc in norm_set:
        return 'found'
    if cat == "label":
        m = re.match(r'^([A-Za-z_][A-Za-z0-9_]*):', stripped)
        if m and m.group(1) in kotlin_labels:
            return 'found'
    if cat == "instruction":
        instr_only = re.sub(r'^[A-Za-z_]\w*:\s+', '', stripped).strip()
        instr_norm = normalize_no_comment(instr_only)
        if instr_norm and instr_norm in norm_set:
            return 'found'
        if instr_norm:
            for kt in texts_lower:
                if instr_norm in kt:
                    return 'found'
    if cat == "dw_directive":
        m = re.match(r'^\s*\.dw\s+(\w+)', stripped)
        if m:
            target = m.group(1)
            if target in kotlin_labels or target in kotlin_content:
                return 'reformatted'
    if cat == "db_directive":
        hex_vals = re.findall(r'\$([0-9a-fA-F]+)', stripped)
        if hex_vals and len(hex_vals) >= 3:
            for kt in texts_lower:
                matches = sum(1 for h in hex_vals if h.lower() in kt)
                if matches >= len(hex_vals) * 0.5:
                    return 'reformatted'
            hex_0x = ['0x' + h.lower() for h in hex_vals]
            for h in hex_0x[:3]:
                if h in kotlin_content.lower():
                    return 'reformatted'
    if cat == "comment":
        comment_text = stripped.lstrip(';').strip()
        if len(comment_text) > 10:
            ct_lower = comment_text.lower()
            for kt in texts_lower:
                if ct_lower in kt:
                    return 'found'
            if ct_lower in kotlin_content.lower():
                return 'reformatted'
    return 'missing'

def main():
    lines_raw, all_labels, code_start = parse_asm_file(ASM_FILE)
    asm_comments, kt_texts, norm_set, texts_lower, kt_labels, kt_content = parse_kotlin_files(KOTLIN_DIR)

    # Build label -> routine mapping
    sorted_labels = sorted(all_labels.items(), key=lambda x: x[1])

    # Collect missing lines with their asm context
    missing_by_routine = defaultdict(list)

    for i in range(code_start - 1, len(lines_raw)):
        raw = lines_raw[i].rstrip('\n')
        stripped = raw.strip()
        if not stripped:
            continue
        cat = categorize_asm_line(stripped)
        if cat in ("empty", "separator", "define"):
            continue
        status = check_line(stripped, cat, norm_set, texts_lower, kt_labels, kt_content)
        if status == 'missing':
            # Find containing routine
            routine = "unknown"
            for idx, (name, ln) in enumerate(sorted_labels):
                if ln > i + 1:
                    break
                routine = name
            missing_by_routine[routine].append((i + 1, stripped, cat))

    # Output grouped by routine, sorted by asm line number
    for routine in sorted(missing_by_routine.keys(), key=lambda r: all_labels.get(r, 0)):
        items = missing_by_routine[routine]
        print(f"\n=== {routine} (asm line {all_labels.get(routine, '?')}) ===")
        for ln, text, cat in items:
            print(f"  {ln:5d} [{cat:14s}]: {text}")

if __name__ == "__main__":
    main()

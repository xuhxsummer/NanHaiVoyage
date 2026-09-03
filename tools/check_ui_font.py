#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
check_ui_font.py — keep the UI font honest.

The game bakes its fonts at startup with FreeTypeFontGenerator from
assets/fonts/nanhai-cjk.ttf, using the character set in
assets/fonts/ui-chars.txt (plus libGDX DEFAULT_CHARS).  Long ago the TTF was a
hand-made subset and the char list was hand-maintained, so newly added UI text
silently rendered as tofu boxes (足/·/， and friends).

This script closes that hole with two generated invariants:

  1. Scan every Java string/char literal under core/android/lwjgl3 and collect
     the real characters the UI can render (comments and log-only text included;
     over-collection is harmless, under-collection is the bug we kill).
  2. Write assets/fonts/ui-chars.txt from that scan (--write), then assert:
       scanned UI chars  ⊆  ui-chars.txt
       ui-chars.txt     ⊆  glyphs actually present in the .ttf cmap
     so no character the sources can render can ever go missing again.

Usage:
    python3 tools/check_ui_font.py            # check only (fails if anything missing)
    python3 tools/check_ui_font.py --write    # regenerate ui-chars.txt, then check
    python3 tools/check_ui_font.py --font X   # check a different font file
    python3 tools/check_ui_font.py --text "银两不足"   # also require extra literal text

Pure stdlib (no pip deps) so it runs on any dev box.
"""
import os
import re
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIRS = ["core/src/main/java", "android/src", "lwjgl3/src"]
DEFAULT_FONT = os.path.join(ROOT, "assets", "fonts", "nanhai-cjk.ttf")
CHARS_TXT = os.path.join(ROOT, "assets", "fonts", "ui-chars.txt")


# ---------------------------------------------------------------------------
# 1. Scan Java sources for every character inside string/char literals.
# ---------------------------------------------------------------------------

def scan_java_text(text):
    """Chars inside "..."/'...' literals of one .java file (comments stripped)."""
    out = []
    i, n = 0, len(text)
    state = "code"  # code | line | block | str | char
    buf = []

    def flush():
        if buf:
            out.extend(buf)
            del buf[:]

    while i < n:
        c = text[i]
        if state == "code":
            if c == "/" and i + 1 < n and text[i + 1] == "/":
                state = "line"
                i += 2
            elif c == "/" and i + 1 < n and text[i + 1] == "*":
                state = "block"
                i += 2
            elif c == '"':
                state = "str"
                i += 1
            elif c == "'":
                state = "char"
                i += 1
            else:
                i += 1
        elif state == "line":
            if c == "\n":
                state = "code"
            i += 1
        elif state == "block":
            if c == "*" and i + 1 < n and text[i + 1] == "/":
                state = "code"
                i += 2
            else:
                i += 1
        elif state == "str":
            if c == "\\":
                if i + 1 < n and text[i + 1] == "u" \
                        and re.fullmatch(r"[0-9a-fA-F]{4}", text[i + 2:i + 6]):
                    buf.append(chr(int(text[i + 2:i + 6], 16)))
                    i += 6
                else:
                    i += 2  # escaped quote/backslash/newline: skip the char
            elif c == '"':
                flush()
                state = "code"
                i += 1
            else:
                buf.append(c)
                i += 1
        elif state == "char":
            if c == "\\":
                if i + 1 < n and text[i + 1] == "u" \
                        and re.fullmatch(r"[0-9a-fA-F]{4}", text[i + 2:i + 6]):
                    buf.append(chr(int(text[i + 2:i + 6], 16)))
                    i += 6
                else:
                    i += 2
            elif c == "'":
                flush()
                state = "code"
                i += 1
            else:
                buf.append(c)
                i += 1
    return out


def scan_sources():
    chars = set()
    files = []
    for d in SRC_DIRS:
        base = os.path.join(ROOT, d)
        if not os.path.isdir(base):
            continue
        for dirpath, _dirs, names in os.walk(base):
            for name in names:
                if name.endswith(".java"):
                    files.append(os.path.join(dirpath, name))
    for path in sorted(files):
        with open(path, "r", encoding="utf-8") as f:
            chars.update(scan_java_text(f.read()))
    return chars


# ---------------------------------------------------------------------------
# 2. Which glyphs does the .ttf actually contain? (minimal TrueType/OpenType
#    cmap reader: format 4 + format 12, no external deps.)
# ---------------------------------------------------------------------------

def ttf_charset(path):
    with open(path, "rb") as f:
        data = f.read()
    if len(data) < 12 or data[:4] == b"ttcf":
        raise SystemExit(f"{path}: unsupported font container (not a bare sfnt)")
    num_tables = struct.unpack(">H", data[4:6])[0]
    tables = {}
    off = 12
    for _ in range(num_tables):
        if off + 16 > len(data):
            break
        tag = data[off:off + 4].decode("latin-1")
        toff, length = struct.unpack(">II", data[off + 8:off + 16])
        tables[tag] = (toff, length)
        off += 16
    if "cmap" not in tables:
        raise SystemExit(f"{path}: no cmap table")
    cmap_off, cmap_len = tables["cmap"]
    n_tables = struct.unpack(">H", data[cmap_off + 2:cmap_off + 4])[0]
    subs = []
    for i in range(n_tables):
        plat, enc, sub_off = struct.unpack(
            ">HHI", data[cmap_off + 4 + i * 8:cmap_off + 12 + i * 8])
        subs.append((plat, enc, cmap_off + sub_off))
    wanted = [(3, 10), (0, 4), (0, 3), (3, 1), (0, 6), (0, 5)]
    sub = None
    for plat, enc in wanted:
        for p, e, s in subs:
            if p == plat and e == enc:
                sub = s
                break
        if sub is not None:
            break
    if sub is None:
        sub = subs[0][2]
    fmt = struct.unpack(">H", data[sub:sub + 2])[0]
    chars = set()
    if fmt == 4:
        seg_count = struct.unpack(">H", data[sub + 6:sub + 8])[0] // 2
        end_base = sub + 14
        start_base = end_base + seg_count * 2 + 2  # + reservedPad
        for i in range(seg_count):
            start = struct.unpack(">H", data[start_base + i * 2:start_base + i * 2 + 2])[0]
            end = struct.unpack(">H", data[end_base + i * 2:end_base + i * 2 + 2])[0]
            if start == 0xFFFF:
                continue
            chars.update(range(start, end + 1))
    elif fmt == 12:
        n_groups = struct.unpack(">I", data[sub + 12:sub + 16])[0]
        for g in range(n_groups):
            base = sub + 16 + g * 12
            sc, ec, _sg = struct.unpack(">III", data[base:base + 12])
            chars.update(range(sc, min(ec, 0x10FFFF) + 1))
    else:
        raise SystemExit(f"{path}: unsupported cmap format {fmt}")
    return chars


# ---------------------------------------------------------------------------
# 3. Main
# ---------------------------------------------------------------------------

def main(argv):
    write = "--write" in argv
    font = DEFAULT_FONT
    extra_text = ""
    if "--font" in argv:
        font = os.path.join(ROOT, argv[argv.index("--font") + 1])
    if "--text" in argv:
        extra_text = argv[argv.index("--text") + 1]
    if not os.path.exists(font):
        raise SystemExit(f"font not found: {font}")

    ui = scan_sources()
    ui.update(extra_text)
    # Keep it clean: skip control chars and U+0020 (space ships in libGDX
    # DEFAULT_CHARS anyway; storing it would only confuse the round-trip read).
    ui = {c for c in ui if ord(c) > 0x20 and c not in "\n\r\t\0"}
    if not ui:
        raise SystemExit("no UI characters found — scan bug?")

    ordered = "".join(sorted(ui))

    if write:
        with open(CHARS_TXT, "w", encoding="utf-8") as f:
            f.write(ordered + "\n")
        print(f"wrote {CHARS_TXT} ({len(ordered)} unique chars)")

    chars_txt = ""
    if os.path.exists(CHARS_TXT):
        with open(CHARS_TXT, "r", encoding="utf-8") as f:
            chars_txt = "".join(f.read().split())

    # ttf_charset returns integer codepoints; compare as characters.
    font_chars = {chr(cp) for cp in ttf_charset(font)}
    missing_from_chars = sorted(ui - set(chars_txt))
    missing_from_font = sorted(ui - font_chars)

    print(f"UI chars in sources : {len(ui)}")
    print(f"chars in ui-chars.txt: {len(chars_txt)}")
    print(f"glyphs in {os.path.basename(font)}: {len(font_chars)}")

    ok = True
    if missing_from_chars:
        ok = False
        print("MISSING from ui-chars.txt (would tofu at runtime):")
        print("  " + "".join(missing_from_chars))
    if missing_from_font:
        ok = False
        print(f"MISSING from font cmap ({font}):")
        print("  " + "".join(missing_from_font))
    if ok:
        print("OK: every UI character the sources can render has a glyph.")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

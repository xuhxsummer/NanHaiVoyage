#!/usr/bin/env python3
# 0.28.25 E: paper-cutout half-body dialogue busts (transparent PNG).
# Flat Tang/nautical palette (parchment/ink/sea). NPC busts + 4 player busts
# bound to avatarIndex 0-3. Run from repo root: python3 tools/gen_busts.py
import math, os

OUT = os.path.join("assets", "ui", "dialogue")
W, H = 384, 512  # ~512px tall half-body

SKIN = (237, 199, 160, 255)
SKIN_SH = (214, 171, 134, 255)
INK = (43, 40, 48, 255)
INK_SOFT = (74, 68, 76, 255)
PARCH = (222, 194, 153, 255)
SEA = (41, 78, 88, 255)
GOLD = (197, 158, 82, 255)
RED = (142, 60, 56, 255)
TEAL = (58, 110, 106, 255)
TEAL_D = (44, 86, 84, 255)
WHITE = (235, 230, 220, 255)

def blank():
    return [[[0, 0, 0, 0] for _ in range(W)] for _ in range(H)]

def ellipse(img, cx, cy, rx, ry, col):
    x0, x1 = max(0, int(cx - rx)), min(W - 1, int(cx + rx))
    y0, y1 = max(0, int(cy - ry)), min(H - 1, int(cy + ry))
    for y in range(y0, y1 + 1):
        dy = (y - cy) / max(ry, 0.001)
        for x in range(x0, x1 + 1):
            dx = (x - cx) / max(rx, 0.001)
            if dx * dx + dy * dy <= 1.0:
                img[y][x] = col

def rect(img, x0, y0, w, h, col):
    for y in range(max(0, y0), min(H, y0 + h)):
        for x in range(max(0, x0), min(W, x0 + w)):
            img[y][x] = col

def tri(img, p0, p1, p2, col):
    def edge(a, b, p):
        return (b[0]-a[0])*(p[1]-a[1]) - (b[1]-a[1])*(p[0]-a[0])
    xmin = max(0, min(p0[0], p1[0], p2[0])); xmax = min(W-1, max(p0[0], p1[0], p2[0]))
    ymin = max(0, min(p0[1], p1[1], p2[1])); ymax = min(H-1, max(p0[1], p1[1], p2[1]))
    area = edge(p0, p1, p2)
    if area == 0: return
    for y in range(ymin, ymax+1):
        for x in range(xmin, xmax+1):
            p = (x+0.5, y+0.5)
            w0 = edge(p1, p2, p) / area
            w1 = edge(p2, p0, p) / area
            w2 = edge(p0, p1, p) / area
            if w0 >= -0.001 and w1 >= -0.001 and w2 >= -0.001:
                img[y][x] = col

def save(img, name):
    path = os.path.join(OUT, name)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", int(W).to_bytes(4, "big") + int(H).to_bytes(4, "big")
                      + bytes([8, 6, 0, 0, 0])))
        raw = b""
        for y in range(H):
            raw += b"\x00" + bytes(v for px in img[y] for v in px)
        comp = zlib.compress(raw, 9)
        f.write(chunk(b"IDAT", comp))
        f.write(chunk(b"IEND", b""))
    print("wrote", path)

def chunk(tag, data):
    return len(data).to_bytes(4, "big") + tag + data + (zlib.crc32(tag + data) & 0xffffffff).to_bytes(4, "big")

import zlib

def base_bust(robe, collar, robe_dark):
    """Half-body: head + shoulders/torso, transparent below the chest line."""
    img = blank()
    # Shoulders / torso
    ellipse(img, W//2, H-90, 150, 118, robe)
    ellipse(img, W//2 - 46, H-70, 52, 62, robe_dark)   # left sleeve hint
    ellipse(img, W//2 + 46, H-70, 52, 62, robe_dark)
    rect(img, W//2 - 26, H-208, 52, 150, robe)          # chest column
    ellipse(img, W//2, H-60, 120, 70, robe)
    # Collar (交领)
    tri(img, (W//2, H-210), (W//2-86, H-96), (W//2-6, H-96), collar)
    tri(img, (W//2, H-210), (W//2+86, H-96), (W//2+6, H-96), collar)
    # Neck
    rect(img, W//2-16, H-232, 32, 34, SKIN_SH)
    # Head
    ellipse(img, W//2, H-268, 64, 72, SKIN)
    ellipse(img, W//2, H-292, 62, 56, SKIN)  # rounded top
    # Hair / headwear base
    ellipse(img, W//2, H-322, 66, 40, INK)
    rect(img, W//2-64, H-330, 128, 26, INK)
    # Ears
    ellipse(img, W//2-64, H-268, 9, 13, SKIN_SH)
    ellipse(img, W//2+64, H-268, 9, 13, SKIN_SH)
    # Face dots are added per-variant via decorate()
    return img

def eyes(img, kind="dot"):
    y = H-262
    for dx in (-24, 24):
        if kind == "dot":
            ellipse(img, W//2+dx, y, 5, 6, INK)
        else:  # warm arc (smiling)
            for t in range(11):
                a = math.pi * t / 10
                ellipse(img, W//2+dx - int(9*math.cos(a)), y+3-int(6*math.sin(a)), 3, 3, INK)

def brows(img, dy=-14):
    for dx in (-24, 24):
        rect(img, W//2+dx-11, H-262+dy, 22, 5, INK)

def mouth(img, kind="line"):
    if kind == "line":
        rect(img, W//2-12, H-230, 24, 5, INK)
    elif kind == "smile":
        for t in range(15):
            a = math.pi * t / 14
            ellipse(img, W//2-14 + int(28*a/math.pi), H-234+int(7*math.sin(a)), 3, 3, INK)

def beard(img, long=False):
    ellipse(img, W//2, H-218, 26, 40 if long else 24, (150, 148, 146, 255))
    if long:
        ellipse(img, W//2, H-190, 18, 34, (150, 148, 148, 255))

def hat(img, col):
    ellipse(img, W//2, H-336, 78, 34, col)
    ellipse(img, W//2, H-352, 40, 26, col)
    ellipse(img, W//2, H-306, 66, 14, INK_SOFT)  # brim shadow band

def headwrap(img, col):
    ellipse(img, W//2, H-318, 68, 36, col)
    rect(img, W//2-70, H-320, 140, 20, col)

def hairbun(img, col):
    ellipse(img, W//2, H-330, 64, 38, col)
    ellipse(img, W//2, H-352, 20, 18, col)

def hairband(img, col):
    rect(img, W//2-62, H-306, 124, 9, col)

def ponytail(img, col):
    ellipse(img, W//2+62, H-296, 16, 30, col)
    ellipse(img, W//2+68, H-252, 12, 26, col)

def bust_zhanggui():
    img = base_bust(SEA, GOLD, (32, 62, 72, 255))
    hat(img, SEA)
    eyes(img); brows(img); mouth(img, "line"); beard(img, long=True)
    ellipse(img, W//2, H-150, 34, 18, GOLD)  # abacus/scale medallion
    return img

def bust_yi():
    img = base_bust(TEAL, PARCH, TEAL_D)
    headwrap(img, (232, 226, 214, 255))
    eyes(img); brows(img); mouth(img, "smile"); beard(img, False)
    ellipse(img, W//2-84, H-120, 16, 16, RED)  # medicine gourd
    return img

def bust_huashi():
    img = base_bust((92, 84, 96, 255), PARCH, (72, 64, 76, 255))
    hairbun(img, INK)
    hairband(img, (66, 60, 70, 255))
    eyes(img, "arc"); brows(img); mouth(img, "smile")
    rect(img, W//2+70, H-140, 10, 56, (120, 60, 46, 255))   # brush
    ellipse(img, W//2+75, H-84, 9, 12, INK)
    return img

def bust_laoren():
    img = base_bust((104, 100, 112, 255), PARCH, (86, 82, 94, 255))
    headwrap(img, (196, 188, 176, 255))
    eyes(img, "arc"); brows(img, -12); mouth(img, "smile"); beard(img, long=True)
    return img

def bust_player(i):
    robes = [(41, 78, 88, 255), (91, 52, 56, 255), (70, 93, 63, 255), (61, 66, 90, 255)]
    accents = [(155, 80, 59, 255), (80, 119, 138, 255), (194, 153, 82, 255), (107, 108, 160, 255)]
    img = base_bust(robes[i], accents[i], tuple(int(c*0.75) for c in robes[i]))
    if i % 2 == 0:
        hairbun(img, INK); hairband(img, accents[i])
    else:
        headwrap(img, INK); ponytail(img, INK)
    eyes(img); brows(img); mouth(img, "line")
    ellipse(img, W//2, H-132, 26, 14, accents[i])  # sash medallion
    return img

def main():
    os.makedirs(OUT, exist_ok=True)
    mapping = {
        "npc_zhanggui.png": bust_zhanggui,
        "npc_yi.png": bust_yi,
        "npc_huashi.png": bust_huashi,
        "npc_laoren.png": bust_laoren,
    }
    for name, fn in mapping.items():
        save(fn(), name)
    for i in range(4):
        save(bust_player(i), f"player_{i}.png")

if __name__ == "__main__":
    main()

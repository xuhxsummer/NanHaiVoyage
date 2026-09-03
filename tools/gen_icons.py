#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen_icons.py — 64x64 transparent pixel icons for NanHai Voyage (一期).
14 goods / 8 beasts / 8 herbs -> assets/textures/{goods,beasts,herbs}/<slug>.png
Deps: numpy + stdlib (PNG writer, Up filter). Run: python3 tools/gen_icons.py
"""
import math
import struct
import zlib

import numpy as np

S = 64
OUT = "assets/textures"

# ----------------------------------------------------------------------------
# PNG writer (RGBA, Up filter, zlib)
# ----------------------------------------------------------------------------
def write_png(path, rgba):
    rgba = np.ascontiguousarray(rgba.astype(np.uint8))
    h, w = rgba.shape[:2]
    stride = w * 4
    raw = bytearray()
    prev = np.zeros(stride, dtype=np.int32)
    for y in range(h):
        f0 = rgba[y].reshape(-1).astype(np.int32)
        raw.append(2)
        raw.extend(((f0 - prev) & 0xFF).astype(np.uint8).tobytes())
        prev = f0
    comp = zlib.compress(bytes(raw), 9)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
           + chunk(b"IDAT", comp) + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)


# ----------------------------------------------------------------------------
# Drawing kit
# ----------------------------------------------------------------------------
def icon():
    return np.zeros((S, S, 4), dtype=np.float64)

def px(img, x, y, c):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < S and 0 <= yi < S:
        img[yi, xi, 0] = c[0]; img[yi, xi, 1] = c[1]; img[yi, xi, 2] = c[2]; img[yi, xi, 3] = 255

def rect(img, x0, y0, x1, y1, c):
    for y in range(int(y0), int(y1) + 1):
        for x in range(int(x0), int(x1) + 1):
            px(img, x, y, c)

def disk(img, cx, cy, r, c):
    for y in range(S):
        for x in range(S):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(img, x, y, c)

def ellipse(img, cx, cy, rx, ry, c):
    for y in range(S):
        for x in range(S):
            dx, dy = (x - cx) / max(rx, 1e-9), (y - cy) / max(ry, 1e-9)
            if dx * dx + dy * dy <= 1.0:
                px(img, x, y, c)

def dome(img, cx, apex_y, rim_y, rim_rx, c):
    """Half-dome: apex at top, widening to rim width at rim_y."""
    for y in range(int(apex_y), int(rim_y) + 1):
        t = (y - apex_y) / max(rim_y - apex_y, 1e-9)
        hw = rim_rx * math.sqrt(max(t, 0.0))
        rect(img, cx - hw, y, cx + hw, y, c)

def line(img, x0, y0, x1, y1, c, w=1):
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(steps + 1):
        t = i / steps
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        off = w // 2
        for oy in range(-off, off + 1):
            for ox in range(-off, off + 1):
                if w <= 2 or (ox + oy) % 2 == 0 or w <= 3:
                    px(img, x + ox, y + oy, c)

def outline(img, c=(30, 24, 34)):
    """1px dark ring around the opaque silhouette (unified style)."""
    alpha = img[..., 3] > 0
    grow = alpha.copy()
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        grow |= np.roll(np.roll(alpha, dy, 0), dx, 1)
    ring = grow & ~alpha
    img[ring, 0], img[ring, 1], img[ring, 2], img[ring, 3] = c[0], c[1], c[2], 255


# Shared palette
WHITE = (240, 240, 238)
CREAM = (238, 226, 190)
TAN = (210, 176, 122)
BROWN = (140, 100, 58)
DBROWN = (92, 62, 34)
GREEN = (96, 150, 70)
DGREEN = (52, 96, 44)
LGREEN = (140, 190, 100)
RED = (200, 70, 60)
PINK = (232, 120, 130)
GOLD = (222, 168, 60)
GRAY = (150, 152, 158)
DGRAY = (86, 88, 96)
BLUE = (90, 140, 190)
LBLUE = (170, 205, 230)

# ----------------------------------------------------------------------------
# GOODS (14)
# ----------------------------------------------------------------------------
def i_silk():
    m = icon()
    ellipse(m, 30, 36, 17, 12, PINK)          # bolt roll
    rect(m, 18, 26, 42, 46, PINK)
    disk(m, 44, 36, 6, (245, 170, 175))       # spiral end
    disk(m, 44, 36, 2, (150, 40, 60))         # spiral core
    line(m, 16, 28, 40, 26, (245, 170, 175), 2)   # top highlight
    line(m, 44, 40, 56, 52, PINK, 3)          # ribbon tail
    line(m, 52, 50, 58, 56, (150, 40, 60), 2) # tail tip
    outline(m)
    return m

def i_porcelain():
    m = icon()
    rect(m, 27, 15, 37, 25, WHITE)            # neck
    ellipse(m, 32, 15, 7, 2.5, WHITE)         # mouth
    ellipse(m, 32, 42, 13, 15, WHITE)         # body
    rect(m, 26, 54, 38, 56, (208, 214, 220))  # foot
    disk(m, 25, 40, 3, (90, 130, 180))        # blue pattern
    disk(m, 39, 44, 3, (90, 130, 180))
    disk(m, 32, 49, 4, (90, 130, 180))
    line(m, 26, 30, 28, 46, (250, 252, 254), 1)   # gloss
    outline(m)
    return m

def i_tea():
    m = icon()
    line(m, 32, 54, 30, 20, DGREEN, 2)        # stem
    ellipse(m, 24, 36, 8, 5, GREEN)           # leaf 1
    line(m, 17, 36, 24, 36, DGREEN, 1)        # vein tip
    line(m, 24, 36, 31, 36, DGREEN, 1)
    ellipse(m, 40, 42, 8, 5, GREEN)           # leaf 2
    line(m, 33, 42, 47, 42, DGREEN, 1)
    disk(m, 31, 15, 3, LGREEN)                # bud
    outline(m)
    return m

def i_salt():
    m = icon()
    for y in range(33, 45):
        hw = 16 - (44 - y) * 1.25
        rect(m, 32 - hw, y, 32 + hw, y, WHITE)
    rect(m, 18, 45, 46, 46, (208, 216, 226))  # base shade
    px(m, 24, 36, (255, 255, 255)); px(m, 38, 34, (255, 255, 255))
    px(m, 30, 30, (216, 226, 238)); px(m, 42, 40, (216, 226, 238))
    outline(m)
    return m

def i_iron():
    m = icon()
    for i, y in enumerate(range(36, 49)):     # big ingot
        hw = 8 + (y - 36) * 0.42
        rect(m, 28 - hw, y, 28 + hw, y, GRAY if y < 46 else DGRAY)
    rect(m, 22, 33, 34, 36, (186, 188, 194))  # top face
    for y in range(26, 35):                   # small ingot
        hw = 5 + (y - 26) * 0.4
        rect(m, 45 - hw, y, 45 + hw, y, (168, 170, 178) if y < 33 else DGRAY)
    rect(m, 41, 24, 49, 26, (200, 202, 208))
    outline(m)
    return m

def i_rice():
    m = icon()
    ellipse(m, 32, 40, 13, 14, TAN)           # sack
    rect(m, 26, 24, 38, 30, (190, 156, 104))  # neck
    ellipse(m, 32, 23, 9, 4, CREAM)           # rice heap
    px(m, 28, 16, CREAM); px(m, 35, 14, CREAM); px(m, 40, 18, CREAM)
    line(m, 25, 28, 39, 28, BROWN, 2)         # tie
    px(m, 32, 38, (176, 142, 92)); px(m, 32, 44, (176, 142, 92))
    outline(m)
    return m

def i_sugarcane():
    m = icon()
    line(m, 26, 16, 26, 52, (120, 170, 80), 3)   # stalk 1
    line(m, 38, 22, 38, 54, (104, 156, 70), 3)   # stalk 2
    for y in (24, 32, 40, 48):
        line(m, 24, y, 28, y, DGREEN, 1)
        line(m, 36, y + 4, 40, y + 4, DGREEN, 1)
    line(m, 26, 16, 16, 10, LGREEN, 2)        # blades
    line(m, 26, 22, 14, 20, LGREEN, 2)
    line(m, 38, 22, 48, 14, LGREEN, 2)
    line(m, 38, 28, 50, 26, LGREEN, 2)
    outline(m)
    return m

def i_agarwood():
    m = icon()
    rect(m, 18, 34, 42, 48, (96, 66, 44))     # log
    rect(m, 18, 34, 42, 36, (120, 86, 56))
    disk(m, 44, 41, 7, (78, 52, 34))          # cut face rings
    disk(m, 44, 41, 5, (108, 74, 46))
    disk(m, 44, 41, 2, (58, 38, 26))
    px(m, 30, 28, GRAY); px(m, 29, 24, GRAY)
    px(m, 30, 20, GRAY); px(m, 28, 15, GRAY)  # incense smoke
    outline(m)
    return m

def i_sappanwood():
    m = icon()
    rect(m, 18, 42, 46, 48, (146, 62, 52))
    rect(m, 22, 34, 46, 40, (176, 80, 66))
    rect(m, 26, 26, 44, 32, (200, 100, 82))
    line(m, 18, 48, 46, 48, (110, 44, 38), 1)
    line(m, 22, 40, 46, 40, (136, 56, 46), 1)
    px(m, 20, 52, (176, 80, 66)); px(m, 44, 52, (176, 80, 66))
    outline(m)
    return m

def i_pepper():
    m = icon()
    disk(m, 25, 37, 7, (104, 72, 40))
    disk(m, 38, 32, 7, (128, 90, 50))
    disk(m, 33, 45, 7, (96, 66, 38))
    disk(m, 27, 25, 5, (128, 90, 50))
    disk(m, 42, 44, 5, (128, 90, 50))
    px(m, 23, 34, (168, 126, 74)); px(m, 36, 29, (178, 136, 82))
    px(m, 31, 42, (160, 118, 68)); px(m, 41, 42, (178, 136, 82))
    outline(m)
    return m

def i_ivory():
    m = icon()
    pts = [(20, 50), (27, 40), (34, 30), (41, 22), (47, 17)]
    ws = [9, 8, 6, 5, 4]
    for i in range(len(pts) - 1):
        line(m, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], CREAM, ws[i])
    disk(m, 20, 50, 4, (196, 178, 138))
    line(m, 30, 52, 42, 38, (222, 204, 164), 4)   # second tusk behind
    outline(m)
    return m

def i_pearl():
    m = icon()
    dome(m, 32, 24, 40, 17, (226, 160, 150))  # top shell
    line(m, 20, 40, 32, 26, (196, 128, 120), 1)
    line(m, 44, 40, 32, 26, (196, 128, 120), 1)
    ellipse(m, 32, 46, 17, 8, (214, 148, 140))    # bottom shell
    disk(m, 32, 42, 5, WHITE)                 # pearl
    px(m, 30, 40, (255, 255, 255))
    outline(m)
    return m

def i_tortoiseshell():
    m = icon()
    dome(m, 32, 20, 44, 18, (196, 130, 52))
    ellipse(m, 32, 45, 19, 4, (140, 88, 30))  # rim
    disk(m, 32, 30, 2.5, (120, 74, 24))
    disk(m, 24, 37, 2.5, (120, 74, 24))
    disk(m, 40, 37, 2.5, (120, 74, 24))
    disk(m, 32, 40, 2.5, (120, 74, 24))
    outline(m)
    return m

def i_betel():
    m = icon()
    ellipse(m, 26, 40, 7, 10, (150, 104, 52))
    ellipse(m, 40, 38, 7, 10, (176, 128, 66))
    ellipse(m, 33, 24, 7, 10, (128, 118, 58))
    line(m, 24, 34, 24, 47, (120, 82, 40), 1)
    line(m, 38, 32, 38, 45, (146, 100, 48), 1)
    line(m, 31, 18, 31, 31, (100, 92, 40), 1)
    px(m, 28, 36, (196, 150, 92)); px(m, 42, 34, (204, 158, 100))
    outline(m)
    return m

# ----------------------------------------------------------------------------
# GOODS extra (0.26.0: 10 more, total 24)
# ----------------------------------------------------------------------------
def i_cotton():
    m = icon()
    rect(m, 16, 26, 46, 44, (238, 240, 242))     # folded cloth stack
    rect(m, 16, 26, 46, 29, (206, 212, 222))
    rect(m, 16, 36, 46, 39, (206, 212, 222))
    for x in (20, 26, 32, 38, 44):               # blue stitch dashes
        px(m, x, 33, (96, 130, 180)); px(m, x, 31, (150, 180, 220))
    line(m, 22, 44, 22, 47, (206, 212, 222), 1)  # fold shading
    line(m, 42, 44, 42, 47, (206, 212, 222), 1)
    disk(m, 24, 18, 5, WHITE)                    # cotton bolls
    disk(m, 32, 14, 6, WHITE)
    disk(m, 40, 18, 4.5, WHITE)
    px(m, 30, 13, (216, 222, 230)); px(m, 26, 16, (216, 222, 230))
    outline(m)
    return m

def i_lacquer():
    m = icon()
    dome(m, 32, 24, 40, 16, (178, 48, 40))       # lacquer bowl
    ellipse(m, 32, 40, 16, 5.5, (178, 48, 40))   # rim
    ellipse(m, 32, 40, 12, 3.5, (70, 24, 22))    # inner dark
    line(m, 17, 38, 47, 38, (222, 168, 60), 1)   # gold band
    rect(m, 26, 20, 38, 24, (140, 36, 32))       # waist ring
    rect(m, 22, 12, 42, 15, (120, 28, 26))       # foot + base
    rect(m, 24, 10, 40, 12, (60, 18, 16))
    line(m, 24, 36, 27, 28, (230, 120, 110), 2)  # gloss
    outline(m)
    return m

def i_bronze():
    m = icon()
    ellipse(m, 32, 36, 14, 11, (172, 124, 66))   # bronze cauldron belly
    rect(m, 18, 32, 46, 36, (172, 124, 66))
    ellipse(m, 32, 46, 14, 3.5, (172, 124, 66))  # rim
    ellipse(m, 32, 46, 10, 2.3, (84, 52, 28))     # opening
    rect(m, 12, 41, 17, 46, (172, 124, 66))       # loop handles
    rect(m, 47, 41, 52, 46, (172, 124, 66))
    rect(m, 20, 18, 23, 32, (140, 98, 50))       # three legs
    rect(m, 41, 18, 44, 32, (140, 98, 50))
    rect(m, 30, 15, 34, 32, (140, 98, 50))
    line(m, 20, 40, 44, 40, (120, 80, 44), 1)    # banding
    px(m, 22, 28, (226, 190, 130)); px(m, 42, 28, (226, 190, 130))
    outline(m)
    return m

def i_glass():
    m = icon()
    ellipse(m, 32, 42, 13, 12, (120, 200, 210))  # 琉璃 jar body
    rect(m, 26, 30, 38, 34, (150, 215, 222))
    rect(m, 27, 22, 37, 30, (170, 225, 230))     # neck
    ellipse(m, 32, 22, 8, 2.5, (210, 240, 242))   # lip
    ellipse(m, 32, 54, 12, 3, (170, 225, 230))    # rounded foot
    line(m, 23, 40, 26, 30, (235, 250, 250), 2)   # gloss streaks
    line(m, 41, 46, 41, 32, (200, 236, 240), 1)
    px(m, 36, 26, (250, 255, 255))
    outline(m)
    return m

def i_frankincense():
    m = icon()
    disk(m, 24, 36, 7, (240, 224, 178))          # pale resin tears
    disk(m, 34, 31, 8, (247, 234, 200))
    disk(m, 41, 39, 6, (236, 216, 168))
    disk(m, 29, 21, 4.5, (243, 228, 188))
    disk(m, 39, 23, 3.5, (245, 232, 196))
    px(m, 33, 28, (255, 250, 225)); px(m, 23, 33, (255, 250, 225))
    px(m, 40, 37, (255, 250, 225))
    outline(m)
    return m

def i_myrrh():
    m = icon()
    disk(m, 23, 37, 7, (150, 80, 46))            # dark reddish resin
    disk(m, 34, 33, 8, (172, 96, 56))
    disk(m, 41, 41, 6, (140, 72, 42))
    disk(m, 31, 23, 4.5, (168, 92, 52))
    px(m, 36, 31, (216, 152, 100)); px(m, 21, 35, (204, 138, 88))
    px(m, 40, 39, (196, 128, 82))
    outline(m)
    return m

def i_cardamom():
    m = icon()
    ellipse(m, 21, 38, 6, 10, (128, 168, 84))    # green pods
    ellipse(m, 33, 40, 7, 11, (108, 150, 72))
    ellipse(m, 45, 36, 6, 10, (134, 174, 90))
    line(m, 21, 30, 21, 46, (88, 128, 58), 1)    # pod ridges
    line(m, 33, 31, 33, 49, (76, 116, 50), 1)
    line(m, 45, 28, 45, 44, (94, 134, 62), 1)
    px(m, 24, 31, (176, 210, 128)); px(m, 36, 33, (168, 202, 120))
    px(m, 48, 29, (180, 214, 132))
    outline(m)
    return m

def i_clove():
    m = icon()
    line(m, 20, 14, 17, 34, (122, 84, 62), 3)    # clove stems
    line(m, 32, 14, 32, 30, (128, 88, 64), 3)
    line(m, 44, 16, 46, 34, (122, 84, 62), 3)
    disk(m, 16, 36, 5, (150, 96, 66))            # dried heads
    disk(m, 32, 32, 5.5, (158, 102, 70))
    disk(m, 47, 36, 5, (150, 96, 66))
    px(m, 16, 38, (198, 140, 96)); px(m, 32, 34, (206, 148, 100))
    px(m, 47, 38, (198, 140, 96))
    line(m, 25, 16, 25, 22, (100, 70, 52), 1)
    outline(m)
    return m

def i_coral():
    m = icon()
    rect(m, 26, 10, 38, 17, (156, 62, 52))       # base stump
    line(m, 32, 17, 32, 38, (214, 72, 60), 4)    # trunk
    line(m, 32, 32, 19, 24, (214, 72, 60), 3)    # branches
    line(m, 32, 32, 45, 24, (214, 72, 60), 3)
    line(m, 19, 24, 14, 17, (196, 62, 54), 2)
    line(m, 45, 24, 50, 17, (196, 62, 54), 2)
    line(m, 32, 38, 26, 48, (214, 72, 60), 2)    # top twigs
    line(m, 32, 38, 39, 49, (214, 72, 60), 2)
    px(m, 30, 35, (252, 140, 120)); px(m, 20, 25, (246, 128, 110))
    px(m, 44, 25, (246, 128, 110))
    outline(m)
    return m

def i_rhinohorn():
    m = icon()
    line(m, 29, 52, 29, 36, (188, 150, 100), 7)  # thick horn base
    line(m, 29, 36, 31, 24, (200, 164, 112), 5)  # taper
    line(m, 31, 24, 34, 14, (212, 178, 126), 3)  # curved tip
    line(m, 24, 46, 34, 46, (150, 116, 76), 1)   # growth rings
    line(m, 25, 39, 33, 39, (150, 116, 76), 1)
    line(m, 26, 32, 34, 32, (150, 116, 76), 1)
    line(m, 27, 25, 35, 25, (150, 116, 76), 1)
    px(m, 32, 43, (240, 210, 160))
    outline(m)
    return m

# ----------------------------------------------------------------------------
# BEASTS (8)
# ----------------------------------------------------------------------------
def i_jingwei():
    m = icon()
    disk(m, 29, 36, 8, WHITE)                 # body
    ellipse(m, 26, 36, 6, 4, (170, 190, 205)) # wing
    disk(m, 38, 26, 5.5, (196, 74, 60))       # head
    px(m, 38, 22, WHITE)                      # crown dot
    px(m, 40, 25, (30, 26, 30))               # eye
    line(m, 43, 27, 49, 25, (226, 150, 60), 2)    # beak
    line(m, 49, 23, 53, 19, BROWN, 2)         # twig
    line(m, 23, 41, 14, 47, (200, 205, 210), 2)   # tail
    line(m, 25, 43, 18, 50, (200, 205, 210), 2)
    line(m, 29, 44, 29, 51, (196, 74, 60), 1) # legs
    line(m, 34, 43, 34, 50, (196, 74, 60), 1)
    outline(m)
    return m

def i_ninefox():
    m = icon()
    for i, (tx, ty) in enumerate([(8, 24), (5, 33), (6, 43), (10, 50)]):
        line(m, 22, 38, tx, ty, WHITE if i % 2 == 0 else (240, 190, 120), 3)
    ellipse(m, 30, 40, 11, 7, (224, 130, 50))     # body
    disk(m, 41, 30, 6, (224, 130, 50))            # head
    line(m, 38, 25, 36, 18, (224, 130, 50), 2)    # ears
    line(m, 44, 25, 46, 18, (224, 130, 50), 2)
    px(m, 36, 19, WHITE); px(m, 46, 19, WHITE)
    line(m, 46, 31, 51, 32, (240, 190, 120), 1)   # snout
    px(m, 43, 29, (30, 26, 30))                   # eye
    line(m, 25, 46, 25, 52, (190, 106, 40), 2)    # legs
    line(m, 35, 46, 35, 52, (190, 106, 40), 2)
    outline(m)
    return m

def i_gudiao():
    m = icon()
    for i, (wx, wy) in enumerate([(12, 20), (9, 29), (11, 38)]):
        line(m, 26, 33, wx, wy, (110, 76, 46), 3)     # spread wing
    disk(m, 28, 37, 9, (140, 98, 58))             # body
    disk(m, 38, 26, 6, (140, 98, 58))             # head
    line(m, 38, 20, 42, 13, WHITE, 2)             # horn
    line(m, 43, 26, 49, 27, (226, 168, 60), 2)    # beak
    line(m, 49, 27, 47, 30, (226, 168, 60), 1)
    px(m, 40, 24, WHITE); px(m, 40, 25, (30, 26, 30))
    line(m, 28, 45, 28, 51, (226, 168, 60), 2)    # talons
    line(m, 34, 45, 34, 51, (226, 168, 60), 2)
    outline(m)
    return m

def i_flyfish():
    m = icon()
    ellipse(m, 32, 40, 13, 6, BLUE)               # body
    disk(m, 44, 40, 5, (120, 165, 210))           # head
    px(m, 46, 38, (20, 22, 30))                   # eye
    line(m, 19, 40, 9, 33, BLUE, 2)               # tail
    line(m, 19, 40, 9, 47, BLUE, 2)
    line(m, 9, 33, 13, 40, BLUE, 1); line(m, 9, 47, 13, 40, BLUE, 1)
    ellipse(m, 30, 29, 10, 4, LBLUE)              # wing 1
    ellipse(m, 36, 33, 8, 3, (200, 224, 240))     # wing 2
    ellipse(m, 32, 34, 6, 2, (200, 224, 240))     # dorsal
    outline(m)
    return m

def i_changyou():
    m = icon()
    ellipse(m, 30, 40, 10, 11, (120, 72, 60))     # body
    disk(m, 32, 24, 8, (120, 72, 60))             # head
    disk(m, 32, 26, 5, (206, 170, 140))           # face
    px(m, 29, 25, (30, 26, 30)); px(m, 35, 25, (30, 26, 30))
    disk(m, 24, 18, 2.5, (120, 72, 60)); disk(m, 40, 18, 2.5, (120, 72, 60))
    line(m, 22, 36, 13, 46, (120, 72, 60), 3)     # arms
    line(m, 38, 36, 47, 46, (120, 72, 60), 3)
    line(m, 26, 50, 24, 56, (120, 72, 60), 3)     # legs
    line(m, 34, 50, 36, 56, (120, 72, 60), 3)
    outline(m)
    return m

def i_xingxing():
    m = icon()
    ellipse(m, 32, 38, 9, 10, (196, 120, 60))
    ellipse(m, 32, 40, 5, 6, (230, 180, 130))     # belly
    disk(m, 32, 22, 7.5, (196, 120, 60))
    disk(m, 32, 24, 4.5, (230, 180, 130))
    px(m, 29, 23, (30, 26, 30)); px(m, 35, 23, (30, 26, 30))
    disk(m, 25, 16, 2.5, (196, 120, 60)); disk(m, 39, 16, 2.5, (196, 120, 60))
    line(m, 24, 32, 15, 22, (196, 120, 60), 3)    # raised arms
    line(m, 40, 32, 49, 22, (196, 120, 60), 3)
    line(m, 28, 47, 28, 54, (170, 100, 48), 2)
    line(m, 36, 47, 36, 54, (170, 100, 48), 2)
    outline(m)
    return m

def i_baize():
    m = icon()
    ellipse(m, 29, 38, 12, 8, WHITE)              # body
    disk(m, 43, 30, 6.5, WHITE)                   # head
    line(m, 40, 23, 36, 16, (208, 212, 220), 2)   # mane
    line(m, 45, 23, 48, 16, (208, 212, 220), 2)
    line(m, 48, 25, 53, 19, GOLD, 2)              # horn
    px(m, 45, 29, (30, 26, 30))                   # eye
    px(m, 44, 25, GOLD)                           # brow mark
    line(m, 21, 45, 21, 53, WHITE, 2)             # legs
    line(m, 28, 45, 28, 53, WHITE, 2)
    line(m, 36, 45, 36, 53, WHITE, 2)
    line(m, 17, 40, 9, 46, (208, 212, 220), 2)    # tail
    outline(m)
    return m

def i_turtle3():
    m = icon()
    dome(m, 30, 24, 44, 15, (90, 132, 80))
    ellipse(m, 30, 45, 16, 4, (60, 96, 56))       # rim
    disk(m, 30, 32, 2.5, (48, 76, 44))
    disk(m, 23, 39, 2.5, (48, 76, 44))
    disk(m, 37, 39, 2.5, (48, 76, 44))
    disk(m, 45, 39, 5, (120, 160, 100))           # head
    px(m, 47, 37, (20, 24, 22))
    line(m, 22, 48, 22, 56, (90, 132, 80), 3)     # three legs only
    line(m, 38, 48, 38, 56, (90, 132, 80), 3)
    line(m, 30, 48, 30, 55, (90, 132, 80), 3)
    outline(m)
    return m

# ----------------------------------------------------------------------------
# HERBS (8)
# ----------------------------------------------------------------------------
def i_ginseng():
    m = icon()
    line(m, 32, 26, 32, 44, (224, 190, 140), 5)   # root body
    line(m, 32, 42, 23, 54, (224, 190, 140), 3)   # legs
    line(m, 32, 42, 41, 54, (224, 190, 140), 3)
    line(m, 32, 44, 32, 56, (224, 190, 140), 2)
    line(m, 32, 33, 23, 39, (224, 190, 140), 2)   # arms
    line(m, 32, 33, 41, 39, (224, 190, 140), 2)
    line(m, 32, 26, 32, 17, GREEN, 2)             # sprout
    ellipse(m, 25, 14, 4, 2.5, GREEN)
    ellipse(m, 39, 14, 4, 2.5, GREEN)
    disk(m, 32, 12, 2, RED)
    outline(m)
    return m

def i_lingzhi():
    m = icon()
    ellipse(m, 32, 34, 16, 9, (168, 62, 44))      # cap
    rect(m, 18, 34, 46, 38, (168, 62, 44))
    rect(m, 28, 41, 36, 54, (150, 112, 74))       # stem
    line(m, 19, 30, 34, 26, (214, 110, 84), 2)    # gloss
    line(m, 20, 40, 44, 40, (120, 42, 30), 1)
    outline(m)
    return m

def i_fuling():
    m = icon()
    disk(m, 32, 40, 12, (198, 172, 132))
    disk(m, 25, 36, 4, (222, 200, 162))
    disk(m, 38, 44, 4, (226, 208, 172))
    disk(m, 23, 46, 3, (198, 172, 132))
    line(m, 40, 32, 48, 25, (140, 110, 72), 2)    # rootlet
    px(m, 30, 46, (170, 142, 104))
    outline(m)
    return m

def i_danggui():
    m = icon()
    line(m, 32, 30, 32, 48, (218, 184, 130), 4)   # root
    line(m, 32, 40, 24, 52, (218, 184, 130), 2)
    line(m, 32, 44, 40, 54, (218, 184, 130), 2)
    line(m, 32, 26, 32, 12, GREEN, 2)             # stem
    line(m, 32, 22, 25, 17, GREEN, 2)             # leaflets
    line(m, 32, 22, 39, 17, GREEN, 2)
    line(m, 32, 16, 26, 11, GREEN, 2)
    line(m, 32, 16, 38, 11, GREEN, 2)
    outline(m)
    return m

def i_heshouwu():
    m = icon()
    disk(m, 25, 42, 8, (166, 122, 78))
    disk(m, 39, 38, 8, (186, 142, 92))
    rect(m, 29, 38, 35, 45, (176, 132, 84))       # join
    line(m, 36, 30, 36, 19, GREEN, 2)             # sprout
    line(m, 36, 24, 30, 19, GREEN, 2)
    line(m, 36, 24, 42, 19, GREEN, 2)
    line(m, 19, 48, 14, 54, (140, 100, 62), 1)
    line(m, 45, 44, 50, 50, (140, 100, 62), 1)
    outline(m)
    return m

def i_guizhi():
    m = icon()
    line(m, 20, 46, 36, 24, (196, 150, 96), 4)    # three sticks
    line(m, 28, 50, 44, 28, (216, 172, 116), 4)
    line(m, 36, 52, 50, 32, (176, 132, 82), 4)
    disk(m, 37, 23, 2.5, (226, 188, 138))         # curled ends
    disk(m, 45, 27, 2.5, (232, 196, 148))
    disk(m, 51, 31, 2.5, (200, 156, 104))
    line(m, 27, 40, 39, 44, (178, 60, 50), 2)     # red tie
    outline(m)
    return m

def i_gancao():
    m = icon()
    for i, x in enumerate((25, 29, 33, 37, 41)):
        xo = 1 if i % 2 == 0 else -1
        line(m, x, 26, x + xo, 54, (214, 176, 120), 2)
        px(m, x + xo, 55, (232, 200, 150))
    disk(m, 24, 21, 3, GREEN)
    disk(m, 32, 17, 3.5, GREEN)
    disk(m, 40, 21, 3, GREEN)
    disk(m, 32, 20, 2, LGREEN)
    outline(m)
    return m

def i_chrysanthemum():
    m = icon()
    for cx, cy in ((32, 18), (42, 22), (46, 30), (42, 38), (32, 42),
                   (22, 38), (18, 30), (22, 22)):
        disk(m, cx, cy, 6, (238, 210, 90))
    for cx, cy in ((38, 19), (44, 26), (44, 34), (38, 40),
                   (26, 40), (20, 34), (20, 26), (26, 19)):
        disk(m, cx, cy, 4.5, (248, 228, 130))
    disk(m, 32, 30, 5, (214, 140, 46))            # center
    line(m, 32, 42, 32, 56, GREEN, 2)             # stem
    line(m, 32, 48, 23, 53, GREEN, 2)             # leaf
    outline(m)
    return m


GOODS = {
    "silk": i_silk, "porcelain": i_porcelain, "tea": i_tea, "salt": i_salt,
    "iron": i_iron, "rice": i_rice, "sugarcane": i_sugarcane,
    "agarwood": i_agarwood, "sappanwood": i_sappanwood, "pepper": i_pepper,
    "ivory": i_ivory, "pearl": i_pearl, "tortoiseshell": i_tortoiseshell,
    "betel": i_betel,
    "cotton": i_cotton, "lacquer": i_lacquer, "bronze": i_bronze,
    "glass": i_glass, "frankincense": i_frankincense, "myrrh": i_myrrh,
    "cardamom": i_cardamom, "clove": i_clove, "coral": i_coral,
    "rhinohorn": i_rhinohorn,
}
BEASTS = {
    "jingwei": i_jingwei, "nine-tail-fox": i_ninefox, "gu-diao": i_gudiao,
    "flying-fish": i_flyfish, "chang-you": i_changyou, "xing-xing": i_xingxing,
    "bai-ze": i_baize, "three-leg-turtle": i_turtle3,
}
HERBS = {
    "ginseng": i_ginseng, "lingzhi": i_lingzhi, "fuling": i_fuling,
    "danggui": i_danggui, "heshouwu": i_heshouwu, "guizhi": i_guizhi,
    "gancao": i_gancao, "chrysanthemum": i_chrysanthemum,
}

if __name__ == "__main__":
    n = 0
    for folder, table in (("goods", GOODS), ("beasts", BEASTS), ("herbs", HERBS)):
        for slug, fn in table.items():
            img = fn()
            alpha = img[..., 3]
            assert alpha.sum() > 300, f"{slug}: nearly empty"
            assert alpha.max() <= 255
            # no stray pixels touching the border
            assert alpha[0].sum() == 0 and alpha[-1].sum() == 0
            assert alpha[:, 0].sum() == 0 and alpha[:, -1].sum() == 0
            write_png(f"{OUT}/{folder}/{slug}.png", img)
            n += 1
    print(f"generated {n} icons")

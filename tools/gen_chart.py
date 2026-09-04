#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen_chart.py — pixel-art 南海海图 for the v0.26.5 full-map overlay.

Draws a stylised South China Sea chart that exactly overlays the game world
(Catalog.WORLD_W x WORLD_H = 4800 x 3600), so port/island markers and labels
drawn at world coords land on the matching pixels.  Land shapes mirror the
rivers/coasts the game world already uses (mainland top band, Indochina west,
Hainan centre, 扬州 river-mouth inset), rendered at 960x720 with:

  * deep-sea base + darker depth gradient toward the far south-east
  * lighter turquoise shallows just off every coast (1-2 px rim)
  * warm sand strand where land meets sea, green-brown land interior
  * faint cartouche grid lines, Xisha/Nansha reef speckle, a few sea-lane arcs
  * an ornate compass rose in the empty south-east corner

Output: assets/textures/chart.png (opaque, drawn with a 4:3 world projection,
row 0 = world NORTH so libGDX y-up drawing matches world y-up).

Dep: numpy + stdlib PNG writer (same writer as gen_icons.py).
Run: python3 tools/gen_chart.py
"""

import math
import os
import struct
import zlib

import numpy as np

OUT = os.path.join("assets", "textures", "chart.png")

W, H = 960, 720          # output pixels (4:3)
WW, WH = 4800.0, 3600.0  # game world units

# ----------------------------------------------------------------------------
# small pure-python RGBA canvas (rows 0..H-1, row 0 = top / world north)
# ----------------------------------------------------------------------------


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 3), dtype=np.float64)

    def fill(self, rgb):
        self.a[:, :] = rgb

    # NOTE on orientation: libGDX/SpriteBatch draws an image with pixel row 0 at
    # the TOP of the drawn quad, which corresponds to the world's NORTH edge
    # (world y = WH).  World coords are y-up, so the vertical transform is
    # row = (WH - wy) / WH * H, i.e. FLIPPED, and row 0 sits at wy = WH.
    def _px(self, x, wy):
        return (int(round(x)), int(round((WH - wy) / WH * (self.h - 1))))

    def rect(self, x0, y0, x1, y1, rgb):
        """World rect (y up, y0 < y1). Pixels inclusive."""
        xa = int(max(0, min(self.w - 1, round(x0))))
        xb = int(max(0, min(self.w - 1, round(x1))))
        pya = (WH - y1) / WH * (self.h - 1)  # world y1 (north) is a SMALLER row
        pyb = (WH - y0) / WH * (self.h - 1)
        ya = int(max(0, min(self.h - 1, round(pya))))
        yb = int(max(0, min(self.h - 1, round(pyb))))
        ya, yb = min(ya, yb), max(ya, yb)
        self.a[ya:yb + 1, xa:xb + 1] = rgb

    def disk(self, cx, cy, rw, rgb):
        """World-space circle (radius rw in world units -> pixels)."""
        sx = self.w / WW
        sy = self.h / WH
        cx_px = cx * sx
        rpx = max(rw * sx, 0.5)
        x0 = int(max(0, math.floor(cx_px - rpx)))
        x1 = int(min(self.w - 1, math.ceil(cx_px + rpx)))
        yc = (WH - cy) / WH * (self.h - 1)
        y0 = int(max(0, math.floor(yc - rpx)))
        y1 = int(min(self.h - 1, math.ceil(yc + rpx)))
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                dx, dy = x - cx_px, y - yc
                if dx * dx + dy * dy <= rpx * rpx:
                    self.a[y, x] = rgb

    def ellipse(self, cx, cy, rxw, ryw, rgb):
        sx = self.w / WW
        cx_px = cx * sx
        rxp = max(rxw * sx, 1.0)
        ryp = max(ryw * sx, 1.0)  # y and x both in pixels; scales match (4:3)
        yc = (WH - cy) / WH * (self.h - 1)
        for y in range(self.h):
            for x in range(self.w):
                dx = (x - cx_px) / rxp
                dy = (y - yc) / ryp
                if dx * dx + dy * dy <= 1.0:
                    self.a[y, x] = rgb

    def dot(self, x, wy, rgb):
        px, py = self._px(x, wy)
        if 0 <= px < self.w and 0 <= py < self.h:
            self.a[py, px] = rgb

    def set_px(self, px, py, rgb):
        if 0 <= px < self.w and 0 <= py < self.h:
            self.a[py, px] = rgb

    def get_px(self, px, py):
        if 0 <= px < self.w and 0 <= py < self.h:
            return self.a[py, px]
        return None

    def lighten(self, mask, factor):
        self.a[mask] = np.clip(self.a[mask] * factor, 0, 255)

    def darken(self, mask, factor):
        self.a[mask] *= factor


# ----------------------------------------------------------------------------
# palette
# ----------------------------------------------------------------------------
DEEP = (22, 58, 86)          # open-sea base (matches game WATER family)
DEEP2 = (16, 48, 74)         # darkest depth, far SE
SHALLOW = (36, 88, 112)      # turquoise shallows rim
SAND = (218, 198, 142)       # strand
LAND = (148, 126, 80)        # khaki land base (matches old chart land colour)
LAND2 = (104, 96, 62)        # darker forested interior
GRID = (58, 96, 122)         # faint cartouche lines


def blend(c1, c2, t):
    return tuple(round(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


# ----------------------------------------------------------------------------
# build
# ----------------------------------------------------------------------------
cv = Canvas(W, H)

# 1. Deep sea base, subtly darker toward the far south-east corner.
cv.fill(DEEP)
for y in range(H):
    for x in range(W):
        t = min(1.0, (x / W) * 0.55 + (y / H) * 0.65)
        cv.set_px(x, y, blend(DEEP, DEEP2, t * 0.5))

# 2. Faint cartouche grid every 800 world units.
for gx in range(0, int(WW) + 1, 800):
    if gx == 0 or gx == WW:
        continue
    px = round(gx / WW * W)
    for y in range(0, H, 2):
        cv.set_px(px, y, GRID)
for gy in range(0, int(WH) + 1, 800):
    if gy == 0 or gy == WH:
        continue
    py = round(H - gy / WH * H)
    for x in range(0, W, 2):
        cv.set_px(x, py, GRID)

# 3. Land masses, designed from the port table so every port sits on/near its
# own coast (top-left region is 中国大陆 south coast; the east edge is 吕宋; the
# west mid band is 中南半岛; 琼州/崖州 sit on Hainan island; 渤泥/爪哇/苏禄 are
# their own islands at the bottom).  Roughly one third of the chart is land.
LAND_BLOBS = [
    # --- China south coast, traced with small overlapping disks (port-labelled).
    ("disk", 3200, 3460, 300),    # 扬州: river mouth on the north bank
    ("disk", 4680, 3520, 260),    # 明州: north-east cape
    ("disk", 4400, 3260, 260),    # 福州
    ("disk", 4150, 3000, 230),    # 泉州
    ("disk", 3900, 2700, 210),    # 潮州
    ("disk", 3550, 2500, 260),    # coast between 潮州 and 广州
    ("disk", 3300, 2850, 210),    # 广州: inner bay shore
    ("disk", 2750, 2550, 240),    # 雷州: peninsula west coast (→ 合浦)
    ("disk", 2500, 2350, 150),    # 雷州半岛 south tip (above 琼州海峡)
    ("disk", 1900, 2220, 170),    # 合浦: 北部湾 top shore
    ("disk", 1350, 2550, 190),    # 钦州: western gulf coast
    # --- Indochina east coast chain (west bank of the gulf)
    ("disk", 1180, 2350, 260),    # 交州 north (gulf's west shore, up to 钦州)
    ("disk", 1180, 2000, 240),    # 交州: gulf west shore mid (port at 1200,1800)
    ("disk", 1200, 1800, 200),    # 交州 proper
    ("disk", 550, 1650, 300),     # 暹罗 inland
    ("disk", 900, 800, 250),      # 真腊: south-east coast near the delta
    ("disk", 1400, 1100, 190),    # 占城 coast finger
    ("disk", 600, 1300, 240),     # mid-Indochina east coast
    ("disk", 150, 2200, 320),     # far-west mass (暹罗湾 interior)
    ("disk", 1750, 400, 180),     # 佛逝: peninsular south tip
    ("disk", 1500, 500, 170),     # 佛逝 north-east coast (bulk the cape)
    # --- Hainan island (own island: 琼州 N coast, 崖州 S coast)
    ("ellipse", 2350, 1650, 250, 420),
    ("disk", 2650, 1950, 120),
    # --- 吕宋 island chain (east edge): 吕宋 (4300,1250) + south island
    ("ellipse", 4550, 1350, 330, 400),
    ("disk", 4300, 1250, 180),    # extra bulk around 吕宋
    ("disk", 4600, 800, 160),     # island south of 吕宋
    ("disk", 3300, 700, 140),     # 苏禄 island (south middle)
    # --- 渤泥 + 爪哇 at the bottom
    ("ellipse", 2900, 600, 300, 150),
    ("ellipse", 2300, 200, 330, 90),
    ("ellipse", 1500, 300, 180, 70),   # tiny isle SW of 爪哇 (decoration)
    # --- Xisha-ish mid-sea islets for route texture
    ("disk", 2300, 1080, 60),     # 永兴 reef island (sits on ISLAND_X/Y 2300,1080!)
    ("disk", 2000, 1200, 55),     # 西沙礁 patch
    ("disk", 3350, 1300, 70),     # 中沙 shallow atoll
]
for blob in LAND_BLOBS:
    kind = blob[0]
    if kind == "rect":
        cv.rect(blob[1], blob[2], blob[3], blob[4], LAND)
    elif kind == "ellipse":
        cv.ellipse(blob[1], blob[2], blob[3], blob[4], LAND)
    else:
        cv.disk(blob[1], blob[2], blob[3], LAND)

# 4. Sandy strand: thin light rim around every land pixel.
land_mask = np.all(cv.a == np.array(LAND), axis=2)
strand = np.zeros_like(land_mask)
for dy in (-1, 0, 1):
    for dx in (-1, 0, 1):
        if dx == 0 and dy == 0:
            continue
        shifted = np.zeros_like(land_mask)
        sy = slice(max(0, dy), min(H, H + dy)) if dy >= 0 else slice(0, H + dy)
        sx = slice(max(0, dx), min(W, W + dx)) if dx >= 0 else slice(0, W + dx)
        tgt_y = slice(max(0, -dy), min(H, H - dy)) if dy < 0 else slice(dy, H + dy)
        tgt_x = slice(max(0, -dx), min(W, W - dx)) if dx < 0 else slice(dx, W + dx)
        try:
            shifted[tgt_y, tgt_x] = land_mask[sy, sx]
        except Exception:
            pass
        strand |= shifted & ~land_mask
# strand colour blended onto the pixels beneath (keeps sea depth tint)
for y in range(H):
    for x in range(W):
        if strand[y, x]:
            cv.a[y, x] = blend(cv.a[y, x], SAND, 0.55)

# 5. Land interior: patchy darker forest, random-ish but deterministic.
rng = np.random.default_rng(20260904)
ys, xs = np.nonzero(land_mask)
for _ in range(1400):
    if len(ys) == 0:
        break
    k = rng.integers(0, len(ys))
    yy, xx = int(ys[k]), int(xs[k])
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            ny, nx = yy + dy, xx + dx
            if 0 <= ny < H and 0 <= nx < W and land_mask[ny, nx] and rng.random() < 0.5:
                cv.a[ny, nx] = blend(cv.a[ny, nx], LAND2, 0.55)

# 6. Turquoise shallows ring just off the strand (sea-side glow).
shallow = np.zeros_like(land_mask)
for dy in (-2, -1, 1, 2):
    for dx in (-2, -1, 1, 2):
        if abs(dx) + abs(dy) > 3:
            continue
        shifted = np.zeros_like(land_mask)
        # approximate 8-neighbour expansion again, then diff
        tgt_y = slice(max(0, -dy), min(H, H - dy))
        tgt_x = slice(max(0, -dx), min(W, W - dx))
        src_y = slice(max(0, dy), min(H, H + dy))
        src_x = slice(max(0, dx), min(W, W + dx))
        try:
            shifted[tgt_y, tgt_x] = land_mask[src_y, src_x]
        except Exception:
            pass
        shallow |= shifted & ~land_mask
for y in range(H):
    for x in range(W):
        if shallow[y, x] and not strand[y, x]:
            cv.a[y, x] = blend(cv.a[y, x], SHALLOW, 0.30)

# 7. Xisha / Nansha reef speckle (south sea atolls), tiny rings.
rng2 = np.random.default_rng(777)
for _ in range(46):
    wx = 1900 + rng2.uniform(0, 1800)
    wy = 250 + rng2.uniform(0, 1100)
    # keep clear of Hainan (2150..2550,1300..2050) and Indochina
    if 2050 <= wx <= 2600 and 1150 <= wy <= 2200:
        continue
    cv.disk(wx, wy, 1.2 + rng2.uniform(0, 2.4), (150, 190, 205))
    cv.disk(wx, wy, 0.6, (70, 130, 150))

# 8. Sea-lane arcs (trade routes) — faint dashed arcs across the open sea.
def dash_arc(cxw, cyw, rw, a0, a1, rgb, step=26.0):
    a = a0
    while a < a1:
        x0 = cxw + math.cos(a) * rw
        y0 = cyw + math.sin(a) * rw
        a += step / max(rw, 1.0)
        x1 = cxw + math.cos(a) * rw
        y1 = cyw + math.sin(a) * rw
        # interpolate a few px along the dash
        for t in range(4):
            tt = t / 4.0
            cv.dot(x0 + (x1 - x0) * tt, y0 + (y1 - y0) * tt, rgb)
        a += step / max(rw, 1.0)


dash_arc(2400, 2200, 1150, math.pi * 0.35, math.pi * 1.25, (255, 235, 160))
dash_arc(2400, 2200, 1550, math.pi * 0.45, math.pi * 1.15, (235, 215, 140))

# 9. Compass rose, bottom-right corner (world units ~4350,450).
cv.disk(4350, 450, 26, (30, 62, 84))
for ang in range(0, 360, 22):
    r_ = math.radians(ang)
    for rr in range(6, 25):
        cv.dot(4350 + math.cos(r_) * rr * 1.35, 450 + math.sin(r_) * rr * 1.35,
               (255, 240, 190))
    if ang % 90 == 0:
        for rr in range(6, 30):
            cv.dot(4350 + math.cos(r_) * rr * 1.35, 450 + math.sin(r_) * rr * 1.35,
                   (255, 120, 90))
cv.disk(4350, 450, 4, (30, 62, 84))

# 10. Grain: lighten with a soft dithered sparkle so big flat seas read as
# "pixel water" rather than a single solid colour.
grain = rng2.random((H, W)) < 0.045
for y in range(H):
    for x in range(W):
        if grain[y, x]:
            r, g, b = cv.a[y, x]
            cv.a[y, x] = (min(255, r + 14), min(255, g + 16), min(255, b + 18))

img = np.clip(cv.a, 0, 255).astype(np.uint8)

# ----------------------------------------------------------------------------
# PNG writer (RGBA, Up filter, zlib) — same as gen_icons.py
# ----------------------------------------------------------------------------
def write_png(path, rgba):
    rgba = np.ascontiguousarray(rgba)
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
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print("wrote", path, w, "x", h)


rgba = np.dstack([img, np.full((H, W), 255, dtype=np.uint8)])
write_png(OUT, rgba)

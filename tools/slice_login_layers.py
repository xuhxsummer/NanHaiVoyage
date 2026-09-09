"""Extract the authored login layers from our existing harbor painting (Pillow).
Run from the repo root. No AI/external assets; only polygon masks and edge extension.
The small filled areas behind sails stay covered by the gently moving cutouts.
"""
from collections import deque
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'android/assets/textures/login'
OUT.mkdir(parents=True, exist_ok=True)
source = Image.open(ROOT / 'assets/textures/login/harbor-hd.png').convert('RGBA')
W, H = source.size
base = source.copy()
# Pixel coordinates in the original painting, from top left. Pivot is in source pixels.
layers = [
    ('sail-rear', [(1291,266),(1337,241),(1348,473),(1321,519),(1271,479)], (1324,513), .38, .008, .95, .8),
    ('sail-main', [(1336,167),(1465,100),(1495,230),(1516,342),(1523,438),(1505,456),(1350,468)], (1354,467), .48, .009, .8, 0),
    ('sail-front', [(1230,382),(1291,350),(1302,501),(1244,525)], (1248,522), .65, .012, 1.08, 1.6),
    ('flag-main', [(1380,52),(1435,66),(1445,105),(1514,137),(1560,185),(1520,180),(1472,147),(1420,110),(1380,91)], (1382,70), 2.0, .045, 1.7, .2),
    ('flag-front', [(1248,312),(1280,330),(1290,344),(1310,365),(1280,360),(1248,339)], (1251,321), 2.5, .04, 1.9, .9),
    ('flag-stern', [(1554,291),(1599,322),(1647,401),(1610,381),(1578,349),(1554,326)], (1557,307), 2.2, .04, 1.6, 1.3),
]
combined = Image.new('L', source.size)
manifest = {'width': W, 'height': H, 'layers': []}
for name, points, pivot, rotation, pulse, rate, phase in layers:
    mask = Image.new('L', source.size)
    ImageDraw.Draw(mask).polygon(points, fill=255)
    if name.startswith('flag'):
        # Keep the red cloth/streamers, leaving the sky between them still.
        px, m = source.load(), mask.load()
        for y in range(H):
            for x in range(W):
                if m[x,y]:
                    r,g,b,_ = px[x,y]
                    if not (r>70 and r>g*1.55 and r>b*2.0): m[x,y]=0
    bounds = mask.getbbox()
    cut = source.copy(); cut.putalpha(mask)
    cut.crop(bounds).save(OUT / (name+'.png'))
    combined = Image.frombytes('L', source.size, bytes(max(a,b) for a,b in zip(combined.tobytes(),mask.tobytes())))
    x,y,right,bottom=bounds
    manifest['layers'].append(dict(name=name,x=x,y=y,width=right-x,height=bottom-y,
        pivotX=pivot[0]-x,pivotY=bottom-pivot[1],rotation=rotation,pulse=pulse,rate=rate,phase=phase))
# Extend surrounding pixels into the removed areas. Only a few edge pixels ever
# become visible; the sail/flag cutouts cover the interior throughout their sway.
mask=combined.load(); px=base.load(); seen=bytearray(W*H); queue=deque()
for y in range(1,H-1):
    for x in range(1,W-1):
        if mask[x,y] and any(not mask[nx,ny] for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]):
            for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
                if not mask[nx,ny]: px[x,y]=px[nx,ny];break
            seen[y*W+x]=1;queue.append((x,y))
while queue:
    x,y=queue.popleft()
    for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
        if 0<=nx<W and 0<=ny<H and mask[nx,ny] and not seen[ny*W+nx]:
            seen[ny*W+nx]=1;px[nx,ny]=px[x,y];queue.append((nx,ny))
base.save(OUT / 'harbor-base.png')
sea=Image.new('L',source.size)
ImageDraw.Draw(sea).polygon([(174,660),(350,640),(600,601),(942,605),(1052,600),(1174,613),
    (1210,663),(1300,690),(1450,694),(1576,670),(1595,684),(1556,738),(1478,771),
    (1312,788),(1204,836),(648,838),(585,822),(427,790),(343,752),(184,724)],fill=255)
sea.filter(ImageFilter.GaussianBlur(5)).resize((836,471),Image.Resampling.LANCZOS).save(OUT / 'sea-mask.png')
(OUT/'layers.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('LOGIN LAYERS:', len(layers), 'sails/flags + static harbor + sea mask at', OUT)

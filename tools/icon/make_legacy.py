"""Renders legacy (API 24-25) launcher PNGs from the adaptive icon vector."""
import re, sys, os
import numpy as np
from PIL import Image, ImageDraw, ImageChops

res = sys.argv[1]
xml = open(os.path.join(res, "drawable/ic_launcher_foreground.xml")).read()
d = re.search(r'android:pathData="([^"]+)"', xml).group(1)
tok = re.findall(r"[MLCZ]|-?\d+\.?\d*", d)

subs, cur, i = [], [], 0
def flat(p0, c1, c2, p1, n=16):
    out = []
    for k in range(1, n + 1):
        t = k / n; u = 1 - t
        out.append((u**3*p0[0]+3*u*u*t*c1[0]+3*u*t*t*c2[0]+t**3*p1[0],
                    u**3*p0[1]+3*u*u*t*c1[1]+3*u*t*t*c2[1]+t**3*p1[1]))
    return out
while i < len(tok):
    c = tok[i]
    if c == "M":
        cur = [(float(tok[i+1]), float(tok[i+2]))]; i += 3
    elif c == "L":
        cur.append((float(tok[i+1]), float(tok[i+2]))); i += 3
    elif c == "C":
        v = [float(x) for x in tok[i+1:i+7]]
        cur += flat(cur[-1], (v[0], v[1]), (v[2], v[3]), (v[4], v[5])); i += 7
    elif c == "Z":
        subs.append(cur); cur = []; i += 1
    else:
        raise SystemExit("unexpected token " + c)

SS = 4
def glyph(size):
    """Foreground glyph mask (even-odd) on a 108dp canvas of `size` px."""
    n = size * SS
    k = n / 512.0
    mask = Image.new("1", (n, n), 0)
    for s in subs:
        layer = Image.new("1", (n, n), 0)
        ImageDraw.Draw(layer).polygon([(x*k, y*k) for x, y in s], fill=1)
        mask = ImageChops.logical_xor(mask, layer)
    return mask.convert("L").resize((size, size), Image.LANCZOS)

def gradient(size):
    y, x = np.mgrid[0:size, 0:size]
    t = (x + y) / (2.0 * (size - 1))
    c = [(0x08, 0xB9, 0xF5), (0x07, 0x55, 0xD9), (0x07, 0x1B, 0x78)]
    a = np.zeros((size, size, 3))
    for ch in range(3):
        a[..., ch] = np.where(t < .5, c[0][ch] + (c[1][ch]-c[0][ch])*t*2,
                              c[1][ch] + (c[2][ch]-c[1][ch])*(t-.5)*2)
    return Image.fromarray(a.astype("uint8")).convert("RGBA")

def legacy(px):
    full = int(round(px * 108 / 72))
    bg = gradient(full)
    white = Image.new("RGBA", (full, full), (255, 255, 255, 255))
    white.putalpha(glyph(full))
    comp = Image.alpha_composite(bg, white)
    m = (full - px) // 2
    vis = comp.crop((m, m, m + px, m + px))
    big = Image.new("L", (px * 4, px * 4), 0)
    ImageDraw.Draw(big).rounded_rectangle((0, 0, px*4-1, px*4-1), radius=int(px*4*0.22), fill=255)
    vis.putalpha(big.resize((px, px), Image.LANCZOS))
    return vis

for dens, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    out = os.path.join(res, "mipmap-" + dens)
    os.makedirs(out, exist_ok=True)
    legacy(px).save(os.path.join(out, "ic_launcher.png"), optimize=True)
    print("wrote", dens, px)

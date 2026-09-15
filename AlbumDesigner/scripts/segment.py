#!/usr/bin/env python3
"""Split each style-sheet JPG into individual transparent PNG stickers."""
import os, glob, sys
import numpy as np
import scipy.ndimage as ndi
from PIL import Image, ImageFilter

RAW = sys.argv[1] if len(sys.argv) > 1 else "stickers_raw"
OUT = sys.argv[2] if len(sys.argv) > 2 else "stickers_out"
os.makedirs(OUT, exist_ok=True)

report = {}
for path in sorted(glob.glob(os.path.join(RAW, "sheet_*.jpg")),
                   key=lambda p: int(''.join(filter(str.isdigit, os.path.basename(p))))):
    name = os.path.splitext(os.path.basename(path))[0]
    im = Image.open(path).convert("RGB")
    W, H = im.size
    a = np.asarray(im).astype(np.int16)
    mn = a.min(axis=2)
    # foreground = anything sufficiently non-white
    fg = (255 - mn) > 26
    # drop tiny speckle, then bridge small gaps inside a single item
    fg = ndi.binary_opening(fg, iterations=1)
    rad = max(3, int(min(W, H) * 0.012))
    st = np.ones((rad, rad), bool)
    merged = ndi.binary_closing(fg, structure=st, iterations=1)
    filled = ndi.binary_fill_holes(merged)
    lbl, n = ndi.label(filled)
    if n == 0:
        report[name] = 0; continue
    sizes = ndi.sum(np.ones_like(lbl), lbl, index=range(1, n + 1))
    slices = ndi.find_objects(lbl)
    total = W * H
    idx = 0
    kept = 0
    order = sorted(range(1, n + 1), key=lambda k: (slices[k-1][0].start, slices[k-1][1].start))
    for k in order:
        area = sizes[k - 1]
        if area < total * 0.0012:      # ignore noise / stray marks
            continue
        sl = slices[k - 1]
        y0, y1 = sl[0].start, sl[0].stop
        x0, x1 = sl[1].start, sl[1].stop
        bw, bh = x1 - x0, y1 - y0
        if bw > W * 0.85 and bh > H * 0.85:   # whole-sheet blob, skip
            continue
        pad = int(min(bw, bh) * 0.08) + 4
        X0, Y0 = max(0, x0 - pad), max(0, y0 - pad)
        X1, Y1 = min(W, x1 + pad), min(H, y1 + pad)
        # alpha from the filled mask of THIS component only (keeps white interiors)
        comp = (lbl[Y0:Y1, X0:X1] == k)
        comp = ndi.binary_fill_holes(comp)
        # soft edge
        alpha = (comp * 255).astype(np.uint8)
        aimg = Image.fromarray(alpha, "L").filter(ImageFilter.GaussianBlur(0.8))
        crop = im.crop((X0, Y0, X1, Y1)).convert("RGBA")
        crop.putalpha(aimg)
        # downscale very large crops
        m = max(crop.size)
        if m > 512:
            s = 512 / m
            crop = crop.resize((int(crop.width * s), int(crop.height * s)), Image.LANCZOS)
        idx += 1; kept += 1
        crop.save(os.path.join(OUT, f"{name}_{idx:02d}.png"))
    report[name] = kept

for k, v in report.items():
    print(f"{k}: {v}")
print("TOTAL:", sum(report.values()))

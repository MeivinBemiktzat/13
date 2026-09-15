#!/usr/bin/env python3
"""Segment batch-2 sheets. Handles non-white backgrounds and hollow frames."""
import os, glob, sys
import numpy as np
import scipy.ndimage as ndi
from PIL import Image, ImageFilter

RAW = "stickers_raw2"
OUT = "stickers_out2"
os.makedirs(OUT, exist_ok=True)

# sheet -> (category, mode). mode: 'blob' | 'gridRxC' | 'framegridRxC'
CFG = {
    1: ("banners", "blob"),
    2: ("frames", "framegrid3x3"),
    3: ("ornaments", "blob"),
    4: ("flowers", "blob"),
    5: ("baby", "blob"),
    6: ("shapes", "blob"),
    7: ("pets", "blob"),
}

def bg_color(a):
    h, w, _ = a.shape
    b = max(4, int(min(h, w) * 0.02))
    border = np.concatenate([
        a[:b].reshape(-1, 3), a[-b:].reshape(-1, 3),
        a[:, :b].reshape(-1, 3), a[:, -b:].reshape(-1, 3)])
    return np.median(border, axis=0)

def fg_mask(a, thr=32):
    bg = bg_color(a)
    d = np.abs(a.astype(np.int16) - bg).max(axis=2)
    return d > thr

def soft_alpha(mask):
    al = (mask * 255).astype(np.uint8)
    return Image.fromarray(al, "L").filter(ImageFilter.GaussianBlur(0.8))

def save_crop(im, mask, box, path):
    X0, Y0, X1, Y1 = box
    crop = im.crop((X0, Y0, X1, Y1)).convert("RGBA")
    a = soft_alpha(mask[Y0:Y1, X0:X1])
    crop.putalpha(a)
    m = max(crop.size)
    if m > 512:
        s = 512 / m
        crop = crop.resize((int(crop.width * s), int(crop.height * s)), Image.LANCZOS)
    crop.save(path)

report = {}
for sheet, (cat, mode) in CFG.items():
    path = os.path.join(RAW, f"new_{sheet}.jpg")
    if not os.path.exists(path):
        continue
    im = Image.open(path).convert("RGB")
    W, H = im.size
    a = np.asarray(im)
    d = os.path.join(OUT, cat)
    os.makedirs(d, exist_ok=True)
    idx = 0
    if mode.startswith("framegrid") or mode.startswith("grid"):
        rc = mode.replace("framegrid", "").replace("grid", "")
        R, C = map(int, rc.split("x"))
        fill = not mode.startswith("framegrid")
        cw, ch = W // C, H // R
        for ri in range(R):
            for ci in range(C):
                x0, y0 = ci * cw, ri * ch
                cell = a[y0:y0 + ch, x0:x0 + cw]
                m = fg_mask(cell, 30)
                m = ndi.binary_opening(m, iterations=1)
                if fill:
                    m = ndi.binary_fill_holes(m)
                if m.sum() < cell.shape[0] * cell.shape[1] * 0.01:
                    continue
                ys, xs = np.where(m)
                pad = 6
                bx0, by0 = max(0, xs.min() - pad), max(0, ys.min() - pad)
                bx1, by1 = min(cw, xs.max() + pad), min(ch, ys.max() + pad)
                full = np.zeros((H, W), bool)
                full[y0:y0 + ch, x0:x0 + cw] = m
                idx += 1
                save_crop(im, full, (x0 + bx0, y0 + by0, x0 + bx1, y0 + by1),
                          os.path.join(d, f"{idx:02d}.png"))
    else:
        fg = fg_mask(a, 30)
        fg = ndi.binary_opening(fg, iterations=1)
        rad = max(3, int(min(W, H) * 0.012))
        st = np.ones((rad, rad), bool)
        merged = ndi.binary_closing(fg, structure=st, iterations=1)
        filled = ndi.binary_fill_holes(merged)
        lbl, n = ndi.label(filled)
        if n == 0:
            report[cat] = 0; continue
        sizes = ndi.sum(np.ones_like(lbl), lbl, index=range(1, n + 1))
        slices = ndi.find_objects(lbl)
        total = W * H
        order = sorted(range(1, n + 1), key=lambda k: (slices[k-1][0].start // (H//6), slices[k-1][1].start))
        for k in order:
            if sizes[k - 1] < total * 0.0012:
                continue
            sl = slices[k - 1]
            y0, y1, x0, x1 = sl[0].start, sl[0].stop, sl[1].start, sl[1].stop
            if (x1 - x0) > W * 0.85 and (y1 - y0) > H * 0.85:
                continue
            comp = ndi.binary_fill_holes(lbl == k)
            pad = int(min(x1 - x0, y1 - y0) * 0.08) + 4
            X0, Y0 = max(0, x0 - pad), max(0, y0 - pad)
            X1, Y1 = min(W, x1 + pad), min(H, y1 + pad)
            idx += 1
            save_crop(im, comp, (X0, Y0, X1, Y1), os.path.join(d, f"{idx:02d}.png"))
    report[cat] = idx

for k, v in report.items():
    print(f"{k}: {v}")
print("TOTAL:", sum(report.values()))

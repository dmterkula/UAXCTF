#!/usr/bin/env python3
"""Tile video frames into labeled contact sheets so many frames can be reviewed in one image.

Usage:
  contact_sheet.py <out_prefix> <cols> <per_sheet> <y0> <y1> <x0> <x1> <frame files...>
    y0..y1 / x0..x1 are crop fractions (0-1) of each frame, e.g. 0.15 0.65 0 1 = middle band.
    Each tile is labeled with its video timestamp (from the f_<sec>.jpg filename).
Prints one line per sheet written: <path> <first frame> <last frame>.
Requires Pillow.
"""
import os
import sys

from PIL import Image, ImageDraw


def main():
    if len(sys.argv) < 9:
        sys.exit(__doc__)
    prefix, cols, per = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
    y0, y1, x0, x1 = map(float, sys.argv[4:8])
    files = sorted(sys.argv[8:])
    for s in range(0, len(files), per):
        chunk = files[s:s + per]
        tiles = []
        for f in chunk:
            im = Image.open(f)
            w, h = im.size
            tiles.append(im.crop((int(w * x0), int(h * y0), int(w * x1), int(h * y1))))
        tw, th = tiles[0].size
        rows = (len(tiles) + cols - 1) // cols
        sheet = Image.new("RGB", (tw * cols, th * rows), "white")
        draw = ImageDraw.Draw(sheet)
        for i, (f, tile) in enumerate(zip(chunk, tiles)):
            x, y = (i % cols) * tw, (i // cols) * th
            sheet.paste(tile, (x, y))
            draw.rectangle([x, y, x + 80, y + 18], fill="yellow")
            draw.text((x + 3, y + 3), os.path.basename(f)[2:-4] + "s", fill="black")
        path = f"{prefix}_{s // per:02d}.jpg"
        sheet.save(path, quality=88)
        print(path, os.path.basename(chunk[0]), os.path.basename(chunk[-1]))


if __name__ == "__main__":
    main()

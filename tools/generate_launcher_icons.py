#!/usr/bin/env python3
"""Regenerates every raster version of the Solo mark so it matches the vector icons.

The launcher itself uses the adaptive icon (minSdk is 26). The bitmaps written here are
the legacy mipmaps, the Play Store listing icon, the README icon and the artwork
placeholder, all drawn from the same geometry as drawable/ic_launcher_foreground.xml
(the "Pure Tone" tuning-fork mark in the 108-unit adaptive grid) and
drawable/ic_launcher_background.xml (the royal-plum plate).

Requires Pillow. Run from the repository root:
    python3 tools/generate_launcher_icons.py
"""

from __future__ import annotations

import math
import os

from PIL import Image, ImageChops, ImageDraw

RES = os.path.join("app", "src", "main", "res")
PLAYSTORE = os.path.join("app", "src", "main", "ic_launcher-playstore.png")
README_ICON = os.path.join("assets", "icon.png")
PLACEHOLDER = os.path.join(RES, "drawable", "placeholder.webp")

UNITS = 108.0  # adaptive icon grid
SUPERSAMPLE = 4

# Plate: vertical plum-to-ink ramp plus a soft amethyst glow behind the mark.
PLATE_STOPS = [(0.0, (0x2B, 0x1A, 0x4D)), (0.55, (0x1A, 0x10, 0x30)), (1.0, (0x0C, 0x09, 0x12))]
GLOW_CENTRE = (54.0, 34.0)
GLOW_RADIUS = 46.0
GLOW_COLOUR = (0x7A, 0x5C, 0xB2)
GLOW_STOPS = [(0.0, 0.30), (0.55, 0.10), (1.0, 0.0)]

# Gold: diagonal ramp across the mark, same stops as the vector gradient.
GOLD_STOPS = [(0.0, (0xFB, 0xEF, 0xD0)), (0.45, (0xE6, 0xC2, 0x7A)), (1.0, (0xB8, 0x89, 0x3A))]
GOLD_FROM = (40.0, 27.0)
GOLD_TO = (68.0, 81.0)
SHADOW = (0x05, 0x03, 0x0A)

# Mark geometry (108-unit grid), identical to the vector drawables.
TINE_HALF = 3.8          # half width of each tine
TINE_X = (46.0, 62.0)    # tine centre lines
TINE_TOP = 30.8          # centre of the rounded tine tips
BEND_Y = 53.0            # centre of the U bend
BEND_OUTER = 11.8
BEND_INNER = 4.2
STEM = (51.2, 60.0, 56.8, 72.0)
BALL = (54.0, 75.2, 5.8)


def _lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def _ramp(stops, t):
    t = min(1.0, max(0.0, t))
    for (o0, c0), (o1, c1) in zip(stops, stops[1:]):
        if o0 <= t <= o1:
            local = (t - o0) / (o1 - o0) if o1 > o0 else 0.0
            if isinstance(c0, tuple):
                return _lerp(c0, c1, local)
            return c0 + (c1 - c0) * local
    return stops[-1][1]


def _field(size, fn, mode="RGB"):
    """Evaluates a smooth colour field at a small size and scales it up."""
    work = min(size, 216)
    img = Image.new(mode, (work, work))
    px = img.load()
    for y in range(work):
        uy = (y + 0.5) / work * UNITS
        for x in range(work):
            ux = (x + 0.5) / work * UNITS
            px[x, y] = fn(ux, uy)
    return img.resize((size, size), Image.BICUBIC) if work != size else img


def plate(size):
    base = _field(size, lambda x, y: _ramp(PLATE_STOPS, y / UNITS))
    gx, gy = GLOW_CENTRE
    glow = _field(
        size,
        lambda x, y: round(255 * _ramp(GLOW_STOPS, math.hypot(x - gx, y - gy) / GLOW_RADIUS)),
        mode="L",
    )
    return Image.composite(Image.new("RGB", (size, size), GLOW_COLOUR), base, glow)


def gold(size):
    ax, ay = GOLD_FROM
    bx, by = GOLD_TO
    dx, dy = bx - ax, by - ay
    span = dx * dx + dy * dy
    return _field(size, lambda x, y: _ramp(GOLD_STOPS, ((x - ax) * dx + (y - ay) * dy) / span))


def mark(size, scale=1.0, dy=0.0):
    """Alpha mask of the mark. [scale] grows it around the centre, [dy] shifts it down."""
    k = size / UNITS

    def p(x, y):
        return (54 + (x - 54) * scale) * k, (54 + (y - 54) * scale + dy) * k

    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    r = TINE_HALF * scale * k
    for tx in TINE_X:
        x0, y0 = p(tx, TINE_TOP)
        _, y1 = p(tx, BEND_Y)
        d.rectangle((x0 - r, y0, x0 + r, y1), fill=255)
        d.ellipse((x0 - r, y0 - r, x0 + r, y0 + r), fill=255)
    cx, cy = p(54, BEND_Y)
    ro, ri = BEND_OUTER * scale * k, BEND_INNER * scale * k
    ring = Image.new("L", (size, size), 0)
    ImageDraw.Draw(ring).ellipse((cx - ro, cy - ro, cx + ro, cy + ro), fill=255)
    hole = Image.new("L", (size, size), 0)
    ImageDraw.Draw(hole).ellipse((cx - ri, cy - ri, cx + ri, cy + ri), fill=255)
    lower = Image.new("L", (size, size), 0)
    ImageDraw.Draw(lower).rectangle((0, cy, size, size), fill=255)
    m = ImageChops.lighter(m, ImageChops.multiply(ImageChops.subtract(ring, hole), lower))
    sx0, sy0 = p(STEM[0], STEM[1])
    sx1, sy1 = p(STEM[2], STEM[3])
    ImageDraw.Draw(m).rectangle((sx0, sy0, sx1, sy1), fill=255)
    bx, by = p(BALL[0], BALL[1])
    br = BALL[2] * scale * k
    ImageDraw.Draw(m).ellipse((bx - br, by - br, bx + br, by + br), fill=255)
    return m


def artwork(size, scale=1.0, with_plate=True):
    """Full 108-unit canvas: plate (optional), soft shadow and the gold mark."""
    work = size * SUPERSAMPLE
    if with_plate:
        canvas = plate(work).convert("RGBA")
    else:
        canvas = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    shadow = mark(work, scale, dy=1.4 * scale).point(lambda v: round(v * 0.30))
    shadow_layer = Image.new("RGBA", (work, work), SHADOW + (0,))
    shadow_layer.putalpha(shadow)
    canvas = Image.alpha_composite(canvas, shadow_layer)
    gold_layer = gold(work).convert("RGBA")
    gold_layer.putalpha(mark(work, scale))
    canvas = Image.alpha_composite(canvas, gold_layer)
    return canvas


def crop_units(img, units):
    """Keeps the centre [units] x [units] of a 108-unit canvas."""
    n = img.size[0]
    inset = round(n * (UNITS - units) / 2 / UNITS)
    return img.crop((inset, inset, n - inset, n - inset))


def masked(img, shape):
    n = img.size[0]
    mask = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(mask)
    if shape == "circle":
        d.ellipse((0, 0, n - 1, n - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, n - 1, n - 1), radius=round(n * 0.22), fill=255)
    out = img.copy()
    out.putalpha(ImageChops.multiply(img.getchannel("A"), mask))
    return out


def launcher_bitmap(size, shape):
    # A legacy icon shows the part of the adaptive canvas a launcher mask would show:
    # the centre 72 units.
    full = artwork(round(size * UNITS / 72))
    return masked(crop_units(full, 72), shape).resize((size, size), Image.LANCZOS)


def placeholder(size):
    """Artwork placeholder: a quiet velvet tile with a faint gold mark."""
    work = size * SUPERSAMPLE
    base = _field(work, lambda x, y: _lerp((0x24, 0x1C, 0x35), (0x14, 0x10, 0x20), (x + y) / (2 * UNITS)))
    glyph = mark(work, 0.9).point(lambda v: round(v * 0.22))
    base = Image.composite(gold(work), base, glyph)
    return base.resize((size, size), Image.LANCZOS)


def main():
    densities = {
        "mdpi": (48, 108),
        "hdpi": (72, 162),
        "xhdpi": (96, 216),
        "xxhdpi": (144, 324),
        "xxxhdpi": (192, 432),
    }
    for density, (icon_size, foreground_size) in densities.items():
        folder = os.path.join(RES, "mipmap-" + density)
        os.makedirs(folder, exist_ok=True)
        launcher_bitmap(icon_size, "squircle").save(
            os.path.join(folder, "ic_launcher.webp"), "WEBP", lossless=True
        )
        launcher_bitmap(icon_size, "circle").save(
            os.path.join(folder, "ic_launcher_round.webp"), "WEBP", lossless=True
        )
        artwork(foreground_size, with_plate=False).resize(
            (foreground_size, foreground_size), Image.LANCZOS
        ).save(os.path.join(folder, "ic_launcher_foreground.webp"), "WEBP", lossless=True)
        print("wrote", folder)

    # Play Store: full-bleed square (the store applies its own mask), canvas scaled 1.2x.
    crop_units(artwork(round(512 * 1.2)), UNITS / 1.2).resize((512, 512), Image.LANCZOS).convert(
        "RGB"
    ).save(PLAYSTORE, "PNG")
    print("wrote", PLAYSTORE)

    icon = masked(crop_units(artwork(round(500 * UNITS / 84)), 84), "squircle")
    icon.resize((500, 500), Image.LANCZOS).save(README_ICON, "PNG")
    print("wrote", README_ICON)

    placeholder(256).save(PLACEHOLDER, "WEBP", quality=92)
    print("wrote", PLACEHOLDER)


if __name__ == "__main__":
    main()

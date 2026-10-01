#!/usr/bin/env python3
"""Regenerates every raster version of the Solo mark so it matches the vector icons.

The launcher itself uses the adaptive icon (minSdk is 26). The bitmaps written here are
the legacy mipmaps, the Play Store listing icon, the README icon and the artwork
placeholder, all drawn from the same geometry as drawable/ic_launcher_foreground.xml
(the "Solo Facet" mark in the 108-unit adaptive grid) and
drawable/ic_launcher_background.xml (the obsidian radial plate).

Solo Facet: a rhombus split into four facets that meet at an off-centre apex, so the
gem reads as lit from the top right, plus one small "solo" dot. Each facet is inset
toward its own incentre so neighbouring facets are separated by an even 1.4-unit gap.

Requires Pillow. Run from the repository root:
    python3 tools/generate_launcher_icons.py            # write every raster
    python3 tools/generate_launcher_icons.py --preview DIR   # light/dark previews only
"""

from __future__ import annotations

import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw

RES = os.path.join("app", "src", "main", "res")
PLAYSTORE = os.path.join("app", "src", "main", "ic_launcher-playstore.png")
README_ICON = os.path.join("assets", "icon.png")
PLACEHOLDER = os.path.join(RES, "drawable", "placeholder.webp")

UNITS = 108.0  # adaptive icon grid
SUPERSAMPLE = 4

# Plate: radial obsidian glow, off-centre toward the top left.
PLATE_CENTRE = (38.0, 30.0)
PLATE_RADIUS = 96.0
PLATE_STOPS = [(0.0, (0x2A, 0x1F, 0x4E)), (0.55, (0x14, 0x10, 0x22)), (1.0, (0x08, 0x07, 0x0C))]

# Mark geometry (108-unit grid), identical to the vector drawables.
N, E, S, W = (54.0, 29.0), (79.0, 54.0), (54.0, 79.0), (29.0, 54.0)
APEX = (57.0, 51.0)
GAP = 1.4
FACETS = [  # (triangle, colour)
    ((N, E, APEX), (0xFF, 0xE3, 0xC2)),
    ((E, S, APEX), (0xFF, 0xAE, 0x70)),
    ((S, W, APEX), (0xE2, 0x56, 0x6E)),
    ((W, N, APEX), (0xFF, 0xC8, 0x96)),
]
DOT = (73.0, 35.0, 3.4)
DOT_COLOUR = (0xFF, 0xE3, 0xC2)
SHADOW = (0x05, 0x03, 0x0A)
SHADOW_ALPHA = 0x4D / 255
SHADOW_DY = 1.4


def inset_triangle(tri, d):
    """Offsets every edge of [tri] inward by [d] (scales about the incentre)."""
    (ax, ay), (bx, by), (cx, cy) = tri
    a = math.dist((bx, by), (cx, cy))
    b = math.dist((ax, ay), (cx, cy))
    c = math.dist((ax, ay), (bx, by))
    p = a + b + c
    ix, iy = (a * ax + b * bx + c * cx) / p, (a * ay + b * by + c * cy) / p
    area = abs((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) / 2
    r = area / (p / 2)
    k = (r - d) / r
    return [(ix + (x - ix) * k, iy + (y - iy) * k) for x, y in tri]


INSET_FACETS = [(inset_triangle(tri, GAP / 2), colour) for tri, colour in FACETS]


def _lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def _ramp(stops, t):
    t = min(1.0, max(0.0, t))
    for (o0, c0), (o1, c1) in zip(stops, stops[1:]):
        if o0 <= t <= o1:
            local = (t - o0) / (o1 - o0) if o1 > o0 else 0.0
            return _lerp(c0, c1, local)
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
    gx, gy = PLATE_CENTRE
    return _field(size, lambda x, y: _ramp(PLATE_STOPS, math.hypot(x - gx, y - gy) / PLATE_RADIUS))


def _pt(x, y, k, scale, dy):
    return (54 + (x - 54) * scale) * k, (54 + (y - 54) * scale + dy) * k


def draw_mark(canvas, scale=1.0, dy=0.0, colour=None, alpha=255):
    """Draws the facets and the dot onto an RGBA [canvas]. [colour] forces one fill."""
    size = canvas.size[0]
    k = size / UNITS
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for tri, fill in INSET_FACETS:
        d.polygon([_pt(x, y, k, scale, dy) for x, y in tri], fill=(colour or fill) + (alpha,))
    cx, cy = _pt(DOT[0], DOT[1], k, scale, dy)
    r = DOT[2] * scale * k
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(colour or DOT_COLOUR) + (alpha,))
    return Image.alpha_composite(canvas, layer)


def artwork(size, scale=1.0, with_plate=True):
    """Full 108-unit canvas: plate (optional), soft shadow and the facet mark."""
    work = size * SUPERSAMPLE
    if with_plate:
        canvas = plate(work).convert("RGBA")
    else:
        canvas = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    canvas = draw_mark(canvas, scale, dy=SHADOW_DY * scale, colour=SHADOW, alpha=round(255 * SHADOW_ALPHA))
    canvas = draw_mark(canvas, scale)
    return canvas.resize((size, size), Image.LANCZOS)


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
    """Artwork placeholder: a quiet obsidian tile with a faint facet mark."""
    work = size * SUPERSAMPLE
    base = _field(work, lambda x, y: _lerp((0x21, 0x1D, 0x2E), (0x11, 0x0F, 0x18), (x + y) / (2 * UNITS)))
    base = draw_mark(base.convert("RGBA"), 0.9, alpha=46)
    return base.convert("RGB").resize((size, size), Image.LANCZOS)


def notification(size):
    """White-only small icon preview (24-unit artboard)."""
    work = size * SUPERSAMPLE
    img = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    k = work / 24.0
    d = ImageDraw.Draw(img)
    n, e, s, w, a = (12, 2.5), (21.5, 12), (12, 21.5), (2.5, 12), (12.6, 11.4)
    for tri in ((n, e, a), (e, s, a), (s, w, a), (w, n, a)):
        d.polygon([(x * k, y * k) for x, y in inset_triangle(tri, 0.5)], fill=(255, 255, 255, 255))
    r = 1.5 * k
    d.ellipse((19.6 * k - r, 4.4 * k - r, 19.6 * k + r, 4.4 * k + r), fill=(255, 255, 255, 255))
    return img.resize((size, size), Image.LANCZOS)


def preview(folder):
    """Renders 48 px and 512 px versions on white and black for visual inspection."""
    os.makedirs(folder, exist_ok=True)
    for px in (48, 512):
        icon = launcher_bitmap(px, "squircle")
        round_icon = launcher_bitmap(px, "circle")
        fg = artwork(px, with_plate=False)
        notif = notification(max(24, px // 2))
        for name, bg in (("light", (255, 255, 255, 255)), ("dark", (0, 0, 0, 255))):
            pad = px // 6
            sheet = Image.new("RGBA", (px * 3 + notif.size[0] + pad * 5, px + pad * 2), bg)
            sheet.alpha_composite(icon, (pad, pad))
            sheet.alpha_composite(round_icon, (px + pad * 2, pad))
            sheet.alpha_composite(fg, (px * 2 + pad * 3, pad))
            tint = Image.new("RGBA", notif.size, (40, 40, 40, 255) if name == "light" else (255, 255, 255, 255))
            tint.putalpha(notif.getchannel("A"))
            sheet.alpha_composite(tint, (px * 3 + pad * 4, pad + (px - notif.size[1]) // 2))
            out = os.path.join(folder, f"preview_{px}_{name}.png")
            sheet.convert("RGB").save(out, "PNG")
            print("wrote", out)


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--preview":
        preview(sys.argv[2])
        return
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
        artwork(foreground_size, with_plate=False).save(
            os.path.join(folder, "ic_launcher_foreground.webp"), "WEBP", lossless=True
        )
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

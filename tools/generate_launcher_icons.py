#!/usr/bin/env python3
"""Regenerates every raster version of the SOLO mark so it matches the vector icons.

The launcher itself uses the adaptive icon (minSdk is 26). The bitmaps written here are
the legacy mipmaps, the Play Store listing icon, the README icon and the artwork
placeholder, all drawn from the same geometry as drawable/ic_launcher_foreground.xml
(the "Spotlight O" mark in the 108-unit adaptive grid) and
drawable/ic_launcher_background.xml (the warm-obsidian radial plate).

Spotlight O: a single bold ring (the O of SOLO) with a crisp wedge notch cut from its
lower-right, and one small solid dot centred inside — a lone voice lit by a spotlight. The
ring is a filled annulus (outer circle minus inner circle) with the notch removed, filled
with one Volt lime->spring gradient; the centred dot is TextPrimary. A soft black copy
offset +1.4 Y is the shadow.

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

# Plate: warm-obsidian radial glow, off-centre toward the top left.
PLATE_CENTRE = (40.0, 34.0)
PLATE_RADIUS = 100.0
PLATE_STOPS = [(0.0, (0x20, 0x25, 0x0F)), (0.55, (0x12, 0x12, 0x14)), (1.0, (0x0A, 0x0A, 0x0C))]

# Spotlight-O mark geometry (108-unit grid), identical to the vector drawables.
CENTRE = (54.0, 54.0)
RING_OUTER = 30.0
RING_INNER = 19.0
# Lower-right wedge notch, measured clockwise in screen coords (y down), 0deg = +x axis.
NOTCH_START = 300.0
NOTCH_END = 345.0
DOT = (54.0, 54.0, 6.0)
DOT_COLOUR = (0xF4, 0xF5, 0xF2)
# Ring linear gradient axis (top-left -> bottom-right) and stops.
RING_START, RING_END = (30.0, 30.0), (78.0, 78.0)
RING_STOPS = [(0.0, (0xE8, 0xFF, 0x8F)), (0.5, (0xD8, 0xFF, 0x3E)), (1.0, (0x9B, 0xE6, 0x4B))]
SHADOW = (0, 0, 0)
SHADOW_ALPHA = 0.28
SHADOW_DY = 1.4

# Notification small icon (24-unit artboard, white only): ring scaled about (12,12).
NOTIF_CENTRE = (12.0, 12.0)
NOTIF_RING_OUTER = 9.6
NOTIF_RING_INNER = 6.1
NOTIF_DOT = (12.0, 12.0, 1.9)


def _lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def _ramp(stops, t):
    t = min(1.0, max(0.0, t))
    for (o0, c0), (o1, c1) in zip(stops, stops[1:]):
        if o0 <= t <= o1:
            local = (t - o0) / (o1 - o0) if o1 > o0 else 0.0
            return _lerp(c0, c1, local)
    return stops[-1][1]


def _field(size, fn, mode="RGB", units=UNITS):
    """Evaluates a smooth colour field at a small size and scales it up."""
    work = min(size, 216)
    img = Image.new(mode, (work, work))
    px = img.load()
    for y in range(work):
        uy = (y + 0.5) / work * units
        for x in range(work):
            ux = (x + 0.5) / work * units
            px[x, y] = fn(ux, uy)
    return img.resize((size, size), Image.BICUBIC) if work != size else img


def plate(size):
    gx, gy = PLATE_CENTRE
    return _field(size, lambda x, y: _ramp(PLATE_STOPS, math.hypot(x - gx, y - gy) / PLATE_RADIUS))


def _ring_gradient(x, y, dy=0.0):
    """Colour of the ring's linear gradient at canvas point (x, y)."""
    sx, sy = RING_START
    ex, ey = RING_END
    dx, dyy = ex - sx, ey - sy
    return _ramp(RING_STOPS, ((x - sx) * dx + (y - (sy + dy)) * dyy) / (dx * dx + dyy * dyy))


def _notched_ring_mask(size, centre, r_out, r_in, dy=0.0, units=UNITS):
    """L mask of a filled annulus with the lower-right wedge removed."""
    k = size / units
    cx, cy = centre[0] * k, (centre[1] + dy) * k
    ro, ri = r_out * k, r_in * k
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((cx - ro, cy - ro, cx + ro, cy + ro), fill=255)
    d.ellipse((cx - ri, cy - ri, cx + ri, cy + ri), fill=0)
    # Carve the wedge: a filled triangle fan from the centre spanning the notch angles.
    span = NOTCH_END - NOTCH_START
    pts = [(cx, cy)]
    steps = 24
    reach = ro * 1.6
    for i in range(steps + 1):
        ang = math.radians(NOTCH_START + span * i / steps)
        pts.append((cx + reach * math.cos(ang), cy + reach * math.sin(ang)))
    d.polygon(pts, fill=0)
    return mask


def draw_mark(canvas, dy=0.0, colour=None, alpha=255):
    """Draws the notched ring and the dot onto an RGBA [canvas]."""
    size = canvas.size[0]
    k = size / UNITS

    ring_mask = _notched_ring_mask(size, CENTRE, RING_OUTER, RING_INNER, dy)
    dot_mask = Image.new("L", canvas.size, 0)
    cx, cy = DOT[0] * k, (DOT[1] + dy) * k
    r = DOT[2] * k
    ImageDraw.Draw(dot_mask).ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)

    if colour is not None:
        ring_fill = Image.new("RGB", canvas.size, colour)
        dot_fill = ring_fill
    else:
        ring_fill = _field(size, lambda x, y: _ring_gradient(x, y, dy))
        dot_fill = Image.new("RGB", canvas.size, DOT_COLOUR)
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    for fill, mask in ((ring_fill, ring_mask), (dot_fill, dot_mask)):
        piece = fill.convert("RGBA")
        piece.putalpha(mask.point(lambda v: v * alpha // 255))
        layer = Image.alpha_composite(layer, piece)
    return Image.alpha_composite(canvas, layer)


def artwork(size, with_plate=True):
    """Full 108-unit canvas: plate (optional), soft shadow and the Spotlight-O mark."""
    work = size * SUPERSAMPLE
    if with_plate:
        canvas = plate(work).convert("RGBA")
    else:
        canvas = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    canvas = draw_mark(canvas, dy=SHADOW_DY, colour=SHADOW, alpha=round(255 * SHADOW_ALPHA))
    canvas = draw_mark(canvas)
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
    """Artwork placeholder: a quiet graphite tile with a faint Spotlight-O."""
    work = size * SUPERSAMPLE
    base = _field(work, lambda x, y: _lerp((0x1C, 0x21, 0x16), (0x0E, 0x11, 0x0E), (x + y) / (2 * UNITS)))
    base = draw_mark(base.convert("RGBA"), alpha=46)
    return base.convert("RGB").resize((size, size), Image.LANCZOS)


def notification(size):
    """White-only small icon preview (24-unit artboard)."""
    work = size * SUPERSAMPLE
    k = work / 24.0
    mask = _notched_ring_mask(work, NOTIF_CENTRE, NOTIF_RING_OUTER, NOTIF_RING_INNER, units=24.0)
    d = ImageDraw.Draw(mask)
    cx, cy, r = NOTIF_DOT[0] * k, NOTIF_DOT[1] * k, NOTIF_DOT[2] * k
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    img = Image.new("RGBA", (work, work), (255, 255, 255, 0))
    img.putalpha(mask)
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
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, icon_size in densities.items():
        folder = os.path.join(RES, "mipmap-" + density)
        os.makedirs(folder, exist_ok=True)
        launcher_bitmap(icon_size, "squircle").save(
            os.path.join(folder, "ic_launcher.webp"), "WEBP", lossless=True
        )
        launcher_bitmap(icon_size, "circle").save(
            os.path.join(folder, "ic_launcher_round.webp"), "WEBP", lossless=True
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

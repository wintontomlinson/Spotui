#!/usr/bin/env python3
"""Regenerates every raster version of the Sonvra mark so it matches the vector icons.

The launcher itself uses the adaptive icon (minSdk is 26). The bitmaps written here are
the legacy mipmaps, the Play Store listing icon, the README icon and the artwork
placeholder, all drawn from the same geometry as drawable/ic_launcher_foreground.xml
(the "Sonic V" mark in the 108-unit adaptive grid) and
drawable/ic_launcher_background.xml (the midnight radial plate).

Sonic V: a bold V monogram made of two thick rounded capsule strokes that meet in a soft
vertex, filled with one azure gradient, plus a solid "source" dot inside the V's mouth,
like sound rising out of a resonance chamber.

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

# Plate: radial midnight glow, off-centre toward the top left.
PLATE_CENTRE = (40.0, 30.0)
PLATE_RADIUS = 100.0
PLATE_STOPS = [(0.0, (0x16, 0x24, 0x4A)), (0.55, (0x0B, 0x12, 0x26)), (1.0, (0x07, 0x09, 0x0D))]

# Mark geometry (108-unit grid), identical to the vector drawables.
ARMS = [((35.0, 38.0), (54.0, 74.0)), ((73.0, 38.0), (54.0, 74.0))]  # capsule centre lines
ARM_R = 7.0
DOT = (54.0, 42.0, 5.0)
DOT_COLOUR = (0xF2, 0xF5, 0xFA)
V_START, V_END = (35.0, 31.0), (54.0, 81.0)  # linear gradient axis
V_STOPS = [(0.0, (0x9C, 0xCB, 0xFF)), (0.5, (0x5B, 0x9B, 0xFF)), (1.0, (0x3F, 0x7B, 0xFF))]
SHADOW = (0, 0, 0)
SHADOW_ALPHA = 0.30
SHADOW_DY = 1.4

# Notification small icon (24-unit artboard, white only).
NOTIF_ARMS = [((3.96, 4.39), (12.0, 19.61)), ((20.04, 4.39), (12.0, 19.61))]
NOTIF_ARM_R = 2.96
NOTIF_DOT = (12.0, 6.08, 2.12)


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


def _v_gradient(x, y, scale=1.0):
    """Colour of the V's linear gradient at canvas point (x, y) for a mark scaled about 54,54."""
    sx, sy = 54 + (V_START[0] - 54) * scale, 54 + (V_START[1] - 54) * scale
    ex, ey = 54 + (V_END[0] - 54) * scale, 54 + (V_END[1] - 54) * scale
    dx, dy = ex - sx, ey - sy
    return _ramp(V_STOPS, ((x - sx) * dx + (y - sy) * dy) / (dx * dx + dy * dy))


def _capsules(draw, arms, r, to_px):
    """Draws each capsule (rectangle + two round ends) of [arms] into an L mask."""
    for a, b in arms:
        (ax, ay), (bx, by) = to_px(*a), to_px(*b)
        rp = r * math.dist((ax, ay), (bx, by)) / math.dist(a, b)
        length = math.dist((ax, ay), (bx, by))
        nx, ny = -(by - ay) / length * rp, (bx - ax) / length * rp
        draw.polygon([(ax + nx, ay + ny), (bx + nx, by + ny), (bx - nx, by - ny), (ax - nx, ay - ny)], fill=255)
        for cx, cy in ((ax, ay), (bx, by)):
            draw.ellipse((cx - rp, cy - rp, cx + rp, cy + rp), fill=255)


def draw_mark(canvas, scale=1.0, dy=0.0, colour=None, alpha=255):
    """Draws the V and the dot onto an RGBA [canvas]. [colour] forces one flat fill."""
    size = canvas.size[0]
    k = size / UNITS

    def to_px(x, y):
        return (54 + (x - 54) * scale) * k, (54 + (y - 54) * scale + dy) * k

    v_mask = Image.new("L", canvas.size, 0)
    _capsules(ImageDraw.Draw(v_mask), ARMS, ARM_R, to_px)  # radius follows to_px's scale
    dot_mask = Image.new("L", canvas.size, 0)
    cx, cy = to_px(DOT[0], DOT[1])
    r = DOT[2] * scale * k
    ImageDraw.Draw(dot_mask).ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)

    if colour is not None:
        v_fill = Image.new("RGB", canvas.size, colour)
        dot_fill = v_fill
    else:
        v_fill = _field(size, lambda x, y: _v_gradient(x, y - dy, scale))
        dot_fill = Image.new("RGB", canvas.size, DOT_COLOUR)
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    for fill, mask in ((v_fill, v_mask), (dot_fill, dot_mask)):
        piece = fill.convert("RGBA")
        piece.putalpha(mask.point(lambda v: v * alpha // 255))
        layer = Image.alpha_composite(layer, piece)
    return Image.alpha_composite(canvas, layer)


def artwork(size, scale=1.0, with_plate=True):
    """Full 108-unit canvas: plate (optional), soft shadow and the Sonic V mark."""
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
    """Artwork placeholder: a quiet graphite tile with a faint Sonic V."""
    work = size * SUPERSAMPLE
    base = _field(work, lambda x, y: _lerp((0x1C, 0x21, 0x2C), (0x0E, 0x11, 0x17), (x + y) / (2 * UNITS)))
    base = draw_mark(base.convert("RGBA"), 0.9, alpha=46)
    return base.convert("RGB").resize((size, size), Image.LANCZOS)


def notification(size):
    """White-only small icon preview (24-unit artboard)."""
    work = size * SUPERSAMPLE
    k = work / 24.0
    mask = Image.new("L", (work, work), 0)
    d = ImageDraw.Draw(mask)
    _capsules(d, NOTIF_ARMS, NOTIF_ARM_R, lambda x, y: (x * k, y * k))
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

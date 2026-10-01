#!/usr/bin/env python3
"""Regenerates every raster version of the SOLO mark so it matches the vector icons.

The launcher itself uses the adaptive icon (minSdk is 26). The bitmaps written here are
the legacy mipmaps, the Play Store listing icon, the README icon and the artwork
placeholder, all drawn from the same geometry as drawable/ic_launcher_foreground.xml
(the "Lumen Prism" mark in the 108-unit adaptive grid) and
drawable/ic_launcher_background.xml (the indigo radial plate).

Lumen Prism: a single upright beam of light descends from a gold beam-tip at the top,
strikes a refraction node and fans out into a short violet->cyan spectrum of three
diverging rays. It reads as "one voice -> pure sound / one light -> a spectrum", tying
to SOLO without copying the Spotlight-O ring, a Sonic V, a tuning fork or the Solo Facet,
and without resembling Spotify, YouTube Music, Apple Music, SoundCloud, JioSaavn, Gaana,
Wynk, Tidal, Deezer or Amazon Music. The beam is a tapered violet shaft, the fan rays run
violet -> indigo -> cyan, and the beam-tip is a warm gold diamond. A soft black copy
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

# Plate: indigo radial glow, off-centre toward the top left.
PLATE_CENTRE = (40.0, 34.0)
PLATE_RADIUS = 100.0
PLATE_STOPS = [(0.0, (0x1A, 0x16, 0x30)), (0.55, (0x13, 0x13, 0x1F)), (1.0, (0x0B, 0x0B, 0x14))]

# Lumen Prism mark geometry (108-unit grid), identical to the vector drawables.
# A beam descends from a gold tip, hits a refraction node and fans into three rays.
# The whole mark is balanced around the grid centre (54,54): beam top at y 28,
# node at y 52, fan reaching ~y 80, so it sits centred in the 66-unit safe circle.
TIP = (54.0, 28.0)          # top of the beam (gold diamond centre)
NODE = (54.0, 52.0)         # refraction node where the beam fans out
BEAM_TOP_HALF = 3.6         # beam half-width at the tip
BEAM_NODE_HALF = 4.6        # beam half-width at the node (slight flare toward the prism)
TIP_DIAMOND = 6.0           # gold diamond half-height at the tip

# Fan rays: angle measured clockwise from straight down (y+), screen coords.
# Three diverging rays violet -> indigo -> cyan, each a slim triangle from the node.
FAN_LEN = 28.0              # ray length from the node
FAN_HALF = 3.4             # ray half-width at its far end
FAN_RAYS = (
    (-38.0, (0xB7, 0x9C, 0xFF)),   # violet, swings left
    (0.0, (0x7C, 0x5C, 0xFF)),     # indigo-violet, straight down
    (38.0, (0x3F, 0xE0, 0xD0)),    # cyan, swings right
)
BEAM_STOPS = [(0.0, (0xB7, 0x9C, 0xFF)), (1.0, (0x7C, 0x5C, 0xFF))]
GOLD = (0xF5, 0xC9, 0x7A)

SHADOW = (0, 0, 0)
SHADOW_ALPHA = 0.28
SHADOW_DY = 1.4

# Notification small icon (24-unit artboard, white only): mark scaled about (12,12).
NOTIF_SCALE = 24.0 / UNITS
NOTIF_CENTRE = (12.0, 12.0)


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


def _poly(scale, offset, pts):
    """Scales 108-unit [pts] by [scale] with a (dx, dy) screen [offset]."""
    dx, dy = offset
    return [(x * scale + dx, y * scale + dy) for x, y in pts]


def _beam_pts(dy=0.0):
    """Tapered beam quad from the gold tip down to the refraction node."""
    tx, ty = TIP[0], TIP[1] + dy
    nx, ny = NODE[0], NODE[1] + dy
    return [
        (tx - BEAM_TOP_HALF, ty),
        (tx + BEAM_TOP_HALF, ty),
        (nx + BEAM_NODE_HALF, ny),
        (nx - BEAM_NODE_HALF, ny),
    ]


def _tip_pts(dy=0.0):
    """Gold diamond capping the beam at the top."""
    tx, ty = TIP[0], TIP[1] + dy
    h = TIP_DIAMOND
    w = BEAM_TOP_HALF + 1.6
    return [(tx, ty - h), (tx + w, ty), (tx, ty + h * 0.35), (tx - w, ty)]


def _ray_pts(angle_deg, dy=0.0):
    """Slim triangle fan ray from the node at [angle_deg] off straight-down."""
    nx, ny = NODE[0], NODE[1] + dy
    a = math.radians(angle_deg)
    # Direction straight down (0,1) rotated by angle.
    dirx, diry = math.sin(a), math.cos(a)
    perpx, perpy = diry, -dirx
    fx, fy = nx + dirx * FAN_LEN, ny + diry * FAN_LEN
    return [
        (nx, ny),
        (fx + perpx * FAN_HALF, fy + perpy * FAN_HALF),
        (fx - perpx * FAN_HALF, fy - perpy * FAN_HALF),
    ]


def _poly_mask(size, pts_units, dy=0.0, units=UNITS):
    k = size / units
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).polygon([(x * k, y * k) for x, y in pts_units], fill=255)
    return mask


def _beam_gradient(x, y, dy=0.0):
    """Colour along the beam shaft from tip (violet) to node (deeper violet)."""
    t = (y - (TIP[1] + dy)) / max(1e-3, (NODE[1] - TIP[1]))
    return _ramp(BEAM_STOPS, t)


def draw_mark(canvas, dy=0.0, colour=None, alpha=255):
    """Draws the beam, gold tip and the violet->cyan fan onto an RGBA [canvas]."""
    size = canvas.size[0]
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))

    # Fan rays first (behind the beam), then the beam, then the gold tip on top.
    pieces = []
    for angle, col in FAN_RAYS:
        pieces.append((_ray_pts(angle, dy), col))
    beam_pts = _beam_pts(dy)
    tip_pts = _tip_pts(dy)

    for pts, col in pieces:
        fill_col = colour if colour is not None else col
        fill = Image.new("RGB", canvas.size, fill_col)
        piece = fill.convert("RGBA")
        piece.putalpha(_poly_mask(size, pts, dy).point(lambda v: v * alpha // 255))
        layer = Image.alpha_composite(layer, piece)

    # Beam shaft (gradient, or flat for shadow/monochrome).
    if colour is not None:
        beam_fill = Image.new("RGB", canvas.size, colour)
    else:
        beam_fill = _field(size, lambda x, y: _beam_gradient(x, y, dy))
    beam = beam_fill.convert("RGBA")
    beam.putalpha(_poly_mask(size, beam_pts, dy).point(lambda v: v * alpha // 255))
    layer = Image.alpha_composite(layer, beam)

    # Gold tip.
    tip_col = colour if colour is not None else GOLD
    tip_fill = Image.new("RGB", canvas.size, tip_col)
    tip = tip_fill.convert("RGBA")
    tip.putalpha(_poly_mask(size, tip_pts, dy).point(lambda v: v * alpha // 255))
    layer = Image.alpha_composite(layer, tip)

    return Image.alpha_composite(canvas, layer)


def artwork(size, with_plate=True):
    """Full 108-unit canvas: plate (optional), soft shadow and the Lumen Prism mark."""
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
    """Artwork placeholder: a quiet indigo tile with a faint Lumen Prism."""
    work = size * SUPERSAMPLE
    base = _field(work, lambda x, y: _lerp((0x1A, 0x16, 0x30), (0x0E, 0x0E, 0x16), (x + y) / (2 * UNITS)))
    base = draw_mark(base.convert("RGBA"), alpha=46)
    return base.convert("RGB").resize((size, size), Image.LANCZOS)


def notification(size):
    """White-only small icon preview (24-unit artboard)."""
    work = size * SUPERSAMPLE
    mask = Image.new("L", (work, work), 0)
    # Reuse the mark polygons scaled from 108 units into the 24-unit artboard.
    scale = NOTIF_SCALE
    off = (NOTIF_CENTRE[0] - 54.0 * scale, NOTIF_CENTRE[1] - 54.0 * scale)
    k = work / 24.0
    d = ImageDraw.Draw(mask)
    for angle, _ in FAN_RAYS:
        pts = _poly(scale, off, _ray_pts(angle))
        d.polygon([(x * k, y * k) for x, y in pts], fill=255)
    d.polygon([(x * k, y * k) for x, y in _poly(scale, off, _beam_pts())], fill=255)
    d.polygon([(x * k, y * k) for x, y in _poly(scale, off, _tip_pts())], fill=255)
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

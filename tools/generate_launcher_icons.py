#!/usr/bin/env python3
"""Regenerates every raster version of the SOLO mark so it matches the vector icons.

The launcher itself uses the adaptive icon (minSdk is 26). The bitmaps written here are
the legacy mipmaps, the Play Store listing icon, the README icon and the artwork
placeholder, all drawn from the same geometry as drawable/ic_launcher_foreground.xml
(the "Aperture S" mark in the 108-unit adaptive grid) and
drawable/ic_launcher_background.xml (the graphite radial plate).

Aperture S: a single continuous stroke sweeps through two opposed arcs to form the
letter S; the counters it leaves behind — the two bowls of the S — read as the open
blades of a camera/lens aperture framing a focused centre. It reads as "one voice ->
one focused sound", tying to SOLO without copying the retired Lumen Prism beam, a
Spotlight-O ring, a Sonic V, a tuning fork or the Solo Facet, and without resembling
Spotify, YouTube Music, Apple Music, SoundCloud, JioSaavn, Gaana, Wynk, Tidal, Deezer
or Amazon Music. The stroke runs a soft-sky -> azure -> cobalt gradient top-to-bottom
on a graphite plate, with a cool-steel focal dot at the aperture centre. A soft black
copy offset +1.4 Y is the shadow.

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

# Plate: graphite radial glow, off-centre toward the top left.
PLATE_CENTRE = (40.0, 34.0)
PLATE_RADIUS = 100.0
PLATE_STOPS = [(0.0, (0x17, 0x1B, 0x20)), (0.55, (0x12, 0x15, 0x19)), (1.0, (0x0E, 0x10, 0x13))]

# Aperture S mark geometry (108-unit grid), identical to the vector drawables.
# The S is one continuous stroke built from two tangent arcs that meet at the waist
# (54,54) where the stroke runs vertical. The upper bowl is a circle centred LEFT of the
# waist (so its right flank forms the top hook curving down-and-right); the lower bowl is
# a circle centred RIGHT of the waist (its left flank forms the bottom hook curving
# up-and-left). Because both centres sit at the waist's y, the two arcs share a vertical
# tangent there and join seamlessly into an S. A small focal dot sits at the aperture
# centre. The whole mark is balanced around the grid centre (54,54) so it sits inside the
# 66-unit safe circle.
CENTRE = (54.0, 54.0)
BOWL_R = 14.0          # radius of each bowl arc centreline
STROKE_HALF = 6.2      # half the stroke width (filled, so this is the thickness)
TERMINAL_TRIM = 14.0   # degrees trimmed off each open terminal so the S reads crisp
DOT_R = 3.6            # cool-steel focal dot at the aperture centre

# Upper bowl centre sits ABOVE the waist, lower bowl centre BELOW it; both on the waist x.
UPPER_CENTRE = (CENTRE[0], CENTRE[1] - BOWL_R)
LOWER_CENTRE = (CENTRE[0], CENTRE[1] + BOWL_R)

# Gradient down the stroke: soft sky -> azure -> cobalt.
STROKE_STOPS = [(0.0, (0x93, 0xC5, 0xFD)), (0.5, (0x3B, 0x82, 0xF6)), (1.0, (0x1D, 0x4E, 0xD8))]
STEEL = (0xCB, 0xD5, 0xE1)   # cool-steel focal dot
ACCENT = (0x3B, 0x82, 0xF6)

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


def _arc_outline(centre, r, half, a0, a1, steps=48):
    """Returns a filled-polygon outline tracing a stroked arc from a0 to a1 (degrees)."""
    cx, cy = centre
    outer, inner = [], []
    for i in range(steps + 1):
        a = math.radians(a0 + (a1 - a0) * i / steps)
        dx, dy = math.cos(a), math.sin(a)
        outer.append((cx + dx * (r + half), cy + dy * (r + half)))
        inner.append((cx + dx * (r - half), cy + dy * (r - half)))
    return outer + inner[::-1]


def _s_polys(dy=0.0):
    """The two tangent arcs forming an upright S.

    Angles are screen coords (0deg = +x/right, 90deg = +y/down, growing clockwise). The
    upper bowl is centred above the waist: it starts at the top-right terminal (~ -45deg)
    and sweeps counter-clockwise over the top and round the left down to the waist
    (90deg, the bottom of this circle), making the upper hook of the S. The lower bowl is
    centred below the waist: it starts at the waist (-90deg = top of this circle) and
    sweeps counter-clockwise round the right and under the bottom to the bottom-left
    terminal (~135deg), making the lower hook. They share a horizontal tangent at the
    waist, so the stroke flows as one continuous upright S.
    """
    uc = (UPPER_CENTRE[0], UPPER_CENTRE[1] + dy)
    lc = (LOWER_CENTRE[0], LOWER_CENTRE[1] + dy)
    t = TERMINAL_TRIM
    # Upper hook: top-right terminal (~ -45deg) -> top -> left -> waist (-270deg = 90deg).
    upper = _arc_outline(uc, BOWL_R, STROKE_HALF, -45.0 - t, -270.0)
    # Lower hook: waist (-90deg) -> right -> bottom -> bottom-left terminal (~135deg).
    lower = _arc_outline(lc, BOWL_R, STROKE_HALF, -90.0, 135.0 + t)
    return upper, lower


def _poly(scale, offset, pts):
    """Scales 108-unit [pts] by [scale] with a (dx, dy) screen [offset]."""
    dx, dy = offset
    return [(x * scale + dx, y * scale + dy) for x, y in pts]


def _poly_mask(size, pts_units, units=UNITS):
    k = size / units
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).polygon([(x * k, y * k) for x, y in pts_units], fill=255)
    return mask


def _stroke_gradient(x, y, dy=0.0):
    """Colour down the S stroke from the top (soft sky) to the bottom (cobalt)."""
    top = CENTRE[1] - BOWL_R * 2 - STROKE_HALF + dy
    bot = CENTRE[1] + BOWL_R * 2 + STROKE_HALF + dy
    t = (y - top) / max(1e-3, (bot - top))
    return _ramp(STROKE_STOPS, t)


def draw_mark(canvas, dy=0.0, colour=None, alpha=255):
    """Draws the two S bowls and the steel focal dot onto an RGBA [canvas]."""
    size = canvas.size[0]
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))

    upper, lower = _s_polys(dy)

    # S stroke (gradient, or flat for shadow/monochrome).
    if colour is not None:
        stroke_fill = Image.new("RGB", canvas.size, colour)
    else:
        stroke_fill = _field(size, lambda x, y: _stroke_gradient(x, y, dy))

    for pts in (upper, lower):
        piece = stroke_fill.convert("RGBA")
        piece.putalpha(_poly_mask(size, pts).point(lambda v: v * alpha // 255))
        layer = Image.alpha_composite(layer, piece)

    # Cool-steel focal dot at the aperture centre.
    k = size / UNITS
    dot_col = colour if colour is not None else STEEL
    dot_mask = Image.new("L", (size, size), 0)
    cx, cy = CENTRE[0] * k, (CENTRE[1] + dy) * k
    r = DOT_R * k
    ImageDraw.Draw(dot_mask).ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    dot_fill = Image.new("RGB", canvas.size, dot_col).convert("RGBA")
    dot_fill.putalpha(dot_mask.point(lambda v: v * alpha // 255))
    layer = Image.alpha_composite(layer, dot_fill)

    return Image.alpha_composite(canvas, layer)


def artwork(size, with_plate=True):
    """Full 108-unit canvas: plate (optional), soft shadow and the Aperture S mark."""
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
    """Artwork placeholder: a quiet graphite tile with a faint Aperture S."""
    work = size * SUPERSAMPLE
    base = _field(work, lambda x, y: _lerp((0x17, 0x1B, 0x20), (0x0E, 0x10, 0x13), (x + y) / (2 * UNITS)))
    base = draw_mark(base.convert("RGBA"), alpha=46)
    return base.convert("RGB").resize((size, size), Image.LANCZOS)


def notification(size):
    """White-only small icon preview (24-unit artboard)."""
    work = size * SUPERSAMPLE
    mask = Image.new("L", (work, work), 0)
    scale = NOTIF_SCALE
    off = (NOTIF_CENTRE[0] - 54.0 * scale, NOTIF_CENTRE[1] - 54.0 * scale)
    k = work / 24.0
    d = ImageDraw.Draw(mask)
    upper, lower = _s_polys()
    for pts in (upper, lower):
        sp = _poly(scale, off, pts)
        d.polygon([(x * k, y * k) for x, y in sp], fill=255)
    # Focal dot.
    cx = (CENTRE[0] * scale + off[0]) * k
    cy = (CENTRE[1] * scale + off[1]) * k
    r = DOT_R * scale * k
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

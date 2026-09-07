#!/usr/bin/env python3
"""Regenerates the raster launcher icons so they match the adaptive icon vectors.

The launcher itself uses the adaptive icon (minSdk is 26), so these bitmaps exist
for the Play Store listing and as fallback assets. Keeping them in step with
drawable/ic_launcher_background.xml and drawable/ic_launcher_foreground.xml stops
the project from shipping two different looking marks.

Run from the repository root:
    python3 tools/generate_launcher_icons.py
"""

from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw

RES = os.path.join("app", "src", "main", "res")
PLAYSTORE = os.path.join("app", "src", "main", "ic_launcher-playstore.png")

# Everything below is expressed in the 108 unit space the vectors use.
VIEWPORT = 108.0
SUPERSAMPLE = 4

PLATE_STOPS = [
    (0.00, (0xFF, 0xC7, 0x60)),
    (0.42, (0xF5, 0xA5, 0x24)),
    (1.00, (0xC9, 0x6A, 0x05)),
]
PLATE_FROM = (6.0, 0.0)
PLATE_TO = (102.0, 108.0)

HIGHLIGHT_CENTRE = (30.0, 24.0)
HIGHLIGHT_RADIUS = 76.0
HIGHLIGHT_STOPS = [(0.00, 0x59), (0.55, 0x14), (1.00, 0x00)]

SHADE_CENTRE = (92.0, 98.0)
SHADE_RADIUS = 62.0
SHADE_COLOUR = (0x7A, 0x3C, 0x02)
SHADE_STOPS = [(0.00, 0x3D), (1.00, 0x00)]

GLYPH_DARK = (0x1C, 0x13, 0x03)
GLYPH_SHADOW = (0x5C, 0x2E, 0x02, 0x3D)

# Waveform bars, matching drawable/ic_launcher_foreground.xml. Each entry is the
# centre x of a bar and the total height of its pill, rounded ends included.
BAR_WIDTH = 8.4
BARS = (
    (30.0, 26.0),
    (42.0, 43.0),
    (54.0, 58.0),
    (66.0, 41.0),
    (78.0, 28.0),
)
CENTRE = (54.0, 54.0)


def _interpolate(stops, t):
    """Value of a stop list at position t, where stops are (offset, value) pairs."""
    if t <= stops[0][0]:
        return stops[0][1]
    if t >= stops[-1][0]:
        return stops[-1][1]
    for (o0, v0), (o1, v1) in zip(stops, stops[1:]):
        if o0 <= t <= o1:
            local = (t - o0) / (o1 - o0) if o1 > o0 else 0.0
            if isinstance(v0, tuple):
                return tuple(round(a + (b - a) * local) for a, b in zip(v0, v1))
            return round(v0 + (v1 - v0) * local)
    return stops[-1][1]


def _plate(size):
    """The amber background, built small and scaled up since it is a smooth ramp."""
    work = 160
    base = Image.new("RGB", (work, work))
    pixels = base.load()
    ax, ay = PLATE_FROM
    bx, by = PLATE_TO
    dx, dy = bx - ax, by - ay
    span = dx * dx + dy * dy
    for y in range(work):
        vy = (y + 0.5) / work * VIEWPORT
        for x in range(work):
            vx = (x + 0.5) / work * VIEWPORT
            t = ((vx - ax) * dx + (vy - ay) * dy) / span
            pixels[x, y] = _interpolate(PLATE_STOPS, min(1.0, max(0.0, t)))

    overlay = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    over_pixels = overlay.load()
    hx, hy = HIGHLIGHT_CENTRE
    sx, sy = SHADE_CENTRE
    for y in range(work):
        vy = (y + 0.5) / work * VIEWPORT
        for x in range(work):
            vx = (x + 0.5) / work * VIEWPORT
            hd = math.dist((vx, vy), (hx, hy)) / HIGHLIGHT_RADIUS
            alpha = _interpolate(HIGHLIGHT_STOPS, min(1.0, hd))
            over_pixels[x, y] = (255, 255, 255, alpha)
    base = Image.alpha_composite(base.convert("RGBA"), overlay)

    overlay = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    over_pixels = overlay.load()
    for y in range(work):
        vy = (y + 0.5) / work * VIEWPORT
        for x in range(work):
            vx = (x + 0.5) / work * VIEWPORT
            sd = math.dist((vx, vy), (sx, sy)) / SHADE_RADIUS
            alpha = _interpolate(SHADE_STOPS, min(1.0, sd))
            over_pixels[x, y] = SHADE_COLOUR + (alpha,)
    base = Image.alpha_composite(base, overlay)

    return base.resize((size, size), Image.LANCZOS)


def _draw_mark(size, scale, colour):
    """Draws the waveform bars on a transparent layer of the given size."""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    unit = size / VIEWPORT
    cx, cy = CENTRE
    half_width = BAR_WIDTH * scale / 2.0

    for bar_x, bar_height in BARS:
        x = cx + (bar_x - cx) * scale
        half_height = bar_height * scale / 2.0
        draw.rounded_rectangle(
            (
                (x - half_width) * unit,
                (cy - half_height) * unit,
                (x + half_width) * unit,
                (cy + half_height) * unit,
            ),
            radius=half_width * unit,
            fill=colour,
        )

    return layer


def _shape_mask(size, shape):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if shape == "circle":
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    elif shape == "squircle":
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=size * 0.235, fill=255)
    else:
        draw.rectangle((0, 0, size - 1, size - 1), fill=255)
    return mask


def build_icon(size, shape, glyph_scale):
    work = size * SUPERSAMPLE
    icon = _plate(work)

    shadow = _draw_mark(work, glyph_scale, GLYPH_SHADOW)
    offset = round(1.4 * work / VIEWPORT)
    shifted = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    shifted.paste(shadow, (0, offset))
    icon = Image.alpha_composite(icon, shifted)
    icon = Image.alpha_composite(icon, _draw_mark(work, glyph_scale, GLYPH_DARK + (255,)))

    icon.putalpha(_shape_mask(work, shape))
    return icon.resize((size, size), Image.LANCZOS)


def build_foreground(size, glyph_scale):
    work = size * SUPERSAMPLE
    layer = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    shadow = _draw_mark(work, glyph_scale, GLYPH_SHADOW)
    offset = round(1.4 * work / VIEWPORT)
    shifted = Image.new("RGBA", (work, work), (0, 0, 0, 0))
    shifted.paste(shadow, (0, offset))
    layer = Image.alpha_composite(layer, shifted)
    layer = Image.alpha_composite(layer, _draw_mark(work, glyph_scale, GLYPH_DARK + (255,)))
    return layer.resize((size, size), Image.LANCZOS)


def main():
    # A legacy icon is not cropped the way an adaptive icon is, so the mark can be
    # a little larger inside the plate without leaving the visible area.
    legacy_scale = 1.18
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
        build_icon(icon_size, "squircle", legacy_scale).save(
            os.path.join(folder, "ic_launcher.webp"), "WEBP", lossless=True
        )
        build_icon(icon_size, "circle", legacy_scale).save(
            os.path.join(folder, "ic_launcher_round.webp"), "WEBP", lossless=True
        )
        build_foreground(foreground_size, 1.0).save(
            os.path.join(folder, "ic_launcher_foreground.webp"), "WEBP", lossless=True
        )
        print("wrote", folder)

    build_icon(512, "square", legacy_scale).convert("RGB").save(PLAYSTORE, "PNG")
    print("wrote", PLAYSTORE)


if __name__ == "__main__":
    main()

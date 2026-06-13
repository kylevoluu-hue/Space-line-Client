#!/usr/bin/env python3
"""Generates the Space~line "black hole" app icon as a multi-size .ico (+ .png).

Pure standard library: renders each size procedurally, PNG-encodes it, and packs
the PNGs into a Vista-style ICO container. No external deps.
"""
import math, struct, zlib, os

OUT_DIR = "branding"
os.makedirs(OUT_DIR, exist_ok=True)

# Palette (R,G,B) 0..1
NAVY_TOP = (0.043, 0.063, 0.125)
NAVY_BOT = (0.016, 0.027, 0.059)
RING_CYAN = (0.216, 0.878, 1.0)
RING_BLUE = (0.298, 0.553, 1.0)
RING_MAG = (0.768, 0.419, 1.0)
STARS = [(-0.32, -0.30), (0.30, -0.34), (0.36, 0.28), (-0.36, 0.24), (0.05, -0.40), (-0.12, 0.40)]


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def clamp01(v):
    return 0.0 if v < 0 else 1.0 if v > 1 else v


def ring_color(angle):
    # Continuous cyan -> magenta -> blue -> cyan cycle (wraps with no seam).
    t = (angle / (2 * math.pi)) % 1.0
    if t < 1 / 3:
        return lerp(RING_CYAN, RING_MAG, t * 3)
    if t < 2 / 3:
        return lerp(RING_MAG, RING_BLUE, (t - 1 / 3) * 3)
    return lerp(RING_BLUE, RING_CYAN, (t - 2 / 3) * 3)


def render(size):
    px = bytearray()
    half = 0.5
    rr = 0.17  # corner radius for the rounded-square mask
    for y in range(size):
        for x in range(size):
            fx = (x + 0.5) / size - 0.5
            fy = (y + 0.5) / size - 0.5
            r = math.hypot(fx, fy)

            # Rounded-square alpha mask.
            dx = max(abs(fx) - (half - rr), 0.0)
            dy = max(abs(fy) - (half - rr), 0.0)
            dist_rr = math.hypot(dx, dy) - rr
            alpha = clamp01(-dist_rr * size * 1.2)
            if alpha <= 0:
                px += bytes((0, 0, 0, 0))
                continue

            # Background vertical gradient + gentle vignette.
            base = lerp(NAVY_TOP, NAVY_BOT, (fy + 0.5))
            col = [base[0], base[1], base[2]]

            # Stars.
            for sx, sy in STARS:
                d = math.hypot(fx - sx, fy - sy)
                s = math.exp(-(d * size / 1.4) ** 2)
                col = [min(1.0, c + s * 0.9) for c in col]

            # Accretion ring.
            angle = math.atan2(fy, fx)
            r0 = 0.34
            sigma = 0.055
            ring = math.exp(-(((r - r0) / sigma) ** 2))
            rc = ring_color(angle)
            glow = math.exp(-(((r - 0.30) / 0.16) ** 2)) * 0.35
            for i in range(3):
                col[i] = min(1.0, col[i] + ring * rc[i] * 1.4 + glow * rc[i])

            # Event horizon (black core) with a thin bright rim.
            if r < 0.255:
                rim = math.exp(-(((r - 0.255) / 0.012) ** 2))
                core = [rim * RING_CYAN[i] for i in range(3)]
                col = core
            elif r < 0.275:
                rim = math.exp(-(((r - 0.262) / 0.02) ** 2))
                for i in range(3):
                    col[i] = min(1.0, col[i] + rim * RING_CYAN[i])

            px += bytes((int(clamp01(col[0]) * 255),
                         int(clamp01(col[1]) * 255),
                         int(clamp01(col[2]) * 255),
                         int(alpha * 255)))
    return bytes(px)


def png_bytes(size, rgba):
    raw = bytearray()
    stride = size * 4
    for y in range(size):
        raw.append(0)
        raw += rgba[y * stride:(y + 1) * stride]

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)

    out = b"\x89PNG\r\n\x1a\n"
    out += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    out += chunk(b"IEND", b"")
    return out


def main():
    sizes = [256, 64, 48, 32, 16]
    pngs = {s: png_bytes(s, render(s)) for s in sizes}

    # Save the largest as a standalone PNG (used by the Fabric mod + docs).
    with open(os.path.join(OUT_DIR, "spaceline.png"), "wb") as f:
        f.write(pngs[256])

    # Pack an ICO with PNG payloads (supported on Windows Vista and later).
    entries = b""
    images = b""
    offset = 6 + 16 * len(sizes)
    for s in sizes:
        data = pngs[s]
        w = 0 if s == 256 else s
        entries += struct.pack("<BBBBHHII", w, w, 0, 0, 1, 32, len(data), offset)
        images += data
        offset += len(data)
    ico = struct.pack("<HHH", 0, 1, len(sizes)) + entries + images
    with open(os.path.join(OUT_DIR, "spaceline.ico"), "wb") as f:
        f.write(ico)
    print("Wrote branding/spaceline.ico and branding/spaceline.png")


if __name__ == "__main__":
    main()

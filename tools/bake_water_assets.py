"""Bake repeat-safe water maps from the committed source plates.

Run with Pillow + NumPy. Outputs are committed; Gradle does not require Python.
Normal PNGs: RGB = tangent XYZ, A = height. Foam PNG: R=bubbles, G=wisps.
"""
from pathlib import Path
import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[1] / "android/assets/textures/water"


def periodic(a):
    """Remove the smooth boundary discontinuity with a periodic Poisson solve."""
    h, w = a.shape
    boundary = np.zeros_like(a)
    boundary[0] = a[-1] - a[0]
    boundary[-1] = -boundary[0]
    boundary[:, 0] += a[:, -1] - a[:, 0]
    boundary[:, -1] -= a[:, -1] - a[:, 0]
    denominator = 2 * np.cos(2 * np.pi * np.arange(w) / w)[None, :] + 2 * np.cos(2 * np.pi * np.arange(h) / h)[:, None] - 4
    denominator[0, 0] = 1
    smooth_fft = np.fft.fft2(boundary) / denominator
    smooth_fft[0, 0] = 0
    return a - np.fft.ifft2(smooth_fft).real


def read_height(name, size):
    im = Image.open(ROOT / name).convert("L").resize((size, size), Image.Resampling.LANCZOS)
    return np.asarray(im.filter(ImageFilter.GaussianBlur(0.7)), dtype=np.float32) / 255


def png(name, data):
    path = ROOT / name
    Image.fromarray(np.uint8(np.clip(data, 0, 1) * 255 + .5)).save(path, optimize=True)
    print(name, path.stat().st_size)


for kind, strength in (("choppy", 13), ("swell", 10)):
    h = periodic(read_height(f"ocean_height_{kind}_01.jpg", 1024))
    h = np.clip((h - np.percentile(h, 1)) / (np.percentile(h, 99) - np.percentile(h, 1)), 0, 1)
    dx = (np.roll(h, -1, axis=1) - np.roll(h, 1, axis=1)) * strength
    dz = (np.roll(h, -1, axis=0) - np.roll(h, 1, axis=0)) * strength
    n = np.stack((-dx, -dz, np.ones_like(h)), axis=-1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True)
    png(f"ocean_normal_{kind}_01.png", np.dstack((n * .5 + .5, h)))

bubbly = np.clip(periodic(read_height("foam_bubbly_01.jpg", 1024)), 0, 1)
wispy = np.clip(periodic(read_height("foam_wispy_01.jpg", 1024)), 0, 1)
png("ocean_foam_masks.png", np.dstack((bubbly, wispy, (bubbly + wispy) * .5)))

# sky_golden_horizon.jpg contains a grayscale height image, not a sky. Use the
# actual sky plate, crop away its photographed ocean, and wrap it into a hemisphere.
plate = Image.open(ROOT / "sky_overcast_equirect.jpg").convert("RGB")
plate = plate.crop((0, 0, plate.width, int(plate.height * .85)))
upper = np.asarray(plate.resize((1024, 512), Image.Resampling.LANCZOS), dtype=np.float32) / 255
# Mirrored azimuth closes the seam; zenith blends to a uniform pole.
upper = np.concatenate((upper, upper[:, ::-1]), axis=1)
pole = upper[:16].mean(axis=(0, 1))
blend = np.clip(np.linspace(0, 1, 512)[:, None, None] * 7, 0, 1)
upper = upper * blend + pole * (1 - blend)
# Cool the lower amber plate to daylight, matching the reference's blue sky.
upper[:, :, 0] *= .92
upper[:, :, 2] = np.clip(upper[:, :, 2] * 1.06, 0, 1)
lower = np.repeat(upper[-1:, :, :], 512, axis=0)
png("ocean_sky_environment.png", np.concatenate((upper, lower), axis=0))

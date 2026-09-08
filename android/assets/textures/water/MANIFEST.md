# Water textures from Summer (Gemini), 2026-09-08

| File | Intended use |
| --- | --- |
| `sky_overcast_equirect.jpg` | Overcast maritime sky / env plate (wide) |
| `sky_golden_horizon.jpg` | Clear/golden horizon sky plate |
| `foam_bubbly_01.jpg` | Foam / whitecap mask (black=water, white=foam) |
| `foam_wispy_01.jpg` | Alternate foam mask |
| `ocean_height_choppy_01.jpg` | Ocean height / displacement (grayscale) |
| `ocean_height_swell_01.jpg` | Alternate height / swell (grayscale) |

Notes:
- Height maps are grayscale, not tangent-space normals yet — convert in shader or bake `ocean_normal_*.png`.
- Foam maps are black-background white foam; use as mask.
- Skies are RGB plates; prefer golden for sunny voyage, overcast as weather variant.

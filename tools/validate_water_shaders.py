"""Compile/link both water quality paths as GLSL ES 1.00 (the Android GLES2 target).
Usage: python3 tools/validate_water_shaders.py /path/to/glslangValidator
"""
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1] / "assets/shaders"
common = (root / "voyage-sky-common.glsl").read_text()
with tempfile.TemporaryDirectory(prefix="nanhai-water-glsl-") as folder:
    for kind, prefix in (("water", ""), ("water", "#define LOW_QUALITY\n"), ("sky", "")):
        paths = []
        for stage in ("vert", "frag"):
            source = (root / f"voyage-{kind}.{stage}").read_text().replace("// SKY_FUNCTIONS", common)
            path = Path(folder) / f"{kind}.{stage}"
            path.write_text("#version 100\n" + prefix + source)
            paths.append(str(path))
        subprocess.run([sys.argv[1], "-l", *paths], check=True)
print("GLES2 SHADER PASS: high water, low water and sky compile/link as GLSL ES 1.00")

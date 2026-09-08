# 0.28.6 ship and harbor art

The existing ship variants now use nine thin sail sections per mast, timber battens,
thin-box standing rigging, a red geometric sail seal, hull plank strakes, stern
window lattice and a swept stern roof. Variant dimensions and sail counts are retained.

Each port has 10–13 terraced halls, swept segmented hip roofs with tile ribs,
galleries, red pillars, emissive lanterns, dock planks/piles and additional green
mountain crags. Decorative pier boats were moved inward to fit the existing
collision envelope. No navigation radii or save schema changed.

One intersecting water draw was removed to eliminate depth-buffer striping.
This does not implement physically based water or a shoreline depth shader.

Geometry is generated once and static port scenery uses the existing ModelCache.
No external service, model files or texture downloads are needed. Android hardware
frame rates have not been measured; desktop OpenGL smoke tests are not a substitute
for a mid-range phone performance benchmark.

The result remains stylized procedural art. Hand-authored hull curvature, carved
architecture, painted texture atlases, cloth animation and richer rigging would
benefit from artist-authored assets (GLB or another supported asset pipeline).
External GLB assets are not required to run this release.

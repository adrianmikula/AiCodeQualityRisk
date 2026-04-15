You are generating icon assets for an IntelliJ Platform plugin. Follow JetBrains’ official requirements EXACTLY.

## Objective

Produce a complete, valid plugin icon set that complies 100% with JetBrains Marketplace and IDE requirements.

## Source Design

Use the provided base icon (refactor cube with dependency tree concept). Maintain:

* Flat design
* Sharp geometric edges
* 2–3 colour palette
* High contrast for visibility at small sizes
* No text or small illegible details

## Required Output Files

### 1. Plugin Logo (Marketplace + IDE)

Create:

* `pluginIcon.svg` (PRIMARY file, required)

Specifications:

* Format: SVG (vector, no raster embedding)
* Canvas: 40x40 px
* Must scale cleanly to 16x16
* No padding or excessive whitespace
* Shapes must be crisp at small sizes
* Avoid strokes < 1px when scaled

### 2. Fallback Raster Icons (Recommended)

Generate PNG versions for compatibility:

* `pluginIcon_40.png` → 40x40 px
* `pluginIcon_80.png` → 80x80 px (2x retina)

Specifications:

* Format: PNG
* Transparent background
* Pixel-perfect alignment
* No anti-aliasing blur on edges

## Design Constraints (CRITICAL)

* Icon must remain recognizable at 16x16 px
* Use bold shapes, not fine details
* Avoid text, letters, or small symbols
* Ensure strong silhouette
* Keep visual balance centered
* Avoid gradients unless extremely subtle (flat preferred)

## IntelliJ Style Alignment

* Match JetBrains icon style:

  * Clean
  * Minimal
  * Slightly bold
* Avoid skeuomorphic or overly detailed styles
* Prefer solid fills over outlines

## Validation Checklist

Before finalizing, verify:

* SVG renders correctly at 16x16, 20x20, 40x40
* No clipping or overflow خارج canvas
* No embedded raster images inside SVG
* File size is optimized (no unnecessary metadata)
* Visual clarity preserved when scaled down

## Output

Return:

* SVG code (pluginIcon.svg)
* Exported PNG files (40px and 80px)
* Brief confirmation that all constraints are satisfied

Do not deviate from these specifications.

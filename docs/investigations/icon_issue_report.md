# Icon Investigation Report — EntropyGuard Plugin

**Documented:** 2026-05-02  
**Investigator:** Kilo (Automated Analysis)  
**Scope:** Plugin logo (Marketplace icon) and toolbar/action icons  
**Relevant Commits:** `fd3fad3`, `c670a88`, `b6f9d76`, `a0367de` (HEAD)

---

## Executive Summary

Two separate icon configuration issues were identified:

1. **Marketplace/Main Plugin Icon** — **completely missing** from plugin manager due to using PNG in wrong location (`icons/pluginIcon_32.png`) instead of required SVG in `META-INF/pluginIcon.svg`.

2. **Toolbar/Action Icons** — rendering as solid white blocks due to **missing leading slash** in icon paths (`icons/pluginIcon_16.png` instead of `/icons/pluginIcon_16.png`), causing incorrect path resolution to `META-INF/`.

---

## Historical Timeline of Icon-Related Changes

### State A — Works (Before `fd3fad3`)
- **Plugin icon**: `icons/pluginIcon_32.png`
- **Action icons**: `icons/pluginIcon_16.png`
- **iconMapper**: Not used
- **Status**: Icons displayed (though not strictly compliant with modern JetBrains requirements)

### State B — First Fix Attempt (`fd3fad3`)
> "First attempt at fixing the toolbar icon"

- Added New UI support with `icons/expui/` directory containing proper SVG icons
- Created `EntropyGuardIconMappings.json` mapping file
- **Added** `<iconMapper mappingFile="EntropyGuardIconMappings.json"/>` extension
- Changed plugin icon reference: `icon="icons/expui/pluginIcon.svg"`
- Changed action icons: `icon="AllIcons.General.Inspection"`
- **Intention**: Support New UI + proper icon mapping
- **Result**: Likely broke due to incorrect icon attribute paths and missing plugin logo placement

### State C — Second Attempt (`c670a88`)
> "Second attempt at adding a toolbar icon"

- **Reverted** plugin icon back to `icons/pluginIcon_32.png`
- **Reverted** action icons back to `icons/pluginIcon_16.png`
- **Kept** the `<iconMapper mappingFile="EntropyGuardIconMappings.json"/>` extension declaration
- **Deleted** `EntropyGuardIconMappings.json` file (dangling reference!)
- **Deleted** `icons/expui/` directory contents from source tree
- **Result**: Created a **broken iconMapper reference** pointing to non-existent file → icon loading failure

### State D — Cleanup (`b6f9d76`)
> "Prepare v1.1.0 release..."

- **Removed** the dangling `<iconMapper>` extension entirely
- Restored stable plugin.xml without iconMapper
- No icon files restored to `icons/expui/`
- **Status**: Stable but still misconfigured (plugin logo wrong location/format)

### State E — Current HEAD (`a0367de`)
- Documentation update only
- No icon changes

---

## Official JetBrains Requirements (as of May 2025)

Sources:
- [Icons — IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/icons.html)
- [Plugin Icon File — IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html)
- [Icons Style Guide — IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/icons-style.html)

### Plugin Logo (Marketplace Display)

| Requirement | Correct Configuration | EntropyGuard Current |
|---|---|---|
| **Format** | SVG only | ❌ PNG |
| **Location** | `META-INF/pluginIcon.svg` | ❌ `icons/pluginIcon_32.png` |
| **Size** | 40×40 design (scales to 80×80) | ❌ 32×32 raster |
| **Dark variant** | Optional `pluginIcon_dark.svg` | ❌ None |
| **Plugin.xml** | `<idea-plugin icon="META-INF/pluginIcon.svg">` or omit | ❌ `icon="icons/pluginIcon_32.png"` |

> "All Plugin Logo images must be in SVG format. This vector image format is required because the Plugin Logo file must be small (ideally less than 2-3kB), and the image must scale without any loss of quality."  
> — [Plugin Logo Documentation](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html)

### Action / Toolbar Icons

| Requirement | Correct Configuration | EntropyGuard Current |
|---|---|---|
| **Base size** | 16×16 px | ⚠️ 16×16 (PNG) |
| **Format** | SVG recommended | ⚠️ PNG acceptable but inferior |
| **Path format** | Leading `/` required: `/icons/pluginIcon.svg` | ❌ `icons/pluginIcon_16.png` (missing `/`) |
| **Dark variant** | `icon_dark.svg` if needed | ❌ None |
| **Location** | `src/main/resources/icons/` | ✅ Correct directory |

> "The path to the icon passed in as argument to `IconLoader.getIcon()` must start with leading `/`."  
> — [Working with Icons — Icons Class](https://plugins.jetbrains.com/docs/intellij/icons.html)

### Why Path Matters

Paths referenced in `plugin.xml` are **relative to the resources root**, not the XML file's directory.

Using `icon="icons/pluginIcon_16.png"` (no leading `/`) is ambiguous and may be interpreted relative to `META-INF/` → IDE looks in `META-INF/icons/` which doesn't exist.

Correct form: `icon="/icons/pluginIcon_16.png"` with leading `/` denotes absolute classpath resource path.

---

## Root Cause Analysis

### Why the Marketplace Icon Disappeared

The plugin descriptor declares:

```xml
<idea-plugin ... icon="icons/pluginIcon_32.png">
```

**What the IDE actually looks for:**

1. For Marketplace display: it reads the `icon` attribute value
2. It loads the resource **relative to the plugin's root classpath**
3. It expects **SVG format** per modern requirements (2024.3+)

Because:
- The path points to a PNG (non-SVG) — IDE may reject it in newer versions
- The path lacks `META-INF/` prefix — may not be resolved correctly
- The plugin icon should be in `META-INF/` by convention and marketplace packaging rules

**Effect**: No icon appears in Plugin Manager/Marketplace.

---

### Why Toolbar Icons Show Solid White Blocks

The action icons are declared:

```xml
<action ... icon="icons/pluginIcon_16.png">
```

**Path resolution:**

| Path Type | With leading `/` | Without leading `/` |
|---|---|---|
| **Meaning** | Absolute (from resources root) | Relative to plugin.xml location (`META-INF/`) |
| **Lookup location** | `classpath:/icons/pluginIcon_16.png` | `classpath:/META-INF/icons/pluginIcon_16.png` |
| **Result** | ✅ Found | ❌ Not found → blank icon |

**Visual effect**: IDE renders a solid 16×16 white rectangle placeholder.

---

## Additional Contributing Factors

### New UI Incomplete Migration Attempt

The `fd3fad3` commit added New UI support (expui icons + iconMapper) then `c670a88` deleted the files but kept the extension reference → dangling config. This was later removed, but demonstrates instability in icon handling.

### SVG Renderability at 16×16

The current `icons/pluginIcon.svg` is a **complex, 2048×2048 detailed gradient-based illustration**. While it's legally vector format, it may render poorly at 16×16 due to:

- Too many path details that blur together at small size
- Gradients that may be flattened to solid color at small scale
- No crisp pixel-aligned shapes

**Official guidance**: For action icons, "flat design with sharp geometric edges" and "no strokes < 1px when scaled" is required. The marketplace SVG is a detailed illustration, not an action icon. Using it for toolbar (16×16) is **not appropriate**.

Better approach: create a **simplified, 16×16-optimized** SVG or PNG with clear silhouette for toolbar use.

---

## Recommended Fixes

### Fix 1 — Plugin Logo (Primary)

1. **Move** `src/main/resources/icons/pluginIcon.svg` → `src/main/resources/META-INF/pluginIcon.svg`
2. **Update `plugin.xml` line 1:**
   ```xml
   <idea-plugin ... icon="META-INF/pluginIcon.svg">
   ```
   Or, omit `icon` attribute entirely and rely on default `META-INF/pluginIcon.svg` auto-discovery.

3. **Ensure SVG is valid**: has `width="40"` and `height="40"` (or scalable design). Current icon uses `width="1024"` — this is acceptable as it scales down, but verify it remains recognizable at 40×40.

### Fix 2 — Toolbar Action Icons

**Option A (Quick fix):**

Update paths to include leading slash:

```xml
<!-- Line 176 -->
<action
    ...
    icon="/icons/pluginIcon_16.png">

<!-- Line 185 -->
<action
    ...
    icon="/icons/pluginIcon_16.png">
```

**Option B (Recommended):**

Replace PNG with a proper 16×16 SVG:

1. Create `src/main/resources/icons/analyze_current_file.svg` (simplified icon)
2. Create dark variant `icons/analyze_current_file_dark.svg` if needed
3. Update references:
   ```xml
   <action icon="/icons/analyze_current_file.svg" ... />
   ```

### Fix 3 — Cleanup Unused Configuration

- Confirm `EntropyGuardIconMappings.json` and `icons/expui/` are **not referenced** in current code (they're not, as of HEAD)
- No further action needed — the iconMapper extension was correctly removed in `b6f9d76`

---

## Validation Checklist

After applying fixes, verify:

- [ ] Plugin icon appears in **Plugin Manager** (Settings → Plugins → Installed)
- [ ] Action icons appear in **Editor context menu** and **Tools menu**
- [ ] Icons render correctly in **Darcula (dark) theme**
- [ ] Icons render correctly in **Light theme**
- [ ] Plugin builds successfully with `./gradlew buildPlugin`
- [ ] Marketplace preview shows correct icon (test with `./gradlew publishPlugin -PpublishChannel=...` dry-run or verify ZIP contents)

---

## References

- JetBrains Plugin Icon Requirements: [docs/intellij/plugin-icon-file.html](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html)
- Working with Icons: [docs/intellij/icons.html](https://plugins.jetbrains.com/docs/intellij/icons.html)
- Icons Style Guide: [docs/intellij/icons-style.html](https://plugins.jetbrains.com/docs/intellij/icons-style.html)
- Plugin Configuration: [docs/intellij/plugin-configuration-file.html](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)

---

## Appendix — Git Diff Summary

```diff
# Between fd3fad3 (first attempt) and HEAD (a0367de):

Deleted:
  src/main/resources/EntropyGuardIconMappings.json
  src/main/resources/icons/expui/pluginIcon.svg
  src/main/resources/icons/expui/pluginIcon@20x20.svg
  src/main/resources/icons/expui/pluginIcon@20x20_dark.svg
  src/main/resources/icons/expui/pluginIcon_dark.svg
  src/main/resources/icons/expui/pluginIcon_toolbar.svg

Modified: src/main/resources/META-INF/plugin.xml
  - Removed <iconMapper mappingFile="..."/> extension (restored to no-mapper state)
  - Changed plugin icon from "icons/expui/pluginIcon.svg" → "icons/pluginIcon_32.png"
  - Changed action icons from "AllIcons.General.Inspection" → "icons/pluginIcon_16.png"

Present but untracked in source tree (exist in build/ only):
  build/resources/main/icons/expui/   # compiled resources, not source
```

**Conclusion**: The `icons/expui/` directory was removed from `src/main/resources/` but remains in the `build/` directory from previous compilation — giving a false impression that the files exist in source. They do not.

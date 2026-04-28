# IntelliJ Plugin Icon Generation Workflow

This document outlines the correct steps for generating and configuring monochrome toolbar icons for IntelliJ plugins using the New UI icon theming system.

## Overview

IntelliJ's New UI requires monochrome SVG icons with specific naming conventions and color values to support automatic theming (light/dark modes). This workflow ensures proper icon display across all UI contexts.

## Prerequisites

- Original monochrome SVG icon file
- Light theme color: `#6C707E`
- Dark theme color: `#CED0D6`

## Preserving Full Icon Detail

When converting to the New UI monochrome format, it's critical to preserve all detail from the original icon to maintain visual fidelity. Follow these steps:

### 2.1 Start from Monochrome Source

Begin with a monochrome version of your icon (e.g., `pluginIcon_monochrome.svg`). This should:
- Contain all paths and elements from the original detailed icon
- Use a single fill color (typically black `#000000` or a neutral gray)
- Preserve gradients as solid fills if needed
- Maintain the same viewBox as the original (e.g., `viewBox="0 0 2048 2048"`)

### 2.2 Preserve All Paths

**Do not simplify or optimize the SVG**. Keep:
- All path elements (75+ paths is common for detailed icons)
- All sub-paths and compound shapes
- All geometric details, no matter how small
- The original viewBox dimensions

### 2.3 Only Modify Required Attributes

When creating variants, change ONLY these attributes:
- `width` and `height` (for sizing: 20×20 or 16×16)
- `fill` color (for theming: `#6C707E` or `#CED0D6`)
- Add `style="display: block;"` to the svg element
- Change `preserveAspectRatio` to `"none"`

**Do NOT modify:**
- Path data (`d` attribute)
- Number of paths
- Group structure
- Transform attributes
- ViewBox

### 2.4 Why This Matters

IntelliJ's New UI theming system works best with detailed monochrome icons because:
- Small details remain visible at small sizes
- The icon retains its recognizability
- Theming (color changes) applies uniformly across all elements
- No visual information is lost during conversion

### 2.5 Example Transformation

**Original monochrome source:**
```xml
<svg viewBox="0 0 2048 2048" width="40" height="40">
  <path fill="#000000" d="M 1019.44 1213.41 C ..."/>
  <path fill="#000000" d="M 1173 1298.62 C ..."/>
  <!-- 73 more paths -->
</svg>
```

**New UI light theme variant (20×20):**
```xml
<svg version="1.1" xmlns="http://www.w3.org/2000/svg" style="display: block;" viewBox="0 0 2048 2048" width="20" height="20" preserveAspectRatio="none">
  <path fill="#6C707E" d="M 1019.44 1213.41 C ..."/>
  <path fill="#6C707E" d="M 1173 1298.62 C ..."/>
  <!-- 73 more paths - identical path data -->
</svg>
```

**New UI dark theme variant (20×20):**
```xml
<svg version="1.1" xmlns="http://www.w3.org/2000/svg" style="display: block;" viewBox="0 0 2048 2048" width="20" height="20" preserveAspectRatio="none">
  <path fill="#CED0D6" d="M 1019.44 1213.41 C ..."/>
  <path fill="#CED0D6" d="M 1173 1298.62 C ..."/>
  <!-- 73 more paths - identical path data -->
</svg>
```

Notice that only the `width`, `height`, and `fill` attributes change. All path data remains identical.

## Step-by-Step Process

### 1. Create expui Directory Structure

Create the expui directory for New UI icons:
```
src/main/resources/icons/expui/
```

### 2. Generate SVG Icon Variants

Create 4 SVG variants with the following specifications:

#### 2.1 Standard Size (20×20)
- **Light theme**: `pluginIcon@20x20.svg`
  - Dimensions: `width="20" height="20"`
  - Fill color: `#6C707E`
  
- **Dark theme**: `pluginIcon@20x20_dark.svg`
  - Dimensions: `width="20" height="20"`
  - Fill color: `#CED0D6`

#### 2.2 Compact Mode Size (16×16)
- **Light theme**: `pluginIcon.svg`
  - Dimensions: `width="16" height="16"`
  - Fill color: `#6C707E`
  
- **Dark theme**: `pluginIcon_dark.svg`
  - Dimensions: `width="16" height="16"`
  - Fill color: `#CED0D6`

**Note**: The compact mode files use the base filename without size suffix, as per IntelliJ's naming conventions.

### 3. Create Icon Mappings JSON

Create `src/main/resources/EntropyGuardIconMappings.json` to map old icon references to new expui icons:

```json
{
  "iconMappings": [
    {
      "iconId": "/icons/pluginIcon.svg",
      "iconPath": "/icons/expui/pluginIcon.svg"
    },
    {
      "iconId": "/icons/pluginIcon_16.png",
      "iconPath": "/icons/expui/pluginIcon.svg"
    },
    {
      "iconId": "/icons/pluginIcon_13.png",
      "iconPath": "/icons/expui/pluginIcon.svg"
    },
    {
      "iconId": "/icons/pluginIcon_monochrome.svg",
      "iconPath": "/icons/expui/pluginIcon.svg"
    }
  ]
}
```

**Critical**: The mapping file uses a flat structure with an "iconMappings" array. Each entry maps an old icon reference to the new expui icon path.

This mapping allows the iconMapper extension to translate legacy icon references to the new expui icons.

### 4. Register Icon Mapper Extension

Add the iconMapper extension to `src/main/resources/META-INF/plugin.xml`:

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- ... other extensions ... -->
    
    <!-- Icon Mapper for New UI -->
    <iconMapper mappingFile="EntropyGuardIconMappings.json"/>
</extensions>
```

**Critical**: Use the `mappingFile` attribute (not `file`) as specified in the official IntelliJ documentation.

### 5. Update Main Plugin Icon Reference

The main plugin icon reference in the `<idea-plugin>` tag should reference the expui path:

```xml
<idea-plugin 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xsi:noNamespaceSchemaLocation="https://plugins.jetbrains.com/schemas/plugin/2/plugin.xsd" 
    icon="icons/expui/pluginIcon.svg">
```

**Critical**: The icon attribute should reference the expui path (`icons/expui/pluginIcon.svg`) directly. This is the correct approach for the newer IntelliJ Platform plugin system.

### 6. Remove Programmatic Icon Loading

Remove any programmatic icon loading from Kotlin/Java code:

#### 6.1 Action Classes
Remove icon initialization from action classes:

**Before:**
```kotlin
import com.intellij.openapi.util.IconLoader

class AnalyzeCurrentFileAction : AnAction() {
    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/pluginIcon_16.png", AnalyzeCurrentFileAction::class.java)
    }
    // ...
}
```

**After:**
```kotlin
class AnalyzeCurrentFileAction : AnAction() {
    // Icon loaded automatically via iconMapper
    // ...
}
```

#### 6.2 Startup Activities
Remove icon loading from tool window registration:

**Before:**
```kotlin
import com.intellij.openapi.util.IconLoader

val toolWindowIcon = IconLoader.getIcon("/icons/pluginIcon_13.png", javaClass)
val toolWindow = toolWindowManager.registerToolWindow(
    RegisterToolWindowTask.notClosable(
        id = "EntropyGuard",
        anchor = ToolWindowAnchor.RIGHT,
        icon = toolWindowIcon
    )
)
```

**After:**
```kotlin
// Icon loaded automatically via iconMapper
val toolWindow = toolWindowManager.registerToolWindow(
    RegisterToolWindowTask.notClosable(
        id = "EntropyGuard",
        anchor = ToolWindowAnchor.RIGHT
    )
)
```

### 7. Verify Icon Structure

Ensure the final directory structure is:

```
src/main/resources/
├── META-INF/
│   └── plugin.xml
├── EntropyGuardIconMappings.json
└── icons/
    ├── expui/
    │   ├── pluginIcon@20x20.svg
    │   ├── pluginIcon@20x20_dark.svg
    │   ├── pluginIcon.svg
    │   └── pluginIcon_dark.svg
    └── [old icons - can be removed if no longer needed]
```

## Testing

1. Build and run the plugin in a development IDE
2. Verify the icon appears correctly in:
   - Plugin list (Settings/Preferences → Plugins)
   - Tool window
   - Action menus
3. Test theme switching between light and dark modes
4. Verify icon color changes appropriately:
   - Light theme: `#6C707E`
   - Dark theme: `#CED0D6`
   - Active state: White (automatic)

## Common Pitfalls

1. **Incorrect iconMapper attribute**: Use `mappingFile` attribute, not `file` attribute in plugin.xml
2. **Incorrect JSON structure**: The mapping file should use a flat structure (no nested `expui` block)
3. **Incorrect icon path in plugin.xml**: The main plugin icon should reference the expui path (`icons/expui/pluginIcon.svg`) directly, not the old path
4. **Missing dark theme variants**: Both light and dark variants are required for proper theming
5. **Wrong dimensions**: Use 20×20 for standard and 16×16 for compact mode
6. **Incorrect fill colors**: Use exactly `#6C707E` for light and `#CED0D6` for dark
7. **Forgetting to remove programmatic loading**: Old IconLoader calls will override the iconMapper

## Color Reference

- **Light theme fill**: `#6C707E` (gray-600 in JetBrains UI)
- **Dark theme fill**: `#CED0D6` (gray-200 in JetBrains UI)
- **Active state**: White (automatically applied by IntelliJ)
- **Disabled state**: Gray (automatically applied by IntelliJ)

## Additional Resources

- [IntelliJ Platform Icons Documentation](https://plugins.jetbrains.com/docs/intellij/icons.html)
- [New UI Icon Guidelines](https://plugins.jetbrains.com/docs/intellij/new-ui-icons-guidelines.html)

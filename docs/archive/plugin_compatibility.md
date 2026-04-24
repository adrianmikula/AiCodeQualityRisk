# Plugin Compatibility Configuration

## Overview

This document describes how the IntelliJ plugin compatibility range is configured and how to modify it.

## The Two Configuration Points

The plugin compatibility is defined in two places:

### 1. plugin.xml

Located at `src/main/resources/META-INF/plugin.xml`:

```xml
<idea-version since-build="233" />
```

- `since-build="233"` specifies the minimum IntelliJ version (233 = 2023.3)
- The comment indicates the intent: "Exclude the until-build attribute entirely, giving open-ended compatibility with all future IntelliJ versions"

### 2. build.gradle.kts

Located at `build.gradle.kts`:

```kotlin
patchPluginXml {
    sinceBuild.set(providers.gradleProperty("intellij.sinceBuild").orElse("233"))
    untilBuild.set(providers.gradleProperty("intellij.untilBuild").orElse(""))
}
```

**IMPORTANT:** The Gradle build script overrides plugin.xml values. Even if plugin.xml has no `until-build`, the Gradle `patchPluginXml` task will set one if configured.

## Current Configuration

- **Default since-build:** 233 (matches plugin.xml)
- **Default until-build:** "" (empty string = no upper limit, giving open-ended compatibility)
- **Resulting compatibility range:** `233.* — *` (all future IntelliJ versions)

## Overriding via Gradle Properties

To set specific compatibility limits, pass Gradle properties:

```bash
# Set both since and until builds
./gradlew -Pintellij.sinceBuild=241 -Pintellij.untilBuild=251.* buildPlugin

# Set only since-build (leave until-build open)
./gradlew -Pintellij.sinceBuild=241 buildPlugin

# Set a specific range
./gradlew -Pintellij.sinceBuild=233 -Pintellij.untilBuild=243.* buildPlugin
```

## Build Number Reference

| IntelliJ Version | Build Number |
|------------------|--------------|
| 2023.3           | 233         |
| 2023.4           | 234         |
| 2024.1           | 241         |
| 2024.2           | 242         |
| 2024.3           | 243         |
| 2025.1           | 251         |

## Troubleshooting

### Issue: Limited Compatibility Range Shows in Marketplace

If the JetBrains Marketplace shows a limited range like "241.0 — 251.*" instead of open-ended:

1. Check build.gradle.kts for explicit `untilBuild.set("251.*")` or similar
2. Ensure `untilBuild` is set to an empty string `""` for open-ended compatibility
3. Run `./gradlew patchPluginXml` and check the generated `build/distributions/*.zip` file's `plugin.xml`

### Verify the Generated Plugin

After building, inspect the generated plugin.xml:

```bash
cd build/distributions
unzip -p *.zip "AI Code Quality Risk-*/resources/META-INF/plugin.xml" | grep "idea-version"
```

Expected output (open-ended):
```xml
<idea-version since-build="233"/>
```

Problematic output (capped):
```xml
<idea-version since-build="233" until-build="251.*"/>
```

## History

- **2026-04-15:** Updated to use Gradle properties for flexible configuration (matching JakartaMigrationMCP/premium-intellij-plugin)
- Previously, `untilBuild.set("251.*")` was hardcoded, causing limited compatibility display
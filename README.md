# AI Code Quality Risk IntelliJ Plugin (MVP v1)

This repository contains a Kotlin-based IntelliJ plugin MVP that analyzes in-progress code changes and shows a risk score with findings and explanations.

## Implemented v1 Features

- Diff/change capture with staged Git diff preference and fallback behavior
- Debounced, cancellable, latest-only analysis pipeline
- Local deterministic analyzer adapter (`LocalMockAnalyzerClient`)
- Tool window UI showing risk score, findings, and explanations
- Lightweight inline editor risk indicator
- Save, edit, focus, and manual-triggered analysis paths

## Project Structure

- `src/main/kotlin/com/aicodequalityrisk/plugin/model` - analysis contracts and view state
- `src/main/kotlin/com/aicodequalityrisk/plugin/capture` - diff/input capture
- `src/main/kotlin/com/aicodequalityrisk/plugin/pipeline` - orchestration and latest-only scheduling
- `src/main/kotlin/com/aicodequalityrisk/plugin/analysis` - analyzer client + local mock implementation
- `src/main/kotlin/com/aicodequalityrisk/plugin/ui` - tool window factory and panel
- `src/main/kotlin/com/aicodequalityrisk/plugin/editor` - inline line painter
- `src/main/kotlin/com/aicodequalityrisk/plugin/startup` - event wiring on project startup
- `src/main/resources/META-INF/plugin.xml` - plugin descriptor and registrations

## Prerequisites

- JDK 21 installed on this machine
- Bash shell available

Note: this workspace may be mounted with `noexec`, so prefer running wrapper commands via `bash gradlew ...`.

## Test

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH" \
bash gradlew test
```

## Run IntelliJ Sandbox

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH" \
bash gradlew runIde
```

## Usage in IDE

- Open any source file and type/edit to trigger analysis automatically.
- Save to trigger explicit save-path analysis.
- Switch editor tabs/files to trigger focus-based analysis.
- Right-click in editor and use `Analyze Current File Risk` for manual trigger.
- Open the `AI Code Risk` tool window to view score, findings, and explanations.

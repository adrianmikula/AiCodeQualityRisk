# AI Code Quality Risk IntelliJ Plugin (MVP v1)

This repository contains a Kotlin-based IntelliJ plugin MVP that analyzes in-progress code changes and shows a risk score with findings and explanations.

## Language Support

- **Java**: Full AST-based analysis with detailed metrics (cyclomatic complexity, nesting depth, method lengths, etc.)
- **Other Languages**: Diff-based pattern analysis (security issues, code smells, etc.)

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

## Setup with Mise (Recommended)

[Mise](https://mise.jdx.dev/) is used to manage development tools and common commands.

1. Install Mise: `curl https://mise.jdx.dev/install.sh | sh`
2. Add to your shell: `echo "eval \"\$(~/.local/bin/mise activate bash)\"" >> ~/.bashrc`
3. In the project directory: `mise trust` && `mise install`

Common commands:
- `mise run build` - Build the project
- `mise run test` - Run tests
- `mise run run-ide` - Run IntelliJ sandbox
- `mise run clean` - Clean build artifacts
- `mise run check` - Run all checks
- `mise run assemble` - Assemble the plugin
- `mise run install` - Install distribution

## Manual Setup (Alternative)

If not using Mise, ensure JDK 21 is available.

### Test

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH" \
bash gradlew test
```

### Run IntelliJ Sandbox

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
- Open the `EntropyGuard` tool window to view score, findings, and explanations.

## Code Generator (Experimental)

The `src/main/kotlin/com/aicodequalityrisk/generator` module contains an experimental code generation tool for studying AI code quality degradation over iterative development.

### Overview

Generates Java Spring Boot projects using LLMs and analyzes code quality metrics to understand how code quality evolves during iterative feature addition.

### Usage

```bash
bash gradlew run
```

Or with custom config:

```bash
bash gradlew run --args="config/generator.json"
```

### Configuration

Key settings in `config/generator.json`:

- `promptTemplates[].iterationFeatures` - sequential features to add
- `modes` - `SINGLE_SHOT` (all at once) vs `ITERATIVE` (one by one)
- `variationsPerPrompt` - number of independent runs

### Output

- Generated projects: `workspace/generated/{project-id}/`
- Results: `workspace/generated/results.csv`

Metrics tracked:
- Duplicate string/number literals
- Duplicate method calls
- Similar method counts
- Lines of code (LOC)

### Experiment Design

Compares code quality between:
- Single-shot generation (all features in one prompt)
- Iterative generation (features added sequentially, each building on previous code)

Analyzes whether quality naturally degrades over long iteration sequences without explicit quality requirements.

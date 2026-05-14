# SlopGuard [formerly EntropyGuard] — AI Code Quality Protection

SlopGuard [formerly EntropyGuard] is an IntelliJ IDEA plugin that catches the hidden quality issues introduced by AI-generated code. It detects security vulnerabilities, duplicate boilerplate, and structural decay that compound over time without obvious failures.

## Overview

AI-generated code isn't broken—it's slowly rotting your codebase. Each AI-generated commit adds hidden technical debt: hardcoded secrets, repetitive boilerplate, and anti-patterns that don't fail immediately but make your code harder to maintain, debug, and extend over time. SlopGuard [formerly EntropyGuard] catches these issues before they compound.

## Core Capabilities

### Multi-Paradigm Detection Engine

**Categories of Detection**

**COMPLEXITY** (8 rules)
- Long methods (>50 lines), deep nesting (>3 levels)
- High cyclomatic complexity (>10), complex method structure
- Heavy boolean logic (>3 operators), long if/else-if chains (>2)
- Null propagation patterns (.orElse(null)), excessive null returns

**DUPLICATION** (4 rules)
- Duplicate string literals (>2), duplicate number literals (>2)
- Repeated method calls (>3 duplicates), large method signatures (>4 parameters)

**PERFORMANCE** (2 rules)
- Thread.sleep calls, magic number usage (>3)

**SECURITY** (10 rules)
- Null assertion (!! operator) — HIGH severity
- Broad exception catch, empty catch blocks
- Plaintext password comparison
- Hardcoded API tokens (AWS, GitHub, Google, Stripe, JWT, Bearer, Basic auth)
- Placeholder domains (example.com, localhost, etc.)
- Hardcoded configuration (URLs, passwords, endpoints)

**CORRUPTION** (4 rules) — *New in v1.1.0*
- Parse failure (both JavaParser + Tree-sitter fail)
- Markdown tokens (code fences, file markers)
- Unbalanced braces (>2)
- Mixed language content (>30% prose density)

**Additional entropy-based detection:**
- Boilerplate bloat (excessive getters/setters)
- Verbose comment spam
- Over-defensive programming
- Poor naming
- Framework misuse
- Excessive documentation

**Detection Techniques**
- **JavaParser AST**: Extracts structural metrics from Java code
- **Tree-sitter fuzzy matching**: Pattern similarity detection for AI-generated boilerplate across multiple languages
- **Dual-parser corruption**: Identifies malformed or non-code content

## Results

- Overall risk score (0-100)
- Up to 7 prioritized findings with title, detail, severity, category, file, and line
- Results stored in `.aicodequalityrisk/latest-scan.json`

**Severity Levels**
- HIGH: Security issues, corrupted content
- MEDIUM: Code smells, duplication, moderate issues
- LOW: Minor issues (long comments, TODO markers)

### UI Integration
- **Tool Window**: Dedicated EntropyGuard panel with score, findings, explanations
- **Inline Editor**: Colored line painters highlight risky lines directly in editor
- **Actions**: 
  - `Analyze Current File Risk` (editor popup menu)
  - `Analyze Project Risk` (Tools menu)

### Model Context Protocol (MCP) Server
- Exposes analysis results via MCP protocol
- JSON-RPC based communication over stdin/stdout
- Provides tools for scanning and retrieving results
- Currently available but not registered in plugin.xml (temporarily disabled per v1.0.2)

## Technical Architecture

**Languages & Frameworks**
- **Primary**: Java (full AST analysis via JavaParser)
- **Secondary**: Kotlin, Python, JavaScript, TypeScript (Tree-sitter diff-based)
- **Frameworks**: Spring, React, Angular, Vue.js, Django, Flask patterns recognized
- **Build Tools**: Maven, Gradle, npm, yarn, pip

**Key Technologies**
- JavaParser for Java AST analysis
- Tree-sitter for fuzzy detection + multi-language support
- Kotlin + kotlinx.serialization + SnakeYAML
- IntelliJ Platform SDK (243.*+)

## Installation

### From JetBrains Marketplace
1. Open IntelliJ IDEA Settings/Preferences → Plugins
2. Search for "SlopGuard [formerly EntropyGuard]" or "EntropyGuard"
3. Click Install
4. Restart IDE

### Manual Installation
1. Download plugin from [GitHub Releases](https://github.com/adrianmikula/EntropyGuard/releases)
2. Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk
3. Select the `.zip` or `.jar` file
4. Restart IDE

**Compatibility**: IntelliJ IDEA 2023.3+ (Community & Ultimate)

## Usage

### Automatic (Real-Time)
- Open any Java/Kotlin/Python/JS/TS file
- Edit code → analysis triggers automatically after cooldown (30s for edits)
- View results in `EntropyGuard` tool window

### Manual Triggers
- **Save file** → explicit save-path analysis
- **Switch tabs** → focus-based analysis
- **Right-click in editor** → `Analyze Current File Risk`
- **Tools menu** → `Analyze Project Risk`

### Interpreting Results
- **Score 0-30**: Low risk — code is reasonably clean
- **Score 31-60**: Medium risk — some hotspots to review
- **Score 61-100**: High risk — significant structural issues

Check findings list; prioritize HIGH severity items first. Click findings to jump to source lines.

## Configuration

### Rule Adjustments
Rules defined in `src/main/resources/config/analysis-rules.yaml` (packaged in plugin). Advanced users can copy and modify locally via `Help → Edit Custom Properties` (future: external config override).

### Excluded Files
Analysis automatically skips: `.md`, `.class`, `.tst`, `.log`, `.jar` files.

## Project Structure (Source)

```
src/main/kotlin/com/aicodequalityrisk/plugin/
├── model/           # Data contracts (AnalysisInput, RiskResult, Finding)
├── capture/         # Diff/input capture (DiffCaptureService)
├── pipeline/        # Orchestration (AnalysisOrchestrator, LatestOnlyRunner)
├── analysis/        # Analysis engines
│   ├── ASTAnalyzer.kt          # JavaParser metrics extractor
│   ├── TreeSitterFuzzyDetector.kt  # Fuzzy duplicate detection
│   ├── CorruptedSourceDetector.kt   # Corruption detection
│   ├── LocalMockAnalyzerClient.kt   # Rule engine + scoring
│   ├── RuleFactory.kt          # YAML rule parser
│   ├── AnalysisConfigLoader.kt # Config loader
│   ├── ASTMetrics.kt           # AST metric data class
│   ├── FuzzyMetrics.kt         # Fuzzy detection results
│   ├── CorruptedSourceMetrics.kt  # Corruption metrics
│   ├── EntropyScoreCalculator.kt  # Category sub-score calculators
│   └── (Enhanced fingerprinting, adaptive thresholds, etc.)
├── ui/              # Tool window (RiskToolWindowFactory, RiskToolWindowPanel)
├── editor/          # Inline painter (InlineRiskLinePainter)
├── startup/         # Project open wiring (PluginStartupActivity)
├── state/           # Persistence (AnalysisStateStore)
├── service/         # Services (LicenseService, McpServerService)
├── actions/         # Menu actions (AnalyzeCurrentFileAction, AnalyzeProjectAction)
└── mcp/             # Model Context Protocol server (currently unused)
```

## Development

### Prerequisites
- JDK 21
- Bash shell
- (Optional) [Mise](https://mise.jdx.dev/) for tool management

### Setup with Mise (Recommended)
```bash
# Install Mise
curl https://mise.jdx.dev/install.sh | sh

# In project directory
mise trust && mise install
```

### Common Commands
```bash
mise run build       # Build plugin
mise run test        # Run tests
mise run run-ide     # Launch IntelliJ sandbox
mise run check       # Lint + typecheck
mise run assemble    # Build distribution ZIP
mise run install     # Install to IDE sandbox
```

### Manual Build
```bash
bash gradlew build
```

### Run in Sandbox
```bash
bash gradlew runIde
```

Plugin outputs to `build/distributions/EntropyGuard-*.zip`.

## Code Generator (Experimental)

Generates Java Spring Boot projects using LLMs to study code quality evolution during iterative development.

### Run
```bash
bash gradlew run
# or with custom config
bash gradlew run --args="config/generator.json"
```

### Output
- Generated projects: `workspace/generated/{project-id}/`
- Results: `workspace/generated/results.csv`

Tracks metrics: duplicate literals, duplicate method calls, similar methods, LOC.

## License

This plugin is proprietary software. Free 7-day trial; unlimited usage requires license.

## Change Log

### v1.1.1 — 2026-05-14
- Updated marketing copy to better address AI-generated code quality issues
- Improved description clarity with concrete examples of detected issues

### v1.1.0 — 2026-04-30
- Renamed plugin to **EntropyGuard**
- Added **Corrupted Source** detection as a new top-level category
- Improved AI duplication signals (enhanced Tree-sitter fingerprints)
- Improved AI security anti-pattern detection (hardcoded secrets, placeholders)
- Fixed code generator tool — now 100% working

### v1.0.3 — 2026-04-25
- New plugin icon
- Fixed licensing to use IntelliJ Marketplace Trial API correctly
- Fixed code generator compilation issues
- Added support for `aichat`, `llm`, and `ollama` CLI tools

### v1.0.2 — 2026-04-17
- Temporarily disabled MCP server
- Added click-to-jump for problem files
- UI layout improvements

### v1.0.1 — 2026-04-14
- General improvements and bug fixes

### v1.0.0 — 2026-04-12
- Initial IntelliJ Marketplace release
- Real-time code quality analysis for AI-generated code
- Security vulnerability detection
- Code quality insights + suggestions
- Inline editor feedback
- Dedicated tool window

## Support

- **Issues**: [GitHub Issues](https://github.com/adrianmikula/EntropyGuard/issues)
- **Email**: adrian.m@tuta.io
- **License**: Commercial (7-day trial, paid license for continued use)

---

Built on the IntelliJ Platform. Uses JavaParser, Tree-sitter, and SnakeYAML.

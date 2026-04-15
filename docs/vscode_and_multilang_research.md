# VSCode Plugin & Multi-Language Support Research

> **Objective**: Reuse the core scanning engine from the existing IntelliJ plugin and adapt it for VSCode while supporting multiple programming languages beyond Java.

## Current Architecture Analysis

### Existing IntelliJ Plugin Components

| Component | Purpose | Java-Specific? |
|-----------|---------|----------------|
| `ASTAnalyzer` | Extracts metrics from code (complexity, nesting, etc.) | **YES** - uses JavaParser |
| `TreeSitterFuzzyDetector` | Detects duplicate code via shingling | **PARTIAL** - only Java grammar |
| `RuleFactory` | Evaluates rules against AST metrics | No - configurable |
| `AnalysisConfigLoader` | Loads YAML rules | No |
| `Risk Scoring Engine` | Calculates weighted scores | No |
| UI (ToolWindow, InlinePainter) | Displays risk indicators | **YES** - IntelliJ API |
| Capture/Diff Service | Captures code changes | No |

### Key Dependencies

```
// From build.gradle.kts
implementation("com.github.javaparser:javaparser-core:3.25.8")
implementation("io.github.bonede:tree-sitter:0.26.6")
implementation("io.github.bonede:tree-sitter-java:0.23.5")
// IntelliJ SDK (via intellij plugin)
```

### What's Reusable vs Language/IDE-Specific

#### ✅ Can Be Reused
1. **Rule Engine** - Already language-agnostic via YAML config
2. **Risk Scoring Model** - Weighted categories (Complexity 25%, Duplication 20%, etc.)
3. **Analysis Pipeline** - Debounced, cancellable, latest-only execution
4. **Config Models** - ASTMetrics, FuzzyMetrics, Findings data classes
5. **Rule Configuration YAML** - Patterns, conditions, severity levels

#### ❌ Must Change for VSCode
1. **Parser Layer** - Replace JavaParser with language-agnostic approach
2. **UI Layer** - Replace IntelliJ UI APIs with VSCode WebViews
3. **Extension Entry Point** - VSCode extension manifest + TypeScript
4. **Event Handlers** - VSCode workspace/file event APIs
5. **Inline Decorations** - VSCode Diagnostic API

#### ❌ Must Change for Multi-Language
1. **AST Metrics** - Abstract language-specific metrics
2. **TreeSitter Grammars** - Add parsers for Python, TypeScript, Go, Rust, etc.
3. **Config Rules** - Some rules may need language variants
4. **Shingle Normalization** - Adjust for language syntax tokens

---

## Proposed Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────────────┐
│                     CROSS-PLATFORM CORE                          │
│  (Reusable Kotlin/Gradle module - NOT IDE-specific)              │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Rule Engine  │  │ Risk Scoring │  │ Analysis Pipeline    │  │
│  │ (YAML-driven)│  │ (Weighted)   │  │ (Debounced/Async)    │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Language-Agnostic Metrics Model              │   │
│  │  - methodCount, nestingDepth, cyclomaticComplexity        │   │
│  │  - duplicateCount, magicNumberCount, etc.                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  IntelliJ SDK   │  │  VSCode Ext API │  │  CLI (future)   │
│  (Kotlin)       │  │  (TypeScript)   │  │  (Any)          │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ - ToolWindow    │  │ - WebView       │  │ - JSON I/O      │
│ - InlineAnnot   │  │ - Diagnostics   │  │ - Exit codes    │
│ - PSI Listeners │  │ - File Watcher  │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    LANGUAGE PARSERS                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ Java    │ │ Kotlin  │ │Python   │ │TypeScript│ │ Rust   │  │
│  │Parser   │ │TreeSitter│ │TreeSitter│ │TreeSitter│ │TreeSitter│ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                  │
│  Each parser:                                                   │
│  - Normalizes code → LanguageAgnosticMetrics                    │
│  - Extracts source positions for inline highlights               │
└─────────────────────────────────────────────────────────────────┘
```

### Module Structure

```
ai-code-quality-risk/
├── core/                           # Cross-platform core library
│   ├── src/main/kotlin/
│   │   └── com/aicodequalityrisk/core/
│   │       ├── analysis/
│   │       │   ├── RuleEngine.kt
│   │       │   ├── RiskScorer.kt
│   │       │   ├── AnalysisPipeline.kt
│   │       │   └── models/
│   │       │       ├── CodeMetrics.kt       # Language-agnostic
│   │       │       ├── Finding.kt
│   │       │       ├── RiskResult.kt
│   │       │       └── RuleConfig.kt
│   │       └── metrics/
│   │           ├── MetricsExtractor.kt      # Interface
│   │           └── FuzzyDetector.kt
│   └── src/main/resources/
│       └── rules/
│           └── analysis-rules.yaml
│
├── intellij-plugin/                # Existing IntelliJ plugin
│   └── (current codebase)
│
├── vscode-extension/               # New VSCode extension
│   ├── src/
│   │   ├── extension.ts            # Entry point
│   │   ├── ui/
│   │   │   ├── RiskDashboard.ts    # WebView
│   │   │   └── InlineDecorations.ts
│   │   ├── core/                   # Wrapper around core library
│   │   │   └── CoreBridge.ts
│   │   └── language/
│   │       └── TreeSitterAdapter.ts
│   ├── package.json
│   └── tsconfig.json
│
└── parsers/                        # Language-specific parsers
    ├── tree-sitter-java/
    ├── tree-sitter-python/
    ├── tree-sitter-typescript/
    ├── tree-sitter-go/
    └── tree-sitter-rust/
```

---

## Implementation Phases

### Phase 1: Extract Core Engine (Week 1)

**Goal**: Create a clean, reusable core library with no IDE dependencies.

#### Tasks:
- [ ] Create new `core` Gradle module
- [ ] Extract rule engine (`RuleFactory.kt` → `RuleEngine.kt`)
- [ ] Extract risk scorer logic
- [ ] Extract analysis pipeline (`AnalysisOrchestrator.kt`)
- [ ] Create language-agnostic `CodeMetrics` interface
- [ ] Abstract AST metrics to support multiple languages
- [ ] Create JSON CLI interface for testing
- [ ] Write unit tests for core functionality
- [ ] Document the core API

#### Deliverables:
- `core-1.0.0.jar` - Published internal library
- CLI tool for local testing: `java -jar core-cli.jar analyze <file>`

---

### Phase 2: VSCode Extension Foundation (Week 2)

**Goal**: Build the VSCode extension shell with basic functionality.

#### Tasks:
- [ ] Scaffold VSCode extension using `yo code` generator
- [ ] Configure `package.json` with proper metadata
- [ ] Implement WebView-based risk dashboard
- [ ] Set up VSCode file watchers for change detection
- [ ] Integrate core library via npm (convert JAR → npm or use polyglot)
- [ ] Implement basic inline decoration via VSCode Diagnostics
- [ ] Add command palette integration
- [ ] Handle extension lifecycle (activate/deactivate)

#### Deliverables:
- Working VSCode extension with basic UI
- Risk dashboard showing score and findings
- Inline decorations for high-risk code

#### Key VSCode APIs to Use:

```typescript
// File watching
vscode.workspace.onDidChangeTextDocument(event => { ... });
vscode.workspace.onDidSaveTextDocument(doc => { ... });

// WebView for dashboard
const panel = vscode.window.createWebviewPanel(...);
panel.webview.html = generateDashboardHtml(results);

// Inline diagnostics
const diagnosticCollection = vscode.languages.createDiagnosticCollection('risk');
diagnosticCollection.set(uri, diagnostics);
```

---

### Phase 3: Multi-Language Parser Abstraction (Week 3)

**Goal**: Support parsing for multiple programming languages.

#### Tasks:
- [ ] Define `LanguageParser` interface
- [ ] Implement TreeSitter adapter for VSCode
- [ ] Add language support:
  - [ ] Python (tree-sitter-python)
  - [ ] TypeScript/JavaScript (tree-sitter-typescript)
  - [ ] Go (tree-sitter-go)
  - [ ] Rust (tree-sitter-rust)
  - [ ] Kotlin (via tree-sitter-kotlin)
- [ ] Normalize all metrics to common `CodeMetrics` model
- [ ] Implement language detection based on file extension
- [ ] Create per-language rule configurations

#### Unified Metrics Interface:

```typescript
interface CodeMetrics {
  // Complexity
  methodCount: number;
  maxMethodLength: number;
  maxNestingDepth: number;
  cyclomaticComplexity: number;
  
  // Duplication
  duplicateBlockCount: number;
  duplicateMethodPairs: MethodSimilarityPair[];
  
  // Code Quality
  magicNumberCount: number;
  longParameterCount: number;
  broadCatchCount: number;
  
  // Language-specific (nullable)
  languageSpecificMetrics?: Record<string, any>;
}
```

---

### Phase 4: Deep Integration (Week 4)

**Goal**: Advanced features and polish.

#### Tasks:
- [ ] Cross-reference duplicates across files/languages
- [ ] Language-aware rule patterns (some rules are language-specific)
- [ ] Enhanced fuzzy matching with larger context windows
- [ ] Performance optimization:
  - [ ] Web Workers for heavy analysis
  - [ ] Incremental parsing for edits
  - [ ] Caching of parsed ASTs
- [ ] Configuration UI in VSCode settings
- [ ] Exportable analysis reports
- [ ] User feedback integration

---

## Technical Decisions

### Decision Matrix

| Decision | Option A | Option B (Recommended) | Trade-offs |
|----------|----------|------------------------|------------|
| **Core Language** | Keep Kotlin | Port to TypeScript | Keep Kotlin = need Kotlin→JS interop; Port = more work but cleaner VSCode integration |
| **Multi-Language Parsing** | Multiple JavaParser ports | **TreeSitter** | TreeSitter has 40+ language grammars, faster, better maintained |
| **Shared Metrics** | Keep ASTMetrics | **Unified CodeMetrics** | Unified interface enables clean multi-language support |
| **Plugin Communication** | HTTP to backend | **In-process with WebAssembly** | WASM = faster, offline; HTTP = simpler, scalable to cloud |
| **VSCode <-> Core** | Native messaging | **npm package wrapper** | npm = easier dependency mgmt |

### Recommended Stack for VSCode Extension

```
VSCode Extension (TypeScript)
├── @ai-code-quality/core      # Core library (TS port or WASM)
├── @tree-sitter/node          # TreeSitter bindings
├── tree-sitter-{lang}         # Language grammars
└── vscode                     # VSCode APIs
```

### Alternative: Kotlin Core + WASM

If keeping Kotlin core is preferred:

```
Kotlin Core (Gradle)
├── Compile to WASM via Kotlin/WASM
└── Publish to npm as @ai-code-quality/core-wasm

VSCode Extension (TypeScript)
├── @ai-code-quality/core-wasm  # WASM module
├── WASI bindings
└── TypeScript wrappers
```

---

## Language Support Matrix

| Language | Parser | Priority | Complexity | Notes |
|----------|--------|----------|------------|-------|
| Java | JavaParser + TreeSitter | P0 | Low | Already supported |
| Kotlin | TreeSitter | P0 | Medium | Similar to Java |
| Python | TreeSitter | P1 | Medium | Whitespace-sensitive |
| TypeScript | TreeSitter | P1 | Medium | JSX/TSX support needed |
| JavaScript | TreeSitter | P1 | Medium | Subset of TypeScript |
| Go | TreeSitter | P2 | Low | Simple grammar |
| Rust | TreeSitter | P2 | Medium | Complex ownership syntax |
| C# | TreeSitter | P3 | High | Large grammar |
| C/C++ | TreeSitter | P3 | High | Preprocessor complexity |

---

## Open Questions

### Needs User Input:

1. **Core Implementation Language**
   - Keep Kotlin (require Kotlin→JS interop) OR
   - Port to TypeScript (simpler VSCode integration)

2. **Deployment Strategy**
   - Option A: VSCode extension bundled with analysis engine
   - Option B: VSCode as thin UI + local HTTP server running Kotlin analysis
   - Option C: Compile Kotlin core to WebAssembly for in-process execution

3. **Priority Languages**
   - Which 2-3 languages are most important for initial release?

4. **Offline vs Cloud**
   - Should the VSCode plugin work fully offline?
   - Is cloud enhancement (AI explanations, etc.) acceptable?

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| TreeSitter bindings instability | Medium | High | Pin versions, test against grammar updates |
| Multi-language metric normalization | High | Medium | Design flexible metric model from start |
| Performance with large files | Medium | Medium | Web Workers, incremental parsing |
| WASM compilation complexity | Medium | Medium | Start with simpler JS port, evaluate WASM later |

---

## Next Steps

1. **Confirm technical decisions** (core language, deployment strategy)
2. **Select priority languages** for initial support
3. **Create detailed implementation tasks** in project tracker
4. **Set up CI/CD** for multi-module build

---

## References

- [VSCode Extension API Documentation](https://code.visualstudio.com/api)
- [TreeSitter](https://tree-sitter.github.io/tree-sitter/)
- [TreeSitter Grammars List](https://tree-sitter.github.io/tree-sitter/)
- [WebView Best Practices](https://code.visualstudio.com/api/extension-guides/webview)
- [WASM for Kotlin](https://kotl.in/wasm)

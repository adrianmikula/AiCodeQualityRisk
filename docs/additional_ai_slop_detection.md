## Final Implementation Plan: AI Code Slop Pattern Detection Tests

Based on my research, I'll create a comprehensive test file `AiCodeSlopPatternsTest.kt` with **100 tests across 10 categories** that detect AI-generated code patterns.

---

### Project: `AiCodeSlopPatternsTest.kt`

**Location**: `src/test/kotlin/com/aicodequalityrisk/plugin/analysis/AiCodeSlopPatternsTest.kt`

---

### The 10 Categories (100 Tests)

| # | Category | What it Detects | Example Patterns |
|---|----------|------------------|-------------------|
| 1 | **Boilerplate Bloat** | Excessive/verbose boilerplate code | 20+ getters manually, redundant null checks, over-abstracted classes |
| 2 | **Verbose Comment Spam** | Unnecessary/NOISY comments | Line-by-line obvious comments, TODO spam, commented-out code |
| 3 | **Over-Defensive Programming** | Excessive null checks, try-catch | 5-level null nesting, empty catch blocks, catching Throwable |
| 4 | **Magic Numbers & Hardcoded Values** | Unnamed constants, embedded secrets | Inline API URLs, port numbers, status code literals |
| 5 | **Complex Boolean Logic** | Hard-to-read conditionals | Nested ternaries, 10+ if-else chains, negated negations |
| 6 | **Deep Nesting** | Excessive control flow depth | 6+ if levels, callback hell, nested loops with early returns |
| 7 | **Verbose Logging** | Excessive/noisy logging | System.out in loops, logging sensitive data, no level filtering |
| 8 | **Poor Naming** | Generic/non-descriptive names | `data`, `temp`, `result` variables, `Manager`/`Handler` classes |
| 9 | **Framework/API Misuse** | Incorrect API usage patterns | Manual SQL concatenation, == for String, deprecated APIs |
| 10 | **Excessive Documentation** | Over-documented trivial code | 200-line Javadoc on 20-line method, outdated comments |

---

### Implementation Strategy

1. **Leverage existing ASTAnalyzer** - Uses JavaParser for AST analysis
2. **Add 10 tests per category** - Each test verifies detection of one pattern
3. **Use ASTMetrics fields** - Existing fields already detect many patterns
4. **May need new metrics** - Some patterns require new ASTMetrics fields

### Existing ASTMetrics that map to patterns:

| Metric | Detects |
|--------|---------|
| `duplicateStringLiteralCount` | Repeated strings, magic strings |
| `magicNumberCount` | Bare numeric literals |
| `hardcodedConfigLiteralCount` | Embedded URLs, secrets |
| `maxNestingDepth` | Deeply nested code |
| `booleanOperatorCount` | Complex boolean expressions |
| `maxElseIfChainLength` | Long if-else chains |
| `broadCatchCount` | Catching Exception/Throwable |
| `emptyCatchCount` | Silent exception swallowing |
| `duplicateMethodCallCount` | Repeated method patterns |
| `maxParameterCount` | Long parameter lists |

---

### Implementation Tasks

1. **Create** `AiCodeSlopPatternsTest.kt` with 10 inner test classes
2. **Category 1-5 tests** (50 tests) - Leverages existing ASTMetrics
3. **Category 6-10 tests** (50 tests) - May require extending detection
4. **Run tests** with `./gradlew test` to verify detection

---

**Ready to implement?** Please **toggle to Act mode** to start creating the 100 test cases.
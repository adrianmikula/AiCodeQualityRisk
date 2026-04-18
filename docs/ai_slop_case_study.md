Good—this is the right place to be pragmatic. You already have the hard part (detection). Now you just need a **repeatable input generator**.

Let’s design this so you can actually run it in a day, not a week.

---

# 1. The goal (keep this tight)

In 48 hours, you want:

* ~15–30 generated projects
* With controlled variation
* Run through your detector
* Produce **one strong, defensible stat about duplication**

Everything else is optional.

---

# 2. High-level workflow

```text
[Config]
   ↓
[Generate Prompt]
   ↓
[Call LLM API]
   ↓
[Write Project to Disk]
   ↓
[Apply Iteration Prompts (optional)]
   ↓
[Run Detection Engine]
   ↓
[Store Results (CSV is fine)]
```

---

# 3. Step-by-step execution plan

## Step 1 — Define your experiment matrix (1–2 hours)

Start small:

```text
Prompts (3):
- CRUD app
- Microservice (Spring Boot)
- “Clean architecture” app

Modes (2):
- single-shot
- iterative (add features)

Models (1–2):
- pick whatever you have easiest access to
```

Total runs:

```
3 prompts × 2 modes × ~3 variations = ~18 projects
```

Perfect.

---

## Step 2 — Define prompt templates (critical)

Don’t hardcode prompts—parameterise them.

### Base template

```text
You are generating a Java Spring Boot project.

Requirements:
- Domain: {{domain}}
- Architecture: {{architecture}}
- Features: {{features}}
- Constraints:
  - Write complete code
  - Use realistic structure
  - Do not explain, only output files

Output format:
<file path="...">
...code...
</file>
```

---

### Iteration prompt

```text
Modify the existing project:

- Add feature: {{feature}}
- Maintain consistency with existing code
- Avoid rewriting unchanged files

Output only changed or new files.
```

👉 This is where duplication creeps in.

---

# 4. Kotlin script design (simple but extensible)

Keep it CLI-based. No UI.

---

## Core data models

```kotlin
data class ExperimentConfig(
    val name: String,
    val promptTemplates: List<PromptTemplate>,
    val modes: List<GenerationMode>,
    val variationsPerPrompt: Int
)

data class PromptTemplate(
    val name: String,
    val domain: String,
    val architecture: String,
    val features: List<String>
)

enum class GenerationMode {
    SINGLE_SHOT,
    ITERATIVE
}
```

---

## Project descriptor

```kotlin
data class GeneratedProject(
    val id: String,
    val promptName: String,
    val mode: GenerationMode,
    val variation: Int,
    val path: Path
)
```

---

# 5. Core components

## 1. LLM Client (thin wrapper)

```kotlin
class LlmClient(private val apiKey: String) {

    fun generate(prompt: String): String {
        // call API (OpenAI, etc)
        // return raw response
    }
}
```

---

## 2. Prompt Builder

```kotlin
class PromptBuilder {

    fun buildBasePrompt(template: PromptTemplate): String {
        return """
            You are generating a Java Spring Boot project.

            Domain: ${template.domain}
            Architecture: ${template.architecture}
            Features: ${template.features.joinToString(", ")}

            Output format:
            <file path="...">
            ...
            </file>
        """.trimIndent()
    }

    fun buildIterationPrompt(feature: String): String {
        return """
            Modify the project:

            Add feature: $feature

            Output only changed files.
        """.trimIndent()
    }
}
```

---

## 3. Response Parser (important)

You need to convert LLM output → files.

```kotlin
class FileExtractor {

    fun extractFiles(response: String): List<Pair<String, String>> {
        val regex = Regex("""<file path="(.*?)">(.*?)</file>""", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(response).map {
            val path = it.groupValues[1]
            val content = it.groupValues[2]
            path to content
        }.toList()
    }
}
```

---

## 4. Project Writer

```kotlin
class ProjectWriter {

    fun writeProject(basePath: Path, files: List<Pair<String, String>>) {
        files.forEach { (path, content) ->
            val filePath = basePath.resolve(path)
            Files.createDirectories(filePath.parent)
            Files.writeString(filePath, content)
        }
    }
}
```

---

## 5. Orchestrator (main engine)

```kotlin
class ExperimentRunner(
    private val llm: LlmClient,
    private val promptBuilder: PromptBuilder,
    private val extractor: FileExtractor,
    private val writer: ProjectWriter
) {

    fun run(config: ExperimentConfig) {
        config.promptTemplates.forEach { template ->
            config.modes.forEach { mode ->
                repeat(config.variationsPerPrompt) { variation ->

                    val projectId = UUID.randomUUID().toString()
                    val projectPath = Paths.get("generated/$projectId")

                    val basePrompt = promptBuilder.buildBasePrompt(template)
                    val baseResponse = llm.generate(basePrompt)

                    val baseFiles = extractor.extractFiles(baseResponse)
                    writer.writeProject(projectPath, baseFiles)

                    if (mode == GenerationMode.ITERATIVE) {
                        val iterationPrompt = promptBuilder.buildIterationPrompt("Add caching")
                        val iterResponse = llm.generate(iterationPrompt)
                        val iterFiles = extractor.extractFiles(iterResponse)
                        writer.writeProject(projectPath, iterFiles)
                    }

                    // Hook your detection engine here
                    runDetection(projectPath, projectId)
                }
            }
        }
    }

    private fun runDetection(path: Path, projectId: String) {
        // call your existing engine
        // output results to CSV
    }
}
```

---

# 6. Output format (keep it simple)

Start with CSV:

```text
project_id,mode,prompt,duplicate_pairs,functions,total_loc
```

You can import into Supabase later.

---

# 7. Key design decisions (these matter)

## 1. File-based output format

* Avoids JSON parsing headaches
* Works well with LLMs

---

## 2. Iterative mode = separate API calls

* Don’t try to maintain conversation state initially
* Just feed back context if needed later

---

## 3. No database (yet)

* CSV + folders is enough for 48h

---

# 8. Avoid these traps

❌ Trying to generate “perfect” projects
❌ Over-complicated prompt engineering
❌ Too many variations
❌ Building a UI

---

# 9. What success looks like

After running this:

You should be able to say something like:

> “Across 18 AI-generated Spring Boot projects, duplication density increased by ~2.3x after iterative changes.”

That’s your post.

---

# 10. If you want to push it one level further

Add:

### Prompt variation knob

```kotlin
val styles = listOf(
    "simple and minimal",
    "enterprise-grade",
    "highly modular"
)
```

This creates *natural diversity* → reduces brittleness in your findings.

---

# 11. Next step (if you want)

I can:

* Help you plug in a **real OpenAI/Claude Kotlin client**
* Design a **duplication scoring formula that’s defensible**
* Or refine your **first dataset → LinkedIn post for maximum impact**

Right now, don’t overthink it—get the first dataset. That’s where the real insights start.















Plan file: /media/adrian/SHARED/Source/AiCodeQualityRisk/.kilo/plans/1776428421520-misty-nebula.md
Read this file first and treat it as the source of truth for implementation.

Implement the following plan:

Plan updated to use **OpenCode's built-in FREE Minimax M2.5** - no API key needed!

**Final approach**:
```bash
opencode run "prompt" --model opencode/minimax
```

This model:
- Is totally FREE (built into OpenCode)
- Scores 80% on SWE-Bench
- Requires NO API key configuration

## Handover from Planning Session

## Discoveries

- **Existing detection engine**: Project already has `ASTAnalyzer` (JavaParser-based) and `TreeSitterFuzzyDetector` that detect duplicates - the generator should integrate with these, not reimplement
- **OpenCode model syntax**: Use `--model opencode/minimax` (not `openrouter/minimax`) for the built-in free model
- **OpenCode headless mode**: Command is `opencode run "prompt"` which outputs to stdout - can be parsed via ProcessBuilder
- **Output format**: LLM should output `<file path="...">...</file>` format per the case study - needs parser for this
- **No HTTP client needed**: Using OpenCode CLI removes okhttp dependency requirement from original plan

## Relevant Files

- `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/ASTAnalyzer.kt` - AST metrics extraction (reuse for detection)
- `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/TreeSitterFuzzyDetector.kt` - Fuzzy duplicate detection (reuse)
- `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/ASTMetrics.kt` - Data model for metrics
- `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/FuzzyMetrics.kt` - Duplicate method pair data
- `docs/ai_slop_case_study.md` - Design reference for prompt templates and iteration workflow

## Implementation Notes

- OpenCode's free model may have rate limits - implement retry logic or fallback
- The `opencode run` command may need `--yes` flag for non-interactive execution
- Consider using a temp directory for generated projects, then run detection on each
- CSV output should include: project_id, mode, prompt_name, variation, duplicate_string_literals, duplicate_number_literals, duplicate_method_calls, duplicate_method_count, max_similarity_score, total_loc

## Todo List

- [x] Review existing codebase and understand existing detection engine
- [~] Create plan for code generation engine implementation
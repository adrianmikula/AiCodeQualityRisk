Alright—let’s turn everything into a **coherent, buildable detection model** rather than a loose set of ideas.

The goal isn’t perfection—it’s:

> **High-confidence, explainable signals that compound into a reliable drift score**

---

# 🧭 1. Core Model: “Intent vs Reality” Scoring

You’re detecting **misalignment across layers**:

* Intent sources → spec, tests, docs, API contracts
* Reality → code behavior

Each signal contributes to a **Feature Drift Score (0–100)**

---

# 🧱 2. Signal Categories (With Weights & Implementation)

## 🔴 A. Spec / Plan → Code Alignment (Weight: 25)

### Signals

* Spec feature not present in code
* Feature partially implemented
* Feature mentioned but no structural evidence

### Implementation

* Parse `.md` files into feature tokens:

  * keyword extraction (pagination, retry, auth, etc.)
  * optional: simple bullet-point parser
* Map keywords → expected code patterns:

  * pagination → `Pageable`, `limit`, `offset`
  * retry → loops, retry libs
* Scan codebase for presence

### Output

* ✅ Implemented
* ⚠️ Partial
* ❌ Missing

---

## 🔴 B. Tests ↔ Code Alignment (Weight: 25)

### Signals

* Test exists, feature missing in code
* Test name implies behavior not implemented
* Test scaffold without assertions
* Feature exists but no tests

### Implementation

* Parse test names (`shouldX`, `testX`)
* Extract keywords from test names
* Detect:

  * assertions present?
  * feature patterns in tested methods?
* Optional:

  * map test → target method via naming

---

## 🔴 C. API Contract Truthfulness (Weight: 20)

### Signals

* Unused parameters (pagination, filters)
* Path variables unused
* HTTP method semantic violations
* Endpoint name vs behavior mismatch

### Implementation

* Parse annotations:

  * `@GetMapping`, `@PostMapping`, etc.
* Extract:

  * route
  * params
* Analyze method body:

  * variable usage
  * DB writes (`save`, `delete`)
* Compare:

  * inputs vs usage
  * method type vs side effects

---

## 🟠 D. Documentation ↔ Code Drift (Weight: 15)

### Signals

* Javadoc claims features not implemented
* Swagger/OpenAPI describes behavior not present
* Docs updated but code not aligned
* “Documentation inflation” (AI hallucinated features)

### Implementation

* Extract doc comments + Swagger annotations
* Keyword extraction
* Match against code patterns
* Optional:

  * hash doc vs method body across scans → detect staleness

---

## 🟠 E. Parameter / Input Utilization (Weight: 10)

*(Pulled out separately because it’s such a strong early signal)*

### Signals

* Method parameters unused
* Config flags ignored
* DTO fields never read

### Implementation

* AST variable usage analysis
* Track:

  * declared params
  * references in method body

---

## 🟠 F. Naming ↔ Implementation Mismatch (Weight: 10)

### Signals

* Method name implies behavior not present

  * `retry`, `paginate`, `secure`, `cache`
* Endpoint names misleading

### Implementation

* Tokenize method names
* Map keywords → expected patterns
* Check presence in method body

---

## 🟡 G. Structured Contract Compliance (TypeSpec/OpenAPI) (Weight: 15)

*(Optional but very strong when available)*

### Signals

* Missing parameters from spec
* Response shape mismatch
* Contract fields ignored

### Implementation

* Parse OpenAPI / TypeSpec
* Compare:

  * endpoints
  * params
  * schemas
* Cross-check with code

---

## 🟡 H. Comment / TODO Drift (Weight: 10)

### Signals

* TODO mentions feature not implemented
* Comments describe future work that never happened
* “We will implement…” patterns

### Implementation

* Scan comments for:

  * TODO, FIXME
  * future-tense phrases
* Extract keywords
* Check code for matching implementation

---

## 🟡 I. Over-Generation / Phantom Features (Weight: 10)

### Signals

* Code implements features not in spec/tests/docs
* Unexpected complexity

### Implementation

* Compare:

  * code keywords vs spec/test keywords
* Detect extra capabilities

---

## 🟡 J. Scaffolding Without Substance (Weight: 10)

### Signals

* Methods with comments but minimal logic
* Placeholder implementations

### Implementation

* Heuristics:

  * comment density vs code density
  * low statement count
  * high TODO ratio

---

# ⚖️ 3. Weight Summary

| Category             | Weight |
| -------------------- | ------ |
| Spec → Code          | 25     |
| Tests ↔ Code         | 25     |
| API Contract         | 20     |
| Docs ↔ Code          | 15     |
| Structured Contracts | 15     |
| Param Utilization    | 10     |
| Naming Mismatch      | 10     |
| TODO Drift           | 10     |
| Phantom Features     | 10     |
| Scaffolding          | 10     |

---

# 🧠 4. Scoring Strategy

## Per Feature:

Each feature gets a score:

```text
Feature Drift Score = Σ(weighted signals)
```

---

## Normalisation:

Clamp to 0–100:

* 0–20 → ✅ Healthy
* 21–50 → ⚠️ Partial drift
* 51–80 → 🔴 Significant drift
* 81–100 → 💀 Severe misalignment

---

# 🧩 5. Feature Extraction (Critical Foundation)

You need a **shared feature vocabulary**

---

## Simple approach (works well):

### Feature = keyword cluster

```json
{
  "pagination": ["page", "size", "limit", "offset"],
  "retry": ["retry", "attempt", "backoff"],
  "auth": ["auth", "token", "jwt", "password"],
  "filtering": ["filter", "criteria", "where"]
}
```

---

Map all signals to these features.

---

# 🚀 6. Output Format (What Makes This Valuable)

Don’t just give scores—give **explanations**:

---

## Example:

> **Feature: Pagination → 🔴 Drift (78/100)**
>
> * Spec: ✅ defined
> * Tests: ❌ missing
> * API: ⚠️ parameters declared but unused
> * Docs: ✅ documented
> * Code: ❌ not implemented
>
> Issues:
>
> * `page`, `size` parameters unused in `getUsers`
> * No pagination logic detected
> * Swagger claims pagination support

---

This is what makes devs trust the tool.

---

# 🔥 7. Implementation Order (Practical)

## Phase 1 (Immediate impact)

* Param usage detection
* TODO/comment drift
* Naming mismatch
* basic spec keyword matching

## Phase 2

* API contract checks
* test name parsing
* doc vs code comparison

## Phase 3

* OpenAPI/TypeSpec parsing
* feature graph + scoring
* temporal drift tracking

---

# 💡 Final Insight

You’re not building:

> “A tool that checks if code is correct”

You’re building:

> **“A system that detects when multiple sources of truth stop agreeing”**

That’s a fundamentally different—and much more valuable—category.

---

If you want next step, I can:

* help you design the **actual AST queries / Kotlin implementation patterns**
* or define a **clean internal data model (Feature, Signal, Evidence)** so this doesn’t turn into spaghetti as it grows

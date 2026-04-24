Below is a **48-hour MVP plan designed for speed, publishability, and early monetisation**, not perfection. The goal is:

> Get a working IntelliJ plugin into JetBrains Marketplace that *demonstrates value in under 10 seconds per use*.

---

# 🚀 MVP V1: “AI Code Risk Radar (Lite)”

## 🎯 Core MVP promise

> “Get an AI-generated risk score + top issues for your current code changes before you commit.”

That’s it.

No Kafka. No deep static analysis. No enterprise complexity.

---

# 🧱 1. MVP Scope (STRICT CUT)

## ✅ MUST HAVE (v1)

### IntelliJ Plugin

* Detect:

  * current file changes OR Git diff (staged preferred)
* Send diff to backend
* Show:

  * Risk score (0–100)
  * 3–7 issues
  * 1–2 AI explanations
* Tool window panel
* Basic inline warning badge (optional but powerful)

---

### Backend (simple REST API)

* Accept diff text
* Run lightweight heuristics
* Call LLM (optional but strongly recommended)
* Return JSON result

---

## ❌ DO NOT BUILD (yet)

* AST parsing
* duplication engine
* test coverage integration
* CI/CD integrations
* MCP full tool suite
* multi-language support beyond Java (initially Java-only is fine)

---

# 🏗️ 2. Architecture (MVP)

```text id="mvp_arch"
[IntelliJ Plugin]
   ↓
POST /analyze-diff
   ↓
[Backend API (Fast/Java/Node)]
   ↓
Heuristics + LLM call
   ↓
Risk JSON
   ↓
[IntelliJ UI]
```

---

# 🧠 3. Risk Engine (MVP Simplified)

You only need **5 signals** for v1.

## 📊 Score model (simple weighted heuristic)

```text id="mvp_score"
Risk Score (0–100):

Complexity        +30
Duplication hints +20
Test gaps         +20
Security hints    +20
AI-style noise    +10
```

---

## ⚙️ Heuristics (MVP version)

### 1. Complexity (cheap heuristics)

* long methods (>40 lines)
* too many if/else blocks
* nested loops

```text id="h1"
+10 if method > 40 lines
+10 if nesting depth > 3
+10 if > 10 branching statements
```

---

### 2. Duplication (very simple MVP)

No AST needed.

Just:

* repeated lines similarity (basic hashing or string similarity)
* repeated blocks in diff

```text id="h2"
+15 if repeated 3+ lines detected in diff
```

---

### 3. Test gap detection

Heuristic:

* file modified in `/service/` or `/core/`
* no corresponding `/test/` change detected

```text id="h3"
+20 if service code changed but no test diff present
```

---

### 4. Security (pattern-based only)

Search for:

* SQL strings
* raw queries
* unsafe deserialization keywords

```text id="h4"
+20 if "SELECT *" or string concatenated SQL found
+20 if ObjectInputStream used
```

---

### 5. AI-generated code signals (very lightweight)

* overly verbose functions
* repetitive patterns
* long explanatory comments

```text id="h5"
+10 if comment density > threshold
+10 if repeated boilerplate patterns
```

---

# 🤖 4. LLM Layer (IMPORTANT FOR VALUE)

You don’t need heavy reasoning—just:

## Prompt:

> “Summarise risks in this code diff. Be concise. List top 5 issues and suggest fixes.”

Return:

* explanation text
* bullet issues
* fix suggestions

👉 This is what makes it feel “AI-powered” even if heuristics are basic.

---

# 📦 5. Backend API (MVP design)

## Endpoint

```
POST /analyze-diff
```

---

## Request

```json id="req"
{
  "repo": "project-name",
  "filePath": "src/Service.java",
  "diff": "git diff content here"
}
```

---

## Response

```json id="res"
{
  "riskScore": 72,
  "level": "HIGH",
  "issues": [
    {
      "type": "COMPLEXITY",
      "message": "Method processOrder is too large (52 lines)",
      "severity": "HIGH"
    },
    {
      "type": "TEST_GAP",
      "message": "Service modified without corresponding test updates",
      "severity": "MEDIUM"
    }
  ],
  "summary": "This change introduces complexity and lacks test coverage.",
  "suggestions": [
    "Split processOrder into smaller methods",
    "Add unit tests for retry logic"
  ]
}
```

---

# 🧩 6. IntelliJ Plugin UI (MVP)

Keep it extremely simple.

---

## 🪟 Tool Window Layout

```text id="ui1"
AI Code Risk Radar
--------------------------------

Overall Risk: 72 (HIGH)

Top Issues:
⚠ Method too large (52 lines)
⚠ Missing test coverage
⚠ Possible duplicated logic

Summary:
"This change increases complexity and reduces test safety."

[Explain]  [Re-analyze]
```

---

## 🎯 Interaction model

* Click tool window → auto-analyze current file
* Button: “Re-analyze diff”
* Optional: auto-run on save

---

## ⚠️ Inline UI (optional if time allows)

* gutter icon:

  * 🟡 40–60
  * 🔴 60+

---

# ⏱️ 7. 48-Hour Execution Plan

This is the most important part.

---

## 🕐 Day 1 (0–24h): “Working pipeline”

### Hours 0–4

* Setup IntelliJ plugin project (Gradle Kotlin)
* Create tool window UI
* Basic HTTP client

---

### Hours 4–10

* Implement diff capture:

  * Git diff OR document change listener
* Send diff to backend

---

### Hours 10–16

* Build backend API:

  * FastAPI (Python) OR Node.js Express
* Implement heuristic scoring

---

### Hours 16–20

* Add LLM call (OpenAI API or similar)
* Format response JSON

---

### Hours 20–24

* Wire plugin → backend → UI display

👉 End of Day 1 goal:

> Working “paste diff → get risk score in IDE”

---

## 🕑 Day 2 (24–48h): “Polish + publish readiness”

### Hours 24–30

* Improve UI (tool window clarity)
* Add severity color coding
* Improve response formatting

---

### Hours 30–36

* Add caching (avoid repeat API calls)
* Improve diff extraction reliability

---

### Hours 36–42

* Add simple onboarding screen
* Add settings panel (API key, endpoint)

---

### Hours 42–48

* Package plugin
* Write JetBrains Marketplace listing
* Record demo GIF

---

# 💰 8. Monetisation Strategy (FASTEST PATH)

You want **low friction adoption first**, then conversion.

---

## 🧪 Pricing model (simple and effective)

### Free tier

* 7-day trial (no signup friction OR email optional)
* unlimited local analysis OR capped daily requests

---

### Paid tier ($9–19/month individual devs)

* unlimited analyses
* history tracking
* better AI explanations
* priority processing

---

### Team tier (later)

* per seat pricing
* shared dashboards
* PR integration

---

## ⚡ Key monetisation insight

Do NOT gate core value immediately.

Instead gate:

* history
* advanced explanations
* batch analysis
* repo-wide insights

---

# 🧲 9. Growth strategy (critical for uptake)

## Where installs come from:

* JetBrains Marketplace search (“code quality”, “AI review”)
* Reddit (r/java, r/programming)
* Hacker News launch
* “AI code review tool” keyword SEO

---

## Hook that sells it:

> “See what your AI-generated code is doing to your codebase before you commit it.”

---

# 🧠 10. What makes this actually work

This succeeds if:

### ✔ It gives insight in <3 seconds

### ✔ It triggers on real workflow (pre-commit / save)

### ✔ It feels “obviously useful” without setup

### ✔ It doesn’t require configuration hell

---

# ⚠️ Honest risk check

* You are competing indirectly with SonarLint
* Your edge is **AI explanation + simplicity**
* If LLM quality is bad → product dies immediately

---

# 🏁 Bottom line

This MVP is viable because:

* small scope
* existing tooling reuse
* strong IDE integration point
* clear pain (AI-generated code quality drift)

---

If you want next step, I can:

1. Generate **real IntelliJ plugin Kotlin starter code**
2. Write the **backend FastAPI/Node implementation**
3. Create the **LLM prompt system (this is critical for quality)**
4. Design the **JetBrains Marketplace listing that maximises installs**

This is one of those ideas where execution speed matters more than sophistication—48 hours is realistic if you stay strict on scope.

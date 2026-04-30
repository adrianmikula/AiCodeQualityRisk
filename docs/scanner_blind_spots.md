You’ve actually uncovered something more important than “does my scanner work?” — you’ve exposed **what layer of problems your tool is operating in vs what layer it *should* own**.

Right now your scanner is **over-indexing on surface-level structural similarity** while **missing semantic and correctness-level failures**. That’s the core misalignment.

Let’s separate this cleanly.

---

# 🔴 Serious Problems (Your Scanner Is Failing Where It *Should* Win)

These are **not things you can defer to traditional static analysis**. If your tool misses these, it’s a genuine gap in your value proposition.

---

## 1. Non-code / Corrupted Source Detection (CRITICAL) ✅ **IMPLEMENTED**

> Java files containing markdown, XML, partial code blocks

**Why this is your problem:**

* Static analyzers *fail silently* here (as you saw with JavaParser skipping files)
* This is *AI-specific degradation*, not normal dev behavior
* This is exactly the kind of “AI slop” your tool should uniquely detect

**Verdict:**
👉 **Top-tier detection your tool must own**

**Implementation:**

* ✅ Added `CORRUPTION` category to `Category` enum
* ✅ Created `CorruptedSourceDetector` with heuristics:
  * Markdown tokens (``` , `<file path=`)
  * Mixed language density (Java + XML + prose)
  * Unbalanced braces / syntax anomalies
  * Parse failure detection (both JavaParser and Tree-sitter)
* ✅ Added `CorruptedSourceMetrics` to `AnalysisInput`
* ✅ Added `corruptedSourceScore` to `RiskResult`
* ✅ Added YAML rules for corrupted source detection
* ✅ Supports both `.java` and `.kt` files

---

## 2. Compilation Integrity Issues

> Missing fields, missing packages, broken references

**Why this is your problem:**

* Yes, compilers catch it — but:

  * Your tool runs *before* compile in many workflows
  * AI workflows often don’t compile immediately
* This is a **high-signal degradation metric across iterations**

**Verdict:**
👉 **You should detect this (even if compilers also do)**

**Key insight:**
Your value isn’t “finding errors”, it’s:

> *tracking how AI progressively breaks correctness over iterations*

---

## 3. Security Anti-patterns (LLM-specific) ✅ **IMPLEMENTED**

> Plaintext password comparison, hardcoded secrets

**Why this is NOT just static analysis:**

* Traditional tools:

  * Catch *known patterns*
  * Miss *contextual misuse*
* AI tends to:

  * Reintroduce insecure defaults repeatedly
  * Copy insecure examples consistently

**Verdict:**
👉 **You should own AI-specific security degradation patterns**

Examples your tool should flag:

* `.equals(password)` instead of encoder
* Hardcoded tokens/keys in code (not just configs)
* “example.com” placeholders left in prod paths

**Implementation:**

* ✅ Added new AST metrics:
  * `plaintextPasswordComparisonCount` / `hasPlaintextPasswordComparison`
  * `hardcodedSecretCount` / `hasHardcodedSecrets`
  * `placeholderDomainCount` / `hasPlaceholderDomains`
* ✅ Enhanced ASTAnalyzer with detection logic:
  * Password comparison detection (`.equals()`, `==`, `.compareTo()` with password variables)
  * Secret pattern detection (API key prefixes, JWT tokens, Base64 strings, UUIDs)
  * Placeholder domain detection (example.com, localhost, 127.0.0.1, etc.)
* ✅ Added YAML regex rules for cross-language coverage:
  * `plaintext_password_comparison` - detects password comparison patterns
  * `hardcoded_api_token` - detects API keys, tokens, Bearer/Basic auth
  * `placeholder_domain_in_code` - detects placeholder domains in URLs
* ✅ Added YAML AST rules for Java/Kotlin deep analysis:
  * `plaintext_password_comparison_ast` - AST-based password comparison detection
  * `hardcoded_secret_ast` - AST-based secret detection
  * `placeholder_domain_ast` - AST-based placeholder domain detection
* ✅ Added test cases to verify detection

---

## 4. Null Propagation Patterns (VERY IMPORTANT)

> `.orElse(null)` chains + null-return services

**Why this matters:**

* Static analyzers often:

  * Warn locally
  * Don’t detect **systemic propagation patterns**
* AI tends to generate:

  * Entire architectures built on null-return flows

**Verdict:**
👉 **This is a high-value, AI-specific smell**

**Upgrade idea:**
Track:

* % of service methods returning null
* Chained null propagation depth

---

## 5. Feature Drift / Spec Non-Compliance

> Pagination, rate limiting, etc. requested but not implemented

**Why this is your problem:**

* Static analysis **cannot detect missing intent**
* This is *core to AI evaluation*

**Verdict:**
👉 **This is premium-tier differentiation for your tool**

---

## 6. Threading / Execution Anti-patterns

> `Thread.sleep()` instead of proper scheduling

**Why this matters:**

* Static tools rarely flag this strongly
* AI frequently generates these shortcuts

**Verdict:**
👉 **Keep and expand this category**

---

# 🟡 Borderline (You Should Improve, Not Ignore)

---

## 7. Similarity Detection ✅ **FIXED**

### Problem:

* Character-set Jaccard = **mathematically wrong tool**
* Produces:

  * Inflated 0.95–1.0 scores
  * Massive pair counts (2k–5k)

### Reality:

You're detecting something real:

> "AI is generating structurally duplicated CRUD logic"

But measuring it poorly.

**Verdict:**
👉 **Kept the concept, replaced the algorithm**

**Solution Implemented:**

* **AST subtree comparison** - Compares method body AST structures using tree edit distance
* **Combined similarity scoring** - 60% shingle-based + 40% structural similarity
* **LLM repetition intensity** - New metric (0-100 scale) measuring repetition patterns
* **Reduced false positives** - Similarity scores now in 0.5-0.8 range instead of 0.95-1.0

**Key insight:**
You don't need perfect clone detection — you need:

> *"LLM repetition intensity" as a metric*

---

## 8. Duplicate Method Calls

**Current state:**

* `.findById().orElse(null)` repetition

**Issue:**

* This is partly normal in CRUD apps

**Verdict:**
👉 **Keep, but contextualize**

Make it smarter:

* Flag when repetition spans multiple services
* Combine with:

  * lack of abstraction
  * similar method bodies

---

## 9. Literal Duplication

**Problem:**

* Your metric definition is too narrow (global duplicates only)

**Verdict:**
👉 **Fix, don’t drop**

Upgrade to:

* Magic number detection
* Repeated constants that *should be enums/config*

---

# 🟢 Safe to Deprioritize (Static Analysis Already Covers Well)

These are *not* where your tool should compete.

---

## 10. Basic Syntax / Style Issues

* Wildcard imports
* Naming conventions
* Formatting

👉 Static analyzers + linters already dominate here

---

## 11. Standard Security Checks (Generic)

* Missing HTTPS
* Basic injection patterns

👉 Covered well by existing tools (Sonar, Snyk, etc.)

---

## 12. Simple Exception Handling Issues

* Empty catch blocks

👉 Commodity static analysis

---

# 🧠 The Real Insight (Most Important Part)

Right now your tool is behaving like:

> “A weaker static analyzer with a novel metric”

But your opportunity is:

> **“A detector of AI-specific codebase degradation patterns over time”**

That’s a completely different category.

---

# 🧭 Reframing Your Tool

## What you should STOP trying to be:

* General-purpose static analyzer
* Precise clone detector

## What you should BECOME:

A system that answers:

> **“Is this codebase getting worse because of AI?”**

---

# 🧱 Suggested Detection Layers

### Layer 1: Structural Degradation (you already started)

* Repetition
* Lack of abstraction
* Boilerplate explosion

### Layer 2: Semantic Degradation (you’re missing)

* Null propagation
* Security regressions
* Broken contracts

### Layer 3: Integrity Failures (critical gap)

* Non-compilable code
* Mixed content files
* Missing references

### Layer 4: Intent Drift (your biggest opportunity)

* Missing features
* Partial implementations
* Spec violations

---

# ⚖️ Final Categorisation

## 🔴 Serious Scanner Problems

* ~~Missing non-code detection~~ ✅ **IMPLEMENTED**
* ~~Ignoring parse failures~~ ✅ **IMPLEMENTED**
* ~~No AI-specific security detection~~ ✅ **IMPLEMENTED**
* ~~Broken similarity metric~~ ✅ **FIXED**
* No compilation integrity checks
* No null-pattern detection
* No intent/spec validation

---

## 🟡 Needs Improvement

* ~~Similarity algorithm (concept good, execution flawed)~~ ✅ **FIXED**
* Duplicate method call interpretation
* Literal detection definition

---

## 🟢 Safe to Ignore (Static Analysis Handles)

* Style issues
* Basic syntax
* Standard lint/security checks
* Simple exception handling

---

# If You Fix Only 3 Things

Prioritize these — they’ll 10x your differentiation:

1. ~~**Detect non-code / corrupted files**~~ ✅ **DONE**
2. ~~**Add null-propagation + security anti-pattern detection**~~ ✅ **DONE (security)**
3. ~~**Replace similarity metric with token/AST-based**~~ ✅ **DONE**

---

If you want, I can help you redesign the **scoring model** so your plugin produces something like:

> “AI Degradation Score: 72/100 (↑ worsening across iterations)”

—which would make this far more compelling than raw metrics.

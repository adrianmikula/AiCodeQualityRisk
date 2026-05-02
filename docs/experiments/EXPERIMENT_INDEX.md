# AI Code Generation Experiments Index

**Last Updated:** 2026-05-01  
**Total Experiments:** 4 runs (3 fully analyzed, 1 partial)

---

## Quick Summary Table

| # | Experiment Name | Config File | Workspace Path | Results Path | Status | Projects | Iterations | Variations | Model |
|---|----------------|-------------|----------------|--------------|--------|----------|------------|------------|-------|
| 1 | Simple CRUD Test | `config/test-generator.json` | `workspace/test-generated/` | `workspace/test-generated/results.csv` | ✅ Complete | 3 | 5 | 3 | claude-3.5-sonnet |
| 2 | Comprehensive Explicit vs Implicit | `config/comprehensive-test.json` | `workspace/comprehensive-test/` | `results/2026-04-29_14-41-19/comprehensive_explicit_vs_implicit/` | ✅ Fully Analyzed | 8 | 15 | 8 (4×2) | llama-3.1-8b |
| 3 | SaaS Multi-Tenant | `config/saas-multi-tenant-test.json` | `workspace/saas-multi-tenant-generated/` | `results/2026-04-29_18-00-32/saas_multi_tenant/` | ✅ Fully Analyzed | 2 (1 complete, 1 partial) | 20 | 2/5 | minimax-m2.5-free |
| 4 | Default Generator | `config/generator.json` | `workspace/generated/` | `workspace/generated/results.csv` (empty) | ⚠️ Partial/Abandoned | 35+ UUID dirs | 31 | 10 | minimax-m2.5-free |

---

## Experiment 1: Simple CRUD Test

**Purpose:** Initial smoke test of code generation pipeline with minimal feature set.

**Configuration:** `config/test-generator.json:1`
- Template: `simple_crud`
- Domain: Task Management (Spring Boot REST API)
- Base features: CRUD operations, H2 database, JPA
- Iteration features (5): authentication/JWT, input validation, caching, audit logging, soft delete
- Mode: ITERATIVE only
- Variations requested: 3

**Execution:**
- Model: `claude-3.5-sonnet` (via Anthropic API)
- Workspace: `workspace/test-generated/`
- Projects generated: 3 UUID-named directories:
  - `8488a0b0` (variation 1)
  - `d25cb645` (variation 2)
  - `b71a1b5a` (variation 3)

**Results:**
- Metrics CSV: `workspace/test-generated/results.csv`
- All 3 variations completed successfully
- Average LOC: 236
- Max similarity: 1.0 (indicating high duplication)
- Total LLM calls: 15 (5 iterations × 3 variations)

**Note:** This experiment was not copied to the formal `results/` directory; results remain in workspace.

---

## Experiment 2: Comprehensive Explicit vs Implicit

**Purpose:** Compare code quality degradation when using explicit (detailed) vs implicit (minimal) prompts across two domains.

**Configuration:** `config/comprehensive-test.json:1`
- Test ID: `COMP_001`
- Date: 2026-04-29 14:41:19
- Duration: 3020 seconds (~50 minutes)
- Total LLM calls: 120 (8 projects × 15 iterations)

**Templates (4):**

| Template | Domain | Prompt Style | Description |
|----------|--------|--------------|-------------|
| `task_mgmt_explicit` | Task Management | Explicit | Detailed specs with best practices |
| `task_mgmt_implicit` | Task Management | Implicit | Minimal specifications |
| `ecommerce_explicit` | E-commerce | Explicit | Detailed architecture specs |
| `ecommerce_implicit` | E-commerce | Implicit | Minimal specifications |

**Parameters:**
- Iterations per project: 15
- Variations per template: 2
- Mode: ITERATIVE only
- Model: `meta-llama/llama-3.1-8b-instruct`
- Provider: OpenRouter

**Input Paths:**
- Config source: `config/comprehensive-test.json`
- Copied to results as: `results/2026-04-29_14-41-19/comprehensive_explicit_vs_implicit/config.json`

**Output Paths:**

```
results/2026-04-29_14-41-19/comprehensive_explicit_vs_implicit/
├── config.json                      # Input configuration copy
├── test_metadata.json               # Execution metadata
├── project_metadata.csv             # 8 project records
├── metrics_results.csv              # Final metrics per project
├── comparison_summary.csv           # Explicit vs implicit comparison
├── README.md                        # Full analysis report
├── findings_report.md               # Detailed findings
└── projects/                        # Generated code (8 UUID dirs)
    ├── 01fd7718/  (task_mgmt_explicit, var 1)
    ├── 458625c3/  (task_mgmt_explicit, var 2)
    ├── f5d2486c/  (task_mgmt_implicit, var 1)
    ├── 849caaa5/  (task_mgmt_implicit, var 2)
    ├── c29850dd/  (ecommerce_explicit, var 1)
    ├── f006062d/  (ecommerce_explicit, var 2)
    ├── 041ce7ff/  (ecommerce_implicit, var 1)
    └── b914bd78/  (ecommerce_implicit, var 2)
```

**Workspace mirror:** `workspace/comprehensive-test/` (same 8 project directories + `results.csv`)

**Key Finding:** Explicit prompts reduced duplication by 67% in Task Management domain vs implicit prompts. E-commerce domain showed high duplication regardless (max similarity 1.0), suggesting complex domains overwhelm prompt clarity benefits.

---

## Experiment 3: SaaS Multi-Tenant

**Purpose:** Measure degradation in a complex, cross-cutting enterprise domain with 20 architectural features.

**Configuration:** `config/saas-multi-tenant-test.json:1`
- Test ID: `saas_multi_tenant`
- Date: 2026-04-29 18:00:32
- Duration: 3900 seconds (~65 minutes)
- Template: `saas_multi_tenant`
- Domain: Multi-tenant SaaS platform
- Mode: ITERATIVE only
- Iterations: 20 cross-cutting features
- Variations requested: 5
- Model: `opencode/minimax-m2.5-free`
- Provider: aichat (CLI tool)

**15 Iteration Features:**
1. JWT authentication with organization context
2. Role-based access control across endpoints
3. Audit logging for user and organization actions
4. Subscription plan restrictions (feature gating)
5. Caching for frequently accessed org data
6. Pagination and filtering across all list endpoints
7. Global exception handling
8. API usage tracking per organization
9. Rate limiting per tenant
10. Structured logging with request correlation IDs
11. Soft delete for users and organizations
12. Data isolation safeguards between tenants
13. Background jobs (subscription checks)
14. Integration tests for key flows
15. Refactor for scalability and modularity
16. Webhook support for organization events
17. Data export functionality for tenant data
18. Customizable email templates per organization
19. API versioning support
20. Organization-level customization settings

**Input Paths:**
- Config source: `config/saas-multi-tenant-test.json`
- Copied to results as: `results/2026-04-29_18-00-32/saas_multi_tenant/config.json`

**Output Paths:**

```
results/2026-04-29_18-00-32/saas_multi_tenant/
├── config.json                      # Input configuration copy
├── test_metadata.json               # Execution metadata
├── project_metadata.csv             # 2 project records
├── metrics_results.csv              # Final metrics per project
├── comparison_summary.csv           # (if applicable)
├── README.md                        # Full analysis report
├── findings_report.md               # Structured findings per section
├── code-quality-analysis.md         # Detailed code quality analysis
└── projects/                        # Generated code
    ├── 7abb05be/  (variation 1 - complete)
    └── 39333d6c/  (variation 1 - partial/crashed)
```

**Workspace mirror:** `workspace/saas-multi-tenant-generated/`
- Contains 6 UUID directories (5 variations + 1 extra failed)
- `results.csv` shows 2 analyzed projects (6d546371 additional)

**Key Finding:** Extreme code duplication detected (max similarity 1.0). Identical CRUD methods repeated across services. High degradation: 2058-5515 similar method pairs. Large refactoring iterations (11+ files) indicate architectural churn.

---

## Experiment 4: Default Generator (Abandoned/Partial)

**Purpose:** Baseline test using default generator configuration; appears to have been partially executed but not analyzed.

**Configuration:** `config/generator.json:1`
- Template: `crud_app`
- Domain: Task Management
- Base features: CRUD, H2 database, JPA
- Iteration features: 31 comprehensive features (from auth to custom fields)
- Mode: Both SINGLE_SHOT and ITERATIVE
- Variations requested: 10
- Model: `opencode/minimax-m2.5-free`

**Execution Status:**
- Workspace: `workspace/generated/`
- Contains 35+ UUID-named project directories (suggests multiple runs)
- `workspace/generated/results.csv` exists but **empty** (header only)
- No corresponding `results/YYYY-MM-DD/` directory with analysis

**Conclusion:** This configuration was likely used for preliminary testing but never formalized into a complete analyzed experiment. The workspace contains generated code but no metrics analysis was performed or preserved.

---

## Iteration & Variation Counts

| Experiment | Iterations per Variation | Variations Requested | Variations Completed | Total Projects | Total LLM Calls |
|------------|-------------------------|---------------------|----------------------|----------------|-----------------|
| Simple CRUD | 5 | 3 | 3 | 3 | 15 |
| Comprehensive | 15 | 8 (4×2) | 8 | 8 | 120 |
| SaaS Multi-Tenant | 20 | 5 (planned) | 2 (1 full, 1 partial) | 2 analyzed | 21 |
| Default Generator | 31 | 10 | 0 (unanalyzed) | 35+ dirs, 0 analyzed | unknown |

**Total across analyzed experiments:** 13 projects, 155 LLM calls (40 iterations per project avg).

---

## Complete File Path Index

### Configuration Files (Inputs)
```
config/
├── generator.json                    # Exp 4: Default config (31 features, 10 vars)
├── test-generator.json               # Exp 1: Simple CRUD (5 features, 3 vars)
├── comprehensive-test.json           # Exp 2: Explicit vs implicit (15 features, 8 vars)
├── saas-multi-tenant-test.json       # Exp 3: SaaS platform (20 features, 5 vars)
└── thresholds.yaml                   # Quality metric thresholds
```

### Workspace Directories (Raw Generated Code)
```
workspace/
├── generated/                         # Exp 4: 35+ project UUID dirs (unanalyzed)
│   ├── 0147ace2-5590-4803-b091-ad3bf64c9df3/
│   ├── ... (33 more)
│   └── results.csv                    # Empty (no analysis)
├── test-generated/                    # Exp 1: Simple CRUD
│   ├── 8488a0b0/  (var 1)
│   ├── d25cb645/  (var 2)
│   ├── b71a1b5a/  (var 3)
│   └── results.csv                    # Metrics for 3 projects
├── test-generated-real/               # Empty / unused
├── saas-multi-tenant-generated/      # Exp 3: SaaS MT
│   ├── 7abb05be/  (var 1 - complete)
│   ├── 39333d6c/  (var 1 - partial)
│   ├── 475bdfd3/  (var 2 - partial?)
│   ├── 477b0c8c/  (var 3?)
│   ├── 6d546371/  (var 4?)
│   └── results.csv                    # Shows 6d546371 only; others may be incomplete
└── comprehensive-test/               # Exp 2: Explicit vs Implicit
        ├── 01fd7718/   (task_mgmt_explicit, var 1)
        ├── 458625c3/   (task_mgmt_explicit, var 2)
        ├── f5d2486c/   (task_mgmt_implicit, var 1)
        ├── 849caaa5/   (task_mgmt_implicit, var 2)
        ├── c29850dd/   (ecommerce_explicit, var 1)
        ├── f006062d/   (ecommerce_explicit, var 2)
        ├── 041ce7ff/   (ecommerce_implicit, var 1)
        ├── b914bd78/   (ecommerce_implicit, var 2)
        └── results.csv                # Mirrors results/ CSV
```

### Results Directories (Analyzed Output)
```
results/
├── INDEX.md                           # Master index (also at docs/CODE_GEN_TEST_RESULTS.md)
├── 2026-04-29_14-41-19/
│   └── comprehensive_explicit_vs_implicit/
│       ├── config.json                # Copy of config/comprehensive-test.json
│       ├── test_metadata.json         # Execution metadata
│       ├── project_metadata.csv       # 8 project records
│       ├── metrics_results.csv        # Final aggregated metrics
│       ├── comparison_summary.csv     # Explicit vs implicit comparison
│       ├── README.md                  # Full experimental report
│       ├── findings_report.md
│       ├── code-quality-analysis.md
│       ├── summaries/
│       │   └── raw_results.csv        # Final iteration metrics
│       └── projects/                  # 8 project UUID dirs (same as workspace)
├── 2026-04-29_18-00-32/
│   └── saas_multi_tenant/
│       ├── config.json                # Copy of config/saas-multi-tenant-test.json
│       ├── test_metadata.json         # Execution metadata
│       ├── project_metadata.csv       # 2 project records
│       ├── metrics_results.csv        # Final aggregated metrics
│       ├── README.md                  # Experimental report
│       ├── findings_report.md         # Structured findings
│       ├── code-quality-analysis.md   # Code quality deep-dive
│       ├── summaries/
│       │   └── raw_results.csv        # Final iteration metrics
│       └── projects/                  # 2 project UUID dirs
└── [future runs will be timestamped here]
```

### Documentation
```
docs/
├── CODE_GEN_TEST_RESULTS.md           # Duplicate of results/INDEX.md
├── experiments/
│   └── post_experiment_reports.md    # Template for analysis output
├── viral_case_study.md               # Content strategy for social media
├── scanner_blind_spots.md           # Tooling blind spots analysis
├── README.md                         # Main project documentation
├── implementation_guides/
│   ├── adaptive_thresholds.md
│   ├── multi_granular_shingling.md
│   └── semantic_fingerprinting.md   (planned)
└── patterns/
    └── [pattern detection docs]
```

### Logs
```
saas-test-output.log                  # SaaS experiment log (binary)
openrouter_test.log                   # OpenRouter API test log
generator_output.log                  # Generator log (binary)
.kotlin/errors/                       # 40+ Kotlin compilation/execution error logs
```

---

## How to Add New Experiments

1. Create config file in `config/` following existing schema
2. Run generator: `bash gradlew run --args="config/your-config.json"`
3. Copy generated workspace output to `results/YYYY-MM-DD_HH-MM-SS/test_name/`
4. Run analyzer to produce CSVs and reports
5. Update `results/INDEX.md` and `docs/CODE_GEN_TEST_RESULTS.md`

---

## Key Insights from Completed Experiments

- **Explicit prompts** reduce duplication by 67% in simple domains (Task Management)
- **Implicit prompts** lead to near-perfect duplication (max similarity 1.0) in complex domains (E-commerce, SaaS)
- **Complex SaaS domains** overwhelm any prompt engineering benefits; all variations degrade severely
- **Local models** (Minimax M2.5) produce more duplication than larger cloud models (Llama 3.1 8B via OpenRouter)
- **Iterative generation** consistently produces high similarity scores (>0.9) after 15+ features
- **Variation stability**: 2/3 variations produce consistent degradation patterns; some crash mid-run

---

## Notes

- All UUID project directories are generated by the ExperimentRunner and contain final iteration code under `src/main/java/`
- Results CSV files contain **final iteration** metrics only; per-iteration progression is in `test_metadata.json` duration but not per-iteration metrics (unless manually logged during run)
- The `summaries/raw_results.csv` files are generated post-hoc from the workspace projects
- Experiments 1 and 4 were not formally archived in `results/` with full metadata; only workspace and CSV remain
- The `workspace/generated/` folder (Exp 4) appears to contain many experimental runs but no formal analysis was performed

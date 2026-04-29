# AI Code Quality Risk Test Results Index

## Overview

This directory contains standardized test results from AI code generation experiments measuring code degradation ("AI slop") over multiple iterations.

## Directory Structure

```
results/
├── INDEX.md                          # This index file
├── YYYY-MM-DD_HH-MM-SS/             # Timestamped test runs
│   └── test_name/
│       ├── config.json                # ← Test input configuration
│       ├── README.md                  # Detailed test documentation
│       ├── test_metadata.json         # Test execution metadata
│       ├── project_metadata.csv       # Project-level metadata
│       ├── metrics_results.csv        # Code quality metrics
│       ├── comparison_summary.csv     # Comparative analysis
│       ├── summaries/
│       │   └── raw_results.csv       # Original generator output
│       └── projects/
│           └── [project_id]/         # Generated code projects
```

## Available Tests

### 2026-04-29_14-41-19

**Test:** comprehensive_explicit_vs_implicit  
**Description:** Comparison of explicit vs implicit prompts across task management and e-commerce domains  
**Projects:** 8 (4 templates × 2 variations)  
**Iterations:** 15 per project  
**Duration:** ~50 minutes  
**Key Finding:** Explicit prompts reduce duplication 67% in simple domains

[View Results →](2026-04-29_14-41-19/comprehensive_explicit_vs_implicit/README.md)

## CSV Schema

### project_metadata.csv
| Column | Description |
|--------|-------------|
| timestamp | ISO 8601 timestamp |
| test_id | Unique test identifier |
| project_id | Generated project UUID |
| template_name | Template used |
| prompt_style | explicit or implicit |
| domain | task_management or e_commerce |
| mode | ITERATIVE or SINGLE_SHOT |
| variation | Variation number |
| iteration_count | Number of iterations |
| llm_model | Model name |
| llm_provider | API provider |

### metrics_results.csv
| Column | Description |
|--------|-------------|
| timestamp | ISO 8601 timestamp |
| test_id | Unique test identifier |
| project_id | Generated project UUID |
| total_loc | Lines of code |
| total_files | Java files generated |
| total_methods | Methods detected |
| dup_string_literals | Duplicate string count |
| dup_number_literals | Duplicate number count |
| dup_method_calls | Duplicate method call count |
| similar_method_pairs | Similar method pair count |
| max_similarity_score | Maximum Jaccard similarity |
| avg_similarity_score | Average Jaccard similarity |
| code_entropy | Calculated entropy score |

## Quick Analysis Commands

### PowerShell

```powershell
# View all test results
Get-ChildItem results -Directory | ForEach-Object {
    $testDir = Join-Path $_.FullName (Get-ChildItem $_.FullName -Directory)[0].Name
    $meta = Get-Content "$testDir\test_metadata.json" | ConvertFrom-Json
    [PSCustomObject]@{
        Date = $_.Name
        Test = $meta.test_name
        Projects = $meta.results_summary.successful_projects
        Duration = $meta.duration_seconds
        Avg_LOC = $meta.results_summary.avg_loc
        Avg_Similarity = $meta.results_summary.avg_max_similarity
    }
}

# Compare explicit vs implicit
Import-Csv results\2026-04-29_14-41-19\comprehensive_explicit_vs_implicit\comparison_summary.csv | 
    Format-Table -AutoSize
```

### Python

```python
import pandas as pd
import json

# Load metrics
metrics = pd.read_csv('results/2026-04-29_14-41-19/comprehensive_explicit_vs_implicit/metrics_results.csv')

# Load metadata
with open('results/2026-04-29_14-41-19/comprehensive_explicit_vs_implicit/test_metadata.json') as f:
    meta = json.load(f)

# Analysis
print(f"Test: {meta['test_name']}")
print(f"Projects: {meta['results_summary']['successful_projects']}")
print(f"Avg LOC: {meta['results_summary']['avg_loc']:.1f}")
print(f"Avg Similarity: {meta['results_summary']['avg_max_similarity']:.3f}")
```

## Degradation Metrics Explained

### Primary Metrics

1. **Similar Method Pairs**: Methods with Jaccard similarity > 0.8
   - Indicates code duplication and lack of abstraction
   - Lower is better

2. **Max Similarity Score**: Highest similarity between any two methods
   - 1.0 = identical methods (worst)
   - < 0.5 = diverse implementations (best)

3. **Code Entropy**: Normalized disorder metric
   - 0.0 = perfectly organized
   - 1.0 = completely chaotic

### Secondary Metrics

- **Duplicate Literals**: Repeated constants (strings, numbers)
- **Duplicate Method Calls**: Repeated invocations
- **LOC Growth**: Lines of code per iteration
- **File Count Growth**: Number of files per iteration

## Adding New Results

To add a new test run:

1. Create directory: `results/YYYY-MM-DD_HH-MM-SS/test_name/`
2. Copy input config to `config.json` ← **Required for reproducibility**
3. Copy project directories to `projects/`
4. Create CSV files:
   - `project_metadata.csv` - Project info
   - `metrics_results.csv` - Quality metrics
   - `comparison_summary.csv` - Comparative analysis (optional)
4. Create JSON file: `test_metadata.json` - Configuration
5. Create documentation: `README.md` - Detailed report
6. Update this INDEX.md

## LLM Models Tested

- meta-llama/llama-3.1-8b-instruct (OpenRouter, free)
- qwen/qwen2.5-coder-14b (LM Studio, local)
- qwen2.5-coder:7b-instruct (Ollama, local)

## Contact

Framework: AI Code Quality Risk IntelliJ Plugin  
Generator: ExperimentRunner.kt  
Analyzer: DetectionRunner.kt  
Parser: FileExtractor.kt

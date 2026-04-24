# Approach for Observing Natural AI Code Entropy

## Overview
Instead of explicitly prompting for poor code quality, our approach focuses on generating normal coding tasks and observing whether code entropy naturally increases through repeated iterations and feature additions.

## Methodology

### 1. Normal Coding Prompts
- All prompts follow standard Java/Spring Boot best practices
- No explicit mention of code quality issues or intentional degradation
- Focus on legitimate feature requests and standard implementation approaches

### 2. Iterative Feature Addition
- Use the ITERATIVE generation mode to add multiple features sequentially
- Each iteration builds upon the previous codebase
- Features are reasonable enhancements that would normally be requested in a project

### 3. Increased Iteration Count
- Set variationsPerPrompt to 5 or higher to generate multiple independent evolutionary paths
- This increases the likelihood of observing quality variations
- Each variation represents a different trajectory of feature additions

### 4. Feature Selection
- Choose features that naturally build upon each other
- Start with basic CRUD operations
- Progress to more complex features like authentication, caching, etc.
- Each feature requires understanding and modifying existing code

### 5. Quality Metrics Collection
- Use the DetectionRunner to measure code quality indicators:
  * Duplicate string literals
  * Duplicate number literals (magic numbers)
  * Duplicate method calls
  * Method similarity scores
  * Lines of code (LOC)
- Compare SINGLE_SHOT vs ITERATIVE modes
- Track quality degradation across iterations within the same project

## Expected Outcomes
Through this approach, we expect to observe:
- Natural accumulation of technical debt as developers (or LLMs) take shortcuts
- Increased duplication as similar patterns are implemented slightly differently
- Growing complexity that makes maintenance harder
- Potential quality differences between single-shot and iterative approaches

The hypothesis is that without explicit quality-focused prompting, LLMs may exhibit quality degradation patterns similar to human developers when faced with evolving requirements and time pressure.

## Implementation Details
1. Modified PromptBuilder.kt to generate standard, quality-neutral prompts
2. Updated config/generator.json with:
   - Normal feature requests for iterationFeatures
   - Increased variationsPerPrompt (5) for more data points
   - Both SINGLE_SHOT and ITERATIVE modes for comparison
3. The DetectionRunner automatically measures quality metrics from generated code

## Analysis
Results will be analyzed in workspace/generated/results.csv to compare:
- Quality metric averages between SINGLE_SHOT and ITERATIVE modes
- Trends in quality metrics across iterations within ITERATIVE projects
- Correlation between number of features added and quality degradation
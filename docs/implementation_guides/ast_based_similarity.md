# AST-Based Similarity Detection Implementation Guide

## Overview

This guide documents the AST-based similarity detection approach that replaces the pure Jaccard-based similarity to reduce inflated scores and better measure "LLM repetition intensity" in AI-generated code.

## Problem with Previous Approach

The original implementation used token-based Jaccard similarity on multi-granular shingles, which:
- Produced inflated 0.95-1.0 scores for CRUD methods
- Generated massive pair counts (2k-5k)
- Measured set-based similarity without considering structural sequence
- Didn't capture the "LLM repetition intensity" metric needed

## Solution: Combined AST + Shingle Approach

The new approach combines:
- **60% shingle-based similarity** (existing MultiGranularShingleBuilder)
- **40% AST structural similarity** (new ASTSubtreeComparator)
- **LLM repetition intensity** metric (new LLMRepetitionIntensity calculator)

## Architecture

### 1. ASTSubtreeComparator

**File**: `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/ASTSubtreeComparator.kt`

**Purpose**: Extracts and compares AST subtree structures from method bodies.

**Key Functions**:
- `extractSubtreeHash(methodNode, source)`: Normalizes AST subtree to a hash string
- `calculateSimilarity(hash1, hash2)`: Calculates structural similarity using LCS
- `calculateTreeEditDistance(hash1, hash2)`: Calculates tree edit distance approximation

**Normalization**:
- Identifiers → "ID"
- Literals → "LIT" or "NUM"
- Control structures → normalized types (IF, FOR, WHILE, etc.)
- Comments → skipped
- Focus on structural patterns, not specific values

**Example**:
```kotlin
// Original code
if (x > 0) {
    System.out.println(x);
}

// Normalized hash
"IF EXPR CALL"
```

### 2. LLMRepetitionIntensity

**File**: `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/LLMRepetitionIntensity.kt`

**Purpose**: Calculates "LLM repetition intensity" as a metric (0-100 scale).

**Key Functions**:
- `calculateIntensity(pairs, totalMethods, threshold)`: Main intensity calculation
- `calculateRepetitionCoverage(pairs, totalMethods, minSimilarMethods)`: % of methods with N+ similar methods
- `identifyRepetitionHotspots(pairs)`: Finds most frequently duplicated methods
- `calculateClassLevelIntensity(pairs, methodToClassMap)`: Per-class intensity
- `calculateIntensityTrend(history)`: Tracks intensity over iterations

**Intensity Formula**:
```
intensity = (repetitionCoverage * 0.4) + (averageSimilarity * 0.4) + (pairDensity * 0.2)
finalScore = intensity * 100 (scaled to 0-100)
```

**Components**:
- **Repetition Coverage**: % of methods similar to ≥N other methods
- **Average Similarity**: Mean similarity across all pairs
- **Pair Density**: Ratio of similar pairs to total possible pairs

### 3. TreeSitterFuzzyDetector Integration

**File**: `src/main/kotlin/com/aicodequalityrisk/plugin/analysis/TreeSitterFuzzyDetector.kt`

**Changes**:
- Added `astComparator: ASTSubtreeComparator`
- Added `repetitionIntensityCalculator: LLMRepetitionIntensity`
- Added `createASTFingerprint()` method
- Updated `analyzeTree()` to use both approaches

**Combined Similarity Calculation**:
```kotlin
val shingleSimilarity = fingerprint.getSimilarityScore(other)
val structuralSimilarity = astFp1.getSimilarityScore(astFp2, astComparator)
val treeEditDistance = astFp1.getTreeEditDistance(astFp2, astComparator)

// 60% shingle + 40% structural
val combinedSimilarity = (shingleSimilarity * 0.6) + (structuralSimilarity * 0.4)
```

### 4. Data Model Updates

**FuzzyMetrics**:
- Added `llmRepetitionIntensity: Double = 0.0`
- Added `astBasedSimilarityEnabled: Boolean = false`

**MethodSimilarityPair**:
- Added `structuralSimilarity: Double = 0.0`
- Added `treeEditDistance: Double = 0.0`

**ASTSubtreeFingerprint** (new):
```kotlin
data class ASTSubtreeFingerprint(
    val name: String,
    val subtreeHash: String,
    val methodLength: Int,
    val complexity: Int,
    val tokenCount: Int
)
```

## Expected Outcomes

### Similarity Score Ranges

| Scenario | Old Range | New Range |
|----------|-----------|-----------|
| Identical methods | 0.95-1.0 | 0.8-1.0 |
| Similar CRUD methods | 0.95-1.0 | 0.5-0.8 |
| Different methods | <0.5 | <0.5 |

### Pair Count Reduction

- **Before**: 2k-5k pairs for large codebases
- **After**: Hundreds of pairs (5-10x reduction)

### LLM Repetition Intensity

- **0-25**: Low repetition (healthy codebase)
- **25-50**: Moderate repetition (some boilerplate)
- **50-75**: High repetition (significant AI-generated patterns)
- **75-100**: Extreme repetition (massive duplication)

## Usage Example

```kotlin
val detector = TreeSitterFuzzyDetector()
val metrics = detector.detect(code, filePath)

// Check AST-based similarity is enabled
println("AST-based: ${metrics.astBasedSimilarityEnabled}")

// Get LLM repetition intensity
println("Intensity: ${metrics.llmRepetitionIntensity}/100")

// Examine individual pairs
for (pair in metrics.duplicateMethodPairs) {
    println("${pair.firstMethod} ~ ${pair.secondMethod}")
    println("  Combined: ${pair.similarity}")
    println("  Structural: ${pair.structuralSimilarity}")
    println("  Tree Edit Distance: ${pair.treeEditDistance}")
}
```

## Testing

### Unit Tests

- `ASTSubtreeComparatorTest`: Tests tree comparison logic
- `LLMRepetitionIntensityTest`: Tests intensity calculation

### Integration Tests

- `TreeSitterFuzzyDetectorTest`: Updated to expect 0.5-0.8 range
- `IntegrationTest`: Updated fuzzyMetrics expectations

## Performance Considerations

- **AST extraction**: O(n) where n = method body size
- **Similarity calculation**: O(m*n) where m,n = hash lengths
- **Overall impact**: Minimal - AST parsing already done for other metrics

## Future Enhancements

1. **Adaptive Weighting**: Adjust 60/40 split based on codebase characteristics
2. **Class-Level Analysis**: Better cross-class repetition detection
3. **Trend Tracking**: Monitor intensity changes across iterations
4. **Threshold Tuning**: Make thresholds configurable per project

## Migration Notes

### Backward Compatibility

- Existing `FuzzyMetrics` fields preserved
- New fields have default values (0.0, false)
- Tests updated to reflect new expectations
- No breaking changes to public API

### Upgrading Existing Code

```kotlin
// Before
if (metrics.maxSimilarityScore > 0.9) {
    // Flag as highly duplicated
}

// After
if (metrics.llmRepetitionIntensity > 50) {
    // Flag as high repetition intensity
}
```

## References

- **Original Issue**: scanner_blind_spots.md - Section 7
- **Related**: multi_granular_shingling.md (shingle-based approach)
- **Implementation Files**:
  - ASTSubtreeComparator.kt
  - LLMRepetitionIntensity.kt
  - TreeSitterFuzzyDetector.kt

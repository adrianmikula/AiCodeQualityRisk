# Performance Benchmarking Guide

## Overview

This guide provides comprehensive performance benchmarking procedures for the enhanced treesitter fuzzy detection system. Benchmarks ensure the system maintains <50ms analysis time while improving accuracy.

## Benchmark Categories

### 1. Processing Time Benchmarks
- **Single file analysis**: Measure time to analyze individual files
- **Batch processing**: Measure throughput for multiple files
- **Incremental analysis**: Measure time for partial file updates
- **Memory usage**: Monitor heap consumption during analysis

### 2. Accuracy Benchmarks
- **Detection accuracy**: True positive rate on AI-generated code
- **False positive rate**: Incorrect flags on human-written code
- **Pattern coverage**: Percentage of AI slop patterns detected
- **Cross-language performance**: Accuracy across different languages

### 3. Scalability Benchmarks
- **File size scaling**: Performance with increasing file sizes
- **Project size scaling**: Performance with larger codebases
- **Concurrent analysis**: Multi-threaded performance
- **Memory scaling**: Memory usage with project size

## Benchmark Setup

### Test Environment
```kotlin
data class BenchmarkEnvironment(
    val jvmVersion: String = System.getProperty("java.version"),
    val availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
    val maxMemory: Long = Runtime.getRuntime().maxMemory(),
    val osName: String = System.getProperty("os.name"),
    val osVersion: String = System.getProperty("os.version")
)

class BenchmarkEnvironmentSetup {
    fun setupEnvironment(): BenchmarkEnvironment {
        // Configure JVM for consistent benchmarking
        System.setProperty("java.awt.headless", "true")
        System.setProperty("jmh.ignore.locks", "true")
        
        // Warm up JVM
        warmUpJvm()
        
        return BenchmarkEnvironment()
    }
    
    private fun warmUpJvm() {
        repeat(1000) {
            // Perform dummy operations to warm up JIT
            "test".hashCode()
        }
        
        System.gc() // Clean up before benchmarks
    }
}
```

### Test Data Generation
```kotlin
class BenchmarkDataGenerator {
    private val random = Random(42) // Fixed seed for reproducibility
    
    fun generateTestFiles(count: Int, sizeRange: IntRange): List<TestFile> {
        return (1..count).map { i ->
            val size = random.nextInt(sizeRange.first, sizeRange.last + 1)
            TestFile(
                name = "TestFile$i.java",
                content = generateJavaCode(size),
                expectedDuplicates = random.nextInt(0, 5),
                complexity = random.nextInt(1, 15)
            )
        }
    }
    
    fun generateJavaCode(lines: Int): String {
        val builder = StringBuilder()
        builder.appendLine("public class GeneratedClass$random {")
        
        repeat(lines / 10) { methodIndex ->
            val methodLines = random.nextInt(5, 20)
            builder.appendLine("    public void method$methodIndex() {")
            
            repeat(methodLines) {
                when (random.nextInt(5)) {
                    0 -> builder.appendLine("        if (condition$random) {")
                    1 -> builder.appendLine("            System.out.println(\"test$random\");")
                    2 -> builder.appendLine("        }")
                    3 -> builder.appendLine("        for (int i$random = 0; i$random < 10; i$random++) {")
                    4 -> builder.appendLine("            doWork$random();")
                }
            }
            
            builder.appendLine("    }")
        }
        
        builder.appendLine("}")
        return builder.toString()
    }
    
    fun generateAIGeneratedSlop(): String {
        // Generate typical AI-generated patterns
        return """
            public class AIGeneratedService {
                private String firstName;
                private String lastName;
                private String email;
                private String phone;
                private String address;
                private String city;
                private String state;
                private String zipCode;
                private String country;
                private int age;
                private String occupation;
                private String company;
                private String department;
                private long salary;
                private String startDate;
                private String endDate;
                private boolean active;
                private String notes;
                private String preferences;
                private String lastLogin;
                
                // Excessive getters and setters
                public String getFirstName() { return firstName; }
                public void setFirstName(String firstName) { this.firstName = firstName; }
                public String getLastName() { return lastName; }
                public void setLastName(String lastName) { this.lastName = lastName; }
                public String getEmail() { return email; }
                public void setEmail(String email) { this.email = email; }
                // ... more repetitive getters/setters
                
                public void processUserData(UserData data) {
                    if (data != null && data.getFirstName() != null && 
                        data.getLastName() != null && data.getEmail() != null &&
                        data.getPhone() != null && data.getAddress() != null) {
                        
                        if (data.getFirstName().length() > 0 && 
                            data.getFirstName().length() < 50 &&
                            data.getLastName().length() > 0 && 
                            data.getLastName().length() < 50) {
                            
                            if (data.getEmail().contains("@") && 
                                data.getEmail().contains(".")) {
                                
                                System.out.println("Processing user: " + 
                                    data.getFirstName() + " " + data.getLastName());
                                    
                                try {
                                    saveUserData(data);
                                } catch (Exception e) {
                                    // Over-defensive error handling
                                    System.err.println("Error saving user data: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
                
                private void saveUserData(UserData data) {
                    // Implementation would go here
                }
            }
        """.trimIndent()
    }
}

data class TestFile(
    val name: String,
    val content: String,
    val expectedDuplicates: Int,
    val complexity: Int
)
```

## Performance Benchmark Tests

### 1. Single File Analysis Benchmarks
```kotlin
class SingleFileBenchmark {
    private val detector = TreeSitterFuzzyDetector()
    private val dataGenerator = BenchmarkDataGenerator()
    
    fun benchmarkSingleFileAnalysis(): BenchmarkResults {
        val testFiles = dataGenerator.generateTestFiles(100, 50..500)
        val results = mutableListOf<SingleFileResult>()
        
        testFiles.forEach { testFile ->
            val startTime = System.nanoTime()
            val startMemory = getUsedMemory()
            
            val metrics = detector.detect(testFile.content, testFile.name)
            
            val endTime = System.nanoTime()
            val endMemory = getUsedMemory()
            
            results.add(
                SingleFileResult(
                    fileName = testFile.name,
                    lines = testFile.content.lines().size,
                    processingTimeMs = (endTime - startTime) / 1_000_000,
                    memoryUsedMB = (endMemory - startMemory) / (1024 * 1024),
                    duplicateCount = metrics.duplicateMethodCount,
                    similarityScore = metrics.maxSimilarityScore
                )
            )
        }
        
        return BenchmarkResults(
            category = "Single File Analysis",
            results = results,
            summary = calculateSummary(results)
        )
    }
    
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun calculateSummary(results: List<SingleFileResult>): BenchmarkSummary {
        return BenchmarkSummary(
            avgProcessingTimeMs = results.map { it.processingTimeMs }.average(),
            maxProcessingTimeMs = results.maxOfOrNull { it.processingTimeMs } ?: 0.0,
            minProcessingTimeMs = results.minOfOrNull { it.processingTimeMs } ?: 0.0,
            avgMemoryUsedMB = results.map { it.memoryUsedMB }.average(),
            maxMemoryUsedMB = results.maxOfOrNull { it.memoryUsedMB } ?: 0.0,
            totalFiles = results.size
        )
    }
}

data class SingleFileResult(
    val fileName: String,
    val lines: Int,
    val processingTimeMs: Long,
    val memoryUsedMB: Long,
    val duplicateCount: Int,
    val similarityScore: Double
)

data class BenchmarkResults(
    val category: String,
    val results: List<SingleFileResult>,
    val summary: BenchmarkSummary
)

data class BenchmarkSummary(
    val avgProcessingTimeMs: Double,
    val maxProcessingTimeMs: Double,
    val minProcessingTimeMs: Double,
    val avgMemoryUsedMB: Double,
    val maxMemoryUsedMB: Double,
    val totalFiles: Int
)
```

### 2. Batch Processing Benchmarks
```kotlin
class BatchProcessingBenchmark {
    private val detector = TreeSitterFuzzyDetector()
    private val dataGenerator = BenchmarkDataGenerator()
    
    fun benchmarkBatchProcessing(): BatchBenchmarkResults {
        val batchSizes = listOf(10, 50, 100, 500, 1000)
        val results = mutableListOf<BatchResult>()
        
        batchSizes.forEach { batchSize ->
            val testFiles = dataGenerator.generateTestFiles(batchSize, 100..1000)
            
            val startTime = System.nanoTime()
            val startMemory = getUsedMemory()
            
            val batchResults = testFiles.map { testFile ->
                detector.detect(testFile.content, testFile.name)
            }
            
            val endTime = System.nanoTime()
            val endMemory = getUsedMemory()
            
            results.add(
                BatchResult(
                    batchSize = batchSize,
                    totalProcessingTimeMs = (endTime - startTime) / 1_000_000,
                    avgProcessingTimeMs = (endTime - startTime) / 1_000_000 / batchSize,
                    memoryUsedMB = (endMemory - startMemory) / (1024 * 1024),
                    throughputFilesPerSecond = batchSize * 1000.0 / ((endTime - startTime) / 1_000_000),
                    totalDuplicates = batchResults.sumOf { it.duplicateMethodCount }
                )
            )
        }
        
        return BatchBenchmarkResults(results)
    }
}

data class BatchResult(
    val batchSize: Int,
    val totalProcessingTimeMs: Long,
    val avgProcessingTimeMs: Double,
    val memoryUsedMB: Long,
    val throughputFilesPerSecond: Double,
    val totalDuplicates: Int
)

data class BatchBenchmarkResults(
    val results: List<BatchResult>
) {
    fun getScalabilityMetrics(): ScalabilityMetrics {
        val firstResult = results.first()
        val lastResult = results.last()
        
        return ScalabilityMetrics(
            timeScalingFactor = lastResult.avgProcessingTimeMs / firstResult.avgProcessingTimeMs,
            memoryScalingFactor = lastResult.memoryUsedMB.toDouble() / firstResult.memoryUsedMB,
            throughputDegradation = firstResult.throughputFilesPerSecond / lastResult.throughputFilesPerSecond
        )
    }
}

data class ScalabilityMetrics(
    val timeScalingFactor: Double,
    val memoryScalingFactor: Double,
    val throughputDegradation: Double
)
```

### 3. Accuracy Benchmarks
```kotlin
class AccuracyBenchmark {
    private val detector = TreeSitterFuzzyDetector()
    private val dataGenerator = BenchmarkDataGenerator()
    
    fun benchmarkDetectionAccuracy(): AccuracyResults {
        val aiGeneratedFiles = generateAIGeneratedTestFiles()
        val humanWrittenFiles = generateHumanWrittenTestFiles()
        
        val aiResults = aiGeneratedFiles.map { testFile ->
            val metrics = detector.detect(testFile.content, testFile.name)
            AccuracyResult(
                fileName = testFile.name,
                isAIGenerated = true,
                expectedDuplicates = testFile.expectedDuplicates,
                actualDuplicates = metrics.duplicateMethodCount,
                correctlyDetected = metrics.duplicateMethodCount > 0
            )
        }
        
        val humanResults = humanWrittenFiles.map { testFile ->
            val metrics = detector.detect(testFile.content, testFile.name)
            AccuracyResult(
                fileName = testFile.name,
                isAIGenerated = false,
                expectedDuplicates = 0,
                actualDuplicates = metrics.duplicateMethodCount,
                correctlyDetected = metrics.duplicateMethodCount == 0
            )
        }
        
        return calculateAccuracyMetrics(aiResults + humanResults)
    }
    
    private fun generateAIGeneratedTestFiles(): List<TestFile> {
        return listOf(
            TestFile(
                name = "AIBoilerplate.java",
                content = dataGenerator.generateAIGeneratedSlop(),
                expectedDuplicates = 5,
                complexity = 8
            ),
            TestFile(
                name = "AIDuplicateMethods.java",
                content = generateDuplicateMethods(),
                expectedDuplicates = 3,
                complexity = 5
            )
        )
    }
    
    private fun generateHumanWrittenTestFiles(): List<TestFile> {
        return listOf(
            TestFile(
                name = "HumanWritten.java",
                content = generateWellStructuredCode(),
                expectedDuplicates = 0,
                complexity = 3
            ),
            TestFile(
                name = "HumanUtility.java",
                content = generateUtilityCode(),
                expectedDuplicates = 0,
                complexity = 2
            )
        )
    }
    
    private fun calculateAccuracyMetrics(results: List<AccuracyResult>): AccuracyResults {
        val aiResults = results.filter { it.isAIGenerated }
        val humanResults = results.filter { !it.isAIGenerated }
        
        val truePositives = aiResults.count { it.correctlyDetected }
        val falseNegatives = aiResults.count { !it.correctlyDetected }
        val trueNegatives = humanResults.count { it.correctlyDetected }
        val falsePositives = humanResults.count { !it.correctlyDetected }
        
        val accuracy = (truePositives + trueNegatives).toDouble() / results.size
        val precision = if (truePositives + falsePositives > 0) {
            truePositives.toDouble() / (truePositives + falsePositives)
        } else 0.0
        val recall = if (truePositives + falseNegatives > 0) {
            truePositives.toDouble() / (truePositives + falseNegatives)
        } else 0.0
        val f1Score = if (precision + recall > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0.0
        
        return AccuracyResults(
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            truePositives = truePositives,
            falsePositives = falsePositives,
            trueNegatives = trueNegatives,
            falseNegatives = falseNegatives,
            totalSamples = results.size
        )
    }
}

data class AccuracyResult(
    val fileName: String,
    val isAIGenerated: Boolean,
    val expectedDuplicates: Int,
    val actualDuplicates: Int,
    val correctlyDetected: Boolean
)

data class AccuracyResults(
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val truePositives: Int,
    val falsePositives: Int,
    val trueNegatives: Int,
    val falseNegatives: Int,
    val totalSamples: Int
)
```

## Performance Targets

### 1. Processing Time Targets
```kotlin
data class PerformanceTargets(
    val maxSingleFileTimeMs: Long = 50,
    val maxBatchAvgTimeMs: Double = 45.0,
    val maxMemoryUsageMB: Long = 50,
    val minThroughputFilesPerSecond: Double = 20.0,
    val maxTimeScalingFactor: Double = 2.0,
    val maxMemoryScalingFactor: Double = 3.0
)

class PerformanceTargetValidator {
    fun validatePerformance(results: BenchmarkResults, targets: PerformanceTargets): ValidationReport {
        val issues = mutableListOf<String>()
        
        if (results.summary.maxProcessingTimeMs > targets.maxSingleFileTimeMs) {
            issues.add("Max processing time ${results.summary.maxProcessingTimeMs}ms exceeds target ${targets.maxSingleFileTimeMs}ms")
        }
        
        if (results.summary.avgMemoryUsedMB > targets.maxMemoryUsageMB) {
            issues.add("Avg memory usage ${results.summary.avgMemoryUsedMB}MB exceeds target ${targets.maxMemoryUsageMB}MB")
        }
        
        return ValidationReport(
            meetsTargets = issues.isEmpty(),
            issues = issues,
            results = results,
            targets = targets
        )
    }
}

data class ValidationReport(
    val meetsTargets: Boolean,
    val issues: List<String>,
    val results: BenchmarkResults,
    val targets: PerformanceTargets
)
```

### 2. Accuracy Targets
```kotlin
data class AccuracyTargets(
    val minAccuracy: Double = 0.85,
    val minPrecision: Double = 0.80,
    val minRecall: Double = 0.80,
    val minF1Score: Double = 0.82,
    val maxFalsePositiveRate: Double = 0.10,
    val maxFalseNegativeRate: Double = 0.15
)

class AccuracyTargetValidator {
    fun validateAccuracy(results: AccuracyResults, targets: AccuracyTargets): AccuracyValidationReport {
        val issues = mutableListOf<String>()
        
        if (results.accuracy < targets.minAccuracy) {
            issues.add("Accuracy ${results.accuracy} below target ${targets.minAccuracy}")
        }
        
        if (results.precision < targets.minPrecision) {
            issues.add("Precision ${results.precision} below target ${targets.minPrecision}")
        }
        
        if (results.recall < targets.minRecall) {
            issues.add("Recall ${results.recall} below target ${targets.minRecall}")
        }
        
        val falsePositiveRate = results.falsePositives.toDouble() / (results.falsePositives + results.trueNegatives)
        if (falsePositiveRate > targets.maxFalsePositiveRate) {
            issues.add("False positive rate $falsePositiveRate above target ${targets.maxFalsePositiveRate}")
        }
        
        return AccuracyValidationReport(
            meetsTargets = issues.isEmpty(),
            issues = issues,
            results = results,
            targets = targets
        )
    }
}

data class AccuracyValidationReport(
    val meetsTargets: Boolean,
    val issues: List<String>,
    val results: AccuracyResults,
    val targets: AccuracyTargets
)
```

## Benchmark Execution Framework

### 1. Benchmark Runner
```kotlin
class BenchmarkRunner {
    private val singleFileBenchmark = SingleFileBenchmark()
    private val batchProcessingBenchmark = BatchProcessingBenchmark()
    private val accuracyBenchmark = AccuracyBenchmark()
    private val performanceTargets = PerformanceTargets()
    private val accuracyTargets = AccuracyTargets()
    
    fun runFullBenchmarkSuite(): ComprehensiveBenchmarkReport {
        println("Starting comprehensive benchmark suite...")
        
        // Performance benchmarks
        val singleFileResults = singleFileBenchmark.benchmarkSingleFileAnalysis()
        val batchResults = batchProcessingBenchmark.benchmarkBatchProcessing()
        
        // Accuracy benchmarks
        val accuracyResults = accuracyBenchmark.benchmarkDetectionAccuracy()
        
        // Validate against targets
        val performanceValidation = PerformanceTargetValidator()
            .validatePerformance(singleFileResults, performanceTargets)
        val accuracyValidation = AccuracyTargetValidator()
            .validateAccuracy(accuracyResults, accuracyTargets)
        
        return ComprehensiveBenchmarkReport(
            performanceResults = singleFileResults,
            batchResults = batchResults,
            accuracyResults = accuracyResults,
            performanceValidation = performanceValidation,
            accuracyValidation = accuracyValidation,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class ComprehensiveBenchmarkReport(
    val performanceResults: BenchmarkResults,
    val batchResults: BatchBenchmarkResults,
    val accuracyResults: AccuracyResults,
    val performanceValidation: ValidationReport,
    val accuracyValidation: AccuracyValidationReport,
    val timestamp: Long
) {
    fun generateReport(): String {
        return buildString {
            appendLine("# Comprehensive Benchmark Report")
            appendLine("Generated: ${Date(timestamp)}")
            appendLine()
            
            appendLine("## Performance Results")
            appendLine("- Avg Processing Time: ${"%.2f".format(performanceResults.summary.avgProcessingTimeMs)}ms")
            appendLine("- Max Processing Time: ${performanceResults.summary.maxProcessingTimeMs}ms")
            appendLine("- Avg Memory Usage: ${"%.2f".format(performanceResults.summary.avgMemoryUsedMB)}MB")
            appendLine("- Performance Targets Met: ${performanceValidation.meetsTargets}")
            appendLine()
            
            appendLine("## Accuracy Results")
            appendLine("- Accuracy: ${"%.2f".format(accuracyResults.accuracy * 100)}%")
            appendLine("- Precision: ${"%.2f".format(accuracyResults.precision * 100)}%")
            appendLine("- Recall: ${"%.2f".format(accuracyResults.recall * 100)}%")
            appendLine("- F1 Score: ${"%.2f".format(accuracyResults.f1Score * 100)}%")
            appendLine("- Accuracy Targets Met: ${accuracyValidation.meetsTargets}")
            appendLine()
            
            if (!performanceValidation.meetsTargets) {
                appendLine("## Performance Issues")
                performanceValidation.issues.forEach { appendLine("- $it") }
                appendLine()
            }
            
            if (!accuracyValidation.meetsTargets) {
                appendLine("## Accuracy Issues")
                accuracyValidation.issues.forEach { appendLine("- $it") }
                appendLine()
            }
        }
    }
}
```

### 2. Continuous Benchmarking
```kotlin
class ContinuousBenchmarkScheduler {
    private val benchmarkRunner = BenchmarkRunner()
    private val timer = Timer()
    
    fun scheduleContinuousBenchmarks(intervalHours: Int = 24) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    val report = benchmarkRunner.runFullBenchmarkSuite()
                    saveBenchmarkReport(report)
                    checkForPerformanceRegression(report)
                } catch (e: Exception) {
                    logger.error("Continuous benchmark failed", e)
                }
            }
        }, 0L, intervalHours * 60 * 60 * 1000L)
    }
    
    private fun saveBenchmarkReport(report: ComprehensiveBenchmarkReport) {
        val reportDir = Paths.get("benchmark-reports")
        Files.createDirectories(reportDir)
        
        val reportFile = reportDir.resolve("benchmark-${report.timestamp}.json")
        val objectMapper = ObjectMapper()
        
        Files.writeString(reportFile, objectMapper.writeValueAsString(report))
    }
    
    private fun checkForPerformanceRegression(report: ComprehensiveBenchmarkReport) {
        val previousReport = loadLatestBenchmarkReport()
        
        if (previousReport != null) {
            val performanceRegression = detectPerformanceRegression(previousReport, report)
            val accuracyRegression = detectAccuracyRegression(previousReport, report)
            
            if (performanceRegression || accuracyRegression) {
                sendRegressionAlert(report, performanceRegression, accuracyRegression)
            }
        }
    }
    
    private fun detectPerformanceRegression(
        previous: ComprehensiveBenchmarkReport,
        current: ComprehensiveBenchmarkReport
    ): Boolean {
        val previousAvgTime = previous.performanceResults.summary.avgProcessingTimeMs
        val currentAvgTime = current.performanceResults.summary.avgProcessingTimeMs
        
        return currentAvgTime > previousAvgTime * 1.2 // 20% degradation threshold
    }
    
    private fun detectAccuracyRegression(
        previous: ComprehensiveBenchmarkReport,
        current: ComprehensiveBenchmarkReport
    ): Boolean {
        return current.accuracyResults.accuracy < previous.accuracyResults.accuracy * 0.9 // 10% degradation threshold
    }
    
    private fun sendRegressionAlert(
        report: ComprehensiveBenchmarkReport,
        performanceRegression: Boolean,
        accuracyRegression: Boolean
    ) {
        val alertMessage = buildString {
            appendLine("🚨 Performance Regression Detected!")
            appendLine("Performance Regression: $performanceRegression")
            appendLine("Accuracy Regression: $accuracyRegression")
            appendLine("Report: ${report.timestamp}")
        }
        
        logger.error(alertMessage)
        // Send to monitoring system, Slack, etc.
    }
}
```

This benchmarking guide provides comprehensive performance testing procedures to ensure the enhanced treesitter fuzzy detection system meets all performance and accuracy targets.

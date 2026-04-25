package com.aicodequalityrisk.plugin.analysis

class AdaptiveThresholdCalculator {
    private val baseThreshold = 0.62
    private val projectBaseline = mutableMapOf<String, Double>()

    fun calculateThreshold(
        fingerprint1: EnhancedMethodFingerprint,
        fingerprint2: EnhancedMethodFingerprint,
        filePath: String?
    ): Double {
        val lengthAdjustment = calculateLengthAdjustment(fingerprint1, fingerprint2)
        val complexityAdjustment = calculateComplexityAdjustment(fingerprint1, fingerprint2)
        val projectAdjustment = calculateProjectAdjustment(filePath)

        val adaptiveThreshold = baseThreshold + lengthAdjustment + complexityAdjustment + projectAdjustment

        return adaptiveThreshold.coerceIn(0.4, 0.85)
    }

    private fun calculateLengthAdjustment(
        fp1: EnhancedMethodFingerprint,
        fp2: EnhancedMethodFingerprint
    ): Double {
        val avgLength = (fp1.methodLength + fp2.methodLength) / 2.0

        return when {
            avgLength < 10 -> -0.15
            avgLength > 50 -> +0.10
            else -> 0.0
        }
    }

    private fun calculateComplexityAdjustment(
        fp1: EnhancedMethodFingerprint,
        fp2: EnhancedMethodFingerprint
    ): Double {
        val avgComplexity = (fp1.complexity + fp2.complexity) / 2.0

        return when {
            avgComplexity < 3 -> -0.10
            avgComplexity > 10 -> +0.15
            else -> 0.0
        }
    }

    private fun calculateProjectAdjustment(filePath: String?): Double {
        val projectKey = extractProjectKey(filePath)
        return projectBaseline.getOrDefault(projectKey, 0.0)
    }

    private fun extractProjectKey(filePath: String?): String {
        return filePath?.substringBeforeLast("/")?.substringBeforeLast("/") ?: "default"
    }

    fun updateProjectBaseline(projectKey: String, baseline: Double) {
        projectBaseline[projectKey] = baseline
    }
}

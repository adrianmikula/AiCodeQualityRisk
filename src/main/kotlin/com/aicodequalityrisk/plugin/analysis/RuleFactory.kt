package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput

class RuleFactory {

    fun createRule(config: RuleConfig): Rule {
        val matchesFunction = createMatchesFunction(config.pattern)
        val category = parseCategory(config.category)
        val finding = config.finding.toFinding().copy(category = category)

        return Rule(matchesFunction, config.scoreDelta, category, finding, config.pattern)
    }

    private fun createMatchesFunction(pattern: PatternConfig): (AnalysisInput) -> Boolean {
        when (pattern.type) {
            "contains" -> {
                val target = pattern.target ?: throw IllegalArgumentException("Contains pattern missing 'target'")
                val value = pattern.value ?: throw IllegalArgumentException("Contains pattern missing 'value'")
                return { input: AnalysisInput ->
                    val text = getTargetText(input, target)
                    text.contains(value)
                }
            }
            "regex" -> {
                val target = pattern.target ?: throw IllegalArgumentException("Regex pattern missing 'target'")
                val value = pattern.value ?: throw IllegalArgumentException("Regex pattern missing 'value'")
                val regex = Regex(value)
                return { input: AnalysisInput ->
                    val text = getTargetText(input, target)
                    regex.containsMatchIn(text)
                }
            }
            "complex" -> {
                val conditions = pattern.conditions ?: throw IllegalArgumentException("Complex pattern missing 'conditions'")
                return { input: AnalysisInput ->
                    conditions.any { condition -> evaluateCondition(input, condition) }
                }
            }
            else -> throw IllegalArgumentException("Unknown pattern type: ${pattern.type}")
        }
    }

    private fun evaluateCondition(input: AnalysisInput, condition: ConditionConfig): Boolean {
        return when (condition.type) {
            "contains" -> {
                val target = condition.target
                val value = condition.value?.toString() ?: throw IllegalArgumentException("Contains condition missing 'value'")
                val text = getTargetText(input, target)
                text.contains(value)
            }
            "regex" -> {
                val target = condition.target
                val value = condition.value?.toString() ?: throw IllegalArgumentException("Regex condition missing 'value'")
                val regex = Regex(value)
                val text = getTargetText(input, target)
                regex.containsMatchIn(text)
            }
            "length" -> {
                val target = condition.target
                val operator = condition.operator ?: throw IllegalArgumentException("Length condition missing 'operator'")
                val value = when (condition.value) {
                    is Int -> condition.value
                    is String -> condition.value.toIntOrNull()
                    else -> null
                } ?: throw IllegalArgumentException("Length condition missing valid 'value'")
                val text = getTargetText(input, target)
                val length = text.length
                when (operator) {
                    ">" -> length > value
                    "<" -> length < value
                    ">=" -> length >= value
                    "<=" -> length <= value
                    "==" -> length == value
                    "!=" -> length != value
                    else -> throw IllegalArgumentException("Unknown length operator: $operator")
                }
            }
            "numeric" -> {
                val target = condition.target
                val operator = condition.operator ?: throw IllegalArgumentException("Numeric condition missing 'operator'")
                val targetValue = getTargetValue(input, target)
                val conditionValue = when (condition.value) {
                    is Int -> condition.value.toDouble()
                    is Double -> condition.value
                    is String -> condition.value.toDoubleOrNull()
                    else -> null
                } ?: throw IllegalArgumentException("Numeric condition missing valid 'value'")

                val numericValue = when (targetValue) {
                    is Int -> targetValue.toDouble()
                    is Double -> targetValue
                    else -> throw IllegalArgumentException("Target $target is not numeric")
                }

                when (operator) {
                    ">" -> numericValue > conditionValue
                    "<" -> numericValue < conditionValue
                    ">=" -> numericValue >= conditionValue
                    "<=" -> numericValue <= conditionValue
                    "==" -> numericValue == conditionValue
                    "!=" -> numericValue != conditionValue
                    else -> throw IllegalArgumentException("Unknown numeric operator: $operator")
                }
            }
            "boolean" -> {
                val target = condition.target
                val targetValue = getTargetValue(input, target)
                val expectedValue = condition.value as? Boolean
                    ?: throw IllegalArgumentException("Boolean condition missing valid 'value'")

                when (targetValue) {
                    is Boolean -> targetValue == expectedValue
                    else -> throw IllegalArgumentException("Target $target is not boolean")
                }
            }
            else -> throw IllegalArgumentException("Unknown condition type: ${condition.type}")
        }
    }

    private fun getTargetText(input: AnalysisInput, target: String): String {
        return when (target) {
            "diffText" -> input.diffText
            "fileSnapshot" -> input.fileSnapshot
            else -> throw IllegalArgumentException("Unknown target: $target")
        }
    }

    private fun getTargetValue(input: AnalysisInput, target: String): Any {
        return when (target) {
            "diffText" -> input.diffText
            "fileSnapshot" -> input.fileSnapshot
            "ast.methodCount" -> input.astMetrics.methodCount
            "ast.maxMethodLength" -> input.astMetrics.maxMethodLength
            "ast.averageMethodLength" -> input.astMetrics.averageMethodLength
            "ast.maxNestingDepth" -> input.astMetrics.maxNestingDepth
            "ast.cyclomaticComplexity" -> input.astMetrics.cyclomaticComplexity
            "ast.classCount" -> input.astMetrics.classCount
            "ast.fieldCount" -> input.astMetrics.fieldCount
            "ast.maxParameterCount" -> input.astMetrics.maxParameterCount
            "ast.stringLiteralCount" -> input.astMetrics.stringLiteralCount
            "ast.duplicateStringLiteralCount" -> input.astMetrics.duplicateStringLiteralCount
            "ast.hardcodedConfigLiteralCount" -> input.astMetrics.hardcodedConfigLiteralCount
            "ast.magicNumberCount" -> input.astMetrics.magicNumberCount
            "ast.hasComplexMethods" -> input.astMetrics.hasComplexMethods
            "ast.hasDeepNesting" -> input.astMetrics.hasDeepNesting
            "ast.hasHighComplexity" -> input.astMetrics.hasHighComplexity
            "ast.hasHardcodedConfig" -> input.astMetrics.hasHardcodedConfig
            "ast.hasMagicNumbers" -> input.astMetrics.hasMagicNumbers
            "ast.hasLongParameterList" -> input.astMetrics.hasLongParameterList
            "ast.duplicateNumberLiteralCount" -> input.astMetrics.duplicateNumberLiteralCount
            "ast.duplicateMethodCallCount" -> input.astMetrics.duplicateMethodCallCount
            "ast.broadCatchCount" -> input.astMetrics.broadCatchCount
            "ast.emptyCatchCount" -> input.astMetrics.emptyCatchCount
            "ast.booleanOperatorCount" -> input.astMetrics.booleanOperatorCount
            "ast.maxElseIfChainLength" -> input.astMetrics.maxElseIfChainLength
            "ast.hasBroadExceptionCatch" -> input.astMetrics.hasBroadExceptionCatch
            "ast.hasEmptyCatchBlock" -> input.astMetrics.hasEmptyCatchBlock
            "ast.hasRepeatedMethodCalls" -> input.astMetrics.hasRepeatedMethodCalls
            "ast.hasHeavyBooleanLogic" -> input.astMetrics.hasHeavyBooleanLogic
            "ast.hasLongIfElseChain" -> input.astMetrics.hasLongIfElseChain
            else -> throw IllegalArgumentException("Unknown target: $target")
        }
    }

    private fun parseCategory(category: String): Category {
        return when (category.uppercase()) {
            "COMPLEXITY" -> Category.COMPLEXITY
            "DUPLICATION" -> Category.DUPLICATION
            "PERFORMANCE" -> Category.PERFORMANCE
            "SECURITY" -> Category.SECURITY
            else -> throw IllegalArgumentException("Unknown category: $category")
        }
    }
}
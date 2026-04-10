package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput

class RuleFactory {

    fun createRule(config: RuleConfig): Rule {
        val matchesFunction = createMatchesFunction(config.pattern)
        val category = parseCategory(config.category)
        val finding = config.finding.toFinding()

        return Rule(matchesFunction, config.scoreDelta, category, finding)
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
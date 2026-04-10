package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import org.yaml.snakeyaml.Yaml
import java.io.InputStream

class AnalysisConfigLoader {

    fun loadConfig(): AnalysisConfig {
        val resourcePath = "/config/analysis-rules.yaml"
        val inputStream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Configuration file not found at: $resourcePath. Available resources: ${javaClass.classLoader.getResources("").toList()}")

        return inputStream.use { stream ->
            val yaml = Yaml()
            val data = yaml.load<Map<String, Any>>(stream)
            parseConfig(data)
        }
    }

    private fun parseConfig(data: Map<String, Any>): AnalysisConfig {
        val rulesData = data["rules"] as? List<Map<String, Any>>
            ?: throw IllegalArgumentException("Missing 'rules' section in config")

        val rules = rulesData.map { parseRule(it) }
        return AnalysisConfig(rules)
    }

    private fun parseRule(ruleData: Map<String, Any>): RuleConfig {
        val name = ruleData["name"] as? String ?: throw IllegalArgumentException("Rule missing 'name'")
        val patternData = ruleData["pattern"] as? Map<String, Any> ?: throw IllegalArgumentException("Rule '$name' missing 'pattern'")
        val scoreDelta = ruleData["scoreDelta"] as? Int ?: throw IllegalArgumentException("Rule '$name' missing 'scoreDelta'")
        val category = ruleData["category"] as? String ?: throw IllegalArgumentException("Rule '$name' missing 'category'")
        val findingData = ruleData["finding"] as? Map<String, Any> ?: throw IllegalArgumentException("Rule '$name' missing 'finding'")

        val pattern = parsePattern(patternData)
        val finding = parseFinding(findingData)

        return RuleConfig(name, pattern, scoreDelta, category, finding)
    }

    private fun parsePattern(patternData: Map<String, Any>): PatternConfig {
        val type = patternData["type"] as? String ?: throw IllegalArgumentException("Pattern missing 'type'")
        val target = patternData["target"] as? String
        val value = patternData["value"] as? String
        val conditionsData = patternData["conditions"] as? List<Map<String, Any>>

        val conditions = conditionsData?.map { parseCondition(it) }

        return PatternConfig(type, target, value, conditions)
    }

    private fun parseCondition(conditionData: Map<String, Any>): ConditionConfig {
        val type = conditionData["type"] as? String ?: throw IllegalArgumentException("Condition missing 'type'")
        val target = conditionData["target"] as? String ?: throw IllegalArgumentException("Condition missing 'target'")
        val value = conditionData["value"]
        val operator = conditionData["operator"] as? String

        return ConditionConfig(type, target, value, operator)
    }

    private fun parseFinding(findingData: Map<String, Any>): FindingConfig {
        val title = findingData["title"] as? String ?: throw IllegalArgumentException("Finding missing 'title'")
        val detail = findingData["detail"] as? String ?: throw IllegalArgumentException("Finding missing 'detail'")
        val severity = findingData["severity"] as? String ?: throw IllegalArgumentException("Finding missing 'severity'")

        return FindingConfig(title, detail, severity)
    }
}
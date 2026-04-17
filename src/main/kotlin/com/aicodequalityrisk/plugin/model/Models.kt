package com.aicodequalityrisk.plugin.model

import com.aicodequalityrisk.plugin.analysis.ASTMetrics
import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.analysis.FuzzyMetrics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AnalysisInput(
    val projectPath: String,
    val filePath: String?,
    val trigger: TriggerType,
    val diffText: String,
    val fileSnapshot: String,
    val astMetrics: ASTMetrics = ASTMetrics(),
    val fuzzyMetrics: FuzzyMetrics = FuzzyMetrics()
)

enum class TriggerType {
    EDIT,
    SAVE,
    FOCUS,
    MANUAL
}

data class RiskResult(
    val score: Int,
    val complexityScore: Int = 0,
    val duplicationScore: Int = 0,
    val performanceScore: Int = 0,
    val securityScore: Int = 0,
    val boilerplateBloatScore: Int = 0,
    val verboseCommentSpamScore: Int = 0,
    val overDefensiveProgrammingScore: Int = 0,
    val magicNumbersScore: Int = 0,
    val complexBooleanLogicScore: Int = 0,
    val deepNestingScore: Int = 0,
    val verboseLoggingScore: Int = 0,
    val poorNamingScore: Int = 0,
    val frameworkMisuseScore: Int = 0,
    val excessiveDocumentationScore: Int = 0,
    val findings: List<Finding> = emptyList(),
    val explanations: List<String> = emptyList(),
    val sourceFilePath: String? = null
) {
    val complexityConsolidated: Int
        get() = listOf(complexityScore, deepNestingScore, complexBooleanLogicScore, overDefensiveProgrammingScore).average().toInt()

    val duplicationConsolidated: Int
        get() = listOf(duplicationScore, boilerplateBloatScore).average().toInt()

    val performanceConsolidated: Int
        get() = listOf(performanceScore, verboseLoggingScore, magicNumbersScore, poorNamingScore, frameworkMisuseScore).average().toInt()

    val securityConsolidated: Int
        get() = listOf(securityScore, verboseCommentSpamScore, excessiveDocumentationScore).average().toInt()
    companion object {
        fun fromJson(json: String): RiskResult {
            val map = parseJson(json)
            return RiskResult(
                score = (map["score"] as? Double)?.toInt() ?: 0,
                complexityScore = (map["complexityScore"] as? Double)?.toInt() ?: 0,
                duplicationScore = (map["duplicationScore"] as? Double)?.toInt() ?: 0,
                performanceScore = (map["performanceScore"] as? Double)?.toInt() ?: 0,
                securityScore = (map["securityScore"] as? Double)?.toInt() ?: 0,
                boilerplateBloatScore = (map["boilerplateBloatScore"] as? Double)?.toInt() ?: 0,
                verboseCommentSpamScore = (map["verboseCommentSpamScore"] as? Double)?.toInt() ?: 0,
                overDefensiveProgrammingScore = (map["overDefensiveProgrammingScore"] as? Double)?.toInt() ?: 0,
                magicNumbersScore = (map["magicNumbersScore"] as? Double)?.toInt() ?: 0,
                complexBooleanLogicScore = (map["complexBooleanLogicScore"] as? Double)?.toInt() ?: 0,
                deepNestingScore = (map["deepNestingScore"] as? Double)?.toInt() ?: 0,
                verboseLoggingScore = (map["verboseLoggingScore"] as? Double)?.toInt() ?: 0,
                poorNamingScore = (map["poorNamingScore"] as? Double)?.toInt() ?: 0,
                frameworkMisuseScore = (map["frameworkMisuseScore"] as? Double)?.toInt() ?: 0,
                excessiveDocumentationScore = (map["excessiveDocumentationScore"] as? Double)?.toInt() ?: 0,
                findings = (map["findings"] as? List<*>)?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { parseFinding(it) }
                } ?: emptyList(),
                explanations = (map["explanations"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                sourceFilePath = map["sourceFilePath"] as? String
            )
        }

        private fun parseFinding(map: Map<*, *>): Finding? {
            return Finding(
                title = map["title"] as? String ?: "",
                detail = map["detail"] as? String ?: "",
                severity = Severity.valueOf(map["severity"] as? String ?: "LOW"),
                category = try { com.aicodequalityrisk.plugin.analysis.Category.valueOf(map["category"] as? String ?: "COMPLEXITY") } catch (e: Exception) { com.aicodequalityrisk.plugin.analysis.Category.COMPLEXITY },
                filePath = map["filePath"] as? String,
                lineNumber = (map["lineNumber"] as? Double)?.toInt()
            )
        }

        private fun parseJson(json: String): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            var i = 0
            fun skipWhitespace() { while (i < json.length && json[i].isWhitespace()) i++ }
            fun parseValue(): Any {
                skipWhitespace()
                when {
                    json[i] == '{' -> {
                        val map = mutableMapOf<String, Any>()
                        i++
                        skipWhitespace()
                        while (i < json.length && json[i] != '}') {
                            skipWhitespace()
                            if (json[i] == '"') {
                                i++
                                val key = buildString {
                                    while (i < json.length && json[i] != '"') {
                                        if (json[i] == '\\') { i++; if (i < json.length) append(json[i]) }
                                        else append(json[i])
                                        i++
                                    }
                                    if (i < json.length) i++
                                }
                                skipWhitespace()
                                if (i < json.length && json[i] == ':') i++
                                val value = parseValue()
                                map[key] = value
                                skipWhitespace()
                                if (i < json.length && json[i] == ',') i++
                            } else i++
                        }
                        if (i < json.length) i++
                        return result.apply { for ((k, v) in map) put(k, v) }.also { result.clear(); result.putAll(it as Map<String, Any>) }
                    }
                    json[i] == '[' -> {
                        val list = mutableListOf<Any>()
                        i++
                        skipWhitespace()
                        while (i < json.length && json[i] != ']') {
                            list.add(parseValue())
                            skipWhitespace()
                            if (i < json.length && json[i] == ',') i++
                        }
                        if (i < json.length) i++
                        return list
                    }
                    json[i] == '"' -> {
                        i++
                        return buildString {
                            while (i < json.length && json[i] != '"') {
                                if (json[i] == '\\') { i++; if (i < json.length) append(json[i]) }
                                else append(json[i])
                                i++
                            }
                            if (i < json.length) i++
                        }
                    }
                    json[i] == 't' && json.substring(i, i + 4) == "true" -> { i += 4; return true }
                    json[i] == 'f' && json.substring(i, i + 5) == "false" -> { i += 5; return false }
                    json[i] == 'n' && json.substring(i, i + 4) == "null" -> { i += 4; return "null" }
                    else -> {
                        val num = buildString {
                            while (i < json.length && (json[i].isDigit() || json[i] == '.' || json[i] == '-')) append(json[i++])
                        }
                        return num.toDoubleOrNull() ?: num
                    }
                }
            }
            return (parseValue() as? Map<String, Any>) ?: emptyMap()
        }
    }

    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"score\":${score},")
        sb.append("\"complexityScore\":${complexityScore},")
        sb.append("\"duplicationScore\":${duplicationScore},")
        sb.append("\"performanceScore\":${performanceScore},")
        sb.append("\"securityScore\":${securityScore},")
        sb.append("\"boilerplateBloatScore\":${boilerplateBloatScore},")
        sb.append("\"verboseCommentSpamScore\":${verboseCommentSpamScore},")
        sb.append("\"overDefensiveProgrammingScore\":${overDefensiveProgrammingScore},")
        sb.append("\"magicNumbersScore\":${magicNumbersScore},")
        sb.append("\"complexBooleanLogicScore\":${complexBooleanLogicScore},")
        sb.append("\"deepNestingScore\":${deepNestingScore},")
        sb.append("\"verboseLoggingScore\":${verboseLoggingScore},")
        sb.append("\"poorNamingScore\":${poorNamingScore},")
        sb.append("\"frameworkMisuseScore\":${frameworkMisuseScore},")
        sb.append("\"excessiveDocumentationScore\":${excessiveDocumentationScore},")
        sb.append("\"findings\":[")
        sb.append(findings.joinToString(",") { it.toJson() })
        sb.append("],")
        sb.append("\"explanations\":[")
        sb.append(explanations.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" })
        sb.append("],")
        sb.append("\"sourceFilePath\":")
        sb.append(if (sourceFilePath != null) "\"${sourceFilePath?.replace("\"", "\\\"")}\"" else "null")
        sb.append("}")
        return sb.toString()
    }
}

data class Finding(
    val title: String,
    val detail: String,
    val severity: Severity,
    val category: Category = Category.COMPLEXITY,
    val filePath: String? = null,
    val lineNumber: Int? = null
) {
    fun toJson(): String {
        return """{"title":"${title.replace("\"", "\\\"")}","detail":"${detail.replace("\"", "\\\"")}","severity":"${severity.name}","category":"${category.name}","filePath":${filePath?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"},"lineNumber":${lineNumber}}"""
    }
}

enum class Severity {
    LOW,
    MEDIUM,
    HIGH
}

sealed class AnalysisViewState {
    data object Idle : AnalysisViewState()
    data object Loading : AnalysisViewState()
    data class Ready(val result: RiskResult) : AnalysisViewState()
    data class Error(val message: String) : AnalysisViewState()

    companion object {
        fun toJson(result: RiskResult): String = result.toJson()

        fun fromJson(json: String): RiskResult? {
            return try {
                RiskResult.fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }
}

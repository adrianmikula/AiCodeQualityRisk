package com.aicodequalityrisk.plugin.mcp

import com.aicodequalityrisk.plugin.analysis.Category
import com.aicodequalityrisk.plugin.model.Finding
import com.aicodequalityrisk.plugin.model.RiskResult
import com.aicodequalityrisk.plugin.model.Severity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpServerServiceTest {

    private fun createTestRiskResult(
        score: Int = 45,
        filePath: String? = "src/main/java/Test.java"
    ): RiskResult = RiskResult(
        score = score,
        complexityScore = 30,
        duplicationScore = 15,
        performanceScore = 5,
        securityScore = 0,
        boilerplateBloatScore = 10,
        verboseCommentSpamScore = 5,
        overDefensiveProgrammingScore = 8,
        magicNumbersScore = 12,
        complexBooleanLogicScore = 20,
        deepNestingScore = 15,
        verboseLoggingScore = 3,
        poorNamingScore = 7,
        frameworkMisuseScore = 0,
        excessiveDocumentationScore = 5,
        findings = listOf(
            Finding(
                title = "Complex boolean logic detected",
                detail = "Found nested boolean expressions with multiple AND/OR operators",
                severity = Severity.HIGH,
                category = Category.COMPLEXITY,
                filePath = filePath,
                lineNumber = 42
            ),
            Finding(
                title = "Deep nesting detected",
                detail = "Method has more than 4 levels of nested blocks",
                severity = Severity.MEDIUM,
                category = Category.COMPLEXITY,
                filePath = filePath,
                lineNumber = 15
            ),
            Finding(
                title = "Magic number usage",
                detail = "Found hardcoded numeric literal that should be a named constant",
                severity = Severity.LOW,
                category = Category.COMPLEXITY,
                filePath = filePath,
                lineNumber = 78
            )
        ),
        explanations = listOf(
            "Risk score combines lightweight syntax heuristics and diff footprint signals.",
            "Use this score as triage guidance; prioritize HIGH severity findings first."
        ),
        sourceFilePath = filePath
    )

    @Test
    fun testRiskResultToJson() {
        val result = createTestRiskResult()
        val json = result.toJson()

        assertTrue(json.contains("\"score\":45"))
        assertTrue(json.contains("\"complexityScore\":30"))
        assertTrue(json.contains("\"findings\":["))
        assertTrue(json.contains("Complex boolean logic detected"))
    }

    @Test
    fun testRiskResultFromJson() {
        val original = createTestRiskResult()
        val json = original.toJson()
        val parsed = RiskResult.fromJson(json)

        assertNotNull(parsed)
        assertEquals(original.score, parsed.score)
        assertEquals(original.complexityScore, parsed.complexityScore)
        assertEquals(original.findings.size, parsed.findings.size)
        assertEquals("Complex boolean logic detected", parsed.findings[0].title)
    }

    @Test
    fun testFindingToJson() {
        val finding = Finding(
            title = "Test Finding",
            detail = "Test detail message",
            severity = Severity.HIGH,
            category = Category.SECURITY,
            filePath = "src/Test.java",
            lineNumber = 100
        )

        val json = finding.toJson()

        assertTrue(json.contains("\"title\":\"Test Finding\""))
        assertTrue(json.contains("\"severity\":\"HIGH\""))
        assertTrue(json.contains("\"category\":\"SECURITY\""))
        assertTrue(json.contains("\"lineNumber\":100"))
    }

    @Test
    fun testRoundTripWithAllFields() {
        val result = RiskResult(
            score = 72,
            complexityScore = 25,
            duplicationScore = 20,
            performanceScore = 10,
            securityScore = 5,
            boilerplateBloatScore = 8,
            verboseCommentSpamScore = 4,
            overDefensiveProgrammingScore = 6,
            magicNumbersScore = 9,
            complexBooleanLogicScore = 11,
            deepNestingScore = 7,
            verboseLoggingScore = 3,
            poorNamingScore = 5,
            frameworkMisuseScore = 2,
            excessiveDocumentationScore = 3,
            findings = listOf(
                Finding(
                    title = "Security issue",
                    detail = "Found potential SQL injection vulnerability",
                    severity = Severity.HIGH,
                    category = Category.SECURITY,
                    filePath = "src/Auth.java",
                    lineNumber = 45
                )
            ),
            explanations = listOf("Test explanation"),
            sourceFilePath = "src/Auth.java"
        )

        val json = result.toJson()
        val parsed = RiskResult.fromJson(json)

        assertEquals(72, parsed.score)
        assertEquals(25, parsed.complexityScore)
        assertEquals(1, parsed.findings.size)
        assertEquals(Severity.HIGH, parsed.findings[0].severity)
        assertEquals(Category.SECURITY, parsed.findings[0].category)
    }

    @Test
    fun testEmptyFindings() {
        val result = RiskResult(
            score = 10,
            findings = emptyList(),
            sourceFilePath = "src/Empty.java"
        )

        val json = result.toJson()
        val parsed = RiskResult.fromJson(json)

        assertEquals(10, parsed.score)
        assertTrue(parsed.findings.isEmpty())
    }

    @Test
    fun testNullSourceFilePath() {
        val result = RiskResult(
            score = 50,
            findings = emptyList(),
            sourceFilePath = null
        )

        val json = result.toJson()
        val parsed = RiskResult.fromJson(json)

        assertEquals(50, parsed.score)
        assertEquals(null, parsed.sourceFilePath)
    }

    @Test
    fun testAllCategoryScores() {
        val result = RiskResult(
            score = 85,
            complexityScore = 20,
            duplicationScore = 15,
            performanceScore = 10,
            securityScore = 5,
            boilerplateBloatScore = 8,
            verboseCommentSpamScore = 4,
            overDefensiveProgrammingScore = 3,
            magicNumbersScore = 5,
            complexBooleanLogicScore = 4,
            deepNestingScore = 3,
            verboseLoggingScore = 2,
            poorNamingScore = 2,
            frameworkMisuseScore = 2,
            excessiveDocumentationScore = 2,
            findings = emptyList(),
            sourceFilePath = "Test.java"
        )

        val json = result.toJson()
        val parsed = RiskResult.fromJson(json)

        assertEquals(85, parsed.score)
        assertEquals(20, parsed.complexityScore)
        assertEquals(15, parsed.duplicationScore)
        assertEquals(10, parsed.performanceScore)
        assertEquals(5, parsed.securityScore)
        assertEquals(8, parsed.boilerplateBloatScore)
    }

    @Test
    fun testMultipleFindingsRoundTrip() {
        val findings = (1..5).map { i ->
            Finding(
                title = "Finding $i",
                detail = "Detail for finding $i",
                severity = Severity.entries[i % 3],
                category = Category.entries[i % 4],
                filePath = "Test$i.kt",
                lineNumber = i * 10
            )
        }

        val result = RiskResult(
            score = 60,
            findings = findings,
            sourceFilePath = "Test.java"
        )

        val json = result.toJson()
        val parsed = RiskResult.fromJson(json)

        assertEquals(5, parsed.findings.size)
        assertEquals("Finding 1", parsed.findings[0].title)
        assertEquals("Finding 5", parsed.findings[4].title)
    }

    @Test
    fun testConsolidatedComplexityScore() {
        val result = RiskResult(
            score = 0,
            complexityScore = 40,
            deepNestingScore = 20,
            complexBooleanLogicScore = 60,
            overDefensiveProgrammingScore = 80
        )
        assertEquals(50, result.complexityConsolidated)
    }

    @Test
    fun testConsolidatedDuplicationScore() {
        val result = RiskResult(
            score = 0,
            duplicationScore = 30,
            boilerplateBloatScore = 50
        )
        assertEquals(40, result.duplicationConsolidated)
    }

    @Test
    fun testConsolidatedPerformanceScore() {
        val result = RiskResult(
            score = 0,
            performanceScore = 20,
            verboseLoggingScore = 40,
            magicNumbersScore = 60,
            poorNamingScore = 80,
            frameworkMisuseScore = 100
        )
        assertEquals(60, result.performanceConsolidated)
    }

    @Test
    fun testConsolidatedSecurityScore() {
        val result = RiskResult(
            score = 0,
            securityScore = 25,
            verboseCommentSpamScore = 50,
            excessiveDocumentationScore = 75
        )
        assertEquals(50, result.securityConsolidated)
    }

    @Test
    fun testConsolidatedScoresWithDefaultValues() {
        val result = RiskResult(score = 50)
        assertEquals(0, result.complexityConsolidated)
        assertEquals(0, result.duplicationConsolidated)
        assertEquals(0, result.performanceConsolidated)
        assertEquals(0, result.securityConsolidated)
    }

    @Test
    fun testConsolidatedScoresInToJson() {
        val result = RiskResult(
            score = 0,
            complexityScore = 30,
            deepNestingScore = 30,
            complexBooleanLogicScore = 30,
            overDefensiveProgrammingScore = 30
        )
        assertEquals(30, result.complexityConsolidated)
        val json = result.toJson()
        assertTrue(json.contains("complexityScore\":30"))
        assertTrue(json.contains("deepNestingScore\":30"))
    }
}
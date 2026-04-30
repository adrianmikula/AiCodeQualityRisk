package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.analysis.FuzzyMetrics
import com.aicodequalityrisk.plugin.analysis.MethodSimilarityPair
import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.Severity
import com.aicodequalityrisk.plugin.model.TriggerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalMockAnalyzerClientTest {
    private val client = LocalMockAnalyzerClient()

    @Test
    fun `non-null assertion should produce high-severity finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+val x = maybeNull!!",
                fileSnapshot = "val x = maybeNull!!",
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.score > 0, "Score should be greater than 0")
        assertTrue(result.findings.any { it.severity == Severity.HIGH }, "Should have at least one HIGH severity finding")
    }

    @Test
    fun `empty low-risk input should return fallback finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.MANUAL,
                diffText = "+val x = 1",
                fileSnapshot = "val x = 1",
                astMetrics = ASTMetrics()
            )
        )

        assertEquals(1, result.findings.size, "Should have 1 finding")
        assertTrue(result.findings.first().title.contains("No high-risk"), "Should have fallback finding")
    }

    @Test
    fun `multiple rules should accumulate findings`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+// TODO fix\n+if (x) { }",
                fileSnapshot = """
                    fun run() {
                        // TODO fix
                        val x = maybeNull!!
                        try { Thread.sleep(1) } catch (Exception: Exception) {}
                    }
                """.trimIndent() + "\n" + "x".repeat(2200),
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.findings.size >= 3)
        assertTrue(result.findings.any { it.severity == Severity.HIGH })
    }

    @Test
    fun `includes fuzzy duplicate method finding when fuzzy metrics are present`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+public void copyA() {}\n+public void copyB() {}",
                fileSnapshot = "public class Example { public void copyA() {} public void copyB() {} }",
                astMetrics = ASTMetrics(),
                fuzzyMetrics = FuzzyMetrics(
                    duplicateMethodCount = 1,
                    maxSimilarityScore = 0.72,
                    duplicateMethodPairs = listOf(MethodSimilarityPair("copyA", "copyB", 0.72))
                )
            )
        )

        assertTrue(result.findings.any { it.title.contains("Possible duplicated logic detected") })
        assertTrue(result.findings.any { it.category == Category.DUPLICATION })
    }

    @Test
    fun `magic numbers score should be positive when magic numbers exist in code`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+val x = 42",
                fileSnapshot = "val x = 42",
                astMetrics = ASTMetrics(magicNumberCount = 3, hasMagicNumbers = true)
            )
        )

        assertTrue(result.magicNumbersScore > 0, "magicNumbersScore should be > 0 when magicNumberCount = 3")
    }

    @Test
    fun `deep nesting score should be positive when deep nesting exists`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+if (a) { if (b) { if (c) { if (d) {} } } }",
                fileSnapshot = "if (a) { if (b) { if (c) { if (d) {} } } }",
                astMetrics = ASTMetrics(maxNestingDepth = 5, hasDeepNesting = true)
            )
        )

        assertTrue(result.deepNestingScore > 0, "deepNestingScore should be > 0 when maxNestingDepth = 5")
    }

    @Test
    fun `complex boolean logic score should be positive when boolean logic is complex`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+if (a && b || c && d)",
                fileSnapshot = "if (a && b || c && d)",
                astMetrics = ASTMetrics(booleanOperatorCount = 6, hasHeavyBooleanLogic = true, maxElseIfChainLength = 3)
            )
        )

        assertTrue(result.complexBooleanLogicScore > 0, "complexBooleanLogicScore should be > 0 with heavy boolean logic")
    }

    @Test
    fun `over defensive programming score should be positive when repeated method calls exist`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+obj.getA().getB().getC()",
                fileSnapshot = "obj.getA().getB().getC()",
                astMetrics = ASTMetrics(hasRepeatedMethodCalls = true, duplicateMethodCallCount = 5)
            )
        )

        assertTrue(result.overDefensiveProgrammingScore > 0, "overDefensiveProgrammingScore should be > 0 when repeated calls exist")
    }

    @Test
    fun `boilerplate bloat score should be positive when methods are long`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+fun run() { ... }",
                fileSnapshot = "fun run() { ... }",
                astMetrics = ASTMetrics(averageMethodLength = 80.0, maxMethodLength = 150, duplicateStringLiteralCount = 8)
            )
        )

        assertTrue(result.boilerplateBloatScore > 0, "boilerplateBloatScore should be > 0 when methods are long")
    }

    @Test
    fun `framework misuse score should be positive when broad exception catch exists`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+catch (Exception e) { }",
                fileSnapshot = "catch (Exception e) { }",
                astMetrics = ASTMetrics(hasBroadExceptionCatch = true, broadCatchCount = 2, hasEmptyCatchBlock = true, emptyCatchCount = 1)
            )
        )

        assertTrue(result.frameworkMisuseScore > 0, "frameworkMisuseScore should be > 0 when broad catch exists")
    }

    @Test
    fun `verbose logging score should be positive when many string literals exist`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+log.info(\"debug\")",
                fileSnapshot = "log.info(\"debug\")",
                astMetrics = ASTMetrics(stringLiteralCount = 25)
            )
        )

        assertTrue(result.verboseLoggingScore > 0, "verboseLoggingScore should be > 0 when many string literals exist")
    }

    @Test
    fun `poor naming score should be positive when hardcoded config exists`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+val url = \"http://api.example.com\"",
                fileSnapshot = "val url = \"http://api.example.com\"",
                astMetrics = ASTMetrics(hasHardcodedConfig = true, hardcodedConfigLiteralCount = 5)
            )
        )

        assertTrue(result.poorNamingScore > 0, "poorNamingScore should be > 0 when hardcoded config exists")
    }

    @Test
    fun `deep nesting score should be zero when no deep nesting exists`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+val x = 1",
                fileSnapshot = "val x = 1",
                astMetrics = ASTMetrics(maxNestingDepth = 2, hasDeepNesting = false)
            )
        )

        assertEquals(0, result.deepNestingScore, "deepNestingScore should be 0 when no deep nesting exists")
    }

    @Test
    fun `framework misuse score should be zero when no exception issues exist`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+val x = 1",
                fileSnapshot = "val x = 1",
                astMetrics = ASTMetrics(hasBroadExceptionCatch = false, hasEmptyCatchBlock = false)
            )
        )

        assertEquals(0, result.frameworkMisuseScore, "frameworkMisuseScore should be 0 when no catch issues")
    }

    @Test
    fun `verbose comment spam score should be positive when many verbose comments exist`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+// comment\n+// comment\n+// comment",
                fileSnapshot = "// comment\n// comment\n// comment\nval x = 1",
                astMetrics = ASTMetrics(lineCommentCount = 25, hasVerboseComments = true)
            )
        )

        assertTrue(result.verboseCommentSpamScore > 0, "verboseCommentSpamScore should be > 0 when many verbose comments exist")
    }

    @Test
    fun `excessive documentation score should be positive when too many javadocs exist`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.kt",
                trigger = TriggerType.EDIT,
                diffText = "+/** doc */",
                fileSnapshot = "/** doc */\nfun a() {}\n/** doc */\nfun b() {}\n/** doc */\nfun c() {}",
                astMetrics = ASTMetrics(javadocCommentCount = 15, methodCount = 3, hasExcessiveComments = true)
            )
        )

        assertTrue(result.excessiveDocumentationScore > 0, "excessiveDocumentationScore should be > 0 when excessive javadocs exist")
    }

    @Test
    fun `plaintext password comparison should produce high-severity finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+if (input.equals(password)) { }",
                fileSnapshot = "if (input.equals(password)) { }",
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.findings.any { it.title.contains("Plaintext password comparison detected") }, "Should detect plaintext password comparison")
        assertTrue(result.findings.any { it.severity == Severity.HIGH }, "Should have HIGH severity")
    }

    @Test
    fun `hardcoded API token should produce high-severity finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+String apiKey = \"sk-1234567890abcdef\";",
                fileSnapshot = "String apiKey = \"sk-1234567890abcdef\";",
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.findings.any { it.title.contains("Hardcoded API token detected") }, "Should detect hardcoded API token")
        assertTrue(result.findings.any { it.severity == Severity.HIGH }, "Should have HIGH severity")
    }

    @Test
    fun `placeholder domain should produce high-severity finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+String apiUrl = \"https://example.com/api\";",
                fileSnapshot = "String apiUrl = \"https://example.com/api\";",
                astMetrics = ASTMetrics()
            )
        )

        assertTrue(result.findings.any { it.title.contains("Placeholder domain") }, "Should detect placeholder domain")
        assertTrue(result.findings.any { it.severity == Severity.HIGH }, "Should have HIGH severity")
    }

    @Test
    fun `AST plaintext password comparison should trigger security finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+if (user.password.equals(input)) { }",
                fileSnapshot = "if (user.password.equals(input)) { }",
                astMetrics = ASTMetrics(plaintextPasswordComparisonCount = 1, hasPlaintextPasswordComparison = true)
            )
        )

        assertTrue(result.securityScore > 0, "securityScore should be > 0 when plaintext password comparison detected")
        assertTrue(result.findings.any { it.title.contains("Plaintext password comparison") }, "Should detect via AST")
    }

    @Test
    fun `AST hardcoded secret should trigger security finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+String secret = \"sk-1234567890\";",
                fileSnapshot = "String secret = \"sk-1234567890\";",
                astMetrics = ASTMetrics(hardcodedSecretCount = 1, hasHardcodedSecrets = true)
            )
        )

        assertTrue(result.securityScore > 0, "securityScore should be > 0 when hardcoded secret detected")
        assertTrue(result.findings.any { it.title.contains("Hardcoded secret") }, "Should detect via AST")
    }

    @Test
    fun `AST placeholder domain should trigger security finding`() {
        val result = client.analyze(
            AnalysisInput(
                projectPath = "/tmp",
                filePath = "/tmp/Test.java",
                trigger = TriggerType.EDIT,
                diffText = "+String url = \"http://localhost:8080\";",
                fileSnapshot = "String url = \"http://localhost:8080\";",
                astMetrics = ASTMetrics(placeholderDomainCount = 1, hasPlaceholderDomains = true)
            )
        )

        assertTrue(result.securityScore > 0, "securityScore should be > 0 when placeholder domain detected")
        assertTrue(result.findings.any { it.title.contains("Placeholder domain") }, "Should detect via AST")
    }
}

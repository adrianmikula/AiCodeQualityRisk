package com.aicodequalityrisk.plugin.analysis

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.intellij.openapi.diagnostic.Logger
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.*

/**
 * AST-based code analyzer for Java.
 * 
 * NOTE: This analyzer currently only supports Java via JavaParser.
 * Kotlin and Scala are supported through TreeSitterFuzzyDetector for duplicate detection,
 * but do not have full AST analysis (security patterns, complexity metrics, etc.).
 * 
 * Future enhancement: Add language-specific AST analyzers for Kotlin (using Kotlin compiler metadata)
 * and Scala (using scalac or scala-meta) to provide parity with Java analysis capabilities.
 */
class ASTAnalyzer {

    private val logger = Logger.getInstance(ASTAnalyzer::class.java)
    private val javaParser = JavaParser()
    private val hardcodedConfigPatterns = listOf(
        "http://",
        "https://",
        "localhost",
        "password",
        "passwd",
        "apiKey",
        "api_key",
        "secret",
        "token",
        "jdbc:",
        "://",
        ".com",
        ".net",
        ".org",
        "/etc/",
        "C:\\"
    )

    private val secretPatterns = listOf(
        "sk-",  // Stripe/OpenAI secret keys
        "pk-",  // Stripe public keys
        "AKIA", // AWS access keys
        "AIza", // Google API keys
        "ya29", // Google OAuth tokens
        "ghp_", // GitHub personal access tokens
        "gho_", // GitHub OAuth tokens
        "ghu_", // GitHub user tokens
        "ghs_", // GitHub server tokens
        "ghr_", // GitHub refresh tokens
        "Bearer", // Bearer tokens in code
        "Basic", // Basic auth in code
        "eyJ"   // JWT tokens (base64 header)
    )

    private val placeholderDomainPatterns = listOf(
        "example.com",
        "example.org",
        "example.net",
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "[::1]",
        "test.com",
        "dev.com",
        "staging.com",
        "placeholder"
    )

    private val passwordVariableNames = listOf(
        "password",
        "passwd",
        "pwd",
        "pass",
        "secret",
        "token",
        "credential",
        "auth"
    )

    fun analyzeCode(code: String): ASTMetrics {
        return try {
            val compilationUnit = javaParser.parse(code).result.orElse(null) ?: return ASTMetrics()
            extractMetrics(compilationUnit)
        } catch (e: Exception) {
            logger.warn("AST analysis failed", e)
            ASTMetrics()
        }
    }

    private fun extractMetrics(cu: CompilationUnit): ASTMetrics {
        val methods = cu.findAll(MethodDeclaration::class.java)
        val classes = cu.findAll(ClassOrInterfaceDeclaration::class.java)
        val fields = cu.findAll(FieldDeclaration::class.java)

        val methodLengths = methods.map { calculateMethodLength(it) }
        val nestingDepths = methods.map { calculateNestingDepth(it.body.orElse(null)) }
        val complexities = methods.map { calculateCyclomaticComplexity(it.body.orElse(null)) }

        val stringLiterals = cu.findAll(StringLiteralExpr::class.java).map { it.value }
        val duplicateStringLiteralCount = stringLiterals.groupingBy { it }.eachCount().count { it.value > 1 }
        val hardcodedConfigLiteralCount = stringLiterals.count { matchesHardcodedConfig(it) }
        val hardcodedSecretCount = stringLiterals.count { matchesSecret(it) }
        val placeholderDomainCount = stringLiterals.count { matchesPlaceholderDomain(it) }
        val plaintextPasswordComparisonCount = countPlaintextPasswordComparisons(cu)

        val numberLiterals = buildList<String> {
            addAll(cu.findAll(IntegerLiteralExpr::class.java).map { it.value })
            addAll(cu.findAll(LongLiteralExpr::class.java).map { it.value })
            addAll(cu.findAll(DoubleLiteralExpr::class.java).map { it.value })
        }
        val duplicateNumberLiteralCount = numberLiterals.groupingBy { it }.eachCount().count { it.value > 1 }
        val magicNumberCount = numberLiterals.count { isMagicNumber(it) }

        val methodCallNames = cu.findAll(MethodCallExpr::class.java).map { it.name.asString() }
        val duplicateMethodCallCount = methodCallNames.groupingBy { it }.eachCount().count { it.value > 1 }

        val catchClauses = cu.findAll(CatchClause::class.java)
        val broadCatchCount = catchClauses.count { catchClause ->
            when (catchClause.parameter.type.asString()) {
                "Exception", "java.lang.Exception", "Throwable", "java.lang.Throwable" -> true
                else -> false
            }
        }
        val emptyCatchCount = catchClauses.count { it.body.statements.isEmpty() }

        val booleanOperatorCount = cu.findAll(BinaryExpr::class.java).count {
            it.operator == BinaryExpr.Operator.AND || it.operator == BinaryExpr.Operator.OR
        }

        val maxElseIfChainLength = methods.maxOfOrNull { method ->
            method.body.map { body ->
                body.findAll(IfStmt::class.java).map { calculateIfElseChainLength(it) }.maxOrNull() ?: 0
            }.orElse(0)
        } ?: 0

        val lineCommentCount = cu.findAll(LineComment::class.java).size
        val blockCommentCount = cu.findAll(BlockComment::class.java).size
        val javadocCommentCount = cu.findAll(JavadocComment::class.java).size
        val totalCommentCount = lineCommentCount + blockCommentCount + javadocCommentCount
        val codeLineCount = cu.toString().lines().count { it.isNotBlank() }
        val commentToCodeRatio = if (codeLineCount > 0) totalCommentCount.toDouble() / codeLineCount else 0.0

        val nullReturnMetrics = calculateNullReturnMetrics(cu)
        val nullReturnCount = nullReturnMetrics.first
        val maxNullChainDepth = nullReturnMetrics.second
        val hasNullReturns = nullReturnCount > 5 || maxNullChainDepth > 3

        val maxMethodLength = methodLengths.maxOrNull() ?: 0
        val averageMethodLength = if (methodLengths.isNotEmpty()) methodLengths.average() else 0.0
        val maxNestingDepth = nestingDepths.maxOrNull() ?: 0
        val totalComplexity = complexities.sum()
        val maxParameterCount = methods.maxOfOrNull { it.parameters.size } ?: 0
        val fieldCount = fields.sumOf { it.variables.size }

        return ASTMetrics(
            methodCount = methods.size,
            maxMethodLength = maxMethodLength,
            averageMethodLength = averageMethodLength,
            maxNestingDepth = maxNestingDepth,
            cyclomaticComplexity = totalComplexity,
            classCount = classes.size,
            fieldCount = fieldCount,
            maxParameterCount = maxParameterCount,
            stringLiteralCount = stringLiterals.size,
            duplicateStringLiteralCount = duplicateStringLiteralCount,
            duplicateNumberLiteralCount = duplicateNumberLiteralCount,
            hardcodedConfigLiteralCount = hardcodedConfigLiteralCount,
            magicNumberCount = magicNumberCount,
            duplicateMethodCallCount = duplicateMethodCallCount,
            broadCatchCount = broadCatchCount,
            emptyCatchCount = emptyCatchCount,
            booleanOperatorCount = booleanOperatorCount,
            maxElseIfChainLength = maxElseIfChainLength,
            lineCommentCount = lineCommentCount,
            blockCommentCount = blockCommentCount,
            javadocCommentCount = javadocCommentCount,
            hasComplexMethods = maxMethodLength > 50,
            hasDeepNesting = maxNestingDepth > 3,
            hasHighComplexity = totalComplexity > 10,
            hasHardcodedConfig = hardcodedConfigLiteralCount > 0,
            hasMagicNumbers = magicNumberCount > 3,
            hasLongParameterList = maxParameterCount > 4,
            hasBroadExceptionCatch = broadCatchCount > 0,
            hasEmptyCatchBlock = emptyCatchCount > 0,
            hasRepeatedMethodCalls = duplicateMethodCallCount > 0,
            hasHeavyBooleanLogic = booleanOperatorCount > 3,
            hasLongIfElseChain = maxElseIfChainLength > 2,
            hasExcessiveComments = commentToCodeRatio > 0.3,
            hasVerboseComments = lineCommentCount > 10 || blockCommentCount > 5,
            nullReturnCount = nullReturnCount,
            maxNullChainDepth = maxNullChainDepth,
            hasNullReturns = hasNullReturns,
            plaintextPasswordComparisonCount = plaintextPasswordComparisonCount,
            hardcodedSecretCount = hardcodedSecretCount,
            placeholderDomainCount = placeholderDomainCount,
            hasPlaintextPasswordComparison = plaintextPasswordComparisonCount > 0,
            hasHardcodedSecrets = hardcodedSecretCount > 0,
            hasPlaceholderDomains = placeholderDomainCount > 0
        )
    }

    private fun calculateMethodLength(method: MethodDeclaration): Int {
        return method.body?.let { body ->
            body.toString().lines().size
        } ?: 0
    }

    private fun calculateNestingDepth(statement: Statement?): Int {
        if (statement == null) return 0

        return when (statement) {
            is BlockStmt -> {
                val childDepths = statement.statements.map { calculateNestingDepth(it) }
                childDepths.maxOrNull()?.plus(1) ?: 1
            }
            is IfStmt -> {
                val thenDepth = calculateNestingDepth(statement.thenStmt)
                val elseDepth = statement.elseStmt.map { calculateNestingDepth(it) }.orElse(0)
                maxOf(thenDepth, elseDepth) + 1
            }
            is ForStmt, is WhileStmt, is DoStmt -> {
                calculateNestingDepth(statement.toBlock()?.statements?.firstOrNull()) + 1
            }
            is SwitchStmt -> {
                val caseDepths = statement.entries.map { entry ->
                    entry.statements.map { calculateNestingDepth(it) }.maxOrNull() ?: 0
                }
                (caseDepths.maxOrNull() ?: 0) + 1
            }
            else -> 1
        }
    }

    private fun calculateCyclomaticComplexity(statement: Statement?): Int {
        if (statement == null) return 1

        var complexity = 1

        statement.walk { node ->
            when (node) {
                is IfStmt -> complexity++
                is ForStmt, is WhileStmt, is DoStmt -> complexity++
                is SwitchStmt -> complexity += node.entries.size
            }
        }

        return complexity
    }

    private fun calculateIfElseChainLength(statement: Statement?): Int {
        if (statement !is IfStmt) return 0

        var length = 1
        var current: Statement? = statement.elseStmt.orElse(null)

        while (current is IfStmt) {
            length++
            current = current.elseStmt.orElse(null)
        }

        return length
    }

    private fun Statement.toBlock(): BlockStmt? {
        return this as? BlockStmt
    }

    private fun matchesHardcodedConfig(value: String): Boolean {
        val normalized = value.lowercase()
        return hardcodedConfigPatterns.any { normalized.contains(it.lowercase()) }
    }

    private fun isMagicNumber(value: String): Boolean {
        val cleaned = value.removeSuffix("L").removeSuffix("l")
        val number = cleaned.toDoubleOrNull() ?: return false
        return number != 0.0 && number != 1.0 && number != -1.0
    }

    private fun calculateNullReturnMetrics(cu: CompilationUnit): Pair<Int, Int> {
        val code = cu.toString()
        
        // Count .orElse(null) occurrences
        val orElseNullPattern = Regex("""\.orElse\s*\(\s*null\s*\)""")
        val nullReturnCount = orElseNullPattern.findAll(code).count()
        
        // Calculate maximum chain depth (e.g., .orElse().orElse(null) = depth 2)
        val chainPattern = Regex("""(\.orElse\s*\()""")
        val maxChainDepth = calculateMaxChainDepth(code, chainPattern)
        
        return Pair(nullReturnCount, maxChainDepth)
    }

    private fun calculateMaxChainDepth(code: String, pattern: Regex): Int {
        val matches = pattern.findAll(code).toList()
        if (matches.isEmpty()) return 0
        
        var maxDepth = 0
        var currentDepth = 0
        
        for (match in matches) {
            currentDepth++
            if (currentDepth > maxDepth) {
                maxDepth = currentDepth
            }
            
            // Reset depth if we encounter a semicolon or closing brace (end of statement)
            val afterMatch = code.substring(match.range.last + 1).take(50)
            if (afterMatch.contains(";") || afterMatch.contains("}")) {
                currentDepth = 0
            }
        }
        
        return maxDepth
    }

    private fun matchesSecret(value: String): Boolean {
        val normalized = value.lowercase()
        return secretPatterns.any { normalized.contains(it.lowercase()) } ||
               isBase64Like(value) ||
               isJwtLike(value) ||
               isUuidLike(value)
    }

    private fun isBase64Like(value: String): Boolean {
        if (value.length < 20) return false
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        return value.all { it in base64Chars } && (value.endsWith("=") || value.endsWith("=="))
    }

    private fun isJwtLike(value: String): Boolean {
        return value.startsWith("eyJ") && value.contains(".")
    }

    private fun isUuidLike(value: String): Boolean {
        val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return uuidPattern.matches(value)
    }

    private fun matchesPlaceholderDomain(value: String): Boolean {
        val normalized = value.lowercase()
        return placeholderDomainPatterns.any { normalized.contains(it.lowercase()) }
    }

    private fun countPlaintextPasswordComparisons(cu: CompilationUnit): Int {
        var count = 0

        // Check method calls like .equals(password), .compareTo(password)
        cu.findAll(MethodCallExpr::class.java).forEach { methodCall ->
            val methodName = methodCall.nameAsString
            if (methodName in listOf("equals", "compareTo")) {
                val args = methodCall.arguments
                if (args.isNotEmpty()) {
                    val firstArg = args[0].toString().lowercase()
                    if (passwordVariableNames.any { firstArg.contains(it) }) {
                        count++
                    }
                }
            }
        }

        // Check binary expressions like password ==, password.equals()
        cu.findAll(BinaryExpr::class.java).forEach { binaryExpr ->
            val left = binaryExpr.left.toString().lowercase()
            val right = binaryExpr.right.toString().lowercase()
            if (binaryExpr.operator == BinaryExpr.Operator.EQUALS ||
                binaryExpr.operator == BinaryExpr.Operator.NOT_EQUALS) {
                if (passwordVariableNames.any { left.contains(it) } ||
                    passwordVariableNames.any { right.contains(it) }) {
                    count++
                }
            }
        }

        return count
    }
}
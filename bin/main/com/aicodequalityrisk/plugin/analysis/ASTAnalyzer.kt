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
            hasVerboseComments = lineCommentCount > 10 || blockCommentCount > 5
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
}
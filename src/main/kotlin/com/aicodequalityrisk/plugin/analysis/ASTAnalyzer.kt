package com.aicodequalityrisk.plugin.analysis

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.intellij.openapi.diagnostic.Logger
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
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

        val maxMethodLength = methodLengths.maxOrNull() ?: 0
        val averageMethodLength = if (methodLengths.isNotEmpty()) methodLengths.average() else 0.0
        val maxNestingDepth = nestingDepths.maxOrNull() ?: 0
        val totalComplexity = complexities.sum()
        val maxParameterCount = methods.maxOfOrNull { it.parameters.size } ?: 0
        val fieldCount = fields.sumOf { it.variables.size }

        val originalCode = cu.toString()
        val commentCount = countComments(originalCode)
        val hasGenericMethodNames = hasGenericMethodNames(methods)
        val hasManagerHandlerClasses = hasManagerHandlerClasses(classes)

        val sqlStringConcatCount = countSqlStringConcat(cu)
        val stringEqualsCount = countStringEquals(cu)
        val deprecatedApiUsageCount = countDeprecatedApiUsage(cu)
        val loggingInLoopCount = countLoggingInLoop(cu)

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
            commentCount = commentCount,
            hasExcessiveComments = commentCount > 5,
            hasGenericMethodNames = hasGenericMethodNames,
            hasManagerHandlerClasses = hasManagerHandlerClasses,
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
            sqlStringConcatCount = sqlStringConcatCount,
            stringEqualsCount = stringEqualsCount,
            deprecatedApiUsageCount = deprecatedApiUsageCount,
            loggingInLoopCount = loggingInLoopCount
        )
    }

    private fun calculateMethodLength(method: MethodDeclaration): Int {
        return method.body?.let { body ->
            body.toString().lines().size
        } ?: 0
    }

    private fun calculateNestingDepth(statement: Statement?, currentDepth: Int = 0): Int {
        if (statement == null) return currentDepth

        return when (statement) {
            is BlockStmt -> {
                statement.statements.maxOfOrNull { calculateNestingDepth(it, currentDepth) } 
                    ?: currentDepth
            }
            is IfStmt -> {
                val thenDepth = calculateNestingDepth(statement.thenStmt, currentDepth + 1)
                val elseDepth = statement.elseStmt.map { calculateNestingDepth(it, currentDepth + 1) }.orElse(currentDepth)
                maxOf(thenDepth, elseDepth)
            }
            is ForStmt -> {
                calculateNestingDepth(statement.body, currentDepth + 1)
            }
            is WhileStmt -> {
                calculateNestingDepth(statement.body, currentDepth + 1)
            }
            is DoStmt -> {
                calculateNestingDepth(statement.body, currentDepth + 1)
            }
            is SwitchStmt -> {
                statement.entries.maxOfOrNull { entry ->
                    entry.statements.maxOfOrNull { calculateNestingDepth(it, currentDepth + 1) } 
                        ?: currentDepth + 1
                } ?: currentDepth
            }
            else -> currentDepth + 1
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

    private fun countComments(code: String): Int {
        var count = 0
        var i = 0
        while (i < code.length) {
            if (code[i] == '/' && i + 1 < code.length) {
                if (code[i + 1] == '/') {
                    count++
                    i += 2
                    while (i < code.length && code[i] != '\n') i++
                    continue
                } else if (code[i + 1] == '*') {
                    count++
                    i += 2
                    while (i + 1 < code.length && !(code[i] == '*' && code[i + 1] == '/')) {
                        i++
                    }
                    i += 2
                    continue
                }
            }
            i++
        }
        return count
    }

    private fun hasGenericMethodNames(methods: List<MethodDeclaration>): Boolean {
        val genericPatterns = listOf(
            "process", "handle", "doIt", "execute", "run",
            "data", "info", "result", "value", "item",
            "temp", "tmp", "flag", "check", "validate"
        )
        return methods.any { method ->
            val name = method.nameAsString.lowercase()
            genericPatterns.any { pattern ->
                name == pattern || name.startsWith(pattern) || name.endsWith(pattern)
            }
        }
    }

    private fun hasManagerHandlerClasses(classes: List<ClassOrInterfaceDeclaration>): Boolean {
        return classes.any { cls ->
            val name = cls.nameAsString
            name.endsWith("Manager", ignoreCase = true) ||
                    name.endsWith("Handler", ignoreCase = true) ||
                    name.endsWith("Helper", ignoreCase = true) ||
                    name.endsWith("Util", ignoreCase = true) ||
                    name.endsWith("Service", ignoreCase = true)
        }
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

    private fun countSqlStringConcat(cu: CompilationUnit): Int {
        val binaryExprs = cu.findAll(BinaryExpr::class.java)
        val sqlKeywords = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "WHERE", "FROM", "JOIN", "ON", "AND", "OR", "ORDER BY", "GROUP BY", "HAVING")
        var count = 0
        
        for (expr in binaryExprs) {
            if (expr.operator == BinaryExpr.Operator.PLUS) {
                val left = expr.left.toString().uppercase()
                val right = expr.right.toString().uppercase()
                if (sqlKeywords.any { left.contains(it) || right.contains(it) }) {
                    count++
                }
            }
        }
        return count
    }

    private fun countStringEquals(cu: CompilationUnit): Int {
        val binaryExprs = cu.findAll(BinaryExpr::class.java)
        var count = 0
        val stringVarNames = mutableSetOf<String>()
        
        for (method in cu.findAll(MethodDeclaration::class.java)) {
            method.parameters.filter { it.type.asString() == "String" }.forEach { 
                stringVarNames.add(it.nameAsString) 
            }
        }
        
        for (expr in binaryExprs) {
            if (expr.operator == BinaryExpr.Operator.EQUALS || expr.operator == BinaryExpr.Operator.NOT_EQUALS) {
                val left = expr.left.toString()
                val right = expr.right.toString()
                if (left in stringVarNames || right in stringVarNames || left == "null" || right == "null") {
                    count++
                }
            }
        }
        return count
    }

    private fun countDeprecatedApiUsage(cu: CompilationUnit): Int {
        var count = 0
        val typeDeclarations = cu.findAll(TypeDeclaration::class.java)
        
        for (typeDecl in typeDeclarations) {
            for (member in typeDecl.members) {
                val code = member.toString()
                if (code.contains("java.util.Date") || code.contains("java.util.Vector") || code.contains("java.util.Hashtable") ||
                    code.contains("java.sql.Date") || code.contains("new Date()") || code.contains("new Vector()") || code.contains("new Hashtable()")) {
                    count++
                }
                if (member is FieldDeclaration) {
                    for (varDecl in member.variables) {
                        val typeStr = varDecl.type.toString()
                        if (typeStr == "Date" || typeStr == "Vector" || typeStr == "Hashtable" ||
                            typeStr == "java.util.Date" || typeStr == "java.util.Vector" || typeStr == "java.util.Hashtable") {
                            count++
                        }
                    }
                }
                if (member is MethodDeclaration) {
                    for (param in member.parameters) {
                        val paramType = param.type.toString()
                        if (paramType == "Date" || paramType == "Vector" || paramType == "Hashtable" ||
                            paramType == "java.util.Date" || paramType == "java.util.Vector" || paramType == "java.util.Hashtable") {
                            count++
                        }
                    }
                }
            }
        }
        
        for (method in cu.findAll(MethodDeclaration::class.java)) {
            for (param in method.parameters) {
                val paramType = param.type.toString()
                if (paramType == "Date" || paramType == "Vector" || paramType == "Hashtable" ||
                    paramType == "java.util.Date" || paramType == "java.util.Vector" || paramType == "java.util.Hashtable") {
                    count++
                }
            }
            method.body.ifPresent { body ->
                val methodCode = body.toString()
                if (methodCode.contains("new Date()") || methodCode.contains("new Vector()") || methodCode.contains("new Hashtable()")) {
                    count++
                }
            }
        }
        
        return count
    }

    private fun countLoggingInLoop(cu: CompilationUnit): Int {
        val forStmts = cu.findAll(ForStmt::class.java)
        val whileStmts = cu.findAll(WhileStmt::class.java)
        var count = 0
        
        for (forStmt in forStmts) {
            val bodyCode = forStmt.body.toString()
            if (bodyCode.contains("System.out.println") || bodyCode.contains("logger.") || 
                bodyCode.contains("Log.") || bodyCode.contains("log.")) {
                count++
            }
        }
        
        for (whileStmt in whileStmts) {
            val bodyCode = whileStmt.body.toString()
            if (bodyCode.contains("System.out.println") || bodyCode.contains("logger.") || 
                bodyCode.contains("Log.") || bodyCode.contains("log.")) {
                count++
            }
        }
        
        return count
    }
}
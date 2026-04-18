package com.aicodequalityrisk.generator.runner

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.StringLiteralExpr
import java.nio.file.Path
import java.nio.file.Files
import kotlin.math.max

data class DetectionResult(
    val duplicateStringLiterals: Int,
    val duplicateNumberLiterals: Int,
    val duplicateMethodCalls: Int,
    val duplicateMethodCount: Int,
    val maxSimilarityScore: Double,
    val totalLoc: Int
)

class DetectionRunner {
    private val javaParser = JavaParser()

    fun analyze(projectPath: Path): DetectionResult {
        val javaFiles = Files.walk(projectPath)
            .filter { it.toString().endsWith(".java") }
            .toList()

        var totalDuplicateStrings = 0
        var totalDuplicateNumbers = 0
        var totalDuplicateMethodCalls = 0
        var maxSimilarity = 0.0
        var totalLoc = 0
        var allMethods = mutableListOf<MethodInfoData>()
        var similarPairs = emptyList<SimilarPair>()

        javaFiles.forEach { file ->
            val code = Files.readString(file)
            try {
                val cu = javaParser.parse(code).result.orElse(null) ?: return@forEach
                val metrics = analyzeFile(cu)
                totalDuplicateStrings += metrics.duplicateStrings
                totalDuplicateNumbers += metrics.duplicateNumbers
                totalDuplicateMethodCalls += metrics.duplicateMethodCalls
                totalLoc += metrics.loc
                allMethods.addAll(metrics.methods)
            } catch (e: Exception) {
                System.err.println("Failed to parse $file: ${e.message}")
            }
        }

        if (allMethods.size >= 2) {
            similarPairs = findSimilarMethods(allMethods)
            if (similarPairs.isNotEmpty()) {
                maxSimilarity = similarPairs.maxOfOrNull { it.similarity } ?: 0.0
            }
        }

        return DetectionResult(
            duplicateStringLiterals = totalDuplicateStrings,
            duplicateNumberLiterals = totalDuplicateNumbers,
            duplicateMethodCalls = totalDuplicateMethodCalls,
            duplicateMethodCount = similarPairs.size,
            maxSimilarityScore = maxSimilarity,
            totalLoc = totalLoc
        )
    }

    private fun makeMethodInfo(name: String, body: String): MethodInfoData {
        return MethodInfoData(name, body)
    }

    private fun analyzeFile(cu: CompilationUnit): FileMetrics {
        val methodsList: List<MethodDeclaration> = cu.findAll(MethodDeclaration::class.java)
        
        val stringLiterals: List<String> = cu.findAll(StringLiteralExpr::class.java).map { it.value }
        
        val duplicateStrings: Int = stringLiterals
            .groupingBy { it }
            .eachCount()
            .count { it.value > 1 }

        val numberLiterals: MutableList<String> = mutableListOf()
        for (lit in cu.findAll(com.github.javaparser.ast.expr.IntegerLiteralExpr::class.java)) {
            numberLiterals.add(lit.value)
        }
        for (lit in cu.findAll(com.github.javaparser.ast.expr.LongLiteralExpr::class.java)) {
            numberLiterals.add(lit.value)
        }

        val duplicateNumbers: Int = numberLiterals
            .groupingBy { it }
            .eachCount()
            .count { it.value > 1 }

        val methodCallNames: List<String> = cu.findAll(com.github.javaparser.ast.expr.MethodCallExpr::class.java)
            .map { it.name.asString() }
        val duplicateMethodCalls: Int = methodCallNames
            .groupingBy { it }
            .eachCount()
            .count { it.value > 1 }

        val loc: Int = cu.toString().lines().count { it.isNotBlank() }

        val methodInfos = mutableListOf<MethodInfoData>()
        var idx = 0
        while (idx < methodsList.size) {
            val method: MethodDeclaration = methodsList[idx]
            val methodName: String = method.getName().asString()
            val normalized: String = normalizeMethodBody(method)
            val info: MethodInfoData = makeMethodInfo(methodName, normalized)
            methodInfos.add(info)
            idx = idx + 1
        }

        return FileMetrics(
            duplicateStrings = duplicateStrings,
            duplicateNumbers = duplicateNumbers,
            duplicateMethodCalls = duplicateMethodCalls,
            loc = loc,
            methods = methodInfos
        )
    }

    private fun normalizeMethodBody(method: MethodDeclaration): String {
        return method.toString()
            .replace(Regex("\"[^\"]*\""), "\"\"")
            .replace(Regex("\\b\\d+\\"), "0")
            .replace(Regex("//.*"), "")
            .replace(Regex("/\\*.*?\\*/"), "")
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    private fun findSimilarMethods(methods: List<MethodInfoData>): List<SimilarPair> {
        val pairs = mutableListOf<SimilarPair>()
        for (i in methods.indices) {
            for (j in i + 1 until methods.size) {
                val sim = jaccardSimilarity(methods[i].normalizedBody, methods[j].normalizedBody)
                if (sim >= 0.5) {
                    pairs.add(SimilarPair(methods[i].name, methods[j].name, sim))
                }
            }
        }
        return pairs
    }

    private fun jaccardSimilarity(a: String, b: String): Double {
        val setA = a.toSet()
        val setB = b.toSet()
        if (setA.isEmpty() && setB.isEmpty()) return 0.0
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private data class MethodInfoData(
        val name: String,
        val normalizedBody: String
    )

    private data class SimilarPair(
        val first: String,
        val second: String,
        val similarity: Double
    )

    private data class FileMetrics(
        val duplicateStrings: Int,
        val duplicateNumbers: Int,
        val duplicateMethodCalls: Int,
        val loc: Int,
        val methods: List<MethodInfoData>
    )
}
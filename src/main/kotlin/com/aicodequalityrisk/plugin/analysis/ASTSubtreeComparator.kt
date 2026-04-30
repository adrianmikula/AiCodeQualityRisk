package com.aicodequalityrisk.plugin.analysis

import org.treesitter.TSNode

/**
 * Compares AST subtrees to measure structural similarity between method bodies.
 * Uses tree edit distance approximation via normalized structural hashing.
 */
class ASTSubtreeComparator {
    
    /**
     * Extracts a normalized structural hash from a method body node.
     * Normalization removes identifiers, literals, and specific types to focus on structure.
     */
    fun extractSubtreeHash(methodNode: TSNode, source: String): String {
        val bodyNode = findMethodBody(methodNode) ?: return ""
        return normalizeSubtree(bodyNode, source)
    }
    
    /**
     * Calculates structural similarity between two method bodies.
     * Returns 0.0 (no similarity) to 1.0 (identical structure).
     */
    fun calculateSimilarity(hash1: String, hash2: String): Double {
        if (hash1.isEmpty() || hash2.isEmpty()) return 0.0
        if (hash1 == hash2) return 1.0
        
        // Use longest common subsequence of normalized tokens as similarity metric
        val tokens1 = hash1.split(" ")
        val tokens2 = hash2.split(" ")
        
        val lcsLength = longestCommonSubsequence(tokens1, tokens2)
        val maxLength = maxOf(tokens1.size, tokens2.size)
        
        return if (maxLength == 0) 0.0 else lcsLength.toDouble() / maxLength.toDouble()
    }
    
    /**
     * Calculates tree edit distance approximation using normalized token sequences.
     */
    fun calculateTreeEditDistance(hash1: String, hash2: String): Double {
        if (hash1.isEmpty() || hash2.isEmpty()) return 1.0
        if (hash1 == hash2) return 0.0
        
        val tokens1 = hash1.split(" ")
        val tokens2 = hash2.split(" ")
        
        // Levenshtein distance on normalized token sequences
        val distance = levenshteinDistance(tokens1, tokens2)
        val maxLength = maxOf(tokens1.size, tokens2.size)
        
        return if (maxLength == 0) 0.0 else distance.toDouble() / maxLength.toDouble()
    }
    
    private fun findMethodBody(methodNode: TSNode): TSNode? {
        // Find the block node that contains the method body
        for (i in 0 until methodNode.getChildCount()) {
            val child = methodNode.getChild(i)
            if (!child.isNull() && child.getType() == "block") {
                return child
            }
        }
        return null
    }
    
    private fun normalizeSubtree(node: TSNode, source: String): String {
        val normalizedTokens = mutableListOf<String>()
        normalizeNode(node, source, normalizedTokens)
        return normalizedTokens.joinToString(" ")
    }
    
    private fun normalizeNode(node: TSNode, source: String, tokens: MutableList<String>) {
        val nodeType = node.getType()
        
        // Skip certain nodes that don't contribute to structural similarity
        if (nodeType in setOf("comment", "line_comment", "block_comment")) {
            return
        }
        
        // Normalize leaf nodes (identifiers, literals, etc.)
        if (node.getNamedChildCount() == 0) {
            val normalized = normalizeLeaf(node, nodeType)
            if (normalized.isNotEmpty()) {
                tokens.add(normalized)
            }
            return
        }
        
        // Add structural node type
        tokens.add(normalizeNodeType(nodeType))
        
        // Recursively process children
        for (i in 0 until node.getNamedChildCount()) {
            val child = node.getNamedChild(i)
            if (!child.isNull()) {
                normalizeNode(child, source, tokens)
            }
        }
    }
    
    private fun normalizeLeaf(node: TSNode, nodeType: String): String {
        return when (nodeType) {
            "identifier", "type_identifier" -> "ID"
            "string_literal", "char_literal", "text" -> "LIT"
            "decimal_integer_literal", "decimal_float_literal", 
            "hex_integer_literal", "octal_integer_literal", "binary_integer_literal" -> "NUM"
            "true", "false" -> "BOOL"
            "null" -> "NULL"
            else -> nodeType.uppercase()
        }
    }
    
    private fun normalizeNodeType(nodeType: String): String {
        return when (nodeType) {
            "if_statement" -> "IF"
            "for_statement" -> "FOR"
            "while_statement" -> "WHILE"
            "do_statement" -> "DO"
            "try_statement" -> "TRY"
            "catch_clause" -> "CATCH"
            "finally_clause" -> "FINALLY"
            "switch_expression", "switch_statement" -> "SWITCH"
            "case" -> "CASE"
            "return_statement" -> "RETURN"
            "throw_statement" -> "THROW"
            "break_statement" -> "BREAK"
            "continue_statement" -> "CONTINUE"
            "expression_statement" -> "EXPR"
            "local_variable_declaration" -> "VAR"
            "field_declaration" -> "FIELD"
            "method_invocation" -> "CALL"
            "object_creation_expression" -> "NEW"
            "assignment_expression" -> "ASSIGN"
            "binary_expression" -> "BINOP"
            "unary_expression" -> "UNOP"
            "ternary_expression" -> "TERNARY"
            "cast_expression" -> "CAST"
            "instanceof_expression" -> "INSTANCEOF"
            else -> nodeType.uppercase()
        }
    }
    
    private fun longestCommonSubsequence(a: List<String>, b: List<String>): Int {
        val m = a.size
        val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) {
            for (j in 0..n) {
                when {
                    i == 0 || j == 0 -> dp[i][j] = 0
                    a[i - 1] == b[j - 1] -> dp[i][j] = dp[i - 1][j - 1] + 1
                    else -> dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        
        return dp[m][n]
    }
    
    private fun levenshteinDistance(a: List<String>, b: List<String>): Int {
        val m = a.size
        val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
    }
}

/**
 * Fingerprint containing AST subtree hash and metadata for a method.
 */
data class ASTSubtreeFingerprint(
    val name: String,
    val subtreeHash: String,
    val methodLength: Int,
    val complexity: Int,
    val tokenCount: Int
) {
    fun getSimilarityScore(other: ASTSubtreeFingerprint, comparator: ASTSubtreeComparator): Double {
        return comparator.calculateSimilarity(subtreeHash, other.subtreeHash)
    }
    
    fun getTreeEditDistance(other: ASTSubtreeFingerprint, comparator: ASTSubtreeComparator): Double {
        return comparator.calculateTreeEditDistance(subtreeHash, other.subtreeHash)
    }
}

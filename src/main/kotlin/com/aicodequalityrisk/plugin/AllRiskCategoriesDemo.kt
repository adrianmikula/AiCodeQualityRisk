package com.aicodequalityrisk.plugin

/**
 * AllRiskCategoriesDemo - A class demonstrating all 10 risk categories.
 * Each category should trigger a non-zero score when analyzed.
 */
class AllRiskCategoriesDemo {

    // ============================================================
    // 1. BOILERPLATE BLOAT: averageMethodLength > 50, maxMethodLength > 100
    //    duplicateStringLiteralCount > 3
    // ============================================================
    
    fun thisIsAVeryLongMethodWithLotsOfBoilerplateCode() {
        val message1 = "Processing data"
        val message2 = "Processing data"
        val message3 = "Processing data"
        val message4 = "Processing data"
        
        var temp1 = 1
        temp1 = temp1
        var temp2 = 2
        temp2 = temp2
        
        val result1 = calculateSomething(temp1)
        val result2 = calculateSomething(temp2)
        val result3 = calculateSomething(3)
        val result4 = calculateSomething(4)
        val result5 = calculateSomething(5)
        
        println(message1)
        println(message2)
        println(message3)
        println(message4)
        
        if (result1 != null) {
            if (result2 != null) {
                if (result3 != null) {
                    if (result4 != null) {
                        println("All results valid")
                    }
                }
            }
        }
    }

    fun anotherVeryLongMethodForBoilerplate() {
        val data = "Starting up"
        val dup1 = "Starting up"
        val dup2 = "Starting up"
        val dup3 = "Starting up"
        
        for (i in 0..10) {
            println(data)
            println(dup1)
            println(dup2)
            println(dup3)
        }
        
        doProcess()
        doProcess()
        doProcess()
    }

    // ============================================================
    // 2. VERBOSE COMMENT SPAM: lineCommentCount > 10 OR blockCommentCount > 5
    // ============================================================
    
    // Line comment 1
    // Line comment 2
    // Line comment 3
    // Line comment 4
    // Line comment 5
    // Line comment 6
    // Line comment 7
    // Line comment 8
    // Line comment 9
    // Line comment 10
    // Line comment 11
    // Line comment 12
    fun heavilyCommentedFunction() {
        // Initialize the variable
        val x = 10
        // Add 20 to x
        val y = x + 20
        // Print the result
        println(y)
    }

    /* Block comment 1 */
    /* Block comment 2 */
    /* Block comment 3 */
    /* Block comment 4 */
    /* Block comment 5 */
    /* Block comment 6 */
    fun blockCommentHeavy() {
        // More comments
        // More comments
        // More comments
    }

    // ============================================================
    // 3. OVER-DEFENSIVE: duplicateMethodCallCount > 2
    // ============================================================
    
    fun defensiveProgramming() {
        val list = mutableListOf<String>()
        list.add("item1")
        list.add("item2")
        list.add("item3")
        list.size
        list.size
        list.size
        
        val map = mutableMapOf<String, Int>()
        map.put("a", 1)
        map.put("b", 2)
        map.get("a")
        map.get("a")
        map.get("b")
        map.get("b")
    }

    // ============================================================
    // 4. MAGIC NUMBERS: magicNumberCount > 0
    // ============================================================
    
    fun magicNumbersExample() {
        val rate = 1.15
        val discount = 0.9
        val shipping = 5.99
        val timeout = 30000
        val maxRetries = 3
        val buffer = 1024
        val port = 8080
        
        val total = 100.0 * rate - discount * 100 + shipping
        println(total)
    }

    // ============================================================
    // 5. COMPLEX BOOLEAN: booleanOperatorCount > 5, maxElseIfChainLength > 2
    // ============================================================
    
    fun complexBooleanLogic() {
        val a = true
        val b = false
        val c = true
        
        if (a && b || c && !a || b && !c || (a || b) && (c || b) && !a) {
            println("Complex")
        }
        
        if (a && b && c || !a && !b && !c || a && !b || !a && b) {
            println("More")
        }
    }

    fun longElseIfChain() {
        val x = 3
        if (x == 1) { println("one") }
        else if (x == 2) { println("two") }
        else if (x == 3) { println("three") }
        else if (x == 4) { println("four") }
        else if (x == 5) { println("five") }
    }

    // ============================================================
    // 6. DEEP NESTING: maxNestingDepth > 3
    // ============================================================
    
    fun deeplyNestedConditions() {
        val data = getData()
        if (data != null) {
            if (data.isValid) {
                if (data.content != null) {
                    if (data.content.isNotEmpty()) {
                        for (item in data.content) {
                            if (item != null) {
                                if (item.isReady) {
                                    println(item.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // 7. VERBOSE LOGGING: stringLiteralCount > 10
    // ============================================================
    
    fun lotsOfLogging() {
        println("Starting")
        println("Step 1")
        println("Step 2")
        println("Step 3")
        println("Step 4")
        println("Step 5")
        println("Step 6")
        println("Step 7")
        println("Step 8")
        println("Step 9")
        println("Step 10")
        println("Complete")
    }

    // ============================================================
    // 8. POOR NAMING: hardcodedConfigLiteralCount > 2
    // ============================================================
    
    fun hardcodedUrls() {
        val apiUrl = "https://api.example.com"
        val dbUrl = "jdbc:mysql://localhost:3306/db"
        val adminEmail = "admin@example.com"
        val basePath = "/home/user"
    }

    // ============================================================
    // 9. FRAMEWORK MISUSE: broadCatchCount > 0, emptyCatchCount > 0
    // ============================================================
    
    fun exceptionProblems() {
        try {
            riskyOperation()
        } catch (e: Exception) {
            // Empty catch
        }
        
        try {
            anotherRiskyCall()
        } catch (e: Throwable) {
            // Also empty
        }
    }

    // ============================================================
    // 10. EXCESSIVE DOCUMENTATION: javadocCommentCount > 5 OR commentToCodeRatio > 0.3
    // ============================================================

    /**
     * Processes user data and returns a result.
     * Takes a user object as input parameter.
     * Validates the input data before processing.
     * Returns a processed result object.
     */
    fun processUserData(user: User) {
        /** Validates the user input */
        if (user.name.isNotEmpty()) {
            /** Processes the name */
            println(user.name)
        }
    }

    /**
     * Another method with Javadoc.
     * More documentation here.
     * Even more documentation.
     * And some more.
     */
    fun anotherMethod() {}

    /**
     * Yet another Javadoc.
     */
    fun yetAnother() {}

    /**
     * More documentation.
     */
    fun moreDocs() {}

    /**
     * Even more methods.
     */
    fun evenMore() {}
}

data class User(val name: String)

class DataClass(
    val isValid: Boolean,
    val content: List<ItemClass>?
)

class ItemClass(
    val name: String,
    val isReady: Boolean
)

fun getData(): DataClass? = null
fun calculateSomething(x: Int): Int? = x
fun doProcess() {}
fun riskyOperation() {}
fun anotherRiskyCall() {}


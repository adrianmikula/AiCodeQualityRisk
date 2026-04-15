class BadQualityCodeExamples {

    // CATEGORY: POOR NAMING - Unclear, generic, or misleading names
    fun process() {
        val d = 10 // What is d?
        val temp = "hello" // Generic temp
        val data = listOf(1, 2, 3) // What kind of data?
        val result = data.filter { it > d }
        val x = result.sum()
        println(x)
    }

    fun handle() {
        val a = 5
        val b = 10
        val c = a + b
        doSomething(c)
    }

    fun doSomething(v: Int) {
        println(v)
    }





    // CATEGORY: MAGIC NUMBERS - Hardcoded numeric literals without explanation
    fun calculate() {
        val price = 100.0
        val tax = price * 1.15 // What is 1.15?
        val discount = price * 0.9 // What is 0.9?
        val shipping = 5.99 // Flat shipping fee?
        val total = tax + shipping - discount
        println(total)
    }

    fun timeRelated() {
        Thread.sleep(5000) // Why 5 seconds?
        val timeout = 30000 // 30 seconds?
        val retry = 3 // 3 retries?
        val maxSize = 100 // Max size what?
    }


    // CATEGORY: DEEP NESTING - Excessive nested conditions
    fun complexNesting() {
        val user = getUser()
        if (user != null) {
            if (user.isActive) {
                if (user.hasPermission) {
                    if (user.role != null) {
                        if (user.role.name == "ADMIN") {
                            if (user.isVerified) {
                                println("Full access granted")
                            }
                        }
                    }
                }
            }
        }
    }

    fun getUser(): User? = null


    // CATEGORY: COMPLEX BOOLEAN LOGIC - Overly complicated boolean expressions
    fun complexBoolean() {
        val a = true
        val b = false
        val c = true
        val d = false

        if (a && b || c && !d || (a || b) && (c || d) && !a) {
            println("Do something")
        }

        val result = a && b && c && d || !a && !b && !c && !d || (a ^ b) && (c ^ d)
    }


    // CATEGORY: BOILERPLATE BLOAT - Excessive boilerplate code
    data class Person(val name: String, val age: Int)

    fun boilerplateMethod(person: Person?) {
        if (person != null) {
            val p = person
            if (p.name != null) {
                val n = p.name
                if (n.length > 0) {
                    println(n)
                }
            }
        }
    }

    fun verboseNullCheck() {
        var str: String? = "hello"
        if (str != null) {
            val s = str
            if (s != null) {
                val x = s
                if (x != null) {
                    println(x.length)
                }
            }
        }
    }


    // CATEGORY: OVER-DEFENSIVE PROGRAMMING - Unnecessary defensive checks
    fun defensiveCode(items: List<String>?) {
        if (items != null) {
            if (items.size > 0) {
                if (items.isNotEmpty()) {
                    for (item in items) {
                        if (item != null) {
                            if (item.isNotEmpty()) {
                                println(item)
                            }
                        }
                    }
                }
            }
        }
    }

    fun unnecessarySafety() {
        var list = mutableListOf<String>()
        if (list.add("test")) {
            if (list.size > 0) {
                println(list[0])
            }
        }
    }


    // CATEGORY: VERBOSE COMMENT SPAM - Excessive obvious comments
    fun commentedTooMuch() {
        // This is a variable named x
        val x = 10 // Initialize x to 10
        // This is a variable named y
        val y = 20 // Initialize y to 20
        // This is a variable named z
        val z = x + y // Add x and y together
        // Print the result
        println(z) // Output the sum
    }

    // This function calculates the sum of two numbers
    // It takes two integer parameters: a and b
    // It returns the sum of a and b
    fun add(a: Int, b: Int): Int {
        // Create a variable to store the result
        val result = a + b // Add a and b
        // Return the result
        return result // Return the sum
    }


    // CATEGORY: EXCESSIVE DOCUMENTATION - Too much redundant documentation
    /**
     * This is a data class representing a user.
     * It has a name property of type String.
     * It has an age property of type Int.
     */
    data class User(
        /** The name of the user */
        val name: String,
        /** The age of the user */
        val age: Int
    )

    /**
     * This function processes user data.
     * It takes a User object as input.
     * It processes the user data.
     * It returns nothing.
     */
    fun processUser(user: User) {
        // Process user
    }


    // CATEGORY: VERBOSE LOGGING - Excessive logging
    fun verboseLogging() {
        println("Starting process...")
        println("Initializing variables...")
        println("Loading data...")
        println("Processing step 1...")
        println("Processing step 2...")
        println("Processing step 3...")
        println("Finalizing...")
        println("Done!")
    }

    fun loggingEveryStep(i: Int) {
        println("Before if")
        if (i > 0) {
            println("Inside if")
            println("Before for")
            for (j in 0..i) {
                println("In loop j=$j")
            }
            println("After for")
        }
        println("After if")
    }


    // CATEGORY: SECURITY - Security vulnerabilities
    fun securityIssues() {
        val input = readLine()
        val query = "SELECT * FROM users WHERE id = " + input // SQL Injection
        println(query)

        val cmd = "ls " + input // Command injection
        Runtime.getRuntime().exec(cmd)

        val path = input // Path traversal
        println("Accessing: $path")

        var password: String? = null
        println(password!!) // NPE risk

        val secret = "password123" // Hardcoded secret
    }

    fun xssExample(html: String) {
        val output = "<div>$html</div>" // XSS vulnerability
        println(output)
    }


    // CATEGORY: PERFORMANCE - Performance issues
    fun performanceProblems() {
        val list = (1..10000).toList()

        // Inefficient string concatenation in loop
        var result = ""
        for (item in list) {
            result = result + item + ","
        }

        // Nested loops
        for (i in list) {
            for (j in list) {
                println("$i, $j")
            }
        }

        // Inefficient list operations
        val filtered = list.filter { it > 5000 }.map { it * 2 }.toList()

        // Creating unnecessary objects in loop
        for (i in 0..1000) {
            val obj = StringBuilder()
            obj.append(i)
        }

        Thread.sleep(100) // Blocking in production code
    }

    fun regexInLoop() {
        val items = listOf("a", "b", "c", "d")
        for (item in items) {
            val pattern = Regex(".*$item.*")
            val matches = pattern.matches(item)
        }
    }


    // CATEGORY: FRAMEWORK MISUSE - Incorrect API usage
    fun frameworkMisuse() {
        val map = mutableMapOf<String, Int>()

        // Wrong way to check key existence
        val value = map["key"]
        if (value != null) {
            println(value)
        }

        // Using mutableListOf then adding in loop
        val items = mutableListOf<String>()
        for (i in 0..10) {
            items.add("item$i")
        }

        // String comparison mistake
        val str: String? = "test"
        if (str == "test") { // This works but inconsistent with === usage
            println("found")
        }
    }

    // CATEGORY: DUPLICATION - Repeated code
    fun duplication1() {
        val x = 10
        val y = 20
        val sum = x + y
        println("Sum: $sum")

        val a = 30
        val b = 40
        val result = a + b
        println("Sum: $result")
    }

    fun duplication2() {
        validateInput("test")
        validateInput("data")
        validateInput("value")
    }

    fun validateInput(input: String) {
        if (input.isEmpty()) {
            println("Empty")
        }
        if (input.length < 5) {
            println("Too short")
        }
        if (input.contains(" ")) {
            println("Contains space")
        }
    }


    // CATEGORY: COMPLEXITY - Overall code complexity
    fun highlyComplex() {
        val conditions = listOf(true, false, true, false, true)
        for (condition in conditions) {
            if (condition) {
                when (condition) {
                    true -> {
                        if (condition && !condition) {
                            println("Branch A")
                        } else if (!condition || condition) {
                            println("Branch B")
                        } else {
                            println("Branch C")
                        }
                    }
                    false -> {
                        println("False branch")
                    }
                }
            }
        }
    }
}

class User(
    val name: String,
    val age: Int,
    val isActive: Boolean = true,
    val hasPermission: Boolean = true,
    val role: Role? = Role.USER,
    val isVerified: Boolean = false
)

enum class Role {
    ADMIN, USER, GUEST
}

fun readLine(): String = ""
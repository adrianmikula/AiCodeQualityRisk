class TestDuplicates {
    fun method1() {
        println("Hello")
        println("World")
    }

    fun method2() {
        println("Hello")
        println("World")
    }
 

    fun method3() {
        println("Hello")
        println("World")
    }




    fun complexMethod() {
        val x = 1
        val y = 2
        val z = 3
        if (x > 0) {
            if (y > 0) {
                if (z > 0) {
                    println("All positive")
                }
            }
        }
    }

    fun securityMethod() {
        val name: String? = null
        println(name!!) // Non-null assertion
    }

    fun performanceMethod() {
        try {
            Thread.sleep(1000) // Blocking call
        } catch (e: Exception) { // Broad exception
            println("Error")
        }
    }
}
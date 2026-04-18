/**
 * AllRiskCategoriesDemo - A Java class demonstrating all 10 code quality risk categories.
 * Each category should trigger a non-zero score when analyzed.
 */
public class AllRiskCategoriesDemo {

    // ============================================================
    // 1. BOILERPLATE BLOAT: averageMethodLength > 50, maxMethodLength > 100
    //    duplicateStringLiteralCount > 3
    // ============================================================



    public void veryLongMethodWithBoilerplate() {
        String message1 = "Processing data";
        String message2 = "Processing data";
        String message3 = "Processing data";
        String message4 = "Processing data";
        
        int temp1 = 1;
        temp1 = temp1;
        int temp2 = 2;
        temp2 = temp2;
        
        Integer result1 = calculateSomething(temp1);
        Integer result2 = calculateSomething(temp2);
        Integer result3 = calculateSomething(3);
        Integer result4 = calculateSomething(4);
        Integer result5 = calculateSomething(5);
        
        System.out.println(message1);
        System.out.println(message2);
        System.out.println(message3);
        System.out.println(message4);
        
        if (result1 != null) {
            if (result2 != null) {
                if (result3 != null) {
                    if (result4 != null) {
                        System.out.println("All results valid");
                    }
                }
            }
        }
        
        // More boilerplate to push method length over 100 lines
        for (int i = 0; i < 50; i++) {
            System.out.println("Iteration " + i);
            int x = i;
            x = x + 1;
            int y = x;
            y = y;
            int z = y;
            z = z;
        }
    }

    public void anotherVeryLongMethodForBoilerplate() {
        String data = "Starting up";
        String dup1 = "Starting up";
        String dup2 = "Starting up";
        String dup3 = "Starting up";
        
        for (int i = 0; i < 10; i++) {
            System.out.println(data);
            System.out.println(dup1);
            System.out.println(dup2);
            System.out.println(dup3);
        }
        
        doProcess();
        doProcess();
        doProcess();
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
    public void heavilyCommentedFunction() {
        // Initialize the variable
        int x = 10;
        // Add 20 to x
        int y = x + 20;
        // Print the result
        System.out.println(y);
    }

    /* Block comment 1 */
    /* Block comment 2 */
    /* Block comment 3 */
    /* Block comment 4 */
    /* Block comment 5 */
    /* Block comment 6 */
    public void blockCommentHeavy() {
        // More comments
        // More comments
        // More comments
    }

    // ============================================================
    // 3. OVER-DEFENSIVE: duplicateMethodCallCount > 2
    // ============================================================
    
    public void defensiveProgramming() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        list.size();
        list.size();
        list.size();
        
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.get("a");
        map.get("a");
        map.get("b");
        map.get("b");
    }

    // ============================================================
    // 4. MAGIC NUMBERS: magicNumberCount > 0
    // ============================================================
    
    public void magicNumbersExample() {
        double rate = 1.15;
        double discount = 0.9;
        double shipping = 5.99;
        int timeout = 30000;
        int maxRetries = 3;
        int buffer = 1024;
        int port = 8080;
        
        double total = 100.0 * rate - discount * 100 + shipping;
        System.out.println(total);
        
        try {
            Thread.sleep(5000); // 5000 is a magic number
        } catch (InterruptedException e) {
        }
    }

    // ============================================================
    // 5. COMPLEX BOOLEAN: booleanOperatorCount > 5, maxElseIfChainLength > 2
    // ============================================================
    
    public void complexBooleanLogic() {
        boolean a = true;
        boolean b = false;
        boolean c = true;
        
        if (a && b || c && !a || b && !c || (a || b) && (c || b) && !a) {
            System.out.println("Complex");
        }
        
        if (a && b && c || !a && !b && !c || a && !b || !a && b) {
            System.out.println("More");
        }
    }

    public void longElseIfChain() {
        int x = 3;
        if (x == 1) { System.out.println("one"); }
        else if (x == 2) { System.out.println("two"); }
        else if (x == 3) { System.out.println("three"); }
        else if (x == 4) { System.out.println("four"); }
        else if (x == 5) { System.out.println("five"); }
    }

    // ============================================================
    // 6. DEEP NESTING: maxNestingDepth > 3
    // ============================================================
    
    public void deeplyNestedConditions() {
        DataWrapper data = getData();
        if (data != null) {
            if (data.isValid()) {
                if (data.getContent() != null) {
                    if (!data.getContent().isEmpty()) {
                        for (ItemClass item : data.getContent()) {
                            if (item != null) {
                                if (item.isReady()) {
                                    System.out.println(item.getName());
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
    
    public void lotsOfLogging() {
        System.out.println("Starting");
        System.out.println("Step 1");
        System.out.println("Step 2");
        System.out.println("Step 3");
        System.out.println("Step 4");
        System.out.println("Step 5");
        System.out.println("Step 6");
        System.out.println("Step 7");
        System.out.println("Step 8");
        System.out.println("Step 9");
        System.out.println("Step 10");
        System.out.println("Complete");
    }

    // ============================================================
    // 8. POOR NAMING: hardcodedConfigLiteralCount > 2
    // ============================================================
    
    public void hardcodedUrls() {
        String apiUrl = "https://api.example.com";
        String dbUrl = "jdbc:mysql://localhost:3306/db";
        String adminEmail = "admin@example.com";
        String basePath = "/home/user";
    }

    // ============================================================
    // 9. FRAMEWORK MISUSE: broadCatchCount > 0, emptyCatchCount > 0
    // ============================================================
    
    public void exceptionProblems() {
        try {
            riskyOperation();
        } catch (Exception e) {
            // Empty catch
        }
        
        try {
            anotherRiskyCall();
        } catch (Throwable e) {
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
    public void processUserData(User user) {
        /** Validates the user input */
        if (!user.getName().isEmpty()) {
            /** Processes the name */
            System.out.println(user.getName());
        }
    }

    /**
     * Another method with Javadoc.
     * More documentation here.
     * Even more documentation.
     * And some more.
     */
    public void anotherMethod() {}

    /**
     * Yet another Javadoc.
     */
    public void yetAnother() {}

    /**
     * More documentation.
     */
    public void moreDocs() {}

    /**
     * Even more methods.
     */
    public void evenMore() {}

    // Helper methods
    public Integer calculateSomething(int x) { return x; }
    public void doProcess() {}
    public void riskyOperation() { throw new RuntimeException(); }
    public void anotherRiskyCall() { throw new Error(); }
    public DataWrapper getData() { return null; }
}

/**
 * User data class representing a user in the system.
 * Contains name and age properties.
 */
class User {
    private String name;
    private int age;
    
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
}

/**
 * Data wrapper class for containing content items.
 * Provides access to validity and content properties.
 */
class DataWrapper {
    private boolean valid;
    private java.util.List<ItemClass> content;
    
    public DataWrapper(boolean valid, java.util.List<ItemClass> content) {
        this.valid = valid;
        this.content = content;
    }
    
    public boolean isValid() { return valid; }
    public java.util.List<ItemClass> getContent() { return content; }
}

/**
 * Item class representing a single content item.
 * Has name and ready status properties.
 */
class ItemClass {
    private String name;
    private boolean ready;
    
    public ItemClass(String name, boolean ready) {
        this.name = name;
        this.ready = ready;
    }
    
    public String getName() { return name; }
    public boolean isReady() { return ready; }
}
package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class EntropyScoresTest {
    private val detector = TreeSitterFuzzyDetector()

    @Test
    fun `entropy scores are enabled`() {
        val code = """
            public class Example {
                public void methodA() {
                    System.out.println("Hello");
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.entropyScoresEnabled, "Entropy scores should be enabled")
    }

    @Test
    fun `detects boilerplate bloat - getter setter heavy class`() {
        val code = """
            public class UserDTO {
                private String firstName;
                private String lastName;
                private String email;
                private String phone;
                private String address;
                private String city;
                private String state;
                private String zipCode;
                private String country;
                private int age;
                private String occupation;
                private String company;
                private String department;
                private long salary;
                private String startDate;
                private String endDate;
                private boolean active;
                private String notes;
                private String preferences;
                private String lastLogin;

                public String getFirstName() { return firstName; }
                public void setFirstName(String val) { this.firstName = val; }
                public String getLastName() { return lastName; }
                public void setLastName(String val) { this.lastName = val; }
                public String getEmail() { return email; }
                public void setEmail(String val) { this.email = val; }
                public String getPhone() { return phone; }
                public void setPhone(String val) { this.phone = val; }
                public String getAddress() { return address; }
                public void setAddress(String val) { this.address = val; }
                public String getCity() { return city; }
                public void setCity(String val) { this.city = val; }
                public String getState() { return state; }
                public void setState(String val) { this.state = val; }
                public String getZipCode() { return zipCode; }
                public void setZipCode(String val) { this.zipCode = val; }
                public String getCountry() { return country; }
                public void setCountry(String val) { this.country = val; }
                public int getAge() { return age; }
                public void setAge(int val) { this.age = val; }
                public String getOccupation() { return occupation; }
                public void setOccupation(String val) { this.occupation = val; }
                public String getCompany() { return company; }
                public void setCompany(String val) { this.company = val; }
                public String getDepartment() { return department; }
                public void setDepartment(String val) { this.department = val; }
                public long getSalary() { return salary; }
                public void setSalary(long val) { this.salary = val; }
                public String getStartDate() { return startDate; }
                public void setStartDate(String val) { this.startDate = val; }
                public String getEndDate() { return endDate; }
                public void setEndDate(String val) { this.endDate = val; }
                public boolean isActive() { return active; }
                public void setActive(boolean val) { this.active = val; }
                public String getNotes() { return notes; }
                public void setNotes(String val) { this.notes = val; }
                public String getPreferences() { return preferences; }
                public void setPreferences(String val) { this.preferences = val; }
                public String getLastLogin() { return lastLogin; }
                public void setLastLogin(String val) { this.lastLogin = val; }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/UserDTO.java")

        assertTrue(metrics.boilerplateBloatScore > 0.3,
            "Should detect boilerplate bloat in getter/setter heavy class, score: ${metrics.boilerplateBloatScore}")
    }

    @Test
    fun `no boilerplate bloat in normal class`() {
        val code = """
            public class BusinessLogic {
                public int calculateSum(List<Integer> numbers) {
                    int sum = 0;
                    for (int num : numbers) {
                        sum += num;
                    }
                    return sum;
                }

                public boolean isPrime(int n) {
                    if (n <= 1) return false;
                    for (int i = 2; i <= Math.sqrt(n); i++) {
                        if (n % i == 0) return false;
                    }
                    return true;
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/BusinessLogic.java")

        assertEquals(0.0, metrics.boilerplateBloatScore,
            "Should not detect boilerplate in normal business logic class")
    }

    @Test
    fun `detects verbose comments`() {
        val code = """
            public class Example {
                // This method does something
                // It takes a parameter
                // And returns a result
                // Please note the implementation
                public int process(int x) {
                    // Check if x is positive
                    if (x > 0) {
                        // Multiply by 2
                        return x * 2;
                    }
                    // Return original value
                    return x;
                }

                // Another method
                // For demonstration
                // Very important
                public void helper() {
                    // Print message
                    System.out.println("help");
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.verboseCommentScore > 0.0,
            "Should detect verbose comments, score: ${metrics.verboseCommentScore}")
    }

    @Test
    fun `detects over defensive coding`() {
        val code = """
            public class Example {
                public void process(String data, Integer count, List<String> items) {
                    if (data != null) {
                        if (count != null) {
                            if (items != null) {
                                if (!items.isEmpty()) {
                                    for (String item : items) {
                                        if (item != null) {
                                            if (item.length() > 0) {
                                                System.out.println(item);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.overDefensiveScore > 0.0,
            "Should detect over-defensive coding with excessive null checks, score: ${metrics.overDefensiveScore}")
    }

    @Test
    fun `detects poor naming`() {
        val code = """
            public class Example {
                public int calc(int a, int b, int c) {
                    int x = a + b;
                    int y = x * c;
                    int z = y - a;
                    return z;
                }

                public void proc(String s, List<String> l) {
                    for (String i : l) {
                        if (i.equals(s)) {
                            System.out.println(i);
                        }
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.poorNamingScore > 0.0,
            "Should detect poor naming with single-letter variables, score: ${metrics.poorNamingScore}")
    }

    @Test
    fun `good naming has low poor naming score`() {
        val code = """
            public class Example {
                public int calculateTotal(int basePrice, int quantity, int taxRate) {
                    int subtotal = basePrice * quantity;
                    int tax = subtotal * taxRate / 100;
                    int finalAmount = subtotal + tax;
                    return finalAmount;
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertEquals(0.0, metrics.poorNamingScore,
            "Good naming should have zero poor naming score")
    }

    @Test
    fun `detects framework misuse`() {
        val code = """
            public class Example {
                public void riskyMethod1() {
                    try {
                        doSomething();
                    } catch (Exception e) {
                        // ignore
                    }
                }

                public void riskyMethod2() {
                    try {
                        doAnotherThing();
                    } catch (Throwable t) {
                        // silently swallow
                    }
                }

                public void riskyMethod3() {
                    try {
                        riskyOperation();
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.frameworkMisuseScore > 0.0,
            "Should detect framework misuse with broad exception catching, score: ${metrics.frameworkMisuseScore}")
    }

    @Test
    fun `detects excessive documentation`() {
        val code = """
            /**
             * This is a utility class.
             * It provides various helper methods.
             * Use it wisely.
             * Please read the documentation.
             */
            public class Example {
                /**
                 * This method adds two numbers.
                 * @param a the first number
                 * @param b the second number
                 * @return the sum of a and b
                 * @throws IllegalArgumentException if numbers are invalid
                 * @deprecated use addNumbers instead
                 */
                public int add(int a, int b) {
                    return a + b;
                }

                /**
                 * This method subtracts two numbers.
                 * @param a the first number
                 * @param b the second number
                 * @return the difference
                 */
                public int subtract(int a, int b) {
                    return a - b;
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.excessiveDocumentationScore > 0.0,
            "Should detect excessive documentation, score: ${metrics.excessiveDocumentationScore}")
    }

    @Test
    fun `normal documentation has low score`() {
        val code = """
            public class Example {
                public int add(int a, int b) {
                    return a + b;
                }

                public int subtract(int a, int b) {
                    return a - b;
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertEquals(0.0, metrics.excessiveDocumentationScore,
            "Code without javadoc should have zero excessive documentation score")
    }

    @Test
    fun `all entropy scores are in valid range`() {
        val code = """
            public class Example {
                /**
                 * Helper method
                 */
                public void process(int x) {
                    if (x > 0) {
                        System.out.println(x);
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        // All scores should be between 0.0 and 1.0
        assertTrue(metrics.boilerplateBloatScore in 0.0..1.0,
            "Boilerplate bloat score should be in valid range: ${metrics.boilerplateBloatScore}")
        assertTrue(metrics.verboseCommentScore in 0.0..1.0,
            "Verbose comment score should be in valid range: ${metrics.verboseCommentScore}")
        assertTrue(metrics.overDefensiveScore in 0.0..1.0,
            "Over defensive score should be in valid range: ${metrics.overDefensiveScore}")
        assertTrue(metrics.poorNamingScore in 0.0..1.0,
            "Poor naming score should be in valid range: ${metrics.poorNamingScore}")
        assertTrue(metrics.frameworkMisuseScore in 0.0..1.0,
            "Framework misuse score should be in valid range: ${metrics.frameworkMisuseScore}")
        assertTrue(metrics.excessiveDocumentationScore in 0.0..1.0,
            "Excessive documentation score should be in valid range: ${metrics.excessiveDocumentationScore}")
    }

    @Test
    fun `entropy scores returned even with single method`() {
        val code = """
            public class Example {
                /**
                 * A well documented single method
                 * @param data the input data
                 * @return the processed result
                 */
                public String process(String data) {
                    if (data != null) {
                        return data.toUpperCase();
                    }
                    return null;
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/Example.java")

        assertTrue(metrics.entropyScoresEnabled, "Entropy scores should be enabled even with single method")
        assertTrue(metrics.excessiveDocumentationScore > 0.0,
            "Should still calculate entropy scores with single method")
    }

    @Test
    fun `comprehensive entropy detection`() {
        val code = """
            /**
             * Data Transfer Object for user information.
             * This class holds user data.
             * @author Developer
             * @version 1.0
             */
            public class UserDTO {
                private String n;  // user's name
                private int a;     // user's age
                private String e;  // email

                /**
                 * Gets the name.
                 * @return the name
                 */
                public String getN() { return n; }

                /**
                 * Sets the name.
                 * @param val the new name
                 */
                public void setN(String val) { this.n = val; }

                /**
                 * Gets the age.
                 * @return the age
                 */
                public int getA() { return a; }

                /**
                 * Sets the age.
                 * @param val the new age
                 */
                public void setA(int val) { this.a = val; }

                /**
                 * Gets the email.
                 * @return the email
                 */
                public String getE() { return e; }

                /**
                 * Sets the email.
                 * @param val the new email
                 */
                public void setE(String val) { this.e = val; }

                /**
                 * Validates the user data.
                 * @return true if valid
                 */
                public boolean validate() {
                    try {
                        if (n != null && e != null) {
                            return n.length() > 0 && e.contains("@");
                        }
                        return false;
                    } catch (Exception ex) {
                        return false;
                    }
                }
            }
        """.trimIndent()

        val metrics = detector.detect(code, "/tmp/UserDTO.java")

        assertTrue(metrics.entropyScoresEnabled, "Entropy should be enabled")
        assertTrue(metrics.boilerplateBloatScore > 0.0, "Should detect boilerplate")
        assertTrue(metrics.poorNamingScore > 0.0, "Should detect poor naming (single letter fields)")
        assertTrue(metrics.frameworkMisuseScore > 0.0, "Should detect framework misuse (catch Exception)")
        assertTrue(metrics.excessiveDocumentationScore > 0.0, "Should detect excessive documentation")
    }
}

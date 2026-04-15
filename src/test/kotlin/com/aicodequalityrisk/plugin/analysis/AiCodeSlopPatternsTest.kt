package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AiCodeSlopPatternsTest {
    private val analyzer = ASTAnalyzer()

    @Test
    fun testFieldCount() {
        val src = "public class A { int a; int b; int c; }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.fieldCount >= 3)
    }

    @Test
    fun testManualGetterSetterBloat() {
        val src = "public class UserDTO {" +
            "private String firstName;" +
            "private String lastName;" +
            "private String email;" +
            "private String phone;" +
            "private String address;" +
            "private String city;" +
            "private String state;" +
            "private String zipCode;" +
            "private String country;" +
            "private int age;" +
            "private String occupation;" +
            "private String company;" +
            "private String department;" +
            "private long salary;" +
            "private String startDate;" +
            "private String endDate;" +
            "private boolean active;" +
            "private String notes;" +
            "private String preferences;" +
            "private String lastLogin;" +
            "public String getFirstName() { return firstName; }" +
            "public void setFirstName(String val) { this.firstName = val; }" +
            "public String getLastName() { return lastName; }" +
            "public void setLastName(String val) { this.lastName = val; }" +
            "public String getEmail() { return email; }" +
            "public void setEmail(String val) { this.email = val; }" +
            "public String getPhone() { return phone; }" +
            "public void setPhone(String val) { this.phone = val; }" +
            "public String getAddress() { return address; }" +
            "public void setAddress(String val) { this.address = val; }" +
            "public String getCity() { return city; }" +
            "public void setCity(String val) { this.city = val; }" +
            "public String getState() { return state; }" +
            "public void setState(String val) { this.state = val; }" +
            "public String getZipCode() { return zipCode; }" +
            "public void setZipCode(String val) { this.zipCode = val; }" +
            "public String getCountry() { return country; }" +
            "public void setCountry(String val) { this.country = val; }" +
            "public int getAge() { return age; }" +
            "public void setAge(int val) { this.age = val; }" +
            "public String getOccupation() { return occupation; }" +
            "public void setOccupation(String val) { this.occupation = val; }" +
            "public String getCompany() { return company; }" +
            "public void setCompany(String val) { this.company = val; }" +
            "public String getDepartment() { return department; }" +
            "public void setDepartment(String val) { this.department = val; }" +
            "public long getSalary() { return salary; }" +
            "public void setSalary(long val) { this.salary = val; }" +
            "public String getStartDate() { return startDate; }" +
            "public void setStartDate(String val) { this.startDate = val; }" +
            "public String getEndDate() { return endDate; }" +
            "public void setEndDate(String val) { this.endDate = val; }" +
            "public boolean isActive() { return active; }" +
            "public void setActive(boolean val) { this.active = val; }" +
            "public String getNotes() { return notes; }" +
            "public void setNotes(String val) { this.notes = val; }" +
            "public String getPreferences() { return preferences; }" +
            "public void setPreferences(String val) { this.preferences = val; }" +
            "public String getLastLogin() { return lastLogin; }" +
            "public void setLastLogin(String val) { this.lastLogin = val; }" +
            "}"

        val result = analyzer.analyzeCode(src)
        assertTrue(result.fieldCount >= 20)
    }

    @Test
    fun testEmptyCatchBlocks() {
        val src = "public class A { void m1() { try { } catch (Exception e) {} } void m2() { try { } catch (Exception e) {} } void m3() { try { } catch (Exception e) {} } }"
        val result = analyzer.analyzeCode(src)
        assertEquals(3, result.emptyCatchCount)
        assertTrue(result.hasEmptyCatchBlock)
    }

    @Test
    fun testCatchingThrowable() {
        val src = "public class A { void m() { try { } catch (Throwable t) { } } void m2() { try { } catch (Throwable t) { } } }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.broadCatchCount >= 2)
    }

    @Test
    fun testHardcodedApiKeys() {
        val src = "public class A { void m() { String x = \"sk-1234567890abcdefghijklmnopqrstuvwxyz\"; String y = \"super_secret_key_12345\"; String z = \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\"; } }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.hardcodedConfigLiteralCount >= 1)
    }

    @Test
    fun testMagicNumbers() {
        val src = "public class A { void m() { int a = 30000; int b = 5; int c = 1024; int d = 65536; double e = 0.15; } }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.magicNumberCount >= 5)
    }

    @Test
    fun testDeepNesting() {
        val src = "public class A { void m(Object o) { if (o != null) { if (o instanceof String) { String s = (String)o; if (s.length() > 0) { if (s.length() < 100) { if (!s.isEmpty()) { if (s.trim().length() > 0) { System.out.println(s); } } } } } } } }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.maxNestingDepth >= 5)
    }

    @Test
    fun testLongIfElseChain() {
        val src = "public class A { String m(int c) { if (c == 200) return \"OK\"; else if (c == 201) return \"Created\"; else if (c == 400) return \"Bad\"; else if (c == 401) return \"Unauth\"; else if (c == 403) return \"Forbidden\"; else if (c == 404) return \"NotFound\"; else if (c == 500) return \"Err\"; else if (c == 503) return \"Unavail\"; return \"X\"; } }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.maxElseIfChainLength >= 1)
    }

    @Test
    fun testHeavyBooleanLogic() {
        val src = "public class A { boolean m(String n, int a, String e, boolean w) { return n != null && n.length() > 2 && n.length() < 50 && a > 0 && a < 150 && e != null && e.contains(\"@\") && e.contains(\".\") && (w || a < 18); } }"
        val result = analyzer.analyzeCode(src)
        assertTrue(result.booleanOperatorCount >= 8)
    }
}
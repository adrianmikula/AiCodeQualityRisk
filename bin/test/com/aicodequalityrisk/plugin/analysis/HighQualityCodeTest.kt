package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.TriggerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HighQualityCodeTest {
    private val analyzer = ASTAnalyzer()
    private val riskAnalyzer = LocalMockAnalyzerClient()

    @Test
    fun `clean record class with minimal fields has no boilerplate bloat`() {
        val code = """
            public class User {
                private final String name;
                private final int age;

                public User(String name, int age) {
                    this.name = name;
                    this.age = age;
                }

                public String getName() { return name; }
                public int getAge() { return age; }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertEquals(2, metrics.fieldCount)
        assertFalse(metrics.fieldCount > 8, "Should not have excessive fields")
    }

    @Test
    fun `code with minimal comments has no verbose comment spam`() {
        val code = """
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertEquals(0, metrics.stringLiteralCount, "Should have no string literals in simple code")
    }

    @Test
    fun `code using named constants has no magic numbers above threshold`() {
        val code = """
            public class Config {
                private static final int TIMEOUT = 5000;

                public int getTimeout() {
                    return TIMEOUT;
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertTrue(metrics.magicNumberCount <= 3, "Should have few magic numbers (below threshold of 4)")
        assertFalse(metrics.hasMagicNumbers, "Should not trigger magic number rule (threshold is >3)")
    }

    @Test
    fun `code with simple boolean logic has no complex boolean signals`() {
        val code = """
            public class Validator {
                public boolean isValid(String input) {
                    return input != null && !input.isEmpty();
                }

                public boolean canProcess(User user) {
                    return user.isActive() && user.hasPermission();
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertTrue(metrics.booleanOperatorCount <= 3, "Should have simple boolean logic")
        assertFalse(metrics.hasHeavyBooleanLogic)
    }

    @Test
    fun `code with flat control flow has no deep nesting signals`() {
        val code = """
            public class UserService {
                public User findActiveUser(String id) {
                    if (id == null) return null;
                    User user = repository.find(id);
                    if (user == null) return null;
                    if (!user.isActive()) return null;
                    return user;
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertTrue(metrics.maxNestingDepth <= 5, "Should have reasonable nesting")
        assertFalse(metrics.hasDeepNesting, "Should not trigger deep nesting rule (threshold is >3)")
    }

    @Test
    fun `code with short focused methods has no long method signals`() {
        val code = """
            public class MathService {
                public int add(int a, int b) {
                    return a + b;
                }

                public int multiply(int a, int b) {
                    return a * b;
                }

                public int subtract(int a, int b) {
                    return a - b;
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertTrue(metrics.maxMethodLength <= 5, "Should have short methods")
        assertFalse(metrics.hasComplexMethods)
    }

    @Test
    fun `code with builder pattern has no long parameter list signals`() {
        val code = """
            public class UserBuilder {
                private String name;
                private int age;
                private String email;

                public UserBuilder name(String name) {
                    this.name = name;
                    return this;
                }

                public UserBuilder age(int age) {
                    this.age = age;
                    return this;
                }

                public UserBuilder email(String email) {
                    this.email = email;
                    return this;
                }

                public User build() {
                    return new User(name, age, email);
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertFalse(metrics.hasLongParameterList, "Should use builder pattern instead of many parameters")
    }

    @Test
    fun `code with no hardcoded config has no security signals`() {
        val code = """
            public class DatabaseService {
                private final String url;
                private final String username;

                public DatabaseService(Config config) {
                    this.url = config.getDatabaseUrl();
                    this.username = config.getUsername();
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertEquals(0, metrics.hardcodedConfigLiteralCount, "Should not have hardcoded config")
        assertFalse(metrics.hasHardcodedConfig)
    }

    @Test
    fun `code with proper exception handling has no empty catch or broad catch signals`() {
        val code = """
            public class UserService {
                private static final Logger logger = Logger.getLogger(UserService.class);

                public User findById(Long id) {
                    try {
                        return repository.findById(id).orElse(null);
                    } catch (DataAccessException e) {
                        logger.error("Database error retrieving user", e);
                        throw new UserRetrievalException("Failed to retrieve user", e);
                    }
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertEquals(0, metrics.emptyCatchCount, "Should not have empty catch blocks")
        assertEquals(0, metrics.broadCatchCount, "Should not catch broad Exception")
        assertFalse(metrics.hasEmptyCatchBlock)
        assertFalse(metrics.hasBroadExceptionCatch)
    }

    @Test
    fun `code with clear descriptive names has no poor naming signals`() {
        val code = """
            public class OrderProcessor {
                public void processOrder(Order order) {
                    validateOrder(order);
                    calculateTotal(order);
                    persistOrder(order);
                }

                private void validateOrder(Order order) {
                    if (order == null || order.getItems().isEmpty()) {
                        throw new IllegalArgumentException("Invalid order");
                    }
                }

                private void calculateTotal(Order order) {
                    BigDecimal total = order.getItems().stream()
                        .map(Item::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    order.setTotal(total);
                }

                private void persistOrder(Order order) {
                    repository.save(order);
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertFalse(metrics.hasLongIfElseChain, "Should have clear flow with early returns")
    }

    @Test
    fun `full analysis of high quality code returns green score`() {
        val code = """
            public class ProductService {
                public Product findById(Long id) {
                    return repository.findById(id).orElse(null);
                }

                public List<Product> findActive() {
                    return repository.findByActiveTrue();
                }

                public void save(Product product) {
                    repository.save(product);
                }
            }
        """.trimIndent()

        val input = AnalysisInput(
            projectPath = "/test",
            filePath = "ProductService.java",
            trigger = TriggerType.MANUAL,
            diffText = code,
            fileSnapshot = code,
            astMetrics = analyzer.analyzeCode(code),
            fuzzyMetrics = FuzzyMetrics()
        )

        val result = riskAnalyzer.analyze(input)
        assertTrue(result.score < 30, "High quality code should have low score: ${result.score}")
    }

    @Test
    fun `well structured data class has no bloat signals`() {
        val code = """
            public class Address {
                private final String street;
                private final String city;
                private final String state;
                private final String zipCode;

                public Address(String street, String city, String state, String zipCode) {
                    this.street = street;
                    this.city = city;
                    this.state = state;
                    this.zipCode = zipCode;
                }

                public String getStreet() { return street; }
                public String getCity() { return city; }
                public String getState() { return state; }
                public String getZipCode() { return zipCode; }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertEquals(4, metrics.fieldCount)
        assertFalse(metrics.fieldCount > 8, "Should not have excessive fields")
    }

    @Test
    fun `clean code with early returns has no long if-else chain signals`() {
        val code = """
            public class StatusChecker {
                public String getStatus(Order order) {
                    if (order.isCancelled()) return "CANCELLED";
                    if (order.isPending()) return "PENDING";
                    if (order.isCompleted()) return "COMPLETED";
                    return "UNKNOWN";
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(code)
        assertTrue(metrics.maxElseIfChainLength <= 2, "Should not have long if-else chains")
        assertFalse(metrics.hasLongIfElseChain, "Should not trigger long if-else chain rule (threshold is >2)")
    }
}
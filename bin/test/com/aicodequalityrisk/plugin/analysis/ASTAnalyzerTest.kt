package com.aicodequalityrisk.plugin.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ASTAnalyzerTest {
    private val analyzer = ASTAnalyzer()

    @Test
    fun `detects repeated string and numeric literals and repeated method calls`() {
        val source = """
            public class Example {
                public void run() {
                    System.out.println("HELLO");
                    System.out.println("HELLO");
                    System.out.println("HELLO");
                    int x = 42;
                    int y = 42;
                    int z = 42;
                    doWork();
                    doWork();
                    doWork();
                    doWork();
                }

                public void doWork() {}
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)
        println("METRICS: $metrics")

        assertEquals(1, metrics.duplicateStringLiteralCount)
        assertEquals(1, metrics.duplicateNumberLiteralCount)
        assertTrue(metrics.duplicateMethodCallCount >= 1)
        assertTrue(metrics.hasRepeatedMethodCalls)
    }

    @Test
    fun `detects broad exception catch and empty catch block`() {
        val source = """
            import java.io.IOException;

            public class Example {
                public void run() {
                    try { System.out.println("ok"); } catch (Exception e) {}
                    try { System.out.println("ok"); } catch (Throwable t) {}
                    try { System.out.println("ok"); } catch (IOException e) { System.out.println(e.getMessage()); }
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        assertEquals(2, metrics.broadCatchCount)
        assertEquals(2, metrics.emptyCatchCount)
        assertTrue(metrics.hasBroadExceptionCatch)
        assertTrue(metrics.hasEmptyCatchBlock)
    }

    @Test
    fun `detects heavy boolean logic and long if else chain`() {
        val source = """
            public class Example {
                public void run(boolean a, boolean b, boolean c, boolean d, boolean e) {
                    if (a && b || c && d || e) {
                        System.out.println("ok");
                    } else if (b) {
                        System.out.println("b");
                    } else if (c) {
                        System.out.println("c");
                    } else if (d) {
                        System.out.println("d");
                    }
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        assertTrue(metrics.booleanOperatorCount >= 3)
        assertTrue(metrics.hasHeavyBooleanLogic)
        assertEquals(4, metrics.maxElseIfChainLength)
        assertTrue(metrics.hasLongIfElseChain)
    }

    @Test
    fun `detects long parameter lists and large class state`() {
        val source = """
            public class Example {
                private int a;
                private int b;
                private int c;
                private int d;
                private int e;
                private int f;
                private int g;
                private int h;
                private int i;

                public void run(int a, int b, int c, int d, int e, int f) {
                    System.out.println(a + b + c + d + e + f);
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        assertEquals(9, metrics.fieldCount)
        assertTrue(metrics.hasLongParameterList)
        assertEquals(6, metrics.maxParameterCount)
    }

    @Test
    fun `detects hardcoded configuration and magic numbers`() {
        val source = """
            public class Example {
                public void run() {
                    String endpoint = "https://api.example.com/v1/resource";
                    String secret = "mySecretToken";
                    int timeout = 5000;
                    int retries = 3;
                    int attempts = 3;
                    int limit = 42;
                }
            }
        """.trimIndent()

        val metrics = analyzer.analyzeCode(source)

        assertTrue(metrics.hardcodedConfigLiteralCount >= 1)
        assertTrue(metrics.hasHardcodedConfig)
        assertTrue(metrics.magicNumberCount >= 3)
        assertTrue(metrics.hasMagicNumbers)
    }
}

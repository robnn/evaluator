package hu.robnn.evaluator.java

import hu.robnn.evaluator.EvaluatorFactory
import hu.robnn.evaluator.ProgrammingLanguage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JavaEvaluatorTest {

    @Test
    fun test1() {
        val evaluator = EvaluatorFactory.getEvaluatorForLanguage(ProgrammingLanguage.JAVA)
        val classes = mutableMapOf("Alma" to "public class Alma { public Alma() { } public int add(Integer a, Integer b) { return a + b; } }",
                "AlmaTest" to "public class AlmaTest { public AlmaTest() {} public boolean testAdd() { Alma a = new Alma(); " +
                        "System.out.println(\"Hello\"); " +
                        " return a.add(1,2) == 3; } }")
        val result = evaluator.evaluate(classes, "AlmaTest", "testAdd", mutableListOf(), mutableListOf())
        assertEquals(true, result.methodCallResult?.result)
        assertEquals("Hello\n", result.methodCallResult?.output)
    }
}
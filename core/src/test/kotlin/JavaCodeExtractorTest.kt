import org.cubewhy.reguard.core.decompiler.JavaCodeExtractor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaCodeExtractorTest {

    private val extractor = JavaCodeExtractor()

    @Test
    fun `should extract simple class with method and field`() {
        val source = """
            public class HelloWorld {
                private int count = 5;

                public void greet() {
                    System.out.println("Hello");
                }
            }
        """.trimIndent()

        val result = extractor.extractFromSource(source)
        assertEquals(result.size, 1)

        val clazz = result.first()
        assertEquals(clazz.className, "HelloWorld")
        assertTrue(clazz.fields.any { it.name == "count" && it.code.contains("int count = 5") })
        assertTrue(clazz.methods.any { it.name == "greet" && it.code.contains("System.out.println") })
    }

    @Test
    fun `should extract constructor and static block`() {
        val source = """
            public class Sample {
                static {
                    System.loadLibrary("native");
                }

                public Sample() {
                    System.out.println("Constructor");
                }
            }
        """.trimIndent()

        val result = extractor.extractFromSource(source)
        val clazz = result.first()
        assertEquals(clazz.constructors.size, 1)
        assertTrue(clazz.staticInitializers.any { it.contains("System.loadLibrary") })
    }

    @Test
    fun `should extract nested classes`() {
        val source = """
            public class Outer {
                public class Inner {
                    public void say() {}
                }
            }
        """.trimIndent()

        val result = extractor.extractFromSource(source)
        assertEquals(result.size, 1)
        val outer = result.first()
        assertEquals(outer.innerClasses.size, 1)
        assertEquals(outer.innerClasses.first().className, "Inner")
    }

    @Test
    fun `should extract enum with entries and methods`() {
        val source = """
            public enum Status {
                OK, ERROR;

                public boolean isOk() {
                    return this == OK;
                }
            }
        """.trimIndent()

        val result = extractor.extractFromSource(source)
        assertEquals(result.size, 1)

        val enum = result.first()
        assertEquals(enum.className, "Status")
        assertTrue(enum.fields.map { it.name }.containsAll(setOf("OK", "ERROR")))
        assertTrue(enum.methods.any { it.name == "isOk" })
    }

    @Test
    fun `should extract annotation with members`() {
        val source = """
            public @interface MyAnno {
                String value();
                int count() default 1;
            }
        """.trimIndent()

        val result = extractor.extractFromSource(source)
        assertEquals(result.size, 1)
        val anno = result.first()
        assertEquals(anno.className, "MyAnno")
        assertTrue(anno.methods.map{it.name}.containsAll(setOf("value", "count")))
    }

    @Test
    fun `should throw on invalid java`() {
        val source = "public class { malformed"

        assertThrows<RuntimeException> {
            extractor.extractFromSource(source)
        }
    }
}

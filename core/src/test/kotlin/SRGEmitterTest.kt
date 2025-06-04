import org.cubewhy.reguard.core.emitter.SRGEmitter
import org.cubewhy.reguard.script.core.ClassMapping
import org.cubewhy.reguard.script.core.FieldMapping
import org.cubewhy.reguard.script.core.MappingTree
import org.cubewhy.reguard.script.core.MethodMapping
import org.cubewhy.reguard.script.core.ResolvedParameter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SRGEmitterTest {

    @Test
    fun `test SRGEmitter with simple class`() {
        val mappingTree = MappingTree(
            packageName = "com.example",
            imports = emptyMap(),
            classes = listOf(
                ClassMapping(
                    originalName = "MyClass",
                    fallbackObfuscatedName = "com.example.a",
                    metadata = emptyMap(),
                    methods = listOf(
                        MethodMapping(
                            originalName = "doSomething",
                            obfuscatedName = "a",
                            parameters = listOf(
                                ResolvedParameter("int", false),
                                ResolvedParameter("java.lang.String", false)
                            ),
                            javadoc = null,
                            aliases = emptyList(),
                            metadata = emptyMap()
                        )
                    ),
                    fields = listOf(
                        FieldMapping(
                            originalName = "myField",
                            obfuscatedName = "b",
                            aliases = emptyList(),
                            metadata = emptyMap()
                        )
                    ),
                    innerClasses = emptyList(),
                    aliases = emptyList()
                )
            )
        )

        val expected = """
            CL: com/example/a com/example/MyClass
            FD: com/example/a/b com/example/MyClass/myField
            MD: com/example/a/a (ILjava/lang/String;)V com/example/MyClass/doSomething (ILjava/lang/String;)V

        """.trimIndent()

        val actual = SRGEmitter.emit(mappingTree)

        assertEquals(expected.trim(), actual.trim())
    }
}

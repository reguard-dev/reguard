import org.cubewhy.reguard.core.emitter.ProGuardEmitter
import org.cubewhy.reguard.script.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProGuardEmitterTest {

    @Test
    fun `emit should generate correct ProGuard format`() {
        val tree = MappingTree(
            packageName = "com.example",
            imports = emptyMap(),
            classes = listOf(
                ClassMapping(
                    originalName = "MyClass",
                    fallbackObfuscatedName = "com.example.a",
                    aliases = listOf(AliasEntry(version = "v2", obfuscatedName = "b")),
                    metadata = emptyMap(),
                    fields = listOf(
                        FieldMapping(
                            originalName = "myField",
                            obfuscatedName = "b",
                            aliases = emptyList(),
                            metadata = emptyMap()
                        )
                    ),
                    methods = listOf(
                        MethodMapping(
                            originalName = "doSomething",
                            obfuscatedName = "a",
                            aliases = emptyList(),
                            metadata = emptyMap(),
                            javadoc = null,
                            parameters = listOf(
                                ResolvedParameter("int", isVarargs = false),
                                ResolvedParameter("java.lang.String", isVarargs = true)
                            )
                        )
                    ),
                    innerClasses = emptyList()
                )
            )
        )

        val expected = """
            com.example.MyClass -> com.example.a:
                myField -> b
                doSomething(int,java.lang.String...) -> a

        """.trimIndent()

        val actual = ProGuardEmitter.emit(tree)

        assertEquals(expected.trimEnd(), actual.trimEnd())
    }

    @Test
    fun `emit should use alias version if provided`() {
        val tree = MappingTree(
            packageName = "com.example",
            imports = emptyMap(),
            classes = listOf(
                ClassMapping(
                    originalName = "MyClass",
                    fallbackObfuscatedName = "a",
                    aliases = listOf(AliasEntry(version = "v2", obfuscatedName = "com.example.c")),
                    metadata = emptyMap(),
                    fields = listOf(
                        FieldMapping(
                            originalName = "myField",
                            obfuscatedName = "b",
                            aliases = listOf(AliasEntry(version = "v2", obfuscatedName = "z")),
                            metadata = emptyMap()
                        )
                    ),
                    methods = listOf(
                        MethodMapping(
                            originalName = "doSomething",
                            obfuscatedName = "a",
                            aliases = listOf(AliasEntry(version = "v2", obfuscatedName = "x")),
                            metadata = emptyMap(),
                            javadoc = null,
                            parameters = listOf(
                                ResolvedParameter("int", isVarargs = false)
                            )
                        )
                    ),
                    innerClasses = emptyList()
                )
            )
        )

        val expected = """
            com.example.MyClass -> com.example.c:
                myField -> z
                doSomething(int) -> x

        """.trimIndent()

        val actual = ProGuardEmitter.emit(tree, aliasVersion = "v2")

        assertEquals(expected.trimEnd(), actual.trimEnd())
    }
}

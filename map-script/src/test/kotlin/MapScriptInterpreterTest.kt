import org.cubewhy.reguard.script.core.ClassDeclaration
import org.cubewhy.reguard.script.core.FieldDeclaration
import org.cubewhy.reguard.script.core.ImportDeclaration
import org.cubewhy.reguard.script.core.MappingFile
import org.cubewhy.reguard.script.core.MethodDeclaration
import org.cubewhy.reguard.script.core.PackageDeclaration
import org.cubewhy.reguard.script.core.Parameter
import org.cubewhy.reguard.script.interpreter.MapScriptInterpreter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MapScriptInterpreterTest {

    @Test
    fun `test interpret basic`() {
        val ast = MappingFile(
            packageDecl = PackageDeclaration("com.example"),
            imports = listOf(
                ImportDeclaration("java.util.List", null),
                ImportDeclaration("kotlin.collections.Map", "KMap")
            ),
            classes = listOf(
                ClassDeclaration(
                    name = "MyClass",
                    obfuscatedName = "com.example.a",
                    metadata = mapOf("type" to "class"),
                    methods = listOf(
                        MethodDeclaration(
                            name = "foo",
                            obfuscatedName = "com.example.b",
                            parameters = listOf(Parameter("List", false)),
                            javadoc = "Test method",
                            aliases = emptyList(),
                            metadata = mapOf("deprecated" to "false")
                        )
                    ),
                    fields = listOf(
                        FieldDeclaration(
                            name = "bar",
                            obfuscatedName = "c",
                            aliases = emptyList(),
                            metadata = emptyMap()
                        )
                    ),
                    innerClasses = emptyList(),
                    aliases = emptyList()
                )
            )
        )

        val interpreter = MapScriptInterpreter()
        val result = interpreter.interpret(ast)

        assertEquals("com.example", result.packageName)
        assertEquals(2, result.imports.size)
        assertEquals("java.util.List", result.imports["List"])
        assertEquals("kotlin.collections.Map", result.imports["KMap"])

        val clazz = result.classes.first()
        assertEquals("MyClass", clazz.originalName)
        assertEquals("com.example.a", clazz.fallbackObfuscatedName)
        assertEquals("class", clazz.metadata["type"])

        assertEquals(1, clazz.methods.size)
        val method = clazz.methods[0]
        assertEquals("foo", method.originalName)
        assertEquals("com.example.b", method.obfuscatedName)
        assertEquals("Test method", method.javadoc)
        assertEquals("false", method.metadata["deprecated"])
        assertEquals(1, method.parameters.size)
        assertEquals("java.util.List", method.parameters[0].type)
        assertFalse(method.parameters[0].isVarargs)

        assertEquals(1, clazz.fields.size)
        val field = clazz.fields[0]
        assertEquals("bar", field.originalName)
        assertEquals("c", field.obfuscatedName)
    }
}
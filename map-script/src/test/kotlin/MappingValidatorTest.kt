import org.cubewhy.reguard.script.core.AliasEntry
import org.cubewhy.reguard.script.core.ClassMapping
import org.cubewhy.reguard.script.core.FieldMapping
import org.cubewhy.reguard.script.core.MappingTree
import org.cubewhy.reguard.script.core.MethodMapping
import org.cubewhy.reguard.script.core.ResolvedParameter
import org.cubewhy.reguard.script.validate.ErrorType
import org.cubewhy.reguard.script.validate.MappingValidator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MappingValidatorTest {

    private val validator = MappingValidator()

    private fun makeAlias(version: String) = AliasEntry(version, "obf", emptyMap(), false)

    @Test
    fun testNoErrorsWhenUnique() {
        val tree = MappingTree(
            packageName = "test",
            imports = emptyMap(),
            classes = listOf(
                ClassMapping(
                    originalName = "A",
                    fallbackObfuscatedName = "com.example.a",
                    metadata = emptyMap(),
                    methods = listOf(
                        MethodMapping(
                            originalName = "foo",
                            obfuscatedName = "f",
                            parameters = emptyList(),
                            javadoc = null,
                            aliases = listOf(makeAlias("v1")),
                            metadata = emptyMap()
                        )
                    ),
                    fields = listOf(
                        FieldMapping(
                            originalName = "bar",
                            obfuscatedName = "b",
                            aliases = listOf(makeAlias("v1")),
                            metadata = emptyMap()
                        )
                    ),
                    innerClasses = emptyList(),
                    aliases = emptyList()
                )
            )
        )
        val errors = validator.validate(tree)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testDuplicateClassNames() {
        val tree = MappingTree(
            packageName = "test",
            imports = emptyMap(),
            classes = listOf(
                ClassMapping("A", "com.example.a", emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
                ClassMapping("A", "com.example.b", emptyMap(), emptyList(), emptyList(), emptyList(), emptyList())
            )
        )
        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_CLASS && it.message.contains("Duplicate class name") })
    }

    @Test
    fun testDuplicateObfuscatedClassNames() {
        val tree = MappingTree(
            packageName = "test",
            imports = emptyMap(),
            classes = listOf(
                ClassMapping("A", "com.example.x", emptyMap(), emptyList(), emptyList(), emptyList(), emptyList()),
                ClassMapping("B", "com.example.x", emptyMap(), emptyList(), emptyList(), emptyList(), emptyList())
            )
        )
        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_OBFUSCATED_CLASS && it.message.contains("Duplicate obfuscated class name") })
    }

    @Test
    fun testDuplicateMethodSignatures() {
        val method1 = MethodMapping(
            originalName = "foo",
            obfuscatedName = "f1",
            parameters = listOf(ResolvedParameter("int", false)),
            javadoc = null,
            aliases = emptyList(),
            metadata = emptyMap()
        )
        val method2 = MethodMapping(
            originalName = "foo",
            obfuscatedName = "f2",
            parameters = listOf(ResolvedParameter("int", false)),
            javadoc = null,
            aliases = emptyList(),
            metadata = emptyMap()
        )
        val classMapping = ClassMapping(
            originalName = "A",
            fallbackObfuscatedName = "com.example.a",
            metadata = emptyMap(),
            methods = listOf(method1, method2),
            fields = emptyList(),
            innerClasses = emptyList(),
            aliases = emptyList()
        )
        val tree = MappingTree("test", emptyMap(), listOf(classMapping))
        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_METHOD && it.message.contains("Duplicate method signature") })
    }

    @Test
    fun testDuplicateObfuscatedMethodNames() {
        val method1 = MethodMapping(
            originalName = "foo1",
            obfuscatedName = "f",
            parameters = emptyList(),
            javadoc = null,
            aliases = emptyList(),
            metadata = emptyMap()
        )
        val method2 = MethodMapping(
            originalName = "foo2",
            obfuscatedName = "f",
            parameters = emptyList(),
            javadoc = null,
            aliases = emptyList(),
            metadata = emptyMap()
        )
        val classMapping = ClassMapping(
            originalName = "A",
            fallbackObfuscatedName = "a",
            metadata = emptyMap(),
            methods = listOf(method1, method2),
            fields = emptyList(),
            innerClasses = emptyList(),
            aliases = emptyList()
        )
        val tree = MappingTree("test", emptyMap(), listOf(classMapping))
        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_OBFUSCATED_METHOD && it.message.contains("Duplicate obfuscated method name") })
    }

    @Test
    fun testDuplicateFieldNames() {
        val field1 = FieldMapping("bar", "b1", emptyList(), emptyMap())
        val field2 = FieldMapping("bar", "b2", emptyList(), emptyMap())
        val classMapping = ClassMapping(
            originalName = "A",
            fallbackObfuscatedName = "a",
            metadata = emptyMap(),
            methods = emptyList(),
            fields = listOf(field1, field2),
            innerClasses = emptyList(),
            aliases = emptyList()
        )
        val tree = MappingTree("test", emptyMap(), listOf(classMapping))
        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_FIELD && it.message.contains("Duplicate field name") })
    }

    @Test
    fun testDuplicateObfuscatedFieldNames() {
        val field1 = FieldMapping("bar1", "b", emptyList(), emptyMap())
        val field2 = FieldMapping("bar2", "b", emptyList(), emptyMap())
        val classMapping = ClassMapping(
            originalName = "A",
            fallbackObfuscatedName = "a",
            metadata = emptyMap(),
            methods = emptyList(),
            fields = listOf(field1, field2),
            innerClasses = emptyList(),
            aliases = emptyList()
        )
        val tree = MappingTree("test", emptyMap(), listOf(classMapping))
        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_OBFUSCATED_FIELD && it.message.contains("Duplicate obfuscated field name") })
    }

    @Test
    fun testDuplicateAliasVersions() {
        val alias1 = AliasEntry("v1", "obf1", emptyMap(), false)
        val alias2 = AliasEntry("v1", "obf2", emptyMap(), false)

        val method = MethodMapping(
            originalName = "foo",
            obfuscatedName = "f",
            parameters = emptyList(),
            javadoc = null,
            aliases = listOf(alias1, alias2),
            metadata = emptyMap()
        )
        val classMapping = ClassMapping(
            originalName = "A",
            fallbackObfuscatedName = "a",
            metadata = emptyMap(),
            methods = listOf(method),
            fields = emptyList(),
            innerClasses = emptyList(),
            aliases = emptyList()
        )
        val tree = MappingTree("test", emptyMap(), listOf(classMapping))

        val errors = validator.validate(tree)
        assertTrue(errors.any { it.type == ErrorType.DUPLICATE_VERSION && it.message.contains("Duplicate version") })
    }
}

import org.cubewhy.reguard.script.core.AliasEntry
import org.cubewhy.reguard.script.core.ClassMapping
import org.cubewhy.reguard.script.core.FieldMapping
import org.cubewhy.reguard.script.core.MappingTree
import org.cubewhy.reguard.script.core.MethodMapping
import org.cubewhy.reguard.script.parser.MappingLookup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MappingLookupTest {

    private fun createTestMappingTree(): MappingTree {
        val field1 = FieldMapping(
            originalName = "fieldA",
            obfuscatedName = "fA",
            aliases = emptyList(),
            metadata = emptyMap()
        )
        val field2 = FieldMapping(
            originalName = "fieldB",
            obfuscatedName = "fB",
            aliases = listOf(AliasEntry("v1", "fB_v1", emptyMap(), false)),
            metadata = emptyMap()
        )
        val method1 = MethodMapping(
            originalName = "methodX",
            obfuscatedName = "mX",
            parameters = emptyList(),
            javadoc = null,
            aliases = emptyList(),
            metadata = emptyMap()
        )
        val method2 = MethodMapping(
            originalName = "methodY",
            obfuscatedName = "mY",
            parameters = emptyList(),
            javadoc = null,
            aliases = listOf(AliasEntry("v2", "mY_v2", emptyMap(), false)),
            metadata = emptyMap()
        )
        val class1 = ClassMapping(
            originalName = "com.example.Class1",
            fallbackObfuscatedName = "a",
            metadata = emptyMap(),
            methods = listOf(method1, method2),
            fields = listOf(field1, field2),
            innerClasses = emptyList(),
            aliases = listOf(AliasEntry("v1", "a_v1", emptyMap(), false))
        )
        val class2 = ClassMapping(
            originalName = "com.example.Class2",
            fallbackObfuscatedName = "b",
            metadata = emptyMap(),
            methods = emptyList(),
            fields = emptyList(),
            innerClasses = emptyList(),
            aliases = emptyList()
        )
        return MappingTree(
            packageName = "com.example",
            imports = emptyMap(),
            classes = listOf(class1, class2)
        )
    }

    private val lookup = MappingLookup(createTestMappingTree())

    @Test
    fun testFindOriginalClassName() {
        assertEquals("com.example.Class1", lookup.findOriginalClassName("a"))
        assertEquals("com.example.Class2", lookup.findOriginalClassName("b"))
        assertNull(lookup.findOriginalClassName("nonexistent"))
    }

    @Test
    fun testFindObfuscatedClassName() {
        assertEquals("a", lookup.findObfuscatedClassName("com.example.Class1"))
        assertEquals("b", lookup.findObfuscatedClassName("com.example.Class2"))
        assertNull(lookup.findObfuscatedClassName("nonexistent"))
    }

    @Test
    fun testFindMethodMapping() {
        val mX = lookup.findMethodMapping("com.example.Class1", "methodX")
        assertNotNull(mX)
        assertEquals("mX", mX?.obfuscatedName)

        val mY = lookup.findMethodMapping("com.example.Class1", "mY")
        assertNotNull(mY)
        assertEquals("methodY", mY?.originalName)

        assertNull(lookup.findMethodMapping("com.example.Class1", "nonexistent"))
        assertNull(lookup.findMethodMapping("nonexistent", "methodX"))
    }

    @Test
    fun testFindFieldMapping() {
        val fA = lookup.findFieldMapping("com.example.Class1", "fieldA")
        assertNotNull(fA)
        assertEquals("fA", fA?.obfuscatedName)

        val fB = lookup.findFieldMapping("com.example.Class1", "fB")
        assertNotNull(fB)
        assertEquals("fieldB", fB?.originalName)

        assertNull(lookup.findFieldMapping("com.example.Class1", "nonexistent"))
        assertNull(lookup.findFieldMapping("nonexistent", "fieldA"))
    }

    @Test
    fun testFindByVersion() {
        val v1Aliases = lookup.findByVersion("v1")
        assertTrue(v1Aliases.any { it.version == "v1" })
        assertTrue(v1Aliases.any { it.version == "v1" && it.obfuscatedName == "a_v1" })

        val v2Aliases = lookup.findByVersion("v2")
        assertTrue(v2Aliases.any { it.version == "v2" })
        assertFalse(v2Aliases.any { it.version == "v2" && it.obfuscatedName == "a_v1" })

        val none = lookup.findByVersion("nonexistent")
        assertTrue(none.isEmpty())
    }

    @Test
    fun testGetAllVersions() {
        val versions = lookup.getAllVersions()
        assertTrue(versions.contains("v1"))
        assertTrue(versions.contains("v2"))
        assertFalse(versions.contains("nonexistent"))
    }
}

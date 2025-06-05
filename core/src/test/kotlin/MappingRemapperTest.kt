import io.mockk.*
import org.cubewhy.reguard.core.decompiler.MappingRemapper
import org.cubewhy.reguard.script.core.FieldMapping
import org.cubewhy.reguard.script.core.MethodMapping
import org.cubewhy.reguard.script.parser.MappingLookup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MappingRemapperTest {

    private lateinit var lookup: MappingLookup
    private lateinit var remapper: MappingRemapper

    @BeforeEach
    fun setUp() {
        lookup = mockk(relaxed = true)
        remapper = MappingRemapper(lookup)
    }

    @Test
    fun `map class name when mapping exists`() {
        every { lookup.findObfuscatedClassName("com.example.Foo") } returns "a.b.C"

        val result = remapper.map("com/example/Foo")

        assertEquals("a/b/C", result)
    }

    @Test
    fun `map class name falls back when mapping missing`() {
        every { lookup.findObfuscatedClassName("com.example.Foo") } returns null

        val result = remapper.map("com/example/Foo")

        assertEquals("com/example/Foo", result)
    }

    @Test
    fun `map method name when mapping exists`() {
        every { lookup.findMethodMapping("com.example.Foo", "doSomething") } returns
                MethodMapping("doSomething", "a", emptyList(), null, emptyList(), emptyMap())

        val result = remapper.mapMethodName("com/example/Foo", "doSomething", "()V")

        assertEquals("a", result)
    }

    @Test
    fun `map method name falls back when mapping missing`() {
        every { lookup.findMethodMapping("com.example.Foo", "doSomething") } returns null

        val result = remapper.mapMethodName("com/example/Foo", "doSomething", "()V")

        assertEquals("doSomething", result)
    }

    @Test
    fun `map field name when mapping exists`() {
        every { lookup.findFieldMapping("com.example.Foo", "value") } returns
                FieldMapping("value", "x", emptyList(), emptyMap())

        val result = remapper.mapFieldName("com/example/Foo", "value", "I")

        assertEquals("x", result)
    }

    @Test
    fun `map field name falls back when mapping missing`() {
        every { lookup.findFieldMapping("com.example.Foo", "value") } returns null

        val result = remapper.mapFieldName("com/example/Foo", "value", "I")

        assertEquals("value", result)
    }

    @Test
    fun `useVersion calls mappingLookup`() {
        every { lookup.useVersion("v1") } just Runs

        remapper.useVersion("v1")

        verify { lookup.useVersion("v1") }
    }
}

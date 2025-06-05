import kotlinx.coroutines.runBlocking
import org.cubewhy.reguard.core.decompiler.impl.VineFlowerDecompiler
import org.cubewhy.reguard.script.parser.emptyMappingLookup
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class VineFlowerDecompilerTest {

    private val decompiler = VineFlowerDecompiler()

    @Test
    fun `decompile asm generated class`() {
        runBlocking {
            val classNode = ClassNode().apply {
                version = V1_8
                access = ACC_PUBLIC
                name = "TestAsmClass"
                superName = "java/lang/Object"

                // public int testField;
                fields.add(FieldNode(ACC_PUBLIC, "testField", "I", null, null))

                // public TestAsmClass() { super(); }
                methods.add(MethodNode(ACC_PUBLIC, "<init>", "()V", null, null).apply {
                    instructions.apply {
                        add(org.objectweb.asm.tree.InsnNode(ALOAD)) // this
                        add(
                            org.objectweb.asm.tree.MethodInsnNode(
                                INVOKESPECIAL,
                                "java/lang/Object",
                                "<init>",
                                "()V",
                                false
                            )
                        )
                        add(org.objectweb.asm.tree.InsnNode(RETURN))
                    }
                    maxStack = 1
                    maxLocals = 1
                })

                // public void testMethod() { return; }
                methods.add(MethodNode(ACC_PUBLIC, "testMethod", "()V", null, null).apply {
                    instructions.apply {
                        add(org.objectweb.asm.tree.InsnNode(RETURN))
                    }
                    maxStack = 0
                    maxLocals = 1
                })
            }

            val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            classNode.accept(cw)
            val classBytes = cw.toByteArray()

            // call VineFlowerDecompiler
            val decompiledClasses = decompiler.decompile(classBytes, emptyMappingLookup())

            assertTrue(decompiledClasses.isNotEmpty(), "Decompiled classes should not be empty")

            val decompiledClass = decompiledClasses.find { it.className == "TestAsmClass" }
            assertNotNull(decompiledClass, "Should find TestAsmClass")

            decompiledClass!!.apply {
                assertTrue(fields.any { it.name == "testField" }, "Should contain testField")
                assertTrue(methods.any { it.name == "testMethod" }, "Should contain testMethod")
                assertTrue(constructors.isNotEmpty(), "Should contain constructor")
            }
        }
    }
}

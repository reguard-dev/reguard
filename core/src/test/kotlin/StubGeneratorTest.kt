import org.cubewhy.reguard.core.stub.StubGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import java.lang.reflect.InvocationTargetException

class StubGeneratorTest {

    private val stubGenerator = StubGenerator()

    private fun generateMockClassBytes(): ByteArray {
        val cn = ClassNode().apply {
            version = Opcodes.V1_8
            access = Opcodes.ACC_PUBLIC
            name = "MockClass"
            superName = "java/lang/Object"
            interfaces = emptyList()
        }

        val ctor = MethodNode(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        ctor.instructions.apply {
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false))
            add(InsnNode(Opcodes.RETURN))
        }
        ctor.maxStack = 1
        ctor.maxLocals = 1
        cn.methods.add(ctor)

        // public void doSomething()
        val doSomething = MethodNode(
            Opcodes.ACC_PUBLIC,
            "doSomething",
            "()V",
            null,
            null
        )
        doSomething.instructions.add(InsnNode(Opcodes.RETURN))
        doSomething.maxStack = 0
        doSomething.maxLocals = 1
        cn.methods.add(doSomething)

        // public int calculate(int x)
        val calculate = MethodNode(
            Opcodes.ACC_PUBLIC,
            "calculate",
            "(I)I",
            null,
            null
        )
        calculate.instructions.apply {
            add(VarInsnNode(Opcodes.ILOAD, 1))
            add(InsnNode(Opcodes.IRETURN))
        }
        calculate.maxStack = 1
        calculate.maxLocals = 2
        cn.methods.add(calculate)

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        cn.accept(cw)
        return cw.toByteArray()
    }

    class DynamicClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        fun define(name: String, bytes: ByteArray): Class<*> {
            return defineClass(name, bytes, 0, bytes.size)
        }
    }

    @Test
    fun `test stub generation from asm tree mock class`() {
        val mockClassBytes = generateMockClassBytes()

        // generate stub
        val stubBytes = stubGenerator.generateStubClass(mockClassBytes.inputStream())

        // load stub
        val loader = DynamicClassLoader(this.javaClass.classLoader)
        val stubClass = loader.define("MockClass", stubBytes)

        // create instance
        val instance = stubClass.getDeclaredConstructor().newInstance()

        val doSomethingMethod = stubClass.getMethod("doSomething")
        val ex1 = assertThrows<InvocationTargetException> {
            doSomethingMethod.invoke(instance)
        }
        assertTrue(ex1.cause is UnsupportedOperationException)
        assertEquals("Stub!", ex1.cause?.message)

        val calculateMethod = stubClass.getMethod("calculate", Int::class.javaPrimitiveType)
        val ex2 = assertThrows<InvocationTargetException> {
            calculateMethod.invoke(instance, 123)
        }
        assertTrue(ex2.cause is UnsupportedOperationException)
        assertEquals("Stub!", ex2.cause?.message)
    }
}

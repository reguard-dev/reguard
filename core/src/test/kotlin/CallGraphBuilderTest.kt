import org.cubewhy.reguard.core.callgraph.CallGraphBuilder
import org.cubewhy.reguard.core.callgraph.MethodRef
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CallGraphBuilderTest {

    @Test
    fun `addClass should build correct call graph`() {
        val classA = ClassNode().apply {
            name = "com/example/A"
            methods = mutableListOf()

            val methodFoo = MethodNode(Opcodes.ACC_PUBLIC, "foo", "()V", null, null).apply {
                instructions = InsnList().apply {
                    add(MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "com/example/B",
                        "bar",
                        "()V",
                        false
                    ))
                    add(InsnNode(Opcodes.RETURN))
                }
            }

            methods.add(methodFoo)
        }

        val builder = CallGraphBuilder.newBuilder()
        builder.addClass(classA)

        val graph = builder.build()

        val from = MethodRef("com/example/A", "foo", "()V")
        val to = MethodRef("com/example/B", "bar", "()V")

        assertTrue(graph.containsKey(from), "Graph should contain the caller method")

        val callees = graph[from]
        assertNotNull(callees, "Callees should not be null")
        assertTrue(callees.contains(to), "Caller method should call callee method")
    }
}
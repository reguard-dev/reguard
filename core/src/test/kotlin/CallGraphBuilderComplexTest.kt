import org.cubewhy.reguard.core.callgraph.CallGraphBuilder
import org.cubewhy.reguard.core.callgraph.MethodRef
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallGraphBuilderComplexTest {

    private fun createMethod(
        access: Int,
        name: String,
        desc: String,
        calledMethods: List<MethodRef> = emptyList()
    ): MethodNode {
        val mn = MethodNode(access, name, desc, null, null)
        val insns = InsnList()
        for (callee in calledMethods) {
            insns.add(
                MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    callee.owner,
                    callee.name,
                    callee.desc,
                    false
                )
            )
        }
        insns.add(InsnNode(Opcodes.RETURN))
        mn.instructions = insns
        return mn
    }

    private fun createClassNode(
        className: String,
        methods: List<MethodNode>
    ): ClassNode {
        return ClassNode().apply {
            name = className
            this.methods = methods.toMutableList()
        }
    }

    @Test
    fun `complex call graph build test`() {
        val aFoo = MethodRef("A", "foo", "()V")
        val bBar = MethodRef("B", "bar", "()V")
        val bBaz = MethodRef("B", "baz", "()V")
        val cQux = MethodRef("C", "qux", "()V")

        val classA = createClassNode(
            "A",
            listOf(
                createMethod(Opcodes.ACC_PUBLIC, "foo", "()V", listOf(bBar, bBaz))
            )
        )

        val classB = createClassNode(
            "B",
            listOf(
                createMethod(Opcodes.ACC_PUBLIC, "bar", "()V", listOf(cQux)),
                createMethod(Opcodes.ACC_PUBLIC, "baz", "()V", emptyList())
            )
        )

        val classC = createClassNode(
            "C",
            listOf(
                createMethod(Opcodes.ACC_PUBLIC, "qux", "()V", listOf(aFoo))
            )
        )

        val builder = CallGraphBuilder.newBuilder()
        builder.addClass(classA)
            .addClass(classB)
            .addClass(classC)

        val graph = builder.build()

        assertTrue(graph.containsKey(aFoo))
        assertEquals(setOf(bBar, bBaz), graph[aFoo])

        assertTrue(graph.containsKey(bBar))
        assertEquals(setOf(cQux), graph[bBar])

        assertTrue(graph.containsKey(bBaz))
        assertTrue(graph[bBaz]!!.isEmpty())

        assertTrue(graph.containsKey(cQux))
        assertEquals(setOf(aFoo), graph[cQux])
    }
}
package org.cubewhy.reguard.core.callgraph

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

class CallGraphBuilder private constructor() {
    private val graph: MutableMap<MethodRef, MutableSet<MethodRef>> = mutableMapOf()

    companion object {
        fun newBuilder(): CallGraphBuilder {
            return CallGraphBuilder()
        }

        fun newBuilder(existGraph: Map<MethodRef, Set<MethodRef>>): CallGraphBuilder {
            return CallGraphBuilder(existGraph)
        }
    }

    private constructor(graph: Map<MethodRef, Set<MethodRef>>) : this() {
        graph.forEach { (key, valueSet) ->
            this.graph[key] = valueSet.toMutableSet()
        }
    }

    fun addClass(classNode: ClassNode): CallGraphBuilder {
        for (methodNode in classNode.methods) {
            val caller = MethodRef(classNode.name, methodNode.name, methodNode.desc)

            graph.putIfAbsent(caller, mutableSetOf())

            val instructions = methodNode.instructions
            for (insn in instructions) {
                if (insn is MethodInsnNode) {
                    val callee = MethodRef(insn.owner, insn.name, insn.desc)
                    graph.computeIfAbsent(caller) { mutableSetOf() }.add(callee)
                }
            }
        }
        return this
    }


    fun build(): Map<MethodRef, Set<MethodRef>> {
        return graph.toMap()
    }
}
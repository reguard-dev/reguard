package org.cubewhy.reguard.core.stub

import org.objectweb.asm.*
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream

class StubGenerator {

    fun generateStubClass(inputStream: InputStream): ByteArray {
        val classReader = ClassReader(inputStream)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

        return buildStubClass(classNode)
    }

    fun buildStubClass(classNode: ClassNode): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
        val cv = StubClassVisitor(classWriter, classNode)

        classNode.accept(cv)

        return classWriter.toByteArray()
    }

    private inner class StubClassVisitor(
        cv: ClassVisitor,
        private val originalClass: ClassNode
    ) : ClassVisitor(Opcodes.ASM9, cv) {

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            if (name == "<init>") {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                mv.visitCode()

                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    "java/lang/Object",
                    "<init>",
                    "()V",
                    false
                )

                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()
                return mv
            }

            if (name == "<clinit>") {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                mv.visitCode()
                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(0, 0)
                mv.visitEnd()
                return mv
            }

            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)

            if ((access and Opcodes.ACC_ABSTRACT) != 0 || (access and Opcodes.ACC_NATIVE) != 0 ||
                (originalClass.access and Opcodes.ACC_INTERFACE) != 0
            ) {
                return mv
            }

            mv.visitCode()

            mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException")
            mv.visitInsn(Opcodes.DUP)
            mv.visitLdcInsn("Stub!")
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/UnsupportedOperationException",
                "<init>",
                "(Ljava/lang/String;)V",
                false
            )
            mv.visitInsn(Opcodes.ATHROW)

            mv.visitMaxs(3, 1)
            mv.visitEnd()

            return mv
        }

        override fun visitField(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            val newAccess = access and Opcodes.ACC_FINAL.inv()
            return super.visitField(newAccess, name, descriptor, signature, value)
        }
    }
}
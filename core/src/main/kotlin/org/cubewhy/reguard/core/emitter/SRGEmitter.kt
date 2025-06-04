package org.cubewhy.reguard.core.emitter

import org.cubewhy.reguard.script.core.MappingTree
import org.cubewhy.reguard.script.core.ResolvedParameter
import org.cubewhy.reguard.script.utils.ifNullOrEmpty

object SRGEmitter {

    /**
     * Emits the SRG mapping string for the provided [mappingTree].
     *
     * The output format includes class (CL), field (FD), and method (MD) mappings.
     * Obfuscated names may vary by [version]; if the version-specific name is not found,
     * the default obfuscated name is used instead.
     *
     * @param mappingTree The mapping data to export.
     * @param version Optional version alias to resolve specific obfuscation versions.
     * @return The SRG-formatted mapping as a string.
     */
    fun emit(mappingTree: MappingTree, version: String? = null): String {
        val builder = StringBuilder()

        mappingTree.classes.forEach { cls ->
            val originalClass = buildFullClassName(mappingTree.packageName, cls.originalName)
            val obfuscatedClass = cls.getObfuscatedNameForVersion(version).let {
                if (!it.isNullOrEmpty()) it else cls.fallbackObfuscatedName.replace(".", "/")
            }

            builder.appendLine("CL: $obfuscatedClass $originalClass")

            cls.fields.forEach { field ->
                val obfField = field.getObfuscatedNameForVersion(version).ifNullOrEmpty { field.obfuscatedName }
                builder.appendLine("FD: $obfuscatedClass/$obfField $originalClass/${field.originalName}")
            }

            cls.methods.forEach { method ->
                val obfMethod = method.getObfuscatedNameForVersion(version).ifNullOrEmpty { method.obfuscatedName }
                val desc = buildMethodDescriptor(method.parameters)
                builder.appendLine("MD: $obfuscatedClass/$obfMethod $desc $originalClass/${method.originalName} $desc")
            }
        }
        return builder.toString()
    }

    /**
     * Builds the fully qualified class name in internal form (slash separated).
     *
     * @param packageName The package name prefix.
     * @param className The simple class name.
     * @return The slash-separated full class name, e.g. "org/cubewhy/Example".
     */
    private fun buildFullClassName(packageName: String, className: String): String {
        return if (packageName.isNotEmpty()) {
            "$packageName.$className"
        } else {
            className
        }.replace('.', '/')
    }

    /**
     * Constructs the method descriptor string based on parameter types.
     *
     * Note: This implementation assumes the return type is void ("V").
     *
     * @param parameters List of method parameters.
     * @return Method descriptor string, e.g. "(ILjava/lang/String;)V".
     */
    private fun buildMethodDescriptor(parameters: List<ResolvedParameter>): String {
        val paramDescriptors = parameters.joinToString("") { param ->
            (if (param.isVarargs) "[" else "") +
                    when (param.type) {
                        "byte" -> "B"
                        "char" -> "C"
                        "double" -> "D"
                        "float" -> "F"
                        "int" -> "I"
                        "long" -> "J"
                        "short" -> "S"
                        "boolean" -> "Z"
                        "void" -> "V"
                        else -> "L${param.type.replace('.', '/')};"
                    }
        }
        return "($paramDescriptors)V" // Assuming void return type for simplicity
    }
}
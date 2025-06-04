package org.cubewhy.reguard.core.emitter

import org.cubewhy.reguard.script.core.MappingTree

object ProGuardEmitter {
    /**
     * Export mapping tree to ProGuard format, optionally using specified alias version.
     *
     * @param aliasVersion the alias version to use for obfuscated names, or null to use fallback.
     */
    fun emit(mappingTree: MappingTree, aliasVersion: String? = null): String {
        val builder = StringBuilder()

        mappingTree.classes.forEach { classMapping ->
            val originalClass = if (mappingTree.packageName.isNotEmpty()) {
                "${mappingTree.packageName}.${classMapping.originalName}"
            } else {
                classMapping.originalName
            }

            val obfuscatedClass = classMapping.getObfuscatedNameForVersion(aliasVersion)
                ?: if (mappingTree.packageName.isNotEmpty()) {
                    classMapping.fallbackObfuscatedName
                } else {
                    classMapping.fallbackObfuscatedName
                }

            builder.appendLine("$originalClass -> $obfuscatedClass:")

            // Fields
            classMapping.fields.forEach { field ->
                val obfFieldName = field.getObfuscatedNameForVersion(aliasVersion) ?: field.obfuscatedName
                builder.appendLine("    ${field.originalName} -> $obfFieldName")
            }

            // Methods
            classMapping.methods.forEach { method ->
                val obfMethodName = method.getObfuscatedNameForVersion(aliasVersion) ?: method.obfuscatedName
                val paramTypes = method.parameters.joinToString(",") { param ->
                    if (param.isVarargs) "${param.type}..." else param.type
                }
                builder.appendLine("    ${method.originalName}($paramTypes) -> $obfMethodName")
            }

            builder.appendLine()
        }

        return builder.toString()
    }
}
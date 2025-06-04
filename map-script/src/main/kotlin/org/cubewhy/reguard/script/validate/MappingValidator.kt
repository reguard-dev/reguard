package org.cubewhy.reguard.script.validate

import org.cubewhy.reguard.script.core.AliasEntry
import org.cubewhy.reguard.script.core.ClassMapping
import org.cubewhy.reguard.script.core.MappingTree

class MappingValidator {

    fun validate(mappingTree: MappingTree): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Check for duplicate class names
        val classNames = mutableSetOf<String>()
        val obfuscatedClassNames = mutableSetOf<String>()

        mappingTree.classes.forEach { classMapping ->
            if (!classNames.add(classMapping.originalName)) {
                errors.add(
                    ValidationError(
                        "Duplicate class name: ${classMapping.originalName}",
                        ErrorType.DUPLICATE_CLASS
                    )
                )
            }

            if (!obfuscatedClassNames.add(classMapping.fallbackObfuscatedName)) {
                errors.add(
                    ValidationError(
                        "Duplicate obfuscated class name: ${classMapping.fallbackObfuscatedName}",
                        ErrorType.DUPLICATE_OBFUSCATED_CLASS
                    )
                )
            }

            // Validate methods within class
            validateMethods(classMapping, errors)

            // Validate fields within class
            validateFields(classMapping, errors)
        }

        return errors
    }

    private fun validateMethods(classMapping: ClassMapping, errors: MutableList<ValidationError>) {
        val methodSignatures = mutableSetOf<String>()
        val obfuscatedMethodNames = mutableSetOf<String>()

        classMapping.methods.forEach { method ->
            val signature = "${method.originalName}(${method.parameters.joinToString(",") { it.type }})"
            if (!methodSignatures.add(signature)) {
                errors.add(
                    ValidationError(
                        "Duplicate method signature in ${classMapping.originalName}: $signature",
                        ErrorType.DUPLICATE_METHOD
                    )
                )
            }

            if (!obfuscatedMethodNames.add(method.obfuscatedName)) {
                errors.add(
                    ValidationError(
                        "Duplicate obfuscated method name in ${classMapping.originalName}: ${method.obfuscatedName}",
                        ErrorType.DUPLICATE_OBFUSCATED_METHOD
                    )
                )
            }

            // Validate aliases
            validateAliases(method.aliases, errors, "method ${classMapping.originalName}.${method.originalName}")
        }
    }

    private fun validateFields(classMapping: ClassMapping, errors: MutableList<ValidationError>) {
        val fieldNames = mutableSetOf<String>()
        val obfuscatedFieldNames = mutableSetOf<String>()

        classMapping.fields.forEach { field ->
            if (!fieldNames.add(field.originalName)) {
                errors.add(
                    ValidationError(
                        "Duplicate field name in ${classMapping.originalName}: ${field.originalName}",
                        ErrorType.DUPLICATE_FIELD
                    )
                )
            }

            if (!obfuscatedFieldNames.add(field.obfuscatedName)) {
                errors.add(
                    ValidationError(
                        "Duplicate obfuscated field name in ${classMapping.originalName}: ${field.obfuscatedName}",
                        ErrorType.DUPLICATE_OBFUSCATED_FIELD
                    )
                )
            }

            // Validate aliases
            validateAliases(field.aliases, errors, "field ${classMapping.originalName}.${field.originalName}")
        }
    }

    private fun validateAliases(aliases: List<AliasEntry>, errors: MutableList<ValidationError>, context: String) {
        val versions = mutableSetOf<String>()

        aliases.forEach { alias ->
            if (!versions.add(alias.version)) {
                errors.add(
                    ValidationError(
                        "Duplicate version ${alias.version} in $context",
                        ErrorType.DUPLICATE_VERSION
                    )
                )
            }
        }
    }
}
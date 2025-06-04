package org.cubewhy.reguard.script.parser

import org.cubewhy.reguard.script.core.*

class MappingLookup(private val mappingTree: MappingTree) {

    private val classLookup: Map<String, ClassMapping> = mappingTree.classes.associateBy { it.originalName }
    private val reverseClassLookup: Map<String, ClassMapping> = mappingTree.classes.associateBy { it.fallbackObfuscatedName }

    /**
     * Find original name from obfuscated name
     */
    fun findOriginalClassName(obfuscatedName: String): String? {
        return reverseClassLookup[obfuscatedName]?.originalName
    }

    /**
     * Find obfuscated name from original name
     */
    fun findObfuscatedClassName(originalName: String): String? {
        return classLookup[originalName]?.fallbackObfuscatedName
    }

    /**
     * Find method mapping by class and method name
     */
    fun findMethodMapping(className: String, methodName: String): MethodMapping? {
        val classMapping = classLookup[className] ?: reverseClassLookup[className]
        return classMapping?.methods?.find { it.originalName == methodName || it.obfuscatedName == methodName }
    }

    /**
     * Find field mapping by class and field name
     */
    fun findFieldMapping(className: String, fieldName: String): FieldMapping? {
        val classMapping = classLookup[className] ?: reverseClassLookup[className]
        return classMapping?.fields?.find { it.originalName == fieldName || it.obfuscatedName == fieldName }
    }

    /**
     * Find mapping by version (for aliases)
     */
    fun findByVersion(version: String): List<AliasEntry> {
        val results = mutableListOf<AliasEntry>()

        mappingTree.classes.forEach { classMapping ->
            results.addAll(classMapping.aliases.filter { it.version == version })
            classMapping.methods.forEach { method ->
                results.addAll(method.aliases.filter { it.version == version })
            }
            classMapping.fields.forEach { field ->
                results.addAll(field.aliases.filter { it.version == version })
            }
        }

        return results
    }

    /**
     * Get all available versions
     */
    fun getAllVersions(): Set<String> {
        val versions = mutableSetOf<String>()

        mappingTree.classes.forEach { classMapping ->
            versions.addAll(classMapping.aliases.map { it.version })
            classMapping.methods.forEach { method ->
                versions.addAll(method.aliases.map { it.version })
            }
            classMapping.fields.forEach { field ->
                versions.addAll(field.aliases.map { it.version })
            }
        }

        return versions
    }
}
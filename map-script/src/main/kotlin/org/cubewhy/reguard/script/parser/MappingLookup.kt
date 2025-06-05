package org.cubewhy.reguard.script.parser

import org.cubewhy.reguard.script.core.*

class MappingLookup(private val mappingTrees: List<MappingTree>, private var version: String? = null) {

    private val classLookup: Map<String, ClassMapping> = buildMap {
        mappingTrees.forEach { tree ->
            tree.classes.forEach { classMapping ->
                putIfAbsent(classMapping.originalName, classMapping)
            }
        }
    }

    private val reverseClassLookup: Map<String, ClassMapping> = buildMap {
        mappingTrees.forEach { tree ->
            tree.classes.forEach { classMapping ->
                putIfAbsent(classMapping.fallbackObfuscatedName, classMapping)
            }
        }
    }

    private val treeIndexedClassLookup: Map<Int, Map<String, ClassMapping>> =
        mappingTrees.mapIndexed { index, tree ->
            index to tree.classes.associateBy { it.originalName }
        }.toMap()

    private val treeIndexedReverseClassLookup: Map<Int, Map<String, ClassMapping>> =
        mappingTrees.mapIndexed { index, tree ->
            index to tree.classes.associateBy { it.fallbackObfuscatedName }
        }.toMap()

    constructor(mappingTree: MappingTree) : this(listOf(mappingTree))

    fun useVersion(version: String?) {
        this.version = version
    }

    fun findOriginalClassName(obfuscatedName: String): String? {
        val classMapping = reverseClassLookup[obfuscatedName] ?: return null

        if (version != null) {
            val versionAlias = classMapping.aliases.find {
                it.version == version && !it.isRemoved && it.obfuscatedName == obfuscatedName
            }
            if (versionAlias != null) {
                return classMapping.originalName
            }
        }

        return if (classMapping.fallbackObfuscatedName == obfuscatedName) {
            classMapping.originalName
        } else null
    }

    /**
     * Find obfuscated name from original name with version support
     */
    fun findObfuscatedClassName(originalName: String): String? {
        val classMapping = classLookup[originalName] ?: return null

        if (version != null) {
            val versionObfuscatedName = classMapping.getObfuscatedNameForVersion(version)
            if (versionObfuscatedName != null) {
                return versionObfuscatedName
            }
        }

        // use fallback
        return classMapping.fallbackObfuscatedName
    }

    /**
     * Find original name from obfuscated name with tree index and version support
     */
    fun findOriginalClassNameWithSource(obfuscatedName: String): Pair<String, Int>? {
        treeIndexedReverseClassLookup.forEach { (treeIndex, lookup) ->
            lookup[obfuscatedName]?.let { classMapping ->
                // check version
                if (version != null) {
                    val versionAlias = classMapping.aliases.find {
                        it.version == version && !it.isRemoved && it.obfuscatedName == obfuscatedName
                    }
                    if (versionAlias != null) {
                        return classMapping.originalName to treeIndex
                    }
                }

                // Fallback
                if (classMapping.fallbackObfuscatedName == obfuscatedName) {
                    return classMapping.originalName to treeIndex
                }
            }
        }
        return null
    }

    /**
     * Find obfuscated name from original name with tree index and version support
     */
    fun findObfuscatedClassNameWithSource(originalName: String): Pair<String, Int>? {
        treeIndexedClassLookup.forEach { (treeIndex, lookup) ->
            lookup[originalName]?.let { classMapping ->
                // match version
                if (version != null) {
                    val versionObfuscatedName = classMapping.getObfuscatedNameForVersion(version)
                    if (versionObfuscatedName != null) {
                        return versionObfuscatedName to treeIndex
                    }
                }

                // Fallback
                return classMapping.fallbackObfuscatedName to treeIndex
            }
        }
        return null
    }

    /**
     * Find all possible original names for an obfuscated name with version support
     */
    fun findAllOriginalClassNames(obfuscatedName: String): List<MappingResult> {
        val results = mutableListOf<MappingResult>()
        treeIndexedReverseClassLookup.forEach { (treeIndex, lookup) ->
            lookup[obfuscatedName]?.let { classMapping ->
                var matched = false

                // check version
                if (version != null) {
                    val versionAlias = classMapping.aliases.find {
                        it.version == version && !it.isRemoved && it.obfuscatedName == obfuscatedName
                    }
                    if (versionAlias != null) {
                        results.add(MappingResult(
                            originalName = classMapping.originalName,
                            obfuscatedName = versionAlias.obfuscatedName,
                            version = version,
                            treeIndex = treeIndex,
                            mappingTree = mappingTrees[treeIndex],
                            isVersionSpecific = true
                        ))
                        matched = true
                    }
                }

                // Fallback
                if (!matched && classMapping.fallbackObfuscatedName == obfuscatedName) {
                    results.add(MappingResult(
                        originalName = classMapping.originalName,
                        obfuscatedName = classMapping.fallbackObfuscatedName,
                        version = null,
                        treeIndex = treeIndex,
                        mappingTree = mappingTrees[treeIndex],
                        isVersionSpecific = false
                    ))
                }
            }
        }
        return results
    }

    /**
     * Find method mapping by class and method name with version support
     */
    fun findMethodMapping(className: String, methodName: String): MethodMapping? {
        val classMapping = classLookup[className] ?: reverseClassLookup[className] ?: return null

        return classMapping.methods.find { method ->
            // check origin name
            if (method.originalName == methodName) return@find true

            if (version != null) {
                val versionObfuscatedName = method.getObfuscatedNameForVersion(version)
                if (versionObfuscatedName == methodName) return@find true
            }

            // Fallback to default
            method.obfuscatedName == methodName
        }
    }

    /**
     * Find all method mappings across all trees with version support
     */
    fun findAllMethodMappings(className: String, methodName: String): List<MethodMappingResult> {
        val results = mutableListOf<MethodMappingResult>()

        treeIndexedClassLookup.forEach { (treeIndex, lookup) ->
            val classMapping = lookup[className] ?: treeIndexedReverseClassLookup[treeIndex]?.get(className)
            classMapping?.methods?.forEach { methodMapping ->
                var matched = false
                var actualObfuscatedName = methodMapping.obfuscatedName
                var isVersionSpecific = false

                // check origin name
                if (methodMapping.originalName == methodName) {
                    matched = true
                    // try to match version
                    if (version != null) {
                        val versionName = methodMapping.getObfuscatedNameForVersion(version)
                        if (versionName != null) {
                            actualObfuscatedName = versionName
                            isVersionSpecific = true
                        }
                    }
                } else {
                    // try to match version
                    if (version != null) {
                        val versionObfuscatedName = methodMapping.getObfuscatedNameForVersion(version)
                        if (versionObfuscatedName == methodName) {
                            matched = true
                            actualObfuscatedName = versionObfuscatedName
                            isVersionSpecific = true
                        }
                    }

                    // Fallback to default
                    if (!matched && methodMapping.obfuscatedName == methodName) {
                        matched = true
                    }
                }

                if (matched) {
                    results.add(MethodMappingResult(
                        methodMapping = methodMapping,
                        classMapping = classMapping,
                        actualObfuscatedName = actualObfuscatedName,
                        version = if (isVersionSpecific) version else null,
                        treeIndex = treeIndex,
                        mappingTree = mappingTrees[treeIndex],
                        isVersionSpecific = isVersionSpecific
                    ))
                }
            }
        }

        return results
    }

    /**
     * Find field mapping by class and field name with version support
     */
    fun findFieldMapping(className: String, fieldName: String): FieldMapping? {
        val classMapping = classLookup[className] ?: reverseClassLookup[className] ?: return null

        return classMapping.fields.find { field ->
            if (field.originalName == fieldName) return@find true

            if (version != null) {
                val versionObfuscatedName = field.getObfuscatedNameForVersion(version)
                if (versionObfuscatedName == fieldName) return@find true
            }

            // Fallback to default
            field.obfuscatedName == fieldName
        }
    }

    /**
     * Find all field mappings across all trees with version support
     */
    fun findAllFieldMappings(className: String, fieldName: String): List<FieldMappingResult> {
        val results = mutableListOf<FieldMappingResult>()

        treeIndexedClassLookup.forEach { (treeIndex, lookup) ->
            val classMapping = lookup[className] ?: treeIndexedReverseClassLookup[treeIndex]?.get(className)
            classMapping?.fields?.forEach { fieldMapping ->
                var matched = false
                var actualObfuscatedName = fieldMapping.obfuscatedName
                var isVersionSpecific = false

                if (fieldMapping.originalName == fieldName) {
                    matched = true
                    if (version != null) {
                        val versionName = fieldMapping.getObfuscatedNameForVersion(version)
                        if (versionName != null) {
                            actualObfuscatedName = versionName
                            isVersionSpecific = true
                        }
                    }
                } else {
                    if (version != null) {
                        val versionObfuscatedName = fieldMapping.getObfuscatedNameForVersion(version)
                        if (versionObfuscatedName == fieldName) {
                            matched = true
                            actualObfuscatedName = versionObfuscatedName
                            isVersionSpecific = true
                        }
                    }

                    if (!matched && fieldMapping.obfuscatedName == fieldName) {
                        matched = true
                    }
                }

                if (matched) {
                    results.add(FieldMappingResult(
                        fieldMapping = fieldMapping,
                        classMapping = classMapping,
                        actualObfuscatedName = actualObfuscatedName,
                        version = if (isVersionSpecific) version else null,
                        treeIndex = treeIndex,
                        mappingTree = mappingTrees[treeIndex],
                        isVersionSpecific = isVersionSpecific
                    ))
                }
            }
        }

        return results
    }

    /**
     * Find mapping by version (for aliases)
     */
    fun findByVersion(version: String): List<AliasEntry> {
        val results = mutableListOf<AliasEntry>()

        mappingTrees.forEach { tree ->
            tree.classes.forEach { classMapping ->
                results.addAll(classMapping.aliases.filter { it.version == version && !it.isRemoved })
                classMapping.methods.forEach { method ->
                    results.addAll(method.aliases.filter { it.version == version && !it.isRemoved })
                }
                classMapping.fields.forEach { field ->
                    results.addAll(field.aliases.filter { it.version == version && !it.isRemoved })
                }
            }
        }

        return results
    }

    /**
     * Find mappings by version with tree source information
     */
    fun findByVersionWithSource(version: String): List<AliasEntryResult> {
        val results = mutableListOf<AliasEntryResult>()

        mappingTrees.forEachIndexed { treeIndex, tree ->
            tree.classes.forEach { classMapping ->
                classMapping.aliases.filter { it.version == version && !it.isRemoved }.forEach { alias ->
                    results.add(AliasEntryResult(alias, treeIndex, tree, classMapping))
                }
                classMapping.methods.forEach { method ->
                    method.aliases.filter { it.version == version && !it.isRemoved }.forEach { alias ->
                        results.add(AliasEntryResult(alias, treeIndex, tree, classMapping, method))
                    }
                }
                classMapping.fields.forEach { field ->
                    field.aliases.filter { it.version == version && !it.isRemoved }.forEach { alias ->
                        results.add(AliasEntryResult(alias, treeIndex, tree, classMapping, fieldMapping = field))
                    }
                }
            }
        }

        return results
    }

    /**
     * Get all available versions across all mapping trees
     */
    fun getAllVersions(): Set<String> {
        val versions = mutableSetOf<String>()

        mappingTrees.forEach { tree ->
            tree.classes.forEach { classMapping ->
                versions.addAll(classMapping.aliases.filter { !it.isRemoved }.map { it.version })
                classMapping.methods.forEach { method ->
                    versions.addAll(method.aliases.filter { !it.isRemoved }.map { it.version })
                }
                classMapping.fields.forEach { field ->
                    versions.addAll(field.aliases.filter { !it.isRemoved }.map { it.version })
                }
            }
        }

        return versions
    }

    /**
     * Get versions by mapping tree
     */
    fun getVersionsByTree(): Map<Int, Set<String>> {
        return mappingTrees.mapIndexed { index, tree ->
            val versions = mutableSetOf<String>()
            tree.classes.forEach { classMapping ->
                versions.addAll(classMapping.aliases.filter { !it.isRemoved }.map { it.version })
                classMapping.methods.forEach { method ->
                    versions.addAll(method.aliases.filter { !it.isRemoved }.map { it.version })
                }
                classMapping.fields.forEach { field ->
                    versions.addAll(field.aliases.filter { !it.isRemoved }.map { it.version })
                }
            }
            index to versions
        }.toMap()
    }

    /**
     * Get mapping tree by index
     */
    fun getMappingTree(index: Int): MappingTree? {
        return mappingTrees.getOrNull(index)
    }

    /**
     * Get all mapping trees
     */
    fun getAllMappingTrees(): List<MappingTree> = mappingTrees

    /**
     * Get the number of mapping trees
     */
    fun getTreeCount(): Int = mappingTrees.size

    /**
     * Find best matching class mapping considering priority and version
     * Priority: Version Exact Match > Exact Match > Partial Match`
     */
    fun findBestClassMapping(name: String): ClassMapping? {
        if (version != null) {
            // Version Exact Match
            mappingTrees.forEach { tree ->
                tree.classes.forEach { classMapping ->
                    val hasVersionAlias = classMapping.aliases.any {
                        it.version == version && !it.isRemoved &&
                                (it.obfuscatedName == name || classMapping.originalName == name)
                    }
                    if (hasVersionAlias) {
                        return classMapping
                    }
                }
            }
        }

        // Exact Match
        classLookup[name]?.let { return it }
        reverseClassLookup[name]?.let { return it }

        // Partial Match
        val partialMatches = mappingTrees.flatMap { it.classes }
            .filter {
                it.originalName.contains(name, ignoreCase = true) ||
                        it.fallbackObfuscatedName.contains(name, ignoreCase = true) ||
                        it.aliases.any { alias ->
                            !alias.isRemoved && alias.obfuscatedName.contains(name, ignoreCase = true)
                        }
            }

        return partialMatches.firstOrNull()
    }

    fun isVersionSupported(originalName: String, version: String): Boolean {
        val classMapping = classLookup[originalName] ?: return false
        return classMapping.aliases.any { it.version == version && !it.isRemoved }
    }

    fun getAvailableNamesForVersion(originalClassName: String, version: String): ClassVersionInfo? {
        val classMapping = classLookup[originalClassName] ?: return null

        val classObfuscatedName = classMapping.getObfuscatedNameForVersion(version)
            ?: classMapping.fallbackObfuscatedName

        val methods = classMapping.methods.map { method ->
            val obfuscatedName = method.getObfuscatedNameForVersion(version) ?: method.obfuscatedName
            VersionMemberInfo(method.originalName, obfuscatedName, method.getObfuscatedNameForVersion(version) != null)
        }

        val fields = classMapping.fields.map { field ->
            val obfuscatedName = field.getObfuscatedNameForVersion(version) ?: field.obfuscatedName
            VersionMemberInfo(field.originalName, obfuscatedName, field.getObfuscatedNameForVersion(version) != null)
        }

        return ClassVersionInfo(
            originalName = originalClassName,
            obfuscatedName = classObfuscatedName,
            version = version,
            hasVersionSpecificMapping = classMapping.getObfuscatedNameForVersion(version) != null,
            methods = methods,
            fields = fields
        )
    }
}

data class MappingResult(
    val originalName: String,
    val obfuscatedName: String,
    val version: String?,
    val treeIndex: Int,
    val mappingTree: MappingTree,
    val isVersionSpecific: Boolean
)

data class MethodMappingResult(
    val methodMapping: MethodMapping,
    val classMapping: ClassMapping,
    val actualObfuscatedName: String,
    val version: String?,
    val treeIndex: Int,
    val mappingTree: MappingTree,
    val isVersionSpecific: Boolean
)

data class FieldMappingResult(
    val fieldMapping: FieldMapping,
    val classMapping: ClassMapping,
    val actualObfuscatedName: String,
    val version: String?,
    val treeIndex: Int,
    val mappingTree: MappingTree,
    val isVersionSpecific: Boolean
)

data class AliasEntryResult(
    val aliasEntry: AliasEntry,
    val treeIndex: Int,
    val mappingTree: MappingTree,
    val classMapping: ClassMapping,
    val methodMapping: MethodMapping? = null,
    val fieldMapping: FieldMapping? = null
)

data class ClassVersionInfo(
    val originalName: String,
    val obfuscatedName: String,
    val version: String,
    val hasVersionSpecificMapping: Boolean,
    val methods: List<VersionMemberInfo>,
    val fields: List<VersionMemberInfo>
)

data class VersionMemberInfo(
    val originalName: String,
    val obfuscatedName: String,
    val hasVersionSpecificMapping: Boolean
)

fun emptyMappingLookup(): MappingLookup = MappingLookup(emptyList())

fun List<MappingLookup>.merge(): MappingLookup {
    val allTrees = this.flatMap { it.getAllMappingTrees() }
    return MappingLookup(allTrees)
}
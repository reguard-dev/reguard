package org.cubewhy.reguard.script.core

interface Aliasable {
    val aliases: List<AliasEntry>

    fun getObfuscatedNameForVersion(version: String?): String? {
        if (version == null) return null

        val alias = this.aliases.find { it.version == version && !it.isRemoved }
        return alias?.obfuscatedName
    }
}

data class ClassMapping(
    val originalName: String,
    val fallbackObfuscatedName: String,
    val metadata: Map<String, String>,
    val methods: List<MethodMapping>,
    val fields: List<FieldMapping>,
    val innerClasses: List<ClassMapping>,
    override val aliases: List<AliasEntry>
): Aliasable

data class MethodMapping(
    val originalName: String,
    val obfuscatedName: String,
    val parameters: List<ResolvedParameter>,
    val javadoc: String?,
    override val aliases: List<AliasEntry>,
    val metadata: Map<String, String>
): Aliasable

data class FieldMapping(
    val originalName: String,
    val obfuscatedName: String,
    override val aliases: List<AliasEntry>,
    val metadata: Map<String, String>
): Aliasable
package org.cubewhy.reguard.script.core

sealed class ASTNode

data class MappingFile(
    val packageDecl: PackageDeclaration?,
    val imports: List<ImportDeclaration>,
    val classes: List<ClassDeclaration>
) : ASTNode()

data class PackageDeclaration(
    val packageName: String
) : ASTNode()

data class ImportDeclaration(
    val importPath: String,
    val alias: String? = null
) : ASTNode()

data class ClassDeclaration(
    val name: String,
    val obfuscatedName: String?,
    val metadata: Map<String, String> = emptyMap(),
    val methods: List<MethodDeclaration> = emptyList(),
    val fields: List<FieldDeclaration> = emptyList(),
    val innerClasses: List<ClassDeclaration> = emptyList(),
    val aliases: List<AliasEntry> = emptyList()
) : ASTNode()

data class MethodDeclaration(
    val name: String,
    val parameters: List<Parameter>,
    val obfuscatedName: String?,
    val javadoc: String? = null,
    val aliases: List<AliasEntry> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) : ASTNode()

data class FieldDeclaration(
    val name: String,
    val obfuscatedName: String?,
    val aliases: List<AliasEntry> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) : ASTNode()

data class Parameter(
    val type: String,
    val isVarargs: Boolean = false
) : ASTNode()

data class AliasEntry(
    val version: String,
    val obfuscatedName: String,
    val metadata: Map<String, String> = emptyMap(),
    val isRemoved: Boolean = false
) : ASTNode()
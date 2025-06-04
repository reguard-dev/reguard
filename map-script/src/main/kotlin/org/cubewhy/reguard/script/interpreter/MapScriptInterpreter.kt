package org.cubewhy.reguard.script.interpreter

import org.cubewhy.reguard.script.core.ClassDeclaration
import org.cubewhy.reguard.script.core.ClassMapping
import org.cubewhy.reguard.script.core.FieldDeclaration
import org.cubewhy.reguard.script.core.FieldMapping
import org.cubewhy.reguard.script.core.ImportDeclaration
import org.cubewhy.reguard.script.core.MappingFile
import org.cubewhy.reguard.script.core.MappingTree
import org.cubewhy.reguard.script.core.MethodDeclaration
import org.cubewhy.reguard.script.core.MethodMapping
import org.cubewhy.reguard.script.core.ResolvedParameter

class MapScriptInterpreter {

    fun interpret(ast: MappingFile): MappingTree {
        val packageName = ast.packageDecl?.packageName ?: ""
        val imports = processImports(ast.imports)
        val classes = ast.classes.map { processClass(it, imports) }

        return MappingTree(packageName, imports, classes)
    }

    private fun processImports(imports: List<ImportDeclaration>): Map<String, String> {
        val importMap = mutableMapOf<String, String>()

        imports.forEach { import ->
            val alias = import.alias ?: import.importPath.substringAfterLast('.')
            importMap[alias] = import.importPath
        }

        return importMap
    }

    private fun processClass(classDecl: ClassDeclaration, imports: Map<String, String>): ClassMapping {
        val methods = classDecl.methods.map { processMethod(it, imports) }
        val fields = classDecl.fields.map { processField(it) }
        val innerClasses = classDecl.innerClasses.map { processClass(it, imports) }

        return ClassMapping(
            originalName = classDecl.name,
            fallbackObfuscatedName = classDecl.obfuscatedName ?: classDecl.name,
            metadata = classDecl.metadata,
            methods = methods,
            fields = fields,
            innerClasses = innerClasses,
            aliases = classDecl.aliases
        )
    }

    private fun processMethod(methodDecl: MethodDeclaration, imports: Map<String, String>): MethodMapping {
        val resolvedParameters = methodDecl.parameters.map { param ->
            ResolvedParameter(
                type = resolveType(param.type, imports),
                isVarargs = param.isVarargs
            )
        }

        return MethodMapping(
            originalName = methodDecl.name,
            obfuscatedName = methodDecl.obfuscatedName ?: methodDecl.name,
            parameters = resolvedParameters,
            javadoc = methodDecl.javadoc,
            aliases = methodDecl.aliases,
            metadata = methodDecl.metadata
        )
    }

    private fun processField(fieldDecl: FieldDeclaration): FieldMapping {
        return FieldMapping(
            originalName = fieldDecl.name,
            obfuscatedName = fieldDecl.obfuscatedName ?: fieldDecl.name,
            aliases = fieldDecl.aliases,
            metadata = fieldDecl.metadata
        )
    }

    private fun resolveType(typeName: String, imports: Map<String, String>): String {
        return imports[typeName] ?: typeName
    }
}
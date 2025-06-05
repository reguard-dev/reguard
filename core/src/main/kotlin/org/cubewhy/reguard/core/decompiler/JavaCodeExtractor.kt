package org.cubewhy.reguard.core.decompiler

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.*

/**
 * A utility class that parses Java source code and extracts structural class information.
 *
 * This extractor supports classes, interfaces, enums, annotations, methods, fields,
 * constructors, static initializers, and inner classes. It is useful for tooling that
 * needs to analyze or transform Java source code, such as deobfuscators or reverse-engineering tools.
 */
class JavaCodeExtractor(languageLevel: ParserConfiguration.LanguageLevel = ParserConfiguration.LanguageLevel.JAVA_21) {

    private val parser = JavaParser(
        ParserConfiguration()
            .setLanguageLevel(languageLevel)
    )

    /**
     * Parses raw Java source code and extracts all top-level class structures.
     *
     * @param sourceCode the full Java source code as a string.
     * @return a list of [DecompiledClass] instances representing parsed class structures.
     * @throws RuntimeException if parsing fails.
     */
    fun extractFromSource(sourceCode: String): List<DecompiledClass> {
        val parseResult = parser.parse(sourceCode)
        if (!parseResult.isSuccessful) {
            throw RuntimeException("Failed to parse Java source: ${parseResult.problems}")
        }

        return extractFromCompilationUnit(parseResult.result.get())
    }

    /**
     * Parses the given [CompilationUnit] and extracts all top-level classes, interfaces, enums, and annotations.
     *
     * @param cu the parsed [CompilationUnit] from JavaParser.
     * @return a list of [DecompiledClass] instances.
     */
    private fun extractFromCompilationUnit(cu: CompilationUnit): List<DecompiledClass> {
        val classes = mutableListOf<DecompiledClass>()

        cu.types.forEach { typeDeclaration ->
            when (typeDeclaration) {
                is ClassOrInterfaceDeclaration -> {
                    classes.add(extractClass(typeDeclaration))
                }

                is EnumDeclaration -> {
                    classes.add(extractEnum(typeDeclaration))
                }

                is AnnotationDeclaration -> {
                    classes.add(extractAnnotation(typeDeclaration))
                }
            }
        }

        return classes
    }

    /**
     * Extracts metadata from a class or interface declaration.
     *
     * @param classDecl the class or interface declaration node.
     * @return a [DecompiledClass] representing the class and its members.
     */
    private fun extractClass(classDecl: ClassOrInterfaceDeclaration): DecompiledClass {
        val className = classDecl.nameAsString
        val methods = mutableListOf<DecompiledMethod>()
        val fields = mutableListOf<DecompiledField>()
        val constructors = mutableListOf<DecompiledConstructor>()
        val staticInitializers = mutableListOf<String>()
        val innerClasses = mutableListOf<DecompiledClass>()

        classDecl.members.forEach { member ->
            when (member) {
                is MethodDeclaration -> {
                    methods.add(extractMethod(member))
                }

                is FieldDeclaration -> {
                    fields.addAll(extractFields(member))
                }

                is ConstructorDeclaration -> {
                    constructors.add(extractConstructor(member))
                }

                is InitializerDeclaration -> {
                    if (member.isStatic) {
                        staticInitializers.add(member.toString())
                    }
                }

                is ClassOrInterfaceDeclaration -> {
                    innerClasses.add(extractClass(member))
                }

                is EnumDeclaration -> {
                    innerClasses.add(extractEnum(member))
                }

                is AnnotationDeclaration -> {
                    innerClasses.add(extractAnnotation(member))
                }
            }
        }

        return DecompiledClass(
            className = className,
            methods = methods,
            fields = fields,
            constructors = constructors,
            staticInitializers = staticInitializers,
            innerClasses = innerClasses
        )
    }

    /**
     * Extracts metadata from an enum declaration.
     *
     * @param enumDecl the enum declaration node.
     * @return a [DecompiledClass] representing the enum and its members.
     */
    private fun extractEnum(enumDecl: EnumDeclaration): DecompiledClass {
        val className = enumDecl.nameAsString
        val methods = mutableListOf<DecompiledMethod>()
        val fields = mutableListOf<DecompiledField>()
        val constructors = mutableListOf<DecompiledConstructor>()
        val staticInitializers = mutableListOf<String>()
        val innerClasses = mutableListOf<DecompiledClass>()

        enumDecl.entries.forEach { entry ->
            fields.add(
                DecompiledField(
                    name = entry.nameAsString,
                    code = entry.toString()
                )
            )
        }

        enumDecl.members.forEach { member ->
            when (member) {
                is MethodDeclaration -> {
                    methods.add(extractMethod(member))
                }

                is FieldDeclaration -> {
                    fields.addAll(extractFields(member))
                }

                is ConstructorDeclaration -> {
                    constructors.add(extractConstructor(member))
                }

                is InitializerDeclaration -> {
                    if (member.isStatic) {
                        staticInitializers.add(member.toString())
                    }
                }

                is ClassOrInterfaceDeclaration -> {
                    innerClasses.add(extractClass(member))
                }

                is EnumDeclaration -> {
                    innerClasses.add(extractEnum(member))
                }
            }
        }

        return DecompiledClass(
            className = className,
            methods = methods,
            fields = fields,
            constructors = constructors,
            staticInitializers = staticInitializers,
            innerClasses = innerClasses
        )
    }


    /**
     * Extracts metadata from an annotation declaration.
     *
     * @param annotationDecl the annotation declaration node.
     * @return a [DecompiledClass] representing the annotation type and its members.
     */
    private fun extractAnnotation(annotationDecl: AnnotationDeclaration): DecompiledClass {
        val className = annotationDecl.nameAsString
        val methods = mutableListOf<DecompiledMethod>()

        annotationDecl.members.forEach { member ->
            when (member) {
                is AnnotationMemberDeclaration -> {
                    methods.add(
                        DecompiledMethod(
                            name = member.nameAsString,
                            code = member.toString()
                        )
                    )
                }

                is MethodDeclaration -> {
                    methods.add(extractMethod(member))
                }
            }
        }

        return DecompiledClass(
            className = className,
            methods = methods,
            fields = emptyList(),
            constructors = emptyList(),
            staticInitializers = emptyList(),
            innerClasses = emptyList()
        )
    }

    /**
     * Extracts a single method's name and full source code.
     *
     * @param method the method declaration node.
     * @return a [DecompiledMethod] containing the method name and code.
     */
    private fun extractMethod(method: MethodDeclaration): DecompiledMethod {
        return DecompiledMethod(
            name = method.nameAsString,
            code = method.toString()
        )
    }

    /**
     * Extracts all fields declared in a single field declaration.
     * Handles multiple variable declarations in the same statement.
     *
     * @param fieldDecl the field declaration node.
     * @return a list of [DecompiledField] instances.
     */
    private fun extractFields(fieldDecl: FieldDeclaration): List<DecompiledField> {
        return fieldDecl.variables.map { variable ->
            DecompiledField(
                name = variable.nameAsString,
                code = "${fieldDecl.modifiers.joinToString(" ")} ${fieldDecl.elementType} ${variable.nameAsString}${
                    if (variable.initializer.isPresent) " = ${variable.initializer.get()}" else ""
                };"
            )
        }
    }

    /**
     * Extracts the full constructor code.
     *
     * @param constructor the constructor declaration node.
     * @return a [DecompiledConstructor] containing the constructor code.
     */
    private fun extractConstructor(constructor: ConstructorDeclaration): DecompiledConstructor {
        return DecompiledConstructor(
            code = constructor.toString()
        )
    }
}
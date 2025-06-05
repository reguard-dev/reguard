package org.cubewhy.reguard.core.decompiler

data class DecompiledClass(
    val className: String,

    val methods: List<DecompiledMethod>,
    val fields: List<DecompiledField>,
    val constructors: List<DecompiledConstructor>,
    val staticInitializers: List<String>,
    val innerClasses: List<DecompiledClass>,
)

data class DecompiledMethod(
    val name: String,
    val code: String
)

data class DecompiledField(
    val name: String,
    val code: String
)

data class DecompiledConstructor(
    val code: String,
)
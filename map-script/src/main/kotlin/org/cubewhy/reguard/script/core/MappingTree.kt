package org.cubewhy.reguard.script.core

data class MappingTree(
    val packageName: String,
    val imports: Map<String, String>,
    val classes: List<ClassMapping>
)
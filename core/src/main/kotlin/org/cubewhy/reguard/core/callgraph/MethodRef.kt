package org.cubewhy.reguard.core.callgraph

data class MethodRef(
    val owner: String,
    val name: String,
    val desc: String,
)

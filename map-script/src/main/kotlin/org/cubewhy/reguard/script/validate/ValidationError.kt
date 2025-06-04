package org.cubewhy.reguard.script.validate

data class ValidationError(
    val message: String,
    val type: ErrorType
)
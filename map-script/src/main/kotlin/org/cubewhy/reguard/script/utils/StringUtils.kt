package org.cubewhy.reguard.script.utils

inline fun String?.ifNullOrEmpty(defaultValue: () -> String): String {
    return if (this == null || this.isEmpty()) defaultValue() else this
}

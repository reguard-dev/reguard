package org.cubewhy.reguard.core.decompiler

import org.cubewhy.reguard.script.parser.MappingLookup

interface Decompiler {
    suspend fun decompile(classBinary: ByteArray, mapping: MappingLookup): List<DecompiledClass>
}
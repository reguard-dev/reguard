package org.cubewhy.reguard.core.decompiler

import org.cubewhy.reguard.script.parser.MappingLookup
import org.objectweb.asm.commons.Remapper

/**
 * An ASM [Remapper] implementation that delegates name translation to a [MappingLookup].
 *
 * The *version* concept is entirely handled inside the supplied [MappingLookup] —​ the
 * remapper itself does **not** store the version string, it merely invokes
 * [MappingLookup.useVersion] whenever the caller requests a different mapping version.
 *
 * All ``map*`` overrides first try to obtain a mapped name from the lookup; if none is
 * available the original name is returned unchanged (fall‑through to ASM's default logic).
 *
 * Only class, method and field identifiers are remapped. Descriptors are handled by the
 * base [Remapper] (it calls back into [map] / [mapType] for the contained types), so we
 * usually don't need to override [mapDesc] explicitly.
 */
class MappingRemapper(
    private val mappingLookup: MappingLookup,
) : Remapper() {

    /**
     * Switches the active mapping version on the fly.
     * This method is thread‑unsafe because the underlying [MappingLookup] is mutable.
     * Callers must provide external synchronisation if they reuse the *same* remapper
     * instance from multiple threads.
     */
    fun useVersion(version: String?) {
        mappingLookup.useVersion(version)
    }

    /* ------------------------------------------------------------------------- */
    /*  Class names                                                              */
    /* ------------------------------------------------------------------------- */

    /** Remap an internal JVM class name (e.g. `com/example/Foo`). */
    override fun map(internalName: String): String {
        val dotName = internalName.replace('/', '.')
        val mapped = mappingLookup.findObfuscatedClassName(dotName) ?: return internalName
        return mapped.replace('.', '/')
    }

    /* ------------------------------------------------------------------------- */
    /*  Field names                                                              */
    /* ------------------------------------------------------------------------- */

    /**
     * Remap a field name given its owner class. `owner` is still the *internal* JVM name
     * at this point.
     */
    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        val ownerDot = owner.replace('/', '.')
        val mapping = mappingLookup.findFieldMapping(ownerDot, name)
        return mapping?.obfuscatedName ?: name
    }

    /* ------------------------------------------------------------------------- */
    /*  Method names                                                             */
    /* ------------------------------------------------------------------------- */

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        val ownerDot = owner.replace('/', '.')
        val mapping = mappingLookup.findMethodMapping(ownerDot, name)
        return mapping?.obfuscatedName ?: name
    }
}

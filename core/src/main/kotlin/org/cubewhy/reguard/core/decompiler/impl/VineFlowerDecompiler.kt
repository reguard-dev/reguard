package org.cubewhy.reguard.core.decompiler.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cubewhy.reguard.core.decompiler.DecompiledClass
import org.cubewhy.reguard.core.decompiler.Decompiler
import org.cubewhy.reguard.core.decompiler.JavaCodeExtractor
import org.cubewhy.reguard.script.parser.MappingLookup
import org.cubewhy.reguard.script.parser.emptyMappingLookup
import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Manifest

class VineFlowerDecompiler : Decompiler {

    private val javaCodeExtractor = JavaCodeExtractor()

    override suspend fun decompile(
        classBinary: ByteArray,
        mapping: MappingLookup
    ): List<DecompiledClass> = withContext(Dispatchers.IO) {

        val tempDir = Files.createTempDirectory("vineflower_decompile")
        val outputDir = Files.createTempDirectory("vineflower_output")

        try {
            val tempClassFile = tempDir.resolve("temp.class")
            Files.write(tempClassFile, classBinary)

            val options = mapOf(
                IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES to "1",
                IFernflowerPreferences.DECOMPILE_INNER to "1",
                IFernflowerPreferences.DECOMPILE_ENUM to "1",
                IFernflowerPreferences.REMOVE_BRIDGE to "1",
                IFernflowerPreferences.REMOVE_SYNTHETIC to "1",
                IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH to "0",
                IFernflowerPreferences.BYTECODE_SOURCE_MAPPING to "1",
                IFernflowerPreferences.DUMP_ORIGINAL_LINES to "0",
                IFernflowerPreferences.IGNORE_INVALID_BYTECODE to "1",
                IFernflowerPreferences.VERIFY_ANONYMOUS_CLASSES to "1"
            )

            val resultCollector = DecompilationResultCollector()

            val fernflower = Fernflower(
                resultCollector,
                options,
                VineFlowerLogger()
            )

            fernflower.addSource(tempClassFile.toFile())

            fernflower.decompileContext()

            val decompiledSources = resultCollector.getResults()

            val decompiledClasses = mutableListOf<DecompiledClass>()

            decompiledSources.forEach { (className, sourceCode) ->
                try {
                    val extractedClasses = javaCodeExtractor.extractFromSource(sourceCode)
                    decompiledClasses.addAll(extractedClasses)
                } catch (e: Exception) {
                    println("Failed to extract class $className: ${e.message}")
                }
            }

            decompiledClasses
        } finally {
            // cleanup
            cleanupDirectory(tempDir)
            cleanupDirectory(outputDir)
        }
    }

    private fun cleanupDirectory(dir: Path) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map { it.toFile() }
                .forEach { it.delete() }
        } catch (_: Exception) {
        }
    }
}

private class DecompilationResultCollector : IResultSaver {

    private val results = mutableMapOf<String, String>()

    override fun saveFolder(path: String) {
    }

    override fun copyFile(source: String, path: String, entryName: String) {
    }

    override fun saveClassFile(
        path: String,
        qualifiedName: String,
        entryName: String,
        content: String,
        mapping: IntArray?
    ) {
        results[qualifiedName] = content
    }

    override fun createArchive(path: String, archiveName: String, manifest: Manifest?) {
    }

    override fun saveDirEntry(path: String, archiveName: String, entryName: String) {
    }

    override fun copyEntry(source: String, path: String, archiveName: String, entry: String) {
    }

    override fun saveClassEntry(
        path: String,
        archiveName: String,
        qualifiedName: String,
        entryName: String,
        content: String
    ) {
        results[qualifiedName] = content
    }

    override fun closeArchive(path: String, archiveName: String) {
    }

    fun getResults(): Map<String, String> = results.toMap()
}


private class VineFlowerLogger : IFernflowerLogger() {

    override fun writeMessage(message: String, severity: Severity) {
    }

    override fun writeMessage(message: String, severity: Severity, t: Throwable) {
        writeMessage("$message: ${t.message}", severity)
        if (severity == Severity.ERROR) {
            t.printStackTrace()
        }
    }
}

suspend fun VineFlowerDecompiler.decompileFromClassName(
    className: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader
): List<DecompiledClass> {
    val resourceName = className.replace('.', '/') + ".class"
    val inputStream = classLoader.getResourceAsStream(resourceName)
        ?: throw IllegalArgumentException("Class not found: $className")

    val classBinary = inputStream.use { it.readBytes() }
    return decompile(classBinary, emptyMappingLookup())
}


suspend fun VineFlowerDecompiler.decompileFromJar(
    jarPath: String,
    className: String
): List<DecompiledClass> {
    val jarFile = java.util.jar.JarFile(jarPath)
    val entry = jarFile.getJarEntry(className.replace('.', '/') + ".class")
        ?: throw IllegalArgumentException("Class $className not found in JAR: $jarPath")

    val classBinary = jarFile.getInputStream(entry).use { it.readBytes() }
    jarFile.close()

    return decompile(classBinary, emptyMappingLookup())
}

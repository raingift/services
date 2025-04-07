package com.hermes.service.transform

import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Traverse directories and jar packages to do find the specified class file
 * for dealing
 *
 * */
abstract class BaseTransformAllClassesTask : DefaultTask() {

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val start = System.currentTimeMillis()
        JarOutputStream(output.asFile.get().outputStream()).use { jarOutput ->
            // Scan directory (Copy and Collection)
            processDir(jarOutput)

            debugCollection()

            // Scan Jar, Copy & Scan & Code Inject & return Inject
            var originInject: ByteArray? = processJar(jarOutput)

            debugCollection()
            // Do inject
            processInject(originInject, jarOutput)
        }
        println("Service plugin inject time spend ${System.currentTimeMillis() - start} ms")
    }

    private fun processInject(originInject: ByteArray?, jarOutput: JarOutputStream) {
        logger("Start inject byte code")
        if (originInject == null) { // Check
            logger.error("Can not find Service inject point, Do you import Service?")
            return
        }
        val resultByteArray = doInject(originInject)
        jarOutput.saveEntry(injectGenerateClassInfo(), ByteArrayInputStream(resultByteArray))
        logger("Inject byte code successful")
    }

    private fun processJar(jarOutput: JarOutputStream): ByteArray? {
        var originInject: ByteArray? = null
        allJars.get().map { it.asFile }.forEach { file ->
            logger("Jar file is $file")
            val jar = JarFile(file)
            jar.entries().iterator().forEach { jarEntry ->
                // Exclude directory
                if (jarEntry.isDirectory || filterInvalidFiler(jarEntry.name)) {
                    return@forEach
                }
                logger("\tJar entry is ${jarEntry.name}")

                if (jarEntry.name != injectGenerateClassInfo()) {
                    // Scan and choose
                    postProcessJar(jarEntry, jar)
                    // Copy
                    jar.getInputStream(jarEntry).use { input ->
                        jarOutput.saveEntry(jarEntry.name, input)
                    }
                } else {
                    // Skip
                    logger("Find inject byte code, Skip ${jarEntry.name}")
                    jar.getInputStream(jarEntry).use { inputs ->
                        originInject = inputs.readAllBytes()
                        logger("Find origin inject byte code size is ${originInject?.size}")
                    }
                }
            }
            jar.close()
        }
        return originInject
    }

    private fun processDir(jarOutput: JarOutputStream) {
        allDirectories.get().forEach { directory ->
            val absolutePath = directory.asFile.absolutePath
            val directoryPath = if (absolutePath.endsWith(File.separatorChar)) {
                absolutePath
            } else {
                absolutePath + File.separatorChar
            }
            logger("Directory is $directoryPath")

            directory.asFile.walk().filter { it.isFile }.forEach { file ->
                logger("\tDirectory file name ${file.name}")

                val entryName = if (File.separator == "/") {
                    file.path.substringAfter(directoryPath)
                } else {
                    file.path.substringAfter(directoryPath).replace(File.separatorChar, '/')
                }

                logger("\tDirectory entry name: $entryName")

                if (entryName.isNotEmpty()) {
                    // Use stream to detect register, Take care, stream can only be read once,
                    // So, When Scan and Copy should open different stream;
                    // Copy
                    postProcessDir(entryName, file)

                    file.inputStream().use { input ->
                        jarOutput.saveEntry(entryName, input)
                    }
                }
            }
        }
    }

    private fun JarOutputStream.saveEntry(entryName: String, inputStream: InputStream) {
        try {
            this.putNextEntry(JarEntry(entryName))
            IOUtils.copy(inputStream, this)
            this.closeEntry()
        } catch (e: Exception) {
            logger.error("Merge jar error entry:$entryName, error is $e ")
        } finally {
            this.closeEntry()
        }
    }

    /**
     * debug collection info
     */
    open fun debugCollection() {}

    /**
     * non-core methods to log info
     */
    open fun logger(info: String) {
        println(info)
    }

    /**
     * filter invalid filer
     */
    open fun filterInvalidFiler(name: String): Boolean {
        return false
    }

    /**
     * post to do process jar info
     * @param entry
     * @param jar
     */
    abstract fun postProcessJar(entry: JarEntry, jar: JarFile)

    /**
     * post to do process dir info
     * @param entryName
     * @param file
     */
    abstract fun postProcessDir(entryName: String, file: File)

    /**
     * inject to generate class info
     */
    abstract fun injectGenerateClassInfo(): String

    /**
     * do to inject code to the the class
     * @param originInject origin class byte array info
     */
    abstract fun doInject(originInject: ByteArray): ByteArray

}
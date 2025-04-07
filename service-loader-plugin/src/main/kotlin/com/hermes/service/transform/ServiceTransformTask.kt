package com.hermes.service.transform

import com.hermes.service.loader.ServiceLoaderPluginExtension
import java.io.ByteArrayInputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

abstract class ServiceTransformTask() : BaseTransformAllClassesTask() {

    private var extension =
        project.extensions.findByName("serviceLoader") as? ServiceLoaderPluginExtension
    private val registry = mutableMapOf<String, MutableList<String>>()

    override fun postProcessDir(entryName: String, file: File) {
        if (!entryName.contains(META_INFO_SERVICES) || PACKAGES_IGNORED.contains(
                entryName.substringBefore("."))) {
            return
        }

        file.readLines().filter(::checkImpl).map {
            file.name to it
        }.forEach { (service, providers) ->
            registry.computeIfAbsent(service) {
                mutableListOf()
            } += providers
        }
        logger("\tregistry: $registry")
    }

    override fun injectGenerateClassInfo(): String = GENERATE_TO_CLASS_FILE_NAME

    override fun postProcessJar(entry: JarEntry, jar: JarFile) {
        if (entry.name.length <= META_INFO_SERVICES.length || !entry.name.startsWith(
                META_INFO_SERVICES)) {
            return
        }

        val entryName = entry.name.substringAfter(META_INFO_SERVICES)
        if (PACKAGES_IGNORED.contains(entryName.substringBefore("."))) {
            return
        }

        jar.getInputStream(entry).bufferedReader().use { reader ->
            reader.readLines().filter(::checkImpl).map {
                entryName to it
            }.forEach { (service, providers) ->
                registry.computeIfAbsent(service) {
                    mutableListOf()
                } += providers
            }
        }
    }

    override fun doInject(originInject: ByteArray): ByteArray =
        InjectUtils.referHackWhenInit(ByteArrayInputStream(originInject), registry)

    override fun debugCollection() {
        if (extension?.debugCollection != true) {
            return
        }
        println("Collect result:")
        registry.forEach { (key, value) ->
            println("[$key]")
            value.forEach {
                println("\t $it")
            }
        }
    }

    override fun logger(info: String) {
        if (extension?.enableDebug == true) {
            println(info)
        }
    }

    override fun filterInvalidFiler(name: String): Boolean {
        return name == "META-INF/MANIFEST.MF" ||
                name.startsWith("META-INF/versions") ||
                name.endsWith("coroutines.pro") ||
                extension?.exclude?.contains(name) == true
    }

    private fun checkImpl(line: String) = line.isNotBlank() && !line.startsWith("#")
}
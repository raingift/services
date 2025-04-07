package com.hermes.service.loader

import com.didiglobal.booster.kotlinx.stream
import com.didiglobal.booster.transform.TransformContext
import com.didiglobal.booster.transform.asm.ClassTransformer
import com.didiglobal.booster.transform.asm.findAll
import com.didiglobal.booster.util.search
import com.google.auto.service.AutoService
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.util.jar.JarFile
import kotlin.streams.toList

/**
 * Represents a transformer which used to replace [java.util.ServiceLoader] with [ShadowServiceLoader]
 *
 * @author hermes
 */
@AutoService(ClassTransformer::class)
class ServiceLoaderTransformer : ClassTransformer {

    private val registry = mutableMapOf<String, MutableList<String>>()

    override fun onPreTransform(context: TransformContext) {
        context.compileClasspath.parallelStream().map { file ->
            when {
                // search from directory
                file.isDirectory -> file.search {
                    it.parentFile.name == "services" && it.parentFile.parentFile.name == "META-INF"
                }.map {
                    it.name to it.readLines().filter(::checkImpl)
                }
                // search from jar files
                file.isFile && file.extension == "jar" -> JarFile(file).use { jar ->
                    jar.entries().iterator().stream().filter {
                        it.name.length > META_INFO_SERVICES.length && it.name.startsWith(META_INFO_SERVICES)
                    }.map {
                        it.name.substringAfter(META_INFO_SERVICES) to jar.getInputStream(it).bufferedReader().use { reader ->
                            reader.readLines().filter(::checkImpl)
                        }
                    }.toList()
                }
                else -> emptyList()
            }
        }.flatMap {
            it.stream()
        }.forEach { (service, providers) ->
            registry.computeIfAbsent(service) {
                mutableListOf()
            } += providers
        }
    }

    override fun transform(context: TransformContext, klass: ClassNode) = when {
        klass.name.substringBefore('/') in PACKAGES_IGNORED -> klass
        klass.name == SERVICE_REGISTRY -> context.transformServiceRegistry(klass)
        else -> klass
    }

    /**
     * @ServiceRegistry
     *  * static {
     *      *    ......
     *      *    registry("com.A.class", "com.AImpl_Creator")
     *      *    registry("com.B.class", "com.BImpl_Creator")
     *      *    ......
     *      *    registry("com.X.class", "com.XImpl_Creator")
     *      * }
     *
     * */
    private fun TransformContext.transformServiceRegistry(klass: ClassNode): ClassNode {
        val clinit = klass.methods.find {
            it.name == "<clinit>" && it.desc == "()V"
        }
                ?: MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, SERVICE_REGISTRY, "<clinit>", "()V", arrayOf()).apply {
                    this.instructions.add(InsnNode(Opcodes.RETURN))
                    klass.methods.add(this)
                }

        clinit.instructions.findAll(Opcodes.RETURN).forEach { ret ->
            registry.flatMap { (k, v) ->
                v.map { k to it }
            }.map { (k, v) ->
                k.replace('.', '/') to v.replace('.', '/') + "_Init"
            }.forEach { (provider, creator) ->

                clinit.instructions.insertBefore(ret, InsnList().apply {
                    add(LdcInsnNode(Type.getType("L${provider};").className))
                    add(LdcInsnNode(Type.getType("L${creator};").className))
                    add(MethodInsnNode(Opcodes.INVOKESTATIC, SERVICE_REGISTRY, "register", "(Ljava/lang/String;Ljava/lang/String;)V", false))
                })
            }
        }

        return klass
    }
}

private fun checkImpl(line: String) = line.isNotBlank() && !line.startsWith("#")

private val PACKAGES_IGNORED = setOf("android", "androidx", "kotlin")

private const val META_INFO_SERVICES = "META-INF/services/"

internal const val SERVICE_REGISTRY = "com/hermes/service/register/ServiceRegistry"

package com.hermes.service.compiler

import com.google.auto.common.MoreElements.getAnnotationMirror
import com.google.auto.common.MoreTypes
import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * Processes {@link AutoService} annotations and generates the service provider creator class
 *
 * @author hermes
 */
@AutoService(Processor::class)
@SupportedAnnotationTypes("com.google.auto.service.AutoService")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ServiceAnnotationProcessor : AbstractProcessor() {

    private val providers = mutableMapOf<String, MutableList<String>>()

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val annotations = roundEnv.getElementsAnnotatedWith(AutoService::class.java)
        if(annotations.isEmpty()) {
            return false
        }
        processAnnotations(roundEnv)
        generateProviderImplInit()
        return false
    }

    private fun processAnnotations(roundEnv: RoundEnvironment) {
        val annotations = roundEnv.getElementsAnnotatedWith(AutoService::class.java)
        annotations.map { it as TypeElement }.forEach { implementer ->
            val annotation = getAnnotationMirror(implementer, AutoService::class.java).get()
            val providers = annotation.valueFieldOfClasses
            if (providers.isEmpty()) {
                processingEnv.error(
                    "No service interfaces provided for element!",
                    implementer,
                    annotation
                )
            } else {
                providers.forEach {
                    val provider = MoreTypes.asTypeElement(it)
                    if (processingEnv.checkImplementer(implementer, provider)) {
                        this.providers.getOrPut(provider.getBinaryName(provider.simpleName.toString())) {
                            mutableListOf()
                        } += implementer.getBinaryName(implementer.simpleName.toString())
                    } else {
                        processingEnv.error(
                            "ServiceProviders must implement their service provider interface. ${implementer.qualifiedName} does not implement ${provider.qualifiedName}",
                            implementer,
                            annotation
                        )
                    }
                }
            }
        }
    }

    /**
     * public final class Implementer_Init implements Callable<Implementer> {
     *      Implementer call() {
     *         return new Implementer();
     *       }
     * }
     */
    private fun generateProviderImplInit() {
        val typeOfCallable = ClassName.get("java.util.concurrent", "Callable")
        this.providers.entries.flatMap { (k, v) ->
            v.map { k to it }
        }.forEach { (_, implementer) ->
            val pkg = implementer.substringBeforeLast('.')
            val name = implementer.substringAfterLast('.')
            val creator = "${name}_Init"
            val typeOfImplementer = ClassName.get(pkg, name)
            val typeOfCallableOfImplementer =
                ParameterizedTypeName.get(typeOfCallable, typeOfImplementer)

            val implementerCreator = TypeSpec.classBuilder(creator)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(typeOfCallableOfImplementer)
                .addMethod(
                    MethodSpec.methodBuilder("call")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addStatement("return new " + '$' + "T()", typeOfImplementer)
                        .returns(typeOfImplementer)
                        .build()
                )
                .build()
            processingEnv.debug("Generating $creator ...")
            val jarFile = JavaFile.builder(pkg, implementerCreator).build()
            jarFile.writeTo(processingEnv.filer)
        }
    }
}

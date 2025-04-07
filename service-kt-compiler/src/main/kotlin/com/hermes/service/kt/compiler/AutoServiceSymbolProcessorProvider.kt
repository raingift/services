package com.hermes.service.kt.compiler

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * kt auto service processor
 * @param AutoService
 */
class AutoServiceSymbolProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AutoServiceSymbolProcessor(
            KspLoggerWrapper(environment.logger), environment.codeGenerator
        )
    }
}

class AutoServiceSymbolProcessor(
    @Suppress("unused") internal val logger: KspLoggerWrapper,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private val providers = mutableMapOf<String, MutableList<String>>()
    private val groups = mutableMapOf<String, KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val elements = resolver
            .getSymbolsWithAnnotation(AutoService::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>().toList()

        if (elements.isEmpty()) {
            return emptyList()
        }

        logger.info("\t auto service : $elements")

        elements.forEach { implClass ->
            val annotation = implClass.annotations
                .first { it.shortName.asString() == "AutoService" }

            val serviceInterfaces = annotation.arguments
                .first { it.name?.asString() == "value" }
                .value as? List<KSType>

            logger.info("\timplClass: $implClass")
            logger.info("\tannotation: $annotation")
            logger.info("\tserviceInterfaces: $serviceInterfaces")

            serviceInterfaces?.forEach { interfaceType ->
                logger.info("\tinterfaceType: $interfaceType")

                if (validateImplementation(implClass, interfaceType)) {
                    val interfaceName =
                        interfaceType.declaration.qualifiedName?.asString() ?: ""
                    val implName = implClass.qualifiedName?.asString() ?: ""

                    groups.put(implName, implClass)

                    providers.getOrPut(interfaceName) {
                        mutableListOf()
                    }.add(implName)
                }
            }
        }

        generateCallableInitClasses(elements)

        return emptyList()
    }

    private fun generateCallableInitClasses(elements: List<KSClassDeclaration>) {
        val typeOfCallable = ClassName("java.util.concurrent", "Callable")
        logger.info("\t auto service generateCallableInitClasses: $elements")
        this.providers.entries.flatMap { (k, v) ->
            v.map { k to it }
        }.forEach { (_, implementer) ->
            val packageName = implementer.substringBeforeLast('.')
            val className = implementer.substringAfterLast('.')
            val creator = "${className}_Init"
            val typeOfImplementer = ClassName(packageName, className)
            val typeOfCallableOfImplementer = typeOfCallable.parameterizedBy(typeOfImplementer)


            logger.info("\tpackageName: $packageName")
            logger.info("\tclassName: $className")
            logger.info("\tcreator: $creator")
            logger.info("\ttypeOfCallable: $typeOfCallable")
            logger.info("\ttypeOfImplementer: $typeOfImplementer")
            logger.info("\ttypeOfCallableOfImplementer: $typeOfCallableOfImplementer")

            val typeSpec = TypeSpec.classBuilder(creator)
                .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
                .addSuperinterface(typeOfCallableOfImplementer)
                .addFunction(
                    FunSpec.builder("call")
                        .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                        .addStatement("return %T()", typeOfImplementer)
                        .returns(typeOfImplementer)
                        .build()
                )
                .build()

            logger.info("group: $groups")

            // Get input source (@AutoService) which gene the output file
            val dependencies = mutableListOf<KSFile>()
            groups[implementer]?.containingFile?.let {
                dependencies.add(it)
            }

            FileSpec.builder(packageName, creator)
                .addFileComment("Auto-generated by KSP. DO NOT EDIT!")
                .addType(typeSpec)
                .build()
                .writeTo(codeGenerator, false, dependencies)
        }
    }
}

private fun AutoServiceSymbolProcessor.validateImplementation(
    implClass: KSClassDeclaration,
    serviceInterface: KSType
): Boolean {
    val isImplementing = implClass.superTypes.any {
        it.resolve().declaration == serviceInterface.declaration
    }
    if (!isImplementing) {
        logger.error(
            "${implClass.qualifiedName} unimplemented interface ${serviceInterface.declaration.qualifiedName}",
            implClass
        )
    }
    return isImplementing
}

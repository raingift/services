package com.hermes.service.compiler

import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.google.auto.common.MoreTypes
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor8
import javax.tools.Diagnostic

fun ProcessingEnvironment.checkImplementer(
    implementer: TypeElement,
    provider: TypeElement
): Boolean {
    val verify: String? = this.options["verify"]
    if (verify?.toBoolean() != true) {
        return true
    }
    return typeUtils.isSubtype(implementer.asType(), provider.asType())
}

fun ProcessingEnvironment.debug(msg: String) {
    if (options.containsKey("debug")) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg)
    }
}

fun ProcessingEnvironment.error(msg: String, element: Element, annotation: AnnotationMirror) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, element, annotation)
}

fun ProcessingEnvironment.fatalError(e: Throwable) {
    val stacktrace = StringWriter().use {
        e.printStackTrace(PrintWriter(it))
    }.toString()
    messager.printMessage(Diagnostic.Kind.ERROR, stacktrace)
}

fun TypeElement.getBinaryName(className: String): String {
    val enclosingElement = this.enclosingElement
    if (enclosingElement is PackageElement) {
        return if (enclosingElement.isUnnamed) {
            className
        } else {
            enclosingElement.qualifiedName.toString() + "." + className
        }
    }

    val typeElement = enclosingElement as TypeElement
    return typeElement.getBinaryName(typeElement.simpleName.toString() + "$" + className)
}

val AnnotationMirror.valueFieldOfClasses: Set<DeclaredType>
    get() = getAnnotationValue(this, "value").accept(object :
        SimpleAnnotationValueVisitor8<Set<DeclaredType>, Any?>() {
        override fun visitType(typeMirror: TypeMirror, v: Any?): Set<DeclaredType>? {
            return setOf(MoreTypes.asDeclared(typeMirror))
        }

        override fun visitArray(values: List<AnnotationValue>, v: Any?): Set<DeclaredType>? {
            return values.flatMap { value ->
                value.accept(this, null)
            }.toSet()
        }
    }, null)

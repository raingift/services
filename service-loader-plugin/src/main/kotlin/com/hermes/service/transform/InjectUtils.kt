package com.hermes.service.transform

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.InputStream

/**
 * Code instrumentation logic needs to be customized by the biz
 *
 * for example as below:
 * generate register code into ServiceRegistry.class
 */
@Suppress("SpellCheckingInspection")
object InjectUtils {
    // refer hack class when object init
    fun referHackWhenInit(
        inputStream: InputStream,
        targetList: MutableMap<String, MutableList<String>>
    ): ByteArray {
        val cr = ClassReader(inputStream)
        val cw = ClassWriter(cr, 0)
        val cv = InjectClassVisitor(Opcodes.ASM7, cw, targetList)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    private class InjectClassVisitor(
        api: Int,
        cv: ClassVisitor,
        val targetList: MutableMap<String, MutableList<String>>? = null
    ) : ClassVisitor(api, cv) {

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            var mv = super.visitMethod(access, name, desc, signature, exceptions)
            // generate code into this method
            if (name == "<clinit>" && desc == "()V") {
                mv = ServiceMethodVisitor(Opcodes.ASM7, mv, targetList)
            }
            return mv
        }
    }

    /**
     * @ServiceRegistry
     *  static {
     *        ......
     *      registry("com.A.class", "com.AImpl_Creator")
     *      registry("com.B.class", "com.BImpl_Creator")
     *        ......
     *      registry("com.X.class", "com.XImpl_Creator")
     *   }
     *
     * */
    private class ServiceMethodVisitor(
        api: Int,
        mv: MethodVisitor,
        val targetList: MutableMap<String, MutableList<String>>? = null
    ) : MethodVisitor(api, mv) {

        override fun visitInsn(opcode: Int) {
            // generate code before return
            if (opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                targetList?.forEach { (interf, implList) ->
                    implList.forEach { name ->
                        println("name >>> $name")

                        val interfName = interf.replace("/", ".")
                        val className = name.replace("/", ".") + "_Init"
                        mv.visitLdcInsn(interfName)// 类名
                        mv.visitLdcInsn(className)// 类名
                        // generate invoke register method into ServiceRegistry.loadRouterMap()
                        mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            GENERATE_TO_CLASS_NAME,
                            REGISTER_METHOD_NAME,
                            "(Ljava/lang/String;Ljava/lang/String;)V",
                            false
                        )
                    }
                }
            }
            super.visitInsn(opcode)
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}
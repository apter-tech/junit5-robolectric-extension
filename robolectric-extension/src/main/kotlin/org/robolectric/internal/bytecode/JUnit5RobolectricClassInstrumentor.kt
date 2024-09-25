package org.robolectric.internal.bytecode

import com.google.common.collect.Iterables
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.JSRInlinerAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.robolectric.util.PerfStatsCollector
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import kotlin.math.max

@Suppress("TooManyFunctions")
internal class JUnit5RobolectricClassInstrumentor : ClassInstrumentor() {

    private val decorator: Decorator = ShadowDecorator()

    companion object {
        private val BOOTSTRAP: Handle
        private val BOOTSTRAP_STATIC: Handle
        private val BOOTSTRAP_INTRINSIC: Handle
        private const val ROBO_INIT_METHOD_NAME = "\$\$robo\$init"
        private val OBJECT_TYPE: Type = Type.getType(Object::class.java)
        private val SHADOW_IMPL = ShadowImpl()

        init {
            val className = Type.getInternalName(InvokeDynamicSupport::class.java)

            val bootstrap = MethodType.methodType(
                CallSite::class.java,
                MethodHandles.Lookup::class.java,
                String::class.java,
                MethodType::class.java
            )

            /*
             * There is an additional int.class argument to the invokedynamic bootstrap method. This conveys
             * whether or not the method invocation represents a native method. A one means the original
             * method was a native method, and a zero means it was not. It should be boolean.class, but
             * that is nt possible due to https://bugs.java.com/bugdatabase/view_bug?bug_id=JDK-8322510.
             */
            val bootstrapMethod =
                bootstrap.appendParameterTypes(
                    MethodHandle::class.java,
                    /* isNative */
                    Int::class.javaPrimitiveType,
                ).toMethodDescriptorString()
            val bootstrapIntrinsic = bootstrap.appendParameterTypes(String::class.java).toMethodDescriptorString()

            BOOTSTRAP = Handle(Opcodes.H_INVOKESTATIC, className, "bootstrap", bootstrapMethod, false)
            BOOTSTRAP_STATIC = Handle(
                Opcodes.H_INVOKESTATIC,
                className,
                "bootstrapStatic",
                bootstrapMethod,
                false,
            )
            BOOTSTRAP_INTRINSIC = Handle(
                Opcodes.H_INVOKESTATIC, className, "bootstrapIntrinsic", bootstrapIntrinsic, false
            )
        }

        private fun directMethodName(mutableClass: MutableClass, originalName: String?): String {
            return SHADOW_IMPL.directMethodName(mutableClass.name, originalName)
        }

        /**
         * Verifies if the @targetMethod is a `<init>(boolean)` constructor for [ ].
         */
        private fun isGregorianCalendarBooleanConstructor(targetMethod: MethodInsnNode): Boolean {
            return (
                (targetMethod.owner == "java/util/GregorianCalendar") &&
                    (targetMethod.name == "<init>") && (targetMethod.desc == "(Z)V")
                )
        }

        /**
         * Replaces the void `<init>(boolean)` constructor for a call to the `void <init>(int,
         * int, int)` one.
         */
        private fun replaceGregorianCalendarBooleanConstructor(
            instructions: MutableListIterator<AbstractInsnNode>,
            targetMethod: MethodInsnNode
        ) {
            // Remove the call to GregorianCalendar(boolean)
            instructions.remove()

            // Discard the already-pushed parameter for GregorianCalendar(boolean)
            instructions.add(InsnNode(Opcodes.POP))

            // Add parameters values for calling GregorianCalendar(int, int, int)
            instructions.add(InsnNode(Opcodes.ICONST_0))
            instructions.add(InsnNode(Opcodes.ICONST_0))
            instructions.add(InsnNode(Opcodes.ICONST_0))

            // Call GregorianCalendar(int, int, int)
            instructions.add(
                MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    targetMethod.owner,
                    targetMethod.name,
                    "(III)V",
                    targetMethod.itf
                )
            )
        }

        /** Replaces protected and private class modifiers with public.  */
        private fun makeClassPublic(clazz: ClassNode) {
            clazz.access = (clazz.access or Opcodes.ACC_PUBLIC) and (Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE).inv()
        }

        private fun generateStaticInitializerNotifierMethod(mutableClass: MutableClass): MethodNode {
            val methodNode = MethodNode(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                "()V",
                null,
            )
            val generator = RobolectricGeneratorAdapter(methodNode)
            generator.push(mutableClass.classType)
            generator.invokeStatic(
                Type.getType(RobolectricInternals::class.java),
                Method("classInitializing", "(Ljava/lang/Class;)V")
            )
            generator.returnValue()
            generator.endMethod()
            return methodNode
        }
    }

    private fun analyzeClass(
        origClassBytes: ByteArray,
        config: InstrumentationConfiguration,
        classNodeProvider: ClassNodeProvider
    ): MutableClass {
        val classNode: ClassNode = object : ClassNode(Opcodes.ASM4) {
            override fun visitMethod(
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<String>?
            ): MethodVisitor {
                val methodVisitor = super.visitMethod(access, name, config.remapParams(desc), signature, exceptions)
                return JSRInlinerAdapter(methodVisitor, access, name, desc, signature, exceptions)
            }
        }

        val classReader = ClassReader(origClassBytes)
        classReader.accept(classNode, 0)
        return MutableClass(classNode, config, classNodeProvider)
    }

    override fun instrumentToBytes(mutableClass: MutableClass): ByteArray {
        instrument(mutableClass)

        val classNode = mutableClass.classNode
        val writer: ClassWriter = InstrumentingClassWriter(mutableClass.classNodeProvider, classNode)
        val remapper: Remapper = object : Remapper() {
            override fun map(internalName: String): String {
                return mutableClass.config.mappedTypeName(internalName)
            }
        }
        val visitor = ClassRemapper(writer, remapper)
        classNode.accept(visitor)

        return writer.toByteArray()
    }

    override fun instrument(
        classDetails: ClassDetails,
        config: InstrumentationConfiguration,
        classNodeProvider: ClassNodeProvider
    ): ByteArray {
        val perfStats = PerfStatsCollector.getInstance()
        val mutableClass = perfStats.measure<MutableClass, RuntimeException>("analyze class") {
            analyzeClass(
                classDetails.classBytes,
                config,
                classNodeProvider
            )
        }
        val instrumentedBytes = perfStats.measure<ByteArray, RuntimeException>("instrument class") {
            instrumentToBytes(
                mutableClass
            )
        }
        recordPackageStats(perfStats, mutableClass)
        return instrumentedBytes
    }

    private fun recordPackageStats(perfStats: PerfStatsCollector, mutableClass: MutableClass) {
        val className = mutableClass.name
        var i = className.indexOf('.')
        while (i != -1) {
            perfStats.incrementCount("instrument package " + className.substring(0, i))
            i = className.indexOf('.', i + 1)
        }
    }

    override fun instrument(mutableClass: MutableClass) {
        try {
            // Need Java version >=7 to allow invokedynamic
            mutableClass.classNode.version =
                max(mutableClass.classNode.version.toDouble(), Opcodes.V1_7.toDouble()).toInt()

            if (mutableClass.name == "android.util.SparseArray") {
                addSetToSparseArray(mutableClass)
            }

            instrumentMethods(mutableClass)

            if (mutableClass.isInterface) {
                mutableClass.addInterface(Type.getInternalName(InstrumentedInterface::class.java))
            } else {
                makeClassPublic(mutableClass.classNode)
                if ((mutableClass.classNode.access and Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
                    mutableClass.classNode.visitAnnotation(
                        "Lcom/google/errorprone/annotations/DoNotMock;",
                        true,
                    ).visit(
                        "value",
                        "This class is final. Consider using the real thing, or " +
                            "adding/enhancing a Robolectric shadow for it."
                    )
                }
                mutableClass.classNode.access = mutableClass.classNode.access and Opcodes.ACC_FINAL.inv()

                // If there is no constructor, adds one
                addNoArgsConstructor(mutableClass)

                addRoboInitMethod(mutableClass)

                removeFinalFromFields(mutableClass)

                decorator.decorate(mutableClass)
            }
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception
        ) {
            @Suppress("TooGenericExceptionThrown")
            throw RuntimeException("failed to instrument " + mutableClass.name, e)
        }
    }

    // See https://github.com/robolectric/robolectric/issues/6840
    // Adds Set(int, object) to android.util.SparseArray.
    private fun addSetToSparseArray(mutableClass: MutableClass) {
        for (method: MethodNode in mutableClass.methods) {
            if (("set" == method.name)) {
                return
            }
        }

        val setFunction = MethodNode(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC,
            "set",
            "(ILjava/lang/Object;)V",
            "(ITE;)V",
            null
        )
        val generator = RobolectricGeneratorAdapter(setFunction)
        generator.loadThis()
        generator.loadArg(0)
        generator.loadArg(1)
        generator.invokeVirtual(mutableClass.classType, Method("put", "(ILjava/lang/Object;)V"))
        generator.returnValue()
        mutableClass.addMethod(setFunction)
    }

    /**
     * Checks if the first or second instruction is a Jacoco load instruction. Robolectric is not
     * capable at the moment of re-instrumenting Jacoco-instrumented constructors, so these are
     * currently skipped.
     *
     * @param ctor constructor method node
     * @return whether or not the constructor can be instrumented
     */
    @Suppress("ReturnCount")
    private fun isJacocoInstrumented(ctor: MethodNode): Boolean {
        val insns = ctor.instructions.toArray()
        if (insns.size > 1) {
            var node: AbstractInsnNode? = insns[0]
            if (node is LabelNode) {
                node = insns[1]
            }
            if ((node is LdcInsnNode && node.cst is ConstantDynamic)) {
                val cst = node.cst as ConstantDynamic
                return (cst.name == "\$jacocoData")
            } else if (node is MethodInsnNode) {
                return (node.name == "\$jacocoInit")
            }
        }
        return false
    }

    /**
     * Adds a call $$robo$init, which instantiates a shadow object if required. This is to support
     * custom shadows for Jacoco-instrumented classes (except cnstructor shadows).
     */
    override fun addCallToRoboInit(mutableClass: MutableClass, ctor: MethodNode) {
        val returnNode = Iterables.find(
            ctor.instructions,
            { node: AbstractInsnNode ->
                if (node.getOpcode() == Opcodes.INVOKESPECIAL) {
                    val mNode: MethodInsnNode = node as MethodInsnNode
                    return@find (
                        (
                            (mNode.owner == mutableClass.internalClassName) ||
                                (mNode.owner == mutableClass.classNode.superName)
                            )
                        )
                }
                false
            },
            null
        )
        ctor.instructions.insert(
            returnNode,
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                mutableClass.classType.internalName,
                ROBO_INIT_METHOD_NAME,
                "()V"
            )
        )
        ctor.instructions.insert(returnNode, VarInsnNode(Opcodes.ALOAD, 0))
    }

    @Suppress("NestedBlockDepth")
    private fun instrumentMethods(mutableClass: MutableClass) {
        if (mutableClass.isInterface) {
            for (method: MethodNode in mutableClass.methods) {
                rewriteMethodBody(mutableClass, method)
            }
        } else {
            for (method: MethodNode in mutableClass.methods) {
                rewriteMethodBody(mutableClass, method)

                if ((method.name == "<clinit>")) {
                    method.name = ShadowConstants.STATIC_INITIALIZER_METHOD_NAME
                    mutableClass.addMethod(generateStaticInitializerNotifierMethod(mutableClass))
                } else if ((method.name == "<init>")) {
                    if (isJacocoInstrumented(method)) {
                        addCallToRoboInit(mutableClass, method)
                    } else {
                        instrumentConstructor(mutableClass, method)
                    }
                } else if (!isSyntheticAccessorMethod(method) && !Modifier.isAbstract(method.access)) {
                    instrumentNormalMethod(mutableClass, method)
                }
            }
        }
    }

    private fun addNoArgsConstructor(mutableClass: MutableClass) {
        if (!mutableClass.foundMethods.contains("<init>()V")) {
            val defaultConstructor = MethodNode(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC,
                "<init>",
                "()V",
                "()V",
                null,
            )
            val generator = RobolectricGeneratorAdapter(defaultConstructor)
            generator.loadThis()
            generator.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                mutableClass.classNode.superName,
                "<init>",
                "()V",
                false
            )
            generator.loadThis()
            generator.invokeVirtual(mutableClass.classType, Method(ROBO_INIT_METHOD_NAME, "()V"))
            generator.returnValue()
            mutableClass.addMethod(defaultConstructor)
        }
    }

    /**
     * Generates code like this:
     *
     * <pre>
     * protected void $$robo$init() {
     *      if (__robo_data__ == null) {
     *          __robo_data__ = RobolectricInternals.initializing(this);
     *      }
     * }
     * </pre>
     */
    private fun addRoboInitMethod(mutableClass: MutableClass) {
        val initMethodNode = MethodNode(
            Opcodes.ACC_PROTECTED or Opcodes.ACC_SYNTHETIC,
            ROBO_INIT_METHOD_NAME,
            "()V",
            null,
            null,
        )
        val generator = RobolectricGeneratorAdapter(initMethodNode)
        val alreadyInitialized = Label()
        generator.loadThis() // this
        generator.getField(
            mutableClass.classType,
            ShadowConstants.CLASS_HANDLER_DATA_FIELD_NAME,
            OBJECT_TYPE,
        ) // contents of __robo_data__
        generator.ifNonNull(alreadyInitialized)
        generator.loadThis() // this
        generator.loadThis() // this, this
        writeCallToInitializing(mutableClass, generator)
        // this, __robo_data__
        generator.putField(
            mutableClass.classType,
            ShadowConstants.CLASS_HANDLER_DATA_FIELD_NAME,
            OBJECT_TYPE,
        )
        generator.mark(alreadyInitialized)
        generator.returnValue()
        mutableClass.addMethod(initMethodNode)
    }

    override fun writeCallToInitializing(
        mutableClass: MutableClass,
        generator: RobolectricGeneratorAdapter
    ) {
        generator.invokeStatic(
            Type.getType(RobolectricInternals::class.java),
            Method("initializing", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE))
        )
    }

    private fun removeFinalFromFields(mutableClass: MutableClass) {
        for (fieldNode: FieldNode in mutableClass.fields) {
            fieldNode.access = fieldNode.access and Modifier.FINAL.inv()
        }
    }

    private fun isSyntheticAccessorMethod(method: MethodNode): Boolean {
        return (method.access and Opcodes.ACC_SYNTHETIC) != 0
    }

    /**
     * Constructors are instrumented as follows:
     *
     *
     *  * The original constructor will be stripped of its instructions leading up to, and
     * including, the call to super() or this(). It is also renamed to $$robo$$__constructor__
     *  * A method called __constructor__ is created and its job is to call
     * $$robo$$__constructor__. The __constructor__ method is what gets shadowed if a Shadow
     * wants to shadow a constructor.
     *  * A new constructor is created and contains the stripped instructions of the original
     * constructor leading up to, and including, the call to super() or this(). Then, it has a
     * call to $$robo$init to initialize the Class' Shadow Object. Then, it uses invokedynamic
     * to call __constructor__. Finally, it contains any instructions that might occur after the
     * return statement in the original constructor.
     *
     *
     * @param method the constructor to instrument
     */
    override fun instrumentConstructor(mutableClass: MutableClass, method: MethodNode) {
        val methodAccess = method.access
        makeMethodPrivate(method)

        val callSuper = extractCallToSuperConstructor(mutableClass, method)
        method.name = directMethodName(mutableClass, ShadowConstants.CONSTRUCTOR_METHOD_NAME)
        mutableClass.addMethod(
            redirectorMethod(mutableClass, method, ShadowConstants.CONSTRUCTOR_METHOD_NAME)
        )

        val exceptions = exceptionArray(method)
        val initMethodNode = MethodNode(methodAccess, "<init>", method.desc, method.signature, exceptions)
        val generator = RobolectricGeneratorAdapter(initMethodNode)
        initMethodNode.instructions.add(callSuper)
        generator.loadThis()
        generator.invokeVirtual(mutableClass.classType, Method(ROBO_INIT_METHOD_NAME, "()V"))
        generateClassHandlerCall(
            mutableClass,
            method,
            ShadowConstants.CONSTRUCTOR_METHOD_NAME,
            generator,
            false
        )

        generator.endMethod()

        val postamble = extractInstructionsAfterReturn(method, initMethodNode)
        if (postamble.size() > 0) {
            initMethodNode.instructions.add(postamble)
        }
        mutableClass.addMethod(initMethodNode)
    }

    /**
     * Checks to see if there are instructions after RETURN. If there are, it will check to see if
     * they belong in the call-to-super, or the shadowable part of the constructor.
     */
    private fun extractInstructionsAfterReturn(method: MethodNode, initMethodNode: MethodNode): InsnList {
        val removedInstructions = InsnList()
        val returnNode = Iterables.find(
            method.instructions,
            { node: AbstractInsnNode -> node is InsnNode && node.getOpcode() == Opcodes.RETURN },
            null
        )
        if (returnNode == null) {
            return removedInstructions
        }
        if (returnNode.next is LabelNode) {
            // There are instructions after the return, check where they belong. Note this is a very rare
            // edge case and only seems to happen with desugared+proguarded classes such as
            // play-services-basement's ApiException.
            val labelAfterReturn = returnNode.next as LabelNode
            val inInitMethodNode = Iterables.any(
                initMethodNode.instructions
            ) { input: AbstractInsnNode? ->
                (input is JumpInsnNode && input.label === labelAfterReturn)
            }

            if (inInitMethodNode) {
                while (returnNode.next != null) {
                    val node = returnNode.next
                    method.instructions.remove(node)
                    removedInstructions.add(node)
                }
            }
        }
        return removedInstructions
    }

    @Suppress("NestedBlockDepth")
    private fun extractCallToSuperConstructor(
        mutableClass: MutableClass,
        ctor: MethodNode
    ): InsnList {
        val removedInstructions = InsnList()
        // Start removing instructions at the beginning of the method. The first instructions of
        // constructors may vary.
        var startIndex = 0

        val insns = ctor.instructions.toArray()
        for (i in insns.indices) {
            val node = insns[i]

            when (node.opcode) {
                Opcodes.INVOKESPECIAL -> {
                    val mnode = node as MethodInsnNode
                    if (
                        (mnode.owner == mutableClass.internalClassName) ||
                        (mnode.owner == mutableClass.classNode.superName)
                    ) {
                        if ("<init>" != mnode.name) {
                            throw AssertionError("Invalid MethodInsnNode name")
                        }

                        // remove all instructions in the range 0 (the start) to invokespecial
                        // <init>
                        while (startIndex <= i) {
                            ctor.instructions.remove(insns[startIndex])
                            removedInstructions.add(insns[startIndex])
                            startIndex++
                        }
                        return removedInstructions
                    }
                }

                Opcodes.ATHROW -> {
                    ctor.visitCode()
                    ctor.visitInsn(Opcodes.RETURN)
                    ctor.visitEnd()
                    return removedInstructions
                }

                else -> {}
            }
        }

        @Suppress("TooGenericExceptionThrown")
        throw RuntimeException("huh? " + ctor.name + ctor.desc)
    }

    /**
     * Instruments a normal method
     *
     *
     *  * Rename the method from `methodName` to `$$robo$$methodName`.
     *  * Make it private so we can invoke it directly without subclass overrides taking
     * precedence.
     *  * Remove `final` modifiers, if present.
     *  * Create a delegator method named `methodName` which delegates to the [       ].
     *
     */
    override fun instrumentNormalMethod(mutableClass: MutableClass, method: MethodNode) {
        // if not abstract, set a final modifier
        if ((method.access and Opcodes.ACC_ABSTRACT) == 0) {
            method.access = method.access or Opcodes.ACC_FINAL
        }
        val isNativeMethod = (method.access and Opcodes.ACC_NATIVE) != 0
        if (isNativeMethod) {
            instrumentNativeMethod(mutableClass, method)
        }

        // Create delegator method with same name as original method. The delegator method will use
        // invokedynamic to decide at runtime whether to call original method or shadowed method
        val originalName = method.name
        method.name = directMethodName(mutableClass, originalName)

        val delegatorMethodNode = MethodNode(
            method.access,
            originalName,
            method.desc,
            method.signature,
            exceptionArray(method)
        )
        delegatorMethodNode.visibleAnnotations = method.visibleAnnotations
        delegatorMethodNode.access =
            delegatorMethodNode.access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT or Opcodes.ACC_FINAL).inv()

        makeMethodPrivate(method)

        val generator = RobolectricGeneratorAdapter(delegatorMethodNode)
        generateClassHandlerCall(mutableClass, method, originalName, generator, isNativeMethod)
        generator.endMethod()
        mutableClass.addMethod(delegatorMethodNode)
    }

    /**
     * Creates native stub which returns the default return value.
     *
     * @param mutableClass Class to be instrumented
     * @param method Method to be instrumented, must be native
     */
    override fun instrumentNativeMethod(mutableClass: MutableClass, method: MethodNode) {
        val nativeBindingMethodName = SHADOW_IMPL.directNativeMethodName(mutableClass.name, method.name)

        // Generate native binding method
        val nativeBindingMethod = MethodNode(
            Opcodes.ASM4,
            nativeBindingMethodName,
            method.desc,
            method.signature,
            exceptionArray(method)
        )
        nativeBindingMethod.access = method.access or Opcodes.ACC_SYNTHETIC
        makeMethodPrivate(nativeBindingMethod)
        mutableClass.addMethod(nativeBindingMethod)

        method.access = method.access and Opcodes.ACC_NATIVE.inv()

        val generator = RobolectricGeneratorAdapter(method)

        val returnType = generator.returnType
        generator.pushDefaultReturnValueToStack(returnType)
        generator.returnValue()
    }

    // todo rename
    private fun redirectorMethod(
        mutableClass: MutableClass,
        method: MethodNode,
        newName: String
    ): MethodNode {
        val redirector = MethodNode(
            Opcodes.ASM4,
            newName,
            method.desc,
            method.signature,
            exceptionArray(method)
        )
        redirector.access = method.access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT or Opcodes.ACC_FINAL).inv()
        makeMethodPrivate(redirector)
        val generator = RobolectricGeneratorAdapter(redirector)
        generator.invokeMethod(mutableClass.internalClassName, method)
        generator.returnValue()
        return redirector
    }

    override fun exceptionArray(method: MethodNode): Array<String> {
        val exceptions = method.exceptions
        return exceptions.toTypedArray<String>()
    }

    /** Filters methods that might need special treatment because of various reasons  */
    private fun rewriteMethodBody(mutableClass: MutableClass, callingMethod: MethodNode) {
        val instructions = callingMethod.instructions.iterator()
        while (instructions.hasNext()) {
            val node = instructions.next()

            when (node.opcode) {
                Opcodes.NEW -> {
                    val newInsnNode = node as TypeInsnNode
                    newInsnNode.desc = mutableClass.config.mappedTypeName(newInsnNode.desc)
                }

                Opcodes.GETFIELD, Opcodes.PUTFIELD, Opcodes.GETSTATIC, Opcodes.PUTSTATIC -> {
                    val fieldInsnNode = node as FieldInsnNode
                    fieldInsnNode.desc = mutableClass.config.mappedTypeName(fieldInsnNode.desc) // todo test
                }

                Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE, Opcodes.INVOKESPECIAL, Opcodes.INVOKEVIRTUAL -> {
                    val targetMethod = node as MethodInsnNode
                    targetMethod.desc = mutableClass.config.remapParams(targetMethod.desc)
                    if (isGregorianCalendarBooleanConstructor(targetMethod)) {
                        replaceGregorianCalendarBooleanConstructor(instructions, targetMethod)
                    } else if (mutableClass.config.shouldIntercept(targetMethod)) {
                        interceptInvokeVirtualMethod(mutableClass, instructions, targetMethod)
                    }
                }

                Opcodes.INVOKEDYNAMIC -> {}
                else -> {}
            }
        }
    }

    /**
     * Decides to call through the appropriate method to intercept the method with an INVOKEVIRTUAL
     * Opcode, depending if the invokedynamic bytecode instruction is available (Java 7+).
     */
    override fun interceptInvokeVirtualMethod(
        mutableClass: MutableClass?,
        instructions: MutableListIterator<AbstractInsnNode>,
        targetMethod: MethodInsnNode
    ) {
        instructions.remove() // remove the method invocation

        val type = Type.getObjectType(targetMethod.owner)
        var description = targetMethod.desc
        val owner = type.className

        if (targetMethod.opcode != Opcodes.INVOKESTATIC) {
            val thisType = type.descriptor
            description = "(" + thisType + description.substring(1)
        }

        instructions.add(
            InvokeDynamicInsnNode(targetMethod.name, description, BOOTSTRAP_INTRINSIC, owner)
        )
    }

    /** Replaces protected and public class modifiers with private.  */
    override fun makeMethodPrivate(method: MethodNode) {
        method.access = (method.access or Opcodes.ACC_PRIVATE) and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED).inv()
    }

    // todo javadocs
    override fun generateClassHandlerCall(
        mutableClass: MutableClass,
        originalMethod: MethodNode,
        originalMethodName: String?,
        generator: RobolectricGeneratorAdapter,
        isNativeMethod: Boolean
    ) {
        val original = Handle(
            getTag(originalMethod),
            mutableClass.classType.internalName,
            originalMethod.name,
            originalMethod.desc,
            getTag(originalMethod) == Opcodes.H_INVOKEINTERFACE
        )

        if (generator.isStatic) {
            generator.loadArgs()
            generator.invokeDynamic(
                originalMethodName,
                originalMethod.desc,
                BOOTSTRAP_STATIC,
                original,
                isNativeMethod
            )
        } else {
            val desc = "(" + mutableClass.classType.descriptor + originalMethod.desc.substring(1)
            generator.loadThis()
            generator.loadArgs()
            generator.invokeDynamic(originalMethodName, desc, BOOTSTRAP, original, isNativeMethod)
        }

        generator.returnValue()
    }

    override fun getTag(m: MethodNode): Int {
        return if (Modifier.isStatic(m.access)) Opcodes.H_INVOKESTATIC else Opcodes.H_INVOKESPECIAL
    }

    // implemented in DirectClassInstrumentor
    override fun setAndroidJarSDKVersion(androidJarSDKVersion: Int) = Unit

    // implemented in DirectClassInstrumentor
    override fun getAndroidJarSDKVersion(): Int {
        return -1
    }
}

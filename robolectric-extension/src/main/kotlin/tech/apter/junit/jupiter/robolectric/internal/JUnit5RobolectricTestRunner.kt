package tech.apter.junit.jupiter.robolectric.internal

import java.lang.reflect.Method
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.SandboxManager.SandboxBuilder
import org.robolectric.internal.SandboxTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.internal.bytecode.Sandbox
import org.robolectric.util.inject.Injector

internal class JUnit5RobolectricTestRunner(clazz: Class<*>, injector: Injector = defaultInjectorBuilder().build()) :
    RobolectricTestRunner(clazz, injector) {
    private val logger get() = createLogger()
    fun frameworkMethod(method: Method): FrameworkMethod = children.first { it.name == method.name }

    fun bootstrapSdkEnvironment(): Sandbox = sdkEnvironment(children.first())

    fun sdkEnvironment(frameworkMethod: FrameworkMethod): Sandbox {
        return getSandbox(frameworkMethod).also {
            configureSandbox(it, frameworkMethod)
        }
    }

    fun runBeforeTest(
        sdkEnvironment: Sandbox,
        frameworkMethod: FrameworkMethod,
        bootstrappedMethod: Method,
    ) {
        logger.trace {
            "runBeforeTest ${bootstrappedMethod.declaringClass.simpleName}::${bootstrappedMethod.name}"
        }
        super.beforeTest(sdkEnvironment, frameworkMethod, bootstrappedMethod)
    }

    fun runAfterTest(frameworkMethod: FrameworkMethod, bootstrappedMethod: Method) {
        logger.trace {
            "runAfterTest${bootstrappedMethod.declaringClass.simpleName}::${bootstrappedMethod.name}"
        }
        try {
            super.afterTest(frameworkMethod, bootstrappedMethod)
        } finally {
            super.finallyAfterTest(frameworkMethod)
        }
    }

    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration {
        return InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotAcquireClass(JUnit5RobolectricSandboxBuilder::class.java)
            .build()
    }

    override fun computeTestMethods() = computeJUnit5TestMethods()

    override fun isIgnored(child: FrameworkMethod) = isJUnit5Ignored(child)

    override fun validatePublicVoidNoArgMethods(
        annotation: Class<out Annotation>,
        isStatic: Boolean,
        errors: MutableList<Throwable>,
    ) = validatePublicVoidNoArgJUnit5Methods(annotation, isStatic, errors)

    override fun getHelperTestRunner(bootstrappedTestClass: Class<*>): SandboxTestRunner.HelperTestRunner =
        HelperTestRunner(bootstrappedTestClass)

    private class HelperTestRunner(bootstrappedTestClass: Class<*>) :
        RobolectricTestRunner.HelperTestRunner(bootstrappedTestClass) {
        override fun computeTestMethods(): MutableList<FrameworkMethod> = computeJUnit5TestMethods()

        override fun isIgnored(child: FrameworkMethod) = isJUnit5Ignored(child)

        override fun validatePublicVoidNoArgMethods(
            annotation: Class<out Annotation>,
            isStatic: Boolean,
            errors: MutableList<Throwable>,
        ) = validatePublicVoidNoArgJUnit5Methods(annotation, isStatic, errors)
    }

    private companion object {
        private fun defaultInjectorBuilder() = defaultInjector()
            .bind(SandboxBuilder::class.java, JUnit5RobolectricSandboxBuilder::class.java)
    }
}

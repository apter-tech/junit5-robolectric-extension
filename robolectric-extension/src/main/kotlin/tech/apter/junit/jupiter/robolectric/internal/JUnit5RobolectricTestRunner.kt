package tech.apter.junit.jupiter.robolectric.internal

import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.SandboxManager
import org.robolectric.internal.SandboxManager.SandboxBuilder
import org.robolectric.internal.SandboxTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.internal.bytecode.Sandbox
import org.robolectric.internal.dependency.MavenDependencyResolver
import org.robolectric.util.inject.Injector
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import tech.apter.junit.jupiter.robolectric.internal.extensions.hasTheSameParameterTypes
import tech.apter.junit.jupiter.robolectric.internal.extensions.outerMostDeclaringClass
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal data class TestClassContainer(val testClass: Class<*>)

@Suppress("TooManyFunctions")
internal class JUnit5RobolectricTestRunner(
    clazz: Class<*>,
    injector: Injector = defaultInjectorBuilder().bind(
        TestClassContainer::class.java,
        TestClassContainer(testClass = clazz),
    ).build(),
) : RobolectricTestRunner(clazz, injector) {
    private inline val logger get() = createLogger()
    private val childrenCache = mutableListOf<FrameworkMethod>()
    private val sandboxLock = Any()

    override fun getChildren(): MutableList<FrameworkMethod> {
        if (childrenCache.isEmpty()) {
            synchronized(childrenCache) {
                if (childrenCache.isEmpty()) {
                    childrenCache.addAll(super.getChildren())
                }
            }
        }
        return childrenCache
    }

    fun frameworkMethod(method: Method): FrameworkMethod = children.first {
        method.name == it.method.name &&
            method.declaringClass.name == it.declaringClass.name &&
            method.hasTheSameParameterTypes(it.method)
    }

    fun bootstrapSdkEnvironment(): AndroidSandbox = sdkEnvironment(children.first())

    fun sdkEnvironment(frameworkMethod: FrameworkMethod): AndroidSandbox {
        return getSandbox(frameworkMethod).also {
            configureSandbox(it, frameworkMethod)
        }
    }

    override fun getSandbox(method: FrameworkMethod): AndroidSandbox {
        synchronized(sandboxLock) {
            val originalClassLoader = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = createParentClassLoader(testClass.javaClass)
            return super.getSandbox(method).also {
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
        }
    }

    fun runBeforeTest(
        sdkEnvironment: Sandbox,
        frameworkMethod: FrameworkMethod,
        bootstrappedMethod: Method,
    ) {
        beforeTestLock(testClass.javaClass).withLock {
            logger.trace { "runBeforeTest ${bootstrappedMethod.declaringClass.simpleName}::${bootstrappedMethod.name}" }
            super.beforeTest(sdkEnvironment, frameworkMethod, bootstrappedMethod)
        }
    }

    fun runAfterTest(frameworkMethod: FrameworkMethod, bootstrappedMethod: Method) {
        logger.trace { "runAfterTest ${frameworkMethod.declaringClass.simpleName}::${frameworkMethod.name}" }
        super.afterTest(frameworkMethod, bootstrappedMethod)
    }

    fun runFinallyAfterTest(frameworkMethod: FrameworkMethod) {
        logger.trace { "runFinallyAfterTest ${frameworkMethod.declaringClass.simpleName}::${frameworkMethod.name}" }
        super.finallyAfterTest(frameworkMethod)
    }

    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration {
        return InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotAcquirePackage("tech.apter.junit.jupiter.robolectric.internal.")
            .doNotAcquireClass(RobolectricExtension::class.java).build()
    }

    override fun computeTestMethods() = computeJUnit5TestMethods()

    override fun validateNoNonStaticInnerClass(errors: MutableList<Throwable>) {
        // Skip validation
    }

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

        override fun validateNoNonStaticInnerClass(errors: MutableList<Throwable>) {
            // Skip validation
        }

        override fun isIgnored(child: FrameworkMethod) = isJUnit5Ignored(child)

        override fun validatePublicVoidNoArgMethods(
            annotation: Class<out Annotation>,
            isStatic: Boolean,
            errors: MutableList<Throwable>,
        ) = validatePublicVoidNoArgJUnit5Methods(annotation, isStatic, errors)
    }

    private companion object {
        private val beforeTestLocks = ConcurrentHashMap<String, ReentrantReadWriteLock>()
        private val sdkSandboxParentClassLoaderCache = ConcurrentHashMap<String, ClassLoader>()

        private fun defaultInjectorBuilder() =
            defaultInjector().bind(SandboxBuilder::class.java, JUnit5RobolectricSandboxBuilder::class.java)
                .bind(MavenDependencyResolver::class.java, JUnit5MavenDependencyResolver::class.java)
                .bind(SandboxManager::class.java, JUnit5RobolectricSandboxManager::class.java)

        private fun beforeTestLock(testClass: Class<*>): Lock =
            beforeTestLocks.getOrPut(testClass.outerMostDeclaringClass().name) {
                ReentrantReadWriteLock()
            }.writeLock()

        private fun createParentClassLoader(testClass: Class<*>) =
            sdkSandboxParentClassLoaderCache.getOrPut(testClass.outerMostDeclaringClass().name) {
                createLogger().trace {
                    "parent class loader created for ${testClass.name.substringAfterLast('.')}"
                }
                SdkSandboxParentClassLoader(Thread.currentThread().contextClassLoader)
            }
    }
}

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package tech.apter.junit.jupiter.robolectric.internal

import com.google.common.annotations.VisibleForTesting
import jdk.internal.loader.BuiltinClassLoader
import jdk.internal.loader.URLClassPath
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.runners.model.FrameworkMethod
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.internal.bytecode.Sandbox
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import tech.apter.junit.jupiter.robolectric.internal.extensions.isExtendedWithRobolectric
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNestedTest
import tech.apter.junit.jupiter.robolectric.internal.extensions.outerMostDeclaringClass
import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions")
internal class JUnit5RobolectricTestRunnerHelper(testClass: Class<*>) {
    private inline val logger get() = createLogger()
    private var _robolectricTestRunner: JUnit5RobolectricTestRunner? = null

    init {
        validateNestedTestClassCanNotOverrideRuntimeSdk(testClass)
        validateNestedTestClassCanNotApplyAnnotations(
            testClass,
            LooperMode::class.java,
            GraphicsMode::class.java,
        )
        createTestEnvironmentForClass(testClass)
    }

    @VisibleForTesting
    val robolectricTestRunner: JUnit5RobolectricTestRunner get() = requireNotNull(_robolectricTestRunner)

    fun sdkEnvironment(testMethod: Method?): Sandbox {
        return if (testMethod == null) {
            robolectricTestRunner.bootstrapSdkEnvironment()
        } else {
            val frameworkMethod = robolectricTestRunner.frameworkMethod(testMethod)
            return robolectricTestRunner.sdkEnvironment(frameworkMethod)
        }
    }

    private fun validateNestedTestClassCanNotOverrideRuntimeSdk(testClass: Class<*>) {
        val config = testClass.getAnnotation(Config::class.java)
        if (testClass.isNestedTest && config != null && config.sdk.isNotEmpty()) {
            error("Robolectric runtime sdk cannot be overwritten on a nested test class: ${testClass.name}")
        }
    }

    private fun validateNestedTestClassCanNotApplyAnnotations(
        testClass: Class<*>,
        vararg annotationsClasses: Class<out Annotation>,
    ) {
        annotationsClasses.forEach { annotationClass ->
            val annotation = testClass.getAnnotation(annotationClass)
            if (annotation != null) {
                error("")
            }
        }
    }

    private fun validateNestedTestMethodsCanNotOverrideRuntimeSdk(testMethod: FrameworkMethod) {
        val config = testMethod.getAnnotation(Config::class.java)
        if (config != null && config.sdk.isNotEmpty()) {
            error(
                "Robolectric runtime sdk cannot be overwritten on a test method: " +
                    "${testMethod.declaringClass.simpleName}::${testMethod.name}"
            )
        }
    }

    private fun createTestEnvironmentForClass(testClass: Class<*>) {
        _robolectricTestRunner = JUnit5RobolectricTestRunner(testClass)
    }

    fun beforeEach(testMethod: Method) {
        val frameworkMethod = robolectricTestRunner.frameworkMethod(testMethod)
        val sdkEnvironment = robolectricTestRunner.sdkEnvironment(frameworkMethod)
        logger.trace { "$sdkEnvironment beforeEach(${testMethod.name})" }
        validateNestedTestMethodsCanNotOverrideRuntimeSdk(frameworkMethod)
        sdkEnvironment.runOnMainThreadWithRobolectric {
            robolectricTestRunner.runBeforeTest(sdkEnvironment, frameworkMethod, testMethod)
        }
    }

    fun afterEach(testMethod: Method) {
        val frameworkMethod = robolectricTestRunner.frameworkMethod(testMethod)
        val sdkEnvironment = sdkEnvironment(testMethod)
        logger.trace { "$sdkEnvironment afterEach(${testMethod.name})" }
        sdkEnvironment.runOnMainThread(
            Callable {
                robolectricTestRunner.runAfterTest(frameworkMethod, testMethod)
                sdkEnvironment.runWithRobolectric {
                    robolectricTestRunner.runFinallyAfterTest(frameworkMethod)
                }
            }
        )
    }

    fun proceedInvocation(testMethod: Method?, invocation: InvocationInterceptor.Invocation<Void>) {
        val sdkEnvironment = sdkEnvironment(testMethod)
        logger.trace { "$sdkEnvironment processInvocation(${testMethod?.name})" }
        sdkEnvironment.runOnMainThreadWithRobolectric { invocation.proceed() }
    }

    fun clearCachedRobolectricTestRunnerEnvironment() {
        _robolectricTestRunner = null
    }

    internal companion object {
        @VisibleForTesting
        internal var interceptedClassLoader: ClassLoader? = null
        private val helperRunnerCache by lazy { ConcurrentHashMap<String, JUnit5RobolectricTestRunnerHelper>() }
        private val appClassLoaderCache by lazy { ConcurrentHashMap<Class<*>, ClassLoader>() }

        internal fun setUp() {
            check(interceptedClassLoader == null) { "interceptedClassLoader is already set" }
            val originalClassLoader = checkNotNull(Thread.currentThread().contextClassLoader) {
                "Current thread's contextClassLoader must be not null"
            }
            interceptedClassLoader = originalClassLoader
            Thread.currentThread().contextClassLoader = RobolectricTestClassClassLoader(
                originalClassLoader,
                ::robolectricClassLoaderFactory,
            )
        }

        internal fun reset() {
            checkNotNull(interceptedClassLoader) { "interceptedClassLoader is not yet set" }
            helperRunnerCache.values.forEach {
                it.clearCachedRobolectricTestRunnerEnvironment()
            }
            helperRunnerCache.clear()
            appClassLoaderCache.clear()
            Thread.currentThread().contextClassLoader = interceptedClassLoader
        }

        internal fun getInstance(testClass: Class<*>): JUnit5RobolectricTestRunnerHelper {
            require(testClass.isExtendedWithRobolectric()) {
                "Test class ${testClass.name} is not extended with ${RobolectricExtension::class.simpleName}"
            }
            return helperRunnerCache.getOrPut(testClass.name) {
                val originalClassLoader = Thread.currentThread().contextClassLoader
                val appClassLoader = appClassLoaderCache.getOrPut(testClass.outerMostDeclaringClass()) {
                    createLogger().trace {
                        "app class loader created for ${testClass.name.substringAfterLast('.')}"
                    }
                    createAppClassLoader(originalClassLoader)
                }
                // Make sure robolectric class loader's parent is a new appClassLoader
                Thread.currentThread().contextClassLoader = appClassLoader
                JUnit5RobolectricTestRunnerHelper(testClass).also {
                    Thread.currentThread().contextClassLoader = originalClassLoader
                }
            }
        }

        private fun createAppClassLoader(originalClassLoader: ClassLoader): ClassLoader {
            val builtinClassLoader = originalClassLoader as BuiltinClassLoader
            val appClassLoaderClass = Class.forName("jdk.internal.loader.ClassLoaders\$AppClassLoader")
            val constructor = appClassLoaderClass.getDeclaredConstructor(
                BuiltinClassLoader::class.java,
                URLClassPath::class.java
            )
            constructor.isAccessible = true
            val instance = constructor.newInstance(
                builtinClassLoader,
                urlClassPath(builtinClassLoader)
            ) as ClassLoader
            constructor.isAccessible = false
            return instance
        }

        private fun urlClassPath(builtinClassLoader: BuiltinClassLoader): URLClassPath {
            val field = BuiltinClassLoader::class.java.getDeclaredField("ucp")
            field.isAccessible = true
            val ucp = field[builtinClassLoader] as URLClassPath
            field.isAccessible = false
            return ucp
        }
    }
}

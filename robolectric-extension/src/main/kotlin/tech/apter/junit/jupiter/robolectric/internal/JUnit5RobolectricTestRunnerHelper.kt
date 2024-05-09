@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package tech.apter.junit.jupiter.robolectric.internal

import com.google.common.annotations.VisibleForTesting
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.robolectric.internal.bytecode.Sandbox
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import tech.apter.junit.jupiter.robolectric.internal.extensions.isExtendedWithRobolectric
import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions")
internal class JUnit5RobolectricTestRunnerHelper private constructor(testClass: Class<*>) {
    private inline val logger get() = createLogger()
    private var _robolectricTestRunner: JUnit5RobolectricTestRunner? = null

    init {
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

    private fun createTestEnvironmentForClass(testClass: Class<*>) {
        _robolectricTestRunner = JUnit5RobolectricTestRunner(testClass)
    }

    fun beforeEach(testMethod: Method) {
        val frameworkMethod = robolectricTestRunner.frameworkMethod(testMethod)
        val sdkEnvironment = robolectricTestRunner.sdkEnvironment(frameworkMethod)
        logger.trace { "$sdkEnvironment beforeEach(${testMethod.name})" }
        sdkEnvironment.runOnMainThreadWithRobolectric {
            robolectricTestRunner.runBeforeTest(sdkEnvironment, frameworkMethod, testMethod)
        }
    }

    fun afterEach(testMethod: Method) {
        val frameworkMethod = robolectricTestRunner.frameworkMethod(testMethod)
        val sdkEnvironment = sdkEnvironment(testMethod)
        logger.trace { "$sdkEnvironment afterEach(${testMethod.name})" }
        with(sdkEnvironment) {
            runOnMainThread(
                Callable {
                    runWithRobolectric {
                        robolectricTestRunner.runAfterTest(frameworkMethod, testMethod)
                    }
                    robolectricTestRunner.runFinallyAfterTest(this, frameworkMethod)
                }
            )
        }
    }

    fun proceedInvocation(testMethod: Method?, invocation: InvocationInterceptor.Invocation<Void>) {
        val sdkEnvironment = sdkEnvironment(testMethod)
        logger.trace { "$sdkEnvironment processInvocation(${testMethod?.name})" }
        sdkEnvironment.runOnMainThreadWithRobolectric { invocation.proceed() }
    }

    internal companion object {
        @VisibleForTesting
        internal var interceptedClassLoader: ClassLoader? = null
        private val helperRunnerCache by lazy { ConcurrentHashMap<String, JUnit5RobolectricTestRunnerHelper>() }

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

        internal fun shutdown() {
            checkNotNull(interceptedClassLoader) { "interceptedClassLoader is not yet set" }
            helperRunnerCache.clear()
            Thread.currentThread().contextClassLoader = interceptedClassLoader
        }

        internal fun getInstance(testClass: Class<*>): JUnit5RobolectricTestRunnerHelper {
            require(testClass.isExtendedWithRobolectric()) {
                "Test class ${testClass.name} is not extended with ${RobolectricExtension::class.simpleName}"
            }
            return helperRunnerCache.getOrPut(testClass.name) {
                JUnit5RobolectricTestRunnerHelper(testClass)
            }
        }
    }
}

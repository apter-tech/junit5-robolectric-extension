package tech.apter.junit.jupiter.robolectric.internal

import com.google.common.annotations.VisibleForTesting
import org.junit.runners.model.FrameworkMethod
import org.robolectric.internal.bytecode.Sandbox
import java.lang.reflect.Method

internal class JUnit5RobolectricTestRunnerHelper {
    private inline val logger get() = createLogger()
    private var _robolectricTestRunner: JUnit5RobolectricTestRunner? = null
    private var _sdkEnvironment: Sandbox? = null
    private var _frameworkMethod: FrameworkMethod? = null

    @VisibleForTesting
    val robolectricTestRunner: JUnit5RobolectricTestRunner get() = requireNotNull(_robolectricTestRunner)
    val sdkEnvironment: Sandbox get() = requireNotNull(_sdkEnvironment)

    @VisibleForTesting
    val frameworkMethod: FrameworkMethod get() = requireNotNull(_frameworkMethod)

    fun loadRobolectricClassLoader() {
        logger.trace { "loadRobolectricClassLoader" }
        Thread.currentThread().replaceClassLoader(sdkEnvironment.robolectricClassLoader).also {
            if (interceptedClassLoader == null) {
                interceptedClassLoader = it
            }
        }
    }

    fun resetClassLoaderToOriginal() {
        logger.trace { "resetClassLoaderToOriginal" }
        if (interceptedClassLoader != null) {
            Thread.currentThread().contextClassLoader = interceptedClassLoader
        }
    }

    fun createTestEnvironmentForClass(testClass: Class<*>) {
        _robolectricTestRunner = JUnit5RobolectricTestRunner(testClass)
        _sdkEnvironment = robolectricTestRunner.bootstrapSdkEnvironment()
    }

    fun createTestEnvironmentForMethod(testMethod: Method) {
        val frameworkMethod = robolectricTestRunner.frameworkMethod(testMethod)
        _sdkEnvironment = robolectricTestRunner.sdkEnvironment(frameworkMethod)
        _frameworkMethod = frameworkMethod
    }

    fun beforeEach(testMethod: Method) {
        robolectricTestRunner.runBeforeTest(sdkEnvironment, frameworkMethod, testMethod)
    }

    fun afterEach(testMethod: Method) {
        robolectricTestRunner.runAfterTest(frameworkMethod, testMethod)
    }

    fun afterEachFinally() {
        robolectricTestRunner.runFinallyAfterTest(frameworkMethod)
    }

    fun clearCachedRobolectricTestRunnerEnvironment() {
        _robolectricTestRunner = null
        _sdkEnvironment = null
        _frameworkMethod = null
    }

    internal companion object {
        internal var interceptedClassLoader: ClassLoader? = null
            private set
    }
}

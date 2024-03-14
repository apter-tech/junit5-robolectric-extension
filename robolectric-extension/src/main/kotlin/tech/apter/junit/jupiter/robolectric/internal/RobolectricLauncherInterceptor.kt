package tech.apter.junit.jupiter.robolectric.internal

import org.junit.platform.launcher.LauncherInterceptor
import tech.apter.junit.jupiter.robolectric.internal.placeholder.RobolectricPlaceholderTest
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("LibraryEntitiesShouldNotBePublic")
class RobolectricLauncherInterceptor : LauncherInterceptor {
    private inline val logger get() = createLogger()
    private val testRunnerHelper by lazy { JUnit5RobolectricTestRunnerHelper() }

    override fun <T : Any?> intercept(invocation: LauncherInterceptor.Invocation<T>): T {
        logger.trace { "intercept" }

        if (!robolectricLoaded.getAndSet(true)) {
            // Create a dummy environment before JUnit5 Jupiter Engine creates any test instance.
            // The test class should be loaded by the org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader.
            // Otherwise, the InstrumentationRegistry.getInstrumentation() would be null.
            // This environment should be replaced before starting any test.
            testRunnerHelper.createTestEnvironmentForClass(RobolectricPlaceholderTest::class.java)
            testRunnerHelper.loadRobolectricClassLoader()
            testRunnerHelper.clearCachedRobolectricTestRunnerEnvironment()
        }

        return invocation.proceed()
    }

    override fun close() {
        logger.trace { "close" }
        testRunnerHelper.resetClassLoaderToOriginal()
        AndroidMainThreadExecutor.shutdown()
    }

    companion object {
        @JvmStatic
        private val robolectricLoaded = AtomicBoolean(false)
    }
}

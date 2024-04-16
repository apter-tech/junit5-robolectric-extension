package tech.apter.junit.jupiter.robolectric.internal

import org.junit.platform.launcher.LauncherInterceptor
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("LibraryEntitiesShouldNotBePublic")
class RobolectricLauncherInterceptor : LauncherInterceptor {
    private inline val logger get() = createLogger()

    override fun <T : Any?> intercept(invocation: LauncherInterceptor.Invocation<T>): T {
        logger.trace { "intercept" }

        if (!classLoaderReplaced.getAndSet(true)) {
            JUnit5RobolectricTestRunnerHelper.setUp()
        }

        return invocation.proceed()
    }

    override fun close() {
        logger.trace { "close" }
        JUnit5RobolectricTestRunnerHelper.reset()
        classLoaderReplaced.set(false)
    }

    private companion object {
        @JvmStatic
        private val classLoaderReplaced = AtomicBoolean(false)
    }
}

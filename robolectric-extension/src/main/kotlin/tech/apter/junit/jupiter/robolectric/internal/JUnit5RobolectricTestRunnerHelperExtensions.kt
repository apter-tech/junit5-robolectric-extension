package tech.apter.junit.jupiter.robolectric.internal

import java.util.concurrent.Callable

internal fun <T> JUnit5RobolectricTestRunnerHelper.runOnMainThread(
    action: JUnit5RobolectricTestRunnerHelper.() -> T
): T =
    sdkEnvironment.runOnMainThread(
        Callable {
            action()
        }
    )

internal fun <T> JUnit5RobolectricTestRunnerHelper.runOnMainThreadWithRobolectric(
    action: JUnit5RobolectricTestRunnerHelper.() -> T
): T =
    runOnMainThread {
        runWithRobolectric(action)
    }

internal fun <T> JUnit5RobolectricTestRunnerHelper.runWithRobolectric(
    action: JUnit5RobolectricTestRunnerHelper.() -> T
): T {
    loadRobolectricClassLoader()
    return action().also {
        resetClassLoaderToOriginal()
    }
}

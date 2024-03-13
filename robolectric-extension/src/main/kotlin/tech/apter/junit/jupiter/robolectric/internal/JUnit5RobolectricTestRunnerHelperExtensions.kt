package tech.apter.junit.jupiter.robolectric.internal

import java.util.concurrent.Callable

internal inline fun <T> JUnit5RobolectricTestRunnerHelper.runOnMainThread(crossinline action: JUnit5RobolectricTestRunnerHelper.() -> T): T =
    sdkEnvironment.runOnMainThread(
        Callable {
            action()
        }
    )

internal inline fun <T> JUnit5RobolectricTestRunnerHelper.runOnMainThreadWithRobolectric(crossinline action: JUnit5RobolectricTestRunnerHelper.() -> T): T =
    runOnMainThread {
        runWithRobolectric(action)
    }

internal inline fun <T> JUnit5RobolectricTestRunnerHelper.runWithRobolectric(crossinline action: JUnit5RobolectricTestRunnerHelper.() -> T): T {
    loadRobolectricClassLoader()
    return action().also {
        resetClassLoaderToOriginal()
    }
}

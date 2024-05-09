package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.internal.bytecode.Sandbox
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import java.util.concurrent.Callable

internal fun <T> Sandbox.runOnMainThreadWithRobolectric(action: () -> T): T {
    return runOnMainThread(
        Callable {
            return@Callable runWithRobolectric(action)
        }
    )
}

internal fun <T> Sandbox.runWithRobolectric(action: () -> T): T {
    loadRobolectricClassLoader()
    return action().also {
        resetClassLoaderToOriginal()
    }
}

internal fun Sandbox.loadRobolectricClassLoader() {
    createLogger().trace {
        "loadRobolectricClassLoader ${robolectricClassLoader.javaClass.simpleName}@${
            System.identityHashCode(
                robolectricClassLoader
            )
        }"
    }
    Thread.currentThread().contextClassLoader = robolectricClassLoader
}

internal fun Sandbox.resetClassLoaderToOriginal() {
    createLogger().trace {
        "resetClassLoaderToOriginal ${robolectricClassLoader.parent.javaClass.simpleName}@${
            System.identityHashCode(
                robolectricClassLoader.parent
            )
        }"
    }
    Thread.currentThread().contextClassLoader = robolectricClassLoader.parent
}

internal fun Sandbox.clearShadowLooperCache() {
    val shadowLooperClass = robolectricClassLoader.loadClass("org.robolectric.shadows.ShadowLooper")
    shadowLooperClass.getDeclaredMethod("clearLooperMode").invoke(null)
}

internal fun Sandbox.resetLooper() {
    resetMainLooper()
    resetMyLooper()
}

private fun Sandbox.resetMainLooper() {
    val looperClass = robolectricClassLoader.loadClass("android.os.Looper")

    @Suppress("DiscouragedPrivateApi")
    val sMainLooperField = looperClass.getDeclaredField("sMainLooper")
    sMainLooperField.isAccessible = true
    sMainLooperField.set(null, null)
    sMainLooperField.isAccessible = false
}

private fun Sandbox.resetMyLooper() {
    val looperClass = robolectricClassLoader.loadClass("android.os.Looper")

    @Suppress("DiscouragedPrivateApi")
    val sThreadLocalField = looperClass.getDeclaredField("sThreadLocal")
    sThreadLocalField.isAccessible = true
    val threadLocal = (sThreadLocalField.get(null) as ThreadLocal<*>)
    threadLocal.remove()
    sThreadLocalField.isAccessible = false
}

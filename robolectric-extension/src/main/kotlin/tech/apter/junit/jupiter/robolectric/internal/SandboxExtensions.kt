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

internal fun <T> Sandbox.runWithRobolectricParent(action: () -> T): T {
    loadRobolectricParentClassLoader()
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

internal fun Sandbox.loadRobolectricParentClassLoader() {
    createLogger().trace {
        "loadRobolectricParentClassLoader ${robolectricClassLoader.javaClass.simpleName}@${
            System.identityHashCode(
                robolectricClassLoader.parent
            )
        }"
    }
    Thread.currentThread().contextClassLoader = robolectricClassLoader.parent
}

internal fun Sandbox.resetClassLoaderToOriginal() {
    createLogger().trace {
        "resetClassLoaderToOriginal ${robolectricClassLoader.parent.javaClass.simpleName}@${
            System.identityHashCode(
                robolectricClassLoader.parent.parent
            )
        }"
    }
    Thread.currentThread().contextClassLoader = robolectricClassLoader.parent.parent
}

package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.ApkLoader
import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.bytecode.ShadowProviders
import org.robolectric.pluginapi.Sdk
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

@Suppress("LongParameterList")
internal class JUnit5RobolectricAndroidSandbox(
    runtimeSdk: Sdk,
    compileSdk: Sdk,
    resourcesMode: ResourcesMode,
    apkLoader: ApkLoader,
    testEnvironmentSpec: TestEnvironmentSpec,
    sdkSandboxClassLoader: SdkSandboxClassLoader,
    shadowProviders: ShadowProviders,
    sqLiteMode: SQLiteMode.Mode,
) : AndroidSandbox(
    runtimeSdk,
    compileSdk,
    resourcesMode,
    apkLoader,
    testEnvironmentSpec,
    sdkSandboxClassLoader,
    shadowProviders,
    sqLiteMode,
) {
    override fun mainThreadFactory(): ThreadFactory {
        return ThreadFactory { r: Runnable? ->
            val name = "${createThreadId()}-SDK-${sdk.apiLevel}"
            Thread(ThreadGroup(name), r, "Main-Thread-$name")
        }
    }

    private companion object {
        private val threadIds = AtomicLong(1)
        private fun createThreadId(): Long = threadIds.getAndIncrement()
    }
}

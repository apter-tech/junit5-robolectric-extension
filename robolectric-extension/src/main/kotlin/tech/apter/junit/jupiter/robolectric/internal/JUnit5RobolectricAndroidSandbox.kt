package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.ApkLoader
import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.bytecode.ShadowProviders
import org.robolectric.pluginapi.Sdk
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadFactory

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
            val name = "SDK-${sdk.apiLevel}"
            Thread(ThreadGroup(name), r, "$name-Main-Thread-${sdk.createThreadId()}")
        }
    }

    private companion object {
        private val threadIds = ConcurrentHashMap<String, Int>()
        private fun Sdk.createThreadId(): Int = threadIds.getOrPut("$apiLevel") { 1 }
            .also { threadIds["$apiLevel"] = it + 1 }
    }
}

package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.ApkLoader
import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.bytecode.ShadowProviders
import org.robolectric.pluginapi.Sdk
import java.util.concurrent.Callable

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
    override fun <T : Any?> runOnMainThread(callable: Callable<T>): T =
        AndroidMainThreadExecutor.execute(callable)
}

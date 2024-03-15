package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.ApkLoader
import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.SandboxManager
import org.robolectric.internal.bytecode.ClassInstrumentor
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.internal.bytecode.ShadowProviders
import org.robolectric.pluginapi.Sdk
import java.util.Collections
import javax.inject.Inject

internal class JUnit5RobolectricSandboxBuilder @Inject constructor(
    private val apkLoader: ApkLoader,
    @Suppress("VisibleForTests")
    private val testEnvironmentSpec: AndroidSandbox.TestEnvironmentSpec,
    private val shadowProviders: ShadowProviders,
    private val classInstrumentor: ClassInstrumentor,
) : SandboxManager.SandboxBuilder {
    private val logger get() = createLogger()

    override fun build(
        instrumentationConfig: InstrumentationConfiguration,
        runtimeSdk: Sdk,
        compileSdk: Sdk,
        resourcesMode: ResourcesMode,
        sqLiteMode: SQLiteMode.Mode,
    ): AndroidSandbox {
        logger.trace { "build" }
        val sdkSandboxClassLoader = getOrCreateClassLoader(instrumentationConfig, runtimeSdk)
        return JUnit5RobolectricAndroidSandbox(
            runtimeSdk,
            compileSdk,
            resourcesMode,
            apkLoader,
            testEnvironmentSpec,
            sdkSandboxClassLoader,
            shadowProviders,
            sqLiteMode,
        )
    }

    private fun getOrCreateClassLoader(
        instrumentationConfig: InstrumentationConfiguration,
        runtimeSdk: Sdk,
    ): SdkSandboxClassLoader {
        val key = Key(instrumentationConfig, runtimeSdk)
        return classLoaderCache.getOrPut(key) {
            logger.debug { "Create ${SdkSandboxClassLoader::class.simpleName} instance for $runtimeSdk." }
            SdkSandboxClassLoader(instrumentationConfig, runtimeSdk, classInstrumentor)
        }
    }

    private data class Key(
        private val configuration: InstrumentationConfiguration,
        private val runtimeSdk: Sdk,
    )

    private companion object {
        @JvmStatic
        private val classLoaderCache = Collections.synchronizedMap<Key, SdkSandboxClassLoader>(mutableMapOf())
    }
}

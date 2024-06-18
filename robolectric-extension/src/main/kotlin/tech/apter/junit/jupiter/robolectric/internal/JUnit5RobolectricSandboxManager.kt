package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.SandboxManager
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.pluginapi.Sdk
import org.robolectric.plugins.SdkCollection
import tech.apter.junit.jupiter.robolectric.internal.extensions.outerMostDeclaringClass
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class JUnit5RobolectricSandboxManager @Inject constructor(
    private val testClassContainer: TestClassContainer,
    private val sandboxBuilder: SandboxBuilder,
    private val sdkCollection: SdkCollection,
) : SandboxManager(sandboxBuilder, sdkCollection) {

    @Synchronized
    override fun getAndroidSandbox(
        instrumentationConfig: InstrumentationConfiguration,
        sdk: Sdk,
        resourcesMode: ResourcesMode,
        looperMode: LooperMode.Mode,
        sqliteMode: SQLiteMode.Mode,
        graphicsMode: GraphicsMode.Mode,
    ): AndroidSandbox {
        val testClass = testClassContainer.testClass
        val compileSdk = sdkCollection.maxSupportedSdk

        val key = SandboxKey(
            testClassName = testClass.outerMostDeclaringClass().name,
            instrumentationConfiguration = instrumentationConfig,
            sdk = sdk,
            resourcesMode = resourcesMode,
        )
        // Return the same sandbox for nested tests
        return sandboxCache.getOrPut(key) {
            sandboxBuilder.build(instrumentationConfig, sdk, compileSdk, resourcesMode, sqliteMode)
        }.apply {
            updateModes(sqliteMode)
        }
    }

    private data class SandboxKey(
        private val testClassName: String,
        private val instrumentationConfiguration: InstrumentationConfiguration,
        private val sdk: Sdk,
        private val resourcesMode: ResourcesMode,
    )

    private companion object {
        private val sandboxCache = ConcurrentHashMap<SandboxKey, AndroidSandbox>()
    }
}

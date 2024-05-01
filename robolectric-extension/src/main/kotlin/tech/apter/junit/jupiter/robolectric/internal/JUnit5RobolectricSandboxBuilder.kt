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
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import tech.apter.junit.jupiter.robolectric.internal.extensions.outerMostDeclaringClass
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class JUnit5RobolectricSandboxBuilder @Inject constructor(
    private val testClassContainer: TestClassContainer,
    private val apkLoader: ApkLoader,
    @Suppress("VisibleForTests")
    private val testEnvironmentSpec: AndroidSandbox.TestEnvironmentSpec,
    private val shadowProviders: ShadowProviders,
    private val classInstrumentor: ClassInstrumentor,
) : SandboxManager.SandboxBuilder {
    private inline val logger get() = createLogger()

    override fun build(
        instrumentationConfig: InstrumentationConfiguration,
        runtimeSdk: Sdk,
        compileSdk: Sdk,
        resourcesMode: ResourcesMode,
        sqLiteMode: SQLiteMode.Mode,
    ): AndroidSandbox {
        val testClass = testClassContainer.testClass
        logger.trace { "build AndroidSandbox[$runtimeSdk] ${testClass.name.substringAfterLast('.')}" }
        val sdkSandboxClassLoader = createClassLoader(testClass, instrumentationConfig, runtimeSdk)
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

    private fun createClassLoader(
        testClass: Class<*>,
        instrumentationConfig: InstrumentationConfiguration,
        runtimeSdk: Sdk,
    ): SdkSandboxClassLoader {
        val outerMostDeclaringClassName = testClass.outerMostDeclaringClass().name
        return classLoaderCache.getOrPut(outerMostDeclaringClassName) {
            logger.debug {
                "Create ${SdkSandboxClassLoader::class.simpleName}[$runtimeSdk] instance for ${
                    outerMostDeclaringClassName.substringAfterLast('.')
                }"
            }
            SdkSandboxClassLoader(instrumentationConfig, runtimeSdk, classInstrumentor)
        }
    }

    private companion object {
        private val classLoaderCache = ConcurrentHashMap<String, SdkSandboxClassLoader>()
    }
}

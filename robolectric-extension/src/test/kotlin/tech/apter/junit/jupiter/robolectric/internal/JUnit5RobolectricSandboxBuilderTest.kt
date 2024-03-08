package tech.apter.junit.jupiter.robolectric.internal

import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.ApkLoader
import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.internal.bytecode.ShadowProviders
import org.robolectric.internal.dependency.MavenDependencyResolver
import org.robolectric.pluginapi.Sdk
import org.robolectric.plugins.DefaultSdkProvider
import org.robolectric.versioning.AndroidVersions.T
import org.robolectric.versioning.AndroidVersions.U
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@ExtendWith(RobolectricExtension::class)
@Ignore
class JUnit5RobolectricSandboxBuilderTest {

    private val dependencyResolver = MavenDependencyResolver()
    private val defaultSdkProvider = DefaultSdkProvider(dependencyResolver)

    @Test
    fun `Given the same arguments when call build twice then should return two different sandboxes with the same classloader `() {
        // Given
        val instrumentationConfiguration = createInstrumentationConfiguration()
        val runtimeSdk: Sdk = defaultSdkProvider.DefaultSdk(U.SDK_INT, "14", "10818077", "REL", 17)
        val compileSdk: Sdk = defaultSdkProvider.DefaultSdk(U.SDK_INT, "14", "10818077", "REL", 17)
        val resourcesMode: ResourcesMode = ResourcesMode.BINARY
        val sqLiteMode: SQLiteMode.Mode = SQLiteMode.Mode.NATIVE

        subjectUnderTest {
            // When
            val sandbox1 = build(instrumentationConfiguration, runtimeSdk, compileSdk, resourcesMode, sqLiteMode)
            // And
            val sandbox2 = build(instrumentationConfiguration, runtimeSdk, compileSdk, resourcesMode, sqLiteMode)

            // Then
            assertIs<AndroidSandbox>(sandbox1)
            assertIs<AndroidSandbox>(sandbox2)
            assertNotSame(sandbox1, sandbox2)
            assertSame(sandbox1.robolectricClassLoader, sandbox2.robolectricClassLoader)
        }
    }

    @Test
    fun `Given different arguments when call build twice then should return two different sandboxes with different classloaders`() {
        // Given
        val instrumentationConfiguration1 = createInstrumentationConfiguration()
        val runtimeSdk1: Sdk = defaultSdkProvider.DefaultSdk(U.SDK_INT, "14", "10818077", "REL", 17)
        val compileSdk1: Sdk = defaultSdkProvider.DefaultSdk(U.SDK_INT, "14", "10818077", "REL", 17)
        val instrumentationConfiguration2 = createInstrumentationConfiguration()
        val runtimeSdk2: Sdk = defaultSdkProvider.DefaultSdk(T.SDK_INT, "13", "9030017", "Tiramisu", 9)
        val compileSdk2: Sdk = defaultSdkProvider.DefaultSdk(T.SDK_INT, "13", "9030017", "Tiramisu", 9)
        val resourcesMode: ResourcesMode = ResourcesMode.BINARY
        val sqLiteMode: SQLiteMode.Mode = SQLiteMode.Mode.NATIVE

        subjectUnderTest {
            val sandbox1 = build(instrumentationConfiguration1, runtimeSdk1, compileSdk1, resourcesMode, sqLiteMode)
            val sandbox2 = build(instrumentationConfiguration2, runtimeSdk2, compileSdk2, resourcesMode, sqLiteMode)

            assertIs<AndroidSandbox>(sandbox1)
            assertIs<AndroidSandbox>(sandbox2)
            assertNotSame(sandbox1, sandbox2)
            assertNotSame(sandbox1.robolectricClassLoader, sandbox2.robolectricClassLoader)
        }
    }

    private fun subjectUnderTest(
        action: JUnit5RobolectricSandboxBuilder.() -> Unit
    ): JUnit5RobolectricSandboxBuilder = JUnit5RobolectricSandboxBuilder(
        ApkLoader(),
        AndroidSandbox.TestEnvironmentSpec(),
        ShadowProviders(emptyList()),
    ).apply {
        action()
    }

    companion object {
        private fun createInstrumentationConfiguration() =
            InstrumentationConfiguration.newBuilder().doNotAcquirePackage("java.")
                .doNotAcquirePackage("jdk.internal.")
                .doNotAcquirePackage("sun.")
                .doNotAcquirePackage("org.robolectric.annotation.")
                .doNotAcquirePackage("org.robolectric.internal.")
                .doNotAcquirePackage("org.robolectric.pluginapi.")
                .doNotAcquirePackage("org.robolectric.util.")
                .doNotAcquirePackage("org.junit")
                .build()
    }
}

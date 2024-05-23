package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.annotation.SQLiteMode
import org.robolectric.internal.AndroidSandbox
import org.robolectric.internal.ResourcesMode
import org.robolectric.internal.SandboxManager.SandboxBuilder
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.pluginapi.Sdk
import org.robolectric.plugins.SdkCollection
import org.robolectric.util.inject.Injector
import tech.apter.junit.jupiter.robolectric.tools.TestUtil
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@Ignore
class JUnit5RobolectricSandboxBuilderTest {

    @Test
    fun `Given the same arguments when call build twice then should return two different sandboxes with the same classloader `() {
        // Given
        val instrumentationConfiguration = createInstrumentationConfiguration()
        val runtimeSdk: Sdk = TestUtil.sdkCollection.getSdk(33)
        val compileSdk: Sdk = TestUtil.sdkCollection.getSdk(33)
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
        val runtimeSdk1: Sdk = TestUtil.sdkCollection.getSdk(33)
        val compileSdk1: Sdk = TestUtil.sdkCollection.getSdk(33)
        val instrumentationConfiguration2 = createInstrumentationConfiguration()
        val runtimeSdk2: Sdk = TestUtil.sdkCollection.getSdk(32)
        val compileSdk2: Sdk = TestUtil.sdkCollection.getSdk(32)
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
    ): JUnit5RobolectricSandboxBuilder = Injector.Builder()
        .bind(SandboxBuilder::class.java, JUnit5RobolectricSandboxBuilder::class.java)
        .bind(SdkCollection::class.java, TestUtil.sdkCollection)
        .build()
        .getInstance(JUnit5RobolectricSandboxBuilder::class.java)
        .apply(action)

    companion object {
        private fun createInstrumentationConfiguration() =
            InstrumentationConfiguration.newBuilder()
                .doNotAcquirePackage("java.")
                .doNotAcquirePackage("jdk.internal.")
                .doNotAcquirePackage("sun.")
                .doNotAcquirePackage("org.robolectric.annotation.")
                .doNotAcquirePackage("org.robolectric.internal.")
                .doNotAcquirePackage("org.robolectric.pluginapi.")
                .doNotAcquirePackage("org.robolectric.util.")
                .doNotAcquirePackage("org.junit")
                .doNotAcquireClass(JUnit5RobolectricSandboxBuilder::class.java)
                .build()
    }
}

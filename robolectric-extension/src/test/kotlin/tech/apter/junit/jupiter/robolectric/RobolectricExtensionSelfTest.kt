package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(RobolectricExtension::class)
@Config(application = RobolectricExtensionSelfTest.MyTestApplication::class)
@DisplayName("Given a test class extended with robolectric")
@Execution(ExecutionMode.SAME_THREAD)
class RobolectricExtensionSelfTest {
    @Test
    fun shouldInitializeAndBindApplicationButNotCallOnCreate() {
        val application = assertDoesNotThrow { getApplicationContext<Context>() }
        assertIs<MyTestApplication>(application, "application")
        assertTrue("onCreateCalled") { application.onCreateWasCalled }
        if (RuntimeEnvironment.useLegacyResources()) {
            assertNotNull(RuntimeEnvironment.getAppResourceTable(), "Application resource loader")
        }
    }

    @Test
    fun setStaticValue_shouldIgnoreFinalModifier() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "expected value")
        assertEquals("expected value", Build.MODEL)
    }

    @Test
    @Config(qualifiers = "fr")
    fun internalBeforeTest_testValuesResQualifiers_fr() {
        assertContains(RuntimeEnvironment.getQualifiers(), "fr")
    }

    @Test
    fun internalBeforeTest_testValuesResQualifiers() {
        assertContains(RuntimeEnvironment.getQualifiers(), "en")
    }

    @Test
    @Timeout(1000)
    fun whenTestHarnessUsesDifferentThread_shouldStillReportAsMainThread() {
        assertSame(Thread.currentThread(), Looper.getMainLooper().thread)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.KITKAT])
    @Ignore
    fun testVersionConfiguration() {
        assertEquals(Build.VERSION_CODES.KITKAT, Build.VERSION.SDK_INT)
        assertEquals("4.4", Build.VERSION.RELEASE)
    }

    @Test
    @Throws(Exception::class)
    fun hamcrestMatchersDontBlowUpDuringLinking() {
        MatcherAssert.assertThat(true, CoreMatchers.`is`(true))
    }

    @Nested
    @Config(qualifiers = "fr")
    @DisplayName("when nested test has fr res qualifier config")
    inner class CustomQualifierSelfTest {
        @Test
        fun `then runtime environment's qualifiers should contains fr`() {
            assertContains(RuntimeEnvironment.getQualifiers(), "fr")
        }
    }

    @Nested
    @DisplayName("when test nested")
    inner class NestedSelfTest {
        @Test
        fun `then robolectric should be available`() {
            val application = assertDoesNotThrow { getApplicationContext<Context>() }
            assertNotNull(application)
            assertIs<Application>(application, "application")
        }
    }

    companion object {
        private var onTerminateCalledFromMain: Boolean? = null

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun resetStaticState_shouldBeCalled_onMainThread() {
            assertNotNull(onTerminateCalledFromMain)
            assertTrue { requireNotNull(onTerminateCalledFromMain) }
        }
    }

    class MyTestApplication : Application() {
        internal var onCreateWasCalled = false

        override fun onCreate() {
            this.onCreateWasCalled = true
        }

        override fun onTerminate() {
            onTerminateCalledFromMain = Looper.getMainLooper().thread === Thread.currentThread()
        }
    }
}

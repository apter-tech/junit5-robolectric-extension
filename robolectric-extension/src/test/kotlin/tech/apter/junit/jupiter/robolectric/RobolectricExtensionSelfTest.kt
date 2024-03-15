package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.atomic.AtomicInteger
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
class RobolectricExtensionSelfTest {

    @Test
    fun shouldInitializeAndBindApplicationButNotCallOnCreate() {
        val application = assertDoesNotThrow { ApplicationProvider.getApplicationContext<Context>() }
        assertIs<MyTestApplication>(application, "application")
        assertTrue("onCreateCalled") { application.onCreateWasCalled }
        if (RuntimeEnvironment.useLegacyResources()) {
            assertNotNull(RuntimeEnvironment.getAppResourceTable(), "Application resource loader")
        }
    }

    @Test
    fun `Before the test before class should be fired one`() {
        assertEquals(1, beforeAllFired.get())
    }

    @Test
    fun setStaticValue_shouldIgnoreFinalModifier() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "expected value")
        assertEquals("expected value", Build.MODEL)
    }

    @Test
    @Config(qualifiers = "fr")
    fun internalBeforeTest_testValuesResQualifiers() {
        assertContains(RuntimeEnvironment.getQualifiers(), "fr")
    }

    @Test
    fun testMethod_shouldBeInvoked_onMainThread() {
        assertSame(Thread.currentThread(), Looper.getMainLooper().thread)
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

    companion object {
        private var onTerminateCalledFromMain: Boolean? = null
        private val beforeAllFired = AtomicInteger(0)

        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun setUpClass() {
            beforeAllFired.incrementAndGet()
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun resetStaticState_shouldBeCalled_onMainThread() {
            assertNotNull(onTerminateCalledFromMain)
            assertTrue { requireNotNull(onTerminateCalledFromMain) }
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun tearDown() {
            beforeAllFired.set(0)
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

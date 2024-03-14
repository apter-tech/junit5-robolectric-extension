package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(RobolectricExtension::class)
@Config(application = RobolectricExtensionSelfTest.MyTestApplication::class)
class RobolectricExtensionSelfTest {

    @Test
    fun shouldInitializeAndBindApplicationButNotCallOnCreate() {
        val application = ApplicationProvider.getApplicationContext<Context>()
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

    companion object {
        private var onTerminateCalledFromMain: Boolean? = null
        private val beforeAllFired = AtomicInteger(0)

        @BeforeAll
        @JvmStatic
        fun setUpClass() {
            beforeAllFired.incrementAndGet()
        }

        @AfterAll
        @JvmStatic
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

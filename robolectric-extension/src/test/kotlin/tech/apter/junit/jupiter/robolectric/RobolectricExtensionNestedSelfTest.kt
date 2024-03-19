package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionNestedSelfTest {
    @Nested
    inner class NestedSelfTest {
        @Test
        fun `Given a test extended with robolectric when call a nested test then robolectric should be available`() {
            val application = assertDoesNotThrow { ApplicationProvider.getApplicationContext<Context>() }
            assertNotNull(application)
            assertIs<Application>(application, "application")
        }
    }
}

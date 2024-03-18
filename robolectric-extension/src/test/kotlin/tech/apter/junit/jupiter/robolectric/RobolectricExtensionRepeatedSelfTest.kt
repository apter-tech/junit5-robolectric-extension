package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionRepeatedSelfTest {

    @RepeatedTest(REPEATED_TEST_COUNT)
    fun `Given a test extended with robolectric when call a repeated test then robolectric should be available`(
        testInfo: RepetitionInfo,
    ) {
        testCallCount++
        val application = assertDoesNotThrow { ApplicationProvider.getApplicationContext<Context>() }
        assertNotNull(application)
        assertIs<Application>(application, "application")
        assertEquals(REPEATED_TEST_COUNT, testInfo.totalRepetitions)
    }

    companion object {
        private const val REPEATED_TEST_COUNT = 3
        private var testCallCount: Int = 0

        @AfterAll
        @Throws(Exception::class)
        @JvmStatic
        fun `Repeated test should be called as much as REPEATED_TEST_COUNT`() {
            assertEquals(REPEATED_TEST_COUNT, testCallCount)
        }
    }
}

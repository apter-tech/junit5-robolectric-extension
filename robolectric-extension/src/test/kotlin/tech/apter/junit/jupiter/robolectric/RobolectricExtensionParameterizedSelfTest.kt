package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionParameterizedSelfTest {

    @ParameterizedTest
    @MethodSource("testParameters")
    fun `Given a test extended with robolectric when call parameterized test then robolectric should be available`(
        parameter: Int,
    ) {
        testCallCount++
        val application = assertDoesNotThrow { ApplicationProvider.getApplicationContext<Context>() }
        assertNotNull(application)
        assertIs<Application>(application, "application")
        assertContains(setOf(1, 2, 3), parameter)
    }

    companion object {
        private var testCallCount: Int = 0

        @JvmStatic
        private fun testParameters() = listOf(
            Arguments.of(1),
            Arguments.of(2),
            Arguments.of(3),
        )

        @AfterAll
        @Throws(Exception::class)
        @JvmStatic
        fun `Parameterized test should be called as much as testParameters size`() {
            assertEquals(testParameters().size, testCallCount)
        }
    }
}

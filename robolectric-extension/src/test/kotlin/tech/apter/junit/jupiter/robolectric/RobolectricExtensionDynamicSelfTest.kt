package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionDynamicSelfTest {
    @TestFactory
    fun generateDynamicTests() = testParameters.map { parameter ->
        DynamicTest.dynamicTest(
            "Given a test extended with robolectric when call parameterized test then robolectric should be available [$parameter]",
        ) {
            testCallCount++
            val application = assertDoesNotThrow { getApplicationContext<Context>() }
            assertNotNull(application)
            assertIs<Application>(application, "application")
            assertContains(setOf(1, 2, 3), parameter)
        }
    }

    companion object {
        private val testParameters = listOf(1, 2, 3)
        private var testCallCount: Int = 0

        @AfterAll
        @Throws(Exception::class)
        @JvmStatic
        fun `Parameterized test should be called as much as testParameters size`() {
            assertEquals(testParameters.size, testCallCount)
        }
    }
}

package tech.apter.junit.jupiter.robolectric

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull


@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionTestTemplateSelfTest {

    @TestTemplate
    @ExtendWith(RobolectricTestTemplateInvocationContextProvider::class)
    fun `Given a test extended with robolectric when call a test template then robolectric should be available`() {
        testCallCount++
        val application = assertDoesNotThrow { ApplicationProvider.getApplicationContext<Context>() }
        assertNotNull(application)
        assertIs<Application>(application, "application")
    }

    companion object {
        private var testCallCount: Int = 0

        @AfterAll
        @Throws(Exception::class)
        @JvmStatic
        fun `Test template should be called as much as defined RobolectricTestTemplateInvocationContextProvider`() {
            assertEquals(2, testCallCount)
        }
    }
}

private fun noOpTestTemplateInvocationContext(): TestTemplateInvocationContext =
    object : TestTemplateInvocationContext {}

private class RobolectricTestTemplateInvocationContextProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean = true

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        return Stream.of(noOpTestTemplateInvocationContext(), noOpTestTemplateInvocationContext())
    }
}

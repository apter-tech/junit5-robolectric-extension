package tech.apter.junit.jupiter.robolectric

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertContains

@ExtendWith(RobolectricExtension::class)
@Config(qualifiers = "fr")
@DisplayName("Given a test class with fr qualifier config")
class RobolectricExtensionFrQualifierSelfTest {
    @Test
    fun `then runtime environment's qualifiers should contains fr`() {
        assertContains(RuntimeEnvironment.getQualifiers(), "fr")
    }

    @Test
    @Config(qualifiers = "en")
    fun `and a test method with en qualifier then runtime environment's qualifiers should contains en`() {
        assertContains(RuntimeEnvironment.getQualifiers(), "en")
    }

    @Nested
    @DisplayName("and a nested test class without config")
    @Execution(ExecutionMode.SAME_THREAD)
    inner class NestedWithoutConfigSelfTest {
        @Test
        fun `then runtime environment's qualifiers should contains fr`() {
            assertContains(RuntimeEnvironment.getQualifiers(), "fr")
        }

        @Test
        @Config(qualifiers = "en")
        fun `and a test method with en qualifier then runtime environment's qualifiers should contains en`() {
            assertContains(RuntimeEnvironment.getQualifiers(), "en")
        }
    }

    @Nested
    @DisplayName("and a nested test class where config is overridden to en")
    @Config(qualifiers = "en")
    @Execution(ExecutionMode.SAME_THREAD)
    inner class QualifierOverrideNestedSelfTest {
        @Test
        fun `then runtime environment's qualifiers should contains en`() {
            assertContains(RuntimeEnvironment.getQualifiers(), "en")
        }
    }
}

package tech.apter.junit.jupiter.robolectric

import android.os.Looper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.annotation.LooperMode
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@ExtendWith(RobolectricExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Execution(ExecutionMode.SAME_THREAD)
class RobolectricExtensionLooperModeSelfTest {
    @Test
    @Order(1)
    fun `Given a test method with default looper mode then testMethod should be invoked on the main thread`() {
        assertSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
    }

    @Test
    @Order(2)
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `Given a test method with looper mode paused then testMethod should be invoked on the main thread`() {
        assertSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
    }

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    @Order(3)
    fun `Given a test method with looper mode instrumentation test when called after a paused test method then testMethod should be not invoked on the main thread`() {
        assertNotSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
    }

    @Test
    @Order(4)
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `Given a test method with looper mode paused when called after an instrumentation test method then testMethod should be invoked on the main thread`() {
        assertSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
    }

    @Nested
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    @DisplayName("Given a nested test class with looper mode instrumentation test")
    inner class NestedInstrumentationTest {

        @Test
        @LooperMode(LooperMode.Mode.PAUSED)
        @Order(1)
        fun `and a test method with looper mode paused then testMethod should be invoked on the main thread`() {
            assertSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
        }

        @Test
        @Order(2)
        fun `and a test method with looper with default looper mode set after paused test then testMethod should be not invoked on the main thread`() {
            assertNotSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
        }
    }

    @Nested
    @LooperMode(LooperMode.Mode.PAUSED)
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    @DisplayName("Given a nested test class with looper mode paused")
    inner class NestedPausedTest {
        @Test
        fun `and a test method with looper with default looper mode set then testMethod should be not invoked on the main thread`() {
            assertSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
        }

        @Test
        @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
        fun `and a test method with looper mode instrumentation test when call after a paused test then testMethod should be not invoked on the main thread`() {
            assertNotSame(Thread.currentThread(), Looper.getMainLooper()?.thread)
        }
    }
}

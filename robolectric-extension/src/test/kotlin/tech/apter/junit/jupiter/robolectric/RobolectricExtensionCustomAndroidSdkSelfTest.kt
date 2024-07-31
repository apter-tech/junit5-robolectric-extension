package tech.apter.junit.jupiter.robolectric

import android.os.Build
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
@Execution(ExecutionMode.SAME_THREAD)
class RobolectricExtensionCustomAndroidSdkSelfTest {
    @Test
    fun `Given a test class configured with custom runtime SDK then SDK_INT should be the version set up`() {
        assertEquals(Build.VERSION_CODES.LOLLIPOP, Build.VERSION.SDK_INT)
        assertEquals("5.0.2", Build.VERSION.RELEASE)
    }

    @Nested
    inner class NestedSelfTest {
        @Test
        fun `Given a test class configured with custom runtime SDK when call test from a nested test class then SDK_INT should be the version set up`() {
            assertEquals(Build.VERSION_CODES.LOLLIPOP, Build.VERSION.SDK_INT)
            assertEquals("5.0.2", Build.VERSION.RELEASE)
        }

        @Test
        fun `Given a test class configured with custom runtime SDK when call test from a nested test class then nested test class should be called with the same class loader as the outer test`() {
            assertSame(classLoader, javaClass.classLoader)
        }

        @Nested
        inner class TwoLevelNestedSelfTest {
            @Test
            fun `Given a test class configured with custom runtime SDK when call test from a nested test class then SDK_INT should be the version set up`() {
                assertEquals(Build.VERSION_CODES.LOLLIPOP, Build.VERSION.SDK_INT)
                assertEquals("5.0.2", Build.VERSION.RELEASE)
            }

            @Test
            fun `Given a test class configured with custom runtime SDK when call test from a nested test class then nested test class should be called with the same class loader as the outer test`() {
                assertSame(classLoader, javaClass.classLoader)
            }
        }
    }

    companion object {
        private lateinit var classLoader: ClassLoader

        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun beforeAll() {
            classLoader = Companion::class.java.classLoader
        }
    }
}

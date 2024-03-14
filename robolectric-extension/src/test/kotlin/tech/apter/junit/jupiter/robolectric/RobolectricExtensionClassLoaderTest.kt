package tech.apter.junit.jupiter.robolectric

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionClassLoaderTest {

    @BeforeTest
    fun setUp() {
        assertSdkClassLoader()
    }

    @AfterTest
    fun tearDown() {
        assertSdkClassLoader()
    }

    @Test
    fun testClassLoader() {
        assertSdkClassLoader()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUpClass() {
            assertSdkClassLoader()
        }

        @AfterAll
        @JvmStatic
        fun tearDownClass() {
            assertSdkClassLoader()
        }

        private fun assertSdkClassLoader() {
            val classLoader = Thread.currentThread().contextClassLoader
            assertEquals<Class<*>>(SdkSandboxClassLoader::class.java, classLoader.javaClass)
        }
    }
}

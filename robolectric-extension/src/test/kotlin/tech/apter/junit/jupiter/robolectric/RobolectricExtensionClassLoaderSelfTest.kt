package tech.apter.junit.jupiter.robolectric

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader
import tech.apter.junit.jupiter.robolectric.dummies.DummyClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionClassLoaderSelfTest {

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        assertEquals<Class<*>>(SdkSandboxClassLoader::class.java, currentClassLoader.javaClass)
    }

    @AfterTest
    fun tearDown() {
        assertEquals<Class<*>>(SdkSandboxClassLoader::class.java, currentClassLoader.javaClass)
    }

    @Test
    fun `When test class extended with Robolectric then using SdkSandboxClassLoader`() {
        assertEquals<Class<*>>(SdkSandboxClassLoader::class.java, currentClassLoader.javaClass)
    }

    @Test
    fun `When test class extended with Robolectric then classes should be loaded by SdkSandboxClassLoader`() {
        assertIs<SdkSandboxClassLoader>(DummyClass::class.java.classLoader)
        assertNotNull(DummyClass::class.java.`package`)
        assertTrue { DummyClass::class.java.name.startsWith(DummyClass::class.java.`package`.name) }
    }

    companion object {

        private lateinit var currentClassLoader: ClassLoader

        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun setUpClass() {
            currentClassLoader = Thread.currentThread().contextClassLoader
            assertEquals<Class<*>>(SdkSandboxClassLoader::class.java, currentClassLoader.javaClass)
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun tearDownClass() {
            assertEquals<Class<*>>(SdkSandboxClassLoader::class.java, currentClassLoader.javaClass)
        }
    }
}

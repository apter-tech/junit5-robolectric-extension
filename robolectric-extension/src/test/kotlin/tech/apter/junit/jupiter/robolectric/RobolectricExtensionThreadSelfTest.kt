package tech.apter.junit.jupiter.robolectric

import org.junit.After
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue


@ExtendWith(RobolectricExtension::class)
class RobolectricExtensionThreadSelfTest {
    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        assertTrue { Thread.currentThread() == sThread }
        assertTrue { Thread.currentThread().contextClassLoader == sClassLoader }
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        assertTrue { Thread.currentThread() == sThread }
        assertTrue { Thread.currentThread().contextClassLoader == sClassLoader }
    }

    @Test
    fun firstTest() {
        assertTrue { Thread.currentThread() == sThread }
        assertTrue { Thread.currentThread().contextClassLoader == sClassLoader }
    }

    @Test
    fun secondTest() {
        assertTrue { Thread.currentThread() == sThread }
        assertTrue { Thread.currentThread().contextClassLoader == sClassLoader }
    }

    companion object {
        private var sThread: Thread? = null
        private var sClassLoader: ClassLoader? = null

        @BeforeAll
        @Throws(Exception::class)
        @JvmStatic
        fun beforeClass() {
            sThread = Thread.currentThread()
            sClassLoader = Thread.currentThread().contextClassLoader
        }

        @AfterAll
        @Throws(Exception::class)
        @JvmStatic
        fun afterClass() {
            assertTrue { Thread.currentThread() == sThread }
            assertTrue { Thread.currentThread().contextClassLoader == sClassLoader }
        }
    }
}

package tech.apter.junit.jupiter.robolectric

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader
import tech.apter.junit.jupiter.robolectric.dummies.SingleTestMethodJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.dummies.TwoTestMethodsJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.internal.JUnit5RobolectricTestRunnerHelper
import tech.apter.junit.jupiter.robolectric.internal.loadRobolectricClassLoader
import tech.apter.junit.jupiter.robolectric.internal.resetClassLoaderToOriginal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class JUnit5RobolectricRunnerHelperTest {

    private var originalClassLoader: ClassLoader? = null

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        originalClassLoader = JUnit5RobolectricTestRunnerHelper.interceptedClassLoader
    }

    @AfterTest
    @Throws(Exception::class)
    fun tearDown() {
        Thread.currentThread().contextClassLoader = originalClassLoader
        originalClassLoader = null
    }

    @Test
    fun `When call loadRobolectricClassLoader then contextClassLoader should be an instance of SdkSandboxClassLoader`() {
        subjectUnderTest(SingleTestMethodJunitJupiterTest::class.java) {
            // When
            sdkEnvironment(null).loadRobolectricClassLoader()

            // Then
            assertEquals<Class<*>>(
                SdkSandboxClassLoader::class.java,
                Thread.currentThread().contextClassLoader.javaClass
            )
        }
    }

    @Test
    fun `Given the robolectricClassLoader loaded when call reset resetClassLoaderToOriginal then contextClassLoader should not be an instance of SdkSandboxClassLoader`() {
        subjectUnderTest(SingleTestMethodJunitJupiterTest::class.java) {
            // Given
            sdkEnvironment(null).loadRobolectricClassLoader()

            // When
            sdkEnvironment(null).resetClassLoaderToOriginal()

            // Then
            val currentClassLoader = Thread.currentThread().contextClassLoader
            assertNotEquals<Class<*>>(
                SdkSandboxClassLoader::class.java,
                currentClassLoader.javaClass
            )
            assertSame(originalClassLoader, currentClassLoader)
        }
    }

    @Test
    fun `Given a configured test environment when call reset resetClassLoaderToOriginal then contextClassLoader should be the same as original`() {
        subjectUnderTest(SingleTestMethodJunitJupiterTest::class.java) {
            // When
            sdkEnvironment(null).resetClassLoaderToOriginal()

            // Then
            assertSame(originalClassLoader, Thread.currentThread().contextClassLoader)
        }
    }

    @Test
    fun `When call reset resetClassLoaderToOriginal then contextClassLoader should be the same as original`() {
        subjectUnderTest(SingleTestMethodJunitJupiterTest::class.java) {
            // When
            sdkEnvironment(null).resetClassLoaderToOriginal()

            // Then
            assertSame(originalClassLoader, Thread.currentThread().contextClassLoader)
        }
    }

    @Test
    fun `Given a test class with multiple tests when init helper then runner should be created`() {
        // Given
        val cache = subjectUnderTest(TwoTestMethodsJunitJupiterTest::class.java) {}

        // Then
        assertDoesNotThrow { cache.robolectricTestRunner }
        assertEquals(
            TwoTestMethodsJunitJupiterTest::class.java,
            cache.robolectricTestRunner.testClass.javaClass
        )
    }

    @Test
    fun `Given the runnerHelper configured when call clear then the runnerHelper should be empty`() {
        // Given
        val testClass = TwoTestMethodsJunitJupiterTest::class.java
        val runnerHelper = subjectUnderTest(testClass) {
            // When
            clearCachedRobolectricTestRunnerEnvironment()
        }

        // Then
        assertThrows<IllegalArgumentException> { runnerHelper.robolectricTestRunner }
    }

    private fun subjectUnderTest(
        testClass: Class<*>,
        action: JUnit5RobolectricTestRunnerHelper.() -> Unit,
    ): JUnit5RobolectricTestRunnerHelper =
        JUnit5RobolectricTestRunnerHelper(testClass).apply(action)
}

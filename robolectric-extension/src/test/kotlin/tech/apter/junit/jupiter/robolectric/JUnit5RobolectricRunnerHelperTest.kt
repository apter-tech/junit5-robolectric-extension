package tech.apter.junit.jupiter.robolectric

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader
import org.robolectric.internal.bytecode.Sandbox
import tech.apter.junit.jupiter.robolectric.dummies.SingleTestMethodJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.dummies.TwoTestMethodsJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.internal.JUnit5RobolectricTestRunnerHelper
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
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
        subjectUnderTest {
            // Given
            createTestEnvironmentForClass(SingleTestMethodJunitJupiterTest::class.java)

            // When
            loadRobolectricClassLoader()

            // Then
            assertEquals<Class<*>>(
                SdkSandboxClassLoader::class.java,
                Thread.currentThread().contextClassLoader.javaClass
            )
        }
    }

    @Test
    fun `Given the robolectricClassLoader loaded when call reset resetClassLoaderToOriginal then contextClassLoader should not be an instance of SdkSandboxClassLoader`() {
        subjectUnderTest {
            // Given
            createTestEnvironmentForClass(SingleTestMethodJunitJupiterTest::class.java)
            loadRobolectricClassLoader()

            // When
            resetClassLoaderToOriginal()

            // Then
            val currentClassLoader = Thread.currentThread().contextClassLoader
            assertNotEquals<Class<*>>(SdkSandboxClassLoader::class.java, currentClassLoader.javaClass)
            assertSame(originalClassLoader, currentClassLoader)
        }
    }

    @Test
    fun `Given a configured test environment when call reset resetClassLoaderToOriginal then contextClassLoader should be the same as original`() {
        subjectUnderTest {
            // Given
            createTestEnvironmentForClass(SingleTestMethodJunitJupiterTest::class.java)

            // When
            resetClassLoaderToOriginal()

            // Then
            assertSame(originalClassLoader, Thread.currentThread().contextClassLoader)
        }
    }

    @Test
    fun `When call reset resetClassLoaderToOriginal then contextClassLoader should be the same as original`() {
        subjectUnderTest {
            // When
            resetClassLoaderToOriginal()

            // Then
            assertSame(originalClassLoader, Thread.currentThread().contextClassLoader)
        }
    }

    @Test
    fun `Initially the test runner helper should be empty`() {
        subjectUnderTest {
            // Then
            assertThrows<IllegalArgumentException> { robolectricTestRunner }
            assertThrows<IllegalArgumentException> { sdkEnvironment }
            assertThrows<IllegalArgumentException> { frameworkMethod }
        }
    }

    @Test
    fun `Given a testRunner when call configure with runner the sdk environment should be set`() {
        // Given
        val cache = subjectUnderTest {
            // When
            createTestEnvironmentForClass(TwoTestMethodsJunitJupiterTest::class.java)
        }

        // Then
        assertDoesNotThrow { cache.robolectricTestRunner }
        assertEquals(TwoTestMethodsJunitJupiterTest::class.java, cache.robolectricTestRunner.testClass.javaClass)
        assertDoesNotThrow { cache.sdkEnvironment }
        assertThrows<IllegalArgumentException> { cache.frameworkMethod }
    }

    @Test
    fun `Given the sdk environment configured when call configure with framework method then sdk environment should be reconfigured`() {
        // Given
        val testClass = TwoTestMethodsJunitJupiterTest::class.java
        val testMethod2 = testClass.declaredMethods.first {
            it.name == TwoTestMethodsJunitJupiterTest::testMethod2.name
        }
        lateinit var firstSdkEnvironment: Sandbox

        val runnerHelper = subjectUnderTest {
            // And
            createTestEnvironmentForClass(testClass)
            firstSdkEnvironment = sdkEnvironment

            // When
            createTestEnvironmentForMethod(testMethod2)
        }

        // Then
        assertDoesNotThrow { runnerHelper.robolectricTestRunner }
        assertEquals(testClass, runnerHelper.robolectricTestRunner.testClass.javaClass)
        assertDoesNotThrow { runnerHelper.sdkEnvironment }
        assertNotSame(firstSdkEnvironment, runnerHelper.sdkEnvironment)
        assertDoesNotThrow { (runnerHelper.frameworkMethod) }
        assertEquals(TwoTestMethodsJunitJupiterTest::testMethod2.name, runnerHelper.frameworkMethod.name)
    }

    @Test
    fun `Given the runnerHelper configured when call clear then the runnerHelper should be empty`() {
        // Given
        val testClass = TwoTestMethodsJunitJupiterTest::class.java
        val testMethod2 = testClass.declaredMethods.first {
            it.name == TwoTestMethodsJunitJupiterTest::testMethod2.name
        }
        val runnerHelper = subjectUnderTest {
            // And
            createTestEnvironmentForClass(testClass)

            // And
            createTestEnvironmentForMethod(testMethod2)

            // When
            clearCachedRobolectricTestRunnerEnvironment()
        }

        // Then
        assertThrows<IllegalArgumentException> { runnerHelper.robolectricTestRunner }
        assertThrows<IllegalArgumentException> { runnerHelper.sdkEnvironment }
        assertThrows<IllegalArgumentException> { runnerHelper.frameworkMethod }
    }

    private fun subjectUnderTest(
        action: JUnit5RobolectricTestRunnerHelper.() -> Unit
    ): JUnit5RobolectricTestRunnerHelper =
        JUnit5RobolectricTestRunnerHelper().apply(action)
}

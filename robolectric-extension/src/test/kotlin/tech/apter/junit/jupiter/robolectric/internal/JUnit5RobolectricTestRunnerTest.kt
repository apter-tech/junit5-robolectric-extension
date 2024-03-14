package tech.apter.junit.jupiter.robolectric.internal

import org.junit.jupiter.api.Assertions.assertLinesMatch
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import tech.apter.junit.jupiter.robolectric.fakes.SingleDisabledTestMethodJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.fakes.SingleParameterizedTestMethodJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.fakes.SingleTestMethodJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.tools.TestUtil
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.Result as JUnitResult

class JUnit5RobolectricTestRunnerTest {

    private lateinit var testRunListener: JUnit5RobolectricTestRunListener
    private lateinit var notifier: RunNotifier
    private lateinit var events: MutableList<String>
    private var priorEnabledSdks: String? = null
    private var priorAlwaysInclude: String? = null

    @BeforeTest
    fun setUp() {
        testRunListener = JUnit5RobolectricTestRunListener()
        notifier = RunNotifier()
        notifier.addListener(testRunListener)
        events = mutableListOf()
        priorEnabledSdks = System.getProperty("robolectric.enabledSdks")
        System.clearProperty("robolectric.enabledSdks")
        priorAlwaysInclude = System.getProperty("robolectric.alwaysIncludeVariantMarkersInTestName")
        System.clearProperty("robolectric.alwaysIncludeVariantMarkersInTestName")
    }

    @AfterTest
    fun tearDown() {
        notifier.removeListener(testRunListener)
        TestUtil.resetSystemProperty("robolectric.enabledSdks", priorEnabledSdks)
        TestUtil.resetSystemProperty("robolectric.alwaysIncludeVariantMarkersInTestName", priorAlwaysInclude)
    }

    @Test
    fun `Given a JUnit Jupiter annotated test class when run then test should be finished successfully`() {
        subjectUnderTest(testClass = SingleTestMethodJunitJupiterTest::class.java) {
            // When
            run(notifier)

            // Then
            assertLinesMatch(
                listOf(
                    "started: ${SingleTestMethodJunitJupiterTest::testMethod.name}",
                    "finished: ${SingleTestMethodJunitJupiterTest::testMethod.name}",
                ),
                events,
            )
        }
    }

    @Test
    fun `Given a JUnit Jupiter annotated test class when run then test should be finished successfully2`() {
        subjectUnderTest(testClass = SingleTestMethodJunitJupiterTest::class.java) {
            // When
            run(notifier)

            // Then
            assertLinesMatch(
                listOf(
                    "started: ${SingleTestMethodJunitJupiterTest::testMethod.name}",
                    "finished: ${SingleTestMethodJunitJupiterTest::testMethod.name}",
                ),
                events,
            )
        }
    }

    @Test
    fun `Given a JUnit Jupiter annotated disabled test class when run then test should be skipped`() {
        subjectUnderTest(testClass = SingleDisabledTestMethodJunitJupiterTest::class.java) {
            // When
            run(notifier)

            // Then
            assertLinesMatch(
                listOf(
                    "ignored: ${SingleDisabledTestMethodJunitJupiterTest::disabledMethod.name}",
                ),
                events,
            )
        }
    }

    @Test
    fun `Given a JUnit Jupiter annotated parameterized test class when run then test should be tried to run`() {
        subjectUnderTest(testClass = SingleParameterizedTestMethodJunitJupiterTest::class.java) {
            // When
            run(notifier)

            // Then
            assertLinesMatch(
                listOf(
                    "started: ${SingleParameterizedTestMethodJunitJupiterTest::parameterizedTestMethod.name}",
                    "failure: wrong number of arguments",
                    "finished: ${SingleParameterizedTestMethodJunitJupiterTest::parameterizedTestMethod.name}",
                ),
                events,
            )
        }
    }

    private fun subjectUnderTest(
        testClass: Class<*>,
        action: JUnit5RobolectricTestRunner.() -> Unit,
    ) = JUnit5RobolectricTestRunner(testClass).apply(action)

    private inner class JUnit5RobolectricTestRunListener : RunListener() {
        override fun testRunStarted(description: Description) {
            events.add("run started: ${description.methodName}")
        }

        override fun testRunFinished(result: JUnitResult) {
            events.add("run finished: $result")
        }

        override fun testStarted(description: Description) {
            events.add("started: ${description.methodName}")
        }

        override fun testFinished(description: Description) {
            events.add("finished: ${description.methodName}")
        }

        override fun testAssumptionFailure(failure: Failure) {
            events.add("ignored: ${failure.description.methodName}: ${failure.message}")
        }

        override fun testIgnored(description: Description) {
            events.add("ignored: ${description.methodName}")
        }

        override fun testFailure(failure: Failure) {
            val exception: Throwable = failure.exception
            val message = StringBuilder(exception.message ?: "")
            for (suppressed in exception.suppressed) {
                message.append("\nSuppressed: ${suppressed.message}")
            }
            events.add("failure: $message")
        }
    }
}

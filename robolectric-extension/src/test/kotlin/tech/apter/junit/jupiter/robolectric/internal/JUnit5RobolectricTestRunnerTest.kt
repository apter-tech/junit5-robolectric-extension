package tech.apter.junit.jupiter.robolectric.internal

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import org.robolectric.annotation.Config
import java.util.stream.Stream
import org.junit.runner.Result as JUnitResult

class JUnit5RobolectricTestRunnerTest {

    private lateinit var notifier: RunNotifier
    private lateinit var events: MutableList<String>

    @BeforeEach
    fun setUp() {
        notifier = RunNotifier()
        events = mutableListOf()
        notifier.addListener(JUnit5RobolectricTestRunListener())
    }

    @Test
    fun `Given a JUnit Jupiter annotated test class when run then test should be finished successfully`() {
        subjectUnderTest(testClass = JunitJupiterTest::class.java) {
            // When
            run(notifier)

            // Then
            Assertions.assertLinesMatch(
                events,
                listOf(
                    "started: ${JunitJupiterTest::testMethod.name}",
                    "finished: ${JunitJupiterTest::testMethod.name}",
                ),
            )
        }
    }

    @Test
    fun `Given a JUnit Jupiter annotated disabled test class when run then test should be skipped`() {
        subjectUnderTest(testClass = JunitJupiterDisabledTest::class.java) {
            // When
            run(notifier)

            // Then
            Assertions.assertLinesMatch(
                events,
                listOf(
                    "ignored: ${JunitJupiterDisabledTest::disabledMethod.name}",
                ),
            )
        }
    }

    @Test
    fun `Given a JUnit Jupiter annotated parameterized test class when run then test should be tried to run`() {
        subjectUnderTest(testClass = JunitJupiterParameterizedTest::class.java) {
            // When
            run(notifier)

            // Then
            Assertions.assertLinesMatch(
                events,
                listOf(
                    "started: ${JunitJupiterParameterizedTest::parameterizedTestMethod.name}",
                    "failure: wrong number of arguments",
                    "finished: ${JunitJupiterParameterizedTest::parameterizedTestMethod.name}",
                ),
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

@Config(manifest = Config.NONE)
class JunitJupiterTest {
    @Test
    fun testMethod() = Unit
}

@Config(manifest = Config.NONE)
class JunitJupiterDisabledTest {
    @Disabled
    @Test
    fun disabledMethod() = Unit
}

@Config(manifest = Config.NONE)
class JunitJupiterParameterizedTest {
    @ParameterizedTest
    @MethodSource("testParameters")
    fun parameterizedTestMethod(@Suppress("unused") number: Int) = Unit

    companion object {
        @JvmStatic
        fun testParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(1), Arguments.of(2)
        )
    }
}

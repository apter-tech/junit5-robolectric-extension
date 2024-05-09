package tech.apter.junit.jupiter.robolectric.internal.validation

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.apter.junit.jupiter.robolectric.internal.extensions.isJUnit5NestedTest

internal object TestClassValidator {

    fun validate(testClass: Class<*>) {
        validateParallelModeDefault()
        validateTestWithNestedTestsCanNotBeExecutedConcurrently(testClass)
        validateNestedTestCanNotBeExecutedConcurrently(testClass)
        validateNestedTestClassCanNotOverrideRuntimeSdk(testClass)
        validateNestedTestClassCanNotApplyAnnotations(
            testClass,
            GraphicsMode::class.java,
        )
    }

    private fun validateNestedTestClassCanNotOverrideRuntimeSdk(testClass: Class<*>) {
        val config = testClass.getAnnotation(Config::class.java)
        if (testClass.isJUnit5NestedTest && config != null && config.sdk.isNotEmpty()) {
            error("Robolectric runtime sdk cannot be used on nested test class: ${testClass.name}")
        }
    }

    private fun validateNestedTestClassCanNotApplyAnnotations(
        testClass: Class<*>,
        vararg annotationsClasses: Class<out Annotation>,
    ) {
        if (testClass.isJUnit5NestedTest) {
            annotationsClasses.forEach { annotationClass ->
                val annotation = testClass.getAnnotation(annotationClass)
                if (annotation != null) {
                    error(
                        "${annotationClass.simpleName} annotation cannot be used on a nested test class: " +
                            testClass.name
                    )
                }
            }
        }
    }

    private fun validateParallelModeDefault() {
        val parallelModeDefault = System.getProperty("junit.jupiter.execution.parallel.mode.default")
        if (parallelModeDefault == "concurrent") {
            error("junit.jupiter.execution.parallel.mode.default=concurrent is not supported with Robolectric")
        }
    }

    private fun validateTestWithNestedTestsCanNotBeExecutedConcurrently(
        testClass: Class<*>,
    ) {
        fun Class<*>.isDeclaredJUnit5NestedTestClasses() = declaredClasses.any { it.isJUnit5NestedTest }
        fun Class<*>.isConcurrentExecutionEnabled() =
            (
                System.getProperty("junit.jupiter.execution.parallel.mode.classes.default") == "concurrent" &&
                    executionMode() != ExecutionMode.SAME_THREAD
                ) ||
                executionMode() == ExecutionMode.CONCURRENT

        if (testClass.declaringClass == null) {
            if (testClass.isDeclaredJUnit5NestedTestClasses() && testClass.isConcurrentExecutionEnabled()) {
                error(
                    "${testClass.simpleName} must be annotated with @Execution(ExecutionMode.SAME_THREAD). " +
                        "Because it declared nested test classes. Or" +
                        " system property junit.jupiter.execution.parallel.mode.classes.default=same_thread must be set"
                )
            }
        }
    }

    private fun validateNestedTestCanNotBeExecutedConcurrently(testClass: Class<*>) {
        if (testClass.isJUnit5NestedTest && testClass.executionMode() == ExecutionMode.CONCURRENT) {
            error("Concurrent execution mode not allowed on test class: ${testClass.simpleName}")
        }
    }

    private fun Class<*>.executionMode() = getAnnotation(Execution::class.java)?.value
}

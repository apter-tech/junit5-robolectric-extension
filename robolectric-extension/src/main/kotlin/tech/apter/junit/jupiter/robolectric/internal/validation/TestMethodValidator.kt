package tech.apter.junit.jupiter.robolectric.internal.validation

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.annotation.Config
import java.lang.reflect.Method

internal object TestMethodValidator {

    fun validate(testMethod: Method) {
        validateTestMethodCanNotBeExecutedConcurrently(testMethod)
        validateTestMethodsCanNotOverrideRuntimeSdk(testMethod)
    }

    private fun validateTestMethodsCanNotOverrideRuntimeSdk(testMethod: Method) {
        val config = testMethod.getAnnotation(Config::class.java)
        if (config != null && config.sdk.isNotEmpty()) {
            error(
                "Robolectric runtime sdk cannot be overwritten on a test method: " +
                    "${testMethod.declaringClass.simpleName}::${testMethod.name}"
            )
        }
    }

    private fun validateTestMethodCanNotBeExecutedConcurrently(testMethod: Method) {
        if (testMethod.getAnnotation(Execution::class.java)?.value == ExecutionMode.CONCURRENT) {
            error("${testMethod.name} cannot be annotated @Execution(ExecutionMode.CONCURRENT)")
        }
    }
}

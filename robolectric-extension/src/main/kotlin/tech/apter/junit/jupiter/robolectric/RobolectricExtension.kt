package tech.apter.junit.jupiter.robolectric

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.DynamicTestInvocationContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import tech.apter.junit.jupiter.robolectric.internal.JUnit5RobolectricTestRunnerHelper
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import tech.apter.junit.jupiter.robolectric.internal.validation.TestClassValidator
import tech.apter.junit.jupiter.robolectric.internal.validation.TestMethodValidator
import java.lang.reflect.Method
import kotlin.jvm.optionals.getOrNull

@Suppress("TooManyFunctions", "LibraryEntitiesShouldNotBePublic")
class RobolectricExtension :
    InvocationInterceptor,
    BeforeAllCallback,
    BeforeEachCallback,
    AfterEachCallback,
    AfterAllCallback {
    private inline val logger get() = createLogger()

    init {
        logger.trace { "init" }
    }

    override fun beforeAll(context: ExtensionContext) {
        logger.trace { "beforeAll ${context.requiredTestClass.simpleName}" }
        TestClassValidator(context.requiredTestClass).validate()
    }

    override fun beforeEach(context: ExtensionContext) {
        logger.trace {
            "beforeEach ${context.requiredTestClass.simpleName}::${context.requiredTestMethod.name}"
        }
        TestMethodValidator(context.requiredTestMethod).validate()
        val testRunnerHelper = testRunnerHelper(context.requiredTestClass)
        testRunnerHelper.beforeEach(context.requiredTestMethod)
    }

    override fun afterEach(context: ExtensionContext) {
        logger.trace {
            "afterEach ${context.requiredTestClass.simpleName}::${context.requiredTestMethod.name}"
        }
        val testRunnerHelper = testRunnerHelper(context.requiredTestClass)
        testRunnerHelper.afterEach(context.requiredTestMethod)
    }

    override fun afterAll(context: ExtensionContext) {
        logger.trace { "afterAll ${context.requiredTestClass.simpleName}" }
        testRunnerHelper(context.requiredTestClass).clearCachedRobolectricTestRunnerEnvironment()
    }

    override fun interceptBeforeAllMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptBeforeAllMethod ${extensionContext.requiredTestClass.simpleName}" }
        testRunnerHelper(extensionContext.requiredTestClass).proceedInvocation(
            null,
            invocation,
        )
    }

    override fun interceptBeforeEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace {
            "interceptBeforeEachMethod ${extensionContext.requiredTestClass.simpleName}" +
                "::${extensionContext.requiredTestMethod.name}"
        }
        testRunnerHelper(extensionContext.requiredTestClass).proceedInvocation(
            extensionContext.requiredTestMethod,
            invocation,
        )
    }

    override fun interceptDynamicTest(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: DynamicTestInvocationContext,
        extensionContext: ExtensionContext,
    ) {
        val parent = checkNotNull(extensionContext.parent.getOrNull()) { "ExtensionContext's parent must be not null" }
        logger.trace { "interceptDynamicTest ${parent.requiredTestClass} ${extensionContext.displayName}" }
        testRunnerHelper(parent.requiredTestClass).proceedInvocation(parent.requiredTestMethod, invocation)
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace {
            "interceptTestMethod ${extensionContext.requiredTestClass.simpleName}" +
                "::${extensionContext.requiredTestMethod.name}"
        }
        testRunnerHelper(extensionContext.requiredTestClass).proceedInvocation(
            extensionContext.requiredTestMethod,
            invocation,
        )
    }

    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace {
            "interceptTestTemplateMethod ${extensionContext.requiredTestClass.simpleName}" +
                "::${extensionContext.requiredTestMethod.name}"
        }
        testRunnerHelper(extensionContext.requiredTestClass).proceedInvocation(
            extensionContext.requiredTestMethod,
            invocation,
        )
    }

    override fun interceptAfterEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace {
            "interceptAfterEachMethod ${extensionContext.requiredTestClass.simpleName}" +
                "::${extensionContext.requiredTestMethod.name}"
        }
        testRunnerHelper(extensionContext.requiredTestClass).proceedInvocation(
            extensionContext.requiredTestMethod,
            invocation,
        )
    }

    override fun interceptAfterAllMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptAfterAllMethod ${extensionContext.requiredTestClass.simpleName}" }
        testRunnerHelper(extensionContext.requiredTestClass).proceedInvocation(
            null,
            invocation,
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun testRunnerHelper(testClass: Class<*>) = JUnit5RobolectricTestRunnerHelper.getInstance(testClass)
}

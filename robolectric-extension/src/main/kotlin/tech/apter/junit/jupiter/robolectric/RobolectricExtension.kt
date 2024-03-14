package tech.apter.junit.jupiter.robolectric

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.DynamicTestInvocationContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.commons.util.ReflectionUtils
import tech.apter.junit.jupiter.robolectric.internal.JUnit5RobolectricTestRunnerHelper
import tech.apter.junit.jupiter.robolectric.internal.createLogger
import tech.apter.junit.jupiter.robolectric.internal.runOnMainThread
import tech.apter.junit.jupiter.robolectric.internal.runOnMainThreadWithRobolectric
import tech.apter.junit.jupiter.robolectric.internal.runWithRobolectric

class RobolectricExtension :
    InvocationInterceptor,
    BeforeAllCallback,
    BeforeEachCallback,
    AfterEachCallback,
    AfterAllCallback {
    private inline val logger get() = createLogger()
    private val beforeAllFired = AtomicBoolean(false)
    private val robolectricTestRunnerHelper by lazy { JUnit5RobolectricTestRunnerHelper() }

    override fun beforeAll(context: ExtensionContext) {
        logger.trace { "beforeAll ${context.requiredTestClass.name}" }
        robolectricTestRunnerHelper.createTestEnvironmentForClass(context.requiredTestClass)
    }

    override fun beforeEach(context: ExtensionContext) {
        logger.trace { "beforeEach ${context.requiredTestClass.name}::${context.requiredTestMethod.name}" }
        robolectricTestRunnerHelper.createTestEnvironmentForMethod(context.requiredTestMethod)
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            if (!beforeAllFired.getAndSet(true)) {
                invokeBeforeAllMethods(testClass = context.requiredTestClass)
            }
            beforeEach(context.requiredTestMethod)
        }
    }

    private fun invokeBeforeAllMethods(testClass: Class<*>) {
        val beforeAllMethods = testClass
            .methods
            .filter {
                it.getAnnotation(BeforeAll::class.java) != null &&
                        Modifier.isStatic(it.modifiers)
            }

        beforeAllMethods.forEach {
            logger.trace { "invoke beforeAll ${it.name}" }

            ReflectionUtils.invokeMethod(it, null)
        }
    }

    override fun afterEach(context: ExtensionContext) {
        logger.trace { "afterEach ${context.requiredTestClass.name}::${context.requiredTestMethod.name}" }
        robolectricTestRunnerHelper.runOnMainThread {
            runWithRobolectric {
                afterEach(context.requiredTestMethod)
            }
            afterEachFinally()
        }
    }

    override fun afterAll(context: ExtensionContext) {
        logger.trace { "afterAll ${context.requiredTestClass.name}" }
        robolectricTestRunnerHelper.clearCachedRobolectricTestRunnerEnvironment()
        beforeAllFired.set(false)
    }

    override fun interceptBeforeAllMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptBeforeAllMethod ${extensionContext.requiredTestClass}" }
        invocation.skip()
    }

    override fun interceptBeforeEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptBeforeEachMethod ${extensionContext.requiredTestClass}::${extensionContext.requiredTestMethod}" }
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            super.interceptBeforeEachMethod(invocation, invocationContext, extensionContext)
        }
    }

    override fun interceptDynamicTest(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: DynamicTestInvocationContext,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptDynamicTest ${extensionContext.requiredTestClass}::${extensionContext.requiredTestMethod}" }
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            super.interceptDynamicTest(invocation, invocationContext, extensionContext)
        }
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptTestMethod ${extensionContext.requiredTestClass}::${extensionContext.requiredTestMethod}" }
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            super.interceptTestMethod(invocation, invocationContext, extensionContext)
        }
    }

    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptTestTemplateMethod ${extensionContext.requiredTestClass}::${extensionContext.requiredTestMethod}" }
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            super.interceptTestTemplateMethod(invocation, invocationContext, extensionContext)
        }
    }

    override fun interceptAfterEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptAfterEachMethod ${extensionContext.requiredTestClass}::${extensionContext.requiredTestMethod}" }
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            super.interceptAfterEachMethod(invocation, invocationContext, extensionContext)
        }
    }

    override fun interceptAfterAllMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        logger.trace { "interceptAfterAllMethod ${extensionContext.requiredTestClass}" }
        robolectricTestRunnerHelper.runOnMainThreadWithRobolectric {
            super.interceptAfterAllMethod(invocation, invocationContext, extensionContext)
        }
    }
}

package tech.apter.junit.jupiter.robolectric.internal

import tech.apter.junit.jupiter.robolectric.internal.extensions.isNested
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNestedTest
import tech.apter.junit.jupiter.robolectric.internal.extensions.nearestOuterNestedTestOrOuterMostDeclaringClass

internal fun robolectricClassLoaderFactory(testClass: Class<*>): ClassLoader {
    val testClassForRunner = if (testClass.kotlin.isCompanion) {
        testClass.declaringClass
    } else if (testClass.isNestedTest) {
        testClass
    } else if (testClass.isNested) {
        testClass.nearestOuterNestedTestOrOuterMostDeclaringClass()
    } else {
        testClass
    }

    val testRunnerHelper = JUnit5RobolectricTestRunnerHelper.getInstance(testClassForRunner)
    return testRunnerHelper.sdkEnvironment(null).robolectricClassLoader
}

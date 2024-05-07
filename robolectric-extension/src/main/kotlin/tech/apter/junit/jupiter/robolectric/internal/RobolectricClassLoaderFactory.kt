package tech.apter.junit.jupiter.robolectric.internal

import tech.apter.junit.jupiter.robolectric.internal.extensions.isJUnit5NestedTest
import tech.apter.junit.jupiter.robolectric.internal.extensions.nearestOuterNestedTestOrOuterMostDeclaringClass

internal fun robolectricClassLoaderFactory(testClass: Class<*>): ClassLoader {
    val testClassForRunner = if (testClass.kotlin.isCompanion) {
        testClass.declaringClass
    } else if (testClass.isJUnit5NestedTest) {
        testClass
    } else if (testClass.declaringClass != null) {
        testClass.nearestOuterNestedTestOrOuterMostDeclaringClass()
    } else {
        testClass
    }

    val testRunnerHelper = JUnit5RobolectricTestRunnerHelper.getInstance(testClassForRunner)
    return testRunnerHelper.sdkEnvironment(null).robolectricClassLoader
}

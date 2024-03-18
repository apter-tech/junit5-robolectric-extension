package tech.apter.junit.jupiter.robolectric.internal

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import java.lang.reflect.Modifier
import java.util.Collections

private val parameterizedTestAnnotation: Class<out Annotation>? by lazy {
    try {
        @Suppress("UNCHECKED_CAST")
        Class.forName("org.junit.jupiter.params.ParameterizedTest") as? Class<out Annotation>
    } catch (@Suppress("SwallowedException") e: ClassNotFoundException) {
        null
    }
}

internal fun BlockJUnit4ClassRunner.computeJUnit5TestMethods(): MutableList<FrameworkMethod> {
    fun testMethods(testClass: TestClass): MutableList<FrameworkMethod> {
        val testMethods = testClass.getAnnotatedMethods(Test::class.java)
        val testFactoryMethods = testClass.getAnnotatedMethods(TestFactory::class.java)
        val repeatedTestMethods = testClass.getAnnotatedMethods(RepeatedTest::class.java)
        val testTemplateMethods = testClass.getAnnotatedMethods(TestTemplate::class.java)
        val parameterizedTestMethods = if (parameterizedTestAnnotation == null) {
            emptyList()
        } else {
            testClass.getAnnotatedMethods(parameterizedTestAnnotation)
        }

        val nestedTestMethods = testClass
            .javaClass
            .declaredClasses
            .filter { !Modifier.isStatic(it.modifiers) }
            .map { testMethods(TestClass(it)) }
            .flatten()

        val methods = mutableListOf<FrameworkMethod>()
        methods.addAll(testMethods)
        methods.addAll(testTemplateMethods)
        methods.addAll(testFactoryMethods)
        methods.addAll(repeatedTestMethods)
        methods.addAll(parameterizedTestMethods)
        methods.addAll(nestedTestMethods)
        return Collections.unmodifiableList(methods)
    }

    return testMethods(testClass)
}

@Suppress("UnusedReceiverParameter")
internal fun BlockJUnit4ClassRunner.isJUnit5Ignored(child: FrameworkMethod) =
    child.getAnnotation(Disabled::class.java) != null

/**
 * Adds to errors if any method in this class is annotated with annotation, but:
 *
 * * is not public, or
 * * takes parameters, or
 * * returns something other than void, or
 * * is static (given isStatic is false), or
 * * is not static (given isStatic is true).
 */
internal fun BlockJUnit4ClassRunner.validatePublicVoidNoArgJUnit5Methods(
    annotation: Class<out Annotation>,
    isStatic: Boolean,
    errors: MutableList<Throwable>,
) {
    if (annotation == org.junit.Test::class.java) {
        validatePublicVoidNoArgMethods(Test::class.java, isStatic, errors)
        parameterizedTestAnnotation?.let { validatePublicVoidArgMethods(it, isStatic, errors) }
    } else {
        val jUnit5Annotation = when (annotation) {
            Before::class.java -> BeforeEach::class.java
            After::class.java -> After::class.java
            BeforeClass::class.java -> BeforeAll::class.java
            AfterClass::class.java -> AfterAll::class.java
            else -> annotation
        }
        validatePublicVoidNoArgMethods(jUnit5Annotation, isStatic, errors)
    }
}

/**
 * Adds to `errors` if any method in this class is annotated with
 * `annotation`, but:
 *
 *  * is not public, or
 *  * takes parameters, or
 *  * returns something other than void, or
 *  * is static (given `isStatic is false`), or
 *  * is not static (given `isStatic is true`).
 */
internal fun BlockJUnit4ClassRunner.validatePublicVoidNoArgMethods(
    annotation: Class<out Annotation>,
    isStatic: Boolean,
    errors: List<Throwable>,
) {
    val methods: List<FrameworkMethod> = testClass.getAnnotatedMethods(annotation)
    for (eachTestMethod in methods) {
        eachTestMethod.validatePublicVoidNoArg(isStatic, errors)
    }
}

/**
 * Adds to `errors` if any method in this class is annotated with
 * `annotation`, but:
 *
 *  * is not public, or
 *  * returns something other than void, or
 *  * is static (given `isStatic is false`), or
 *  * is not static (given `isStatic is true`).
 */
internal fun BlockJUnit4ClassRunner.validatePublicVoidArgMethods(
    @Suppress("SameParameterValue") annotation: Class<out Annotation>,
    isStatic: Boolean,
    errors: List<Throwable>,
) {
    val methods = testClass.getAnnotatedMethods(annotation)
    for (eachTestMethod in methods) {
        eachTestMethod.validatePublicVoid(isStatic, errors)
    }
}

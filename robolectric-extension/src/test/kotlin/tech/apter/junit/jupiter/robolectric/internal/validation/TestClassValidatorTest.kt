package tech.apter.junit.jupiter.robolectric.internal.validation

import tech.apter.junit.jupiter.robolectric.dummies.AnnotatedNestedJunitJupiterTest
import tech.apter.junit.jupiter.robolectric.dummies.AnnotatedTopLevelJunitJupiterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class TestClassValidatorTest {
    @Test
    fun `when nested class has annotation then validator should raise an error`() {
        val failure = assertFails {
            TestClassValidator.validate(AnnotatedNestedJunitJupiterTest.InnerTests::class.java)
        }
        assertContains(failure.localizedMessage, "GraphicsMode annotation cannot be used on a nested test class")
    }

    @Test
    fun `when top-level class has annotation then validator should not raise an error`() {
        TestClassValidator.validate(AnnotatedTopLevelJunitJupiterTest::class.java)
    }
}

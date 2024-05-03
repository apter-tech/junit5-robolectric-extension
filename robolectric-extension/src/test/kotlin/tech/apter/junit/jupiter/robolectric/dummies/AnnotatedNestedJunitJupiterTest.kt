package tech.apter.junit.jupiter.robolectric.dummies

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.annotation.GraphicsMode
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@Disabled
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(RobolectricExtension::class)
class AnnotatedNestedJunitJupiterTest {
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Nested
    inner class InnerTests {
        @Test
        fun testMethod() = Unit
    }
}

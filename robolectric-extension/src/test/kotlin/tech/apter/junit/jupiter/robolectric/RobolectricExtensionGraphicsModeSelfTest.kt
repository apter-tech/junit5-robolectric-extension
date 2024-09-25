package tech.apter.junit.jupiter.robolectric

import android.graphics.Matrix
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLegacyMatrix
import org.robolectric.shadows.ShadowMatrix
import org.robolectric.shadows.ShadowNativeMatrix
import kotlin.test.Test
import kotlin.test.assertIs

@ExtendWith(RobolectricExtension::class)
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RobolectricExtensionGraphicsModeSelfTest {

    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    @Test
    @Order(1)
    fun test1Legacy() {
        val matrix = Matrix()
        val shadowMatrix: ShadowMatrix = Shadow.extract(matrix)
        assertIs<ShadowLegacyMatrix>(shadowMatrix)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    @Order(2)
    fun test2NativeAfterLegacy() {
        val shadowMatrix: ShadowMatrix = Shadow.extract(Matrix())
        assertIs<ShadowNativeMatrix>(shadowMatrix)
    }

    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    @Test
    @Order(3)
    fun test3LegacyAfterNative() {
        val shadowMatrix: ShadowMatrix = Shadow.extract(Matrix())
        assertIs<ShadowLegacyMatrix>(shadowMatrix)
    }
}

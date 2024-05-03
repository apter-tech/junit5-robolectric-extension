package tech.apter.junit.jupiter.robolectric.dummies

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.LooperMode
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@Disabled
@LooperMode(LooperMode.Mode.PAUSED)
@ExtendWith(RobolectricExtension::class)
class AnnotatedTopLevelJunitJupiterTest {
    @Test
    fun testMethod() = Unit
}

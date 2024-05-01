package tech.apter.junit.jupiter.robolectric.dummies

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@Disabled
@ExtendWith(RobolectricExtension::class)
class SingleDisabledTestMethodJunitJupiterTest {
    @Disabled
    @Test
    fun disabledMethod() = Unit
}

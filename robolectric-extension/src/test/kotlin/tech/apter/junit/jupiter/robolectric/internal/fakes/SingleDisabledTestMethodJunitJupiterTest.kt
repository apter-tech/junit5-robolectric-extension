package tech.apter.junit.jupiter.robolectric.internal.fakes

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class SingleDisabledTestMethodJunitJupiterTest {
    @Disabled
    @Test
    fun disabledMethod() = Unit
}

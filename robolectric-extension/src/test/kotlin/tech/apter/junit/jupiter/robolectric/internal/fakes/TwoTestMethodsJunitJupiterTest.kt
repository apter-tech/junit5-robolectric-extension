package tech.apter.junit.jupiter.robolectric.internal.fakes

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config

@Disabled
class TwoTestMethodsJunitJupiterTest {
    @Test
    fun testMethod1() = Unit

    @Test
    @Config(minSdk = 33)
    fun testMethod2() = Unit
}

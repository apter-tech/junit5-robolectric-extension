package tech.apter.junit.jupiter.robolectric.integration.tests.java11

import android.os.Build
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class Java11Test {
    @Test
    fun `Given an android project with kotlin-dsl build script when using Robolectric with JUnit5 the android app should be available`() {
        assertNotNull(RuntimeEnvironment.getApplication())
    }
}

package tech.apter.junit.jupiter.robolectric.integration.tests.agp.groovy.dsl

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
class AndroidGradlePluginGroovyDslTest {

    @Test
    fun `Given an android project with kotlin-dsl build script when using Robolectric with JUnit5 the android app should be available`() {
        assertNotNull(RuntimeEnvironment.getApplication())
    }
}

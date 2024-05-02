package tech.apter.robolectric.junit.jupiter.gradle.plugin

import org.gradle.api.provider.Property

@Suppress("LibraryEntitiesShouldNotBePublic")
interface RobolectricJUnitJupiterGradlePluginExtension {
    val doNotAddDependencies: Property<Boolean>
}

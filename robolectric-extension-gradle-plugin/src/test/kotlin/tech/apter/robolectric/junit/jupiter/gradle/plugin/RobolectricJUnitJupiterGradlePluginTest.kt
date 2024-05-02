package tech.apter.robolectric.junit.jupiter.gradle.plugin

import com.android.build.gradle.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.tasks.testing.Test as TestTask

class RobolectricJUnitJupiterGradlePluginTest {
    @Test
    fun `Given robolectric-extension-gradle-plugin applied then junitPlatform and launcher interceptors should be enabled on the test tasks`() {
        with(ProjectBuilder.builder().build()) {
            // Given
            pluginManager.apply {
                apply("java-gradle-plugin")
                apply("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin")
            }

            // Then
            val testTasks = tasks.withType(TestTask::class.java).onEach { testTask ->
                assertIs<JUnitPlatformTestFramework>(testTask.testFramework)
                val systemProperties = assertNotNull(testTask.systemProperties)
                assertContains(
                    systemProperties,
                    "junit.platform.launcher.interceptors.enabled"
                )
                assertEquals("true", systemProperties["junit.platform.launcher.interceptors.enabled"])
            }
            assertTrue { testTasks.isNotEmpty() }
        }
    }

    @Test
    fun `Given robolectric-extension-gradle-plugin applied on android project then isIncludeAndroidResources should be true`() {
        with(ProjectBuilder.builder().build()) {
            // Given
            pluginManager.apply {
                apply("com.android.library")
                apply("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin")
            }

            // Then
            val androidExtension = extensions.getByType(LibraryExtension::class.java)
            assertTrue { androidExtension.testOptions.unitTests.isIncludeAndroidResources }
        }
    }

    @Test
    fun `Given robolectric-extension-gradle-plugin applied when project is evaluated then proper dependencies should be added`() {
        with(ProjectBuilder.builder().build()) {
            // Given
            pluginManager.apply {
                apply("java-gradle-plugin")
                apply("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin")
            }

            // When
            triggerEvaluate()

            // Then
            val runtimeDependencies =
                configurations.named("testRuntimeOnly").get().dependencies.map { "${it.group}:${it.name}" }
            val testImplementationDependencies =
                configurations.named("testImplementation").get().dependencies.map { "${it.group}:${it.name}" }
            assertContains(runtimeDependencies, "org.junit.jupiter:junit-jupiter-engine")
            assertContains(testImplementationDependencies, "tech.apter.junit5.jupiter:robolectric-extension")
            assertContains(testImplementationDependencies, "org.junit:junit-bom")
        }
    }

    @Test
    fun `Given robolectric-extension-gradle-plugin applied when doNotAddDependencies is false and project is evaluated then proper dependencies should be added`() {
        with(ProjectBuilder.builder().build()) {
            // Given
            pluginManager.apply {
                apply("java-gradle-plugin")
                apply("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin")
            }

            // When
            extensions.getByType(RobolectricJUnitJupiterGradlePluginExtension::class.java).doNotAddDependencies.set(
                false
            )
            // And
            triggerEvaluate()

            // Then
            val runtimeDependencies =
                configurations.named("testRuntimeOnly").get().dependencies.map { "${it.group}:${it.name}" }
            val testImplementationDependencies =
                configurations.named("testImplementation").get().dependencies.map { "${it.group}:${it.name}" }
            assertContains(runtimeDependencies, "org.junit.jupiter:junit-jupiter-engine")
            assertContains(testImplementationDependencies, "tech.apter.junit5.jupiter:robolectric-extension")
            assertContains(testImplementationDependencies, "org.junit:junit-bom")
        }
    }

    @Test
    fun `Given robolectric-extension-gradle-plugin applied when doNotAddDependencies is true and project is evaluated then proper dependencies should be not added`() {
        with(ProjectBuilder.builder().build()) {
            // Given
            pluginManager.apply {
                apply("java-gradle-plugin")
                apply("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin")
            }

            // When
            extensions.getByType(
                RobolectricJUnitJupiterGradlePluginExtension::class.java
            ).doNotAddDependencies.set(true)
            // And
            triggerEvaluate()

            // Then
            val runtimeDependencies =
                configurations.named("testRuntimeOnly").get().dependencies.map { "${it.group}:${it.name}" }
            val testImplementationDependencies =
                configurations.named("testImplementation").get().dependencies.map { "${it.group}:${it.name}" }
            assertFalse { runtimeDependencies.contains("org.junit.jupiter:junit-jupiter-engine") }
            assertFalse { testImplementationDependencies.contains("tech.apter.junit5.jupiter:robolectric-extension") }
            assertFalse { testImplementationDependencies.contains("org.junit:junit-bom") }
        }
    }

    private fun Project.triggerEvaluate() {
        try {
            (project as DefaultProject).evaluate()
        } catch (e: GradleException) {
            // Ignore exception as a workaround of https://github.com/gradle/gradle/issues/20301#issuecomment-1473013908
            logger.trace("Exception during evaluate is suppressed", e)
        }
    }
}

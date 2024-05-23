package tech.apter.robolectric.junit.jupiter.gradle.plugin

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.Test

@Suppress("LibraryEntitiesShouldNotBePublic")
class RobolectricJUnitJupiterGradlePlugin : Plugin<Project> {
    private inline val logger get() = Logging.getLogger(javaClass)

    override fun apply(target: Project) {
        logger.trace("${javaClass.name} is applied.")

        with(target) {
            val extension = registerExtension()
            setupTestTasks()
            enableIncludeAndroidResources()
            afterEvaluate {
                if (!extension.doNotAddDependencies.get()) {
                    addDependencies()
                }
            }
        }
    }

    private fun Project.registerExtension(): RobolectricJUnitJupiterGradlePluginExtension = extensions.create(
        "robolectricJUnitJupiter",
        RobolectricJUnitJupiterGradlePluginExtension::class.java
    ).apply {
        doNotAddDependencies.convention(false)
    }

    private fun Project.setupTestTasks() {
        tasks.withType(Test::class.java).configureEach { testTask ->
            testTask.prepareForRobolectric()
        }
        tasks.whenTaskAdded { task ->
            if (task is Test) {
                task.prepareForRobolectric()
            }
        }
    }

    private fun Test.prepareForRobolectric() {
        val jvmArgs = listOf(
            "-Djunit.platform.launcher.interceptors.enabled=true",
        )
        useJUnitPlatform()
        jvmArgs(jvmArgs)
    }

    private fun Project.enableIncludeAndroidResources() {
        val androidExtension = extensions.findByName("android") as? CommonExtension<*, *, *, *>
        @Suppress("UnstableApiUsage")
        androidExtension?.testOptions?.unitTests?.isIncludeAndroidResources = true
    }

    private fun Project.addDependencies() {
        with(dependencies) {
            add(
                "testImplementation",
                "tech.apter.junit5.jupiter:robolectric-extension:${BuildConfig.ROBOLECTRIC_EXTENSION_VERSION}"
            )
            add("testImplementation", platform("org.junit:junit-bom:${BuildConfig.JUNIT5_BOM_VERSION}"))
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
        }
    }
}

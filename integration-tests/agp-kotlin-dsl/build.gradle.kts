plugins {
    id("com.android.library")
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.detekt)
}

android {
    namespace = "tech.apter.junit.jupiter.robolectric.integration.tests.agp.kotlin.dsl"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    buildToolsVersion = libs.versions.androidBuildTools.get()

    defaultConfig {
        minSdk = libs.versions.androidMinimumSdk.get().toInt()
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.useJUnitPlatform()
                test.jvmArgs(listOf("-Djunit.platform.launcher.interceptors.enabled=true"))
            }
        }
    }
}

detekt {
    version = libs.versions.detekt.get()
    autoCorrect = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml").asFile)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    detektPlugins(libs.detektFormatting)
    detektPlugins(libs.detektRulesLibraries)
    testImplementation(project(":robolectric-extension"))
    testImplementation(libs.robolectric)
    testImplementation(libs.junit5JupiterApi)
    testRuntimeOnly(libs.junit5JupiterEngine)
}

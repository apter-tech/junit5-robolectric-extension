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

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    testImplementation(project(":robolectric-extension"))
    testImplementation(libs.robolectric)
    testImplementation(libs.junit5JupiterApi)
    testRuntimeOnly(libs.junit5JupiterEngine)
}

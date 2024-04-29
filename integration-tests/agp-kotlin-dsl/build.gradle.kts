import tech.apter.robolectric.junit.jupiter.gradle.plugin.RobolectricJUnitJupiterGradlePlugin

buildscript {
    dependencies {
        classpath("tech.apter.junit5.jupiter:robolectric-extension-gradle-plugin:$version")
    }
}

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.detekt)
}

apply(mapOf("plugin" to RobolectricJUnitJupiterGradlePlugin::class.java))

android {
    namespace = "tech.apter.junit.jupiter.robolectric.integration.tests.agp.kotlin.dsl"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    buildToolsVersion = libs.versions.androidBuildTools.get()

    defaultConfig {
        minSdk = libs.versions.androidMinimumSdk.get().toInt()
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    autoCorrect = true
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    detektPlugins(libs.detektFormatting)
    detektPlugins(libs.detektRulesLibraries)
}

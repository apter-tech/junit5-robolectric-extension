package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.GetInstallerPackageNameMode
import org.robolectric.plugins.GetInstallerPackageNameModeConfigurer
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNested

internal class JUnit5GetInstallerPackageNameModeConfigurer : GetInstallerPackageNameModeConfigurer() {
    override fun getConfigFor(testClass: Class<*>): GetInstallerPackageNameMode.Mode? {
        return if (testClass.isNested) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): GetInstallerPackageNameMode.Mode? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNested) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.Config
import org.robolectric.pluginapi.config.GlobalConfigProvider
import org.robolectric.plugins.ConfigConfigurer
import org.robolectric.plugins.PackagePropertiesLoader
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNonStaticInnerClass

internal class JUnit5ConfigConfigurer(
    packagePropertiesLoader: PackagePropertiesLoader,
    defaultConfigProvider: GlobalConfigProvider,
) : ConfigConfigurer(packagePropertiesLoader, defaultConfigProvider) {
    override fun getConfigFor(testClass: Class<*>): Config? {
        return if (testClass.isNonStaticInnerClass) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): Config? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNonStaticInnerClass) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

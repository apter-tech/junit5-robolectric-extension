package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.LooperMode
import org.robolectric.plugins.LooperModeConfigurer
import org.robolectric.plugins.PackagePropertiesLoader
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNested
import java.util.Properties

internal class JUnit5LooperModeConfigurer(
    systemProperties: Properties,
    propertyFileLoader: PackagePropertiesLoader,
) : LooperModeConfigurer(systemProperties, propertyFileLoader) {
    override fun getConfigFor(testClass: Class<*>): LooperMode.Mode? {
        return if (testClass.isNested) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): LooperMode.Mode? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNested) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

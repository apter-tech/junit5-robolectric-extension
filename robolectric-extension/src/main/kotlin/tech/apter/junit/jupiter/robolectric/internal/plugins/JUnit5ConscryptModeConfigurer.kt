package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.ConscryptMode
import org.robolectric.plugins.ConscryptModeConfigurer
import org.robolectric.plugins.PackagePropertiesLoader
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNested
import java.util.Properties

internal class JUnit5ConscryptModeConfigurer(
    systemProperties: Properties,
    propertyFileLoader: PackagePropertiesLoader,
) : ConscryptModeConfigurer(systemProperties, propertyFileLoader) {
    override fun getConfigFor(testClass: Class<*>): ConscryptMode.Mode? {
        return if (testClass.isNested) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): ConscryptMode.Mode? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNested) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

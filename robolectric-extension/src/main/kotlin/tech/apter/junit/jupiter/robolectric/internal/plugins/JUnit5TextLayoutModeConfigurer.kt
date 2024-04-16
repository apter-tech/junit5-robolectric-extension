package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.TextLayoutMode
import org.robolectric.plugins.PackagePropertiesLoader
import org.robolectric.plugins.TextLayoutModeConfigurer
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNested
import java.util.Properties

internal class JUnit5TextLayoutModeConfigurer(
    systemProperties: Properties,
    propertyFileLoader: PackagePropertiesLoader,
) : TextLayoutModeConfigurer(systemProperties, propertyFileLoader) {
    override fun getConfigFor(testClass: Class<*>): TextLayoutMode.Mode? {
        return if (testClass.isNested) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): TextLayoutMode.Mode? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNested) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

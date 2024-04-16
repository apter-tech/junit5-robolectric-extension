package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.GraphicsMode
import org.robolectric.plugins.GraphicsModeConfigurer
import org.robolectric.plugins.PackagePropertiesLoader
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNested
import java.util.Properties

internal class JUnit5GraphicsModeConfigurer(
    systemProperties: Properties,
    propertyFileLoader: PackagePropertiesLoader,
) : GraphicsModeConfigurer(systemProperties, propertyFileLoader) {

    override fun getConfigFor(testClass: Class<*>): GraphicsMode.Mode? {
        return if (testClass.isNested) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): GraphicsMode.Mode? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNested) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

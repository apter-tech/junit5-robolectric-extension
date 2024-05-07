package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.experimental.LazyApplication
import org.robolectric.plugins.LazyApplicationConfigurer
import org.robolectric.plugins.PackagePropertiesLoader
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNonStaticInnerClass
import java.util.Properties

internal class JUnit5LazyApplicationConfigurer(
    systemProperties: Properties,
    propertyFileLoader: PackagePropertiesLoader,
) : LazyApplicationConfigurer(systemProperties, propertyFileLoader) {

    override fun getConfigFor(testClass: Class<*>): LazyApplication.LazyLoad? {
        return if (testClass.isNonStaticInnerClass) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): LazyApplication.LazyLoad? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNonStaticInnerClass) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

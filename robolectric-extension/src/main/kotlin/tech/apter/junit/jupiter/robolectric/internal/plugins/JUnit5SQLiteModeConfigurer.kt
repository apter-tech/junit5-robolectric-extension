package tech.apter.junit.jupiter.robolectric.internal.plugins

import org.robolectric.annotation.SQLiteMode
import org.robolectric.plugins.PackagePropertiesLoader
import org.robolectric.plugins.SQLiteModeConfigurer
import tech.apter.junit.jupiter.robolectric.internal.extensions.isNonStaticInnerClass
import java.util.Properties

internal class JUnit5SQLiteModeConfigurer(
    systemProperties: Properties,
    propertyFileLoader: PackagePropertiesLoader,
) : SQLiteModeConfigurer(systemProperties, propertyFileLoader) {
    override fun getConfigFor(testClass: Class<*>): SQLiteMode.Mode? {
        return if (testClass.isNonStaticInnerClass) {
            getConfigMergedWithDeclaringClassConfig(testClass)
        } else {
            super.getConfigFor(testClass)
        }
    }

    private fun getConfigMergedWithDeclaringClassConfig(testClass: Class<*>): SQLiteMode.Mode? {
        val config = super.getConfigFor(testClass)
        return if (testClass.isNonStaticInnerClass) {
            val parentConfig = getConfigMergedWithDeclaringClassConfig(testClass.declaringClass)
            config?.let { c -> return parentConfig?.let { p -> merge(p, c) } ?: c } ?: parentConfig
        } else {
            config
        }
    }
}

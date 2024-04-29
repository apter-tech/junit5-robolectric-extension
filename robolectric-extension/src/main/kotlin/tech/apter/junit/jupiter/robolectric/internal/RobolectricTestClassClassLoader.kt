package tech.apter.junit.jupiter.robolectric.internal

import org.junit.platform.commons.logging.Logger
import tech.apter.junit.jupiter.robolectric.internal.extensions.createLogger
import tech.apter.junit.jupiter.robolectric.internal.extensions.isExtendedWithRobolectric
import java.io.InputStream
import java.net.URL
import java.util.Enumeration

/**
 * This ClassLoader is a proxy to load classes with the correct Robolectric ClassLoader.
 * If a test class is not annotated with @ExtendWith(RobolectricExtension::class) then use the `parentClassLoader`.
 * Only used to load the test classes.
 */
internal class RobolectricTestClassClassLoader(
    private val parentClassLoader: ClassLoader,
    private val robolectricClassLoaderFactory: (testClass: Class<*>) -> ClassLoader,
) : ClassLoader() {
    private inline val logger: Logger get() = createLogger()
    override fun loadClass(name: String): Class<*> {
        val clazz = parentClassLoader.loadClass(name)
        val extendedWithRobolectric = clazz.isExtendedWithRobolectric()
        logger.trace {
            "Load ${clazz.name.substringAfterLast(
                '.'
            )} class with ${if (extendedWithRobolectric) "Robolectric" else "app"} classLoader"
        }
        return if (extendedWithRobolectric) {
            robolectricClassLoaderFactory(clazz).loadClass(name)
        } else {
            clazz
        }
    }

    override fun getResource(name: String): URL? {
        logger.trace { "getResource($name)" }
        return parentClassLoader.getResource(name)
    }

    override fun getResourceAsStream(name: String): InputStream? {
        logger.trace { "getResourceAsStream($name)" }
        return parentClassLoader.getResourceAsStream(name)
    }

    override fun getResources(name: String): Enumeration<URL> {
        logger.trace { "getResources($name)" }
        return parentClassLoader.getResources(name)
    }
}

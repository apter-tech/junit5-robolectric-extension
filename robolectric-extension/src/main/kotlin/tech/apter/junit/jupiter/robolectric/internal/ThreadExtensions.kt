package tech.apter.junit.jupiter.robolectric.internal

internal fun Thread.replaceClassLoader(classLoader: ClassLoader?): ClassLoader? {
    val originalClassLoader = contextClassLoader
    contextClassLoader = classLoader
    return originalClassLoader
}

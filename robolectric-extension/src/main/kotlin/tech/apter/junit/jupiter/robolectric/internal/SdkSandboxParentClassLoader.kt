package tech.apter.junit.jupiter.robolectric.internal

import java.io.File
import java.net.URLClassLoader

internal class SdkSandboxParentClassLoader(parent: ClassLoader) : URLClassLoader(
    "sdk-sandbox-parent",
    run {
        val javaClassPath = System.getProperty("java.class.path")
        val robolectricJars = javaClassPath.split(File.pathSeparatorChar).mapNotNull { urlString ->
            return@mapNotNull if (urlString.contains("org.robolectric")) {
                File(urlString).toURI().toURL()
            } else {
                null
            }
        }
        return@run robolectricJars.toTypedArray()
    },
    parent,
)

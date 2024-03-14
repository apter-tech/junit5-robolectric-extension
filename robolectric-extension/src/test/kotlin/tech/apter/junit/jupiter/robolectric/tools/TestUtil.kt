package tech.apter.junit.jupiter.robolectric.tools

import android.R
import com.google.common.io.CharStreams
import org.robolectric.plugins.SdkCollection
import org.robolectric.res.Fs
import org.robolectric.res.ResourcePath
import org.robolectric.util.inject.Injector
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.Properties

object TestUtil {
    private var SYSTEM_RESOURCE_PATH: ResourcePath? = null
    private var TEST_RESOURCE_PATH: ResourcePath? = null
    private var testDirLocation: File? = null

    val sdkCollection: SdkCollection by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getInjectedInstance(SdkCollection::class.java)
    }

    private val injector: Injector = Injector.Builder()
        .bind(Properties::class.java, System.getProperties()).build()

    fun resourcesBaseDir(): Path {
        return resourcesBaseDirFile().toPath()
    }

    private fun resourcesBaseDirFile(): File {
        if (testDirLocation == null) {
            val baseDir = System.getProperty("robolectric-tests.base-dir")
            return File(baseDir, "src/test/resources").also { testDirLocation = it }
        } else {
            return requireNotNull(testDirLocation)
        }
    }

    fun resourceFile(vararg pathParts: String): Path {
        return Fs.join(resourcesBaseDir(), *pathParts)
    }

    fun testResources(): ResourcePath {
        if (TEST_RESOURCE_PATH == null) {
            TEST_RESOURCE_PATH = ResourcePath(R::class.java, resourceFile("res"), resourceFile("assets"))
        }
        return requireNotNull(TEST_RESOURCE_PATH)
    }

    fun systemResources(): ResourcePath {
        if (SYSTEM_RESOURCE_PATH == null) {
            val sdk = sdkCollection.maxSupportedSdk
            val path: Path = sdk.jarPath
            SYSTEM_RESOURCE_PATH = ResourcePath(
                R::class.java, path.resolve("raw-res/res"), path.resolve("raw-res/assets")
            )
        }
        return requireNotNull(SYSTEM_RESOURCE_PATH)
    }

    fun sdkResources(apiLevel: Int): ResourcePath {
        val path: Path = sdkCollection.getSdk(apiLevel).jarPath
        return ResourcePath(null, path.resolve("raw-res/res"), null, null)
    }

    @Throws(IOException::class)
    fun readString(inputStream: InputStream): String {
        return CharStreams.toString(InputStreamReader(inputStream, "UTF-8"))
    }

    fun resetSystemProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }

    private fun <T> getInjectedInstance(clazz: Class<T>): T {
        return injector.getInstance(clazz)
    }
}

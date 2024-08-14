package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.MavenRoboSettings
import org.robolectric.internal.dependency.DependencyJar
import org.robolectric.internal.dependency.MavenArtifactFetcher
import org.robolectric.internal.dependency.MavenDependencyResolver
import org.robolectric.internal.dependency.MavenJarArtifact
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.util.concurrent.ExecutorService


internal class JUnit5MavenDependencyResolver private constructor(
    repositoryUrl: String,
    repositoryId: String,
    repositoryUserName: String?,
    repositoryPassword: String?,
    proxyHost: String?,
    proxyPort: Int,
) : MavenDependencyResolver(repositoryUrl, repositoryId, repositoryUserName, repositoryPassword, proxyHost, proxyPort) {
    @Suppress("unused")
    constructor() : this(
        MavenRoboSettings.getMavenRepositoryUrl(),
        MavenRoboSettings.getMavenRepositoryId(),
        MavenRoboSettings.getMavenRepositoryUserName(),
        MavenRoboSettings.getMavenRepositoryPassword(),
        MavenRoboSettings.getMavenProxyHost(),
        MavenRoboSettings.getMavenProxyPort(),
    )

    private val executorService: ExecutorService = createExecutorService()
    private val mavenArtifactFetcher: MavenArtifactFetcher = createMavenFetcher(
        repositoryUrl,
        repositoryUserName,
        repositoryPassword,
        proxyHost,
        proxyPort,
        localRepositoryDir,
        executorService,
    )

    override fun getLocalArtifactUrls(vararg dependencies: DependencyJar): Array<URL?> {
        val artifacts: List<Pair<DependencyJar, MavenJarArtifact>> = dependencies.map { it to MavenJarArtifact(it) }

        for ((dependencyJar, artifact) in artifacts) {
            val artifactJarFile = File(localRepositoryDir, artifact.jarPath())
            if (!artifactJarFile.exists()) {
                whileLocked(dependencyJar) {
                    if (!artifactJarFile.exists()) {
                        mavenArtifactFetcher.fetchArtifact(artifact)
                    }
                }
            }
        }
        val urls = arrayOfNulls<URL>(dependencies.size)
        try {
            for (i in artifacts.indices) {
                val artifact = artifacts[i].second
                urls[i] = File(localRepositoryDir, artifact.jarPath()).toURI().toURL()
            }
        } catch (e: MalformedURLException) {
            throw AssertionError(e)
        }
        return urls
    }

    private fun createLockFile(dependencyJar: DependencyJar): File {
        val lockFileName = dependencyJar.shortName.replace(SPECIAL_CHARACTERS_IN_FILE_NAME_REGEX.toRegex(), "_")
        return File(System.getProperty("user.home"), "$lockFileName.lock")
    }

    @Suppress("NestedBlockDepth")
    private fun whileLocked(dependencyJar: DependencyJar, runnable: Runnable) {
        val lockFile = createLockFile(dependencyJar)
        try {
            RandomAccessFile(lockFile, "rw").use { raf ->
                raf.channel.use { channel ->
                    var lock: FileLock? = null
                    while (lock == null) {
                        try {
                            lock = channel.tryLock()
                        } catch (e: OverlappingFileLockException) {
                            // Sleep for a while before retrying
                            Thread.sleep(100)
                        }
                    }
                    runnable.run()
                    lock.release()
                }
            }
        } catch (e: IOException) {
            throw IllegalStateException("Couldn't create lock file $lockFile", e)
        } finally {
            lockFile.delete()
        }
    }

    private companion object {
        private const val SPECIAL_CHARACTERS_IN_FILE_NAME_REGEX = """[<>:"\\/|\?\*]"""
    }
}

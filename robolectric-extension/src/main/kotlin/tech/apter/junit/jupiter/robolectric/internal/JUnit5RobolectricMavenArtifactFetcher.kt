package tech.apter.junit.jupiter.robolectric.internal

import com.google.common.base.Strings
import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.google.common.util.concurrent.AsyncCallable
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.robolectric.internal.dependency.MavenArtifactFetcher
import org.robolectric.internal.dependency.MavenJarArtifact
import org.robolectric.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService

/**
 * Class responsible for fetching artifacts from Maven. This uses a thread pool of size two in order
 * to parallelize downloads. It uses the Sun JSSE provider for downloading due to its seamless
 * integration with HTTPUrlConnection.
 */
internal class JUnit5RobolectricMavenArtifactFetcher
@Suppress("LongParameterList")
constructor(
    private val repositoryUrl: String,
    private val repositoryUserName: String?,
    private val repositoryPassword: String?,
    private val proxyHost: String?,
    private val proxyPort: Int,
    private val localRepositoryDir: File,
    private val executorService: ExecutorService
) : MavenArtifactFetcher(
    repositoryUrl,
    repositoryUserName,
    repositoryPassword,
    proxyHost,
    proxyPort,
    localRepositoryDir,
    executorService
) {
    private lateinit var stagingRepositoryDir: File

    @Suppress("UnstableApiUsage", "LongMethod")
    override fun fetchArtifact(artifact: MavenJarArtifact) {
        // Assume that if the file exists in the local repository, it has been fetched successfully.
        if (File(localRepositoryDir, artifact.jarPath()).exists()) {
            Logger.info("Found $artifact in local maven repository")
            return
        }
        stagingRepositoryDir = Files.createTempDir()
        stagingRepositoryDir.deleteOnExit()
        try {
            createArtifactSubdirectory(artifact, stagingRepositoryDir)
            Futures.whenAllSucceed(
                Futures.catching(
                    fetchToStagingRepository(artifact.pomSha512Path()),
                    Exception::class.java,
                    {
                        Futures.getDone(fetchToStagingRepository(artifact.pomSha1Path()))
                    },
                    executorService
                ),
                fetchToStagingRepository(artifact.pomPath()),
                Futures.catching(
                    fetchToStagingRepository(artifact.jarSha512Path()),
                    Exception::class.java,
                    {
                        Futures.getDone(fetchToStagingRepository(artifact.jarSha1Path()))
                    },
                    executorService
                ),
                fetchToStagingRepository(artifact.jarPath())
            ).callAsync(
                {
                    // double check that the artifact has not been installed
                    if (File(localRepositoryDir, artifact.jarPath()).exists()) {
                        removeArtifactFiles(stagingRepositoryDir, artifact)
                        return@callAsync Futures.immediateFuture<Any?>(null)
                    }
                    createArtifactSubdirectory(artifact, localRepositoryDir)
                    val pomResult =
                        validateStagedFiles(artifact.pomPath(), artifact.pomSha512Path(), artifact.pomSha1Path())
                    if (!pomResult.isSuccess()) {
                        throw AssertionError(
                            "SHA mismatch for POM file for $artifact, expected SHA=" +
                                "${pomResult.expectedHashCode()}, actual SHA=${pomResult.calculatedHashCode()}"
                        )
                    }
                    val jarResult =
                        validateStagedFiles(artifact.jarPath(), artifact.jarSha512Path(), artifact.jarSha1Path())
                    if (!jarResult.isSuccess()) {
                        throw AssertionError(
                            "SHA mismatch for jar file for $artifact, expected SHA=" +
                                "${jarResult.expectedHashCode()}, actual SHA=${jarResult.calculatedHashCode()}"
                        )
                    }
                    Logger.info("Checksums validated, moving artifact $artifact to local maven directory")
                    @Suppress("SwallowedException")
                    try {
                        commitFromStaging(artifact.pomSha512Path())
                    } catch (@Suppress("TooGenericExceptionCaught") e: IOException) {
                        commitFromStaging(artifact.pomSha1Path())
                    }
                    commitFromStaging(artifact.pomPath())
                    @Suppress("SwallowedException")
                    try {
                        commitFromStaging(artifact.jarSha512Path())
                    } catch (e: IOException) {
                        commitFromStaging(artifact.jarSha1Path())
                    }
                    commitFromStaging(artifact.jarPath())
                    removeArtifactFiles(stagingRepositoryDir, artifact)
                    Futures.immediateFuture(null)
                },
                executorService
            ).get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // Restore the interrupted status
            removeArtifactFiles(stagingRepositoryDir, artifact)
            removeArtifactFiles(localRepositoryDir, artifact)
            Logger.error("Failed to fetch maven artifact $artifact", e)
            throw AssertionError("Failed to fetch maven artifact $artifact", e)
        } catch (e: ExecutionException) {
            removeArtifactFiles(stagingRepositoryDir, artifact)
            removeArtifactFiles(localRepositoryDir, artifact)
            Logger.error("Failed to fetch maven artifact $artifact", e)
            throw AssertionError("Failed to fetch maven artifact $artifact", e)
        } catch (e: IOException) {
            removeArtifactFiles(stagingRepositoryDir, artifact)
            removeArtifactFiles(localRepositoryDir, artifact)
            Logger.error("Failed to fetch maven artifact $artifact", e)
            throw AssertionError("Failed to fetch maven artifact $artifact", e)
        }
    }

    private fun removeArtifactFiles(repositoryDir: File?, artifact: MavenJarArtifact) {
        File(repositoryDir, artifact.jarPath()).delete()
        File(repositoryDir, artifact.jarSha1Path()).delete()
        File(repositoryDir, artifact.pomPath()).delete()
        File(repositoryDir, artifact.pomSha1Path()).delete()
    }

    @Throws(IOException::class)
    private fun validateStagedFiles(
        filePath: String,
        sha512Path: String,
        sha1Path: String,
    ): ValidationResult {
        val tempFile = File(this.stagingRepositoryDir, filePath)
        val sha512File = File(this.stagingRepositoryDir, sha512Path)
        val sha1File = File(this.stagingRepositoryDir, sha1Path)

        return if (sha512File.isFile) {
            validateStagedFile(tempFile, sha512File, Hashing.sha512())
        } else if (sha1File.isFile) {
            validateStagedFile(tempFile, sha1File, Hashing.sha1())
        } else {
            ValidationResult.create(false, null, null)
        }
    }

    private fun validateStagedFile(
        file: File,
        shaFile: File,
        hashing: HashFunction,
    ): ValidationResult {
        val expected = HashCode.fromString(String(Files.asByteSource(shaFile).read(), StandardCharsets.UTF_8))

        val actual = Files.asByteSource(file).hash(hashing)
        return ValidationResult.create(
            expected == actual,
            expected.toString(),
            actual.toString()
        )
    }

    data class ValidationResult(
        private val isSuccess: Boolean,
        private val expectedHashCode: String?,
        private val calculatedHashCode: String?
    ) {

        fun isSuccess() = isSuccess

        fun expectedHashCode(): String? = expectedHashCode

        fun calculatedHashCode(): String? = calculatedHashCode

        companion object {
            fun create(
                isSuccess: Boolean,
                expectedHashCode: String?,
                calculatedHashCode: String?
            ): ValidationResult {
                return ValidationResult(
                    isSuccess,
                    expectedHashCode,
                    calculatedHashCode
                )
            }
        }
    }

    @Throws(IOException::class)
    private fun createArtifactSubdirectory(artifact: MavenJarArtifact, repositoryDir: File?) {
        val jarPath = File(repositoryDir, artifact.jarPath())
        Files.createParentDirs(jarPath)
    }

    private fun getRemoteUrl(path: String): URL {
        var url = this.repositoryUrl
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        try {
            return URI(url + path).toURL()
        } catch (e: URISyntaxException) {
            throw AssertionError(e)
        } catch (e: MalformedURLException) {
            throw AssertionError(e)
        }
    }

    private fun fetchToStagingRepository(path: String): ListenableFuture<Unit> {
        val remoteUrl = getRemoteUrl(path)
        val destination = File(this.stagingRepositoryDir, path)
        return createFetchToFileTask1(remoteUrl, destination)
    }

    private fun createFetchToFileTask1(remoteUrl: URL, tempFile: File): ListenableFuture<Unit> {
        return Futures.submitAsync(
            FetchToFileTask(
                remoteUrl,
                tempFile,
                repositoryUserName,
                repositoryPassword,
                proxyHost,
                proxyPort
            ),
            this.executorService
        )
    }

    @Throws(IOException::class)
    private fun commitFromStaging(path: String) {
        val source = File(this.stagingRepositoryDir, path)
        val destination = File(this.localRepositoryDir, path)
        Files.move(source, destination)
    }

    internal class FetchToFileTask(
        private val remoteURL: URL,
        private val localFile: File,
        private val repositoryUserName: String?,
        private val repositoryPassword: String?,
        private val proxyHost: String?,
        private val proxyPort: Int
    ) : AsyncCallable<Unit> {
        @Throws(Exception::class)
        override fun call(): ListenableFuture<Unit> {
            val connection: URLConnection
            if (this.proxyHost != null && proxyHost.isNotEmpty() && (this.proxyPort > 0)) {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(this.proxyHost, this.proxyPort))
                connection = remoteURL.openConnection(proxy)
            } else {
                connection = remoteURL.openConnection()
            }
            // Add authorization header if applicable.
            if (!Strings.isNullOrEmpty(this.repositoryUserName)) {
                val encoded = Base64.getEncoder().encodeToString(
                    (this.repositoryUserName + ":" + this.repositoryPassword).toByteArray(StandardCharsets.UTF_8)
                )
                connection.setRequestProperty("Authorization", "Basic $encoded")
            }

            Logger.info("Transferring $remoteURL")
            connection.getInputStream().use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    ByteStreams.copy(inputStream, outputStream)
                    // Ensure all contents are written to disk.
                    outputStream.flush()
                    outputStream.fd.sync()
                }
            }
            return Futures.immediateFuture<Unit>(Unit)
        }
    }
}

private fun MavenJarArtifact.jarSha1Path() = jarPath() + ".sha1"

private fun MavenJarArtifact.pomSha1Path() = pomPath() + ".sha1"

package tech.apter.junit.jupiter.robolectric.internal

import org.robolectric.util.Util
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal object AndroidMainThreadExecutor {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor(mainThreadFactory())

    @Suppress("SwallowedException", "TooGenericExceptionThrown")
    fun <T : Any?> execute(callable: Callable<T>): T {
        val future = executorService.submit(callable)
        try {
            return future.get()
        } catch (e: InterruptedException) {
            future.cancel(true)
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw Util.sneakyThrow<RuntimeException>(e.cause)
        }
    }

    fun shutdown() = executorService.shutdown()

    private fun mainThreadFactory(): ThreadFactory {
        return ThreadFactory { r: Runnable? ->
            val name = "Android SDK"
            Thread(ThreadGroup(name), r, "$name Main Thread")
        }
    }
}

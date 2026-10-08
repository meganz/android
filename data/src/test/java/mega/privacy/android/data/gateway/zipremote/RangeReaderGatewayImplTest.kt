package mega.privacy.android.data.gateway.zipremote

import com.google.common.truth.Truth.assertThat
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RangeReaderGatewayImplTest {

    private val content = ByteArray(10_000) { (it % 251).toByte() }
    private var server: HttpServer? = null
    private var stall = CountDownLatch(1)

    @AfterEach
    fun stop() {
        stall.countDown()
        stall = CountDownLatch(1)
        server?.stop(0)
    }

    @Test
    fun `test that copyRange writes the requested bytes at their offset`(@TempDir dir: File) =
        runTest {
            val underTest = RangeReaderGatewayImpl(UnconfinedTestDispatcher(testScheduler))
            val url = serve(honourRange = true)
            val destination = sparse(dir)

            underTest.copyRange(url, start = 4_000, length = 500, destination = destination)

            val written = ByteArray(500).also {
                RandomAccessFile(destination, "r").use { f -> f.seek(4_000); f.readFully(it) }
            }
            assertThat(written).isEqualTo(content.copyOfRange(4_000, 4_500))
        }

    @Test
    fun `test that copyRange reports the bytes it copied`(@TempDir dir: File) = runTest {
        val underTest = RangeReaderGatewayImpl(UnconfinedTestDispatcher(testScheduler))
        val url = serve(honourRange = true)
        var reported = 0L

        underTest.copyRange(url, 0, 1_000, sparse(dir)) { reported += it }

        assertThat(reported).isEqualTo(1_000)
    }

    @Test
    fun `test that copyRange fails when the server ignores the range`(@TempDir dir: File) =
        runTest {
            val underTest = RangeReaderGatewayImpl(UnconfinedTestDispatcher(testScheduler))
            val url = serve(honourRange = false)

            assertThrows<IOException> { underTest.copyRange(url, 4_000, 500, sparse(dir)) }
        }

    @Test
    fun `test that copyRange fails when the connection is refused`(@TempDir dir: File) = runTest {
        val underTest = RangeReaderGatewayImpl(UnconfinedTestDispatcher(testScheduler))
        assertThrows<IOException> {
            underTest.copyRange("http://127.0.0.1:1/none", 0, 10, sparse(dir))
        }
    }

    @Test
    fun `test that copyRange stops promptly when cancelled`(@TempDir dir: File) = runTest {
        val headersSent = CountDownLatch(1)
        val url = serveStalling(headersSent)
        val reader = RangeReaderGatewayImpl(ioDispatcher = Dispatchers.IO)
        val destination = sparse(dir)

        withContext(Dispatchers.Default) {
            val copy = launch { runCatching { reader.copyRange(url, 0, 1_000, destination) } }
            assertThat(headersSent.await(5, TimeUnit.SECONDS)).isTrue()

            copy.cancel()

            val stoppedIn = measureTime { withTimeout(5_000) { copy.join() } }
            assertThat(stoppedIn).isLessThan(1.seconds)
        }
    }

    private fun serveStalling(headersSent: CountDownLatch): String {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/a.zip") { exchange ->
            exchange.responseHeaders.add("Content-Range", "bytes 0-999/${content.size}")
            exchange.sendResponseHeaders(206, 1_000)
            exchange.responseBody.flush()
            headersSent.countDown()
            stall.await(30, TimeUnit.SECONDS)
            exchange.close()
        }
        http.start()
        server = http
        return "http://127.0.0.1:${http.address.port}/a.zip"
    }

    private fun sparse(dir: File) = File(dir, "a.zip").apply {
        RandomAccessFile(this, "rw").use { it.setLength(content.size.toLong()) }
    }

    private fun serve(honourRange: Boolean): String {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/a.zip") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")
            if (honourRange && range != null) {
                val (from, to) = range.removePrefix("bytes=").split("-").map { it.toInt() }
                val body = content.copyOfRange(from, to + 1)
                exchange.responseHeaders.add("Content-Range", "bytes $from-$to/${content.size}")
                exchange.sendResponseHeaders(206, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            } else {
                exchange.sendResponseHeaders(200, content.size.toLong())
                exchange.responseBody.use { it.write(content) }
            }
        }
        http.start()
        server = http
        return "http://127.0.0.1:${http.address.port}/a.zip"
    }
}

package mega.privacy.android.data.gateway.zipremote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class RangeReaderGatewayImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RangeReaderGateway {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .build()
    }

    override suspend fun copyRange(
        url: String,
        start: Long,
        length: Long,
        destination: File,
        onBytes: (Long) -> Unit,
    ) = withContext(ioDispatcher) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-${start + length - 1}")
            .build()
        val call = client.newCall(request)
        coroutineScope {
            // A socket read blocked on a stalled server does not notice cancellation. Cancelling
            // the call closes the socket, which ends the read with an exception; ensureActive then
            // reports the cancellation rather than that exception.
            val canceller = launch {
                try {
                    awaitCancellation()
                } finally {
                    call.cancel()
                }
            }
            try {
                call.execute().use { response ->
                    copy(response, start, length, destination, onBytes)
                }
            } catch (e: IOException) {
                ensureActive()
                throw e
            } finally {
                canceller.cancel()
            }
        }
    }

    private fun copy(
        response: Response,
        start: Long,
        length: Long,
        destination: File,
        onBytes: (Long) -> Unit,
    ) {
        val startedAt = TimeSource.Monotonic.markNow()
        val end = start + length - 1
        // A server that answers 200 is sending the whole file from byte 0; writing that at
        // [start] would put the wrong bytes where the archive expects this range.
        if (response.code != HTTP_PARTIAL_CONTENT) {
            throw IOException("Expected a partial response, got ${response.code}")
        }
        val contentRange = response.header("Content-Range").orEmpty()
        if (!contentRange.startsWith("bytes $start-")) {
            throw IOException("Unexpected Content-Range: $contentRange")
        }
        var copied = 0L
        RandomAccessFile(destination, "rw").use { file ->
            file.seek(start)
            response.body.byteStream().use { input ->
                val buffer = ByteArray(BUFFER_BYTES)
                while (copied < length) {
                    val read = input.read(
                        buffer,
                        0,
                        minOf(buffer.size.toLong(), length - copied).toInt(),
                    )
                    if (read == -1) break
                    file.write(buffer, 0, read)
                    copied += read
                    onBytes(read.toLong())
                }
            }
        }
        if (copied != length) throw IOException("Read $copied of $length bytes")
        Timber.d(
            "Range %d-%d: %d bytes in %d ms",
            start,
            end,
            copied,
            startedAt.elapsedNow().inWholeMilliseconds,
        )
    }

    private companion object {

        /**
         * The streaming server runs on this device, so a connection that is not made at once is
         * not going to be made.
         */
        val CONNECT_TIMEOUT = 5.seconds
        val READ_TIMEOUT = 30.seconds
        const val BUFFER_BYTES = 64 * 1024
        const val HTTP_PARTIAL_CONTENT = 206
    }
}

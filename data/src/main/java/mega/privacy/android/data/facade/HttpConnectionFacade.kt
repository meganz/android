package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.HttpConnectionGateway
import mega.privacy.android.domain.exception.DataTooLargeException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class HttpConnectionFacade @Inject constructor() : HttpConnectionGateway {
    override suspend fun getDataBytesFromUrl(url: URL): ByteArray? {
        val outputStream = ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        var bytesRead: Int
        val stream = getInputStream(url)
        return stream?.use {
            while (stream.read(chunk).also { readCount -> bytesRead = readCount } != -1) {
                outputStream.write(chunk, 0, bytesRead)
            }
            outputStream.toByteArray()
        }
    }

    override suspend fun getDataBytesFromUrl(url: URL, maxBytes: Int): ByteArray? {
        if (maxBytes <= 0) return ByteArray(0)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            applyTimeouts()
            setRequestProperty("Range", "bytes=0-${maxBytes - 1}")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                return null
            }
            connection.inputStream?.use { stream ->
                val outputStream = ByteArrayOutputStream()
                val chunk = ByteArray(4096)
                var total = 0
                while (total < maxBytes) {
                    val bytesRead = stream.read(chunk, 0, minOf(chunk.size, maxBytes - total))
                    if (bytesRead == -1) break
                    outputStream.write(chunk, 0, bytesRead)
                    total += bytesRead
                }
                outputStream.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun getDataBytesFromUrlWithinLimit(url: URL, maxBytes: Int): ByteArray? {
        require(maxBytes > 0) { "maxBytes must be positive" }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            applyTimeouts()
            // One byte past the limit is enough to learn that the content does not fit.
            setRequestProperty("Range", "bytes=0-$maxBytes")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                return null
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength > maxBytes) throw DataTooLargeException(maxBytes)
            connection.inputStream?.use { stream ->
                if (declaredLength > 0) {
                    stream.readUpTo(declaredLength.toInt())
                } else {
                    stream.readWithinLimit(maxBytes)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getInputStream(url: URL): InputStream? {
        val httpURLConnection = url.openConnection() as HttpURLConnection
        httpURLConnection.applyTimeouts()
        return if (httpURLConnection.responseCode == 200) {
            httpURLConnection.inputStream
        } else {
            null
        }
    }

    // A single allocation of the declared size; the server may still send less than declared.
    private fun InputStream.readUpTo(size: Int): ByteArray {
        val buffer = ByteArray(size)
        var total = 0
        while (total < size) {
            val bytesRead = read(buffer, total, size - total)
            if (bytesRead == -1) break
            total += bytesRead
        }
        return if (total == size) buffer else buffer.copyOf(total)
    }

    // Length unknown (chunked or gzip-decoded response): collect fixed blocks and drop the
    // response as soon as it passes the limit, so an oversized or endless stream costs at most
    // the limit plus one block and is never assembled.
    private fun InputStream.readWithinLimit(maxBytes: Int): ByteArray {
        val blocks = mutableListOf<ByteArray>()
        var total = 0L
        while (true) {
            val block = ByteArray(BLOCK_SIZE)
            var filled = 0
            while (filled < block.size) {
                val bytesRead = read(block, filled, block.size - filled)
                if (bytesRead == -1) break
                filled += bytesRead
            }
            if (filled == 0) break
            total += filled
            if (total > maxBytes) throw DataTooLargeException(maxBytes)
            blocks += if (filled == block.size) block else block.copyOf(filled)
            if (filled < block.size) break
        }
        val result = ByteArray(total.toInt())
        var offset = 0
        for (block in blocks) {
            block.copyInto(result, offset)
            offset += block.size
        }
        return result
    }

    private fun HttpURLConnection.applyTimeouts() {
        connectTimeout = CONNECT_TIMEOUT.inWholeMilliseconds.toInt()
        readTimeout = READ_TIMEOUT.inWholeMilliseconds.toInt()
    }

    private companion object {
        // Without these the platform default is unbounded, so a stalled peer holds the
        // reader forever.
        val CONNECT_TIMEOUT = 30.seconds
        val READ_TIMEOUT = 30.seconds
        const val BLOCK_SIZE = 1024 * 1024
    }
}

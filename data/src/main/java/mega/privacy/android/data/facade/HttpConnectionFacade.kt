package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.HttpConnectionGateway
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

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

    private fun getInputStream(url: URL): InputStream? {
        val httpURLConnection = url.openConnection() as HttpURLConnection
        return if (httpURLConnection.responseCode == 200) {
            httpURLConnection.inputStream
        } else {
            null
        }
    }
}
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

    private fun getInputStream(url: URL): InputStream? {
        val httpURLConnection = url.openConnection() as HttpURLConnection
        return if (httpURLConnection.responseCode == 200) {
            httpURLConnection.inputStream
        } else {
            null
        }
    }
}
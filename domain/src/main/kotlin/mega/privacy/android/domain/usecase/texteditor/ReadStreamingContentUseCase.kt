package mega.privacy.android.domain.usecase.texteditor

import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import java.io.IOException
import java.net.URL
import javax.inject.Inject

/**
 * Reads text content from a streaming URL returned by the local HTTP server.
 * Strips a single trailing newline to match the original file content.
 */
class ReadStreamingContentUseCase @Inject constructor(
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase,
) {

    /**
     * @param urlString Local streaming URL (e.g. from httpServerGetLocalLink).
     * @return The text content read from the URL.
     * @throws IOException when the server did not serve the content. Returning an empty string
     * instead would render the file as a blank document, and would stop the caller from falling
     * back to downloading it.
     */
    suspend operator fun invoke(urlString: String): String {
        val bytes = getDataBytesFromUrlUseCase(URL(urlString))
            ?: throw IOException("Streaming server did not serve content for $urlString")
        var result = String(bytes, Charsets.UTF_8)
        val lastBreak = result.lastIndexOf("\n")
        if (result.isNotEmpty() && lastBreak != -1 && result.length - lastBreak == 1) {
            result = result.removeRange(lastBreak, result.length)
        }
        return result
    }
}

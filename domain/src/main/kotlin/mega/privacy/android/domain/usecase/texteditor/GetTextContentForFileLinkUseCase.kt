package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import javax.inject.Inject

/**
 * Load text content from a public file link.
 *
 * Starts the streaming server, resolves a local streaming URL for the public link
 * via [GetFileUrlByPublicLinkUseCase], and reads the content via HTTP.
 */
class GetTextContentForFileLinkUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val startStreamingServer: StartStreamingServer,
    private val getFileUrlByPublicLinkUseCase: GetFileUrlByPublicLinkUseCase,
    private val readStreamingContentUseCase: ReadStreamingContentUseCase,
) {

    /**
     * @param urlFileLink Public file link URL.
     * @param chunkSizeLines Max lines per emission (default 500).
     */
    operator fun invoke(
        urlFileLink: String,
        chunkSizeLines: Int = 500,
    ): Flow<List<String>> = flow {
        startStreamingServer()
        val localUrl = getFileUrlByPublicLinkUseCase(urlFileLink)
            ?: throw IllegalStateException("Failed to get streaming URL for file link")
        val content = readStreamingContentUseCase(localUrl)
        val lines = content.split("\n")
        lines.chunked(chunkSizeLines).forEach { emit(it) }
    }.flowOn(ioDispatcher)
}

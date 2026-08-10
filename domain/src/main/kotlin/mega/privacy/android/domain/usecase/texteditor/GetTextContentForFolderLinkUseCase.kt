package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import javax.inject.Inject

/**
 * Load text content for a file opened from a folder link.
 *
 * Resolves a content URI via [GetFolderLinkNodeContentUriUseCase], which authorizes the node
 * through the folder API and picks the correct streaming API based on login state (the main
 * account API when the user is logged in, the folder API for anonymous access). Remote content
 * is read over HTTP; local content (already downloaded) is read from disk in chunks.
 *
 * The returned flow uses [flowOn] with [ioDispatcher] so all upstream work runs off the main
 * thread; callers can safely collect on Main.
 */
class GetTextContentForFolderLinkUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase,
    private val readStreamingContentUseCase: ReadStreamingContentUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * @param node Folder link file node to load.
     * @param chunkSizeLines Max lines per emission (default 500).
     * @return Flow of line chunks; caller should accumulate and cap display.
     */
    operator fun invoke(
        node: TypedFileNode,
        chunkSizeLines: Int = 500,
    ): Flow<List<String>> = flow {
        when (val contentUri = getFolderLinkNodeContentUriUseCase(node)) {
            is NodeContentUri.LocalContentUri ->
                emitAll(
                    fileSystemRepository.readLinesFromPathInChunks(
                        contentUri.file.absolutePath,
                        chunkSizeLines,
                    )
                )

            is NodeContentUri.RemoteContentUri -> {
                // NodeContentUri.shouldStopHttpSever is intentionally not honored here: the local
                // streaming server is left running, matching GetTextContentForFileLinkUseCase and
                // GetTextContentForTextEditorUseCase. The flag does not indicate which API's server
                // (main vs folder) was started, so it cannot be stopped reliably from this layer.
                val content = readStreamingContentUseCase(contentUri.url)
                content.split("\n").chunked(chunkSizeLines).forEach { emit(it) }
            }
        }
    }.flowOn(ioDispatcher)
}

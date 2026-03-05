package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import java.net.URL
import javax.inject.Inject

/** Cache folder name for text editor temp/download files (matches data layer constant). */
private const val TEXT_EDITOR_TEMP_FOLDER = "tempMEGA"

/**
 * Use case to load text content for the text editor.
 * Orchestrates: local path, then node local file, then streaming, then download. All I/O on [ioDispatcher].
 */
class GetTextContentForTextEditorUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase,
    private val startStreamingServer: StartStreamingServer,
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode,
    private val downloadNodeUseCase: DownloadNodeUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Load text for the given parameters.
     * @return Content string. Throws on error; caller (e.g. ViewModel) should catch and map to UI state.
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        nodeSourceType: Int,
        localPath: String? = null,
    ): String = withContext(ioDispatcher) {
        when (val resolved = resolveContentSource(nodeHandle, localPath)) {
            is ContentSource.LocalPath -> fileSystemRepository.readTextFromPath(resolved.path)
            is ContentSource.StreamingContent -> resolved.content
        }
    }

    private suspend fun resolveContentSource(nodeHandle: Long, localPath: String?): ContentSource {
        val contextLocalPath = localPath
        if (contextLocalPath != null) {
            if (!fileSystemRepository.doesFileExist(contextLocalPath)) {
                throw IllegalStateException("Local path does not exist: $contextLocalPath")
            }
            if (fileSystemRepository.isFolderPath(contextLocalPath)) {
                throw IllegalStateException("Path is a directory: $contextLocalPath")
            }
            return ContentSource.LocalPath(contextLocalPath)
        }
        val node = getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
            ?: throw IllegalStateException("Node not found or not a file")
        val localPath = getLocalFileForNodeUseCase(node)?.absolutePath
        if (localPath != null &&
            fileSystemRepository.doesFileExist(localPath) &&
            fileSystemRepository.isFilePath(localPath)
        ) {
            return ContentSource.LocalPath(localPath)
        }
        return tryStreamingThenDownload(node)
    }

    private suspend fun tryStreamingThenDownload(node: TypedFileNode): ContentSource {
        return runCatching {
            startStreamingServer()
            val urlString = getStreamingUriStringForNode(node)
            if (!urlString.isNullOrBlank()) {
                ContentSource.StreamingContent(readFromStreamingUrl(urlString))
            } else null
        }.getOrNull() ?: ContentSource.LocalPath(downloadThenReadPath(node))
    }

    private suspend fun readFromStreamingUrl(urlString: String): String {
        val bytes = getDataBytesFromUrlUseCase(URL(urlString)) ?: return ""
        var result = String(bytes, Charsets.UTF_8)
        val lastBreak = result.lastIndexOf("\n")
        if (result.isNotEmpty() && lastBreak != -1 && result.length - lastBreak == 1) {
            result = result.removeRange(lastBreak, result.length)
        }
        return result
    }

    private suspend fun downloadThenReadPath(node: TypedFileNode): String {
        val nodeFileName = node.name.ifEmpty { "file.txt" }
        val destFile = getCacheFileUseCase(TEXT_EDITOR_TEMP_FOLDER, nodeFileName)
            ?: throw IllegalStateException("Cannot get cache file for download")
        var finishError: MegaException? = null
        downloadNodeUseCase(
            node = node,
            destinationPath = destFile.absolutePath,
            appData = listOf(TransferAppData.BackgroundTransfer),
            isHighPriority = true,
        ).collect { event ->
            if (event is TransferEvent.TransferFinishEvent && event.error != null) {
                finishError = event.error
            }
        }
        finishError?.let { throw it }
        return destFile.absolutePath
    }

    /** Local file path to read from, or content already read from streaming URL. */
    private sealed interface ContentSource {
        data class LocalPath(val path: String) : ContentSource
        data class StreamingContent(val content: String) : ContentSource
    }
}

package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.file.GetPartialDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.domain.usecase.streaming.StartStreamingServer
import mega.privacy.android.domain.usecase.streaming.StopStreamingServer
import java.net.URL
import javax.inject.Inject

/**
 * Detects the real [FileTypeInfo] of a node from its content (magic numbers), reading only a
 * header so no full download is needed. Used to identify files that have no extension.
 *
 * The header is read from a local copy when one already exists (offline / cached), otherwise
 * from the node's streaming URL via an HTTP range request. For public-link nodes pass
 * [isLinkNode] = true: folder-link children are authorized and streamed from the main server via
 * [getLocalFolderLinkFromMegaApiUseCase], while file links (not present in the folder API) fall
 * back to [getNodeContentUriUseCase].
 *
 * @return the detected [FileTypeInfo], or null when the content cannot be recognised. An empty
 * file has no content to recognise and is reported as [TextFileTypeInfo] so it stays previewable.
 */
class GetFileTypeInfoByContentUseCase @Inject constructor(
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val startStreamingServer: StartStreamingServer,
    private val stopStreamingServer: StopStreamingServer,
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode,
    private val getPartialDataBytesFromUrlUseCase: GetPartialDataBytesFromUrlUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
) {
    suspend operator fun invoke(node: TypedFileNode, isLinkNode: Boolean = false): FileTypeInfo? {
        if (node.size == 0L) {
            return TextFileTypeInfo(mimeType = "text/plain", extension = node.type.extension)
        }

        val header = readHeader(node, isLinkNode)?.takeIf { it.isNotEmpty() } ?: return null
        val duration = node.type.toDuration()?.inWholeSeconds?.toInt() ?: 0
        return fileSystemRepository.getFileTypeInfoFromContent(header, duration)
    }

    private suspend fun readHeader(node: TypedFileNode, isLinkNode: Boolean): ByteArray? {
        runCatching { getLocalFileForNodeUseCase(node) }.getOrNull()?.let { localFile ->
            if (localFile.exists() && localFile.isFile) {
                return fileSystemRepository.readFirstBytesFromPath(
                    localFile.absolutePath,
                    HEADER_SIZE_BYTES,
                )
            }
        }
        return if (isLinkNode) {
            readHeaderFromLink(node)
        } else {
            readHeaderFromStreaming(node)
        }
    }

    private suspend fun readHeaderFromLink(node: TypedFileNode): ByteArray? {
        val wasRunning = runCatching { httpServerIsRunning() != 0 }.getOrDefault(false)
        if (!wasRunning) startStreamingServer()
        return try {
            val authorizedUrl = getLocalFolderLinkFromMegaApiUseCase(node.id.longValue)
            if (authorizedUrl != null) {
                getPartialDataBytesFromUrlUseCase(URL(authorizedUrl), HEADER_SIZE_BYTES)
            } else {
                readHeaderFromNodeContentUri(node)
            }
        } catch (e: Exception) {
            null
        } finally {
            if (!wasRunning) {
                runCatching { stopStreamingServer() }
            }
        }
    }

    private suspend fun readHeaderFromNodeContentUri(node: TypedFileNode): ByteArray? =
        when (val contentUri = runCatching { getNodeContentUriUseCase(node) }.getOrNull()) {
            is NodeContentUri.LocalContentUri ->
                fileSystemRepository.readFirstBytesFromPath(
                    contentUri.file.absolutePath,
                    HEADER_SIZE_BYTES,
                )

            is NodeContentUri.RemoteContentUri ->
                getPartialDataBytesFromUrlUseCase(URL(contentUri.url), HEADER_SIZE_BYTES)

            null -> null
        }

    private suspend fun readHeaderFromStreaming(node: TypedFileNode): ByteArray? {
        val wasRunning = runCatching { httpServerIsRunning() != 0 }.getOrDefault(false)
        return try {
            if (!wasRunning) startStreamingServer()
            getStreamingUriStringForNode(node)?.takeIf { it.isNotBlank() }?.let { url ->
                getPartialDataBytesFromUrlUseCase(URL(url), HEADER_SIZE_BYTES)
            }
        } catch (e: Exception) {
            null
        } finally {
            if (!wasRunning) {
                runCatching { stopStreamingServer() }
            }
        }
    }

    companion object {
        internal const val HEADER_SIZE_BYTES = 64 * 1024
    }
}

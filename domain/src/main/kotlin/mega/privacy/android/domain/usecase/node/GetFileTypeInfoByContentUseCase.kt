package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
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
 * from the node's streaming URL via an HTTP range request.
 *
 * @return the detected [FileTypeInfo], or null when the content cannot be recognised.
 */
class GetFileTypeInfoByContentUseCase @Inject constructor(
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val startStreamingServer: StartStreamingServer,
    private val stopStreamingServer: StopStreamingServer,
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode,
    private val getPartialDataBytesFromUrlUseCase: GetPartialDataBytesFromUrlUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    suspend operator fun invoke(node: TypedFileNode): FileTypeInfo? {
        val header = readHeader(node)?.takeIf { it.isNotEmpty() } ?: return null
        val duration = node.type.toDuration()?.inWholeSeconds?.toInt() ?: 0
        return fileSystemRepository.getFileTypeInfoFromContent(header, duration)
    }

    private suspend fun readHeader(node: TypedFileNode): ByteArray? {
        runCatching { getLocalFileForNodeUseCase(node) }.getOrNull()?.let { localFile ->
            if (localFile.exists() && localFile.isFile) {
                return fileSystemRepository.readFirstBytesFromPath(
                    localFile.absolutePath,
                    HEADER_SIZE_BYTES,
                )
            }
        }
        return readHeaderFromStreaming(node)
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

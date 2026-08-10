package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class GetOpenableLocalFileForCloudDriveSafUseCase @Inject constructor(
    private val getFileNodeContentForFileNodeUseCase: GetFileNodeContentForFileNodeUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val downloadSafFileForNodeAndAwaitUseCase: DownloadSafFileForNodeAndAwaitUseCase,
) {

    private val downloadMutexes = ConcurrentHashMap<Long, Mutex>()

    suspend operator fun invoke(fileNode: TypedFileNode): File =
        findCachedLocalFile(fileNode) ?: downloadAndGetPreviewFile(fileNode)

    private suspend fun findCachedLocalFile(fileNode: TypedFileNode): File? = runCatching {
        val candidate: File? = when (val content =
            getFileNodeContentForFileNodeUseCase(fileNode, isLinkNode = false)) {
            is FileNodeContent.Pdf -> content.uri.localFileOrNull()
            is FileNodeContent.AudioOrVideo -> content.uri.localFileOrNull()
            is FileNodeContent.TextContent -> getNodeContentUriUseCase(fileNode).localFileOrNull()
            is FileNodeContent.ImageForNode -> getNodeContentUriUseCase(fileNode).localFileOrNull()
            is FileNodeContent.Other -> content.localFile
            else -> null
        }
        candidate?.takeIf { it.isFile && it.exists() && it.length() > 0L }
    }.getOrNull()

    private fun NodeContentUri.localFileOrNull(): File? =
        (this as? NodeContentUri.LocalContentUri)?.file

    private suspend fun downloadAndGetPreviewFile(node: TypedFileNode): File {
        val mutex = downloadMutexes.computeIfAbsent(node.id.longValue) { Mutex() }
        return try {
            mutex.withLock {
                downloadSafFileForNodeAndAwaitUseCase(node)
            }
        } finally {
            // Only remove if no other coroutine is queued
            if (!mutex.isLocked) {
                downloadMutexes.remove(node.id.longValue, mutex)
            }
        }
    }
}

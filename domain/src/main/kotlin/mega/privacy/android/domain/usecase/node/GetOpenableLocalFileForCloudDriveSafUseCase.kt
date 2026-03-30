package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** Resolves a cloud [TypedFileNode] to a local [File] for SAF; uses preview download when needed (per-node lock). */
class GetOpenableLocalFileForCloudDriveSafUseCase @Inject constructor(
    private val getFileNodeContentForFileNodeUseCase: GetFileNodeContentForFileNodeUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val downloadPreviewFileForNodeAndAwaitUseCase: DownloadPreviewFileForNodeAndAwaitUseCase,
) {

    private val downloadMutexes = ConcurrentHashMap<Long, Mutex>()

    suspend operator fun invoke(fileNode: TypedFileNode): File {
        val content = getFileNodeContentForFileNodeUseCase(fileNode, isLinkNode = false)
        return when (content) {
            is FileNodeContent.Pdf ->
                resolveNodeContentUriToOpenableFile(content.uri, fileNode)

            is FileNodeContent.TextContent ->
                resolveNodeContentUriToOpenableFile(getNodeContentUriUseCase(fileNode), fileNode)

            is FileNodeContent.ImageForNode ->
                resolveNodeContentUriToOpenableFile(getNodeContentUriUseCase(fileNode), fileNode)

            is FileNodeContent.AudioOrVideo ->
                resolveNodeContentUriToOpenableFile(content.uri, fileNode)

            //TODO AND-22993
            //is FileNodeContent.UrlContent ->
            //resolveUrlFileContentToOpenableFile(content, fileNode)

            //is FileNodeContent.LocalZipFile ->
            //openOtherOrDownloadPreview(content.localFile, fileNode)

            is FileNodeContent.Other -> {
                val cachedLocalFile = content.localFile
                when {
                    fileNode.type is ZipFileTypeInfo ->
                        openOtherOrDownloadPreview(cachedLocalFile, fileNode)

                    cachedLocalFile != null &&
                            cachedLocalFile.isFile &&
                            cachedLocalFile.exists() &&
                            cachedLocalFile.length() > 0L ->
                        cachedLocalFile

                    else -> downloadAndGetPreviewFile(fileNode)
                }
            }

            else -> downloadAndGetPreviewFile(fileNode)
        }
    }

    private suspend fun openOtherOrDownloadPreview(
        localFile: File?,
        fileNode: TypedFileNode,
    ): File {
        if (localFile != null && localFile.exists() && localFile.length() > 0L) {
            return localFile
        }
        return downloadAndGetPreviewFile(fileNode)
    }

    private suspend fun resolveNodeContentUriToOpenableFile(
        uri: NodeContentUri,
        fileNode: TypedFileNode,
    ): File = when (uri) {
        is NodeContentUri.LocalContentUri -> {
            val localFile = uri.file
            if (localFile.isFile && localFile.exists() && localFile.length() > 0L) {
                localFile
            } else {
                downloadAndGetPreviewFile(fileNode)
            }
        }

        is NodeContentUri.RemoteContentUri ->
            downloadAndGetPreviewFile(fileNode)
    }

    private suspend fun downloadAndGetPreviewFile(node: TypedFileNode): File {
        val mutex = downloadMutexes.computeIfAbsent(node.id.longValue) { Mutex() }
        return try {
            mutex.withLock {
                downloadPreviewFileForNodeAndAwaitUseCase(node)
            }
        } finally {
            // Only remove if no other coroutine is queued
            if (!mutex.isLocked) {
                downloadMutexes.remove(node.id.longValue, mutex)
            }
        }
    }
}

package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Downloads a node to the preview cache folder and suspends until the transfer finishes, tagging
 * the transfer with [TransferAppData.SafDownload] so it's filtered out of the user-visible
 * transfer surfaces (Transfer screen, toolbar widget).
 *
 * @param getFilePreviewDownloadPathUseCase
 * @param downloadNodeUseCase
 */
class DownloadSafFileForNodeAndAwaitUseCase @Inject constructor(
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
) {

    /**
     * Anonymous operator function
     * @param node
     * @return the file
     * @throws FileNotFoundException
     */
    suspend operator fun invoke(node: TypedFileNode): File {
        val downloadPath = getFilePreviewDownloadPathUseCase()
        val downloadDir = File(downloadPath)
        val destFile = File(downloadDir, node.name)
        if (!destFile.isContainedIn(downloadDir)) {
            throw FileNotFoundException("Invalid node name for SAF download: ${node.name}")
        }
        if (destFile.exists() && destFile.length() > 0L) {
            return destFile
        }
        destFile.delete()
        val finishEvent = withTimeout(SAF_DOWNLOAD_TIMEOUT_MS.milliseconds) {
            downloadNodeUseCase(
                node = node,
                destinationPath = downloadPath,
                appData = listOf(TransferAppData.SafDownload),
                isHighPriority = true,
            ).first { it is TransferEvent.TransferFinishEvent } as TransferEvent.TransferFinishEvent
        }
        if (finishEvent.error != null || !destFile.exists() || destFile.length() == 0L) {
            throw FileNotFoundException("SAF download failed or was cancelled: ${node.name}")
        }
        return destFile
    }

    /**
     * Guards against path traversal: a node name is attacker-controlled and may contain "/" or
     * ".." (the app's [ValidateNodeNameUseCase] is bypassed for names set directly via the SDK),
     * which [File] would resolve outside [directory]. Compare canonical paths so the destination is
     * proven to stay within the preview-cache directory before it is read, deleted, or returned.
     */
    private fun File.isContainedIn(directory: File): Boolean {
        val dirPath = directory.canonicalPath
        val filePath = canonicalPath
        return filePath == dirPath || filePath.startsWith(dirPath + File.separator)
    }

    private companion object {
        const val SAF_DOWNLOAD_TIMEOUT_MS = 120_000L // 2 min
    }
}

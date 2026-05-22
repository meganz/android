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

/**
 * Downloads a node to the preview cache folder and suspends until the transfer finishes, tagging
 * the transfer with [TransferAppData.SafDownload] so it's filtered out of the user-visible
 * transfer surfaces (Transfer screen, toolbar widget).
 */
class DownloadSafFileForNodeAndAwaitUseCase @Inject constructor(
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
) {

    suspend operator fun invoke(node: TypedFileNode): File {
        val downloadPath = getFilePreviewDownloadPathUseCase()
        val destFile = File(downloadPath, node.name)
        if (destFile.exists() && destFile.length() > 0L) {
            return destFile
        }
        destFile.delete()
        val finishEvent = withTimeout(SAF_DOWNLOAD_TIMEOUT_MS) {
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

    private companion object {
        const val SAF_DOWNLOAD_TIMEOUT_MS = 120_000L // 2 min
    }
}

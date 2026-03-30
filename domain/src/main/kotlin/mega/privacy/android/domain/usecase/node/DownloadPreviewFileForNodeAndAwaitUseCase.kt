package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWorkerAndWaitUntilIsStartedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingDownloadsForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.previews.GetPreviewDownloadUseCase
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

/** Preview download to cache; waits until file exists ([TransferAppData.PreviewDownload]). */
class DownloadPreviewFileForNodeAndAwaitUseCase @Inject constructor(
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val getPreviewDownloadUseCase: GetPreviewDownloadUseCase,
    private val insertPendingDownloadsForNodesUseCase: InsertPendingDownloadsForNodesUseCase,
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase: StartDownloadsWorkerAndWaitUntilIsStartedUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
) {

    suspend operator fun invoke(node: TypedFileNode): File {
        val downloadPath = getFilePreviewDownloadPathUseCase()
        val destFile = File(downloadPath, node.name)
        if (destFile.exists() && destFile.length() > 0L) {
            return destFile
        }
        if (getPreviewDownloadUseCase(node) == null) {
            destFile.delete()
            insertPendingDownloadsForNodesUseCase(
                nodes = listOf(node),
                destination = UriPath(downloadPath),
                isHighPriority = true,
                appData = TransferAppData.PreviewDownload,
            )
            startDownloadsWorkerAndWaitUntilIsStartedUseCase()
        }
        waitUntilPreviewFileReady(node.name, destFile)
        if (!destFile.exists()) {
            throw FileNotFoundException("Preview download failed or was cancelled: ${node.name}")
        }
        return destFile
    }

    private suspend fun waitUntilPreviewFileReady(fileName: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0L) return
        withTimeout(PREVIEW_DOWNLOAD_TIMEOUT_MS) {
            var seenTransfer = false
            monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)
                .first { result ->
                    val previewGroups = result.activeTransferTotals.actionGroups
                        .filter { it.isPreviewDownload() && it.singleFileName == fileName }
                    if (previewGroups.isNotEmpty()) {
                        seenTransfer = true
                    }
                    seenTransfer &&
                            destFile.exists() &&
                            destFile.length() > 0L
                }
        }
    }

    private companion object {
        const val PREVIEW_DOWNLOAD_TIMEOUT_MS = 120_000L // 2 min
    }
}

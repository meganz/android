package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.HandleAvailableOfflineEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import javax.inject.Inject

/**
 * Use case to handle download transfer events
 */
class HandleDownloadTransferEventsUseCase @Inject constructor(
    private val scanMediaFileUseCase: ScanMediaFileUseCase,
    private val handleAvailableOfflineEventUseCase: HandleAvailableOfflineEventUseCase,
    private val broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
) : IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        events.asList().takeIf { it.isNotEmpty() }?.let { downloadEvents ->
            coroutineScope {
                val checkQuotaDeferred = async { checkTransferOverQuota(downloadEvents) }
                val handleOfflineDeferred = async { handleAvailableOffline(downloadEvents) }
                val scanMediaDeferred = async { scanMediaFiles(downloadEvents) }

                checkQuotaDeferred.await()
                handleOfflineDeferred.await()
                scanMediaDeferred.await()
            }
        }
    }

    private suspend fun checkTransferOverQuota(events: List<TransferEvent>) {
        events.scan(null as Boolean?) { hasOverQuota, event ->
            when {
                !event.transfer.transferType.isDownloadType() -> hasOverQuota
                event is TransferEvent.TransferStartEvent || event is TransferEvent.TransferUpdateEvent -> false
                (event as? TransferEvent.TransferTemporaryErrorEvent)?.error is QuotaExceededMegaException -> true
                else -> hasOverQuota
            }
        }.lastOrNull()?.let { isTransferOverQuota ->
            broadcastTransferOverQuotaUseCase(isTransferOverQuota)
        }
    }

    private suspend fun handleAvailableOffline(events: List<TransferEvent>) {
        events.forEach { event ->
            runCatching { handleAvailableOfflineEventUseCase(event) }
        }
    }

    private fun scanMediaFiles(events: List<TransferEvent>) {
        events.mapNotNull { event ->
            if (event.transfer.transferType == TransferType.DOWNLOAD
                && event is TransferEvent.TransferFinishEvent
                && event.transfer.transferredBytes == event.transfer.totalBytes
                && event.error == null
            ) event.transfer.localPath else null
        }.takeIf { it.isNotEmpty() }
            ?.let { paths ->
                runCatching {
                    scanMediaFileUseCase(paths.toTypedArray(), arrayOf(""))
                }
            }
    }
}
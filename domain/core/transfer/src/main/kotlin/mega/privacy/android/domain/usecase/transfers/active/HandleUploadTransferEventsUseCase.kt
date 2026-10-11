package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import javax.inject.Inject

/**
 * Use case to handle upload transfer events
 */
class HandleUploadTransferEventsUseCase @Inject constructor(
    private val setNodeAttributesAfterUploadUseCase: SetNodeAttributesAfterUploadUseCase,
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        events.asList().takeIf { it.isNotEmpty() }?.let { uploadEvents ->
            coroutineScope {
                val checkQuotaDeferred = async { checkStorageOverQuota(uploadEvents) }
                val checkAttributesDeferred = async { checkNodeAttributes(uploadEvents) }

                checkQuotaDeferred.await()
                checkAttributesDeferred.await()
            }
        }
    }

    private suspend fun checkStorageOverQuota(events: List<TransferEvent>) {
        events.scan(null as Boolean?) { hasOverQuota, event ->
            when {
                !event.transfer.transferType.isUploadType() -> hasOverQuota
                event is TransferEvent.TransferStartEvent || event is TransferEvent.TransferUpdateEvent -> false
                (event as? TransferEvent.TransferTemporaryErrorEvent)?.error is QuotaExceededMegaException
                        && event.transfer.isForeignOverQuota.not() -> true

                else -> hasOverQuota
            }
        }.lastOrNull()?.let { isStorageOverQuota ->
            broadcastStorageOverQuotaUseCase(isStorageOverQuota)
        }
    }

    /**
     * setNodeAttributesAfterUploadUseCase takes a while, so we need to launch it in the application
     * scope to avoid blocking the thread. The worker can cancel the job when it finishes,
     * so we need to launch it in a non cancellable context.
     */
    private fun checkNodeAttributes(events: List<TransferEvent>) {
        applicationScope.launch {
            withContext(NonCancellable) {
                events.filter { event ->
                    (event.transfer.transferType == TransferType.GENERAL_UPLOAD || event.transfer.transferType == TransferType.CHAT_UPLOAD)
                            && event is TransferEvent.TransferFinishEvent
                            && event.error == null
                }.forEach { event ->
                    runCatching {
                        setNodeAttributesAfterUploadUseCase(
                            nodeHandle = event.transfer.nodeHandle,
                            uriPath = UriPath(event.transfer.localPath),
                            appData = event.transfer.appData
                        )
                    }
                }
            }
        }
    }
}
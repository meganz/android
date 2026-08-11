package mega.privacy.android.app.appstate.global.initialisation.appstart

import kotlinx.coroutines.flow.catch
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.domain.entity.transfer.TransferOverQuotaStatus
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaEventUseCase
import mega.privacy.android.domain.usecase.transfers.previews.CancelOverQuotaPreviewDownloadsUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Monitors bandwidth over quota hit while downloading, so the warning is queued for the activity
 * that is in front rather than only reaching the single activity shell.
 *
 * Preview downloads are also cancelled once the quota is exceeded: they cannot advance, and an
 * unfinished one is resumed in the next session keeping its start time, so it would keep opening
 * the loading preview screen for a file nobody is waiting for any more.
 */
class DownloadOverQuotaInitialiser @Inject constructor(
    monitorTransferOverQuotaEventUseCase: MonitorTransferOverQuotaEventUseCase,
    cancelOverQuotaPreviewDownloadsUseCase: CancelOverQuotaPreviewDownloadsUseCase,
    transferOverQuotaEventQueue: TransferOverQuotaEventQueue,
) : AppStartInitialiserAction(action = {
    monitorTransferOverQuotaEventUseCase()
        .catch { Timber.e(it, "Error monitoring download over quota events") }
        .collect { status ->
            Timber.d("Emit download over quota $status")
            transferOverQuotaEventQueue.emit(TransferOverQuotaSource.Download)

            if (status == TransferOverQuotaStatus.OverQuota) {
                runCatching { cancelOverQuotaPreviewDownloadsUseCase() }
                    .onSuccess { tags ->
                        if (tags.isNotEmpty()) {
                            Timber.d("Cancelled preview downloads over quota: $tags")
                        }
                    }
                    .onFailure { Timber.e(it, "Error cancelling preview downloads over quota") }
            }
        }
})

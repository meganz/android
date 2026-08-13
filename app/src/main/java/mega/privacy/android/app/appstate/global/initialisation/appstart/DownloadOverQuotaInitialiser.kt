package mega.privacy.android.app.appstate.global.initialisation.appstart

import kotlinx.coroutines.flow.catch
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.domain.entity.transfer.TransferOverQuotaStatus
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
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
 *
 * The app wide over quota state is raised alongside the queued warning, because the warning screen
 * reads its severity from that state rather than from the event: without it the screen opens with
 * the "transfer quota running low" copy over an exhausted quota. Downloads going through the
 * monitored transfer pipeline already raise it themselves, but the ones on their own SDK listener,
 * such as full size image previews, do not.
 */
class DownloadOverQuotaInitialiser @Inject constructor(
    monitorTransferOverQuotaEventUseCase: MonitorTransferOverQuotaEventUseCase,
    cancelOverQuotaPreviewDownloadsUseCase: CancelOverQuotaPreviewDownloadsUseCase,
    broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
    transferOverQuotaEventQueue: TransferOverQuotaEventQueue,
) : AppStartInitialiserAction(action = {
    monitorTransferOverQuotaEventUseCase()
        .catch { Timber.e(it, "Error monitoring download over quota events") }
        .collect { status ->
            Timber.d("Emit download over quota $status")
            val isOverQuota = status == TransferOverQuotaStatus.OverQuota

            if (isOverQuota) {
                runCatching { broadcastTransferOverQuotaUseCase(true) }
                    .onFailure { Timber.e(it, "Error broadcasting download over quota") }
            }

            transferOverQuotaEventQueue.emit(TransferOverQuotaSource.Download)

            if (isOverQuota) {
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

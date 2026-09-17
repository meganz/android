package mega.privacy.android.app.appstate.global.initialisation.appstart

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsStreamingMediaContentUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferQuotaStateEventUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Monitors the transfer quota state the SDK predicts, so the user is warned before the bandwidth
 * is spent rather than once it is gone.
 *
 * Only the running low state is warned about. Red means what is queued cannot all complete, which
 * the over quota flow already reports once the transfers block, and warning here as well would put
 * a second dialog on top of it.
 *
 * Unlike [StreamOverQuotaInitialiser] it does not raise the app wide over quota state: that pauses
 * the transfers UI and cancels preview downloads, wrong while the account can still transfer.
 */
class TransferQuotaStateInitialiser @Inject constructor(
    monitorTransferQuotaStateEventUseCase: MonitorTransferQuotaStateEventUseCase,
    isStreamingMediaContentUseCase: IsStreamingMediaContentUseCase,
    isInTransferOverQuotaUseCase: IsInTransferOverQuotaUseCase,
    transferOverQuotaEventQueue: TransferOverQuotaEventQueue,
) : AppStartInitialiserAction(action = {
    monitorTransferQuotaStateEventUseCase()
        .catch { Timber.e(it, "Error monitoring predicted transfer quota state events") }
        .filter { it == TransferQuotaState.Orange }
        // An exhausted quota has its own warning, and running low is no longer news once the
        // transfers have blocked, so this would only stack a second dialog on top of it.
        .filterNot { isInTransferOverQuotaUseCase() }
        .collect {
            val source = if (isStreamingMediaContentUseCase()) {
                TransferOverQuotaSource.Streaming
            } else {
                TransferOverQuotaSource.Download
            }
            Timber.d("Emit transfer quota running low as $source")
            transferOverQuotaEventQueue.emit(source)
        }
})

package mega.privacy.android.app.appstate.global.initialisation.appstart

import kotlinx.coroutines.flow.catch
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStreamOverQuotaEventUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Monitors bandwidth over quota hit while streaming and broadcasts the transfer over quota state, so
 * the over quota banner, notification and warning dialog react to it app wide.
 *
 * Streaming can be started from any activity, so monitoring runs at application scope rather than
 * being tied to the activity that started playback.
 */
class StreamOverQuotaInitialiser @Inject constructor(
    monitorStreamOverQuotaEventUseCase: MonitorStreamOverQuotaEventUseCase,
    broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
) : AppStartInitialiserAction(action = {
    monitorStreamOverQuotaEventUseCase()
        .catch { Timber.e(it, "Error monitoring streaming over quota events") }
        .collect { timeLeft ->
            Timber.d("Emit stream over quota $timeLeft")
            broadcastTransferOverQuotaUseCase(true)
        }
})

package mega.privacy.android.app.appstate.global.quota

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel exposing the pending bandwidth over quota event to the activity hosting the navigation
 * back stack.
 *
 * Scoped to that activity, so the warning is shown again when the user opens another one.
 */
@HiltViewModel
class TransferOverQuotaWarningViewModel @Inject constructor(
    private val transferOverQuotaEventQueue: TransferOverQuotaEventQueue,
) : ViewModel() {

    /**
     * Emits while an over quota warning is waiting to be shown.
     */
    val transferOverQuotaEvents: Flow<TransferOverQuotaSource> = transferOverQuotaEventQueue.events

    /**
     * Claim the pending warning, or null when another activity already took it.
     *
     * Every queued event raises a warning: a download or upload the user just started must warn
     * straight away, so this cannot be throttled. Repeats are prevented at the source instead, by
     * whatever hit the quota not retrying on its own.
     */
    fun consumeTransferOverQuotaEvent(): TransferOverQuotaSource? =
        transferOverQuotaEventQueue.consume()
}

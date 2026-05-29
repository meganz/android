package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersResumedEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.UpdateActiveTransfersUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Initializer that updates active transfers after being resumed.
 */
class UpdateActiveTransfersInitializer @Inject constructor(
    private val monitorTransfersResumedEventUseCase: MonitorTransfersResumedEventUseCase,
    private val updateActiveTransfersUseCase: UpdateActiveTransfersUseCase,
) : PostLoginInitialiserAction(action = { _, _ ->
    var reconnectDelay = Duration.ZERO
    monitorTransfersResumedEventUseCase()
        .retry {
            // In case of an error we need to keep monitoring the events, but we add an exponential
            // delay before retrying to avoid potential infinite sync loops in case of recurrent error
            Timber.e(it, "Error monitoring transfers resumed event, retrying in $reconnectDelay")
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceIn(100.milliseconds, 10.minutes)
            true
        }.collect {
            // reset the delay on each successful collect
            runCatching { updateActiveTransfersUseCase() }
                .onFailure {
                    Timber.e(
                        it,
                        "Error updating active transfers in UpdateActiveTransfersInitializer"
                    )
                }
            reconnectDelay = Duration.ZERO
            Timber.v("$it transfers resumed")
        }
})
package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import mega.privacy.android.domain.usecase.transfers.active.MonitorAndHandleTransferEventsUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Initializer that monitors and handles transfer events.
 */
class MonitorTransferEventsInitializer @Inject constructor(
    private val monitorAndHandleTransferEventsUseCase: MonitorAndHandleTransferEventsUseCase,
) : AppStartInitialiserAction(action = {
    var reconnectDelay = Duration.ZERO
    monitorAndHandleTransferEventsUseCase()
        .retry {
            // In case of an error we need to keep monitoring the events, but we add a exponential delay before retrying to avoid potential infinite sync loops in case of recurrent error
            Timber.e(it, "Error monitoring transfer events, retrying in $reconnectDelay")
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceIn(100.milliseconds, 10.minutes)
            true
        }
        .collect {
            // reset the delay on each successful collect
            reconnectDelay = Duration.ZERO
            Timber.v("$it transfer events processed")
        }
})
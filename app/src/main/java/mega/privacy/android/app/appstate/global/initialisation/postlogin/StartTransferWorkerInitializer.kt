package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import mega.privacy.android.domain.usecase.transfers.active.MonitorTransferEventsToStartWorkersIfNeededUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Initializer that monitors transfer events and starts transfers workers accordingly.
 */
class StartTransferWorkerInitializer @Inject constructor(
    private val monitorTransferEventsToStartWorkersIfNeededUseCase: MonitorTransferEventsToStartWorkersIfNeededUseCase,
) : AppStartInitialiserAction(action = {
    var reconnectDelay = Duration.ZERO
    monitorTransferEventsToStartWorkersIfNeededUseCase()
        .retry {
            Timber.e(it, "Error starting Workers, retrying in $reconnectDelay")
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceIn(100.milliseconds, 10.minutes)
            true
        }
        .collect {
            reconnectDelay = Duration.ZERO
            Timber.v("Worker started for $it")
        }
})
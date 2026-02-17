package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.delay
import mega.privacy.android.domain.usecase.transfers.active.UpdateActiveTransfersUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Initializer that updates active transfers.
 */
class UpdateActiveTransfersInitializer @Inject constructor(
    private val updateActiveTransfersUseCase: UpdateActiveTransfersUseCase,
) : PostLoginInitialiserAction(action = { _, _ ->
    runCatching {
        repeat(10) { times ->
            if (updateActiveTransfersUseCase()) {
                return@runCatching
            } else {
                //SDK takes a while to load transfers and there's no event to listen to.
                Timber.d("Waiting for SDK to load transfers: $times")
                delay((times + 1) * 20L)
            }
        }
    }.onFailure {
        Timber.e(it, "Error updating active transfers in MonitorTransferEventsInitializer")
    }
})
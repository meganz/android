package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

class DeleteOldestCompletedTransfersInitializer @Inject constructor(
    private val deleteOldestCompletedTransfersUseCase: DeleteOldestCompletedTransfersUseCase,
) : PostLoginInitialiserAction({ _, _ ->
    runCatching {
        deleteOldestCompletedTransfersUseCase()
    }.onFailure { Timber.e(it) }
})
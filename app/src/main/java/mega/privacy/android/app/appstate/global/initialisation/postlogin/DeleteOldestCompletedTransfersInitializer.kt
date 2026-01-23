package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import timber.log.Timber
import javax.inject.Inject

class DeleteOldestCompletedTransfersInitializer @Inject constructor(
    private val deleteOldestCompletedTransfersUseCase: DeleteOldestCompletedTransfersUseCase,
) : PostLoginInitialiser({ _, _ ->
    runCatching {
        deleteOldestCompletedTransfersUseCase()
    }.onFailure { Timber.e(it) }
})
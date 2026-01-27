package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.transfers.active.UpdateActiveTransfersAndCleanGroupsUseCase
import timber.log.Timber
import javax.inject.Inject

class CleanTransferGroupsInitialiser @Inject constructor(
    private val updateActiveTransfersAndCleanGroupsUseCase: UpdateActiveTransfersAndCleanGroupsUseCase,
) : PostLoginInitialiser({ _, _ ->
    runCatching {
        updateActiveTransfersAndCleanGroupsUseCase()
    }.onFailure { Timber.e(it) }
})
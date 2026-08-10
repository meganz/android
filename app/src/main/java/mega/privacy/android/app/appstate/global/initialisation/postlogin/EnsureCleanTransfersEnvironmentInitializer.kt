package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.usecase.transfers.completed.DeleteAllCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.errorstatus.ClearTransferErrorStatusUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Post login initializer that ensures a clean transfers environment for the logged in account by
 * clearing state left over from a previous account: completed and failed transfers and the transfer
 * error indicator. Only runs on a genuine credentialed login (not a session resume), where no
 * transfers of the new account exist yet, so it cannot race with the transfers worker.
 */
class EnsureCleanTransfersEnvironmentInitializer @Inject constructor(
    private val deleteAllCompletedTransfersUseCase: DeleteAllCompletedTransfersUseCase,
    private val clearTransferErrorStatusUseCase: ClearTransferErrorStatusUseCase,
) : PostLoginInitialiserAction({ _, isFastLogin ->
    if (!isFastLogin) {
        runCatching {
            deleteAllCompletedTransfersUseCase()
            clearTransferErrorStatusUseCase()
        }.onFailure { Timber.e(it) }
    }
})

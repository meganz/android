package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.usecase.offline.StartOfflineSyncWorkerUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import timber.log.Timber
import javax.inject.Inject

/**
 * Post login initialiser that starts the offline sync worker
 */
class OfflineSyncPostLoginInitialiser @Inject constructor(
    private val startOfflineSyncWorkerUseCase: StartOfflineSyncWorkerUseCase,
) : PostLoginInitialiser(
    action = { _, _ ->
        runCatching {
            startOfflineSyncWorkerUseCase()
            Timber.d("Offline sync worker started successfully")
        }.onFailure { exception ->
            Timber.e(exception, "Error starting offline sync worker")
        }
    }
)

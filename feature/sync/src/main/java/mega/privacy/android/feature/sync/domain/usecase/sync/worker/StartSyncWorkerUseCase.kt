package mega.privacy.android.feature.sync.domain.usecase.sync.worker

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case to start the sync worker which will sync the folders when the app is closed
 */
class StartSyncWorkerUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke() {
        syncRepository.startSyncWorker()
    }
}
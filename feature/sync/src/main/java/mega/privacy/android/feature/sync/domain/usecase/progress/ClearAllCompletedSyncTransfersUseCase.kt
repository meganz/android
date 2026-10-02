package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/** Clears every completed sync transfer. */
internal class ClearAllCompletedSyncTransfersUseCase @Inject constructor(
    private val repository: SyncTransferStoreRepository,
) {
    suspend operator fun invoke() = repository.clearAllCompleted()
}

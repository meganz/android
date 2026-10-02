package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/** Starts a new store generation, so the sync transfers are read again from an empty state. */
internal class ResetSyncTransferStoreUseCase @Inject constructor(
    private val repository: SyncTransferStoreRepository,
) {
    suspend operator fun invoke() = repository.reset()
}

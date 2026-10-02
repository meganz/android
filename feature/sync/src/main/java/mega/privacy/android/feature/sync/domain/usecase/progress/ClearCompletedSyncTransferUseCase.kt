package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/** Clears one completed sync transfer by its tag. */
internal class ClearCompletedSyncTransferUseCase @Inject constructor(
    private val repository: SyncTransferStoreRepository,
) {
    suspend operator fun invoke(tag: Int) = repository.clearCompleted(setOf(tag))
}

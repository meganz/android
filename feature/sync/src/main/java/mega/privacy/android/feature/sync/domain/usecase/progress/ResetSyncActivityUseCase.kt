package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import javax.inject.Inject

/** Returns the sync activity to its initial state; it never starts the activity collection. */
internal class ResetSyncActivityUseCase @Inject constructor(
    private val syncProgressRepository: SyncProgressRepository,
) {
    suspend operator fun invoke() = syncProgressRepository.resetSyncActivity()
}

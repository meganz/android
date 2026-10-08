package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import javax.inject.Inject

/** Returns the sync run snapshot to its initial state; it never starts the snapshot collection. */
internal class ResetSyncRunSnapshotUseCase @Inject constructor(
    private val syncProgressRepository: SyncProgressRepository,
) {
    suspend operator fun invoke() = syncProgressRepository.resetSyncRunSnapshot()
}

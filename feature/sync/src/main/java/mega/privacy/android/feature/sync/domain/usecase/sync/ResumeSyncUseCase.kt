package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case to resume a sync by sync id
 */
class ResumeSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(syncId: Long) {
        syncRepository.resumeSync(syncId)
    }
}
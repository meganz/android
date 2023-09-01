package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

internal class ResumeSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(syncId: Long) {
        syncRepository.resumeSync(syncId)
    }
}
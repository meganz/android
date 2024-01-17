package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

internal class RefreshSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke() {
        syncRepository.refreshSync()
    }
}
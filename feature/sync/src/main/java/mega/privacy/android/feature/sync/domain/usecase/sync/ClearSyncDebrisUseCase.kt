package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import javax.inject.Inject

/**
 * Clear .debris (local cache) for all syncs
 *
 */
internal class ClearSyncDebrisUseCase @Inject constructor(
    private val syncDebrisRepository: SyncDebrisRepository
) {

    suspend operator fun invoke() {
        syncDebrisRepository.clear()
    }
}
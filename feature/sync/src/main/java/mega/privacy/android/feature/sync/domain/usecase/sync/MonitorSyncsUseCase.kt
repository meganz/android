package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case for monitoring syncs
 */
class MonitorSyncsUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {


    /**
     * Invoke.
     *
     * @return A [Flow] that emits the syncs
     */
    operator fun invoke(): Flow<List<FolderPair>> = syncRepository.monitorFolderPairChanges()

}

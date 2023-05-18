package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case for monitoring syncs
 */
internal class MonitorSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    operator fun invoke(): Flow<FolderPair> = syncRepository.monitorSync()
}

package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case for monitoring syncs
 */
internal class MonitorSyncsUseCase @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(): Flow<List<FolderPair>> =
        merge(
            flow { emit(getFolderPairsUseCase()) },
            syncRepository
                .monitorSyncChanges()
                .conflate()
                .map { getFolderPairsUseCase() }
        )
}

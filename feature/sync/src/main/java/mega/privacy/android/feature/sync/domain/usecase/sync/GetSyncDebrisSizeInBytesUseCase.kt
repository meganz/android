package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import javax.inject.Inject

/**
 * Use case for getting the total size of all sync debris
 */
internal class GetSyncDebrisSizeInBytesUseCase @Inject constructor(
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val syncDebrisRepository: SyncDebrisRepository,
) {

    /**
     * Invoke.
     *
     * @return The total size of all sync debris in bytes
     */
    suspend operator fun invoke(): Long =
        monitorSyncsUseCase()
            .firstOrNull()
            ?.let { syncs ->
                syncDebrisRepository.getSyncDebrisForSyncs(syncs)
            }?.let { debris ->
                debris.sumOf { it.sizeInBytes }
            } ?: 0
}
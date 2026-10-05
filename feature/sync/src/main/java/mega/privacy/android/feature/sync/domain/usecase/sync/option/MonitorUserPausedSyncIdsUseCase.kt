package mega.privacy.android.feature.sync.domain.usecase.sync.option

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/** Emits the ids of the syncs the user paused, again on every change. */
internal class MonitorUserPausedSyncIdsUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {
    operator fun invoke(): Flow<Set<Long>> = syncPreferencesRepository.monitorUserPausedSyncIds()
}

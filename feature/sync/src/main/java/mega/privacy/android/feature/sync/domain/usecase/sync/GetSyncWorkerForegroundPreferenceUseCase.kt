package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case to get the preference for the sync worker to run in the foreground
 */
class GetSyncWorkerForegroundPreferenceUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {
    /**
     * Gets the preference for the sync worker to run in the foreground
     * @return true if the sync worker should run in the foreground
     */
    suspend operator fun invoke(): Boolean = syncPreferencesRepository.getShouldRunForeground()
}

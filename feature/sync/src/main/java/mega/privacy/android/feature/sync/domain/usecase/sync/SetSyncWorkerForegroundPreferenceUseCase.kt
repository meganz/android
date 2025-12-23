package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case to set the preference for the sync worker to run in the foreground
 */
class SetSyncWorkerForegroundPreferenceUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {
    /**
     * Sets the preference for the sync worker to run in the foreground
     * @param shouldRun true if the sync worker should run in the foreground
     */
    suspend operator fun invoke(shouldRun: Boolean) {
        syncPreferencesRepository.setShouldRunForeground(shouldRun)
    }
}


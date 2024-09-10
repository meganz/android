package mega.privacy.android.feature.sync.domain.usecase.sync.worker

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case to set the frequency with which the periodic sync worker should work
 */
class SetSyncFrequencyUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    /**
     * Set the frequency with which the periodic sync worker should work
     */
    suspend operator fun invoke(syncFrequencyInMinutes: Int) {
        syncPreferencesRepository.setSyncFrequencyInMinutes(syncFrequencyInMinutes)
    }
}
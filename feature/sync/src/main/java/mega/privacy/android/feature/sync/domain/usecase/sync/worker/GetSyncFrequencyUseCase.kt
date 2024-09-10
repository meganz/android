package mega.privacy.android.feature.sync.domain.usecase.sync.worker

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case to get the frequency with which the periodic sync worker should work
 */
class GetSyncFrequencyUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    /**
     * @return frequency in minutes
     */
    suspend operator fun invoke(): Int = syncPreferencesRepository.getSyncFrequencyMinutes()
}
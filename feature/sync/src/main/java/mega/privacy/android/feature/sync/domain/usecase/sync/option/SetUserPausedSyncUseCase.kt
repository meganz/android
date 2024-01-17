package mega.privacy.android.feature.sync.domain.usecase.sync.option

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

internal class SetUserPausedSyncUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    suspend operator fun invoke(syncId: Long, paused: Boolean) {
        if (paused) {
            syncPreferencesRepository.setUserPausedSync(syncId)
        } else {
            syncPreferencesRepository.deleteUserPausedSync(syncId)
        }
    }
}
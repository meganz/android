package mega.privacy.android.feature.sync.domain.usecase.sync.option

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case to check if a sync is paused by the user by sync id
 */
class IsSyncPausedByTheUserUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    suspend operator fun invoke(syncId: Long): Boolean =
        syncPreferencesRepository.isSyncPausedByTheUser(syncId)
}
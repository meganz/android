package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncPromotionPreferencesRepository
import javax.inject.Inject

/**
 * Use case to indicate that Sync Promotion has been shown
 * It updates the values of preferences for display control
 *
 * @param syncPromotionPreferencesRepository    [SyncPromotionPreferencesRepository]
 */
class SetSyncPromotionShownUseCase @Inject constructor(
    private val syncPromotionPreferencesRepository: SyncPromotionPreferencesRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        syncPromotionPreferencesRepository.increaseNumberOfTimesShown(
            currentTimestamp = System.currentTimeMillis()
        )
    }
}
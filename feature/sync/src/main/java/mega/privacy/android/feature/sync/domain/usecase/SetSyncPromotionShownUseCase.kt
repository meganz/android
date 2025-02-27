package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.data.repository.SyncPromotionPreferencesRepositoryImpl.Companion.MAX_NUMBER_OF_TIMES
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
     * Use case to indicate that Sync Promotion has been shown
     * It updates the values of preferences for display control
     *
     * @param doNotShowAgain True to do not show it again or False otherwise
     */
    suspend operator fun invoke(doNotShowAgain: Boolean) {
        if (doNotShowAgain) {
            syncPromotionPreferencesRepository.setNumberOfTimesShown(MAX_NUMBER_OF_TIMES)
            syncPromotionPreferencesRepository.setLastShownTimestamp(timestamp = System.currentTimeMillis())
        } else {
            syncPromotionPreferencesRepository.increaseNumberOfTimesShown(
                currentTimestamp = System.currentTimeMillis()
            )
        }
    }
}
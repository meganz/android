package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncPromotionPreferencesRepository
import mega.privacy.android.shared.sync.domain.IsSyncFeatureEnabledUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case to check if should show Sync Promotion
 *
 * @param syncPromotionPreferencesRepository    [SyncPromotionPreferencesRepository]
 * @param isSyncFeatureEnabledUseCase usecase for checking if sync feature is enabled
 */
class ShouldShowSyncPromotionUseCase @Inject constructor(
    private val syncPromotionPreferencesRepository: SyncPromotionPreferencesRepository,
    private val isSyncFeatureEnabledUseCase: IsSyncFeatureEnabledUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean =
        isSyncFeatureEnabledUseCase() &&
                isNumberOfTimesConditionValid(syncPromotionPreferencesRepository.getNumberOfTimesShown()) &&
                isLastShownTimestampConditionValid(syncPromotionPreferencesRepository.getLastShownTimestamp())

    private fun isLastShownTimestampConditionValid(lastShownTimestamp: Long) =
        getDaysSinceLastShown(lastShownTimestamp) > TWO_WEEKS_IN_DAYS

    private fun isNumberOfTimesConditionValid(numberOfTimes: Int) =
        numberOfTimes < MAX_NUMBER_OF_TIMES

    private fun getDaysSinceLastShown(lastShownTimestamp: Long) =
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastShownTimestamp)

    companion object {
        private const val MAX_NUMBER_OF_TIMES = 6
        private const val TWO_WEEKS_IN_DAYS = 14
    }
}
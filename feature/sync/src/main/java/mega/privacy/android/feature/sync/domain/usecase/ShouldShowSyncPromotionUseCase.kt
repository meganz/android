package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.data.repository.SyncPromotionPreferencesRepositoryImpl.Companion.MAX_NUMBER_OF_TIMES
import mega.privacy.android.feature.sync.data.repository.SyncPromotionPreferencesRepositoryImpl.Companion.TWO_WEEKS_IN_DAYS
import mega.privacy.android.feature.sync.domain.repository.SyncPromotionPreferencesRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case to check if should show Sync Promotion
 *
 * @param syncPromotionPreferencesRepository    [SyncPromotionPreferencesRepository]
 */
class ShouldShowSyncPromotionUseCase @Inject constructor(
    private val syncPromotionPreferencesRepository: SyncPromotionPreferencesRepository,
    private val syncRepository: SyncRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean = !hasSyncs() &&
            isNumberOfTimesConditionValid(syncPromotionPreferencesRepository.getNumberOfTimesShown()) &&
            isLastShownTimestampConditionValid(syncPromotionPreferencesRepository.getLastShownTimestamp())

    private fun isLastShownTimestampConditionValid(lastShownTimestamp: Long) =
        getDaysSinceLastShown(lastShownTimestamp) > TWO_WEEKS_IN_DAYS

    private fun isNumberOfTimesConditionValid(numberOfTimes: Int) =
        numberOfTimes < MAX_NUMBER_OF_TIMES

    private fun getDaysSinceLastShown(lastShownTimestamp: Long) =
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastShownTimestamp)

    private suspend fun hasSyncs() = syncRepository.getFolderPairs().isNotEmpty()
}
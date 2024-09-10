package mega.privacy.android.feature.sync.domain.repository

/**
 * Repository for storing preferences regarding Sync promotion
 */
interface SyncPromotionPreferencesRepository {

    /**
     * Gets the timestamp of the last time the Sync Promotion was shown
     */
    suspend fun getLastShownTimestamp(): Long

    /**
     * Sets the timestamp of the last time the Sync Promotion was shown
     */
    suspend fun setLastShownTimestamp(timestamp: Long)

    /**
     * Gets the number of times the Sync Promotion was shown
     */
    suspend fun getNumberOfTimesShown(): Int

    /**
     * Sets the number of times the Sync Promotion was shown
     */
    suspend fun setNumberOfTimesShown(numberOfTimes: Int)

    /**
     * Increases the number of times the Sync Promotion was shown and
     * sets the timestamp of the last time the Sync Promotion was shown
     */
    suspend fun increaseNumberOfTimesShown(currentTimestamp: Long)
}
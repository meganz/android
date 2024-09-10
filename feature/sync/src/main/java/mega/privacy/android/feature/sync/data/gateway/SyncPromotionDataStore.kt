package mega.privacy.android.feature.sync.data.gateway

internal interface SyncPromotionDataStore {

    suspend fun getLastShownTimestamp(): Long

    suspend fun setLastShownTimestamp(timestamp: Long)

    suspend fun getNumberOfTimesShown(): Int

    suspend fun setNumberOfTimesShown(numberOfTimes: Int)

    suspend fun increaseNumberOfTimesShown(currentTimestamp: Long)
}
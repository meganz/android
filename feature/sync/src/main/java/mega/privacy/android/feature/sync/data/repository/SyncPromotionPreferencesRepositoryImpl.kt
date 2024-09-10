package mega.privacy.android.feature.sync.data.repository

import mega.privacy.android.feature.sync.data.gateway.SyncPromotionDataStore
import mega.privacy.android.feature.sync.domain.repository.SyncPromotionPreferencesRepository
import javax.inject.Inject

internal class SyncPromotionPreferencesRepositoryImpl @Inject constructor(
    private val syncPromotionDataStore: SyncPromotionDataStore,
) : SyncPromotionPreferencesRepository {

    override suspend fun getLastShownTimestamp(): Long =
        syncPromotionDataStore.getLastShownTimestamp()

    override suspend fun setLastShownTimestamp(timestamp: Long) {
        syncPromotionDataStore.setLastShownTimestamp(timestamp)
    }

    override suspend fun getNumberOfTimesShown(): Int =
        syncPromotionDataStore.getNumberOfTimesShown()

    override suspend fun setNumberOfTimesShown(numberOfTimes: Int) {
        syncPromotionDataStore.setNumberOfTimesShown(numberOfTimes)
    }

    override suspend fun increaseNumberOfTimesShown(currentTimestamp: Long) {
        syncPromotionDataStore.increaseNumberOfTimesShown(currentTimestamp)
    }
}
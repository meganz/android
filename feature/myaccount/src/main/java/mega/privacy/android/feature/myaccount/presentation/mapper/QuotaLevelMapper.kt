package mega.privacy.android.feature.myaccount.presentation.mapper

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import javax.inject.Inject

/**
 * Mapper to calculate quota level based on storage percentage and storage state
 */
class QuotaLevelMapper @Inject constructor() {

    /**
     * Calculate quota level based on storage percentage and storage state
     *
     * @param usedPercentage The percentage of storage used
     * @param storageState The current storage state
     * @return The appropriate quota level
     */
    operator fun invoke(
        usedPercentage: Int,
        storageState: StorageState?,
    ): QuotaLevel {
        return when {
            storageState == StorageState.Red || usedPercentage >= 90 -> QuotaLevel.Error
            storageState == StorageState.Orange || usedPercentage >= 80 -> QuotaLevel.Warning
            else -> QuotaLevel.Success
        }
    }
}

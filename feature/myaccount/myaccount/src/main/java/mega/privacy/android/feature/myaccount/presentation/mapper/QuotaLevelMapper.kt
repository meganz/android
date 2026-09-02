package mega.privacy.android.feature.myaccount.presentation.mapper

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import javax.inject.Inject

/**
 * Mapper to calculate quota level based on storage state
 */
class QuotaLevelMapper @Inject constructor() {

    /**
     * Calculate quota level based on storage state
     *
     * @param storageState The current storage state
     * @return The appropriate quota level
     */
    operator fun invoke(
        storageState: StorageState?,
    ): QuotaLevel {
        return when (storageState) {
            StorageState.Red -> QuotaLevel.Error
            StorageState.Orange -> QuotaLevel.Warning
            else -> QuotaLevel.Success
        }
    }
}

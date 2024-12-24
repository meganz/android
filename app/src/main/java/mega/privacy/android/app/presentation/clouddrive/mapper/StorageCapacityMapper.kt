package mega.privacy.android.app.presentation.clouddrive.mapper

import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity.ALMOST_FULL
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity.DEFAULT
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity.FULL
import mega.privacy.android.domain.entity.StorageState
import javax.inject.Inject

/**
 * Mapper to map the [StorageState] to [StorageOverQuotaCapacity]
 */
class StorageCapacityMapper @Inject constructor() {

    /**
     * Invocation method
     * Map the [StorageState] to [StorageOverQuotaCapacity]
     * @param storageState The [StorageState] object
     * @param shouldShow true if the banner should be shown
     * @return The mapped [StorageOverQuotaCapacity]
     */
    operator fun invoke(
        storageState: StorageState,
        shouldShow: Boolean,
    ): StorageOverQuotaCapacity = when (storageState) {
        StorageState.Red -> FULL
        StorageState.Orange -> if (shouldShow) ALMOST_FULL else DEFAULT
        else -> DEFAULT
    }
}
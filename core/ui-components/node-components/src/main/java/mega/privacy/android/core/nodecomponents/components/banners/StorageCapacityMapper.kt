package mega.privacy.android.core.nodecomponents.components.banners

import mega.privacy.android.domain.entity.StorageState
import javax.inject.Inject

/**
 * Mapper to map the [mega.privacy.android.domain.entity.StorageState] to [StorageOverQuotaCapacity]
 */
class StorageCapacityMapper @Inject constructor() {

    /**
     * Invocation method
     * Map the [mega.privacy.android.domain.entity.StorageState] to [StorageOverQuotaCapacity]
     * @param storageState The [mega.privacy.android.domain.entity.StorageState] object
     * @param shouldShow true if the banner should be shown
     * @return The mapped [StorageOverQuotaCapacity]
     */
    operator fun invoke(
        storageState: StorageState,
        shouldShow: Boolean,
    ): StorageOverQuotaCapacity = when (storageState) {
        StorageState.Red -> StorageOverQuotaCapacity.FULL
        StorageState.Orange -> if (shouldShow) StorageOverQuotaCapacity.ALMOST_FULL else StorageOverQuotaCapacity.DEFAULT
        else -> StorageOverQuotaCapacity.DEFAULT
    }
}
package mega.privacy.android.shared.account.overquota

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
        StorageState.Red, StorageState.PayWall -> StorageOverQuotaCapacity.Full
        StorageState.Orange -> if (shouldShow) StorageOverQuotaCapacity.AlmostFull else StorageOverQuotaCapacity.Default
        else -> StorageOverQuotaCapacity.Default
    }
}

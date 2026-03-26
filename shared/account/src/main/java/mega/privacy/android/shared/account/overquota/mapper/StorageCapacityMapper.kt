package mega.privacy.android.shared.account.overquota.mapper

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.account.overquota.model.StorageOverQuotaCapacity
import javax.inject.Inject

/**
 * Mapper to map the [mega.privacy.android.domain.entity.StorageState] to [mega.privacy.android.shared.account.overquota.model.StorageOverQuotaCapacity]
 */
class StorageCapacityMapper @Inject constructor() {

    /**
     * Invocation method
     * Map the [mega.privacy.android.domain.entity.StorageState] to [mega.privacy.android.shared.account.overquota.model.StorageOverQuotaCapacity]
     * @param storageState The [mega.privacy.android.domain.entity.StorageState] object
     * @param shouldShow true if the banner should be shown
     * @return The mapped [mega.privacy.android.shared.account.overquota.model.StorageOverQuotaCapacity]
     */
    operator fun invoke(
        storageState: StorageState,
        shouldShow: Boolean,
    ): StorageOverQuotaCapacity = when (storageState) {
        StorageState.Red, StorageState.PayWall -> StorageOverQuotaCapacity.FULL
        StorageState.Orange -> if (shouldShow) StorageOverQuotaCapacity.ALMOST_FULL else StorageOverQuotaCapacity.DEFAULT
        else -> StorageOverQuotaCapacity.DEFAULT
    }
}
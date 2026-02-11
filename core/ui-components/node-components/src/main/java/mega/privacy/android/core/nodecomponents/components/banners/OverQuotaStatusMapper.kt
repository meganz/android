package mega.privacy.android.core.nodecomponents.components.banners

import mega.privacy.android.domain.entity.StorageState
import javax.inject.Inject

/**
 * Mapper to map the [mega.privacy.android.domain.entity.StorageState] to [OverQuotaIssue]
 */
class OverQuotaStatusMapper @Inject constructor() {

    /**
     * Invocation method
     * Map the [mega.privacy.android.domain.entity.StorageState] to [OverQuotaIssue]
     * @param storageState The [mega.privacy.android.domain.entity.StorageState] object
     * @param shouldShowWarning true if the banner should be shown
     * @return The mapped [OverQuotaIssue]
     */
    operator fun invoke(
        storageState: StorageState,
        transferOverQuota: Boolean,
        freeAccount: Boolean,
    ): OverQuotaStatus = OverQuotaStatus(
        storage = when (storageState) {
            StorageState.Red, StorageState.PayWall -> OverQuotaIssue.Storage.Full
            StorageState.Orange -> OverQuotaIssue.Storage.AlmostFull
            else -> OverQuotaIssue.Storage.None
        },
        transfer = when {
            transferOverQuota && !freeAccount -> OverQuotaIssue.Transfer.TransferOverQuota
            transferOverQuota && freeAccount -> OverQuotaIssue.Transfer.TransferOverQuotaFreeUser
            else -> OverQuotaIssue.Transfer.None
        }
    )
}

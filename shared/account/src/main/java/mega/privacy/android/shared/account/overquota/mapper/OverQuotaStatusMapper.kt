package mega.privacy.android.shared.account.overquota.mapper

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.account.overquota.model.OverQuotaIssue
import mega.privacy.android.shared.account.overquota.model.OverQuotaStatus
import javax.inject.Inject

/**
 * Mapper to map the [mega.privacy.android.domain.entity.StorageState] to [mega.privacy.android.shared.account.overquota.model.OverQuotaIssue]
 */
class OverQuotaStatusMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param storageState
     * @param transferOverQuota
     * @param freeAccount
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
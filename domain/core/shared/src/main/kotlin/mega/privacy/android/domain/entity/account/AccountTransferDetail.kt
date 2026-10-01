package mega.privacy.android.domain.entity.account

/**
 * Account transfer detail
 *
 * @property totalTransfer
 * @property usedTransfer
 * @property usedTransferPercentage
 */
data class AccountTransferDetail(
    val totalTransfer: Long,
    val usedTransfer: Long,
    val usedTransferPercentage: Int,
)
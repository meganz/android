package mega.privacy.android.domain.entity.account

/**
 * Account transfer detail
 *
 * @property totalTransfer
 * @property usedTransfer
 */
data class AccountTransferDetail(
    val totalTransfer: Long,
    val usedTransfer: Long,
) {
    /**
     * Used transfer percentage
     */
    val usedTransferPercentage: Int
        get() = if (totalTransfer > 0) (100 * usedTransfer / totalTransfer).toInt() else 0
}
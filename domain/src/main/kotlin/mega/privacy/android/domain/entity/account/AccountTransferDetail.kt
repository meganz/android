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
        get() = if (totalTransfer > 0) {
            ((usedTransfer.toDouble() / totalTransfer.toDouble()) * 100).toInt()
        } else 0
}
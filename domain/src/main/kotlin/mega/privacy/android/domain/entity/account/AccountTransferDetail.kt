package mega.privacy.android.domain.entity.account

import kotlin.math.roundToInt

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
            ((usedTransfer.toDouble() / totalTransfer.toDouble()) * 100).roundToInt()
        } else 0
}
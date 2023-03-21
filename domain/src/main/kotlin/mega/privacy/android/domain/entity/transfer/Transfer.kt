package mega.privacy.android.domain.entity.transfer

import java.math.BigInteger

/**
 * Transfer
 * a mapper model of MegaTransfer
 *
 * @property totalBytes
 * @property transferredBytes
 * @property transferState
 * @property tag
 * @property transferType
 * @property isFinished
 * @property fileName
 * @property handle
 * @property speed
 * @property isStreamingTransfer
 * @property notificationNumber
 * @property isFolderTransfer
 * @property priority
 * @property appData
 */
data class Transfer(
    val totalBytes: Long,
    val transferredBytes: Long,
    val transferState: TransferState,
    val tag: Int,
    val transferType: TransferType,
    val isFinished: Boolean,
    val fileName: String,
    val handle: Long,
    val speed: Long,
    val isStreamingTransfer: Boolean,
    val notificationNumber: Long,
    val isFolderTransfer: Boolean,
    val priority: BigInteger,
    val appData: String,
) {
    /**
     * Is background transfer
     *
     * @return
     */
    fun isBackgroundTransfer(): Boolean = appData.contains(APP_DATA_BACKGROUND_TRANSFER)

    companion object {
        /**
         * App Data Background Transfer
         */
        const val APP_DATA_BACKGROUND_TRANSFER = "BACKGROUND_TRANSFER"
    }
}

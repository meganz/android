package mega.privacy.android.domain.entity.transfer

/**
 * Entity defining the state of the transfers finished.
 *
 * @property type                     [TransferFinishType]
 * @property nodeName                 Node name.
 * @property nodeId                   Node handle.
 * @property nodeLocalPath            Node local path.
 * @property numberFiles              Number of transferred files.
 * @property isOpenWith               True if should open with other apps, false otherwise.
 * @property chatId                   Chat id.
 */
data class TransfersFinishedState(
    val type: TransferFinishType,
    val nodeName: String? = null,
    val nodeId: Long? = null,
    val nodeLocalPath: String? = null,
    val numberFiles: Int = 1,
    val isOpenWith: Boolean = false,
    val chatId: Long? = null,
)

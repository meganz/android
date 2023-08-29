package mega.privacy.android.domain.entity

/**
 * Sd transfer
 *
 * @property tag The tag of the transfer
 * @property name The name of the transfer
 * @property size The size of the transfer
 * @property nodeHandle The node handle of the transfer
 * @property path The path of the transfer
 * @property appData The app data of the transfer
 */
data class SdTransfer(
    val tag: Int,
    val name: String,
    val size: String,
    val nodeHandle: String,
    val path: String,
    val appData: String,
)
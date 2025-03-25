package mega.privacy.android.domain.entity.file

/**
 * File storage type
 */
sealed class FileStorageType {
    /**
     * Sd card
     */
    data object SdCard : FileStorageType()

    /**
     * Internal type
     * @property deviceModel Device model
     */
    data class Internal(
        val deviceModel: String,
    ) : FileStorageType()

    /**
     * Unknown type
     */
    data object Unknown : FileStorageType()
}

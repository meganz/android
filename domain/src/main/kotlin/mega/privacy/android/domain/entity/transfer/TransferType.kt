package mega.privacy.android.domain.entity.transfer

/**
 * Transfer type
 */
enum class TransferType {
    /**
     * None
     */
    NONE,

    /**
     * Type Download refer to MegaTransfer.TYPE_DOWNLOAD
     */
    TYPE_DOWNLOAD,

    /**
     * Type Upload refer to MegaTransfer.TYPE_UPLOAD
     */
    TYPE_UPLOAD
}
package mega.privacy.android.domain.entity.transfer

/**
 * Transfer finish type.
 */
enum class TransferFinishType {
    /**
     * Type download.
     */
    DOWNLOAD,

    /**
     * Type download for offline.
     */
    DOWNLOAD_OFFLINE,

    /**
     * Type download and open for preview.
     */
    DOWNLOAD_FILE_AND_OPEN_FOR_PREVIEW,

    /**
     * Type download and open.
     */
    DOWNLOAD_AND_OPEN,

    /**
     * Type upload.
     */
    UPLOAD,

    /**
     * Type upload for chat from file explorer.
     */
    FILE_EXPLORER_CHAT_UPLOAD
}
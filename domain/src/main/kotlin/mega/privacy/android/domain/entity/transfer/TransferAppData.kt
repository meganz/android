package mega.privacy.android.domain.entity.transfer

/**
 * Data to identify different types of transfers within the app
 */
sealed interface TransferAppData {
    /**
     * Identify a camera upload transfer
     */
    data object CameraUpload : TransferAppData

    /**
     * Common interface for chat transfers app data
     */
    sealed interface ChatUploadAppData : TransferAppData

    /**
     * Identify a voice clip transfer
     */
    data object VoiceClip : ChatUploadAppData

    /**
     * Identify a chat transfer and its message
     * @param pendingMessageId the chat message Id related to this transfer
     */
    data class ChatUpload(val pendingMessageId: Long) : ChatUploadAppData


    /**
     * Indicates the transfer should be transparent for the user and should not show any notification
     */
    data object BackgroundTransfer : TransferAppData

    /**
     * Identify a download transfers that needs to be stored in the SD card.
     *
     * @param targetPathForSDK the path where the transfers will be stored by the SDK
     * @param finalTargetUri the target uri in the sd card where the file will be moved once downloaded
     * @param parentPath the path to the folder containing the destination file.
     *                   It can be only set when the transfer already exists in SDK.
     */
    data class SdCardDownload(
        val targetPathForSDK: String,
        val finalTargetUri: String,
        val parentPath: String? = null,
    ) : TransferAppData

    /**
     * Identify the original content URI of an upload transfer that needs to be copied to the cache folder for retry purposes.
     * @param originalUri
     */
    data class OriginalContentUri(val originalUri: String) : TransferAppData

    /**
     * Identify a chat download transfer.
     *
     * @param chatId the chat Id related to this transfer
     * @param msgId the message Id related to this transfer
     * @param msgIndex the index of the node in the chat message
     */
    data class ChatDownload(val chatId: Long, val msgId: Long, val msgIndex: Int) :
        TransferAppData

    /**
     * Identify the coordinates of a file upload transfer.
     * This is required in case it is necessary to add them as an attribute of the node after uploading.
     *
     * @param latitude the latitude of the geolocation.
     * @param longitude the longitude of the geolocation.
     */
    data class Geolocation(val latitude: Double, val longitude: Double) : TransferAppData

    /**
     * Identify a transfer that belongs to a group
     *
     * @param groupId the group Id related to this transfer
     */
    data class TransferGroup(val groupId: Long) : TransferAppData

    /**
     * Identify a transfer that is a download only for preview purposes.
     */
    data object PreviewDownload : TransferAppData

    /**
     * Identify a transfer that is a download to make it available offline
     */
    data object OfflineDownload : TransferAppData
}
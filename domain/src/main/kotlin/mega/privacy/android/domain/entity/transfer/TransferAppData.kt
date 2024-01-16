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
    sealed interface ChatTransferAppData : TransferAppData

    /**
     * Identify a voice clip transfer
     */
    data object VoiceClip : ChatTransferAppData

    /**
     * Identify a chat transfer and its message
     * @param pendingMessageId the chat message Id related to this transfer
     */
    data class ChatUpload(val pendingMessageId: Long) : ChatTransferAppData


    /**
     * Indicates the transfer should be transparent for the user and should not show any notification
     */
    data object BackgroundTransfer : TransferAppData

    /**
     * Identify a download transfers that needs to be stored in the SD card.
     * @param targetPath the path where the transfers needs to be stored once it's finished
     * @param targetUri the target uri in the sd card where the file will be moved once downloaded
     */
    data class SdCardDownload(val targetPath: String, val targetUri: String?) : TransferAppData

    /**
     * Identify a transfer that will be used to edit a text file
     * @param mode indicates the download save mode
     * @param fromHomePage indicates if the transfer comes from home page or not
     */
    data class TextFileUpload(val mode: Mode, val fromHomePage: Boolean) :
        TransferAppData {
        /**
         * TextFileData mode
         */
        enum class Mode {
            /**
             * The text file is created
             */
            Create,

            /**
             * The text file is edited
             */
            Edit,

            /**
             * The text file will be viewed
             */
            View,
        }
    }
}
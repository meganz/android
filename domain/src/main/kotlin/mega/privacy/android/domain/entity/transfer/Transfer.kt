package mega.privacy.android.domain.entity.transfer

import java.math.BigInteger

/**
 * Data class used as model for MegaTransfer.
 *
 * @property type [TransferType]
 * @property transferredBytes Transferred bytes during this transfer.
 * @property totalBytes Total bytes to be transferred to complete the transfer.
 * @property localPath Local path related to this transfer.
 *                      For uploads, this property is the path to the source file.
 *                      For downloads, it is the path of the destination file.
 * @property parentPath The parent path related to this transfer.
 *                       For uploads, this property is the path to the folder containing the source file.
 *                       For downloads, it is that path to the folder containing the destination file.
 * @property nodeHandle Handle related to this transfer.
 *                      For downloads, this property is the handle of the source node.
 *                      For uploads, this property is the handle of the new node in [MEGATransferDelegate onTransferFinish:transfer:error:] and [MEGADelegate onTransferFinish:transfer:error:]
 *                      when the error code is MEGAErrorTypeApiOk, otherwise the value is mega::INVALID_HANDLE.
 * @property parentHandle Handle of the parent node related to this transfer.
 *                        For downloads, this property is mega::INVALID_HANDLE.
 *                        For uploads, it is the handle of the destination node (folder) for the uploaded file.
 * @property fileName  Name of the file that is being transferred.
 *                     It's possible to upload a file with a different name ([MEGASdk startUploadWithLocalPath:parent:]).
 *                     In that case,this property is the destination name.
 * @property stage [TransferStage]
 * @property tag An integer that identifies this transfer.
 * @property speed The average speed of this transfer.
 * @property isForeignOverQuota True if the transfer has failed with MEGAErrorTypeApiEOverquota
 *                              and the target is foreign, false otherwise.
 * @property isStreamingTransfer True if this is a streaming transfer, false otherwise.
 * @property isFinished True if the transfer is at finished state (completed, cancelled or failed)
 * @property isFolderTransfer True if it's a folder transfer, false otherwise (file transfer).
 * @property appData  The application data associated with this transfer
 * @property state [TransferState]
 * @property priority Returns the priority of the transfer.
 *                    This value is intended to keep the order of the transfer queue on apps.
 * @property notificationNumber Returns the notification number of the SDK when this MEGATransfer was generated.
 */
data class Transfer(
    val type: TransferType,
    val transferredBytes: Long,
    val totalBytes: Long,
    val localPath: String,
    val parentPath: String,
    val nodeHandle: Long,
    val parentHandle: Long,
    val fileName: String,
    val stage: TransferStage,
    val tag: Int,
    val speed: Long,
    val isForeignOverQuota: Boolean,
    val isStreamingTransfer: Boolean,
    val isFinished: Boolean,
    val isFolderTransfer: Boolean,
    val appData: String,
    val state: TransferState,
    val priority: BigInteger,
    val notificationNumber: Long,
) {
    /**
     * Is voice clip.
     *
     * @return True if the transfer is a voice clip, false otherwise.
     */
    fun isVoiceClip(): Boolean = appData.contains(APP_DATA_VOICE_CLIP)

    /**
     * Is chat upload
     *
     * @return True if the transfer is a chat upload, false otherwise.
     */
    fun isChatUpload(): Boolean = appData.contains(APP_DATA_CHAT)

    /**
     * Is CU upload
     *
     * @return True if the transfer is a CU upload, false otherwise.
     */
    fun isCUUpload(): Boolean = appData.contains(APP_DATA_CU)

    /**
     * Is SD card download
     *
     * @return True if the transfer is an SD card download, false otherwise.
     */
    fun isSDCardDownload(): Boolean = appData.contains(APP_DATA_SD_CARD)

    /**
     * Is text file upload
     *
     * @return True if the transfer is a text file upload, false otherwise.
     */
    fun isTextFileUpload(): Boolean = appData.contains(APP_DATA_TXT_FILE)

    /**
     * Is background transfer
     *
     * @return True if the transfer is a background transfer, false otherwise.
     */
    fun isBackgroundTransfer(): Boolean = appData.contains(APP_DATA_BACKGROUND_TRANSFER)


    companion object {
        /**
         * App data for Voice clips.
         */
        const val APP_DATA_VOICE_CLIP = "VOICE_CLIP"

        /**
         * App data for chat uploads
         */
        const val APP_DATA_CHAT = "CHAT_UPLOAD"

        /**
         * App data for CU uploads.
         */
        const val APP_DATA_CU = "CU_UPLOAD"

        /**
         * App data for SDK card downloads.
         */
        const val APP_DATA_SD_CARD = "SD_CARD_DOWNLOAD"

        /**
         * App data for text files.
         */
        const val APP_DATA_TXT_FILE = "TXT_FILE_UPLOAD"

        /**
         * App data for indicating the data after it, is the value of a transfer parameter
         */
        const val APP_DATA_INDICATOR = ">"

        /**
         * App data for indicating the data after it, is a new transfer parameter.
         */
        const val APP_DATA_SEPARATOR = "-"

        /**
         * App data for indicating the data after it, is a new AppData due to a repeated transfer.
         */
        const val APP_DATA_REPEATED_TRANSFER_SEPARATOR = "!"

        /**
         * App Data Background Transfer
         */
        const val APP_DATA_BACKGROUND_TRANSFER = "BACKGROUND_TRANSFER"
    }
}

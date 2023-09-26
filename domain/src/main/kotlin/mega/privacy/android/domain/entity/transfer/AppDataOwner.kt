package mega.privacy.android.domain.entity.transfer

/**
 * Common interface for classes containing app data to easily share common logic
 */
interface AppDataOwner {
    /**
     * List<TransferAppData> containing all TransferAppData related to this instance
     */
    val appData: List<TransferAppData>
}

/**
 * Is voice clip.
 *
 * @return True if the transfer is a voice clip, false otherwise.
 */
fun AppDataOwner.isVoiceClip(): Boolean = appData.contains(TransferAppData.VoiceClip)

/**
 * Is SD card download
 *
 * @return True if the transfer is an SD card download, false otherwise.
 */
fun AppDataOwner.isSDCardDownload(): Boolean = appData.any { it is TransferAppData.SdCardDownload }

/**
 * Is text file upload
 *
 * @return True if the transfer is a text file upload, false otherwise.
 */
fun AppDataOwner.isTextFileUpload(): Boolean = appData.any { it is TransferAppData.TextFileUpload }

/**
 * Is background transfer
 *
 * @return True if the transfer is a background transfer, false otherwise.
 */
fun AppDataOwner.isBackgroundTransfer(): Boolean =
    appData.contains(TransferAppData.BackgroundTransfer)

/**
 * Gets the pending message id if the transfer is a chat upload. Null otherwise.
 */
fun AppDataOwner.pendingMessageId() =
    appData
        .filterIsInstance(TransferAppData.ChatUpload::class.java)
        .firstOrNull()
        ?.pendingMessageId

/**
 * Get the sdcard transfer path, if the transfer is a sdcard transfer
 *
 * @return a String representation of the transfer path, null if cannot be retrieved
 */
fun AppDataOwner.getSDCardTransferPath() = getSDCardDownloadAppData()?.targetPath

/**
 * @return TransferAppData.SdCardDownload associated to this transfer if it's a SdCard download transfer, null otherwise.
 */
fun AppDataOwner.getSDCardDownloadAppData(): TransferAppData.SdCardDownload? =
    if (isSDCardDownload()) {
        appData
            .filterIsInstance(TransferAppData.SdCardDownload::class.java)
            .firstOrNull()
    } else null

/**
 * @return TransferAppData.SdCardDownload associated to this transfer if it's a SdCard download transfer, null otherwise.
 */
fun AppDataOwner.getTextFileUploadAppData(): TransferAppData.TextFileUpload? =
    if (isTextFileUpload()) {
        appData
            .filterIsInstance(TransferAppData.TextFileUpload::class.java)
            .firstOrNull()
    } else null

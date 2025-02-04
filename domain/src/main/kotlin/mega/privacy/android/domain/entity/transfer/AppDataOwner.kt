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
 * Is background transfer
 *
 * @return True if the transfer is a background transfer, false otherwise.
 */
fun AppDataOwner.isBackgroundTransfer(): Boolean =
    appData.contains(TransferAppData.BackgroundTransfer)

/**
 * Gets the pending message id if the transfer is a chat upload. Null otherwise.
 */
@Deprecated(
    message = "This should be avoid in favor of pendingMessageIds because there can be more than one pending message for a transfer",
    replaceWith = ReplaceWith("appData.pendingMessageIds()")
)
fun AppDataOwner.pendingMessageId() =
    appData
        .filterIsInstance<TransferAppData.ChatUpload>()
        .firstOrNull()
        ?.pendingMessageId

/**
 * Gets the pending message ids if the transfer is a chat upload. Null otherwise.
 */
fun AppDataOwner.pendingMessageIds() =
    appData
        .takeIf { it.any { it is TransferAppData.ChatUpload } }
        ?.filterIsInstance<TransferAppData.ChatUpload>()
        ?.map { it.pendingMessageId }

/**
 * Get the sdcard transfer path, if the transfer is a sdcard transfer
 *
 * @return a String representation of the transfer path, null if cannot be retrieved
 */
fun AppDataOwner.getSDCardTransferPathForSDK() = getSDCardDownloadAppData()?.targetPathForSDK

/**
 * Get the sdcard transfer target uri, if the transfer is a sdcard transfer
 */
fun AppDataOwner.getSDCardFinalTransferUri() = getSDCardDownloadAppData()?.finalTargetUri

/**
 * @return TransferAppData.SdCardDownload associated to this transfer if it's a SdCard download transfer, null otherwise.
 */
fun AppDataOwner.getSDCardDownloadAppData(): TransferAppData.SdCardDownload? =
    if (isSDCardDownload()) {
        appData
            .filterIsInstance<TransferAppData.SdCardDownload>()
            .firstOrNull()
    } else null

/**
 * @return [TransferAppData.ChatDownload] associated to this transfer if it's a ChatDownload transfer, null otherwise.
 */
fun AppDataOwner.getChatDownloadAppData(): TransferAppData.ChatDownload? =
    appData
        .filterIsInstance<TransferAppData.ChatDownload>()
        .firstOrNull()

/**
 * Returns [TransferAppData.Geolocation] associated to this transfer.
 */
fun AppDataOwner.getGeolocation(): TransferAppData.Geolocation? = appData
    .filterIsInstance<TransferAppData.Geolocation>()
    .firstOrNull()

/**
 * Returns [TransferAppData.TransferGroup] associated to this transfer.
 */
fun AppDataOwner.getTransferGroup(): TransferAppData.TransferGroup? = appData
    .filterIsInstance<TransferAppData.TransferGroup>()
    .firstOrNull()

/**
 * Is preview download
 *
 * @return True if the transfer is a preview download, false otherwise.
 */
fun AppDataOwner.isPreviewDownload(): Boolean =
    appData.contains(TransferAppData.PreviewDownload)


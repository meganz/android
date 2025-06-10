package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import javax.inject.Inject

/**
 * Maps a failed [PendingTransfer] to [CompletedTransfer]
 *
 * @param deviceGateway
 * @param fileGateway
 */
class CompletedTransferPendingTransferMapper @Inject constructor(
    private val deviceGateway: DeviceGateway,
    private val fileGateway: FileGateway,
    private val stringWrapper: StringWrapper,
) {

    /**
     * Maps a failed [PendingTransfer] to [CompletedTransfer]
     *
     * @param pendingTransfer
     * @param error
     * @param sizeInBytes
     * @return a [CompletedTransfer]
     */
    suspend operator fun invoke(
        pendingTransfer: PendingTransfer,
        sizeInBytes: Long,
        error: Throwable,
    ) = CompletedTransfer(
        appData = pendingTransfer.appData,
        error = error.localizedMessage ?: error::class.simpleName,
        errorCode = null,
        fileName = pendingTransfer.fileName ?: "",
        handle = pendingTransfer.nodeIdentifier.nodeId.longValue,
        isOffline = isOffline(pendingTransfer),
        originalPath = pendingTransfer.uriPath.value,
        parentHandle = -1L,
        path = pendingTransfer.uriPath.value,
        displayPath = null,
        size = stringWrapper.getSizeString(sizeInBytes),
        timestamp = deviceGateway.now,
        state = TransferState.STATE_FAILED,
        type = pendingTransfer.transferType,
    )

    private suspend fun isOffline(pendingTransfer: PendingTransfer) =
        when (pendingTransfer.transferType) {
            TransferType.DOWNLOAD ->
                pendingTransfer.uriPath.value.let {
                    it.isNotBlank() && it.startsWith(fileGateway.getOfflineFilesRootPath())
                }

            else -> false
        }

}

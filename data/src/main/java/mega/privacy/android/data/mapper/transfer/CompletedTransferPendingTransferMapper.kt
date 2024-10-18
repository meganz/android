package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import nz.mega.sdk.MegaTransfer
import java.io.File
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
    private val transferTypeIntMapper: TransferTypeIntMapper,
    private val transferAppDataStringMapper: TransferAppDataStringMapper,
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
        appData = transferAppDataStringMapper(pendingTransfer.appData),
        error = error.localizedMessage ?: error::class.simpleName,
        fileName = pendingTransfer.path
            .split(File.separator).lastOrNull { it.isNotBlank() } ?: "",
        handle = pendingTransfer.nodeIdentifier.nodeId.longValue,
        isOffline = isOffline(pendingTransfer),
        originalPath = pendingTransfer.path,
        parentHandle = -1L,
        path = pendingTransfer.path,
        size = stringWrapper.getSizeString(sizeInBytes),
        timestamp = deviceGateway.now,
        state = MegaTransfer.STATE_FAILED,
        type = transferTypeIntMapper(pendingTransfer.transferType),
    )

    private suspend fun isOffline(pendingTransfer: PendingTransfer) =
        when (pendingTransfer.transferType) {
            TransferType.DOWNLOAD ->
                pendingTransfer.path.let {
                    it.isNotBlank() && it.startsWith(fileGateway.getOfflineFilesRootPath())
                }

            else -> false
        }

}

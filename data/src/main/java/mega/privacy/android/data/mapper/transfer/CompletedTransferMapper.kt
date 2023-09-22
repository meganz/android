package mega.privacy.android.data.mapper.transfer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * Maps a [Transfer] and [MegaException] to [CompletedTransfer]
 *
 * @param megaApiGateway
 * @param deviceGateway
 * @param fileGateway
 * @param stringWrapper
 */
class CompletedTransferMapper @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val deviceGateway: DeviceGateway,
    private val fileGateway: FileGateway,
    private val stringWrapper: StringWrapper,
    private val transferTypeIntMapper: TransferTypeIntMapper,
    private val transferStateIntMapper: TransferStateIntMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Map a pair of [Transfer] and [MegaException] to [CompletedTransfer]
     *
     * @param transfer
     * @param error
     * @return a [CompletedTransfer]
     */
    suspend operator fun invoke(transfer: Transfer, error: MegaException?) =
        withContext(ioDispatcher) {
            val isOffline = isOffline(transfer)
            CompletedTransfer(
                fileName = transfer.fileName,
                type = transferTypeIntMapper(transfer.transferType),
                state = transferStateIntMapper(transfer.state),
                size = getSizeString(transfer.totalBytes),
                handle = transfer.nodeHandle,
                isOffline = isOffline,
                path = formatTransferPath(transfer, isOffline),
                timestamp = deviceGateway.now,
                error = error?.let { getErrorString(transfer, it) },
                originalPath = transfer.localPath,
                parentHandle = transfer.parentHandle,
            )
        }

    /**
     * Get the offline value of the completed transfer
     *
     * @return true if the completed transfer is transfer to make it available offline
     */

    private suspend fun isOffline(transfer: Transfer) = when (transfer.transferType) {
        TransferType.DOWNLOAD ->
            transfer.parentPath.let {
                it.isNotBlank() && it.startsWith(fileGateway.getOfflineFilesRootPath())
            }

        else -> false
    }

    /**
     * Get the formatted transfer path of the completed transfer
     *
     * @return a formatted String representation of the transfer path
     */
    private suspend fun formatTransferPath(transfer: Transfer, isOffline: Boolean): String =
        transfer.getSDCardTransferPath()?.takeUnless { it.isBlank() }
            ?: run {
                when (transfer.transferType) {
                    TransferType.GENERAL_UPLOAD, TransferType.CAMERA_UPLOADS_UPLOAD, TransferType.CHAT_UPLOAD ->
                        formatNodePath(transfer.parentHandle)

                    TransferType.DOWNLOAD -> {
                        if (isOffline)
                            formatOfflineNodePath(transfer.parentPath, transfer.nodeHandle)
                        else
                            transfer.parentPath
                    }

                    TransferType.NONE -> ""
                }.removeSuffix(File.separator)
            }

    /**
     * Get a formatted string of the size
     *
     * @return a formatted String representation of the size
     */
    private fun getSizeString(size: Long) = stringWrapper.getSizeString(size)

    /**
     * Gets the error string to show as cause of the failure.
     *
     * @param transfer MegaTransfer to get its error.
     * @param error    MegaError of the transfer.
     * @return The error to show as cause of the failure.
     */
    private fun getErrorString(transfer: Transfer, error: MegaException): String =
        if (error is QuotaExceededMegaException && transfer.isForeignOverQuota)
            stringWrapper.getErrorStorageQuota()
        else
            stringWrapper.getErrorStringResource(error)

    /**
     * Format the path of a node.
     *
     * @param handle Handle of the Node
     * @return The path of the of the Node.
     */
    private suspend fun formatNodePath(handle: Long): String = with(megaApiGateway) {
        val node = getMegaNodeByHandle(handle) ?: return ""
        val path = getNodePath(node) ?: ""

        val rootParent = findRootParentNode(node)

        return when {
            rootParent.handle == getRootNode()?.handle ->
                stringWrapper.getCloudDriveSection() + path

            rootParent.handle == getRubbishBinNode()?.handle ->
                stringWrapper.getRubbishBinSection() +
                        path.replace("bin${File.separator}", "")

            node.isInShare ->
                stringWrapper.getTitleIncomingSharesExplorer() + File.separator +
                        path.substring(path.indexOf(":") + 1)

            else -> ""
        }.removeLastFileSeparator()
    }

    /**
     * Remove the last character of the path if it is a file separator.
     *
     * @return The path without the last item if it is a file separator.
     */
    private fun String.removeLastFileSeparator() =
        if (isNotEmpty() && last() == '/')
            substring(0, length - 1)
        else
            this

    /**
     * find the root parent Node of a Node
     *
     * @param node [MegaNode]
     * @return [MegaNode]
     */

    private suspend fun findRootParentNode(node: MegaNode): MegaNode {
        var rootParent = node
        while (true) {
            val parent = megaApiGateway.getParentNode(rootParent) ?: break
            rootParent = parent
        }
        return rootParent
    }

    /**
     * Replaces the root parent path by "Offline" in the offline path received.
     * Used to show the location of an offline node in the app.
     *
     * @param handle identifier of the offline node
     * @return The path with the root parent path replaced by "Offline".
     */
    private suspend fun formatOfflineNodePath(parentPath: String, handle: Long): String {
        val node = megaApiGateway.getMegaNodeByHandle(handle) ?: return ""
        val backupsOfflineFolder = fileGateway.getOfflineFilesBackupsRootPath()
        val offlineFolder = fileGateway.getOfflineFilesRootPath()
        val incomingOfflineFolderPath = node.let {
            val rootParent = findRootParentNode(node)
            fileGateway.getAbsolutePath(offlineFolder + File.separator + rootParent.handle)
        }
        val path = parentPath.run {
            when {
                startsWith(backupsOfflineFolder) ->
                    removePrefix(backupsOfflineFolder)

                incomingOfflineFolderPath != null && startsWith(incomingOfflineFolderPath) ->
                    removePrefix(incomingOfflineFolderPath)

                else ->
                    removePrefix(offlineFolder)
            }
        }
        return stringWrapper.getSavedForOfflineNew() + path
    }
}

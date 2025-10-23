package mega.privacy.android.data.mapper.transfer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.node.NodePathMapper
import mega.privacy.android.data.mapper.transfer.completed.API_EOVERQUOTA_FOREIGN
import mega.privacy.android.data.qualifier.DisplayPathFromUriCache
import mega.privacy.android.data.wrapper.DocumentFileWrapper
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
    private val documentFileWrapper: DocumentFileWrapper,
    private val stringWrapper: StringWrapper,
    private val nodePathMapper: NodePathMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DisplayPathFromUriCache private val displayPathFromUriCache: HashMap<String, String>,
) {

    /**
     * Map a pair of [Transfer] and [MegaException] to [CompletedTransfer]
     *
     * @param transfer
     * @param error
     * @return a [CompletedTransfer]
     */
    suspend operator fun invoke(
        transfer: Transfer,
        error: MegaException?,
    ) = withContext(ioDispatcher) {
        val isOffline = isOffline(transfer)
        val parentNode = megaApiGateway.getMegaNodeByHandle(transfer.parentHandle)
        // If the parent node is not found because it was removed, we try to get it from the transfer node handle
            ?: getParentNodeFromNodeHandle(transfer.nodeHandle)
        val transferPath = formatTransferPath(transfer, parentNode, isOffline)

        CompletedTransfer(
            fileName = transfer.fileName,
            type = transfer.transferType,
            state = transfer.state,
            size = getSizeString(transfer.totalBytes),
            handle = transfer.nodeHandle,
            isOffline = isOffline,
            path = transferPath,
            timestamp = deviceGateway.now,
            error = error?.errorString,
            errorCode = error?.let { getErrorCode(transfer, it) },
            originalPath = transfer.localPath,
            parentHandle = parentNode?.handle ?: transfer.parentHandle,
            appData = transfer.appData,
            displayPath = getDisplayPath(transfer, isOffline).takeUnless { it.isNullOrEmpty() }
                ?: transferPath,
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
    private suspend fun formatTransferPath(
        transfer: Transfer,
        parentNode: MegaNode?,
        isOffline: Boolean,
    ): String =
        when (transfer.transferType) {
            TransferType.GENERAL_UPLOAD, TransferType.CU_UPLOAD, TransferType.CHAT_UPLOAD ->
                parentNode?.let { formatParentNodePath(it) } ?: transfer.parentPath

            TransferType.DOWNLOAD -> {
                if (isOffline)
                    formatOfflineNodePath(transfer.parentPath, transfer.nodeHandle)
                else
                    transfer.parentPath
            }

            TransferType.NONE -> ""
        }.removeSuffix(File.separator)

    private suspend fun getDisplayPath(
        transfer: Transfer,
        isOffline: Boolean,
    ): String? =
        if (transfer.transferType == TransferType.DOWNLOAD && isOffline.not()) {
            displayPathFromUriCache.getOrPut(transfer.parentPath) {
                documentFileWrapper.getDocumentFile(transfer.parentPath)?.let {
                    documentFileWrapper.getAbsolutePathFromContentUri(it.uri)
                } ?: ""
            }.takeIf { it.isNotBlank() }
        } else {
            null
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
    private fun getErrorCode(transfer: Transfer, error: MegaException): Int =
        if (error is QuotaExceededMegaException && transfer.isForeignOverQuota) API_EOVERQUOTA_FOREIGN
        else error.errorCode

    /**
     * Format the path of a node.
     *
     * @param parentNode Parent node of the transfer.
     * @return The path of the of the Node.
     */
    private suspend fun formatParentNodePath(parentNode: MegaNode): String =
        with(megaApiGateway) {
            val path = getNodePath(parentNode) ?: ""
            val rootParent = findRootParentNode(parentNode)

            return nodePathMapper(
                node = parentNode,
                rootParent = rootParent,
                getRootNode = { getRootNode() },
                getRubbishBinNode = { getRubbishBinNode() },
                nodePath = path
            )
        }

    private suspend fun getParentNodeFromNodeHandle(nodeHandle: Long) = with(megaApiGateway) {
        getMegaNodeByHandle(nodeHandle)?.let { getParentNode(it) }
    }

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

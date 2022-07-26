package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.getFileName
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NullFileException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaShare
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [FilesRepository]
 *
 * @property context
 * @property megaApiGateway
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 */
class DefaultFilesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
) : FilesRepository {

    @Throws(MegaException::class)
    override suspend fun getRootFolderVersionInfo(): FolderVersionInfo =
        withContext(ioDispatcher) {
            val rootNode = megaApiGateway.getRootNode()
            suspendCoroutine { continuation ->
                megaApiGateway.getFolderInfo(rootNode,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestFolderInfoCompleted(continuation)
                    ))
            }
        }

    private fun onRequestFolderInfoCompleted(continuation: Continuation<FolderVersionInfo>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(
                    Result.success(
                        with(request.megaFolderInfo) {
                            FolderVersionInfo(
                                numVersions,
                                versionsSize
                            )
                        }
                    )
                )
            } else {
                continuation.failWithError(error)
            }
        }

    override fun monitorNodeUpdates() =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull { it.nodeList?.toList() }

    override suspend fun getRootNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()
    }

    override suspend fun getRubbishBinNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRubbishBinNode()
    }

    override suspend fun getParentNode(node: MegaNode): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getParentNode(node)
    }

    override suspend fun getChildrenNode(parentNode: MegaNode, order: Int?): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getChildrenByNode(parentNode, order)
        }

    override suspend fun getNodeByHandle(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
    }

    override suspend fun getIncomingSharesNode(order: Int?): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getIncomingSharesNode(order)
        }

    override suspend fun getOutgoingSharesNode(order: Int?): List<MegaShare> =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(order)
        }

    override suspend fun authorizeNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiFolderGateway.authorizeNode(handle)
    }

    override suspend fun getCloudSortOrder(): Int = withContext(ioDispatcher) {
        megaLocalStorageGateway.getCloudSortOrder()
    }

    override suspend fun getCameraSortOrder(): Int = withContext(ioDispatcher) {
        megaLocalStorageGateway.getCameraSortOrder()
    }

    override suspend fun getOthersSortOrder(): Int = withContext(ioDispatcher) {
        megaLocalStorageGateway.getOthersSortOrder()
    }

    override suspend fun hasInboxChildren(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.getInboxNode()?.let { megaApiGateway.hasChildren(it) } ?: false
    }

    override suspend fun downloadBackgroundFile(node: MegaNode): String =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val file = CacheFolderManager.buildTempFile(context, node.getFileName())
                if (file == null) {
                    continuation.resumeWith(Result.failure(NullFileException()))
                    return@suspendCoroutine
                }

                megaApiGateway.startDownload(
                    node = node,
                    localPath = file.absolutePath,
                    fileName = file.name,
                    appData = Constants.APP_DATA_BACKGROUND_TRANSFER,
                    startFirst = true,
                    cancelToken = null,
                    listener = OptionalMegaTransferListenerInterface(
                        onTransferTemporaryError = { _, error ->
                            continuation.failWithError(error)
                        },
                        onTransferFinish = { _, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(file.absolutePath))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
        }
}
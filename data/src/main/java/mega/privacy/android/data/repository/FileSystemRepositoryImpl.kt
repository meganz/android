package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.APP_DATA_BACKGROUND_TRANSFER
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.data.qualifier.FileVersionsOption
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.ViewerNode
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [FileSystemRepository]
 *
 * @property context
 * @property megaApiGateway
 * @property megaApiFolderGateway
 * @property megaChatApiGateway
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 * @property shareDataMapper
 * @property megaExceptionMapper
 * @property sortOrderIntMapper
 * @property cacheFolderGateway
 * @property nodeMapper
 * @property fileTypeInfoMapper
 * @property offlineNodeInformationMapper
 * @property fileGateway
 * @property chatFilesFolderUserAttributeMapper
 * @property streamingGateway
 * @property sdCardGateway
 */
internal class FileSystemRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val shareDataMapper: ShareDataMapper,
    private val megaExceptionMapper: MegaExceptionMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cacheFolderGateway: CacheFolderGateway,
    private val nodeMapper: NodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    @FileVersionsOption private val fileVersionsOptionCache: Cache<Boolean>,
    private val streamingGateway: StreamingGateway,
    private val deviceGateway: DeviceGateway,
    private val sdCardGateway: SDCardGateway,
) : FileSystemRepository {

    override val localDCIMFolderPath: String
        get() = fileGateway.localDCIMFolderPath

    override suspend fun downloadBackgroundFile(viewerNode: ViewerNode): String =
        withContext(ioDispatcher) {
            getMegaNode(viewerNode)?.let { node ->
                suspendCoroutine { continuation ->
                    val file = cacheFolderGateway.getCacheFile(
                        CacheFolderConstant.TEMPORARY_FOLDER,
                        node.getFileName()
                    )
                    if (file == null) {
                        continuation.resumeWith(Result.failure(NullFileException()))
                        return@suspendCoroutine
                    }

                    megaApiGateway.startDownload(
                        node = node,
                        localPath = file.absolutePath,
                        fileName = file.name,
                        appData = APP_DATA_BACKGROUND_TRANSFER,
                        startFirst = true,
                        cancelToken = null,
                        listener = OptionalMegaTransferListenerInterface(
                            onTransferTemporaryError = { _, error ->
                                continuation.failWithError(error, "downloadBackgroundFile")
                            },
                            onTransferFinish = { _, error ->
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(file.absolutePath))
                                } else {
                                    continuation.failWithError(error, "downloadBackgroundFile")
                                }
                            }
                        )
                    )
                }
            } ?: throw NullPointerException()
        }

    private suspend fun getMegaNode(viewerNode: ViewerNode): MegaNode? = withContext(ioDispatcher) {
        when (viewerNode) {
            is ViewerNode.ChatNode -> getMegaNodeFromChat(viewerNode)
            is ViewerNode.FileLinkNode -> MegaNode.unserialize(viewerNode.serializedNode)
            is ViewerNode.FolderLinkNode -> getMegaNodeFromFolderLink(viewerNode)
            is ViewerNode.GeneralNode -> megaApiGateway.getMegaNodeByHandle(viewerNode.id)
        }
    }

    private suspend fun getMegaNodeFromChat(chatNode: ViewerNode.ChatNode) =
        withContext(ioDispatcher) {
            with(chatNode) {
                val messageChat = megaChatApiGateway.getMessage(chatId, messageId)
                    ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId)

                if (messageChat != null) {
                    val node = messageChat.megaNodeList.get(0)
                    val chat = megaChatApiGateway.getChatRoom(chatId)

                    if (chat?.isPreview == true) {
                        megaApiGateway.authorizeChatNode(node, chat.authorizationToken)
                    } else {
                        node
                    }
                } else null
            }
        }

    private suspend fun getMegaNodeFromFolderLink(folderLinkNode: ViewerNode.FolderLinkNode) =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(folderLinkNode.id)?.let {
                megaApiFolderGateway.authorizeNode(it)
            }
        }

    override suspend fun getOfflinePath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesRootPath() }

    override suspend fun getOfflineInboxPath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesInboxRootPath() }

    override suspend fun createFolder(name: String) = withContext(ioDispatcher) {
        val megaNode = megaApiGateway.getRootNode()
        megaNode?.let { parentMegaNode ->
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("createFolder") { it.nodeHandle }
                megaApiGateway.createFolder(name, parentMegaNode, listener)
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
    }

    override suspend fun setMyChatFilesFolder(nodeHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setMyChatFilesFolder") {
                chatFilesFolderUserAttributeMapper(it.megaStringMap)?.let { value ->
                    megaApiGateway.base64ToHandle(value)
                        .takeIf { handle -> handle != megaApiGateway.getInvalidHandle() }
                }
            }
            megaApiGateway.setMyChatFilesFolder(nodeHandle, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getFileVersionsOption(forceRefresh: Boolean): Boolean =
        fileVersionsOptionCache.get()?.takeUnless { forceRefresh }
            ?: fetchFileVersionsOption().also {
                fileVersionsOptionCache.set(it)
            }

    private suspend fun fetchFileVersionsOption(): Boolean = withContext(ioDispatcher) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> continuation.resumeWith(Result.success(request.flag))
                        MegaError.API_ENOENT -> continuation.resumeWith(Result.success(true))
                        else -> continuation.failWithError(error, "fetchFileVersionsOption")
                    }
                }
            )
            megaApiGateway.getFileVersionsOption(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getLocalFile(fileNode: FileNode) =
        fileGateway.getLocalFile(
            fileName = fileNode.name,
            fileSize = fileNode.size,
            lastModifiedDate = fileNode.modificationTime,
        )

    override suspend fun getFileStreamingUri(node: Node) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(node.id.longValue)?.let {
            streamingGateway.getLocalLink(it)
        }
    }

    override suspend fun createTempFile(root: String, syncRecord: SyncRecord) =
        withContext(ioDispatcher) {
            val localPath = syncRecord.localPath
                ?: throw IllegalArgumentException("Source path doesn't exist on sync record: $syncRecord")
            val destinationPath = syncRecord.newPath
                ?: throw IllegalArgumentException("Destination path doesn't exist on sync record: $syncRecord")
            fileGateway.createTempFile(root, localPath, destinationPath)
            destinationPath
        }

    override suspend fun removeGPSCoordinates(filePath: String) = withContext(ioDispatcher) {
        fileGateway.removeGPSCoordinates(filePath)
    }

    override suspend fun getDiskSpaceBytes(path: String) = withContext(ioDispatcher) {
        deviceGateway.getDiskSpaceBytes(path)
    }

    override suspend fun deleteFile(file: File) = withContext(ioDispatcher) {
        fileGateway.deleteFile(file)
    }

    override suspend fun createDirectory(path: String) =
        withContext(ioDispatcher) {
            fileGateway.createDirectory(path)
        }

    override suspend fun deleteCameraUploadsTemporaryRootDirectory() =
        withContext(ioDispatcher + NonCancellable) {
            val cameraUploadsCacheFolder = cacheFolderGateway.getCameraUploadsCacheFolder()
            fileGateway.deleteDirectory(path = "${cameraUploadsCacheFolder.absolutePath}${File.separator}")
        }

    override val cacheDir: File
        get() = cacheFolderGateway.cacheDir

    override suspend fun getFingerprint(filePath: String) = withContext(ioDispatcher) {
        megaApiGateway.getFingerprint(filePath)
    }

    override suspend fun doesFolderExists(folderPath: String) = withContext(ioDispatcher) {
        fileGateway.isFileAvailable(folderPath)
    }

    override suspend fun isFolderInSDCardAvailable(uriString: String) = withContext(ioDispatcher) {
        val directoryFile = sdCardGateway.getDirectoryFile(uriString)
        fileGateway.isDocumentFileAvailable(directoryFile)
    }

    override suspend fun doesExternalStorageDirectoryExists() = withContext(ioDispatcher) {
        fileGateway.doesExternalStorageDirectoryExists()
    }
}

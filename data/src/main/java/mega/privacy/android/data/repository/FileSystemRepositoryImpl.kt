package mega.privacy.android.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
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
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.qualifier.FileVersionsOption
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ViewerNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer.COLLISION_CHECK_FINGERPRINT
import nz.mega.sdk.MegaTransfer.COLLISION_RESOLUTION_NEW_WITH_N
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URLConnection
import javax.inject.Inject

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
 * @property cacheGateway
 * @property nodeMapper
 * @property fileTypeInfoMapper
 * @property fileGateway
 * @property chatFilesFolderUserAttributeMapper
 * @property streamingGateway
 * @property sdCardGateway
 * @property fileAttributeGateway
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
    private val cacheGateway: CacheGateway,
    private val nodeMapper: NodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    @FileVersionsOption private val fileVersionsOptionCache: Cache<Boolean>,
    private val streamingGateway: StreamingGateway,
    private val deviceGateway: DeviceGateway,
    private val sdCardGateway: SDCardGateway,
    private val fileAttributeGateway: FileAttributeGateway,
    @ApplicationScope private val sharingScope: CoroutineScope,
) : FileSystemRepository {

    init {
        monitorChatsFilesFolderIdChanges()
    }

    private var myChatsFilesFolderIdFlow: MutableStateFlow<NodeId?> = MutableStateFlow(null)


    override val localDCIMFolderPath: String
        get() = fileGateway.localDCIMFolderPath


    @Deprecated(
        "ViewerNode should be replaced by [TypedNode], there's a similar use-case to download any type of [TypedNode] and receive a flow of the progress: StartDownloadUseCase. Please add [TransferAppData.BackgroundTransfer] to avoid this transfers to be added in the counters of the DownloadService notification",
        replaceWith = ReplaceWith("StartDownloadUseCase")
    )
    override suspend fun downloadBackgroundFile(viewerNode: ViewerNode): String =
        withContext(ioDispatcher) {
            getMegaNode(viewerNode)?.let { node ->
                val file = cacheGateway.getCacheFile(
                    CacheFolderConstant.TEMPORARY_FOLDER,
                    node.getFileName()
                ) ?: throw NullFileException()
                suspendCancellableCoroutine { continuation ->
                    val listener = OptionalMegaTransferListenerInterface(
                        onTransferFinish = { _, error ->
                            if (error.errorCode == API_OK) {
                                continuation.resumeWith(Result.success(file.absolutePath))
                            } else {
                                continuation.failWithError(error, "downloadBackgroundFile")
                            }
                        }
                    )
                    megaApiGateway.startDownload(
                        node = node,
                        localPath = file.absolutePath,
                        fileName = file.name,
                        appData = AppDataTypeConstants.BackgroundTransfer.sdkTypeValue,
                        startFirst = true,
                        cancelToken = null,
                        collisionCheck = COLLISION_CHECK_FINGERPRINT,
                        collisionResolution = COLLISION_RESOLUTION_NEW_WITH_N,
                        listener = listener
                    )
                }
            } ?: throw NullPointerException()
        }

    @Deprecated(
        message = "ViewerNode should be replaced by [TypedNode], there's a mapper to get the corresponding [MegaNode] from any [TypedNode]: MegaNodeMapper",
        replaceWith = ReplaceWith("MegaNodeMapper"),
    )
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
            megaApiFolderGateway.getMegaNodeByHandle(folderLinkNode.id)?.let {
                megaApiFolderGateway.authorizeNode(it)
            }
        }

    override suspend fun getOfflinePath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesRootPath() }

    override suspend fun getOfflineBackupsPath() =
        withContext(ioDispatcher) { fileGateway.getOfflineFilesBackupsRootPath() }

    override suspend fun setMyChatFilesFolder(nodeHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setMyChatFilesFolder") {
                myChatsFilesFolderIdFlow.value = NodeId(nodeHandle)
                chatFilesFolderUserAttributeMapper(it.megaStringMap)?.let { value ->
                    megaApiGateway.base64ToHandle(value)
                        .takeIf { handle -> handle != megaApiGateway.getInvalidHandle() }
                }
            }
            megaApiGateway.setMyChatFilesFolder(nodeHandle, listener)
        }
    }

    override suspend fun getMyChatsFilesFolderId(): NodeId? =
        myChatsFilesFolderIdFlow.value ?: run {
            getMyChatsFilesFolderIdFromGateway()
        }

    private suspend fun getMyChatsFilesFolderIdFromGateway(): NodeId? = withContext(ioDispatcher) {
        runCatching {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getMyChatFilesFolder") {
                    NodeId(it.nodeHandle)
                }
                megaApiGateway.getMyChatFilesFolder(listener)

            }
        }.getOrElse {
            //if error is API_ENOENT it means folder is not set, not an actual error. Otherwise re-throw the error
            if ((it as? MegaException)?.errorCode != API_ENOENT) {
                throw (it)
            } else {
                null
            }
        }?.also {
            myChatsFilesFolderIdFlow.value = it
        }
    }

    private fun monitorChatsFilesFolderIdChanges() {
        sharingScope.launch {
            megaApiGateway.globalUpdates
                .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
                .filter {
                    val currentUserHandle = megaApiGateway.myUser?.handle
                    it.users?.any { user ->
                        user.isOwnChange == 0
                                && user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER.toLong())
                                && user.handle == currentUserHandle
                    } == true
                }
                .catch { Timber.e(it) }
                .flowOn(ioDispatcher)
                .collect {
                    runCatching {
                        getMyChatsFilesFolderIdFromGateway()
                    }.onFailure {
                        Timber.e(it)
                    }
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
                        API_OK -> continuation.resumeWith(Result.success(request.flag))
                        API_ENOENT -> continuation.resumeWith(Result.success(false))
                        else -> continuation.failWithError(error, "fetchFileVersionsOption")
                    }
                }
            )
            megaApiGateway.getFileVersionsOption(listener)
        }
    }

    override suspend fun getLocalFile(fileNode: FileNode) = withContext(ioDispatcher) {
        fileGateway.getLocalFile(
            fileName = fileNode.name,
            fileSize = fileNode.size,
            lastModifiedDate = fileNode.modificationTime,
        )
    }

    override suspend fun getFileByPath(path: String): File? = withContext(ioDispatcher) {
        fileGateway.getFileByPath(path)
    }

    override suspend fun getFileStreamingUri(node: Node) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(node.id.longValue)?.let {
            streamingGateway.getLocalLink(it)
        }
    }

    override suspend fun createTempFile(
        rootPath: String,
        localPath: String,
        destinationPath: String,
    ) = withContext(ioDispatcher) {
        fileGateway.createTempFile(rootPath, localPath, destinationPath)
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
            val cameraUploadsCacheFolder = cacheGateway.getCameraUploadsCacheFolder()
            fileGateway.deleteDirectory(path = "${cameraUploadsCacheFolder?.absolutePath}${File.separator}")
        }

    override suspend fun createCameraUploadsTemporaryRootDirectory() =
        withContext(ioDispatcher) {
            cacheGateway.getCameraUploadsCacheFolder()
        }

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

    override suspend fun doesFileExist(path: String) = withContext(ioDispatcher) {
        File(path).exists()
    }

    override suspend fun getParent(path: String): String = withContext(ioDispatcher) {
        File(path).parent ?: path
    }

    override fun scanMediaFile(paths: Array<String>, mimeTypes: Array<String>) =
        fileGateway.scanMediaFile(paths, mimeTypes)


    override suspend fun getExternalPathByContentUri(uri: String): String? =
        withContext(ioDispatcher) {
            fileGateway.getExternalPathByContentUri(uri)
        }

    override suspend fun getGuessContentTypeFromName(localPath: String): String? =
        withContext(ioDispatcher) {
            URLConnection.guessContentTypeFromName(localPath)
        }

    override suspend fun getVideoGPSCoordinates(filePath: String): Pair<Double, Double>? =
        withContext(ioDispatcher) {
            fileAttributeGateway.getVideoGPSCoordinates(filePath)
        }

    override suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Double, Double>? =
        withContext(ioDispatcher) {
            fileAttributeGateway.getPhotoGPSCoordinates(filePath)
        }

    override suspend fun escapeFsIncompatible(fileName: String, dstPath: String): String? =
        withContext(ioDispatcher) { megaApiGateway.escapeFsIncompatible(fileName, dstPath) }

    override suspend fun setLastModified(path: String, timestamp: Long) =
        withContext(ioDispatcher) {
            fileGateway.setLastModified(path, timestamp)
        }

    override suspend fun saveTextOnContentUri(uri: String, text: String) =
        withContext(ioDispatcher) {
            fileGateway.saveTextOnContentUri(uri, text)
        }

    override suspend fun getUriForFile(file: File, authority: String): String =
        withContext(ioDispatcher) {
            runCatching { fileGateway.getUriForFile(file, authority).toString() }
                .onFailure {
                    if (it !is IllegalArgumentException) {
                        throw it
                    }
                }.getOrNull() ?: Uri.fromFile(file).toString()
        }

    override suspend fun deleteFolderAndItsFiles(path: String) {
        val file = File(path)
        fileGateway.deleteFolderAndSubFolders(file)
    }

    override suspend fun getOfflineFolder(): File =
        withContext(ioDispatcher) { fileGateway.getOfflineFolder() }

    override suspend fun getTotalSize(file: File?): Long =
        withContext(ioDispatcher) { fileGateway.getTotalSize(file) }

    override suspend fun checkFileExistsByUriPath(uriPath: String?): String? =
        withContext(ioDispatcher) {
            try {
                if (File(URI.create(uriPath).path).exists()) {
                    uriPath
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

    override suspend fun isSDCardPath(localPath: String) = withContext(ioDispatcher) {
        sdCardGateway.doesFolderExists(localPath)
    }

    override suspend fun isSDCardCachePath(localPath: String) = withContext(ioDispatcher) {
        sdCardGateway.isSDCardCachePath(localPath)
    }

    override suspend fun copyFilesToDocumentUri(
        source: File,
        destinationUri: UriPath,
    ) = withContext(ioDispatcher) {
        val uri = destinationUri.value.toUri()
        val isTreeUri = DocumentsContract.isTreeUri(uri)
        val destination = if (isTreeUri) {
            DocumentFile.fromTreeUri(context, uri)
        } else {
            DocumentFile.fromSingleUri(context, uri)
        } ?: throw FileNotFoundException("Content uri doesn't exist: $destinationUri")
        fileGateway.copyFilesToDocumentFolder(source, destination)
    }

    override suspend fun copyFiles(source: File, destination: File) = withContext(ioDispatcher) {
        fileGateway.copyFileToFolder(source, destination)
    }

    override fun getFileTypeInfoByName(name: String, duration: Int): FileTypeInfo =
        fileTypeInfoMapper(name, duration)

    override suspend fun moveFileToSd(
        file: File,
        destinationUri: String,
        subFolders: List<String>,
    ) =
        withContext(ioDispatcher) {
            val sourceDocument = DocumentFile.fromFile(file)
            val sdCardUri = Uri.parse(destinationUri)

            val destDocument = getSdDocumentFile(
                sdCardUri,
                subFolders,
                file.name,
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    ?: "application/octet-stream"
            )

            try {
                if (destDocument != null) {
                    val inputStream =
                        context.contentResolver.openInputStream(sourceDocument.uri)
                    val outputStream =
                        context.contentResolver.openOutputStream(destDocument.uri)

                    inputStream?.use { input ->
                        outputStream?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.delete()
                    return@withContext true
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return@withContext false
        }

    private fun getSdDocumentFile(
        folderUri: Uri,
        subFolders: List<String>,
        fileName: String,
        mimeType: String,
    ): DocumentFile? {
        var folderDocument = DocumentFile.fromTreeUri(context, folderUri)

        subFolders.forEach { folder ->
            folderDocument =
                folderDocument?.findFile(folder) ?: folderDocument?.createDirectory(folder)
        }
        folderDocument?.findFile(fileName)?.delete()
        return folderDocument?.createFile(mimeType, fileName)
    }

    override suspend fun createNewImageUri(fileName: String): String? = withContext(ioDispatcher) {
        fileGateway.createNewImageUri(fileName)?.toString()
    }

    override suspend fun createNewVideoUri(fileName: String): String? = withContext(ioDispatcher) {
        fileGateway.createNewVideoUri(fileName)?.toString()
    }

    override suspend fun isFileUri(uriString: String) = withContext(ioDispatcher) {
        fileGateway.isFileUri(uriString)
    }

    override suspend fun isFilePath(path: String) = withContext(ioDispatcher) {
        fileGateway.isFilePath(path)
    }

    override suspend fun getFileFromFileUri(uriString: String) = withContext(ioDispatcher) {
        fileGateway.getFileFromUriFile(uriString)
    }

    override suspend fun isContentUri(uriString: String): Boolean = withContext(ioDispatcher) {
        fileGateway.isContentUri(uriString)
    }

    override suspend fun isDocumentUri(uri: UriPath): Boolean = withContext(ioDispatcher) {
        fileGateway.isDocumentUri(uri)
    }

    override suspend fun isExternalStorageContentUri(uriString: String): Boolean =
        withContext(ioDispatcher) {
            fileGateway.isExternalStorageContentUri(uriString)
        }

    override suspend fun getFileNameFromUri(uriString: String): String? =
        withContext(ioDispatcher) {
            fileGateway.getFileNameFromUri(uriString)
        }

    override suspend fun getFileSizeFromUri(uriString: String): Long? =
        withContext(ioDispatcher) {
            fileGateway.getFileSizeFromUri(uriString)
        }

    override suspend fun copyContentUriToFile(sourceUri: UriPath, targetFile: File) {
        withContext(ioDispatcher) {
            fileGateway.copyContentUriToFile(sourceUri, targetFile)
        }
    }

    override suspend fun getFileSiblingByUri(uriString: String) =
        withContext(ioDispatcher) {
            val currentFile = uriString.toUri().toFile()
            currentFile.parentFile?.listFiles()?.toList()?.filter {
                it.isFile && it.exists() && it.canRead()
            } ?: listOf(currentFile)
        }

    override suspend fun downscaleImage(file: File, destination: File, maxPixels: Long) {
        fileGateway.downscaleImage(file, destination, maxPixels)
    }

    override suspend fun deleteVoiceClip(name: String): Boolean =
        withContext(ioDispatcher + NonCancellable) {
            cacheGateway.getVoiceClipFile(name)?.let { fileGateway.deleteFile(it) } ?: false
        }

    override suspend fun getFileTypeInfo(file: File) =
        withContext(ioDispatcher) {
            val duration = fileAttributeGateway.getVideoDuration(file.absolutePath)
            fileTypeInfoMapper(file.name, duration?.inWholeSeconds?.toInt() ?: 0)
        }

    override suspend fun deleteFileByUri(uri: String): Boolean = withContext(ioDispatcher) {
        fileGateway.deleteFileByUri(Uri.parse(uri))
    }

    override suspend fun getFilesInDocumentFolder(uri: UriPath): DocumentFolder =
        withContext(ioDispatcher) {
            fileGateway.getFilesInDocumentFolder(uri)
        }

    override fun searchFilesInDocumentFolderRecursive(
        folder: UriPath,
        query: String,
    ): Flow<DocumentFolder> = fileGateway.searchFilesInDocumentFolderRecursive(folder, query)
        .flowOn(ioDispatcher)

    override suspend fun copyUri(name: String, source: UriPath, destination: File) =
        withContext(ioDispatcher) {
            fileGateway.copyUriToDocumentFolder(
                name = name,
                source = source.value.toUri(),
                destination = DocumentFile.fromFile(destination)
            )
        }

    override suspend fun copyUri(name: String, source: UriPath, destination: UriPath) =
        withContext(ioDispatcher) {
            val uri = destination.value.toUri()
            val isTreeUri = DocumentsContract.isTreeUri(uri)
            val destinationUri = if (isTreeUri) {
                DocumentFile.fromTreeUri(context, uri)
            } else {
                DocumentFile.fromSingleUri(context, uri)
            } ?: throw FileNotFoundException("Content uri doesn't exist: $destination")
            fileGateway.copyUriToDocumentFolder(
                name = name,
                source = source.value.toUri(),
                destination = destinationUri
            )
        }

    override fun isPathInsecure(path: String): Boolean =
        fileGateway.isPathInsecure(path)

    override fun isMalformedPathFromExternalApp(action: String?, path: String): Boolean =
        fileGateway.isMalformedPathFromExternalApp(action, path)

    override suspend fun getDocumentFileName(uri: UriPath): String = withContext(ioDispatcher) {
        val rawUri = uri.value.toUri()
        val isTreeUri = DocumentsContract.isTreeUri(rawUri)
        val document = if (isTreeUri) {
            DocumentFile.fromTreeUri(context, rawUri)
        } else {
            DocumentFile.fromSingleUri(context, rawUri)
        } ?: throw FileNotFoundException("Content uri doesn't exist: $rawUri")
        document.name.orEmpty()
    }
}

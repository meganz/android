package mega.privacy.android.data.repository.photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.decodeBase64
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.extensions.getValueFor
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.data.mapper.photos.ContentConsumptionMegaStringMapMapper
import mega.privacy.android.data.mapper.photos.TimelineFilterPreferencesJSONMapper
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPhotosRepository @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val megaApiFacade: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val fileGateway: FileGateway,
    private val dateUtilFacade: DateUtilWrapper,
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val timelineFilterPreferencesJSONMapper: TimelineFilterPreferencesJSONMapper,
    private val contentConsumptionMegaStringMapMapper: ContentConsumptionMegaStringMapMapper,
    private val imageNodeMapper: ImageNodeMapper,
    private val cameraUploadsSettingsPreferenceGateway: CameraUploadsSettingsPreferenceGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
) : PhotosRepository {
    @Volatile
    private var isInitialized: Boolean = false

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    private val photosFlow: MutableStateFlow<List<Photo>?> = MutableStateFlow(null)

    private val imageNodesFlow: MutableStateFlow<List<ImageNode>?> = MutableStateFlow(null)

    private val photosCache: MutableMap<NodeId, Photo> = mutableMapOf()

    private val imageNodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    @Volatile
    private var offlineNodesCache: Map<String, Offline> = mapOf()

    private val photosDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(1)

    private val imageNodesDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(1)

    private var monitorOfflineNodesJob: Job? = null

    private var populateNodesJob: Job? = null

    private var monitorNodeUpdatesJob: Job? = null

    private val constraints: List<suspend (Node) -> Boolean> = listOf(
        ::checkMediaNode,
        ::checkCloudDriveNode,
    )

    init {
        monitorOfflineNodes()
    }

    override fun monitorPhotos(): Flow<List<Photo>> {
        initialize()
        return photosFlow.filterNotNull()
    }

    private fun initialize() {
        if (isInitialized) return
        isInitialized = true

        populateNodes()
        monitorNodeUpdates()
    }

    private fun monitorOfflineNodes() {
        monitorOfflineNodesJob?.cancel()
        monitorOfflineNodesJob = nodeRepository.monitorOfflineNodeUpdates()
            .onEach(::handleOfflineNodes)
            .launchIn(appScope)
    }

    private fun handleOfflineNodes(offlineNodes: List<Offline>) {
        try {
            offlineNodesCache = offlineNodes.associateBy { it.handle }
        } catch (_: Throwable) {
        }
    }

    private fun populateNodes() {
        populateNodesJob?.cancel()
        populateNodesJob = appScope.launch {
            val (imageNodes, videoNodes) = fetchNodes()

            updatePhotos(imageNodes, videoNodes)
            updateImageNodes(imageNodes, videoNodes)
        }
    }

    private suspend fun fetchNodes(): List<List<MegaNode>> = withContext(ioDispatcher) {
        awaitAll(
            async { fetchImageNodes() },
            async { fetchVideoNodes() },
        )
    }

    private suspend fun fetchImageNodes(): List<MegaNode> = withContext(ioDispatcher) {
        searchImages().filter { isImageNodeValid(it) }
    }

    private suspend fun fetchVideoNodes(): List<MegaNode> = withContext(ioDispatcher) {
        searchVideos().filter { isVideoNodeValid(it) }
    }

    private suspend fun isImageNodeValid(
        node: MegaNode,
        filterSvg: Boolean = true,
        includeRubbishBin: Boolean = false,
    ): Boolean {
        val fileType = fileTypeInfoMapper(node)
        return node.isFile
                && fileType is ImageFileTypeInfo
                && (fileType !is SvgFileTypeInfo || !filterSvg)
                && (!nodeRepository.isNodeInRubbish(node.handle) || includeRubbishBin)
                && node.hasThumbnail()
    }

    private suspend fun isVideoNodeValid(
        node: MegaNode,
        includeRubbishBin: Boolean = false,
    ): Boolean {
        val fileType = fileTypeInfoMapper(node)
        return node.isFile
                && fileType is VideoFileTypeInfo
                && (!nodeRepository.isNodeInRubbish(node.handle) || includeRubbishBin)
                && node.hasThumbnail()
    }

    private fun updatePhotos(
        imageNodes: List<MegaNode>,
        videoNodes: List<MegaNode>,
    ) = appScope.launch {
        val photos = awaitAll(
            async { imageNodes.map { mapMegaNodeToImage(it) } },
            async { videoNodes.map { mapMegaNodeToVideo(it) } },
        ).flatten()

        withContext(photosDispatcher) {
            photosCache.clear()
            photosCache.putAll(photos.associateBy { NodeId(it.id) })

            val newPhotos = photosCache.values.toList()
            photosFlow.update { newPhotos }
        }
    }

    private fun updateImageNodes(
        imageNodes: List<MegaNode>,
        videoNodes: List<MegaNode>,
    ) = appScope.launch {
        val nodes = (imageNodes + videoNodes).map { node ->
            imageNodeMapper(
                megaNode = node,
                hasVersion = megaApiFacade::hasVersion,
                requireSerializedData = true,
                offline = offlineNodesCache[node.handle.toString()],
            )
        }

        withContext(imageNodesDispatcher) {
            imageNodesCache.clear()
            imageNodesCache.putAll(nodes.associateBy { it.id })

            val newNodes = imageNodesCache.values.toList()
            imageNodesFlow.update { newNodes }
        }
    }

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = nodeRepository.monitorNodeUpdates()
            .onEach(::handleNodeUpdate)
            .launchIn(appScope)
    }

    private suspend fun handleNodeUpdate(nodeUpdate: NodeUpdate) {
        for (node in nodeUpdate.changes.keys) {
            val isPotentialNode = constraints.all { it(node) }

            refreshPhotos(node, isPotentialNode)
            refreshImageNodes(node, isPotentialNode)
        }

        withContext(photosDispatcher) {
            val newPhotos = photosCache.values.toList()
            photosFlow.update { newPhotos }
        }

        withContext(imageNodesDispatcher) {
            val newNodes = imageNodesCache.values.toList()
            imageNodesFlow.update { newNodes }
        }
    }

    private suspend fun refreshPhotos(
        node: Node,
        isPotentialNode: Boolean,
    ) = withContext(photosDispatcher) {
        if (!isPotentialNode) {
            photosCache.remove(node.id)
            return@withContext
        }

        val photo = getMegaNode(nodeId = node.id)?.let { megaNode ->
            if (isImageNodeValid(megaNode)) {
                mapMegaNodeToImage(megaNode)
            } else if (isVideoNodeValid(megaNode)) {
                mapMegaNodeToVideo(megaNode)
            } else {
                null
            }
        }

        if (photo == null) {
            photosCache.remove(node.id)
        } else {
            photosCache[NodeId(photo.id)] = photo
        }
    }

    private suspend fun refreshImageNodes(
        node: Node,
        isPotentialNode: Boolean,
    ) = withContext(imageNodesDispatcher) {
        if (!isPotentialNode) {
            imageNodesCache.remove(node.id)
            return@withContext
        }

        val imageNode = fetchImageNode(nodeId = node.id)
        if (imageNode == null) {
            imageNodesCache.remove(node.id)
        } else {
            imageNodesCache[imageNode.id] = imageNode
        }
    }

    override fun monitorImageNodes(): Flow<List<ImageNode>> = imageNodesFlow
        .filterNotNull()

    private suspend fun checkMediaNode(node: Node): Boolean {
        return node is FileNode && (node.type is ImageFileTypeInfo || node.type is VideoFileTypeInfo)
    }

    private suspend fun checkCloudDriveNode(node: Node): Boolean {
        return nodeRepository.isNodeInCloudDrive(handle = node.id.longValue)
    }

    private suspend fun getMegaNode(nodeId: NodeId): MegaNode? {
        return megaApiFacade.getMegaNodeByHandle(nodeHandle = nodeId.longValue)
    }

    override suspend fun getPublicLinksCount(): Int = withContext(ioDispatcher) {
        megaApiFacade.getPublicLinks().size
    }

    override suspend fun buildDefaultDownloadDir(): File = withContext(ioDispatcher) {
        fileGateway.buildDefaultDownloadDir()
    }

    override suspend fun getCameraUploadFolderId(): Long? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getCameraUploadsHandle()
    }

    override suspend fun getMediaUploadFolderId(): Long? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getMediaUploadsHandle()
    }

    override suspend fun getPhotoFromNodeID(nodeId: NodeId, albumPhotoId: AlbumPhotoId?): Photo? {
        return when (val photo = photosCache[nodeId]) {
            is Photo.Image -> {
                photo.copy(albumPhotoId = albumPhotoId?.id)
            }

            is Photo.Video -> {
                photo.copy(albumPhotoId = albumPhotoId?.id)
            }

            else -> withContext(ioDispatcher) {
                getMegaNode(nodeId)?.let { megaNode ->
                    if (isImageNodeValid(megaNode)) {
                        mapMegaNodeToImage(megaNode, albumPhotoId?.id)
                    } else if (isVideoNodeValid(megaNode)) {
                        mapMegaNodeToVideo(megaNode, albumPhotoId?.id)
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun getPhotosByFolderId(
        folderId: NodeId,
        searchString: String,
        recursive: Boolean,
    ): List<Photo> =
        withContext(ioDispatcher) {
            val parent = getMegaNode(folderId)
            parent?.let { parentNode ->
                val token = MegaCancelToken.createInstance()
                val images = async {
                    val imageNodes = megaApiFacade.searchByType(
                        parentNode = parentNode,
                        searchString = searchString,
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_PHOTO
                    )
                    mapPhotoNodesToImages(imageNodes)
                }
                val videos = async {
                    val videoNodes = megaApiFacade.searchByType(
                        parentNode = parentNode,
                        searchString = searchString,
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_VIDEO
                    )
                    mapPhotoNodesToVideos(videoNodes)
                }
                val photos = images.await() + videos.await()
                suspendCancellableCoroutine { continuation ->
                    continuation.resumeWith(Result.success(photos))
                    continuation.invokeOnCancellation {
                        token.cancel()
                    }
                }
                token.cancel()
                photos
            } ?: emptyList()
        }

    override suspend fun getPhotosByFolderIdInFolderLink(
        folderId: NodeId,
        searchString: String,
        recursive: Boolean,
    ): List<Photo> =
        withContext(ioDispatcher) {
            val parent = megaApiFolder.getMegaNodeByHandle(folderId.longValue)
            parent?.let { parentNode ->
                val token = MegaCancelToken.createInstance()
                val images = async {
                    val imageNodes = megaApiFolder.searchByType(
                        parentNode = parentNode,
                        searchString = searchString,
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_PHOTO
                    )
                    mapPhotoNodesToImages(imageNodes)
                }
                val videos = async {
                    val videoNodes = megaApiFolder.searchByType(
                        parentNode = parentNode,
                        searchString = searchString,
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_VIDEO
                    )
                    mapPhotoNodesToVideos(videoNodes)
                }
                val photos = images.await() + videos.await()
                suspendCancellableCoroutine { continuation ->
                    continuation.resumeWith(Result.success(photos))
                    continuation.invokeOnCancellation {
                        token.cancel()
                    }
                }
                token.cancel()
                photos
            } ?: emptyList()
        }

    private suspend fun searchImages(): List<MegaNode> = withContext(ioDispatcher) {
        val token = MegaCancelToken.createInstance()
        val imageNodes = megaApiFacade.searchByType(
            token,
            MegaApiAndroid.ORDER_MODIFICATION_DESC,
            MegaApiAndroid.FILE_TYPE_PHOTO,
            MegaApiAndroid.SEARCH_TARGET_ROOTNODE
        )
        suspendCancellableCoroutine { continuation ->
            continuation.resumeWith(Result.success(imageNodes))
            continuation.invokeOnCancellation {
                token.cancel()
            }
        }
        token.cancel()
        imageNodes
    }

    private suspend fun searchVideos(): List<MegaNode> = withContext(ioDispatcher) {
        val token = MegaCancelToken.createInstance()
        val videosNodes = megaApiFacade.searchByType(
            token,
            MegaApiAndroid.ORDER_MODIFICATION_DESC,
            MegaApiAndroid.FILE_TYPE_VIDEO,
            MegaApiAndroid.SEARCH_TARGET_ROOTNODE
        )
        suspendCancellableCoroutine { continuation ->
            continuation.resumeWith(Result.success(videosNodes))
            continuation.invokeOnCancellation {
                token.cancel()
            }
        }
        token.cancel()
        videosNodes
    }

    /**
     * Map megaNode to Photo.
     */
    private suspend fun mapMegaNodeToPhoto(megaNode: MegaNode, filterSvg: Boolean = true): Photo? {
        val fileType = fileTypeInfoMapper(megaNode)
        val isValid =
            megaNode.isFile
                    && (fileType is VideoFileTypeInfo
                    || (fileType is ImageFileTypeInfo && checkSvg(filterSvg, fileType))
                    && !megaApiFacade.isInRubbish(megaNode))
        if (isValid.not()) return null
        return if (fileType is ImageFileTypeInfo) {
            mapMegaNodeToImage(megaNode)
        } else {
            mapMegaNodeToVideo(megaNode)
        }
    }

    private fun checkSvg(filterSvg: Boolean, fileType: ImageFileTypeInfo): Boolean {
        return if (filterSvg) {
            fileType !is SvgFileTypeInfo
        } else {
            true
        }
    }

    /**
     * Convert the Photos MegaNode list to Image list
     * @param megaNodes List<MegaNode> of Photo
     * @return List<Photo> / Images
     */
    private suspend fun mapPhotoNodesToImages(megaNodes: List<MegaNode>): List<Photo> =
        coroutineScope {
            megaNodes.map { megaNode ->
                async {
                    runCatching {
                        (fileTypeInfoMapper(megaNode) !is SvgFileTypeInfo && megaNode.isValidPhotoNode())
                            .takeIf { it }
                            ?.let { mapMegaNodeToImage(megaNode) }
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }

    /**
     * Convert the Photos MegaNode list to Video list
     * @param megaNodes List<MegaNode> of Photo
     * @return List<Photo> / Videos
     */
    private suspend fun mapPhotoNodesToVideos(megaNodes: List<MegaNode>): List<Photo> =
        coroutineScope {
            megaNodes.map { megaNode ->
                async {
                    runCatching {
                        (fileTypeInfoMapper(megaNode) is VideoFileTypeInfo && megaNode.isValidPhotoNode())
                            .takeIf { it }
                            ?.let { mapMegaNodeToVideo(megaNode) }
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }

    /**
     * Check valid Photo Node, not include Photo nodes that are in rubbish bin or without thumbnail
     */
    private suspend fun MegaNode.isValidPhotoNode() =
        !nodeRepository.isNodeInRubbish(handle) && this.hasThumbnail()

    /**
     * Convert the MegaNode to Image
     * @param megaNode MegaNode
     * @return Photo / Image
     */
    private suspend fun mapMegaNodeToImage(megaNode: MegaNode, albumPhotoId: Long? = null) =
        imageMapper(
            megaNode.handle,
            albumPhotoId,
            megaNode.parentHandle,
            megaNode.name,
            megaNode.isFavourite,
            dateUtilFacade.fromEpoch(megaNode.creationTime),
            dateUtilFacade.fromEpoch(megaNode.modificationTime),
            getThumbnailCacheFilePath(megaNode),
            getPreviewCacheFilePath(megaNode),
            fileTypeInfoMapper(megaNode),
            megaNode.size,
            megaNode.isTakenDown,
        )

    /**
     * Convert the MegaNode to Video
     * @param megaNode MegaNode
     * @return Photo / Video
     */
    private suspend fun mapMegaNodeToVideo(megaNode: MegaNode, albumPhotoId: Long? = null) =
        videoMapper(
            megaNode.handle,
            albumPhotoId,
            megaNode.parentHandle,
            megaNode.name,
            megaNode.isFavourite,
            dateUtilFacade.fromEpoch(megaNode.creationTime),
            dateUtilFacade.fromEpoch(megaNode.modificationTime),
            getThumbnailCacheFilePath(megaNode),
            getPreviewCacheFilePath(megaNode),
            fileTypeInfoMapper(megaNode),
            megaNode.size,
            megaNode.isTakenDown,
        )

    private suspend fun getThumbnailCacheFilePath(megaNode: MegaNode): String? {
        if (thumbnailFolderPath == null) {
            thumbnailFolderPath =
                cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path
        }
        return thumbnailFolderPath?.let {
            "$it${File.separator}${megaNode.getThumbnailFileName()}"
        }
    }

    private suspend fun getPreviewCacheFilePath(megaNode: MegaNode): String? {
        if (previewFolderPath == null) {
            previewFolderPath =
                cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path
        }
        return previewFolderPath?.let {
            "$it${File.separator}${megaNode.getPreviewFileName()}"
        }
    }

    override suspend fun getPhotosByIds(ids: List<NodeId>): List<Photo> =
        withContext(ioDispatcher) {
            ids.mapNotNull { id ->
                val cache = photosCache[id]
                if (cache != null) {
                    cache
                } else {
                    val node = getMegaNode(id) ?: megaApiFolder.getMegaNodeByHandle(id.longValue)
                    node?.let { mapMegaNodeToPhoto(it, filterSvg = false) }
                }
            }
        }

    override suspend fun getChatPhotoByMessageId(
        chatId: Long,
        messageId: Long,
    ): Photo? =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(chatId)
            val chatMessage = megaChatApiGateway.getMessage(chatId, messageId)
                ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId)
            var node = chatMessage?.megaNodeList?.get(0)
            if (chatRoom?.isPreview == true && node != null) {
                node = megaApiFacade.authorizeChatNode(node, chatRoom.authorizationToken)
            }
            node?.let { mapMegaNodeToPhoto(it, filterSvg = false) }
        }

    override suspend fun getPhotoByPublicLink(link: String): Photo? =
        withContext(ioDispatcher) {
            val node = getPublicNode(link)
            node?.let { mapMegaNodeToPhoto(it, filterSvg = false) }
        }

    override suspend fun getTimelineFilterPreferences(): Map<String, String?>? =
        withContext(ioDispatcher) {
            getContentConsumptionPreferences()?.let { allPreferences ->
                val allCurrentPreferences = allPreferences.getValueFor(
                    TimelinePreferencesJSON.JSON_KEY_CONTENT_CONSUMPTION.value
                )?.decodeBase64()
                timelineFilterPreferencesJSONMapper(allCurrentPreferences)
            }
        }

    override suspend fun setTimelineFilterPreferences(preferences: Map<String, String>): String? =
        withContext(ioDispatcher) {
            val latestPreferencesStringMap = getContentConsumptionPreferences()
            val valueToPut = contentConsumptionMegaStringMapMapper(
                latestPreferencesStringMap,
                preferences,
            )

            suspendCancellableCoroutine { continuation ->
                val listener =
                    continuation.getRequestListener("setUserAttribute(MegaApiJava.USER_ATTR_CC_PREFS)") {
                        it.megaStringMap.getValueFor(
                            TimelinePreferencesJSON.JSON_KEY_CONTENT_CONSUMPTION.value
                        )
                    }

                megaApiFacade.setUserAttribute(
                    type = MegaApiJava.USER_ATTR_CC_PREFS,
                    value = valueToPut,
                    listener = listener
                )

                continuation.invokeOnCancellation {
                    megaApiFacade.removeRequestListener(listener)
                }
            }
        }

    private suspend fun getContentConsumptionPreferences() = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(request))
                        }

                        MegaError.API_ENOENT -> {
                            continuation.resumeWith(Result.success(null))
                        }

                        else -> {
                            continuation.failWithError(error, "getTimelineFilterPreferences")
                        }
                    }
                }
            )
            megaApiFacade.getUserAttribute(
                MegaApiJava.USER_ATTR_CC_PREFS,
                listener
            )

            continuation.invokeOnCancellation {
                megaApiFacade.removeRequestListener(listener)
            }
        }
        request?.let {
            request.megaStringMap
        }
    }

    private suspend fun getPublicNode(nodeFileLink: String): MegaNode? =
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            continuation.resumeWith(Result.success(request.publicNode))
                        } else {
                            continuation.resumeWithException(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        continuation.failWithException(error.toException("getPublicNode"))
                    }
                }
            )
            megaApiFacade.getPublicNode(nodeFileLink, listener)
            continuation.invokeOnCancellation {
                megaApiFacade.removeRequestListener(listener)
            }
        }

    override suspend fun fetchImageNode(
        nodeId: NodeId,
        filterSvg: Boolean,
        includeRubbishBin: Boolean,
    ): ImageNode? = withContext(ioDispatcher) {
        getMegaNode(nodeId)?.let { megaNode ->
            if (isImageNodeValid(megaNode, filterSvg, includeRubbishBin) ||
                isVideoNodeValid(megaNode, includeRubbishBin)
            ) {
                imageNodeMapper(
                    megaNode = megaNode,
                    hasVersion = megaApiFacade::hasVersion,
                    requireSerializedData = true,
                    offline = offlineNodesCache[megaNode.handle.toString()],
                )
            } else {
                null
            }
        }
    }

    override suspend fun fetchImageNode(url: String): ImageNode? {
        return getPublicNode(url)?.let { megaNode ->
            imageNodeMapper(
                megaNode = megaNode,
                hasVersion = megaApiFacade::hasVersion,
                requireSerializedData = true,
                offline = offlineNodesCache[megaNode.handle.toString()],
            )
        }
    }

    override suspend fun getImageNode(nodeId: NodeId): ImageNode? {
        return imageNodesCache[nodeId]
    }

    override suspend fun getMediaDiscoveryNodes(
        parentId: NodeId,
        recursive: Boolean,
    ): List<ImageNode> = withContext(ioDispatcher) {
        val parentNode = getMegaNode(parentId) ?: return@withContext emptyList()

        val token = MegaCancelToken.createInstance()
        val nodes = awaitAll(
            async {
                megaApiFacade.searchByType(
                    parentNode = parentNode,
                    searchString = "*",
                    cancelToken = token,
                    recursive = recursive,
                    order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                    type = MegaApiAndroid.FILE_TYPE_PHOTO,
                ).filter { isImageNodeValid(it) }
            },
            async {
                megaApiFacade.searchByType(
                    parentNode = parentNode,
                    searchString = "*",
                    cancelToken = token,
                    recursive = recursive,
                    order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                    type = MegaApiAndroid.FILE_TYPE_VIDEO,
                ).filter { isVideoNodeValid(it) }
            },
        ).flatten()

        nodes.map { megaNode ->
            imageNodeMapper(
                megaNode = megaNode,
                hasVersion = megaApiFacade::hasVersion,
                requireSerializedData = true,
                offline = offlineNodesCache[megaNode.handle.toString()],
            )
        }
    }

    override suspend fun fetchImageNodes(
        parentId: NodeId,
        order: SortOrder?,
        includeRubbishBin: Boolean,
    ): List<ImageNode> = withContext(ioDispatcher) {
        val parentNode = getMegaNode(parentId) ?: return@withContext emptyList()
        val megaNodes = megaApiFacade.getChildrenByNode(
            parentNode = parentNode,
            order = order?.let { sortOrderIntMapper(it) },
        ).filter {
            isImageNodeValid(
                node = it,
                filterSvg = false,
                includeRubbishBin = includeRubbishBin,
            ) || isVideoNodeValid(
                node = it,
                includeRubbishBin = includeRubbishBin,
            )
        }

        megaNodes.map { megaNode ->
            imageNodeMapper(
                megaNode = megaNode,
                hasVersion = megaApiFacade::hasVersion,
                requireSerializedData = true,
                offline = offlineNodesCache[megaNode.handle.toString()],
            )
        }
    }

    override fun clearCache() {
        isInitialized = false

        populateNodesJob?.cancel()
        populateNodesJob = null

        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = null

        monitorOfflineNodesJob?.cancel()
        monitorOfflineNodesJob = null

        offlineNodesCache = mapOf()
        photosCache.clear()
        imageNodesCache.clear()

        photosFlow.value = null
        imageNodesFlow.value = null
    }
}

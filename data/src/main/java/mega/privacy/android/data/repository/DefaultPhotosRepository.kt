package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
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
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.data.mapper.photos.ContentConsumptionMegaStringMapMapper
import mega.privacy.android.data.mapper.photos.TimelineFilterPreferencesJSONMapper
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
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

/**
 * Default implementation of [PhotosRepository]
 *
 * @property megaApiFacade MegaApiGateway
 * @property ioDispatcher CoroutineDispatcher
 * @property cacheGateway CacheFolderGateway
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 * @property imageMapper ImageMapper
 * @property videoMapper VideoMapper
 */
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
    private val megaLocalStorageFacade: MegaLocalStorageGateway,
    private val dateUtilFacade: DateUtilWrapper,
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val timelineFilterPreferencesJSONMapper: TimelineFilterPreferencesJSONMapper,
    private val contentConsumptionMegaStringMapMapper: ContentConsumptionMegaStringMapMapper,
    private val imageNodeMapper: ImageNodeMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway
) : PhotosRepository {
    private val photosCache: MutableMap<NodeId, Photo> = mutableMapOf()

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    private val refreshPhotosStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val photosStateFlow: MutableStateFlow<List<Photo>?> = MutableStateFlow(null)

    private val refreshImageNodesFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val imageNodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    private val photosRefreshRules = listOf(
        NodeChanges.New,
        NodeChanges.Favourite,
        NodeChanges.Attributes,
        NodeChanges.Parent,
    )

    private var monitorNodeUpdatesJob: Job? = null

    private var refreshPhotosJob: Job? = null

    @Volatile
    private var isMonitoringInitiated: Boolean = false

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = nodeRepository.monitorNodeUpdates()
            .onEach { nodeUpdate ->
                appScope.launch {
                    val nodes = nodeUpdate.changes.keys.toList()
                    nodes.forEach { photosCache.remove(it.id) }
                }

                appScope.launch {
                    val changes = nodeUpdate.changes.values
                    if (changes.flatten().intersect(photosRefreshRules).isNotEmpty()) {
                        refreshPhotos()
                    }
                }
            }.launchIn(appScope)
    }

    private fun monitorRefreshPhotos() {
        refreshPhotosJob?.cancel()
        refreshPhotosJob = refreshPhotosStateFlow
            .filter { it }
            .conflate()
            .onEach {
                val (imageNodes, videoNodes) = fetchPhotosNodes()

                updatePhotosNodes(imageNodes, videoNodes)
                updateTimelineNodes(imageNodes, videoNodes)
            }.launchIn(appScope)
    }

    private fun updatePhotosNodes(
        imageNodes: List<MegaNode>,
        videoNodes: List<MegaNode>,
    ) = appScope.launch {
        val photos = awaitAll(
            async { imageNodes.map { mapMegaNodeToImage(it) } },
            async { videoNodes.map { mapMegaNodeToVideo(it) } },
        ).flatten()

        for (photo in photos) {
            photosCache[NodeId(photo.id)] = photo
        }

        photosStateFlow.update { photos }
        refreshPhotosStateFlow.value = false
    }

    private fun updateTimelineNodes(
        imageNodes: List<MegaNode>,
        videoNodes: List<MegaNode>,
    ) = appScope.launch {
        val offlineMap = megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }
        val nodes = (imageNodes + videoNodes).map { node ->
            val offline: Offline? = offlineMap?.get(node.handle.toString())
            imageNodeMapper(
                megaNode = node,
                hasVersion = megaApiFacade::hasVersion,
                requireSerializedData = true,
                offline = offline
            )
        }

        refreshImageNodesFlow.update { false }
        for (node in nodes) {
            imageNodesCache[node.id] = node
        }

        refreshImageNodesFlow.update { true }
    }

    override fun monitorPhotos(): Flow<List<Photo>> {
        if (!isMonitoringInitiated) {
            isMonitoringInitiated = true

            monitorNodeUpdates()
            monitorRefreshPhotos()
        }
        return photosStateFlow.filterNotNull()
    }

    override fun refreshPhotos() {
        refreshPhotosStateFlow.update { true }
    }

    override suspend fun getPublicLinksCount(): Int = withContext(ioDispatcher) {
        megaApiFacade.getPublicLinks().size
    }

    override suspend fun buildDefaultDownloadDir(): File = withContext(ioDispatcher) {
        fileGateway.buildDefaultDownloadDir()
    }

    override suspend fun getCameraUploadFolderId(): Long? = withContext(ioDispatcher) {
        val getCameraUploadFolderId = megaLocalStorageFacade.getCamSyncHandle()
        getCameraUploadFolderId
    }

    override suspend fun getMediaUploadFolderId(): Long? = withContext(ioDispatcher) {
        val getMediaUploadFolderId = megaLocalStorageFacade.getMegaHandleSecondaryFolder()
        getMediaUploadFolderId
    }

    private suspend fun fetchPhotosNodes(): List<List<MegaNode>> = withContext(ioDispatcher) {
        awaitAll(
            async { fetchImageNodes() },
            async { fetchVideoNodes() },
        )
    }

    private suspend fun fetchImageNodes(): List<MegaNode> = withContext(ioDispatcher) {
        searchImages().filter {
            fileTypeInfoMapper(it) !is SvgFileTypeInfo && it.isValidPhotoNode()
        }
    }

    private suspend fun fetchVideoNodes(): List<MegaNode> = withContext(ioDispatcher) {
        searchVideos().filter {
            it.isValidPhotoNode() && fileTypeInfoMapper(it) is VideoFileTypeInfo
        }
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
                megaApiFacade.getMegaNodeByHandle(nodeHandle = nodeId.longValue)
                    ?.let { megaNode ->
                        megaNode to fileTypeInfoMapper(megaNode)
                    }?.let { (megaNode, fileType) ->
                        when (fileType) {
                            is StaticImageFileTypeInfo, is GifFileTypeInfo, is RawFileTypeInfo -> {
                                mapMegaNodeToImage(megaNode, albumPhotoId?.id)
                            }

                            is VideoFileTypeInfo -> {
                                mapMegaNodeToVideo(megaNode, albumPhotoId?.id)
                            }

                            else -> {
                                null
                            }
                        }
                    }
            }
        }?.also { photosCache[nodeId] = it }
    }

    override suspend fun getPhotosByFolderId(
        folderId: NodeId,
        searchString: String,
        recursive: Boolean,
    ): List<Photo> =
        withContext(ioDispatcher) {
            val parent = megaApiFacade.getMegaNodeByHandle(folderId.longValue)
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

    override suspend fun getPhotosByIds(ids: List<NodeId>): List<Photo> =
        withContext(ioDispatcher) {
            ids.mapNotNull { id ->
                val cache = photosCache[id]
                if (cache != null) {
                    cache
                } else {
                    var photo: Photo? = null

                    (megaApiFacade.getMegaNodeByHandle(id.longValue)
                        ?: megaApiFolder.getMegaNodeByHandle(id.longValue))
                        ?.also { node ->
                            photo = mapMegaNodeToPhoto(node, filterSvg = false)
                            photo?.let {
                                photosCache[id] = it
                            }
                        }
                    photo
                }
            }
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
     * Map megaNodes to Photos.
     */
    private suspend fun mapMegaNodesToPhotos(megaNodes: List<MegaNode>): List<Photo> {
        return megaNodes.mapNotNull { megaNode ->
            val fileType = fileTypeInfoMapper(megaNode)
            val isValid =
                megaNode.isFile
                        && (fileType is VideoFileTypeInfo
                        || fileType is ImageFileTypeInfo
                        && fileType !is SvgFileTypeInfo)
                        && !megaApiFacade.isInRubbish(megaNode)
            if (isValid.not()) return@mapNotNull null
            if (fileType is ImageFileTypeInfo) {
                mapMegaNodeToImage(megaNode)
            } else {
                mapMegaNodeToVideo(megaNode)
            }
        }
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
    private suspend fun mapPhotoNodesToImages(megaNodes: List<MegaNode>): List<Photo> {
        return megaNodes.filter {
            fileTypeInfoMapper(it) !is SvgFileTypeInfo && it.isValidPhotoNode()
        }.map { megaNode ->
            mapMegaNodeToImage(megaNode)
        }
    }

    /**
     * Convert the Photos MegaNode list to Video list
     * @param megaNodes List<MegaNode> of Photo
     * @return List<Photo> / Videos
     */
    private suspend fun mapPhotoNodesToVideos(megaNodes: List<MegaNode>): List<Photo> {
        return megaNodes.filter {
            it.isValidPhotoNode() && fileTypeInfoMapper(it) is VideoFileTypeInfo
        }.map { megaNode ->
            mapMegaNodeToVideo(megaNode)
        }
    }

    /**
     * Check valid Photo Node, not include Photo nodes that are in rubbish bin or without thumbnail
     */
    private suspend fun MegaNode.isValidPhotoNode() =
        !megaApiFacade.isInRubbish(this) && this.hasThumbnail()

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

    override fun clearCache() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = null

        refreshPhotosJob?.cancel()
        refreshPhotosJob = null

        isMonitoringInitiated = false
        photosCache.clear()
        imageNodesCache.clear()

        photosStateFlow.value = null
        refreshPhotosStateFlow.value = true
        refreshImageNodesFlow.value = true
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
            node?.let {
                getPhotoFromCache(it)
            }
        }

    override suspend fun getPhotoByPublicLink(link: String): Photo? =
        withContext(ioDispatcher) {
            val node = getPublicNode(link)
            node?.let {
                getPhotoFromCache(it)
            }
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

    private suspend fun getPhotoFromCache(node: MegaNode): Photo? {
        return photosCache[NodeId(node.handle)]
            ?: mapMegaNodeToPhoto(node, filterSvg = false)?.also {
                photosCache[NodeId(node.handle)] = it
            }
    }

    override fun monitorImageNodes(): Flow<List<ImageNode>> = refreshImageNodesFlow
        .filter { it }
        .mapLatest { imageNodesCache.values.toList() }

    override suspend fun getImageNode(nodeId: NodeId) = imageNodesCache[nodeId]

    override suspend fun getMediaDiscoveryNodes(
        parentID: Long,
        recursive: Boolean,
    ): List<ImageNode> {
        return withContext(ioDispatcher) {
            val parent = megaApiFacade.getMegaNodeByHandle(parentID)
            val searchString = ""
            parent?.let { parentNode ->
                val token = MegaCancelToken.createInstance()
                val images = async {
                    megaApiFacade.searchByType(
                        parentNode = parentNode,
                        searchString = searchString,
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_PHOTO
                    )
                }
                val videos = async {
                    megaApiFacade.searchByType(
                        parentNode = parentNode,
                        searchString = searchString,
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_VIDEO
                    )
                }
                (images.await() + videos.await()).map { node ->
                    val offlineMap =
                        megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }
                    val offline: Offline? = offlineMap?.get(node.handle.toString())
                    imageNodeMapper(
                        megaNode = node,
                        hasVersion = megaApiFacade::hasVersion,
                        requireSerializedData = true,
                        offline = offline
                    )
                }
            } ?: emptyList()
        }
    }
}

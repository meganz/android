package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import nz.mega.sdk.MegaApiAndroid
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
 * @property cacheFolderFacade CacheFolderGateway
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 * @property imageMapper ImageMapper
 * @property videoMapper VideoMapper
 */
@Singleton
internal class DefaultPhotosRepository @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val megaApiFacade: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheFolderFacade: CacheFolderGateway,
    private val megaLocalStorageFacade: MegaLocalStorageGateway,
    private val dateUtilFacade: DateUtilWrapper,
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : PhotosRepository {
    private val photosCache: MutableMap<NodeId, Photo> = mutableMapOf()

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    private val refreshPhotosStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val photosStateFlow: MutableStateFlow<List<Photo>?> = MutableStateFlow(null)

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
                val photos = searchMegaPhotos()
                for (photo in photos) {
                    photosCache[NodeId(photo.id)] = photo
                }

                photosStateFlow.update { photos }
                refreshPhotosStateFlow.value = false
            }.launchIn(appScope)
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
        cacheFolderFacade.buildDefaultDownloadDir()
    }

    override suspend fun getCameraUploadFolderId(): Long? = withContext(ioDispatcher) {
        val getCameraUploadFolderId = megaLocalStorageFacade.getCamSyncHandle()
        getCameraUploadFolderId
    }

    override suspend fun getMediaUploadFolderId(): Long? = withContext(ioDispatcher) {
        val getMediaUploadFolderId = megaLocalStorageFacade.getMegaHandleSecondaryFolder()
        getMediaUploadFolderId
    }

    private suspend fun searchMegaPhotos(): List<Photo> = withContext(ioDispatcher) {
        val images = async { mapPhotoNodesToImages(searchImages()) }
        val videos = async { mapPhotoNodesToVideos(searchVideos()) }
        images.await() + videos.await()
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

    override suspend fun getPhotosByFolderId(folderId: NodeId, recursive: Boolean): List<Photo> =
        withContext(ioDispatcher) {
            val parent = megaApiFacade.getMegaNodeByHandle(folderId.longValue)
            parent?.let { parentNode ->
                val token = MegaCancelToken.createInstance()
                val images = async {
                    val imageNodes = megaApiFacade.searchByType(
                        parentNode = parentNode,
                        searchString = "",
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_PHOTO,
                    )
                    mapPhotoNodesToImages(imageNodes)
                }
                val videos = async {
                    val videoNodes = megaApiFacade.searchByType(
                        parentNode = parentNode,
                        searchString = "",
                        cancelToken = token,
                        recursive = recursive,
                        order = MegaApiAndroid.ORDER_MODIFICATION_DESC,
                        type = MegaApiAndroid.FILE_TYPE_VIDEO,
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
                    megaApiFacade.getMegaNodeByHandle(id.longValue)?.also { node ->
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
    private fun mapMegaNodeToImage(megaNode: MegaNode, albumPhotoId: Long? = null) =
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
        )

    /**
     * Convert the MegaNode to Video
     * @param megaNode MegaNode
     * @return Photo / Video
     */
    private fun mapMegaNodeToVideo(megaNode: MegaNode, albumPhotoId: Long? = null) =
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
        )

    private fun getThumbnailCacheFilePath(megaNode: MegaNode): String? {
        if (thumbnailFolderPath == null) {
            thumbnailFolderPath =
                cacheFolderFacade.getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path
        }
        return thumbnailFolderPath?.let {
            "$it${File.separator}${megaNode.getThumbnailFileName()}"
        }
    }

    private fun getPreviewCacheFilePath(megaNode: MegaNode): String? {
        if (previewFolderPath == null) {
            previewFolderPath =
                cacheFolderFacade.getCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path
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

        photosStateFlow.value = null
        refreshPhotosStateFlow.value = true
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
}

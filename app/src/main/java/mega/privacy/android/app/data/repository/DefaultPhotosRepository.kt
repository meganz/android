package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.mapper.NodeUpdateMapper
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.presentation.favourites.facade.DateUtilWrapper
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.MegaNodeUtil.getPreviewFileName
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [PhotosRepository]
 *
 * @property megaApiFacade MegaApiGateway
 * @property ioDispatcher CoroutineDispatcher
 * @property cacheFolderFacade CacheFolderGateway
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 * @property imageMapper ImageMapper
 * @property videoMapper VideoMapper
 * @property nodeUpdateMapper NodeUpdateMapper
 */
class DefaultPhotosRepository @Inject constructor(
    private val megaApiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheFolderFacade: CacheFolderGateway,
    private val megaLocalStorageFacade: MegaLocalStorageGateway,
    private val dateUtilFacade: DateUtilWrapper,
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val nodeUpdateMapper: NodeUpdateMapper,
) : PhotosRepository {

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

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

    override fun monitorNodeUpdates(): Flow<NodeUpdate> =
        megaApiFacade.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull {
                it.nodeList?.toList()
            }.map { nodeList ->
                nodeUpdateMapper(nodeList)
            }

    override suspend fun searchMegaPhotos(): List<Photo> = withContext(ioDispatcher) {
        val photosNodes =
            (awaitSearchImages() + awaitSearchVideos()).sortedByDescending { it.modificationTime }
        mapMegaNodesToPhotos(photosNodes)
    }

    private suspend fun awaitSearchImages(): List<MegaNode> = withContext(ioDispatcher) {
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
    }

    private suspend fun awaitSearchVideos(): List<MegaNode> = withContext(ioDispatcher) {
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
    }

    /**
     * Convert the MegaNode list to Image list
     * @param nodes List<MegaNode>
     * @return List<Photo> / Images
     */
    private fun mapMegaNodesToImages(nodes: List<MegaNode>) =
        nodes.map { megaNode ->
            mapMegaNodeToImage(megaNode)
        }

    /**
     * Convert the MegaNode list to Video list
     * @param nodes List<MegaNode>
     * @return List<Photo> / Videos
     */
    private fun mapMegaNodesToVideos(nodes: List<MegaNode>) =
        nodes.map { megaNode ->
            mapMegaNodeToVideo(megaNode)
        }

    /**
     * Convert the MegaNode to Image
     * @param megaNode MegaNode
     * @return Photo / Image
     */
    private fun mapMegaNodeToImage(megaNode: MegaNode) =
        imageMapper(
            megaNode.handle,
            megaNode.parentHandle,
            megaNode.name,
            megaNode.isFavourite,
            dateUtilFacade.fromEpoch(megaNode.creationTime),
            dateUtilFacade.fromEpoch(megaNode.modificationTime),
            getThumbnailCacheFilePath(megaNode),
            getPreviewCacheFilePath(megaNode)
        )

    /**
     * Convert the MegaNode to Video
     * @param megaNode MegaNode
     * @return Photo / Video
     */
    private fun mapMegaNodeToVideo(megaNode: MegaNode) =
        videoMapper(
            megaNode.handle,
            megaNode.parentHandle,
            megaNode.name,
            megaNode.isFavourite,
            dateUtilFacade.fromEpoch(megaNode.creationTime),
            dateUtilFacade.fromEpoch(megaNode.modificationTime),
            getThumbnailCacheFilePath(megaNode),
            getPreviewCacheFilePath(megaNode),
            megaNode.duration
        )

    private suspend fun mapMegaNodesToPhotos(megaNodes: List<MegaNode>): List<Photo> =
        withContext(ioDispatcher) {
            megaNodes.filter {
                !megaApiFacade.isInRubbish(it)
            }.map { megaNode ->
                if (megaNode.isVideo()) {
                    mapMegaNodeToVideo(megaNode)
                } else {
                    mapMegaNodeToImage(megaNode)
                }
            }
        }

    private fun getThumbnailCacheFilePath(megaNode: MegaNode): String? {
        if (thumbnailFolderPath == null) {
            thumbnailFolderPath =
                cacheFolderFacade.getCacheFolder(CacheFolderManager.THUMBNAIL_FOLDER)?.path
        }
        return thumbnailFolderPath?.let {
            "$it${File.separator}${megaNode.getThumbnailFileName()}"
        }
    }


    private fun getPreviewCacheFilePath(megaNode: MegaNode): String? {
        if (previewFolderPath == null) {
            previewFolderPath =
                cacheFolderFacade.getCacheFolder(CacheFolderManager.PREVIEW_FOLDER)?.path
        }
        return previewFolderPath?.let {
            "$it${File.separator}${megaNode.getPreviewFileName()}"
        }
    }
}
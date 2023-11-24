package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.videos.VideoNodeMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.VideoNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * Implementation of VideosRepository
 */
internal class VideoSectionRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val fileNodeMapper: FileNodeMapper,
    private val cacheGateway: CacheGateway,
    private val videoNodeMapper: VideoNodeMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoSectionRepository {

    private var thumbnailFolderPath: String? = null
    override suspend fun getAllVideos(order: SortOrder): List<VideoNode> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
            megaApiGateway.searchByType(
                megaCancelToken,
                sortOrderIntMapper(order),
                FILE_TYPE_VIDEO,
                SEARCH_TARGET_ROOTNODE
            ).map { megaNode ->
                videoNodeMapper(
                    fileNode = megaNode.convertToFileNode(
                        offlineItems?.get(megaNode.handle.toString())
                    ),
                    thumbnailFilePath = getThumbnailCacheFilePath(megaNode)
                )
            }
        }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }

    private suspend fun MegaNode.convertToFileNode(offline: Offline?) = fileNodeMapper(
        megaNode = this, requireSerializedData = false, offline = offline
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
}
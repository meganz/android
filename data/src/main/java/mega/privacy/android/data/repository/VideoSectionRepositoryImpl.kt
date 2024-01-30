package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.VideoPlaylistMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import javax.inject.Inject

/**
 * Implementation of VideoSectionRepository
 */
internal class VideoSectionRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val fileNodeMapper: FileNodeMapper,
    private val typedVideoNodeMapper: TypedVideoNodeMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val userSetMapper: UserSetMapper,
    private val videoPlaylistMapper: VideoPlaylistMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoSectionRepository {

    override suspend fun getAllVideos(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
            megaApiGateway.searchByType(
                megaCancelToken,
                sortOrderIntMapper(order),
                FILE_TYPE_VIDEO,
                SEARCH_TARGET_ROOTNODE
            ).map { megaNode ->
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(
                        offlineItems?.get(megaNode.handle.toString())
                    ),
                    duration = megaNode.duration,
                )
            }
        }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }

    private suspend fun MegaNode.convertToFileNode(offline: Offline?) = fileNodeMapper(
        megaNode = this, requireSerializedData = false, offline = offline
    )

    override suspend fun getVideoPlaylists(): List<VideoPlaylist> =
        withContext(ioDispatcher) {
            getAllUserSets().map { userSet ->
                userSet.toVideoPlaylist()
            }
        }

    private suspend fun getAllUserSets(): List<UserSet> {
        val setList = megaApiGateway.getSets()
        val userSets = (0 until setList.size())
            .filter { index ->
                setList.get(index).type() == MegaSet.SET_TYPE_PLAYLIST
            }.map {
                setList.get(it).toUserSet()
            }
            .associateBy { it.id }
        return userSets.values.toList()
    }

    private fun MegaSet.toUserSet(): UserSet {
        val cover = cover().takeIf { it != -1L }
        return userSetMapper(
            id(),
            name(),
            type(),
            cover,
            cts(),
            ts(),
            isExported
        )
    }

    private suspend fun UserSet.toVideoPlaylist(): VideoPlaylist {
        val elementList = megaApiGateway.getSetElements(sid = id)
        val videoNodeList = (0 until elementList.size()).mapNotNull { index ->
            val element = elementList[index]
            megaApiGateway.getMegaNodeByHandle(element.id())?.let { megaNode ->
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(
                        null
                    ),
                    duration = megaNode.duration,
                )
            }
        }
        return videoPlaylistMapper(
            userSet = this,
            videoNodeList = videoNodeList
        )
    }
}
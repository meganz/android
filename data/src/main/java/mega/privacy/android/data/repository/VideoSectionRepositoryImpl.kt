package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.CreateSetElementListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.VideoPlaylistMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import timber.log.Timber
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
            megaApiGateway.getMegaNodeByHandle(element.node())?.let { megaNode ->
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

    override suspend fun createVideoPlaylist(title: String): VideoPlaylist =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { megaRequest, megaError ->
                        if (megaError.errorCode == MegaError.API_OK) {
                            val newSet = megaRequest.megaSet
                            continuation.resumeWith(
                                Result.success(
                                    videoPlaylistMapper(
                                        userSet = newSet.toUserSet(),
                                        videoNodeList = emptyList()
                                    )
                                )
                            )
                        } else {
                            Timber.e("Error creating new album: ${megaError.errorString}")
                            continuation.failWithError(megaError, "createAlbum")
                        }
                    }
                )
                megaApiGateway.createSet(
                    name = title,
                    type = MegaSet.SET_TYPE_PLAYLIST,
                    listener = listener
                )
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }

    override suspend fun addVideosToPlaylist(playlistID: NodeId, videoIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = CreateSetElementListenerInterface(
                    target = videoIDs.size,
                    onCompletion = { success, _ ->
                        continuation.resumeWith(Result.success(success))
                    }
                )
                for (videoID in videoIDs) {
                    megaApiGateway.createSetElement(
                        sid = playlistID.longValue,
                        node = videoID.longValue,
                        listener = listener
                    )
                }
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }
}
package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.CreateSetElementListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.RemoveSetElementListenerInterface
import mega.privacy.android.data.listener.RemoveSetsListenerInterface
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.VideoPlaylistMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of VideoSectionRepository
 */
@Singleton
internal class VideoSectionRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val fileNodeMapper: FileNodeMapper,
    private val typedVideoNodeMapper: TypedVideoNodeMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val userSetMapper: UserSetMapper,
    private val videoPlaylistMapper: VideoPlaylistMapper,
    private val nodeRepository: NodeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoSectionRepository {
    private val videoPlaylistsMap: MutableMap<Long, UserSet> = mutableMapOf()
    private val videoSetsMap: MutableMap<NodeId, MutableSet<Long>> = mutableMapOf()

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
            val offlineItems = getAllOfflineNodeHandle()
            getAllUserSets().map { userSet ->
                userSet.toVideoPlaylist(offlineItems)
            }
        }

    private suspend fun getAllUserSets(): List<UserSet> {
        videoPlaylistsMap.clear()
        videoSetsMap.clear()
        val setList = megaApiGateway.getSets()
        val userSets = (0 until setList.size())
            .filter { index ->
                setList.get(index).type() == MegaSet.SET_TYPE_PLAYLIST
            }.map {
                setList.get(it).toUserSet()
            }
            .associateBy { it.id }
        videoPlaylistsMap.putAll(userSets)
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

    private suspend fun UserSet.toVideoPlaylist(offlineMap: Map<String, Offline>?): VideoPlaylist {
        val elementList = megaApiGateway.getSetElements(sid = id)
        val videoNodeList = (0 until elementList.size()).mapNotNull { index ->
            val element = elementList[index]

            videoSetsMap.getOrPut(NodeId(element.node())) { mutableSetOf() }.add(element.setId())

            megaApiGateway.getMegaNodeByHandle(element.node())?.let { megaNode ->
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(
                        offlineMap?.get(megaNode.handle.toString())
                    ),
                    duration = megaNode.duration,
                    elementID = element.id()
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
                            Timber.e("Error creating new playlist: ${megaError.errorString}")
                            continuation.failWithError(megaError, "createPlaylist")
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

    override suspend fun removeVideoPlaylists(playlistIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = RemoveSetsListenerInterface(
                    target = playlistIDs.size,
                    onCompletion = { success, _ ->
                        continuation.resumeWith(Result.success(success))
                    }
                )
                for (playlistID in playlistIDs) {
                    megaApiGateway.removeSet(
                        sid = playlistID.longValue,
                        listener = listener,
                    )
                }
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }

    override suspend fun removeVideosFromPlaylist(
        playlistID: NodeId,
        videoElementIDs: List<Long>,
    ) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                launch {
                    val listener = RemoveSetElementListenerInterface(
                        target = videoElementIDs.size,
                        onCompletion = { success, _ ->
                            continuation.resumeWith(Result.success(success))
                        }
                    )
                    for (elementID in videoElementIDs) {
                        megaApiGateway.removeSetElement(
                            sid = playlistID.longValue,
                            eid = elementID,
                            listener = listener,
                        )
                    }
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(
                            listener
                        )
                    }
                }
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

    override suspend fun updateVideoPlaylistTitle(
        playlistID: NodeId,
        newTitle: String,
    ): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("updateVideoPlaylistTitle") {
                    it.text
                }
                megaApiGateway.updateSetName(
                    sid = playlistID.longValue,
                    name = newTitle,
                    listener = listener
                )
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }

    override fun monitorVideoPlaylistSetsUpdate(): Flow<List<Long>> =
        merge(
            monitorSetsUpdates(),
            monitorNodeUpdates(),
            monitorOfflineNodeUpdates()
        )

    private fun monitorSetsUpdates(): Flow<List<Long>> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
        .mapNotNull { it.sets }
        .map { sets ->
            sets.filter {
                it.type() == MegaSet.SET_TYPE_PLAYLIST
            }.map {
                it.id()
            }
        }

    private fun monitorNodeUpdates(): Flow<List<Long>> =
        nodeRepository.monitorNodeUpdates()
            .mapNotNull { nodeUpdate ->
                nodeUpdate.changes.keys.flatMap { node ->
                    val setIds = videoSetsMap[node.id] ?: emptySet()
                    setIds.mapNotNull { videoPlaylistsMap[it]?.id }
                }
            }

    private fun monitorOfflineNodeUpdates(): Flow<List<Long>> =
        nodeRepository.monitorOfflineNodeUpdates()
            .mapNotNull { offlineList ->
                offlineList.map { it.handle.toLong() }
            }
}
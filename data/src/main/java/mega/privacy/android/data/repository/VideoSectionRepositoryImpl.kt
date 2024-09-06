package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.listener.AddElementToSetsListenerInterface
import mega.privacy.android.data.listener.CreateSetElementListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.RemoveSetElementListenerInterface
import mega.privacy.android.data.listener.RemoveSetsListenerInterface
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.VideoPlaylistMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedItemMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoSectionRepository
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
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val appPreferencesGateway: AppPreferencesGateway,
    private val videoRecentlyWatchedItemMapper: VideoRecentlyWatchedItemMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoSectionRepository {
    private val videoPlaylistsMap: MutableMap<Long, UserSet> = mutableMapOf()
    private val videoSetsMap: MutableMap<NodeId, MutableSet<Long>> = mutableMapOf()
    private val recentlyWatchedVideosData = mutableListOf<VideoRecentlyWatchedItem>()

    override suspend fun getAllVideos(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
            val filter = megaSearchFilterMapper(
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.VIDEO
            )
            megaApiGateway.searchWithFilter(
                filter,
                sortOrderIntMapper(order),
                megaCancelToken,
            ).map { megaNode ->
                val isOutShared =
                    megaApiGateway.getMegaNodeByHandle(megaNode.parentHandle)?.isOutShare == true
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(offlineItems[megaNode.handle.toString()]),
                    duration = megaNode.duration,
                    isOutShared = isOutShared
                )
            }
        }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo().associateBy { it.handle }

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
            val elementNode = element.node()

            megaApiGateway.getMegaNodeByHandle(elementNode)?.let { megaNode ->
                val isInRubbish = megaApiGateway.isInRubbish(megaNode)
                if (isInRubbish) return@mapNotNull null

                videoSetsMap.getOrPut(NodeId(elementNode)) { mutableSetOf() }
                    .add(element.setId())
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
            }
        }

    override suspend fun addVideoToMultiplePlaylists(playlistIDs: List<Long>, videoID: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = AddElementToSetsListenerInterface(
                    target = playlistIDs,
                    onCompletion = { success, _ ->
                        continuation.resumeWith(Result.success(success))
                    }
                )
                for (playlistID in playlistIDs) {
                    megaApiGateway.createSetElement(
                        sid = playlistID,
                        node = videoID,
                        listener = listener
                    )
                }
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
            }
        }

    override fun monitorSetsUpdates(): Flow<List<Long>> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
        .mapNotNull { it.sets }
        .map { sets ->
            sets.filter {
                it.type() == MegaSet.SET_TYPE_PLAYLIST
            }.map {
                it.id()
            }
        }

    override fun getVideoSetsMap() = videoSetsMap

    override fun getVideoPlaylistsMap() = videoPlaylistsMap

    override suspend fun getVideoPlaylistSets(): List<UserSet> = getAllUserSets()

    override suspend fun saveVideoRecentlyWatched(handle: Long, timestamp: Long) {
        initVideoRecentlyWatchedData()
        recentlyWatchedVideosData.indexOfFirst { it.videoHandle == handle }.let { index ->
            if (index == -1) {
                recentlyWatchedVideosData.add(videoRecentlyWatchedItemMapper(handle, timestamp))
            } else {
                recentlyWatchedVideosData[index] =
                    recentlyWatchedVideosData[index].copy(watchedTimestamp = timestamp)
            }

            appPreferencesGateway.putString(
                PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS,
                Json.encodeToString(recentlyWatchedVideosData)
            )
        }
    }

    private suspend fun initVideoRecentlyWatchedData() {
        if (recentlyWatchedVideosData.isEmpty()) {
            getRecentlyWatchedData()?.let {
                recentlyWatchedVideosData.addAll(it)
            }
        }
    }

    private suspend fun getRecentlyWatchedData(): List<VideoRecentlyWatchedItem>? =
        appPreferencesGateway.monitorString(
            PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS,
            null
        ).firstOrNull()?.let { jsonString ->
            Json.decodeFromString(jsonString)
        }

    override suspend fun getRecentlyWatchedVideoNodes(): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            initVideoRecentlyWatchedData()
            val offlineItems = getAllOfflineNodeHandle()
            recentlyWatchedVideosData.mapNotNull { item ->
                megaApiGateway.getMegaNodeByHandle(item.videoHandle)?.let { megaNode ->
                    typedVideoNodeMapper(
                        fileNode = megaNode.convertToFileNode(offlineItems[megaNode.handle.toString()]),
                        duration = megaNode.duration,
                        watchedTimestamp = item.watchedTimestamp
                    )
                }
            }.sortedByDescending { it.watchedTimestamp }
        }

    override suspend fun clearRecentlyWatchedVideos() = withContext(ioDispatcher) {
        recentlyWatchedVideosData.clear()
        appPreferencesGateway.putString(
            PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS,
            Json.encodeToString(recentlyWatchedVideosData)
        )
    }

    override suspend fun removeRecentlyWatchedItem(handle: Long) {
        initVideoRecentlyWatchedData()
        recentlyWatchedVideosData.removeAll { it.videoHandle == handle }
        appPreferencesGateway.putString(
            PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS,
            Json.encodeToString(recentlyWatchedVideosData)
        )
    }

    companion object {
        private const val PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS =
            "PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS"
    }
}

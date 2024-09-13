package mega.privacy.android.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.MediaPlayerPreferencesGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.audios.TypedAudioNodeMapper
import mega.privacy.android.data.mapper.mediaplayer.RepeatToggleModeMapper
import mega.privacy.android.data.mapper.mediaplayer.SubtitleFileInfoMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.MediaPlayerRepository
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of MediaPlayerRepository
 */
internal class DefaultMediaPlayerRepository @Inject constructor(
    private val megaApi: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val dbHandler: Lazy<DatabaseHandler>,
    private val fileNodeMapper: FileNodeMapper,
    private val typedAudioNodeMapper: TypedAudioNodeMapper,
    private val typedVideoNodeMapper: TypedVideoNodeMapper,
    private val fileGateway: FileGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val appPreferencesGateway: AppPreferencesGateway,
    private val subtitleFileInfoMapper: SubtitleFileInfoMapper,
    private val mediaPlayerPreferencesGateway: MediaPlayerPreferencesGateway,
    private val repeatToggleModeMapper: RepeatToggleModeMapper,
    private val searchFilterMapper: MegaSearchFilterMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MediaPlayerRepository {

    private val playbackInfoMap = mutableMapOf<Long, PlaybackInformation>()

    override suspend fun getLocalLinkForFolderLinkFromMegaApi(nodeHandle: Long): String? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                megaApiFolder.authorizeNode(megaNode)
            }?.let {
                megaApi.httpServerGetLocalLink(it)
            }
        }

    override suspend fun getLocalLinkForFolderLinkFromMegaApiFolder(nodeHandle: Long): String? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                megaApiFolder.authorizeNode(megaNode)
            }?.let {
                megaApiFolder.httpServerGetLocalLink(it)
            }
        }

    override suspend fun getLocalLinkFromMegaApi(nodeHandle: Long): String? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                megaApi.httpServerGetLocalLink(megaNode)
            }
        }

    override suspend fun getAudioNodes(order: SortOrder): List<TypedAudioNode> =
        getMegaNodeByCategory(
            searchCategory = SearchCategory.AUDIO,
            order = order
        ).map { megaNode ->
            convertToTypedAudioNode(megaNode)
        }


    override suspend fun getVideoNodes(order: SortOrder): List<TypedVideoNode> =
        getMegaNodeByCategory(
            searchCategory = SearchCategory.VIDEO,
            order = order
        ).map { megaNode ->
            convertToTypedVideoNode(megaNode)
        }


    override suspend fun getThumbnailFromMegaApi(nodeHandle: Long, path: String): Long? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { node ->
                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("getThumbnailFromMegaApi") { it.nodeHandle }
                    megaApi.getThumbnail(node = node, thumbnailFilePath = path, listener = listener)
                }
            }
        }

    override suspend fun getThumbnailFromMegaApiFolder(nodeHandle: Long, path: String): Long? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { node ->
                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("getThumbnailFromMegaApiFolder") { it.nodeHandle }
                    megaApiFolder.getThumbnail(
                        node = node,
                        thumbnailFilePath = path,
                        listener = listener
                    )
                }
            }
        }

    override suspend fun getAudioNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode> = withContext(ioDispatcher) {
        getMegaNodeByCategory(
            parentId = NodeId(parentHandle),
            searchCategory = SearchCategory.AUDIO,
            order = order,
            recursive = false
        ).map { megaNode ->
            convertToTypedAudioNode(megaNode)
        }
    }

    override suspend fun getVideoNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode> = withContext(ioDispatcher) {
        getMegaNodeByCategory(
            parentId = NodeId(parentHandle),
            searchCategory = SearchCategory.VIDEO,
            order = order,
            recursive = false
        ).map { megaNode ->
            convertToTypedVideoNode(megaNode)
        }
    }

    override suspend fun getAudiosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode> = withContext(ioDispatcher) {
        getMegaNodeByCategoryFromFolderLink(
            parentId = NodeId(parentHandle),
            searchCategory = SearchCategory.AUDIO,
            order = order,
            recursive = false
        ).map { megaNode ->
            convertToTypedAudioNode(megaNode)
        }
    }

    override suspend fun getVideosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode> = withContext(ioDispatcher) {
        getMegaNodeByCategoryFromFolderLink(
            parentId = NodeId(parentHandle),
            searchCategory = SearchCategory.VIDEO,
            order = order,
            recursive = false
        ).map { megaNode ->
            convertToTypedVideoNode(megaNode)
        }
    }

    override suspend fun getAudioNodesFromPublicLinks(order: SortOrder): List<TypedAudioNode> =
        withContext(ioDispatcher) {
            getMegaNodeByCategory(
                searchCategory = SearchCategory.AUDIO,
                order = order,
                searchTarget = SearchTarget.LINKS_SHARE
            ).map { node ->
                convertToTypedAudioNode(node)
            }
        }

    override suspend fun getVideoNodesFromPublicLinks(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            getMegaNodeByCategory(
                searchCategory = SearchCategory.VIDEO,
                order = order,
                searchTarget = SearchTarget.LINKS_SHARE
            ).map { node ->
                convertToTypedVideoNode(node)
            }
        }


    override suspend fun getAudioNodesFromInShares(order: SortOrder): List<TypedAudioNode> =
        withContext(ioDispatcher) {
            getMegaNodeByCategory(
                searchCategory = SearchCategory.AUDIO,
                order = order,
                searchTarget = SearchTarget.INCOMING_SHARE
            ).map { node ->
                convertToTypedAudioNode(node)
            }
        }

    override suspend fun getVideoNodesFromInShares(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            getMegaNodeByCategory(
                searchCategory = SearchCategory.VIDEO,
                order = order,
                searchTarget = SearchTarget.INCOMING_SHARE
            ).map { node ->
                convertToTypedVideoNode(node)
            }
        }

    override suspend fun getAudioNodesFromOutShares(
        lastHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode> = withContext(ioDispatcher) {
        getMegaNodeByCategory(
            searchCategory = SearchCategory.AUDIO,
            order = order,
            parentId = NodeId(lastHandle),
            searchTarget = SearchTarget.OUTGOING_SHARE
        ).map { node ->
            convertToTypedAudioNode(node)
        }
    }

    override suspend fun getVideoNodesFromOutShares(
        lastHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode> = withContext(ioDispatcher) {
        getMegaNodeByCategory(
            searchCategory = SearchCategory.VIDEO,
            order = order,
            parentId = NodeId(lastHandle),
            searchTarget = SearchTarget.OUTGOING_SHARE
        ).map { megaNode ->
            convertToTypedVideoNode(megaNode)
        }
    }

    override suspend fun getAudioNodesByEmail(email: String): List<TypedAudioNode>? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                megaApi.getInShares(megaUser).filter { megaNode ->
                    megaNode.isFile && filterByNodeName(true, megaNode.name)
                }.map { node ->
                    convertToTypedAudioNode(node)
                }
            }
        }

    override suspend fun getVideoNodesByEmail(email: String): List<TypedVideoNode>? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                megaApi.getInShares(megaUser).filter { megaNode ->
                    megaNode.isFile && filterByNodeName(false, megaNode.name)
                }.map { node ->
                    convertToTypedVideoNode(node)
                }
            }
        }

    override suspend fun getAudioNodesByHandles(handles: List<Long>): List<TypedAudioNode> {
        val offlineMap = getAllOfflineNodeHandle()
        return handles.mapNotNull { handle ->
            megaApi.getMegaNodeByHandle(handle)
        }.map { node ->
            convertToTypedAudioNode(node = node, offline = offlineMap?.get(node.handle.toString()))
        }
    }

    override suspend fun getVideoNodesByHandles(handles: List<Long>): List<TypedVideoNode> {
        val offlineMap = getAllOfflineNodeHandle()
        return handles.mapNotNull { handle ->
            megaApi.getMegaNodeByHandle(handle)
        }.map { node ->
            convertToTypedVideoNode(node = node, offline = offlineMap?.get(node.handle.toString()))
        }
    }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }

    override suspend fun getVideoNodeByHandle(handle: Long, attemptFromFolderApi: Boolean) =
        withContext(ioDispatcher) {
            getMegaNodeByHandle(NodeId(handle), attemptFromFolderApi)
                ?.let { megaNode ->
                    convertToTypedVideoNode(
                        node = megaNode,
                        offline = getOfflineNode(megaNode.handle)
                    )
                }
        }

    override suspend fun getAudioNodeByHandle(handle: Long, attemptFromFolderApi: Boolean) =
        withContext(ioDispatcher) {
            getMegaNodeByHandle(NodeId(handle), attemptFromFolderApi)
                ?.let { megaNode ->
                    convertToTypedAudioNode(
                        node = megaNode,
                        offline = getOfflineNode(megaNode.handle)
                    )
                }
        }

    private suspend fun getMegaNodeByHandle(nodeId: NodeId, attemptFromFolderApi: Boolean = false) =
        megaApi.getMegaNodeByHandle(nodeId.longValue)
            ?: takeIf { attemptFromFolderApi }
                ?.let { megaApiFolder.getMegaNodeByHandle(nodeId.longValue) }
                ?.let { megaApiFolder.authorizeNode(it) }

    private suspend fun getOfflineNode(handle: Long) =
        megaLocalRoomGateway.getOfflineInformation(handle)

    override suspend fun getUserNameByEmail(email: String): String? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                getMegaUserNameDB(megaUser)
            }
        }

    override suspend fun megaApiHttpServerStop() = withContext(ioDispatcher) {
        megaApi.httpServerStop()
    }

    override suspend fun megaApiFolderHttpServerStop() = withContext(ioDispatcher) {
        megaApiFolder.httpServerStop()
    }

    override suspend fun megaApiHttpServerIsRunning(): Int = withContext(ioDispatcher) {
        megaApi.httpServerIsRunning()
    }

    override suspend fun megaApiFolderHttpServerIsRunning(): Int = withContext(ioDispatcher) {
        megaApiFolder.httpServerIsRunning()
    }

    override suspend fun megaApiHttpServerStart() = withContext(ioDispatcher) {
        megaApi.httpServerStart()
    }

    override suspend fun megaApiFolderHttpServerStart() = withContext(ioDispatcher) {
        megaApiFolder.httpServerStart()
    }

    override suspend fun megaApiHttpServerSetMaxBufferSize(bufferSize: Int) =
        withContext(ioDispatcher) {
            megaApi.httpServerSetMaxBufferSize(bufferSize)
        }

    override suspend fun megaApiFolderHttpServerSetMaxBufferSize(bufferSize: Int) =
        withContext(ioDispatcher) {
            megaApiFolder.httpServerSetMaxBufferSize(bufferSize)
        }

    override suspend fun getLocalFilePath(typedFileNode: TypedFileNode?): String? =
        withContext(ioDispatcher) {
            typedFileNode?.let {
                fileGateway.getLocalFile(
                    typedFileNode.name,
                    typedFileNode.size,
                    typedFileNode.modificationTime
                )?.path
            }
        }

    override suspend fun deletePlaybackInformation(mediaId: Long) {
        playbackInfoMap.remove(mediaId)
    }

    override suspend fun clearPlaybackInformation() = withContext(ioDispatcher) {
        playbackInfoMap.clear()
        appPreferencesGateway.putString(
            PREFERENCE_KEY_VIDEO_EXIT_TIME,
            Json.encodeToString(playbackInfoMap)
        )
    }

    override suspend fun savePlaybackTimes() {
        appPreferencesGateway.putString(
            PREFERENCE_KEY_VIDEO_EXIT_TIME,
            Gson().toJson(playbackInfoMap)
        )
    }

    override suspend fun updatePlaybackInformation(playbackInformation: PlaybackInformation) {
        playbackInformation.mediaId?.let { mediaId ->
            playbackInfoMap[mediaId] = playbackInformation
        }
    }

    override fun monitorPlaybackTimes(): Flow<Map<Long, PlaybackInformation>?> =
        appPreferencesGateway.monitorString(
            PREFERENCE_KEY_VIDEO_EXIT_TIME,
            null
        ).map { jsonString ->
            jsonString?.let {
                runCatching {
                    Gson().fromJson<Map<Long, PlaybackInformation>?>(
                        it,
                        object : TypeToken<Map<Long, PlaybackInformation>>() {}.type
                    )
                        .let { infoMap ->
                            // If the playbackInfoMap is empty, using the local information
                            // else using the current playbackInfoMap
                            if (playbackInfoMap.isEmpty()) {
                                playbackInfoMap.putAll(infoMap)
                            }
                        }
                }.onFailure {
                    // Log the error jsonString and clear it.
                    Timber.d(it, "The error jsonString: $jsonString")
                    playbackInfoMap.clear()
                    appPreferencesGateway.putString(
                        PREFERENCE_KEY_VIDEO_EXIT_TIME,
                        Gson().toJson(playbackInfoMap)
                    )
                }
                playbackInfoMap
            }
        }

    override suspend fun getFileUrlByNodeHandle(handle: Long): String? = withContext(ioDispatcher) {
        megaApi.getMegaNodeByHandle(handle)?.let { node ->
            megaApi.httpServerGetLocalLink(node)
        }
    }

    override suspend fun getSubtitleFileInfoList(fileSuffix: String) =
        withContext(ioDispatcher) {
            getMegaNodeByCategory(
                query = fileSuffix,
                searchCategory = SearchCategory.ALL,
                order = SortOrder.ORDER_DEFAULT_DESC
            ).map { megaNode ->
                subtitleFileInfoMapper(
                    id = megaNode.handle,
                    name = megaNode.name,
                    url = megaApi.httpServerGetLocalLink(megaNode),
                    parentName = megaApi.getParentNode(megaNode)?.name,
                    isMarkedSensitive = megaNode.isMarkedSensitive,
                    isSensitiveInherited = megaApi.isSensitiveInherited(megaNode)
                )
            }
        }

    override fun monitorAudioBackgroundPlayEnabled() =
        mediaPlayerPreferencesGateway.monitorAudioBackgroundPlayEnabled()

    override suspend fun setAudioBackgroundPlayEnabled(value: Boolean) =
        mediaPlayerPreferencesGateway.setAudioBackgroundPlayEnabled(value)

    override fun monitorAudioShuffleEnabled() =
        mediaPlayerPreferencesGateway.monitorAudioShuffleEnabled()

    override suspend fun setAudioShuffleEnabled(value: Boolean) =
        mediaPlayerPreferencesGateway.setAudioShuffleEnabled(value)

    override fun monitorAudioRepeatMode() =
        mediaPlayerPreferencesGateway.monitorAudioRepeatMode().map {
            repeatToggleModeMapper(it)
        }

    override suspend fun setAudioRepeatMode(value: Int) =
        mediaPlayerPreferencesGateway.setAudioRepeatMode(value)

    override fun monitorVideoRepeatMode() =
        mediaPlayerPreferencesGateway.monitorVideoRepeatMode().map {
            repeatToggleModeMapper(it)
        }

    override suspend fun setVideoRepeatMode(value: Int) =
        mediaPlayerPreferencesGateway.setVideoRepeatMode(value)

    private fun filterByNodeName(isAudio: Boolean, name: String): Boolean =
        MimeTypeList.typeForName(name).let { mimeType ->
            if (isAudio) {
                mimeType.isAudio && !mimeType.isAudioNotSupported
            } else {
                mimeType.isVideo && !mimeType.isVideoNotSupported
            }
        }

    private fun getMegaUserNameDB(user: MegaUser): String? =
        dbHandler.get().findContactByHandle(user.handle)?.let { megaContactDB ->
            when {
                megaContactDB.nickname.isNullOrEmpty().not() -> {
                    megaContactDB.nickname
                }

                megaContactDB.firstName.isNullOrEmpty().not() -> {
                    if (megaContactDB.lastName.isNullOrEmpty().not()) {
                        "${megaContactDB.firstName} ${megaContactDB.lastName}"
                    } else {
                        megaContactDB.firstName
                    }
                }

                megaContactDB.lastName.isNullOrEmpty().not() -> {
                    megaContactDB.lastName
                }

                else -> {
                    megaContactDB.email
                }
            } ?: user.email
        }

    override suspend fun getVideosBySearchType(
        handle: Long,
        searchString: String,
        recursive: Boolean,
        order: SortOrder,
    ): List<TypedVideoNode> {
        val nodes = getMegaNodeByCategory(
            parentId = NodeId(handle),
            recursive = recursive,
            query = searchString,
            searchCategory = SearchCategory.VIDEO,
            order = order
        )
        return nodes.map { node ->
            convertToTypedVideoNode(node)
        }
    }

    private suspend fun getMegaNodeByCategory(
        parentId: NodeId? = null,
        recursive: Boolean = true,
        query: String = "",
        searchCategory: SearchCategory,
        order: SortOrder,
        searchTarget: SearchTarget = SearchTarget.ROOT_NODES,
    ): List<MegaNode> {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = searchFilterMapper(
            parentHandle = parentId,
            searchQuery = query,
            searchTarget = searchTarget,
            searchCategory = searchCategory,
        )
        return if (recursive) {
            megaApi.searchWithFilter(
                filter = filter,
                order = sortOrderIntMapper(order),
                megaCancelToken = token
            )
        } else {
            megaApi.getChildren(
                filter = filter,
                order = sortOrderIntMapper(order),
                megaCancelToken = token
            )
        }
    }

    private suspend fun getMegaNodeByCategoryFromFolderLink(
        parentId: NodeId = NodeId(-1L),
        recursive: Boolean = true,
        query: String = "",
        searchCategory: SearchCategory,
        order: SortOrder,
    ): List<MegaNode> {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = searchFilterMapper(
            parentHandle = parentId,
            searchQuery = query,
            searchTarget = SearchTarget.ROOT_NODES,
            searchCategory = searchCategory,
        )
        return if (recursive) {
            megaApiFolder.search(
                filter = filter,
                order = sortOrderIntMapper(order),
                megaCancelToken = token
            )
        } else {
            megaApiFolder.getChildren(
                filter = filter,
                order = sortOrderIntMapper(order),
                megaCancelToken = token
            )
        }
    }

    private suspend fun convertToTypedVideoNode(
        node: MegaNode,
        offline: Offline? = null,
    ): TypedVideoNode = typedVideoNodeMapper(
        fileNode = node.convertToFileNode(offline),
        node.duration
    )

    private suspend fun convertToTypedAudioNode(
        node: MegaNode,
        offline: Offline? = null,
    ): TypedAudioNode = typedAudioNodeMapper(
        fileNode = node.convertToFileNode(offline),
        node.duration,
    )

    private suspend fun MegaNode.convertToFileNode(offline: Offline?) = fileNodeMapper(
        megaNode = this, requireSerializedData = false, offline = offline
    )

    companion object {
        private const val PREFERENCE_KEY_VIDEO_EXIT_TIME = "PREFERENCE_KEY_VIDEO_EXIT_TIME"
    }
}

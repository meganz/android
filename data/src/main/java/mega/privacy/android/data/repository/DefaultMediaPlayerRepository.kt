package mega.privacy.android.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.MediaPlayerRepository
import nz.mega.sdk.MegaApiJava.FILE_TYPE_AUDIO
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaCancelToken
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
    private val dbHandler: DatabaseHandler,
    private val fileNodeMapper: FileNodeMapper,
    private val typedAudioNodeMapper: TypedAudioNodeMapper,
    private val typedVideoNodeMapper: TypedVideoNodeMapper,
    private val fileGateway: FileGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val appPreferencesGateway: AppPreferencesGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val subtitleFileInfoMapper: SubtitleFileInfoMapper,
    private val mediaPlayerPreferencesGateway: MediaPlayerPreferencesGateway,
    private val repeatToggleModeMapper: RepeatToggleModeMapper,
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
        withContext(ioDispatcher) {
            megaApi.searchByType(
                MegaCancelToken.createInstance(),
                sortOrderIntMapper(order),
                FILE_TYPE_AUDIO,
                SEARCH_TARGET_ROOTNODE
            ).filter {
                it.isFile && filterByNodeName(true, it.name)
            }.map { megaNode ->
                convertToTypedAudioNode(megaNode)
            }
        }


    override suspend fun getVideoNodes(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            megaApi.searchByType(
                MegaCancelToken.createInstance(),
                sortOrderIntMapper(order),
                FILE_TYPE_VIDEO,
                SEARCH_TARGET_ROOTNODE
            ).filter {
                it.isFile && filterByNodeName(false, it.name)
            }.map { megaNode ->
                convertToTypedVideoNode(megaNode)
            }
        }


    override suspend fun getThumbnailFromMegaApi(nodeHandle: Long, path: String): Long? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { node ->
                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("getThumbnailFromMegaApi") { it.nodeHandle }
                    megaApi.getThumbnail(node = node, thumbnailFilePath = path, listener = listener)
                    continuation.invokeOnCancellation {
                        megaApi.removeRequestListener(listener)
                    }
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
                    continuation.invokeOnCancellation {
                        megaApi.removeRequestListener(listener)
                    }
                }
            }
        }

    override suspend fun areCredentialsNull(): Boolean = dbHandler.credentials == null

    override suspend fun getAudioNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode>? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApi.getChildren(parent, sortOrderIntMapper(order)).filter {
                    it.isFile && filterByNodeName(true, it.name)
                }.map { node ->
                    convertToTypedAudioNode(node)
                }
            }
        }

    override suspend fun getVideoNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode>? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApi.getChildren(parent, sortOrderIntMapper(order)).filter {
                    it.isFile && filterByNodeName(false, it.name)
                }.map { node ->
                    convertToTypedVideoNode(node)
                }
            }
        }

    override suspend fun getAudiosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode>? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApiFolder.getChildren(parent, sortOrderIntMapper(order)).filter {
                    it.isFile && filterByNodeName(true, it.name)
                }.map { node ->
                    convertToTypedAudioNode(node)
                }
            }
        }

    override suspend fun getVideosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode>? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApiFolder.getChildren(parent, sortOrderIntMapper(order)).filter {
                    it.isFile && filterByNodeName(false, it.name)
                }.map { node ->
                    convertToTypedVideoNode(node)
                }
            }
        }

    override suspend fun getAudioNodesFromPublicLinks(order: SortOrder): List<TypedAudioNode> =
        withContext(ioDispatcher) {
            megaApi.getPublicLinks(sortOrderIntMapper(order)).filter {
                it.isFile && filterByNodeName(true, it.name)
            }.map { node ->
                convertToTypedAudioNode(node)
            }
        }

    override suspend fun getVideoNodesFromPublicLinks(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            megaApi.getPublicLinks(sortOrderIntMapper(order)).filter {
                it.isFile && filterByNodeName(false, it.name)
            }.map { node ->
                convertToTypedVideoNode(node)
            }
        }


    override suspend fun getAudioNodesFromInShares(order: SortOrder): List<TypedAudioNode> =
        withContext(ioDispatcher) {
            megaApi.getInShares(sortOrderIntMapper(order)).filter {
                it.isFile && filterByNodeName(true, it.name)
            }.map { node ->
                convertToTypedAudioNode(node)
            }
        }

    override suspend fun getVideoNodesFromInShares(order: SortOrder): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            megaApi.getInShares(sortOrderIntMapper(order)).filter {
                it.isFile && filterByNodeName(false, it.name)
            }.map { node ->
                convertToTypedVideoNode(node)
            }
        }

    override suspend fun getAudioNodesFromOutShares(
        lastHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode> =
        withContext(ioDispatcher) {
            var handle: Long? = lastHandle
            val result = mutableListOf<MegaNode>()
            megaApi.getOutShares(sortOrderIntMapper(order)).map { megaShare ->
                megaApi.getMegaNodeByHandle(megaShare.nodeHandle)
            }.let { nodes ->
                for (node in nodes) {
                    if (node != null && node.handle != handle) {
                        handle = node.handle
                        result.add(node)
                    }
                }
            }
            result.filter { megaNode ->
                megaNode.isFile && filterByNodeName(true, megaNode.name)
            }.map { node ->
                convertToTypedAudioNode(node)
            }
        }

    override suspend fun getVideoNodesFromOutShares(
        lastHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            var handle: Long? = lastHandle
            val result = mutableListOf<MegaNode>()
            megaApi.getOutShares(sortOrderIntMapper(order)).map { megaShare ->
                megaApi.getMegaNodeByHandle(megaShare.nodeHandle)
            }.let { nodes ->
                for (node in nodes) {
                    if (node != null && node.handle != handle) {
                        handle = node.handle
                        result.add(node)
                    }
                }
            }
            result.filter { megaNode ->
                megaNode.isFile && filterByNodeName(false, megaNode.name)
            }.map { node ->
                convertToTypedVideoNode(node)
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
        typedFileNode?.let {
            fileGateway.getLocalFile(
                typedFileNode.name,
                typedFileNode.size,
                typedFileNode.modificationTime
            )?.path
        }

    override suspend fun deletePlaybackInformation(mediaId: Long) {
        playbackInfoMap.remove(mediaId)
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
            megaApi.getRootNode()?.let { rootNode ->
                megaApi.search(
                    parent = rootNode,
                    query = fileSuffix,
                    megaCancelToken = MegaCancelToken.createInstance(),
                    order = ORDER_DEFAULT_DESC
                )
            }?.map { megaNode ->
                subtitleFileInfoMapper(
                    id = megaNode.handle,
                    name = megaNode.name,
                    url = megaApi.httpServerGetLocalLink(megaNode),
                    parentName = megaApi.getParentNode(megaNode)?.name
                )
            } ?: emptyList()
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
        dbHandler.findContactByHandle(user.handle)?.let { megaContactDB ->
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
    ): List<TypedVideoNode>? =
        megaApi.getMegaNodeByHandle(nodeHandle = handle)?.let { parent ->
            megaApi.searchByType(
                parent,
                searchString,
                MegaCancelToken.createInstance(),
                recursive,
                sortOrderIntMapper(order),
                FILE_TYPE_VIDEO,
            ).map { node ->
                convertToTypedVideoNode(node)
            }
        }

    private suspend fun convertToTypedVideoNode(
        node: MegaNode,
        offline: Offline? = null,
    ): TypedVideoNode =
        typedVideoNodeMapper(
            fileNode = node.convertToFileNode(offline),
            node.duration
        )

    private suspend fun convertToTypedAudioNode(
        node: MegaNode,
        offline: Offline? = null,
    ): TypedAudioNode =
        typedAudioNodeMapper(
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

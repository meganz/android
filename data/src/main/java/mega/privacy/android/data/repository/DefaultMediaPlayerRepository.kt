package mega.privacy.android.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.mediaplayer.SubtitleFileInfoMapper
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.UnTypedNode
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
import java.io.File
import javax.inject.Inject

/**
 * Implementation of MediaPlayerRepository
 */
internal class DefaultMediaPlayerRepository @Inject constructor(
    private val megaApi: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    private val dbHandler: DatabaseHandler,
    private val nodeMapper: NodeMapper,
    private val cacheFolder: CacheFolderGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val fileGateway: FileGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val appPreferencesGateway: AppPreferencesGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val subtitleFileInfoMapper: SubtitleFileInfoMapper,
) : MediaPlayerRepository {

    private val playbackInfoMap = mutableMapOf<Long, PlaybackInformation>()

    override suspend fun getUnTypedNodeByHandle(handle: Long): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(handle)?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

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

    override suspend fun getAudioNodes(order: SortOrder): List<UnTypedNode> =
        getMediaNodes(isAudio = true, order = order)


    override suspend fun getVideoNodes(order: SortOrder): List<UnTypedNode> =
        getMediaNodes(isAudio = false, order = order)

    /**
     * Get media nodes
     *
     * @param isAudio true is audio, false is video
     * @param order [SortOrder]
     * @return media nodes
     */
    private suspend fun getMediaNodes(isAudio: Boolean, order: SortOrder): List<UnTypedNode> =
        withContext(ioDispatcher) {
            megaApi.searchByType(
                MegaCancelToken.createInstance(),
                sortOrderIntMapper(order),
                if (isAudio) {
                    FILE_TYPE_AUDIO
                } else {
                    FILE_TYPE_VIDEO
                },
                SEARCH_TARGET_ROOTNODE
            ).filter {
                it.isFile && filterByNodeName(isAudio, it.name)
            }.map { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getThumbnailFromMegaApi(nodeHandle: Long, path: String): Long? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { node ->
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("getThumbnailFromMegaApi") { it.nodeHandle }
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
                    val listener = continuation.getRequestListener("getThumbnailFromMegaApiFolder") { it.nodeHandle }
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

    override suspend fun getRubbishNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApi.getRubbishBinNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getInboxNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApi.getInboxNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getRootNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApi.getRootNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getRootNodeFromMegaApiFolder(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiFolder.getRootNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getParentNodeByHandle(parentHandle: Long): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(parentHandle)?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getParentNodeFromMegaApiFolder(parentHandle: Long): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(parentHandle)?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getAudioNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>? =
        getChildrenByParentHandle(isAudio = true, parentHandle = parentHandle, order = order)

    override suspend fun getVideoNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>? =
        getChildrenByParentHandle(isAudio = false, parentHandle = parentHandle, order = order)

    /**
     * Get nodes by parent handle
     *
     * @param isAudio true is audio, false is video
     * @param parentHandle parent handle
     * @param order [SortOrder]
     * @return nodes
     */
    private suspend fun getChildrenByParentHandle(
        isAudio: Boolean,
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApi.getChildren(parent, sortOrderIntMapper(order)).filter {
                    it.isFile && filterByNodeName(isAudio, it.name)
                }.map { node ->
                    convertToUnTypedNode(node)
                }
            }
        }

    override suspend fun getAudiosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>? =
        getChildrenByParentHandleFromMegaApiFolder(
            isAudio = true,
            parentHandle = parentHandle,
            order = order
        )

    override suspend fun getVideosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>? =
        getChildrenByParentHandleFromMegaApiFolder(
            isAudio = false,
            parentHandle = parentHandle,
            order = order
        )

    /**
     * Get nodes by parent handle from mega api folder
     *
     * @param isAudio true is audio, false is video
     * @param parentHandle parent handle
     * @param order [SortOrder]
     * @return nodes
     */
    private suspend fun getChildrenByParentHandleFromMegaApiFolder(
        isAudio: Boolean,
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApiFolder.getChildren(parent, sortOrderIntMapper(order)).filter {
                    it.isFile && filterByNodeName(isAudio, it.name)
                }.map { node ->
                    convertToUnTypedNode(node)
                }
            }
        }

    override suspend fun getAudioNodesFromPublicLinks(order: SortOrder): List<UnTypedNode> =
        getNodesFromPublicLinks(isAudio = true, order = order)

    override suspend fun getVideoNodesFromPublicLinks(order: SortOrder): List<UnTypedNode> =
        getNodesFromPublicLinks(isAudio = false, order = order)

    /**
     * Get media nodes from public links
     *
     * @param isAudio true is audio, false is video
     * @param order [SortOrder]
     * @return media nodes
     */
    private suspend fun getNodesFromPublicLinks(
        isAudio: Boolean,
        order: SortOrder,
    ): List<UnTypedNode> =
        withContext(ioDispatcher) {
            megaApi.getPublicLinks(sortOrderIntMapper(order)).filter {
                it.isFile && filterByNodeName(isAudio, it.name)
            }.map { node ->
                convertToUnTypedNode(node)
            }
        }

    override suspend fun getAudioNodesFromInShares(order: SortOrder): List<UnTypedNode> =
        getNodesFromInShares(isAudio = true, order = order)

    override suspend fun getVideoNodesFromInShares(order: SortOrder): List<UnTypedNode> =
        getNodesFromInShares(isAudio = false, order = order)

    /**
     * Get media nodes from InShares
     *
     * @param isAudio true is audio, false is video
     * @param order [SortOrder]
     * @return media nodes
     */
    private suspend fun getNodesFromInShares(
        isAudio: Boolean,
        order: SortOrder,
    ): List<UnTypedNode> =
        withContext(ioDispatcher) {
            megaApi.getInShares(sortOrderIntMapper(order)).filter {
                it.isFile && filterByNodeName(isAudio, it.name)
            }.map { node ->
                convertToUnTypedNode(node)
            }
        }

    override suspend fun getAudioNodesFromOutShares(
        lastHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode> =
        getNodesFromOutShares(isAudio = true, lastHandle = lastHandle, order = order)

    override suspend fun getVideoNodesFromOutShares(
        lastHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode> =
        getNodesFromOutShares(isAudio = false, lastHandle = lastHandle, order = order)

    /**
     * Get media nodes from OutShares
     *
     * @param isAudio true is audio, false is video
     * @param lastHandle the handle of last item
     * @param order [SortOrder]
     * @return media nodes
     */
    private suspend fun getNodesFromOutShares(
        isAudio: Boolean,
        lastHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode> =
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
                megaNode.isFile && filterByNodeName(isAudio, megaNode.name)
            }.map { node ->
                convertToUnTypedNode(node)
            }
        }

    override suspend fun getAudioNodesByEmail(email: String): List<UnTypedNode>? =
        getNodesByEmail(isAudio = true, email = email)

    override suspend fun getVideoNodesByEmail(email: String): List<UnTypedNode>? =
        getNodesByEmail(isAudio = false, email = email)

    /**
     * Get nodes by email
     *
     * @param isAudio true is audio, false is video
     * @param email email of account
     * @return List<UnTypedNode>?
     */
    private suspend fun getNodesByEmail(isAudio: Boolean, email: String): List<UnTypedNode>? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                megaApi.getInShares(megaUser).filter { megaNode ->
                    megaNode.isFile && filterByNodeName(isAudio, megaNode.name)
                }.map { node ->
                    convertToUnTypedNode(node)
                }
            }
        }

    override suspend fun getUserNameByEmail(email: String): String? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                getMegaUserNameDB(megaUser)
            }
        }

    override suspend fun getNodesByHandles(handles: List<Long>): List<UnTypedNode> =
        handles.mapNotNull { handle ->
            megaApi.getMegaNodeByHandle(handle)
        }.map { node ->
            convertToUnTypedNode(node)
        }

    override suspend fun megaApiHttpServerStop() = megaApi.httpServerStop()

    override suspend fun megaApiFolderHttpServerStop() = megaApiFolder.httpServerStop()

    override suspend fun megaApiHttpServerIsRunning(): Int = megaApi.httpServerIsRunning()

    override suspend fun megaApiFolderHttpServerIsRunning(): Int =
        megaApiFolder.httpServerIsRunning()

    override suspend fun megaApiHttpServerStart() = megaApi.httpServerStart()

    override suspend fun megaApiFolderHttpServerStart() = megaApiFolder.httpServerStart()

    override suspend fun megaApiHttpServerSetMaxBufferSize(bufferSize: Int) =
        megaApi.httpServerSetMaxBufferSize(bufferSize)

    override suspend fun megaApiFolderHttpServerSetMaxBufferSize(bufferSize: Int) =
        megaApiFolder.httpServerSetMaxBufferSize(bufferSize)

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

    override suspend fun getFileUrlByNodeHandle(handle: Long): String? =
        megaApi.getMegaNodeByHandle(handle)?.let { node ->
            megaApi.httpServerGetLocalLink(node)
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

    private fun filterByNodeName(isAudio: Boolean, name: String): Boolean =
        MimeTypeList.typeForName(name).let { mimeType ->
            if (isAudio) {
                mimeType.isAudio && !mimeType.isAudioNotSupported
            } else {
                mimeType.isVideo && !mimeType.isVideoNotSupported
            }
        }

    /**
     * Get the thumbnail cache file path
     * @param megaNode MegaNode
     * @return thumbnail cache file path
     */
    private fun getThumbnailCacheFilePath(megaNode: MegaNode) =
        cacheFolder.getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.let { thumbnail ->
            "$thumbnail${File.separator}${megaNode.getThumbnailFileName()}"
        }

    private fun getMegaUserNameDB(user: MegaUser): String? =
        dbHandler.findContactByHandle(user.handle.toString())?.let { megaContactDB ->
            when {
                megaContactDB.nickname.isNullOrEmpty().not() -> {
                    megaContactDB.nickname
                }
                megaContactDB.name.isNullOrEmpty().not() -> {
                    if (megaContactDB.lastName.isNullOrEmpty().not()) {
                        "$megaContactDB.name $megaContactDB.lastName"
                    } else {
                        megaContactDB.name
                    }
                }
                megaContactDB.lastName.isNullOrEmpty().not() -> {
                    megaContactDB.lastName
                }
                else -> {
                    megaContactDB.mail
                }
            } ?: user.email
        }

    private suspend fun convertToUnTypedNode(node: MegaNode): UnTypedNode =
        nodeMapper(
            node,
            ::getThumbnailCacheFilePath,
            megaApi::hasVersion,
            megaApi::getNumChildFolders,
            megaApi::getNumChildFiles,
            fileTypeInfoMapper,
            megaApi::isPendingShare,
            megaApi::isInRubbish,
        )


    companion object {
        private const val PREFERENCE_KEY_VIDEO_EXIT_TIME = "PREFERENCE_KEY_VIDEO_EXIT_TIME"
    }
}
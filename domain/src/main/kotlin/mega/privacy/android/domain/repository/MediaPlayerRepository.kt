package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.MegaException

/**
 * Repository for media player
 */
interface MediaPlayerRepository {

    /**
     * Get [UnTypedNode] by handle
     *
     * @param handle node handle
     * @return [UnTypedNode]?
     */
    suspend fun getUnTypedNodeByHandle(handle: Long): UnTypedNode?

    /**
     * Returns a URL to a node in the local HTTP proxy server for folder link from MegaApiFolder
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLinkForFolderLinkFromMegaApiFolder(nodeHandle: Long): String?

    /**
     * Returns a URL to a node in the local HTTP proxy server for folder link from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLinkForFolderLinkFromMegaApi(nodeHandle: Long): String?

    /**
     * Returns a URL to a node in the local HTTP proxy server from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLinkFromMegaApi(nodeHandle: Long): String?

    /**
     * Get all audio nodes
     *
     * @param order list order
     * @return audio nodes
     */
    suspend fun getAudioNodes(order: SortOrder): List<UnTypedNode>

    /**
     * Get all video nodes
     *
     * @param order list order
     * @return video nodes
     */
    suspend fun getVideoNodes(order: SortOrder): List<UnTypedNode>

    /**
     * Get thumbnail from MegaApiFolder
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromMegaApiFolder(nodeHandle: Long, path: String): Long?

    /**
     * Get thumbnail from MegaApi
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromMegaApi(nodeHandle: Long, path: String): Long?

    /**
     * Credentials whether is null
     *
     * @return true is null, otherwise is false
     */
    suspend fun areCredentialsNull(): Boolean

    /**
     * Get rubbish node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getRubbishNode(): UnTypedNode?

    /**
     * Get inbox node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getInboxNode(): UnTypedNode?

    /**
     * Get root node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getRootNode(): UnTypedNode?

    /**
     * MegaApiFolder gets root node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getRootNodeFromMegaApiFolder(): UnTypedNode?

    /**
     * MegaApiFolder gets parent node by handle
     *
     * @param parentHandle node handle
     * @return [UnTypedNode]?
     */
    suspend fun getParentNodeFromMegaApiFolder(parentHandle: Long): UnTypedNode?

    /**
     * Get audio children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[UnTypedNode]>?
     */
    suspend fun getAudioNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>?

    /**
     * Get video children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[UnTypedNode]>?
     */
    suspend fun getVideoNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>?

    /**
     * Get audio children by parent handle from MegaApiFolder
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[UnTypedNode]>?
     */
    suspend fun getAudiosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>?

    /**
     * Get video children by parent handle from MegaApiFolder
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[UnTypedNode]>?
     */
    suspend fun getVideosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<UnTypedNode>?

    /**
     * Get audio nodes from public links
     * @param order list order
     * @return List<[UnTypedNode]>
     */
    suspend fun getAudioNodesFromPublicLinks(order: SortOrder): List<UnTypedNode>

    /**
     * Get video nodes from public links
     * @param order list order
     * @return List<[UnTypedNode]>
     */
    suspend fun getVideoNodesFromPublicLinks(order: SortOrder): List<UnTypedNode>

    /**
     * Get audio nodes from InShares
     * @param order list order
     * @return List<[UnTypedNode]>
     */
    suspend fun getAudioNodesFromInShares(order: SortOrder): List<UnTypedNode>

    /**
     * Get video nodes from InShares
     * @param order list order
     * @return List<[UnTypedNode]>
     */
    suspend fun getVideoNodesFromInShares(order: SortOrder): List<UnTypedNode>

    /**
     * Get audio nodes from OutShares
     * @param order list order
     * @return List<[UnTypedNode]>
     */
    suspend fun getAudioNodesFromOutShares(lastHandle: Long, order: SortOrder): List<UnTypedNode>

    /**
     * Get video nodes from OutShares
     * @param order list order
     * @return List<[UnTypedNode]>
     */
    suspend fun getVideoNodesFromOutShares(lastHandle: Long, order: SortOrder): List<UnTypedNode>

    /**
     * Get audio nodes by email
     *
     * @param email email of account
     * @return List<[UnTypedNode]>?
     */
    suspend fun getAudioNodesByEmail(email: String): List<UnTypedNode>?

    /**
     * Get video nodes by email
     *
     * @param email email of account
     * @return List<[UnTypedNode]>?
     */
    suspend fun getVideoNodesByEmail(email: String): List<UnTypedNode>?

    /**
     * Get username by email
     *
     * @param email email of account
     * @return username
     */
    suspend fun getUserNameByEmail(email: String): String?

    /**
     * Get nodes by handles
     *
     * @param handles handle list
     * @return List<[UnTypedNode]>
     */
    suspend fun getNodesByHandles(handles: List<Long>): List<UnTypedNode>

    /**
     * MegaApi http server stop
     */
    suspend fun megaApiHttpServerStop()

    /**
     * MegaApiFolder http server stop
     */
    suspend fun megaApiFolderHttpServerStop()

    /**
     * MegaApi http server whether is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend fun megaApiHttpServerIsRunning(): Int

    /**
     * MegaApiFolder http server whether is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend fun megaApiFolderHttpServerIsRunning(): Int

    /**
     * MegaApi http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend fun megaApiHttpServerStart(): Boolean

    /**
     * MegaApiFolder http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend fun megaApiFolderHttpServerStart(): Boolean

    /**
     * MegaApi sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun megaApiHttpServerSetMaxBufferSize(bufferSize: Int)

    /**
     * MegaApiFolder sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun megaApiFolderHttpServerSetMaxBufferSize(bufferSize: Int)

    /**
     * Get the local folder path
     *
     * @param typedFileNode [TypedFileNode]
     * @return local file if it exists
     */
    suspend fun getLocalFilePath(typedFileNode: TypedFileNode?): String?

    /**
     * Delete the playback information
     *
     * @param mediaId the media id of deleted item
     */
    suspend fun deletePlaybackInformation(mediaId: Long)

    /**
     * Save the playback times
     */
    suspend fun savePlaybackTimes()

    /**
     * Update playback information
     *
     * @param playbackInformation the new playback information
     */
    suspend fun updatePlaybackInformation(playbackInformation: PlaybackInformation)

    /**
     * Monitor playback times
     *
     * @return Flow<Map<Long, PlaybackInformation>?>
     */
    fun monitorPlaybackTimes(): Flow<Map<Long, PlaybackInformation>?>

    /**
     * Get file url by node handle
     *
     * @param handle node handle
     * @return local link
     */
    suspend fun getFileUrlByNodeHandle(handle: Long): String?

    /**
     * Get subtitle file info list
     *
     * @param fileSuffix subtitle suffix
     * @return [SubtitleFileInfo] list
     */
    suspend fun getSubtitleFileInfoList(fileSuffix: String): List<SubtitleFileInfo>
}

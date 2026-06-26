package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import java.io.File

/**
 * The repository interface regarding Timeline.
 */
interface PhotosRepository {
    /**
     * Loads the next page of photos from the local database.
     *
     * This function retrieves the next set of [Photo] items based on the current pagination state.
     * It is intended to be called when additional photos need to be loaded (e.g. on scroll).
     */
    suspend fun loadNextPageOfPhotos()

    /**
     * Monitor paginated photos
     *
     * @return Flow of List of [Photo]
     */
    fun monitorPaginatedPhotos(): Flow<List<Photo>>

    /**
     * Monitor photos
     */
    @Deprecated("Please consider using monitorMediaTypedNodes")
    fun monitorPhotos(): Flow<List<Photo>>

    /**
     * Get public links count
     */
    suspend fun getPublicLinksCount(): Int

    /**
     * create default download folder
     */
    suspend fun buildDefaultDownloadDir(): File

    /**
     * Get Camera Upload Folder handle
     *
     * @return
     */
    suspend fun getCameraUploadFolderId(): Long?

    /**
     * Get Media Upload Folder handle
     *
     * @return
     */
    suspend fun getMediaUploadFolderId(): Long?

    /**
     * Get photo with [nodeId]
     */
    suspend fun getPhotoFromNodeID(
        nodeId: NodeId,
        albumPhotoId: AlbumPhotoId? = null,
        refresh: Boolean = false,
    ): Photo?

    /**
     * Get Photos from a folder
     *
     * @param folderId
     * @param searchString "*" search All
     * @param recursive True search Photo from sub folders, false only search current folder
     */
    suspend fun getPhotosByFolderId(
        folderId: NodeId,
        searchString: String = "",
        recursive: Boolean,
        isFromFolderLink: Boolean = false,
    ): List<Photo>

    /**
     * Get Photos By a list of id
     */
    suspend fun getPhotosByIds(ids: List<NodeId>): List<Photo>

    /**
     * Clear all photos cache
     */
    fun clearCache()

    /**
     * Get Chat Photos by chatId and message Id
     *
     * @param chatId id of the chat
     * @param messageId id of the messages
     *
     * @return photo
     */
    suspend fun getChatPhotoByMessageId(chatId: Long, messageId: Long): Photo?

    /**
     * Get Photos by public link
     *
     * @param link link
     *
     * @return photo
     */
    suspend fun getPhotoByPublicLink(link: String): Photo?

    /**
     * Get the preferences for Timeline Filter
     */
    suspend fun getTimelineFilterPreferences(): Map<String, String?>?

    /**
     * Set the preferences for Timeline Filter
     */
    suspend fun setTimelineFilterPreferences(preferences: Map<String, String>): String?

    /**
     * Monitor timeline nodes
     */
    fun monitorImageNodes(): Flow<List<ImageNode>>

    /**
     * Monitor timeline image nodes for the image viewer, derived from the in-memory
     * media nodes already loaded for the timeline grid ([monitorMediaTypedNodes]).
     *
     * Unlike [monitorImageNodes] this does not trigger a full-account media search or
     * eagerly serialize every node, so opening an image from the timeline is fast and
     * holds no serialized blobs in memory.
     */
    fun monitorTimelineImageNodes(): Flow<List<ImageNode>>

    /**
     * Get image node from cache
     * @param nodeId
     */
    suspend fun getImageNodeFromCache(nodeId: NodeId): ImageNode?

    /**
     * Get image node
     * @param nodeId
     */
    suspend fun fetchImageNode(
        nodeId: NodeId,
        filterSvg: Boolean = true,
        includeRubbishBin: Boolean = false,
    ): ImageNode?

    /**
     * Get image node
     * @param url
     */
    suspend fun fetchImageNode(url: String): ImageNode?

    /**
     * Monitor media discovery nodes
     */
    suspend fun getMediaDiscoveryNodes(parentId: NodeId, recursive: Boolean): List<ImageNode>

    /**
     * Monitor folder link media discovery nodes
     */
    suspend fun getPublicMediaDiscoveryNodes(
        parentId: NodeId,
        recursive: Boolean,
    ): List<ImageNode>

    /**
     * Monitor imageNodes from a folder
     */
    suspend fun fetchImageNodes(
        parentId: NodeId,
        order: SortOrder?,
        includeRubbishBin: Boolean = false,
    ): List<ImageNode>

    /**
     * Get imageNodes from files
     */
    suspend fun getImageNodesInFiles(files: List<File>): List<ImageNode>

    /**
     * Get imageNode by chatId and messageId
     *
     * @param chatId
     * @param messageId
     */
    suspend fun getImageNodeFromChatMessage(
        chatId: Long,
        messageId: Long,
    ): ImageNode?

    /**
     * Get link url by typedFileNode
     *
     * @param typedFileNode
     */
    suspend fun getHttpServerLocalLink(typedFileNode: TypedFileNode): String?

    /**
     * Check is hidden nodes onboarded
     */
    suspend fun isHiddenNodesOnboarded(): Boolean

    /**
     * Set hidden nodes onboarded
     */
    suspend fun setHiddenNodesOnboarded()

    /**
     * Monitor image result from cache
     */
    fun monitorImageResult(nodeId: NodeId): Flow<ImageResult>?

    /**
     * Save image result into cache
     */
    suspend fun saveImageResult(nodeId: NodeId, imageResult: ImageResult)

    /**
     * Clear the cached image result for a single node, but only if its download never
     * completed. Used to evict an abandoned (e.g. cancelled) partial result so the node
     * is re-fetched on the next access instead of serving a stale, partially loaded image.
     *
     * @param nodeId the node whose uncompleted cached result should be evicted
     */
    suspend fun clearImageResult(nodeId: NodeId)

    /**
     * Clear all image result from cache
     */
    fun clearImageResult(uncompletedOnly: Boolean)

    /**
     * Retrieve recent queries
     */
    suspend fun retrieveRecentQueries(): List<String>

    /**
     * Save search queries
     */
    suspend fun saveRecentQueries(queries: List<String>)

    /**
     * Monitor whether the CU has been shown in timeline screen
     */
    val cameraUploadShownFlow: Flow<Boolean>

    /**
     * Set the CU has been shown in timeline screen.
     */
    suspend fun setCameraUploadShown()

    /**
     * Monitor the enable camera upload banner dismissed timestamp.
     */
    val enableCameraUploadBannerDismissedTimestamp: Flow<Long?>

    /**
     * Set the enable camera upload banner dismissed timestamp.
     */
    suspend fun setEnableCameraUploadBannerDismissedTimestamp()

    /**
     * Reset the enable camera upload banner dismissed timestamp.
     */
    suspend fun resetEnableCameraUploadBannerDismissedTimestamp()

    /**
     * Retrieve all media in a list of [TypedNode]s.
     */
    suspend fun getMediaTypedNodes(): List<TypedNode>

    /**
     * Monitor the media in a list of [TypedNode]s.
     */
    val monitorMediaTypedNodes: Flow<List<TypedNode>>

    /**
     * Get media timeline sections
     */
    suspend fun getMediaTimelineSections(filter: MediaTimelineFilter): List<MediaTimelineSection>

    /**
     * List media timeline nodes for a single [section] using offset-based pagination.
     *
     * The query is scoped to the section's date range, so pagination happens independently within
     * the section: any position can be loaded directly by [offset] without paging through the items
     * before it.
     *
     * @param filter the [MediaTimelineFilter] describing the scope of the media to list
     * @param section the [MediaTimelineSection] whose media is being paginated
     * @param order the [SortOrder] to apply to the results
     * @param maxElements the maximum number of nodes to return for the page (0 = no limit)
     * @param offset the zero-based index, within the section, of the first node to return
     * @return the page of media nodes as a list of [TypedFileNode]
     */
    suspend fun listMediaNodesByPage(
        filter: MediaTimelineFilter,
        section: MediaTimelineSection,
        order: SortOrder,
        maxElements: Int,
        offset: Long,
    ): List<TypedFileNode>
}

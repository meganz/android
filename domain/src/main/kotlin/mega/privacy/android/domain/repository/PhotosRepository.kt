package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import java.io.File

/**
 * The repository interface regarding Timeline.
 */
interface PhotosRepository {
    /**
     * Monitor photos
     */
    fun monitorPhotos(): Flow<List<Photo>>

    /**
     * Refresh photos
     */
    fun refreshPhotos()

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
    suspend fun getPhotoFromNodeID(nodeId: NodeId, albumPhotoId: AlbumPhotoId? = null): Photo?

    /**
     * Get Photos from a folder
     *
     * @param folderId
     * @param recursive True search Photo from sub folders, false only search current folder
     */
    suspend fun getPhotosByFolderId(folderId: NodeId, recursive: Boolean): List<Photo>

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
}
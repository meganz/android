package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist

/**
 * Repository related to video section
 */
interface VideoSectionRepository {
    /**
     * Get all videos
     *
     * @param order the list order
     * @return typed video node list
     */
    suspend fun getAllVideos(order: SortOrder): List<TypedVideoNode>

    /**
     * Get video playlists
     *
     * @return video playlist lists
     */
    suspend fun getVideoPlaylists(): List<VideoPlaylist>

    /**
     * Create a video playlist
     *
     * @param title video playlist title
     * @return created video playlist
     */
    suspend fun createVideoPlaylist(title: String): VideoPlaylist

    /**
     * Add videos to the playlist
     *
     * @param playlistID playlist id
     * @param videoIDs added video ids
     *
     * @return the number of added videos
     */
    suspend fun addVideosToPlaylist(playlistID: NodeId, videoIDs: List<NodeId>): Int

    /**
     * Remove video playlists
     *
     * @param playlistIDs removed playlist ids
     */
    suspend fun removeVideoPlaylists(playlistIDs: List<NodeId>): List<Long>

    /**
     * Remove videos from the playlist
     *
     * @param playlistID playlist id
     * @param videoElementIDs removed video element ids
     * @return the number of removed videos
     */
    suspend fun removeVideosFromPlaylist(playlistID: NodeId, videoElementIDs: List<Long>): Int

    /**
     * Update video playlist title
     *
     * @param playlistID playlist id
     * @param newTitle new title
     * @return updated title
     */
    suspend fun updateVideoPlaylistTitle(playlistID: NodeId, newTitle: String): String

    /**
     * Monitor video playlist sets update
     *
     * @return a flow of all new video playlist set ids update
     */
    fun monitorSetsUpdates(): Flow<List<Long>>

    /**
     * Get video sets map that is used for saving set ids and all element ids of the set
     *
     * @return Map<NodeId, MutableSet<Long>>
     */
    fun getVideoSetsMap(): Map<NodeId, MutableSet<Long>>

    /**
     * Get video playlist map that is used for saving UserSets
     *
     * @return Map<Long, UserSet>
     */
    fun getVideoPlaylistsMap(): Map<Long, UserSet>

    /**
     * Get video playlist sets
     *
     * @return video playlist sets
     */
    suspend fun getVideoPlaylistSets(): List<UserSet>

    /**
     * Add video to the multiple video playlists
     *
     * @param playlistIDs playlist id list
     * @param videoID added video id
     *
     * @return the ids of the added video playlist which added the video
     */
    suspend fun addVideoToMultiplePlaylists(playlistIDs: List<Long>, videoID: Long): List<Long>

    /**
     * Save the data of video recently watched
     *
     * @param handle the handle of the video node
     * @param timestamp saved timestamp
     */
    suspend fun saveVideoRecentlyWatched(handle: Long, timestamp: Long)

    /**
     * Get the data of video recently watched
     *
     * @return the list of video nodes that includes the watched timestamp
     */
    suspend fun getRecentlyWatchedVideoNodes(): List<TypedVideoNode>

    /**
     * Clear the data of video recently watched
     */
    suspend fun clearRecentlyWatchedVideos()

    /**
     * Remove the item of recently watched
     *
     * @param handle removed item handle
     */
    suspend fun removeRecentlyWatchedItem(handle: Long)
}
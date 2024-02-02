package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
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
}
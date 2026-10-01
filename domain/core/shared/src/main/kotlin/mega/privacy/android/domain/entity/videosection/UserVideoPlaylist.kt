package mega.privacy.android.domain.entity.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import kotlin.time.Duration

/**
 * The video playlist that created by user
 *
 * @property id The playlist ID
 * @property title The playlist title
 * @property cover The playlist cover
 * @property creationTime The playlist creation time
 * @property modificationTime The playlist modification time
 */
data class UserVideoPlaylist(
    val id: NodeId,
    val title: String,
    val cover: Long?,
    val creationTime: Long,
    val modificationTime: Long,
    override val thumbnailList: List<NodeId>?,
    override val numberOfVideos: Int,
    override val totalDuration: Duration,
    override val videos: List<TypedVideoNode>?,
) : VideoPlaylist
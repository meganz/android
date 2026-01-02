package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

@Serializable
data object MediaMainNavKey : MainNavItemNavKey

@Serializable
data class AlbumContentNavKey(val id: Long?, val type: String?) : NavKey

@Serializable
data class VideoPlaylistDetailNavKey(val playlistId: NodeId, val type: PlaylistType) : NavKey
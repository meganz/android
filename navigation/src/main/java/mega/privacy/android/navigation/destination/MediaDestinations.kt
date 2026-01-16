package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

@Serializable
data object MediaMainNavKey : MainNavItemNavKey

@Serializable
data class AlbumContentNavKey(val id: Long?, val type: String?) : NavKey

@Serializable
data class VideoPlaylistDetailNavKey(val playlistHandle: Long, val type: PlaylistType) : NavKey

@Serializable
data object MediaSearchNavKey : NavKey

@Serializable
data class AlbumCoverSelectionNavKey(val albumId: Long) : NavKey

@Serializable
data class PhotosSelectionNavKey(
    val albumId: Long,
    val selectionMode: Int,
    val captureResult: Boolean = true,
) : NavKey {
    companion object {
        const val RESULT = "PhotosSelectionNavKey::result"
    }
}

@Serializable
@Parcelize
data object CameraUploadsProgressNavKey : NavKey, Parcelable

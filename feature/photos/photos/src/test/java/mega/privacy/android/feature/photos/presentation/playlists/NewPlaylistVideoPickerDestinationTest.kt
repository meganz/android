package mega.privacy.android.feature.photos.presentation.playlists

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.navigation.destination.SelectVideosForPlaylistNavKey
import mega.privacy.android.navigation.destination.VideoPlaylistDetailNavKey
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewPlaylistVideoPickerDestinationTest {
    private val playlistHandle = 1L

    @Test
    fun `test that newPlaylistVideoPickerDestination opens the legacy picker when the cloud explorer is unavailable`() {
        val destination = newPlaylistVideoPickerDestination(
            playlistHandle = playlistHandle,
            isCloudExplorerAvailable = false,
        )

        assertThat(destination).isEqualTo(
            SelectVideosForPlaylistNavKey(
                playlistHandle = playlistHandle,
                isNewlyCreated = true,
            )
        )
    }

    @Test
    fun `test that newPlaylistVideoPickerDestination opens the playlist detail as newly created when the cloud explorer is available`() {
        val destination = newPlaylistVideoPickerDestination(
            playlistHandle = playlistHandle,
            isCloudExplorerAvailable = true,
        )

        assertThat(destination).isEqualTo(
            VideoPlaylistDetailNavKey(
                playlistHandle = playlistHandle,
                type = PlaylistType.User,
                isNewlyCreated = true,
            )
        )
    }
}

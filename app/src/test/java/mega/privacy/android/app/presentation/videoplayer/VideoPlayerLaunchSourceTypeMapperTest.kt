package mega.privacy.android.app.presentation.videoplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerLaunchSourceType
import mega.privacy.android.app.presentation.videoplayer.model.toVideoPlayerLaunchSourceType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Covers [toVideoPlayerLaunchSourceType], which resolves the legacy adapter-type integers the
 * player is launched with into [VideoPlayerLaunchSourceType].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerLaunchSourceTypeMapperTest {

    @ParameterizedTest(name = "test that {0} maps to {1}")
    @MethodSource("provideAdapterTypes")
    fun `test that an adapter type maps to its launch source type`(
        adapterType: Int,
        expected: VideoPlayerLaunchSourceType,
    ) {
        assertThat(adapterType.toVideoPlayerLaunchSourceType()).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test that {0} maps to Unknown")
    @MethodSource("provideUnmappedValues")
    fun `test that an unrecognised adapter type maps to Unknown`(adapterType: Int) {
        assertThat(adapterType.toVideoPlayerLaunchSourceType())
            .isEqualTo(VideoPlayerLaunchSourceType.Unknown)
    }

    @Test
    fun `test that every launch source type except Unknown is reachable from an adapter type`() {
        val mapped = provideAdapterTypes()
            .map { it.get()[1] as VideoPlayerLaunchSourceType }
            .toList()
            .toSet()
        val expected = VideoPlayerLaunchSourceType.entries.toSet() -
                VideoPlayerLaunchSourceType.Unknown
        assertThat(mapped).isEqualTo(expected)
    }

    @Test
    fun `test that a colliding value maps to the meaning the video player reads`() {
        // 2035 and 2036 mean different things in Constants and NodeSourceTypeInt. The player
        // reads them as the Constants meaning; this test pins that down so a later attempt to
        // merge the two namespaces fails loudly here instead of silently rerouting a launch.
        assertThat(Constants.VERSIONS_ADAPTER).isEqualTo(NodeSourceTypeInt.VIDEO_PLAYLISTS_ADAPTER)
        assertThat(Constants.VERSIONS_ADAPTER.toVideoPlayerLaunchSourceType())
            .isEqualTo(VideoPlayerLaunchSourceType.Versions)

        assertThat(Constants.FROM_IMAGE_VIEWER)
            .isEqualTo(NodeSourceTypeInt.VIDEO_RECENTLY_WATCHED_ADAPTER)
        assertThat(Constants.FROM_IMAGE_VIEWER.toVideoPlayerLaunchSourceType())
            .isEqualTo(VideoPlayerLaunchSourceType.ImageViewer)
    }

    @Test
    fun `test that the invalid sentinel maps to Unknown`() {
        assertThat(Constants.INVALID_VALUE.toVideoPlayerLaunchSourceType())
            .isEqualTo(VideoPlayerLaunchSourceType.Unknown)
    }

    private fun provideAdapterTypes(): Stream<Arguments> = Stream.of(
        Arguments.of(Constants.OFFLINE_ADAPTER, VideoPlayerLaunchSourceType.Offline),
        Arguments.of(Constants.ZIP_ADAPTER, VideoPlayerLaunchSourceType.Zip),
        Arguments.of(
            NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
            VideoPlayerLaunchSourceType.FileBrowser
        ),
        Arguments.of(NodeSourceTypeInt.RUBBISH_BIN_ADAPTER, VideoPlayerLaunchSourceType.RubbishBin),
        Arguments.of(NodeSourceTypeInt.BACKUPS_ADAPTER, VideoPlayerLaunchSourceType.Backups),
        Arguments.of(NodeSourceTypeInt.LINKS_ADAPTER, VideoPlayerLaunchSourceType.Links),
        Arguments.of(
            NodeSourceTypeInt.INCOMING_SHARES_ADAPTER,
            VideoPlayerLaunchSourceType.IncomingShares
        ),
        Arguments.of(
            NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
            VideoPlayerLaunchSourceType.OutgoingShares
        ),
        Arguments.of(NodeSourceTypeInt.FAVOURITES_ADAPTER, VideoPlayerLaunchSourceType.Favourites),
        Arguments.of(Constants.CONTACT_FILE_ADAPTER, VideoPlayerLaunchSourceType.ContactFile),
        Arguments.of(Constants.FOLDER_LINK_ADAPTER, VideoPlayerLaunchSourceType.FolderLink),
        Arguments.of(NodeSourceTypeInt.FILE_LINK_ADAPTER, VideoPlayerLaunchSourceType.FileLink),
        Arguments.of(Constants.FROM_CHAT, VideoPlayerLaunchSourceType.Chat),
        Arguments.of(Constants.FROM_IMAGE_VIEWER, VideoPlayerLaunchSourceType.ImageViewer),
        Arguments.of(Constants.FROM_MEDIA_DISCOVERY, VideoPlayerLaunchSourceType.MediaDiscovery),
        Arguments.of(Constants.FROM_ALBUM_SHARING, VideoPlayerLaunchSourceType.AlbumSharing),
        Arguments.of(Constants.VERSIONS_ADAPTER, VideoPlayerLaunchSourceType.Versions),
        Arguments.of(Constants.RECENTS_ADAPTER, VideoPlayerLaunchSourceType.Recents),
        Arguments.of(Constants.RECENTS_BUCKET_ADAPTER, VideoPlayerLaunchSourceType.RecentsBucket),
        Arguments.of(Constants.SEARCH_BY_ADAPTER, VideoPlayerLaunchSourceType.Search),
        Arguments.of(Constants.VIDEO_BROWSE_ADAPTER, VideoPlayerLaunchSourceType.VideoBrowse),
    )

    private fun provideUnmappedValues(): Stream<Arguments> = Stream.of(
        // 0 is what a mocked Intent returns for an unstubbed getIntExtra, and it must not be
        // mistaken for a real source.
        Arguments.of(0),
        Arguments.of(Constants.INVALID_VALUE),
        Arguments.of(Int.MAX_VALUE),
        Arguments.of(NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER),
    )
}

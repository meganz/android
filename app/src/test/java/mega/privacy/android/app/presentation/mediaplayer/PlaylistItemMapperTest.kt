package mega.privacy.android.app.presentation.mediaplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.icon.pack.R
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaylistItemMapperTest {

    private lateinit var underTest: PlaylistItemMapper

    private val fileTypeIconMapper = mock<FileTypeIconMapper>()

    private val expectedNodeHandle: Long = 111
    private val expectedNodeName = "test"
    private val expectedExtension = "mp4"
    private val expectedIcon = R.drawable.ic_video_medium_solid
    private val expectedIndex = 0
    private val expectedType = 1
    private val expectedSize: Long = 999
    private val expectedDuration = 1000.seconds

    @BeforeAll
    fun setUp() {
        whenever(fileTypeIconMapper(expectedExtension)).thenReturn(expectedIcon)
        underTest = PlaylistItemMapper(fileTypeIconMapper)
    }

    @Test
    fun `test that correct values are returned when thumbnail is null`() = runTest {
        val expectedThumbnail: File? = null
        val actual = underTest(
            nodeHandle = expectedNodeHandle,
            nodeName = expectedNodeName,
            thumbnailFile = expectedThumbnail,
            index = expectedIndex,
            type = expectedType,
            size = expectedSize,
            duration = expectedDuration,
            fileExtension = expectedExtension
        )

        assertMappedPlaylistItem(actual, expectedThumbnail)
    }

    @Test
    fun `test that correct values are returned when thumbnail is not null`() = runTest {
        val expectedThumbnail: File = mock()

        val actual = underTest(
            expectedNodeHandle,
            expectedNodeName,
            expectedThumbnail,
            expectedIndex,
            expectedType,
            expectedSize,
            expectedDuration,
            fileExtension = expectedExtension
        )

        assertMappedPlaylistItem(actual, expectedThumbnail)
    }

    private fun assertMappedPlaylistItem(
        playlistITem: PlaylistItem,
        expectedThumbnail: File?,
    ) {
        playlistITem.let {
            Assertions.assertAll(
                "Grouped Assertions of ${playlistITem::class.simpleName}",
                { assertThat(it.nodeHandle).isEqualTo(expectedNodeHandle) },
                { assertThat(it.nodeName).isEqualTo(expectedNodeName) },
                { assertThat(it.thumbnail).isEqualTo(expectedThumbnail) },
                { assertThat(it.size).isEqualTo(expectedSize) },
                { assertThat(it.type).isEqualTo(expectedType) },
                { assertThat(it.duration).isEqualTo(expectedDuration) },
                { assertThat(it.icon).isEqualTo(expectedIcon) },
                { assertThat(it.index).isEqualTo(expectedIndex) }
            )
        }
    }
}
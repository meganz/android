package test.mega.privacy.android.app.presentation.mediaplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.mapper.toPlaylistItemMapper
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal class PlaylistItemMapperTest {

    @Test
    fun `test that correct values are returned when thumbnail is null`() = runTest {
        val expectedNodeHandle: Long = 111
        val expectedNodeName = "test"
        val expectedThumbnail: File? = null
        val expectedIndex = 0
        val expectedType = 1
        val expectedSize: Long = 999
        val expectedDuration = 1000.seconds

        val actual = toPlaylistItemMapper(
            expectedNodeHandle,
            expectedNodeName,
            expectedThumbnail,
            expectedIndex,
            expectedType,
            expectedSize,
            expectedDuration
        )

        assertThat(actual.nodeHandle).isEqualTo(expectedNodeHandle)
        assertThat(actual.nodeName).isEqualTo(expectedNodeName)
        assertThat(actual.thumbnail).isEqualTo(expectedThumbnail)
        assertThat(actual.index).isEqualTo(expectedIndex)
        assertThat(actual.type).isEqualTo(expectedType)
        assertThat(actual.size).isEqualTo(expectedSize)
        assertThat(actual.duration).isEqualTo(expectedDuration)
    }

    @Test
    fun `test that correct values are returned when thumbnail is not null`() = runTest {
        val expectedNodeHandle: Long = 111
        val expectedNodeName = "test"
        val expectedThumbnail: File = mock()
        val expectedIndex = 0
        val expectedType = 1
        val expectedSize: Long = 999
        val expectedDuration = 1000.seconds

        val actual = toPlaylistItemMapper(
            expectedNodeHandle,
            expectedNodeName,
            expectedThumbnail,
            expectedIndex,
            expectedType,
            expectedSize,
            expectedDuration
        )

        assertThat(actual.nodeHandle).isEqualTo(expectedNodeHandle)
        assertThat(actual.nodeName).isEqualTo(expectedNodeName)
        assertThat(actual.thumbnail).isEqualTo(expectedThumbnail)
        assertThat(actual.index).isEqualTo(expectedIndex)
        assertThat(actual.type).isEqualTo(expectedType)
        assertThat(actual.size).isEqualTo(expectedSize)
        assertThat(actual.duration).isEqualTo(expectedDuration)
    }
}
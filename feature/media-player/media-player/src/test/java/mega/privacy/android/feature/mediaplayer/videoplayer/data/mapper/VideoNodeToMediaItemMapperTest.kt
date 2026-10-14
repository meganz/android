package mega.privacy.android.feature.mediaplayer.videoplayer.data.mapper

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoNodeToMediaItemMapperTest {

    private val underTest = VideoNodeToMediaItemMapper()

    @Test
    fun `test that invoke puts the handle in the media id`() {
        val result = underTest(handle = TEST_HANDLE, uri = mock<Uri>())

        assertThat(result.mediaId).isEqualTo(TEST_HANDLE.toString())
    }

    @Test
    fun `test that invoke puts the file name in the display title`() {
        val result = underTest(handle = TEST_HANDLE, uri = mock<Uri>(), displayName = TEST_NAME)

        // Not the title: anything set here wins over the container's own metadata, so using the
        // title would hide the real one for good.
        assertThat(result.mediaMetadata.displayTitle.toString()).isEqualTo(TEST_NAME)
        assertThat(result.mediaMetadata.title).isNull()
    }

    @Test
    fun `test that invoke leaves the metadata alone when there is no file name`() {
        val result = underTest(handle = TEST_HANDLE, uri = mock<Uri>())

        assertThat(result.mediaMetadata.displayTitle).isNull()
    }

    @Test
    fun `test that invoke keeps the uri it was given`() {
        val uri = mock<Uri>()

        val result = underTest(handle = TEST_HANDLE, uri = uri)

        assertThat(result.localConfiguration?.uri).isEqualTo(uri)
    }

    @Test
    fun `test that invoke takes the media id and the display title off a node`() {
        val node = mock<TypedVideoNode> {
            on { id }.thenReturn(NodeId(TEST_HANDLE))
            on { name }.thenReturn(TEST_NAME)
        }

        val result = underTest(node = node, uri = mock<Uri>())

        assertThat(result.mediaId).isEqualTo(TEST_HANDLE.toString())
        assertThat(result.mediaMetadata.displayTitle.toString()).isEqualTo(TEST_NAME)
    }

    private companion object {
        const val TEST_HANDLE = 123456L
        const val TEST_NAME = "video.mp4"
    }
}

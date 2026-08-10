package mega.privacy.android.feature.mediaplayer.data.mapper

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.feature.mediaplayer.data.MediaHandleStore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AudioNodeToMediaItemMapperTest {

    private lateinit var underTest: AudioNodeToMediaItemMapper
    private lateinit var mediaHandleStore: MediaHandleStore

    private val uri: Uri = mock()

    @BeforeEach
    fun setUp() {
        mediaHandleStore = MediaHandleStore()
        underTest = AudioNodeToMediaItemMapper(mediaHandleStore)
    }

    @Test
    fun `test that invoke with node assigns a UUID media id mapped to the node handle`() {
        val handle = 123L
        val node = mock<TypedAudioNode>().also {
            whenever(it.id).thenReturn(NodeId(handle))
            whenever(it.name).thenReturn(null)
        }

        val result = underTest(node, uri)

        assertThat(mediaHandleStore.getHandle(result.mediaId)).isEqualTo(handle)
        assertThat(result.mediaId).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        )
    }

    @Test
    fun `test that invoke with node sets uri correctly`() {
        val node = mock<TypedAudioNode>().also {
            whenever(it.id).thenReturn(NodeId(1L))
        }

        val result = underTest(node, uri)

        assertThat(result.localConfiguration?.uri).isEqualTo(uri)
    }

    @Test
    fun `test that invoke with raw fields assigns a UUID media id mapped to the handle`() {
        val handle = 456L

        val result = underTest(handle = handle, uri = uri)

        assertThat(mediaHandleStore.getHandle(result.mediaId)).isEqualTo(handle)
        assertThat(result.mediaId).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        )
    }

    @Test
    fun `test that invoke with raw fields sets uri correctly`() {
        val result = underTest(handle = 1L, uri = uri)

        assertThat(result.localConfiguration?.uri).isEqualTo(uri)
    }

    @Test
    fun `test that invoke with node does not set media metadata title`() {
        val node = mock<TypedAudioNode>().also {
            whenever(it.id).thenReturn(NodeId(1L))
            whenever(it.name).thenReturn("my song.mp3")
        }

        val result = underTest(node, uri)

        assertThat(result.mediaMetadata.title).isNull()
    }

    @Test
    fun `test that invoke with raw fields does not set media metadata title`() {
        val result = underTest(handle = 1L, uri = uri)

        assertThat(result.mediaMetadata.title).isNull()
    }
}

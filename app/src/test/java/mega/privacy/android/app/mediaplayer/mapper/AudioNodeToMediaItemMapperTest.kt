package mega.privacy.android.app.mediaplayer.mapper

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioNodeToMediaItemMapperTest {

    private lateinit var underTest: AudioNodeToMediaItemMapper

    private val uri: Uri = mock()

    @BeforeEach
    fun setUp() {
        underTest = AudioNodeToMediaItemMapper()
    }

    @Test
    fun `test that invoke with node sets media id to node handle as string`() {
        val handle = 123L
        val node = mock<TypedAudioNode>().also {
            whenever(it.id).thenReturn(NodeId(handle))
        }

        val result = underTest(node, uri)

        assertThat(result.mediaId).isEqualTo(handle.toString())
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
    fun `test that invoke with raw fields sets media id to handle as string`() {
        val handle = 456L

        val result = underTest(handle = handle, uri = uri)

        assertThat(result.mediaId).isEqualTo(handle.toString())
    }

    @Test
    fun `test that invoke with raw fields sets uri correctly`() {
        val result = underTest(handle = 1L, uri = uri)

        assertThat(result.localConfiguration?.uri).isEqualTo(uri)
    }
}

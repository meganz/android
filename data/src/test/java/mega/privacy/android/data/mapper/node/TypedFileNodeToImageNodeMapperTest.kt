package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypedFileNodeToImageNodeMapperTest {

    private lateinit var underTest: TypedFileNodeToImageNodeMapper

    private val thumbnailFromServerMapper = mock<ThumbnailFromServerMapper>()
    private val previewFromServerMapper = mock<PreviewFromServerMapper>()
    private val fullImageFromServerMapper = mock<FullImageFromServerMapper>()
    private val megaApiGateway = mock<MegaApiGateway>()

    private val handle = 123L

    @BeforeAll
    fun setup() {
        underTest = TypedFileNodeToImageNodeMapper(
            thumbnailFromServerMapper = thumbnailFromServerMapper,
            previewFromServerMapper = previewFromServerMapper,
            fullImageFromServerMapper = fullImageFromServerMapper,
            megaApiGateway = megaApiGateway,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        thumbnailFromServerMapper,
        previewFromServerMapper,
        fullImageFromServerMapper,
        megaApiGateway,
    )

    private fun stubNode(): TypedFileNode = mock {
        on { id }.thenReturn(NodeId(handle))
        on { name }.thenReturn("photo.jpg")
        on { serializedData }.thenReturn("blob")
    }

    @Test
    fun `test that node fields are delegated from the source node`() {
        val node = stubNode()

        val result = underTest(node)

        assertThat(result.id).isEqualTo(NodeId(handle))
        assertThat(result.name).isEqualTo("photo.jpg")
    }

    @Test
    fun `test that thumbnail preview and full size paths are null`() {
        val node = stubNode()

        val result = underTest(node)

        assertThat(result.thumbnailPath).isNull()
        assertThat(result.previewPath).isNull()
        assertThat(result.fullSizePath).isNull()
    }

    @Test
    fun `test that latitude and longitude default to zero`() {
        val node = stubNode()

        val result = underTest(node)

        assertThat(result.latitude).isEqualTo(0.0)
        assertThat(result.longitude).isEqualTo(0.0)
    }

    @Test
    fun `test that downloadThumbnail resolves the mega node lazily by handle`() = runTest {
        val node = stubNode()
        val megaNode = mock<MegaNode>()
        whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
        whenever(thumbnailFromServerMapper(megaNode)).thenReturn { path -> "thumb-$path" }

        val result = underTest(node).downloadThumbnail("p")

        assertThat(result).isEqualTo("thumb-p")
    }

    @Test
    fun `test that downloadPreview resolves the mega node lazily by handle`() = runTest {
        val node = stubNode()
        val megaNode = mock<MegaNode>()
        whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
        whenever(previewFromServerMapper(megaNode)).thenReturn { path -> "preview-$path" }

        val result = underTest(node).downloadPreview("p")

        assertThat(result).isEqualTo("preview-p")
    }

    @Test
    fun `test that downloadFullImage resolves the mega node lazily and emits progress`() = runTest {
        val node = stubNode()
        val megaNode = mock<MegaNode>()
        val progress = ImageProgress.Completed("p")
        whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
        whenever(fullImageFromServerMapper(megaNode)).thenReturn { _, _, _ -> flowOf(progress) }

        val result = underTest(node).downloadFullImage("p", true) {}.toList()

        assertThat(result).containsExactly(progress)
    }

    @Test
    fun `test that downloadThumbnail throws when the mega node cannot be resolved`() = runTest {
        val node = stubNode()
        whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(null)

        val exception = runCatching { underTest(node).downloadThumbnail("p") }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
    }
}

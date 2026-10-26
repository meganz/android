package mega.privacy.android.app.fetcher

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.PreviewRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetChatThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaThumbnailFetcherTest {
    private val getThumbnailUseCase: GetThumbnailUseCase = mock()
    private val getPublicNodeThumbnailUseCase: GetPublicNodeThumbnailUseCase = mock()
    private val getChatThumbnailUseCase: GetChatThumbnailUseCase = mock()
    private val getPreviewUseCase: GetPreviewUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private lateinit var underTest: MegaThumbnailFetcher

    @BeforeEach
    fun resetMocks() = reset(
        getThumbnailUseCase,
        getPublicNodeThumbnailUseCase,
        getChatThumbnailUseCase,
        getPreviewUseCase,
        getNodeByIdUseCase
    )

    @Test
    fun `test that throw NullPointerException when fetch cloud drive node and getThumbnailUseCase returns null`() =
        runTest {
            val request: ThumbnailRequest = mock()
            underTest = MegaThumbnailFetcher(
                request,
                { getThumbnailUseCase },
                { getPublicNodeThumbnailUseCase },
                { getChatThumbnailUseCase },
                { getPreviewUseCase },
                { getNodeByIdUseCase }
            )
            whenever(request.isPublicNode).thenReturn(true)
            whenever(getPublicNodeThumbnailUseCase(any(), any())).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getThumbnailUseCase)
            verifyNoInteractions(getChatThumbnailUseCase)
        }

    @Test
    fun `test that throw NullPointerException when fetch public node and getPublicNodeThumbnailUseCase returns null`() =
        runTest {
            val request: ThumbnailRequest = mock()
            underTest = MegaThumbnailFetcher(
                request,
                { getThumbnailUseCase },
                { getPublicNodeThumbnailUseCase },
                { getChatThumbnailUseCase },
                { getPreviewUseCase },
                { getNodeByIdUseCase }
            )
            whenever(request.isPublicNode).thenReturn(false)
            whenever(getThumbnailUseCase(any(), any())).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getPublicNodeThumbnailUseCase)
            verifyNoInteractions(getChatThumbnailUseCase)
        }

    @Test
    fun `test that throw NullPointerException when fetch chat node and getChatThumbnailUseCase returns null`() =
        runTest {
            val request: ChatThumbnailRequest = mock()
            underTest = MegaThumbnailFetcher(
                request,
                { getThumbnailUseCase },
                { getPublicNodeThumbnailUseCase },
                { getChatThumbnailUseCase },
                { getPreviewUseCase },
                { getNodeByIdUseCase }
            )
            whenever(getChatThumbnailUseCase(any(), any())).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getThumbnailUseCase)
            verifyNoInteractions(getPublicNodeThumbnailUseCase)
        }

    @Test
    fun `test that throw NullPointerException when fetch preview and the node is not found`() =
        runTest {
            underTest = previewFetcher()
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getPreviewUseCase)
        }

    @Test
    fun `test that preview is not requested when the node has none`() = runTest {
        val node = mock<TypedFileNode>()
        whenever(node.hasPreview).thenReturn(false)
        underTest = previewFetcher()
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)

        // The repository does not short-circuit previews the way it does thumbnails, so without
        // this check every track without a preview would cost a failed SDK call.
        assertThrows<NullPointerException> { underTest.fetch() }
        verifyNoInteractions(getPreviewUseCase)
    }

    private fun previewFetcher() = MegaThumbnailFetcher(
        PreviewRequest(NodeId(1L)),
        { getThumbnailUseCase },
        { getPublicNodeThumbnailUseCase },
        { getChatThumbnailUseCase },
        { getPreviewUseCase },
        { getNodeByIdUseCase }
    )
}

package mega.privacy.android.app.fetcher

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.thumbnailpreview.GetChatThumbnailUseCase
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
    private lateinit var underTest: MegaThumbnailFetcher

    @BeforeEach
    fun resetMocks() = reset(
        getThumbnailUseCase,
        getPublicNodeThumbnailUseCase,
        getChatThumbnailUseCase
    )

    @Test
    fun `test that throw NullPointerException when fetch cloud drive node and getThumbnailUseCase returns null`() =
        runTest {
            val request: ThumbnailRequest = mock()
            underTest = MegaThumbnailFetcher(
                request,
                { getThumbnailUseCase },
                { getPublicNodeThumbnailUseCase },
                { getChatThumbnailUseCase }
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
                { getChatThumbnailUseCase }
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
                { getChatThumbnailUseCase }
            )
            whenever(getChatThumbnailUseCase(any(), any())).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getThumbnailUseCase)
            verifyNoInteractions(getPublicNodeThumbnailUseCase)
        }
}
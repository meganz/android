package test.mega.privacy.android.app.fetcher

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.fetcher.MegaThumbnailFetcher
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import org.junit.jupiter.api.BeforeAll
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
    private val request: ThumbnailRequest = mock()
    private val getThumbnailUseCase: GetThumbnailUseCase = mock()
    private val getPublicNodeThumbnailUseCase: GetPublicNodeThumbnailUseCase = mock()
    private lateinit var underTest: MegaThumbnailFetcher

    @BeforeAll
    fun setUp() {
        underTest = MegaThumbnailFetcher(
            request,
            { getThumbnailUseCase },
            { getPublicNodeThumbnailUseCase }
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        request,
        getThumbnailUseCase,
        getPublicNodeThumbnailUseCase
    )

    @Test
    fun `test that throw NullPointerException when fetch cloud drive node and getThumbnailUseCase returns null`() =
        runTest {
            whenever(request.isPublicNode).thenReturn(true)
            whenever(getPublicNodeThumbnailUseCase(any(), any())).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getThumbnailUseCase)
        }

    @Test
    fun `test that throw NullPointerException when fetch public node and getPublicNodeThumbnailUseCase returns null`() =
        runTest {
            whenever(request.isPublicNode).thenReturn(false)
            whenever(getThumbnailUseCase(any(), any())).thenReturn(null)
            assertThrows<NullPointerException> { underTest.fetch() }
            verifyNoInteractions(getPublicNodeThumbnailUseCase)
        }
}
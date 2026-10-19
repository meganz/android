package mega.privacy.android.app.listeners.global

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.usecase.pdf.CheckIfShouldDeleteLastPageViewedInPdfUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GlobalOnNodesUpdateHandlerTest {

    private val ratingHandler = mock<RatingHandlerImpl>()
    private val checkIfShouldDeleteLastPageViewedInPdfUseCase =
        mock<CheckIfShouldDeleteLastPageViewedInPdfUseCase>()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var underTest: GlobalOnNodesUpdateHandler

    @BeforeEach
    fun setUp() {
        reset(ratingHandler, checkIfShouldDeleteLastPageViewedInPdfUseCase)
        underTest = GlobalOnNodesUpdateHandler(
            appContext = mock<Context>(),
            applicationScope = testScope,
            checkIfShouldDeleteLastPageViewedInPdfUseCase = checkIfShouldDeleteLastPageViewedInPdfUseCase,
            megaApi = mock<MegaApiAndroid>(),
            megaNavigator = mock<MegaNavigator>(),
            ratingHandler = ratingHandler,
        )
    }

    @Test
    fun `test that invoke does not call the sharing rating check on the calling thread`() =
        runTest(testDispatcher) {
            underTest(arrayListOf(publicLinkNode()))

            verify(ratingHandler, never()).showRatingBaseOnSharing()

            advanceUntilIdle()

            verify(ratingHandler).showRatingBaseOnSharing()
        }

    @Test
    fun `test that invoke calls the sharing rating check once when many nodes have a new public link`() =
        runTest(testDispatcher) {
            underTest(arrayListOf(publicLinkNode(), publicLinkNode(), publicLinkNode()))
            advanceUntilIdle()

            verify(ratingHandler, times(1)).showRatingBaseOnSharing()
        }

    @Test
    fun `test that invoke does not call the sharing rating check when the public link is removed`() =
        runTest(testDispatcher) {
            underTest(arrayListOf(publicLinkNode(publicLink = null)))
            advanceUntilIdle()

            verify(ratingHandler, never()).showRatingBaseOnSharing()
        }

    @Test
    fun `test that invoke does not call the sharing rating check when no node has a public link change`() =
        runTest(testDispatcher) {
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
                on { hasChanged(any()) } doReturn false
            }

            underTest(arrayListOf(node))
            advanceUntilIdle()

            verify(ratingHandler, never()).showRatingBaseOnSharing()
        }

    @Test
    fun `test that invoke does not throw when the sharing rating check fails`() =
        runTest(testDispatcher) {
            whenever(ratingHandler.showRatingBaseOnSharing()).doThrow(RuntimeException())

            underTest(arrayListOf(publicLinkNode()))
            advanceUntilIdle()

            verify(ratingHandler).showRatingBaseOnSharing()
        }

    @Test
    fun `test that invoke does nothing when the node list is null`() = runTest(testDispatcher) {
        underTest(null)
        advanceUntilIdle()

        verify(ratingHandler, never()).showRatingBaseOnSharing()
    }

    private fun publicLinkNode(publicLink: String? = "https://mega.nz/file/abc"): MegaNode =
        mock {
            on { isInShare } doReturn false
            on { hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK.toLong()) } doReturn true
            on { this.publicLink } doReturn publicLink
        }
}

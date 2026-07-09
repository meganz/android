package mega.privacy.android.feature.documentscanner.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.PageQuality
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.usecase.MonitorScanSessionUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.RemoveScannedPageUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.ReorderScannedPagesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanReviewViewModelTest {

    private lateinit var underTest: ScanReviewViewModel

    private val monitorScanSessionUseCase = mock<MonitorScanSessionUseCase>()
    private val removeScannedPageUseCase = mock<RemoveScannedPageUseCase>()
    private val reorderScannedPagesUseCase = mock<ReorderScannedPagesUseCase>()

    private val sessionFlow = MutableStateFlow(emptySession())

    @BeforeEach
    fun setUp() {
        reset(monitorScanSessionUseCase, removeScannedPageUseCase, reorderScannedPagesUseCase)
        sessionFlow.value = emptySession()
        whenever(monitorScanSessionUseCase()).thenReturn(sessionFlow)
        underTest = ScanReviewViewModel(
            monitorScanSessionUseCase,
            removeScannedPageUseCase,
            reorderScannedPagesUseCase,
        )
    }

    @Test
    fun `test that session pages map to review items with 1-based page numbers`() = runTest {
        sessionFlow.value = ScanSession(
            id = "s",
            pages = listOf(page("a", 0), page("b", 1)),
            captureMode = CaptureMode.AUTO,
            createdAt = 0L,
        )

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.pages.map { it.id }).containsExactly("a", "b").inOrder()
            assertThat(state.pages.map { it.pageNumber }).containsExactly(1, 2).inOrder()
            assertThat(state.pages.map { it.thumbnailUri }).containsExactly("a_thumb", "b_thumb").inOrder()
        }
    }

    @Test
    fun `test that onDeletePage delegates to removeScannedPageUseCase`() = runTest {
        underTest.onDeletePage("a")

        verify(removeScannedPageUseCase).invoke("a")
    }

    @Test
    fun `test that onReorder delegates to reorderScannedPagesUseCase`() = runTest {
        underTest.onReorder(fromIndex = 0, toIndex = 2)

        verify(reorderScannedPagesUseCase).invoke(0, 2)
    }

    @Test
    fun `test that onReorder to the same index does nothing`() = runTest {
        underTest.onReorder(fromIndex = 1, toIndex = 1)

        verify(reorderScannedPagesUseCase, org.mockito.kotlin.never()).invoke(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
        )
    }

    private companion object {
        fun emptySession() = ScanSession(
            id = "empty",
            pages = emptyList(),
            captureMode = CaptureMode.AUTO,
            createdAt = 0L,
        )

        fun page(id: String, order: Int) = ScannedPage(
            id = id,
            imageUri = "$id.jpg",
            thumbnailUri = "${id}_thumb",
            order = order,
            capturedAt = 0L,
            quality = PageQuality.GOOD,
            boundary = null,
        )
    }
}

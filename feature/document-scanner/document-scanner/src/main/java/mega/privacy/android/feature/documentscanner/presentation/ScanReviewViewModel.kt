package mega.privacy.android.feature.documentscanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.usecase.MonitorScanSessionUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.RemoveScannedPageUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.ReorderScannedPagesUseCase
import mega.privacy.android.feature.documentscanner.presentation.model.ReviewPageUiItem
import mega.privacy.android.feature.documentscanner.presentation.model.ScanReviewUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the page-review screen. Observes the scan session and exposes the
 * captured pages for the grid, plus a delete action.
 */
@HiltViewModel
internal class ScanReviewViewModel @Inject constructor(
    private val monitorScanSessionUseCase: MonitorScanSessionUseCase,
    private val removeScannedPageUseCase: RemoveScannedPageUseCase,
    private val reorderScannedPagesUseCase: ReorderScannedPagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanReviewUiState())
    val uiState: StateFlow<ScanReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            monitorScanSessionUseCase()
                .catch { Timber.e(it, "[DocScanner] Review session monitor failed") }
                .collect { session ->
                    _uiState.update { state ->
                        state.copy(pages = session.pages.map(ScannedPage::toReviewItem))
                    }
                }
        }
    }

    /** Delete the page with [pageId] from the session (its files are removed too). */
    fun onDeletePage(pageId: String) {
        viewModelScope.launch {
            runCatching { removeScannedPageUseCase(pageId) }
                .onFailure { Timber.e(it, "[DocScanner] Delete page failed") }
        }
    }

    /** Move the page at [fromIndex] to [toIndex], re-indexing the rest. */
    fun onReorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch {
            runCatching { reorderScannedPagesUseCase(fromIndex, toIndex) }
                .onFailure { Timber.e(it, "[DocScanner] Reorder pages failed") }
        }
    }
}

private fun ScannedPage.toReviewItem() = ReviewPageUiItem(
    id = id,
    imageUri = imageUri,
    thumbnailUri = thumbnailUri,
    pageNumber = order + 1,
)

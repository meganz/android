package mega.privacy.android.feature.documentscanner.presentation.model

/**
 * UI state for the page-review screen.
 *
 * @property pages The captured pages in order, as lightweight items for the grid.
 */
internal data class ScanReviewUiState(
    val pages: List<ReviewPageUiItem> = emptyList(),
)

/**
 * A single captured page as shown in the review screen.
 *
 * @property id Page id, used for delete/reorder/retake actions.
 * @property imageUri URI of the full-resolution page image, shown in the preview.
 * @property thumbnailUri URI of the page thumbnail, shown in the strip.
 * @property pageNumber 1-based position.
 */
internal data class ReviewPageUiItem(
    val id: String,
    val imageUri: String,
    val thumbnailUri: String,
    val pageNumber: Int,
)

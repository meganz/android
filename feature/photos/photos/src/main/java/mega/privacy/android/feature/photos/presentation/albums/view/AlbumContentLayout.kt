package mega.privacy.android.feature.photos.presentation.albums.view

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.feature.photos.model.PhotoUiState

/**
 * Defines the visual arrangement pattern for Album photos
 *
 * https://www.figma.com/design/AOF1eI4dWuo2cX536lIyOz/-DSN-84--Photos-%E2%80%94-Albums?node-id=572-5583&p=f&t=NrzwjPcFYkSznLDk-0
 */
@Immutable
sealed class AlbumContentLayout(
    val content: ImmutableList<PhotoUiState>,
) {
    abstract val key: String

    /**
     * The primary (larger) tile appears at the start of the row or column,
     * followed by 2 smaller tiles at the end
     */
    data class HighlightStart(
        val photos: ImmutableList<PhotoUiState>,
    ) : AlbumContentLayout(photos) {
        override val key: String =
            "layout_highlight_start_${photos.joinToString { it.id.toString() }}"
    }


    /**
     * All tiles appear in the same row or column with the same size
     */
    data class Uniform(
        val photos: ImmutableList<PhotoUiState>,
    ) : AlbumContentLayout(photos) {
        override val key: String = "layout_uniform_${photos.joinToString { it.id.toString() }}"
    }


    /**
     * The primary (larger) tile appears at the end of the row or column,
     * followed by 2 smaller tiles at the start.
     */
    data class HighlightEnd(
        val photos: ImmutableList<PhotoUiState>,
    ) : AlbumContentLayout(photos) {
        override val key: String =
            "layout_highlight_end_${photos.joinToString { it.id.toString() }}"
    }
}

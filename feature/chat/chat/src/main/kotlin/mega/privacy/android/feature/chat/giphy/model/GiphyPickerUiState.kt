package mega.privacy.android.feature.chat.giphy.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.domain.entity.chat.Giphy

/**
 * UI state of the GIPHY picker.
 *
 * @property query Text currently typed in the search field. Blank means trending GIFs are shown.
 * @property gifs GIFs loaded so far for the current query.
 * @property isLoading True while a page of GIFs is being requested.
 * @property isEndOfList True once the last page for the current query has been loaded.
 * @property emptyState Reason why there are no GIFs to show, or null when there are.
 */
@Stable
internal data class GiphyPickerUiState(
    val query: String = "",
    val gifs: ImmutableList<Giphy> = persistentListOf(),
    val isLoading: Boolean = true,
    val isEndOfList: Boolean = false,
    val emptyState: GiphyEmptyState? = null,
)

/**
 * Reason why the GIPHY picker has no GIFs to show.
 */
internal enum class GiphyEmptyState {
    /**
     * The request succeeded but returned no GIFs.
     */
    NoResults,

    /**
     * The request failed, typically because the GIPHY service is unavailable.
     */
    ServerDown,
}

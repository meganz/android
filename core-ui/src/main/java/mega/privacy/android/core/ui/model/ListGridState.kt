package mega.privacy.android.core.ui.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState

/**
 * State holder for both LazyListState and LazyGridState.
 * @property lazyListState
 * @property lazyGridState
 */
data class ListGridState(
    val lazyListState: LazyListState = LazyListState(),
    val lazyGridState: LazyGridState = LazyGridState(),
)
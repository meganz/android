package mega.privacy.android.app.presentation.search.view

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.model.SearchState
import mega.privacy.android.core.ui.controls.chips.TextButtonWithIconChip
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.search.SearchCategory

/**
 * Search filter chips view
 *
 * @param selectedFilter selected SearchFilter
 * @param filters [SearchFilter]
 * @param updateFilter updates filter selection
 */
@Composable
fun SearchFilterChipsView(
    filters: List<SearchFilter>,
    updateFilter: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
    selectedFilter: SearchFilter? = null,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filters.size) { item ->
            val selectedChip = filters[item]
            val isChecked = selectedFilter == selectedChip
            TextButtonWithIconChip(
                isChecked = isChecked,
                text = filters[item].name,
                onClick = {
                    updateFilter(selectedChip)
                },
                iconId = if (isChecked) R.drawable.icon_check else null
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun PreviewSearchFilterChipsView(
    @PreviewParameter(SearchStatePreviewsProvider::class) viewState: SearchState,
) {
    var state by mutableStateOf(viewState)
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SearchFilterChipsView(
            selectedFilter = state.selectedFilter,
            filters = state.filters,
            updateFilter = {
                state = state.copy(selectedFilter = it)
            },
        )
    }
}

internal class SearchStatePreviewsProvider : PreviewParameterProvider<SearchState> {
    override val values: Sequence<SearchState>
        get() = listOf(searchState).asSequence()

    companion object {
        val searchState = SearchState(
            nodes = null,
            searchParentHandle = 12345L,
            isInProgress = false,
            searchQuery = "query",
            textSubmitted = true,
            searchDepth = 1,
            selectedFilter = SearchFilter(SearchCategory.IMAGES, "Images"),
            filters = listOf(
                SearchFilter(SearchCategory.IMAGES, "Images"),
                SearchFilter(SearchCategory.DOCUMENTS, "Docs"),
                SearchFilter(SearchCategory.AUDIO, "Audio"),
                SearchFilter(SearchCategory.VIDEO, "Video"),
            )
        )
    }
}
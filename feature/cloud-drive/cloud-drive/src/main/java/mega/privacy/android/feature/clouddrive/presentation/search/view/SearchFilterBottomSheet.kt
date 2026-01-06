package mega.privacy.android.feature.clouddrive.presentation.search.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.HeaderTextStyle
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.list.PrimaryHeaderListItem
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.labelResId
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.titleResId
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterResult
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterType
import mega.privacy.android.icon.pack.IconPack

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchFilterBottomSheetContent(
    filterType: SearchFilterType,
    selectedTypeFilter: TypeFilterOption?,
    selectedDateModifiedFilter: DateFilterOption?,
    selectedDateAddedFilter: DateFilterOption?,
    onFilterSelected: (SearchFilterResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        stickyHeader {
            BoxSurface(
                surfaceColor = SurfaceColor.Surface1
            ) {
                PrimaryHeaderListItem(
                    modifier = Modifier.testTag(SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG),
                    text = stringResource(filterType.titleResId),
                    headerTextStyle = HeaderTextStyle.Medium,
                    enableClick = false
                )
            }
        }

        when (filterType) {
            SearchFilterType.TYPE -> {
                items(TypeFilterOption.entries) { option ->
                    FilterOptionItem(
                        option = option,
                        isSelected = option == selectedTypeFilter,
                        labelResId = option.labelResId,
                        onOptionSelected = { onFilterSelected(SearchFilterResult.Type(it)) }
                    )
                }
            }

            SearchFilterType.LAST_MODIFIED -> {
                items(DateFilterOption.entries) { option ->
                    FilterOptionItem(
                        option = option,
                        isSelected = option == selectedDateModifiedFilter,
                        labelResId = option.labelResId,
                        onOptionSelected = { onFilterSelected(SearchFilterResult.DateModified(it)) }
                    )
                }
            }

            SearchFilterType.DATE_ADDED -> {
                items(DateFilterOption.entries) { option ->
                    FilterOptionItem(
                        option = option,
                        isSelected = option == selectedDateAddedFilter,
                        labelResId = option.labelResId,
                        onOptionSelected = { onFilterSelected(SearchFilterResult.DateAdded(it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> FilterOptionItem(
    option: T,
    isSelected: Boolean,
    labelResId: Int,
    onOptionSelected: (T?) -> Unit,
) {
    OneLineListItem(
        modifier = Modifier.testTag("${SEARCH_FILTER_OPTION_TAG}_$option"),
        text = stringResource(labelResId),
        trailingElement = if (isSelected) {
            {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
                    tint = IconColor.Secondary
                )
            }
        } else null,
        onClickListener = { onOptionSelected(if (isSelected) null else option) }
    )
}

@CombinedThemePreviews
@Composable
private fun SearchFilterBottomSheetContentTypePreview() {
    AndroidThemeForPreviews {
        SearchFilterBottomSheetContent(
            filterType = SearchFilterType.TYPE,
            selectedTypeFilter = TypeFilterOption.Documents,
            selectedDateModifiedFilter = null,
            selectedDateAddedFilter = null,
            onFilterSelected = {}
        )
    }
}

internal const val SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG = "search_filter_bottom_sheet:title"
internal const val SEARCH_FILTER_OPTION_TAG = "search_filter_bottom_sheet:option"

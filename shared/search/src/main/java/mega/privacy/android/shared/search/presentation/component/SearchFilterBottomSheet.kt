package mega.privacy.android.shared.search.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.HeaderTextStyle
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.list.PrimaryHeaderListItem
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.search.presentation.model.SearchFilterOption
import mega.privacy.android.shared.search.presentation.model.SearchFilterOptions

/**
 * Bottom sheet content listing the options of a single filter.
 *
 * Emits [onOptionSelected] with the filter id and the chosen option id, or null when the user
 * taps the already-selected option (clearing the filter). The consumer maps these ids back to
 * its own filter values.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchFilterBottomSheetContent(
    filterOptions: SearchFilterOptions,
    onOptionSelected: (filterId: String, optionId: String?) -> Unit,
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
                    text = filterOptions.title.text,
                    headerTextStyle = HeaderTextStyle.Medium,
                    enableClick = false
                )
            }
        }

        items(filterOptions.options) { option ->
            val isSelected = option.id == filterOptions.selectedOptionId
            FilterOptionItem(
                option = option,
                isSelected = isSelected,
                onClick = {
                    onOptionSelected(
                        filterOptions.id,
                        if (isSelected) null else option.id
                    )
                }
            )
        }
    }
}

@Composable
private fun FilterOptionItem(
    option: SearchFilterOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    OneLineListItem(
        modifier = Modifier.testTag("${SEARCH_FILTER_OPTION_TAG}_${option.id}"),
        text = option.label.text,
        trailingElement = if (isSelected) {
            {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
                    tint = IconColor.Secondary
                )
            }
        } else null,
        onClickListener = onClick
    )
}

@CombinedThemePreviews
@Composable
private fun SearchFilterBottomSheetContentPreview() {
    AndroidThemeForPreviews {
        SearchFilterBottomSheetContent(
            filterOptions = SearchFilterOptions(
                id = "type",
                title = LocalizedText.Literal("File type"),
                options = listOf(
                    SearchFilterOption("images", LocalizedText.Literal("Images")),
                    SearchFilterOption("documents", LocalizedText.Literal("Documents")),
                    SearchFilterOption("audio", LocalizedText.Literal("Audio")),
                ),
                selectedOptionId = "documents",
            ),
            onOptionSelected = { _, _ -> }
        )
    }
}

internal const val SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG = "search_filter_bottom_sheet:title"
internal const val SEARCH_FILTER_OPTION_TAG = "search_filter_bottom_sheet:option"

package mega.privacy.android.core.nodecomponents.sheet.sort

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.HeaderTextStyle
import mega.android.core.ui.components.list.PrimaryHeaderListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

internal const val SORT_BOTTOM_SHEET_TITLE_TAG = "sort_bottom_sheet:title"

/**
 * Interface for sort option items that can be used in the SortBottomSheet.
 *
 * @property displayName Human-readable name to display in the UI
 */
interface SortOptionItem {
    @get:StringRes
    val displayName: Int
    val testTag: String
}

/**
 * Result data class containing the selected sort option and order.
 *
 * @param T The type of sort option item
 * @property sortOptionItem The selected sort option
 * @property sortDirection The selected sort direction (ascending or descending)
 */
data class SortBottomSheetResult<T : SortOptionItem>(
    val sortOptionItem: T,
    val sortDirection: SortDirection,
)

/**
 * A bottom sheet component for selecting sort options and order.
 *
 * @param T The type of sort option items
 * @param title The title displayed at the top of the bottom sheet
 * @param options List of available sort options
 * @param sheetState The state of the bottom sheet
 * @param modifier Modifier to apply to the bottom sheet
 * @param selectedSort Currently selected sort option and order, if any
 * @param onDismissRequest Callback when the bottom sheet is dismissed
 * @param onSortOptionSelected Callback when a sort option is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : SortOptionItem> SortBottomSheet(
    title: String,
    options: List<T>,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    selectedSort: SortBottomSheetResult<T>? = null,
    onDismissRequest: () -> Unit = {},
    onSortOptionSelected: (SortBottomSheetResult<T>?) -> Unit,
) {
    var internalSort by remember {
        // Make sure the initial sort is valid by checking if the selectedSort is in the options list.
        mutableStateOf(selectedSort.takeIf { it?.sortOptionItem in options })
    }
    val currentSort by remember {
        derivedStateOf { internalSort }
    }

    LaunchedEffect(selectedSort) {
        internalSort = selectedSort
    }

    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        modifier = modifier.statusBarsPadding(),
        onDismissRequest = onDismissRequest
    ) {
        PrimaryHeaderListItem(
            modifier = Modifier
                .testTag(SORT_BOTTOM_SHEET_TITLE_TAG),
            text = title,
            headerTextStyle = HeaderTextStyle.Medium,
            enableClick = false
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                val isSelected = currentSort?.sortOptionItem == option
                FlexibleLineListItem(
                    modifier = Modifier
                        .testTag(option.testTag),
                    title = stringResource(option.displayName),
                    trailingElement = {
                        if (isSelected) {
                            MegaIcon(
                                modifier = Modifier.size(24.dp),
                                painter = rememberVectorPainter(
                                    if (currentSort?.sortDirection == SortDirection.Ascending) {
                                        IconPack.Small.Thin.Outline.ArrowUp
                                    } else {
                                        IconPack.Small.Thin.Outline.ArrowDown
                                    }
                                ),
                                contentDescription = currentSort?.sortDirection?.name,
                                tint = IconColor.Secondary,
                            )
                        }
                    },
                    onClickListener = {
                        val result = if (isSelected) {
                            selectedSort?.toggleOrder()
                        } else {
                            option.toSortResult()
                        }

                        internalSort = result
                        onSortOptionSelected(result)
                    }
                )
            }
        }
    }
}

/**
 * Extension function to create a SortBottomSheetResult from a SortOptionItem with default ascending order.
 */
private fun <T : SortOptionItem> T.toSortResult(): SortBottomSheetResult<T> =
    SortBottomSheetResult(this, SortDirection.Ascending)

/**
 * Extension function to toggle the sort order of an existing SortBottomSheetResult.
 */
private fun <T : SortOptionItem> SortBottomSheetResult<T>.toggleOrder(): SortBottomSheetResult<T> =
    SortBottomSheetResult(
        sortOptionItem,
        if (sortDirection == SortDirection.Ascending) SortDirection.Descending else SortDirection.Ascending
    )

// For preview purposes, we define a simple enum class for sort options.
private enum class PreviewSortOption(
    override val displayName: Int,
    override val testTag: String = "sort_option:$displayName",
) : SortOptionItem {
    Name(sharedR.string.action_sort_by_name),
    Date(sharedR.string.action_sort_by_created),
    Size(sharedR.string.action_sort_by_size),
    Label(sharedR.string.action_sort_by_label),
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun SortBottomSheetPreview() {
    AndroidThemeForPreviews {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { true }
        )
        var selectedSort by remember {
            mutableStateOf<SortBottomSheetResult<PreviewSortOption>?>(null)
        }

        LaunchedEffect(Unit) {
            sheetState.show()
        }

        SortBottomSheet(
            title = "Sort by (${selectedSort?.sortOptionItem?.displayName ?: "-"}, ${selectedSort?.sortOptionItem ?: "-"})",
            options = PreviewSortOption.entries,
            sheetState = sheetState,
            selectedSort = selectedSort,
            onSortOptionSelected = {
                selectedSort = it
            },
        )
    }
}
package mega.privacy.android.core.nodecomponents.sheet.sort

import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.HeaderTextStyle
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack

internal const val SORT_BOTTOM_SHEET_TITLE_TAG = "sort_bottom_sheet:title"

/**
 * Interface for sort option items that can be used in the SortBottomSheet.
 *
 * @property key Unique identifier for the sort option
 * @property displayName Human-readable name to display in the UI
 */
interface SortOptionItem {
    val key: String
    val displayName: String
    val testTag: String
}

/**
 * Represents the sort order direction.
 */
enum class SortOrder {
    /** Sort in ascending order (A-Z, 1-9, oldest to newest) */
    Ascending,

    /** Sort in descending order (Z-A, 9-1, newest to oldest) */
    Descending
}

/**
 * Result data class containing the selected sort option and order.
 *
 * @param T The type of sort option item
 * @property sortOptionItem The selected sort option
 * @property sortOrder The selected sort order (ascending or descending)
 */
data class SortBottomSheetResult<T : SortOptionItem>(
    val sortOptionItem: T,
    val sortOrder: SortOrder,
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
        modifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        SecondaryHeaderListItem(
            modifier = Modifier
                .testTag(SORT_BOTTOM_SHEET_TITLE_TAG),
            text = title,
            headerTextStyle = HeaderTextStyle.Medium,
            enableClick = false
        )

        options.forEach { option ->
            val isSelected = currentSort?.sortOptionItem == option
            FlexibleLineListItem(
                modifier = Modifier
                    .testTag(option.testTag),
                title = option.displayName,
                trailingElement = {
                    if (isSelected) {
                        MegaIcon(
                            modifier = Modifier.size(24.dp),
                            painter = rememberVectorPainter(
                                if (currentSort?.sortOrder == SortOrder.Ascending) {
                                    IconPack.Small.Thin.Outline.ArrowUp
                                } else {
                                    IconPack.Small.Thin.Outline.ArrowDown
                                }
                            ),
                            contentDescription = currentSort?.sortOrder?.name,
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

/**
 * Extension function to create a SortBottomSheetResult from a SortOptionItem with default ascending order.
 */
private fun <T : SortOptionItem> T.toSortResult(): SortBottomSheetResult<T> =
    SortBottomSheetResult(this, SortOrder.Ascending)

/**
 * Extension function to toggle the sort order of an existing SortBottomSheetResult.
 */
private fun <T : SortOptionItem> SortBottomSheetResult<T>.toggleOrder(): SortBottomSheetResult<T> =
    SortBottomSheetResult(
        sortOptionItem,
        if (sortOrder == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending
    )

// For preview purposes, we define a simple enum class for sort options.
private enum class PreviewSortOption(
    override val key: String,
    override val displayName: String,
    override val testTag: String = "sort_option:$key",
) : SortOptionItem {
    Name("name", "Name"),
    Date("date", "Date"),
    Size("size", "Size"),
    Type("type", "Type"),
    Modified("modified", "Modified"),
    Created("created", "Created"),
    Favourite("favourite", "Favourite"),
    Custom("custom", "Custom")
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
            title = "Sort by (${selectedSort?.sortOptionItem?.displayName ?: "-"}, ${selectedSort?.sortOrder ?: "-"})",
            options = PreviewSortOption.entries,
            sheetState = sheetState,
            selectedSort = selectedSort,
            onSortOptionSelected = {
                selectedSort = it
            },
        )
    }
}
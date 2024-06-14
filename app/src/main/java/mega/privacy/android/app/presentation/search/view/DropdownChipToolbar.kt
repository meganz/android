package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.chip.ChipBar
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun DropdownChipToolbar(
    chipItems: List<ChipItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ChipBar(modifier = modifier) {
        chipItems.forEach {
            DropdownChip(
                isSelected = it.isSelected,
                isEnabled = enabled,
                notSelectedTitle = it.notSelectedTitle,
                selectedFilterTitle = it.selectedFilterTitle,
                onFilterClicked = it.onFilterClicked,
                chipTestTag = it.testTag,
            )
        }
    }
}

@Composable
private fun DropdownChip(
    isSelected: Boolean,
    isEnabled: Boolean,
    notSelectedTitle: String,
    selectedFilterTitle: String,
    onFilterClicked: () -> Unit,
    chipTestTag: String,
    modifier: Modifier = Modifier,
) {
    MegaChip(
        selected = isSelected,
        text = if (isSelected) selectedFilterTitle else notSelectedTitle,
        enabled = isEnabled,
        modifier = modifier.testTag(chipTestTag),
        onClick = onFilterClicked,
        trailingIcon = R.drawable.ic_chevron_down,
    )
}

/**
 * A chip's data and action
 */
data class ChipItem(
    /**
     * Is chip selected
     */
    val isSelected: Boolean,
    /**
     * Chip title without selection
     */
    val notSelectedTitle: String,
    /**
     * Chip title with selected filter
     */
    val selectedFilterTitle: String,
    /**
     * Action for chip click
     */
    val onFilterClicked: () -> Unit,
    /**
     * Test tag for chip
     */
    val testTag: String,
)

@CombinedThemePreviews
@Composable
private fun DropdownChipToolbarPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DropdownChipToolbar(
            listOf(
                ChipItem(
                    isSelected = false,
                    notSelectedTitle = "Type",
                    selectedFilterTitle = "Photos",
                    onFilterClicked = {},
                    testTag = "",
                ),
                ChipItem(
                    isSelected = false,
                    notSelectedTitle = "Date added",
                    selectedFilterTitle = "Older",
                    onFilterClicked = {},
                    testTag = "",
                ),
                ChipItem(
                    isSelected = true,
                    notSelectedTitle = "Last modified",
                    selectedFilterTitle = "Last 7 days",
                    onFilterClicked = {},
                    testTag = "",
                )
            )
        )
    }
}
package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.chip.Chip
import mega.privacy.android.shared.original.core.ui.controls.chip.ChipBar
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
    Chip(
        selected = isSelected,
        enabled = isEnabled,
        contentDescription = "Dropdown Chip",
        modifier = modifier,
        onClick = onFilterClicked,
    ) {
        Text(
            modifier = Modifier.testTag(chipTestTag),
            text = if (isSelected) selectedFilterTitle else notSelectedTitle,
        )
        Icon(
            modifier = Modifier.testTag(DROPDOWN_CHIP_ICON_TEST_TAG).size(18.dp),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_down),
            contentDescription = "Choose Options",
        )
    }
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

internal const val DROPDOWN_CHIP_ICON_TEST_TAG = "drop_down_chips:drop_down_icon"

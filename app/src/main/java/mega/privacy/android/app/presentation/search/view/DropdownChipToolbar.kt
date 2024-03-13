package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chip.Chip
import mega.privacy.android.core.ui.controls.chip.ChipBar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for types dropdown chip
 */
const val TYPE_DROPDOWN_CHIP_TEST_TAG = "type_dropdown_chip_test_tag"

@Composable
internal fun DropdownChipToolbar(
    isTypeFilterSelected: Boolean,
    typeFilterTitle: String,
    selectedTypeFilterTitle: String,
    onTypeFilterClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipBar(modifier = modifier) {
        DropdownChip(
            isSelected = isTypeFilterSelected,
            notSelectedTitle = typeFilterTitle,
            selectedFilterTitle = selectedTypeFilterTitle,
            onFilterClicked = onTypeFilterClicked,
            chipTestTag = TYPE_DROPDOWN_CHIP_TEST_TAG,
        )
    }
}

@Composable
private fun DropdownChip(
    isSelected: Boolean,
    notSelectedTitle: String,
    selectedFilterTitle: String,
    onFilterClicked: () -> Unit,
    modifier: Modifier = Modifier,
    chipTestTag: String = "",
) {
    Chip(
        selected = isSelected,
        contentDescription = "Dropdown Chip",
        modifier = modifier,
        onClick = onFilterClicked,
    ) {
        Text(
            modifier = Modifier.testTag(chipTestTag),
            text = if (isSelected) selectedFilterTitle else notSelectedTitle,
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_down),
            contentDescription = "Choose Options",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DropdownChipToolbarPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DropdownChipToolbar(
            isTypeFilterSelected = false,
            typeFilterTitle = "Type",
            selectedTypeFilterTitle = "Photos",
            onTypeFilterClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DropdownChipToolbarSelectedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DropdownChipToolbar(
            isTypeFilterSelected = true,
            typeFilterTitle = "Type",
            selectedTypeFilterTitle = "Photos",
            onTypeFilterClicked = {},
        )
    }
}

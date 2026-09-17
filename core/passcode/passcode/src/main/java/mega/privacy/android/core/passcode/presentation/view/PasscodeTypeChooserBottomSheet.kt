package mega.privacy.android.core.passcode.presentation.view

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.settings.SettingsGroupOptionsItems
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.passcode.presentation.model.PasscodeTypeOption
import mega.privacy.android.shared.resources.R

/**
 * Lets the user choose between a 4 digit pin, a 6 digit pin and an alphanumeric passcode.
 *
 * @param selectedOption
 * @param onOptionSelected
 * @param onDismissRequest
 * @param modifier
 * @param sheetState
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasscodeTypeChooserBottomSheet(
    selectedOption: PasscodeTypeOption,
    onOptionSelected: (PasscodeTypeOption) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(PASSCODE_TYPE_CHOOSER_SHEET_TAG),
    ) {
        PasscodeTypeOptions(
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected,
        )
    }
}

/**
 * The chooser's options, separated from the sheet so they can be rendered on their own.
 *
 * @param selectedOption
 * @param onOptionSelected
 */
@Composable
internal fun ColumnScope.PasscodeTypeOptions(
    selectedOption: PasscodeTypeOption,
    onOptionSelected: (PasscodeTypeOption) -> Unit,
) {
    val labels = PasscodeTypeOption.entries.associateWith {
        stringResource(id = it.labelResource())
    }
    SettingsGroupOptionsItems(
        key = PASSCODE_TYPE_OPTION_KEY,
        values = PasscodeTypeOption.entries,
        selectedValue = selectedOption,
        valueToString = { labels.getValue(it) },
        onValueSelected = { _, option -> onOptionSelected(option) },
    )
}

private fun PasscodeTypeOption.labelResource() = when (this) {
    PasscodeTypeOption.FourDigits -> R.string.four_pin_lock
    PasscodeTypeOption.SixDigits -> R.string.six_pin_lock
    PasscodeTypeOption.Alphanumeric -> R.string.AN_pin_lock
}

internal const val PASSCODE_TYPE_CHOOSER_SHEET_TAG = "passcode_type_chooser:bottom_sheet"

/**
 * Key given to [SettingsGroupOptionsItems], which derives its option test tags from it as
 * `settings_passcode_type_<index>:list_item` and `settings_passcode_type_<index>:radio_button`.
 */
internal const val PASSCODE_TYPE_OPTION_KEY = "passcode_type"

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun PasscodeTypeChooserBottomSheetPreview(
    @PreviewParameter(PasscodeTypeOptionProvider::class) selectedOption: PasscodeTypeOption,
) {
    AndroidThemeForPreviews {
        PasscodeTypeChooserBottomSheet(
            selectedOption = selectedOption,
            onOptionSelected = {},
            onDismissRequest = {},
        )
    }
}

private class PasscodeTypeOptionProvider : PreviewParameterProvider<PasscodeTypeOption> {
    override val values = PasscodeTypeOption.entries.asSequence()
}

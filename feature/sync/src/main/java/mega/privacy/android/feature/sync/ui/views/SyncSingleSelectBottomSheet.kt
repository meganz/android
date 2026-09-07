package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import kotlinx.coroutines.launch
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.sheets.SheetActionHeader
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * One selectable entry of [SyncSingleSelectBottomSheet].
 *
 * @param value the value reported back when this entry is picked
 * @param label the already localised text shown to the user
 */
internal data class SyncSingleSelectOption<T : Any>(
    val value: T,
    val label: String,
)

/**
 * A modal bottom sheet offering a mutually exclusive choice from [options].
 *
 * Picking an entry reports it through [onOptionSelected] and then animates the sheet away, so
 * callers only have to hold the current selection. Keep the sheet composed until
 * [onDismissRequest] arrives — tearing it down inside [onOptionSelected] cancels the closing
 * animation and swallows that callback.
 *
 * @param selectedValue the currently picked value, or `null` while nothing is picked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : Any> SyncSingleSelectBottomSheet(
    title: String,
    options: List<SyncSingleSelectOption<T>>,
    selectedValue: T?,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val coroutineScope = rememberCoroutineScope()

    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        SyncSingleSelectSheetContent(
            title = title,
            options = options,
            selectedValue = selectedValue,
            subtitle = subtitle,
            onOptionSelected = { value ->
                onOptionSelected(value)
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
            },
        )
    }
}

/**
 * The sheet body, hoisted out of [SyncSingleSelectBottomSheet] so it can be rendered without a
 * sheet window in previews and screenshot tests.
 */
@Composable
internal fun <T : Any> SyncSingleSelectSheetContent(
    title: String,
    options: List<SyncSingleSelectOption<T>>,
    selectedValue: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.selectableGroup()) {
        SheetActionHeader(
            title = title,
            subtitle = subtitle,
        )

        options.forEach { option ->
            val isSelected = option.value == selectedValue

            OneLineListItem(
                // the radio is a separately clickable node, so its state would otherwise be
                // announced apart from the label it belongs to
                modifier = Modifier.semantics {
                    testTag = "$SYNC_SINGLE_SELECT_OPTION_TAG:${option.label}"
                    role = Role.RadioButton
                    selected = isSelected
                },
                text = option.label,
                onClickListener = { onOptionSelected(option.value) },
                leadingElement = {
                    MegaRadioButton(
                        modifier = Modifier.clearAndSetSemantics {
                            testTag = "$SYNC_SINGLE_SELECT_RADIO_TAG:${option.label}"
                        },
                        identifier = option.value,
                        selected = isSelected,
                        // the radio has its own ripple, so it must select rather than swallow taps
                        onOptionSelected = { onOptionSelected(option.value) },
                    )
                },
            )
        }
    }
}

internal const val SYNC_SINGLE_SELECT_OPTION_TAG = "sync_single_select_sheet:option"
internal const val SYNC_SINGLE_SELECT_RADIO_TAG = "sync_single_select_sheet:radio"

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun SyncSingleSelectBottomSheetPreview() {
    AndroidThemeForPreviews {
        SyncSingleSelectBottomSheet(
            title = "Unit",
            options = listOf(
                SyncSingleSelectOption("B", "Bytes"),
                SyncSingleSelectOption("KB", "Kilobytes"),
                SyncSingleSelectOption("MB", "Megabytes"),
            ),
            selectedValue = "KB",
            onOptionSelected = {},
            onDismissRequest = {},
            sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
        )
    }
}

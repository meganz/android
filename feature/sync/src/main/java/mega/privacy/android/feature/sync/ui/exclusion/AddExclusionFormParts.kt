package mega.privacy.android.feature.sync.ui.exclusion

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.inputfields.ReadOnlyTextInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectBottomSheet
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectOption
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/** A picker chip. [chosen] drives both the check mark and the selected styling. */
@Composable
internal fun ExclusionPickerChip(
    @StringRes labelRes: Int,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaChip(
        // the chip clears its own semantics subtree, discarding the role, selected state and click
        // action it would otherwise carry, and only an outer node survives that
        modifier = modifier.semantics {
            role = Role.Button
            selected = chosen
            onClick {
                onClick()
                true
            }
        },
        selected = chosen,
        text = stringResource(labelRes),
        leadingIcon = iconPackR.drawable.ic_check_medium_thin_outline.takeIf { chosen },
        onClick = onClick,
    )
}

/**
 * The field the rule's value is typed into.
 *
 * The value is matched verbatim, so the first letter is not auto-capitalised: that would silently
 * author a rule the user did not type. Autocorrect can still rewrite it; the field offers no
 * switch for that yet.
 */
@Composable
internal fun ExclusionValueField(
    value: String,
    errorText: String?,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextInputField(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.None,
        text = value,
        errorText = errorText,
        onValueChanged = onValueChanged,
    )
}

/** The line above the pickers saying what the rule being built will do. */
@Composable
internal fun ExclusionFormDescription(
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
) {
    MegaText(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = stringResource(textRes),
        textColor = TextColor.Secondary,
        style = AppTheme.typography.bodyMedium,
    )
}

/**
 * A neutral hint under the value field. The field renders supporting text only in a success, error
 * or warning colour, so a neutral hint has to sit beside it until the field itself offers one.
 */
@Composable
internal fun ExclusionFormHelperText(
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
) {
    MegaText(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        text = stringResource(textRes),
        textColor = TextColor.Secondary,
        style = AppTheme.typography.bodySmall,
    )
}

/**
 * The Type sheet.
 *
 * @param options the entries offered, which the Edit screen narrows: an existing rule carries a
 * node type, and [SyncExclusionTypeOption.Extensions] is a mode of the Add form rather than one
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExclusionTypeBottomSheet(
    selectedValue: SyncExclusionTypeOption?,
    onOptionSelected: (SyncExclusionTypeOption) -> Unit,
    onDismissRequest: () -> Unit,
    options: List<SyncExclusionTypeOption> = SyncExclusionTypeOption.entries,
) {
    SyncSingleSelectBottomSheet(
        title = stringResource(sharedR.string.sync_exclusions_add_type),
        options = options.map {
            SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
        },
        selectedValue = selectedValue,
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
    )
}

/**
 * The field a size bound is typed into. The number is trailing, beside the unit that reads it.
 *
 * A [KeyboardType.Number] field keeps itself to digits, including on paste, so a bound cannot
 * arrive carrying the unit the field beside it already names.
 */
@Composable
internal fun ExclusionSizeValueField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextInputField(
        modifier = modifier,
        keyboardType = KeyboardType.Number,
        text = value,
        inputTextAlign = TextAlign.End,
        onValueChanged = onValueChanged,
    )
}

/**
 * The unit beside a size bound: a read-only field that opens the unit picker.
 *
 * The field is a read-only text field, which takes a tap before an enclosing clickable sees it,
 * so the tap target is an overlay laid over it rather than a modifier on it.
 */
@Composable
internal fun ExclusionSizeUnitField(
    unit: SyncSizeUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.DropdownList
            onClick {
                onClick()
                true
            }
        },
    ) {
        ReadOnlyTextInputField(
            text = stringResource(unit.labelRes),
            trailingIcon = {
                MegaIcon(
                    painter = painterResource(
                        iconPackR.drawable.ic_chevron_down_small_regular_outline,
                    ),
                    tint = IconColor.Secondary,
                    contentDescription = null,
                )
            },
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}

/** The size condition sheet, titled for the files it leaves out rather than for the chip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExclusionSizeConditionBottomSheet(
    selectedValue: SyncSizeCondition?,
    onOptionSelected: (SyncSizeCondition) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SyncSingleSelectBottomSheet(
        title = stringResource(sharedR.string.sync_exclusions_add_size_condition_title),
        options = SyncSizeCondition.entries.map {
            SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
        },
        selectedValue = selectedValue,
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExclusionSizeUnitBottomSheet(
    selectedValue: SyncSizeUnit,
    onOptionSelected: (SyncSizeUnit) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SyncSingleSelectBottomSheet(
        title = stringResource(sharedR.string.sync_exclusions_add_size_unit_title),
        options = SyncSizeUnit.entries.map {
            SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
        },
        selectedValue = selectedValue,
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
    )
}

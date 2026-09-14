package mega.privacy.android.feature.sync.ui.exclusion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
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

/** The Type sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExclusionTypeBottomSheet(
    selectedValue: SyncExclusionTypeOption?,
    onOptionSelected: (SyncExclusionTypeOption) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SyncSingleSelectBottomSheet(
        title = stringResource(sharedR.string.sync_exclusions_add_type),
        options = SyncExclusionTypeOption.entries.map {
            SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
        },
        selectedValue = selectedValue,
        onOptionSelected = onOptionSelected,
        onDismissRequest = onDismissRequest,
    )
}

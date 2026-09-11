package mega.privacy.android.feature.sync.ui.exclusion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectBottomSheet
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectOption
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
/**
 * The by-name body of the Add exclusion screen: the description line the pickers drive, the two
 * picker chips and the name field.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddExclusionByNameForm(
    state: AddExclusionByNameFormState,
    onTypeOptionSelected: (SyncExclusionTypeOption) -> Unit,
    onConditionSelected: (SyncExclusionCondition) -> Unit,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleSheet by rememberSaveable { mutableStateOf<AddExclusionByNamePicker?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        state.descriptionRes?.let {
            MegaText(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(ADD_EXCLUSION_DESCRIPTION_TAG),
                text = stringResource(it),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
            )
        }

        // the two labels together overflow one line in longer locales, and the chip clips rather
        // than wraps its own text
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerChip(
                labelRes = state.typeOption?.labelRes ?: sharedR.string.sync_exclusions_add_type,
                chosen = state.typeOption != null,
                onClick = { visibleSheet = AddExclusionByNamePicker.Type },
                modifier = Modifier.testTag(ADD_EXCLUSION_TYPE_CHIP_TAG),
            )
            if (state.showsConditionChip) {
                PickerChip(
                    labelRes = state.condition?.labelRes
                        ?: sharedR.string.sync_exclusions_add_condition,
                    chosen = state.condition != null,
                    onClick = { visibleSheet = AddExclusionByNamePicker.Condition },
                    modifier = Modifier.testTag(ADD_EXCLUSION_CONDITION_CHIP_TAG),
                )
            }
        }

        TextInputField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag(ADD_EXCLUSION_NAME_FIELD_TAG),
            keyboardType = KeyboardType.Text,
            // a name pattern is matched verbatim, so auto-capitalising the first letter would
            // silently author a rule the user did not type. Autocorrect can still rewrite it; the
            // field offers no switch for that yet
            capitalization = KeyboardCapitalization.None,
            text = state.value,
            errorText = state.errorRes?.let { stringResource(it) },
            onValueChanged = onValueChanged,
        )

        // the field renders supporting text only in a success, error or warning colour, so a
        // neutral hint has to sit beside it until the field itself offers one
        if (state.showsWildcardHelper) {
            MegaText(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag(ADD_EXCLUSION_WILDCARD_HELPER_TAG),
                text = stringResource(sharedR.string.sync_exclusions_add_name_wildcard_helper),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodySmall,
            )
        }
    }

    when (visibleSheet) {
        AddExclusionByNamePicker.Type -> SyncSingleSelectBottomSheet(
            title = stringResource(sharedR.string.sync_exclusions_add_type),
            options = SyncExclusionTypeOption.entries.map {
                SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
            },
            selectedValue = state.typeOption,
            onOptionSelected = onTypeOptionSelected,
            onDismissRequest = { visibleSheet = null },
        )

        AddExclusionByNamePicker.Condition -> SyncSingleSelectBottomSheet(
            title = stringResource(sharedR.string.sync_exclusions_add_condition),
            options = SyncExclusionCondition.entries.map {
                SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
            },
            selectedValue = state.condition,
            onOptionSelected = onConditionSelected,
            onDismissRequest = { visibleSheet = null },
        )

        null -> Unit
    }
}

@Composable
private fun PickerChip(
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

/** Which picker is open. One at a time, so the two cannot both be shown. */
private enum class AddExclusionByNamePicker { Type, Condition }

internal const val ADD_EXCLUSION_DESCRIPTION_TAG = "add_exclusion_by_name:description"
internal const val ADD_EXCLUSION_TYPE_CHIP_TAG = "add_exclusion_by_name:type_chip"
internal const val ADD_EXCLUSION_CONDITION_CHIP_TAG = "add_exclusion_by_name:condition_chip"
internal const val ADD_EXCLUSION_NAME_FIELD_TAG = "add_exclusion_by_name:name_field"
internal const val ADD_EXCLUSION_WILDCARD_HELPER_TAG = "add_exclusion_by_name:wildcard_helper"

private class AddExclusionByNameFormStateProvider :
    PreviewParameterProvider<AddExclusionByNameFormState> {
    override val values = sequenceOf(
        AddExclusionByNameFormState(),
        AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.Folders,
            condition = SyncExclusionCondition.BeginningWith,
            value = "temp",
        ),
        AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.FilesAndFolders,
            condition = SyncExclusionCondition.MatchingWildcard,
            value = "*.tmp",
        ),
        AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.Files,
            condition = SyncExclusionCondition.MatchingExactly,
            value = "notes.txt",
            errorRes = sharedR.string.sync_exclusions_add_name_duplicate_error,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AddExclusionByNameFormPreview(
    @PreviewParameter(AddExclusionByNameFormStateProvider::class)
    state: AddExclusionByNameFormState,
) {
    AndroidThemeForPreviews {
        AddExclusionByNameForm(
            state = state,
            onTypeOptionSelected = {},
            onConditionSelected = {},
            onValueChanged = {},
        )
    }
}

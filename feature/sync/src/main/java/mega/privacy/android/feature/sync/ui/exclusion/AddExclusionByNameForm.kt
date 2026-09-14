package mega.privacy.android.feature.sync.ui.exclusion

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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectBottomSheet
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectOption
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
            ExclusionFormDescription(
                textRes = it,
                modifier = Modifier.testTag(ADD_EXCLUSION_DESCRIPTION_TAG),
            )
        }

        // the two labels together overflow one line in longer locales, and the chip clips rather
        // than wraps its own text
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExclusionPickerChip(
                labelRes = state.typeOption?.labelRes ?: sharedR.string.sync_exclusions_add_type,
                chosen = state.typeOption != null,
                onClick = { visibleSheet = AddExclusionByNamePicker.Type },
                modifier = Modifier.testTag(ADD_EXCLUSION_TYPE_CHIP_TAG),
            )
            if (state.showsConditionChip) {
                ExclusionPickerChip(
                    labelRes = state.condition?.labelRes
                        ?: sharedR.string.sync_exclusions_add_condition,
                    chosen = state.condition != null,
                    onClick = { visibleSheet = AddExclusionByNamePicker.Condition },
                    modifier = Modifier.testTag(ADD_EXCLUSION_CONDITION_CHIP_TAG),
                )
            }
        }

        ExclusionValueField(
            value = state.value,
            errorText = state.errorRes?.let { stringResource(it) },
            onValueChanged = onValueChanged,
            modifier = Modifier.testTag(ADD_EXCLUSION_NAME_FIELD_TAG),
        )

        if (state.showsWildcardHelper) {
            ExclusionFormHelperText(
                textRes = sharedR.string.sync_exclusions_add_name_wildcard_helper,
                modifier = Modifier.testTag(ADD_EXCLUSION_WILDCARD_HELPER_TAG),
            )
        }
    }

    when (visibleSheet) {
        AddExclusionByNamePicker.Type -> ExclusionTypeBottomSheet(
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

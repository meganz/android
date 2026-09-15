package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The by-size body of the Add exclusion screen: the description line, the condition chip, and one
 * bound per the chosen condition.
 *
 * Size rules apply to files only, so no Type chip appears here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExclusionBySizeForm(
    state: AddExclusionBySizeFormState,
    onConditionSelected: (SyncSizeCondition) -> Unit,
    onFirstBoundValueChanged: (String) -> Unit,
    onFirstBoundUnitSelected: (SyncSizeUnit) -> Unit,
    onSecondBoundValueChanged: (String) -> Unit,
    onSecondBoundUnitSelected: (SyncSizeUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleSheet by rememberSaveable { mutableStateOf<AddExclusionBySizePicker?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        ExclusionFormDescription(
            textRes = sharedR.string.sync_exclusions_add_description_size,
            modifier = Modifier.testTag(ADD_EXCLUSION_SIZE_DESCRIPTION_TAG),
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ExclusionPickerChip(
                labelRes = state.condition?.labelRes
                    ?: sharedR.string.sync_exclusions_add_condition,
                chosen = state.condition != null,
                onClick = { visibleSheet = AddExclusionBySizePicker.Condition },
                modifier = Modifier.testTag(ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG),
            )
        }

        if (state.showsBounds) {
            SizeBoundRow(
                bound = state.firstBound,
                onValueChanged = onFirstBoundValueChanged,
                onUnitClick = { visibleSheet = AddExclusionBySizePicker.FirstUnit },
                valueTestTag = ADD_EXCLUSION_SIZE_FIRST_VALUE_TAG,
                unitTestTag = ADD_EXCLUSION_SIZE_FIRST_UNIT_TAG,
            )
        }

        if (state.showsSecondBound) {
            MegaText(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(ADD_EXCLUSION_SIZE_CONJUNCTION_TAG),
                text = stringResource(
                    sharedR.string.sync_exclusions_add_size_range_conjunction,
                ),
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyMedium,
            )

            SizeBoundRow(
                bound = state.secondBound,
                onValueChanged = onSecondBoundValueChanged,
                onUnitClick = { visibleSheet = AddExclusionBySizePicker.SecondUnit },
                valueTestTag = ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG,
                unitTestTag = ADD_EXCLUSION_SIZE_SECOND_UNIT_TAG,
            )
        }
    }

    // the flag survives process death but the draft it points at does not, so a restored sheet
    // can name a bound the rebuilt form no longer draws
    when (visibleSheet?.takeIf { it.isDrawnUnder(state) }) {
        AddExclusionBySizePicker.Condition -> ExclusionSizeConditionBottomSheet(
            selectedValue = state.condition,
            onOptionSelected = onConditionSelected,
            onDismissRequest = { visibleSheet = null },
        )

        AddExclusionBySizePicker.FirstUnit -> ExclusionSizeUnitBottomSheet(
            selectedValue = state.firstBound.unit,
            onOptionSelected = onFirstBoundUnitSelected,
            onDismissRequest = { visibleSheet = null },
        )

        AddExclusionBySizePicker.SecondUnit -> ExclusionSizeUnitBottomSheet(
            selectedValue = state.secondBound.unit,
            onOptionSelected = onSecondBoundUnitSelected,
            onDismissRequest = { visibleSheet = null },
        )

        null -> Unit
    }
}

/** One bound: the digits, and the unit they are read in. */
@Composable
private fun SizeBoundRow(
    bound: SyncSizeBoundInput,
    onValueChanged: (String) -> Unit,
    onUnitClick: () -> Unit,
    valueTestTag: String,
    unitTestTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExclusionSizeValueField(
            value = bound.value,
            onValueChanged = onValueChanged,
            modifier = Modifier
                .weight(VALUE_FIELD_WEIGHT)
                .testTag(valueTestTag),
        )

        ExclusionSizeUnitField(
            unit = bound.unit,
            onClick = onUnitClick,
            modifier = Modifier
                .weight(UNIT_FIELD_WEIGHT)
                .testTag(unitTestTag),
        )
    }
}

/** Which picker is open. One at a time, so two sheets cannot both be shown. */
private enum class AddExclusionBySizePicker { Condition, FirstUnit, SecondUnit }

/** Whether the field this picker belongs to is on screen under [state]. */
private fun AddExclusionBySizePicker.isDrawnUnder(state: AddExclusionBySizeFormState): Boolean =
    when (this) {
        AddExclusionBySizePicker.Condition -> true
        AddExclusionBySizePicker.FirstUnit -> state.showsBounds
        AddExclusionBySizePicker.SecondUnit -> state.showsSecondBound
    }

// the unit holds two or three characters and a chevron, so it takes the smaller share; it stays
// a weight rather than a fixed width because a translated unit can be longer than "KB"
private const val VALUE_FIELD_WEIGHT = 1.6f
private const val UNIT_FIELD_WEIGHT = 1f

internal const val ADD_EXCLUSION_SIZE_DESCRIPTION_TAG = "add_exclusion_by_size:description"
internal const val ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG = "add_exclusion_by_size:condition_chip"
internal const val ADD_EXCLUSION_SIZE_FIRST_VALUE_TAG = "add_exclusion_by_size:first_value"
internal const val ADD_EXCLUSION_SIZE_FIRST_UNIT_TAG = "add_exclusion_by_size:first_unit"
internal const val ADD_EXCLUSION_SIZE_CONJUNCTION_TAG = "add_exclusion_by_size:conjunction"
internal const val ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG = "add_exclusion_by_size:second_value"
internal const val ADD_EXCLUSION_SIZE_SECOND_UNIT_TAG = "add_exclusion_by_size:second_unit"

private class AddExclusionBySizeFormStateProvider :
    PreviewParameterProvider<AddExclusionBySizeFormState> {
    override val values = sequenceOf(
        AddExclusionBySizeFormState(),
        AddExclusionBySizeFormState(
            condition = SyncSizeCondition.BiggerThan,
            firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Gigabytes),
        ),
        AddExclusionBySizeFormState(
            condition = SyncSizeCondition.SmallerThan,
            firstBound = SyncSizeBoundInput(value = "10", unit = SyncSizeUnit.Kilobytes),
        ),
        AddExclusionBySizeFormState(
            condition = SyncSizeCondition.NotBetween,
            firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
            secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AddExclusionBySizeFormPreview(
    @PreviewParameter(AddExclusionBySizeFormStateProvider::class)
    state: AddExclusionBySizeFormState,
) {
    AndroidThemeForPreviews {
        AddExclusionBySizeForm(
            state = state,
            onConditionSelected = {},
            onFirstBoundValueChanged = {},
            onFirstBoundUnitSelected = {},
            onSecondBoundValueChanged = {},
            onSecondBoundUnitSelected = {},
        )
    }
}

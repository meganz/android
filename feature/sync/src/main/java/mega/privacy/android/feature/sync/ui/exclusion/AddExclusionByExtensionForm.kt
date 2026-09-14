package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByExtensionFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionExtensionError
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The by-extension body of the Add exclusion screen: the description line, the Type chip and the
 * one field the extensions are typed into.
 */
@Composable
internal fun AddExclusionByExtensionForm(
    state: AddExclusionByExtensionFormState,
    onTypeOptionSelected: (SyncExclusionTypeOption) -> Unit,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typeSheetVisible by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ExclusionFormDescription(
            textRes = sharedR.string.sync_exclusions_add_description_extensions,
            modifier = Modifier.testTag(ADD_EXCLUSION_EXTENSION_DESCRIPTION_TAG),
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ExclusionPickerChip(
                labelRes = SyncExclusionTypeOption.Extensions.labelRes,
                chosen = true,
                onClick = { typeSheetVisible = true },
                modifier = Modifier.testTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG),
            )
        }

        ExclusionValueField(
            value = state.value,
            errorText = state.error?.let { errorText(it) },
            onValueChanged = onValueChanged,
            modifier = Modifier.testTag(ADD_EXCLUSION_EXTENSION_FIELD_TAG),
        )

        if (state.showsHelper) {
            ExclusionFormHelperText(
                textRes = sharedR.string.sync_exclusions_add_extension_helper,
                modifier = Modifier.testTag(ADD_EXCLUSION_EXTENSION_HELPER_TAG),
            )
        }
    }

    if (typeSheetVisible) {
        ExclusionTypeBottomSheet(
            selectedValue = SyncExclusionTypeOption.Extensions,
            onOptionSelected = onTypeOptionSelected,
            onDismissRequest = { typeSheetVisible = false },
        )
    }
}

@Composable
private fun errorText(error: AddExclusionExtensionError): String = when (error) {
    AddExclusionExtensionError.Invalid ->
        stringResource(sharedR.string.sync_exclusions_add_extension_invalid_error)

    is AddExclusionExtensionError.Duplicate -> stringResource(
        sharedR.string.sync_exclusions_add_extension_duplicate_error,
        error.extension,
    )
}

internal const val ADD_EXCLUSION_EXTENSION_DESCRIPTION_TAG =
    "add_exclusion_by_extension:description"
internal const val ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG =
    "add_exclusion_by_extension:type_chip"
internal const val ADD_EXCLUSION_EXTENSION_FIELD_TAG =
    "add_exclusion_by_extension:extension_field"
internal const val ADD_EXCLUSION_EXTENSION_HELPER_TAG =
    "add_exclusion_by_extension:helper"

private class AddExclusionByExtensionFormStateProvider :
    PreviewParameterProvider<AddExclusionByExtensionFormState> {
    override val values = sequenceOf(
        AddExclusionByExtensionFormState(),
        AddExclusionByExtensionFormState(value = "log"),
        AddExclusionByExtensionFormState(value = "mp4, zip"),
        AddExclusionByExtensionFormState(
            value = "log, mp4, mp3",
            error = AddExclusionExtensionError.Duplicate("mp3"),
        ),
        AddExclusionByExtensionFormState(
            value = "my doc",
            error = AddExclusionExtensionError.Invalid,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AddExclusionByExtensionFormPreview(
    @PreviewParameter(AddExclusionByExtensionFormStateProvider::class)
    state: AddExclusionByExtensionFormState,
) {
    AndroidThemeForPreviews {
        AddExclusionByExtensionForm(
            state = state,
            onTypeOptionSelected = {},
            onValueChanged = {},
        )
    }
}

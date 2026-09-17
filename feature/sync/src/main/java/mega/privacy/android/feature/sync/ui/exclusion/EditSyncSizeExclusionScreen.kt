package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncSizeExclusionUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit

/**
 * The Edit exclusion screen for the file's size bounds: the shared chrome around the by-size
 * form, prefilled from the bounds the file declares.
 *
 * @param onDelete invoked when the user takes the bounds out through the trash action, which
 * clears both of them: a range whose upper half survived would silently start excluding the files
 * the lower bound had been keeping out
 * @param onSave invoked when the user keeps the bounds as the form now reads them
 * @param onDismiss invoked when the user backs out or cancels, discarding the draft
 */
@Composable
internal fun EditSyncSizeExclusionScreen(
    uiState: EditSyncSizeExclusionUiState,
    onConditionSelected: (SyncSizeCondition) -> Unit,
    onFirstBoundValueChanged: (String) -> Unit,
    onFirstBoundUnitSelected: (SyncSizeUnit) -> Unit,
    onSecondBoundValueChanged: (String) -> Unit,
    onSecondBoundUnitSelected: (SyncSizeUnit) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EditExclusionScaffold(
        onDelete = onDelete,
        onSave = { if (!uiState.showsBlockingError) onSave() },
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        AddExclusionBySizeForm(
            state = uiState.bySize,
            onConditionSelected = onConditionSelected,
            onFirstBoundValueChanged = onFirstBoundValueChanged,
            onFirstBoundUnitSelected = onFirstBoundUnitSelected,
            onSecondBoundValueChanged = onSecondBoundValueChanged,
            onSecondBoundUnitSelected = onSecondBoundUnitSelected,
        )
    }
}

private class EditSyncSizeExclusionUiStateProvider :
    PreviewParameterProvider<EditSyncSizeExclusionUiState> {
    override val values = sequenceOf(
        EditSyncSizeExclusionUiState(
            bySize = AddExclusionBySizeFormState(
                condition = SyncSizeCondition.BiggerThan,
                firstBound = SyncSizeBoundInput(value = "100", unit = SyncSizeUnit.Megabytes),
            ),
        ),
        EditSyncSizeExclusionUiState(
            bySize = AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            ),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun EditSyncSizeExclusionScreenPreview(
    @PreviewParameter(EditSyncSizeExclusionUiStateProvider::class)
    uiState: EditSyncSizeExclusionUiState,
) {
    AndroidThemeForPreviews {
        EditSyncSizeExclusionScreen(
            uiState = uiState,
            onConditionSelected = {},
            onFirstBoundValueChanged = {},
            onFirstBoundUnitSelected = {},
            onSecondBoundValueChanged = {},
            onSecondBoundUnitSelected = {},
            onDelete = {},
            onSave = {},
            onDismiss = {},
        )
    }
}

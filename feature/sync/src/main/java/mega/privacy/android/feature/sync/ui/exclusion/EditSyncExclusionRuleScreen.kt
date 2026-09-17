package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption

/**
 * The Edit exclusion screen for a name rule: the shared chrome around the by-name form,
 * prefilled from the chosen rule.
 *
 * @param onDelete invoked when the user takes the rule out through the trash action
 * @param onSave invoked when the user keeps the rule as the form now reads it
 * @param onDismiss invoked when the user backs out or cancels, discarding the draft
 */
@Composable
internal fun EditSyncExclusionRuleScreen(
    uiState: EditSyncExclusionRuleUiState,
    onTypeOptionSelected: (SyncExclusionTypeOption) -> Unit,
    onConditionSelected: (SyncExclusionCondition) -> Unit,
    onValueChanged: (String) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EditExclusionScaffold(
        onDelete = onDelete,
        onSave = onSave,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        AddExclusionByNameForm(
            state = uiState.byName,
            onTypeOptionSelected = onTypeOptionSelected,
            onConditionSelected = onConditionSelected,
            onValueChanged = onValueChanged,
            typeOptions = EDIT_EXCLUSION_TYPE_OPTIONS,
        )
    }
}

/**
 * The types an existing rule can be moved between. Extensions is left out: it is a mode of the Add
 * form rather than a node type, and picking it would leave the form showing a name field no
 * condition describes.
 */
private val EDIT_EXCLUSION_TYPE_OPTIONS = listOf(
    SyncExclusionTypeOption.Files,
    SyncExclusionTypeOption.Folders,
    SyncExclusionTypeOption.FilesAndFolders,
)

private class EditSyncExclusionRuleUiStateProvider :
    PreviewParameterProvider<EditSyncExclusionRuleUiState> {
    override val values = sequenceOf(
        EditSyncExclusionRuleUiState(
            byName = AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.EndingWith,
                value = "tmp",
            ),
        ),
        EditSyncExclusionRuleUiState(
            byName = AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.FilesAndFolders,
                condition = SyncExclusionCondition.MatchingWildcard,
                value = "draft?.*",
            ),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun EditSyncExclusionRuleScreenPreview(
    @PreviewParameter(EditSyncExclusionRuleUiStateProvider::class)
    uiState: EditSyncExclusionRuleUiState,
) {
    AndroidThemeForPreviews {
        EditSyncExclusionRuleScreen(
            uiState = uiState,
            onTypeOptionSelected = {},
            onConditionSelected = {},
            onValueChanged = {},
            onDelete = {},
            onSave = {},
            onDismiss = {},
        )
    }
}

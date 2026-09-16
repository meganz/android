package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.Button
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The Edit exclusion screen: the chrome around the by-name form, prefilled from the chosen rule.
 *
 * Deleting is the top bar's sole trailing action and asks for no confirmation, because the screen
 * hands the deletion back for the exclusions list to stage: Cancel there still undoes it.
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
    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(EDIT_EXCLUSION_TOP_BAR_TAG),
                title = stringResource(sharedR.string.sync_exclusions_edit_title),
                navigationType = AppBarNavigationType.Back(onDismiss),
                actions = listOf(CommonMenuAction.Delete),
                onActionPressed = { onDelete() },
            )
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                modifier = Modifier.testTag(EDIT_EXCLUSION_BUTTON_GROUP_TAG),
                buttonGroup = listOf(
                    {
                        Button.TextOnlyButtonM3(
                            text = stringResource(sharedR.string.general_dialog_cancel_button),
                            onClick = onDismiss,
                        )
                    },
                    {
                        Button.PrimaryButtonM3(
                            text = stringResource(sharedR.string.general_action_save),
                            onClick = onSave,
                        )
                    },
                ),
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                AddExclusionByNameForm(
                    state = uiState.byName,
                    onTypeOptionSelected = onTypeOptionSelected,
                    onConditionSelected = onConditionSelected,
                    onValueChanged = onValueChanged,
                    typeOptions = EDIT_EXCLUSION_TYPE_OPTIONS,
                )
            }
        },
    )
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

internal const val EDIT_EXCLUSION_TOP_BAR_TAG = "edit_exclusion:top_bar"
internal const val EDIT_EXCLUSION_BUTTON_GROUP_TAG = "edit_exclusion:button_group"

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

package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule

/**
 * Binds [EditSyncExclusionRuleScreen] to the draft the view model holds.
 *
 * @param rule the rule the exclusions list showed, which the form opens prefilled from
 * @param onDelete invoked when the user takes the rule out through the trash action
 * @param onSave invoked with the change the form describes, for the exclusions list to stage. The
 * control is offered only while the form describes one, and a draft describing none falls back to
 * [onDismiss]
 * @param onDismiss invoked when the user backs out or cancels, discarding the draft
 */
@Composable
internal fun EditSyncExclusionRuleRoute(
    rule: SyncExclusionRule,
    onDelete: () -> Unit,
    onSave: (SyncExclusionEdit) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditSyncExclusionRuleViewModel =
        hiltViewModel<EditSyncExclusionRuleViewModel, EditSyncExclusionRuleViewModel.Factory> {
            it.create(rule)
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditSyncExclusionRuleScreen(
        uiState = uiState,
        onTypeOptionSelected = viewModel::onTypeOptionSelected,
        onConditionSelected = viewModel::onConditionSelected,
        onValueChanged = viewModel::onValueChanged,
        onDelete = onDelete,
        onSave = {
            val edit = viewModel.buildEdit()
            if (edit == null) onDismiss() else onSave(edit)
        },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

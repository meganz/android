package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.navigation.AddSyncExclusionResult

/**
 * Binds [AddSyncExclusionRuleScreen] to the drafts the view model holds.
 *
 * @param onConfirm invoked with what the user accepted, for the exclusions list to stage. The
 * control is offered only while the drafts hold something that can be added, and a draft that does
 * not falls back to [onDismiss]
 * @param onDismiss invoked when the user closes or cancels, discarding every draft
 */
@Composable
internal fun AddSyncExclusionRuleRoute(
    onConfirm: (AddSyncExclusionResult) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddSyncExclusionRuleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddSyncExclusionRuleScreen(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onTypeOptionSelected = viewModel::onTypeOptionSelected,
        onConditionSelected = viewModel::onConditionSelected,
        onNameValueChanged = viewModel::onNameValueChanged,
        onExtensionValueChanged = viewModel::onExtensionValueChanged,
        onSizeConditionSelected = viewModel::onSizeConditionSelected,
        onFirstBoundValueChanged = viewModel::onFirstBoundValueChanged,
        onFirstBoundUnitSelected = viewModel::onFirstBoundUnitSelected,
        onSecondBoundValueChanged = viewModel::onSecondBoundValueChanged,
        onSecondBoundUnitSelected = viewModel::onSecondBoundUnitSelected,
        onConfirm = {
            val result = viewModel.buildResult()
            if (result == null) onDismiss() else onConfirm(result)
        },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

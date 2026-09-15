package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Binds [AddSyncExclusionRuleScreen] to the drafts the view model holds.
 *
 * @param onConfirm invoked when the user accepts the rule they have built
 * @param onDismiss invoked when the user closes or cancels, discarding every draft
 */
@Composable
internal fun AddSyncExclusionRuleRoute(
    onConfirm: () -> Unit,
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
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

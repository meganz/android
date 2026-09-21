package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowUiItem
import mega.privacy.android.shared.resources.R as sharedR

/** The Manage exclusions screen, bound to the sync named by [syncId]. */
@Composable
internal fun ManageSyncExclusionsRoute(
    syncId: Long,
    localRoot: UriPath,
    onFinished: () -> Unit,
    onAddExclusion: (Long) -> Unit,
    onEditRule: (SyncExclusionRowUiItem) -> Unit,
    onEditSize: (SyncSizeExclusion) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageSyncExclusionsViewModel =
        hiltViewModel<ManageSyncExclusionsViewModel, ManageSyncExclusionsViewModel.Factory> {
            it.create(ManageSyncExclusionsViewModel.Args(syncId, localRoot))
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chips by viewModel.chips.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current

    LaunchedEffect(uiState.hasSaveFailed) {
        if (!uiState.hasSaveFailed) return@LaunchedEffect
        try {
            snackbarHostState?.showSnackbar(
                resources.getString(sharedR.string.sync_exclusions_manage_save_failed),
            )
        } finally {
            viewModel.onSaveFailureShown()
        }
    }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinished()
    }

    // system back has to go through the ViewModel too, or it leaves by a path that never asks
    // about unsaved edits
    BackHandler(onBack = viewModel::onBackPressed)

    if (uiState.isDiscardPromptShown) {
        BasicDialog(
            title = stringResource(sharedR.string.general_dialog_title_discard_changes),
            description = stringResource(sharedR.string.general_dialog_discard_changes_message),
            positiveButtonText = stringResource(sharedR.string.general_dialog_discard_button),
            onPositiveButtonClicked = viewModel::onDiscardConfirmed,
            negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
            onNegativeButtonClicked = viewModel::onDiscardDismissed,
            onDismiss = viewModel::onDiscardDismissed,
        )
    }

    ManageSyncExclusionsScreen(
        uiState = uiState,
        chips = chips,
        rows = rows,
        onBackPressed = viewModel::onBackPressed,
        onAddClicked = { onAddExclusion(syncId) },
        onSaveClicked = viewModel::onSaveClicked,
        onRetryClicked = viewModel::onRetryClicked,
        onRuleClicked = onEditRule,
        onExtensionRemoved = { viewModel.onRuleRemoved(it.ruleIndex, it.rule) },
        onSizeClicked = { uiState.staged?.size?.let(onEditSize) },
        modifier = modifier,
    )
}

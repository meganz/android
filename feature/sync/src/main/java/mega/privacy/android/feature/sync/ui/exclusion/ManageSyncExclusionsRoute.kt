package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.Flow
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.navigation.EditSyncExclusionRuleResult
import mega.privacy.android.feature.sync.navigation.SyncExclusionResultEffect
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionEditErrorMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowUiItem
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The Manage exclusions screen, bound to the sync named by [syncId], or, when [syncId] is null,
 * configuring a model still staged for a folder pair that does not exist yet.
 *
 * @param staged in Review mode, the model already staged by the setup screen to seed the screen
 * from, so re-entering it shows the user's own work rather than resetting it
 * @param monitorResult the navigation results the Add, Edit and size screens hand back
 * @param clearResult takes a result out of the channel once it has been staged
 * @param onAddExclusion opens the Add screen with the rules staged so far, for it to check a new
 * rule against
 * @param onReviewSaved hands the staged model to the setup screen once Review mode's Save is
 * confirmed
 */
@Composable
internal fun ManageSyncExclusionsRoute(
    syncId: Long?,
    localRoot: UriPath,
    staged: ParsedMegaIgnore?,
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onFinished: () -> Unit,
    onAddExclusion: (syncId: Long?, staged: List<SyncExclusionRule>) -> Unit,
    onEditRule: (SyncExclusionRowUiItem) -> Unit,
    onEditSize: (SyncSizeExclusion) -> Unit,
    onReviewSaved: (ParsedMegaIgnore) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageSyncExclusionsViewModel =
        hiltViewModel<ManageSyncExclusionsViewModel, ManageSyncExclusionsViewModel.Factory> {
            it.create(ManageSyncExclusionsViewModel.Args(syncId, localRoot, staged))
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chips by viewModel.chips.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val editErrorMapper = remember { SyncExclusionEditErrorMapper() }
    // hoisted so the effects below capture only stable values and are not restarted
    // mid-snackbar by an unrelated recomposition
    val saveFailedMessage = stringResource(sharedR.string.sync_exclusions_manage_save_failed)
    val defaultsRestoredMessage =
        stringResource(sharedR.string.sync_exclusions_manage_snackbar_defaults_restored)
    val exclusionAddedMessage =
        stringResource(sharedR.string.sync_exclusions_manage_snackbar_added)
    val exclusionUpdatedMessage =
        stringResource(sharedR.string.sync_exclusions_manage_snackbar_updated)

    SyncExclusionResultEffect(
        monitorResult = monitorResult,
        clearResult = clearResult,
        onExclusionAdded = viewModel::onExclusionAdded,
        onRuleEdited = { result ->
            when (result) {
                is EditSyncExclusionRuleResult.Saved ->
                    viewModel.onRuleEdited(result.ruleIndex, result.expected, result.edit)

                is EditSyncExclusionRuleResult.Deleted ->
                    viewModel.onRuleRemoved(result.ruleIndex, result.expected)
            }
        },
        onSizeBoundsChanged = { viewModel.onSizeBoundsChanged(it.bounds) },
    )

    EventEffect(
        event = uiState.saveFailedEvent,
        onConsumed = viewModel::onSaveFailureShown,
    ) {
        snackbarHostState?.showSnackbar(saveFailedMessage)
    }

    EventEffect(
        event = uiState.defaultsRestoredEvent,
        onConsumed = viewModel::onDefaultsRestoredShown,
    ) {
        snackbarHostState?.showSnackbar(defaultsRestoredMessage)
    }

    EventEffect(
        event = uiState.exclusionAddedEvent,
        onConsumed = viewModel::onExclusionAddedShown,
    ) {
        snackbarHostState?.showSnackbar(exclusionAddedMessage)
    }

    EventEffect(
        event = uiState.exclusionUpdatedEvent,
        onConsumed = viewModel::onExclusionUpdatedShown,
    ) {
        snackbarHostState?.showSnackbar(exclusionUpdatedMessage)
    }

    EventEffect(
        event = uiState.editErrorEvent,
        onConsumed = viewModel::onEditErrorShown,
    ) { error ->
        snackbarHostState?.showSnackbar(resources.getString(editErrorMapper(error)))
    }

    EventEffect(
        event = uiState.reviewSavedEvent,
        onConsumed = viewModel::onReviewSavedShown,
    ) { exclusions ->
        onReviewSaved(exclusions)
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
        onAddClicked = { onAddExclusion(syncId, uiState.staged?.rules.orEmpty()) },
        onSaveClicked = viewModel::onSaveClicked,
        onRetryClicked = viewModel::onRetryClicked,
        onRuleClicked = onEditRule,
        onExtensionRemoved = { viewModel.onRuleRemoved(it.ruleIndex, it.rule) },
        onSizeClicked = { uiState.staged?.size?.let(onEditSize) },
        onRestoreDefaultsClicked = viewModel::onDefaultsRestored,
        modifier = modifier,
    )
}

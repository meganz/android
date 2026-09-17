package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion

/**
 * Binds [EditSyncSizeExclusionScreen] to the draft the view model holds.
 *
 * @param exclusion the bounds the exclusions list showed, which the form opens prefilled from
 * @param onDelete invoked when the user takes the bounds out through the trash action, clearing
 * both bounds rather than only the one the chosen condition names
 * @param onSave invoked when the user keeps the bounds as the form now reads them
 * @param onDismiss invoked when the user backs out or cancels, discarding the draft
 */
@Composable
internal fun EditSyncSizeExclusionRoute(
    exclusion: SyncSizeExclusion,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditSyncSizeExclusionViewModel =
        hiltViewModel<EditSyncSizeExclusionViewModel, EditSyncSizeExclusionViewModel.Factory> {
            it.create(exclusion)
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditSyncSizeExclusionScreen(
        uiState = uiState,
        onConditionSelected = viewModel::onConditionSelected,
        onFirstBoundValueChanged = viewModel::onFirstBoundValueChanged,
        onFirstBoundUnitSelected = viewModel::onFirstBoundUnitSelected,
        onSecondBoundValueChanged = viewModel::onSecondBoundValueChanged,
        onSecondBoundUnitSelected = viewModel::onSecondBoundUnitSelected,
        onDelete = onDelete,
        onSave = onSave,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

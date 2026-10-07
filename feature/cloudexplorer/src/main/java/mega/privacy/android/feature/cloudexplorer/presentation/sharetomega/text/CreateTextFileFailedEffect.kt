package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Shows an error snackbar when the temporary share-text file could not be created.
 *
 * @param event [ShareTextToMegaUiState.Data.createFileFailedEvent]
 * @param onConsumed called once the snackbar has been shown
 */
@Composable
internal fun CreateTextFileFailedEffect(
    event: StateEvent,
    onConsumed: () -> Unit,
) {
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current

    EventEffect(event = event, onConsumed = onConsumed) {
        snackbarHostState?.showAutoDurationSnackbar(
            resources.getString(sharedR.string.general_text_error)
        )
    }
}

package mega.privacy.android.app.presentation.meeting.view

import android.annotation.SuppressLint
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Show there are participants waiting in the waiting room dialog
 *
 * @param state                     [InMeetingUiState]
 * @param onSnackbarMessageConsumed Action when the snackbar message was consumed.
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SpeakerCallView(
    state: InMeetingUiState,
    onSnackbarMessageConsumed: () -> Unit,
) {
    if (!state.snackbarInSpeakerViewMessage.equals(consumed)) {
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
        )

        EventEffect(
            event = state.snackbarInSpeakerViewMessage,
            onConsumed = onSnackbarMessageConsumed
        ) {
            snackbarHostState.showAutoDurationSnackbar(
                message = it,
            )
        }
    }
}
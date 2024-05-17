package mega.privacy.android.app.presentation.meeting.view


import android.annotation.SuppressLint
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar

/**
 * Show compose view snackbar in meeting fragment
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SnackbarInMeetingView(
    viewModel: InMeetingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.state.collectAsStateWithLifecycle()

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
    ) { data ->
        MegaSnackbar(snackbarData = data)
    }

    EventEffect(
        event = uiState.snackbarMessage,
        onConsumed = { viewModel.onSnackbarMessageConsumed() }
    ) {
        if (!uiState.snackbarMessage.equals(consumed)) {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = context.getString(R.string.meetings_lower_hand_option_button),
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                viewModel.lowerHandToStopSpeak()
            }
        }
    }
}
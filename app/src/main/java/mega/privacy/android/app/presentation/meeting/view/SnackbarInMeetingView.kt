package mega.privacy.android.app.presentation.meeting.view

import android.annotation.SuppressLint
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Show compose view snackbar in meeting fragment
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SnackbarInMeetingView(
    meetingActivityViewModel: MeetingActivityViewModel = hiltViewModel(),
    inMeetingViewModel: InMeetingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val uiState by meetingActivityViewModel.state.collectAsStateWithLifecycle()

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
    ) { data ->
        MegaSnackbar(snackbarData = data)
    }

    EventEffect(
        event = uiState.handRaisedSnackbarMsg,
        onConsumed = {}
    ) {
        if (!uiState.handRaisedSnackbarMsg.equals(consumed) && !uiState.isBottomPanelExpanded) {
            coroutineScope.launch {
                val result = snackbarHostState.showAutoDurationSnackbar(
                    message = it,
                    actionLabel = if (uiState.showLowerHandButtonInSnackbar)
                        context.getString(R.string.meetings_lower_hand_option_button)
                    else
                        context.getString(R.string.contact_view),
                )

                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        when {
                            uiState.showLowerHandButtonInSnackbar -> inMeetingViewModel.lowerHandToStopSpeak()
                            else -> meetingActivityViewModel.showParticipantsList(shouldBeShown = true)
                        }
                    }

                    SnackbarResult.Dismissed -> meetingActivityViewModel.onHandRaisedSnackbarMsgConsumed()
                }
            }
        }
    }
}
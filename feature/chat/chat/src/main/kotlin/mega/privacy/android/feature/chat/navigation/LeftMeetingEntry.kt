package mega.privacy.android.feature.chat.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.chat.meeting.left.LeftMeetingViewModel
import mega.privacy.android.feature.chat.meeting.left.view.LeftMeetingScreen

/**
 * Left meeting entry rendering the Compose left meeting screen.
 *
 * Hosted by the app module's gated `LeftMeetingNavKey` destination.
 *
 * @param callEndedDueToFreePlanLimits Whether the call hit the free plan participant cap.
 * @param callEndedDueToTooManyParticipants Whether the call was already full.
 * @param onClose Called when the guest dismisses the screen.
 * @param onCreateAccount Called when the guest chooses to create an account.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun LeftMeetingEntry(
    callEndedDueToFreePlanLimits: Boolean,
    callEndedDueToTooManyParticipants: Boolean,
    onClose: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val viewModel = hiltViewModel<LeftMeetingViewModel, LeftMeetingViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                LeftMeetingViewModel.Args(
                    callEndedDueToFreePlanLimits = callEndedDueToFreePlanLimits,
                    callEndedDueToTooManyParticipants = callEndedDueToTooManyParticipants,
                )
            )
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LeftMeetingScreen(
        uiState = uiState,
        onCloseClick = onClose,
        onCreateAccountClick = onCreateAccount,
        onNoticeShown = viewModel::onNoticeShown,
    )
}

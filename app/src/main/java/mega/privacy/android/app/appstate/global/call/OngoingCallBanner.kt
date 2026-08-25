package mega.privacy.android.app.appstate.global.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.feature.chat.components.ReturnToCallBanner

/**
 * Global return-to-call banner rendered in the app shell.
 *
 * Shows only when there is a call in progress and navigates back to the meeting when tapped.
 *
 * @param modifier [Modifier]
 */
@Composable
fun OngoingCallBanner(modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<OngoingCallBannerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OngoingCallBannerContent(uiState = uiState, modifier = modifier)
}

@Composable
internal fun OngoingCallBannerContent(
    uiState: OngoingCallBannerUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    uiState.currentCall?.let { call ->
        ReturnToCallBanner(
            text = stringResource(id = R.string.call_in_progress_layout),
            callStartTimestampSeconds = call.initialTimestamp,
            onClick = { startMeetingActivity(context, call.chatId) },
            modifier = modifier,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OngoingCallBannerContentPreview() {
    AndroidThemeForPreviews {
        OngoingCallBannerContent(
            uiState = OngoingCallBannerUiState(
                currentCall = ChatCall(chatId = 1L, callId = 1L),
                themeMode = ThemeMode.System,
            ),
        )
    }
}

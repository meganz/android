package mega.privacy.android.app.appstate.global.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * Global return-to-call banner for the app shell, to be provided through
 * [mega.android.core.ui.components.LocalMegaScaffoldTopBanner] so each screen's scaffold renders
 * it beneath its own top bar.
 *
 * The state is collected once by the caller, so every scaffold shares it instead of each nav entry
 * creating its own [OngoingCallBannerViewModel]. The banner shows only when there is a call in
 * progress and navigates back to the meeting when tapped.
 */
@Composable
fun rememberOngoingCallBanner(): @Composable () -> Unit {
    val viewModel = hiltViewModel<OngoingCallBannerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    return remember<@Composable () -> Unit> { { OngoingCallBannerContent(uiState = uiState) } }
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

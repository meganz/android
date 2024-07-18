package mega.privacy.android.app.main.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.shared.original.core.ui.controls.chat.ReturnToCallBanner
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun OngoingCallBanner(
    viewModel: OngoingCallViewModel = hiltViewModel(),
    onShow: (Boolean) -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    OngoingCallBannerContent(uiState = uiState, onShow = onShow)
}

@Composable
internal fun OngoingCallBannerContent(
    uiState: OngoingCallUiState,
    onShow: (Boolean) -> Unit,
) {
    LaunchedEffect(uiState.isShown) {
        onShow(uiState.isShown)
    }
    val context = LocalContext.current
    uiState.currentCall?.takeIf { uiState.isShown }?.let { call ->
        OriginalTempTheme(isDark = uiState.themeMode.isDarkMode()) {
            ReturnToCallBanner(
                text = stringResource(id = R.string.call_in_progress_layout),
                onBannerClicked = { startMeetingActivity(context, call.chatId) },
                duration = uiState.getDurationFromInitialTimestamp()
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun OngoingCallBannerPreview() {
    OngoingCallBannerContent(
        uiState = OngoingCallUiState(
            isShown = true,
            currentCall = ChatCall(chatId = 1L, callId = 1L)
        ),
        onShow = {}
    )
}
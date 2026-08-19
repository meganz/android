package mega.privacy.android.feature.chat.meeting.recording.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import mega.privacy.android.feature.chat.meeting.recording.model.CallRecordingConsentUiState

@Composable
fun CallRecordingConsentView(
    state: CallRecordingConsentUiState,
    onDisplayed: (Long) -> Unit,
    onConsent: (Long) -> Unit,
    onDecline: (Long) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToWebUrl: (String) -> Unit,
) {
    LaunchedEffect(state) {
        if (state is CallRecordingConsentUiState.ConsentAlreadyHandled) {
            onDismiss()
        }
    }

    if (state is CallRecordingConsentUiState.ConsentRequired) {
        CallRecordingConsentDialog(
            onDisplayed = { onDisplayed(state.chatId) },
            onConfirm = {
                onConsent(state.chatId)
                onDismiss()
            },
            onDismiss = {
                onDecline(state.chatId)
                onDismiss()
            },
            privacyUrl = state.privacyUrl,
            onPrivacyLinkClick = onNavigateToWebUrl,
        )
    }
}
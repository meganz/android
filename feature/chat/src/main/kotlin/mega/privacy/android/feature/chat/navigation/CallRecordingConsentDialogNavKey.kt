package mega.privacy.android.feature.chat.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.chat.meeting.recording.CallRecordingConsentDialogViewModel
import mega.privacy.android.feature.chat.meeting.recording.view.CallRecordingConsentView
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.WebSiteNavKey

@Serializable
data class CallRecordingConsentDialogNavKey(val chatId: Long) : DialogNavKey

fun EntryProviderScope<DialogNavKey>.callRecordingConsentDialog(
    navigate: (NavKey) -> Unit,
    onHandled: () -> Unit,
    remove: (NavKey) -> Unit,
) {
    entry<CallRecordingConsentDialogNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val viewModel = hiltViewModel<CallRecordingConsentDialogViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CallRecordingConsentView(
            state = state,
            onDismiss = {
                onHandled()
                remove(key)
            },
            onDisplayed = viewModel::onDisplayed,
            onConsent = viewModel::accept,
            onDecline = viewModel::decline,
            onNavigateToWebUrl = { navigate(WebSiteNavKey(it)) },
        )
    }
}
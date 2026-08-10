package mega.privacy.android.feature.contact.info.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.feature.contact.info.ContactInfoViewModel
import mega.privacy.android.feature.contact.info.view.ContactInfoScreen
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Contact info entry. Renders the Compose contact info screen for the contact resolved either
 * from [email] (contact list entry point) or from [chatId] (1:1 chat entry point). Pops itself
 * when the contact cannot be resolved.
 *
 * Hosted by the app module's gated `ContactInfoNavKey` destination (behind `ContactInfoComposeUI`).
 *
 * @param navigationHandler
 * @param email email of the contact, or null when entering from a chat.
 * @param chatId id of the 1:1 chat with the contact, or null when entering by email.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun ContactInfoEntry(
    navigationHandler: NavigationHandler,
    email: String?,
    chatId: Long?,
) {
    val viewModel = hiltViewModel<ContactInfoViewModel, ContactInfoViewModel.Factory> { factory ->
        factory.create(email = email, chatId = chatId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EventEffect(
        event = state.closeEvent,
        onConsumed = viewModel::onCloseEventConsumed,
    ) {
        navigationHandler.back()
    }
    ContactInfoScreen(
        state = state,
        onNavigateBack = navigationHandler::back,
        onSendMessageClick = {},
        onStartAudioCallClick = {},
        onStartVideoCallClick = {},
        onNicknameClick = {},
        onVerifyCredentialsClick = {},
        onShareContactClick = {},
        onSharedFoldersClick = {},
        onNotificationToggled = {},
        onSharedFilesClick = {},
        onManageChatHistoryClick = {},
        onRemoveContactClick = {},
    )
}

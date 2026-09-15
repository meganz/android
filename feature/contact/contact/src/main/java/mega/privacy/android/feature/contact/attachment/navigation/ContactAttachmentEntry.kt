package mega.privacy.android.feature.contact.attachment.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.feature.contact.attachment.ContactAttachmentViewModel
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentUiState
import mega.privacy.android.feature.contact.attachment.view.ContactAttachmentScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

/**
 * Contact attachment entry. Renders the Compose screen listing the contacts attached to the chat
 * message identified by [chatId] and [messageId]. Row "view contact info" routes through the gated
 * [ContactInfoNavKey] destination, and starting a conversation navigates to the resulting chat.
 *
 * Hosted by the app module's gated `ContactAttachmentNavKey` destination (behind
 * `ContactComposeFeature`).
 *
 * @param navigationHandler
 * @param chatId Chat id containing the message.
 * @param messageId Id of the message holding the contact attachments.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun ContactAttachmentEntry(
    navigationHandler: NavigationHandler,
    chatId: Long,
    messageId: Long,
) {
    val viewModel =
        hiltViewModel<ContactAttachmentViewModel, ContactAttachmentViewModel.Factory> { factory ->
            factory.create(chatId = chatId, messageId = messageId)
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    (state as? ContactAttachmentUiState.Data)?.let { data ->
        EventEffect(
            event = data.openChatEvent,
            onConsumed = viewModel::onOpenChatEventConsumed,
        ) { openChatId ->
            navigationHandler.navigate(ShowChatMessagesNavKey(openChatId))
        }
        EventEffect(
            event = data.openContactInfoEvent,
            onConsumed = viewModel::onOpenContactInfoEventConsumed,
        ) { email ->
            navigationHandler.navigate(ContactInfoNavKey(email = email))
        }
    }

    ContactAttachmentScreen(
        state = state,
        onNavigateBack = navigationHandler::back,
        onPrimaryActionClick = viewModel::onPrimaryActionClick,
        onContactClicked = viewModel::onContactClicked,
        onViewContactInfo = { email ->
            navigationHandler.navigate(ContactInfoNavKey(email = email))
        },
        onStartConversation = viewModel::onStartConversation,
        onInviteContact = viewModel::onInviteContact,
        onMessageEventConsumed = viewModel::onMessageEventConsumed,
    )
}

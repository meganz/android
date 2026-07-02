package mega.privacy.android.feature.contact.group.create.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.group.create.CreateGroupChatViewModel
import mega.privacy.android.feature.contact.group.create.view.CreateGroupChatScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey

/**
 * Create group chat entry. Renders the Compose "create group chat" screen (contacts multi-select plus
 * a group settings step) and, on confirm, publishes a [CreateGroupChatNavKey.NewGroupChatResult] under
 * [CreateGroupChatNavKey.KEY]. The screen reports the selection and settings; the consuming caller
 * performs the actual group creation, mirroring the legacy `AddContactActivity` group-mode contract.
 *
 * Hosted by the app module's gated `CreateGroupChatNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 * @param viewModel
 */
@Composable
fun CreateGroupChatEntry(
    navigationHandler: NavigationHandler,
    viewModel: CreateGroupChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CreateGroupChatScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onConfirm = { handles, title, isEkr, isChatLink, allowAddParticipants ->
            navigationHandler.returnResult(
                CreateGroupChatNavKey.KEY,
                CreateGroupChatNavKey.NewGroupChatResult(
                    emails = viewModel.emailsForSelected(handles),
                    title = title,
                    isEkr = isEkr,
                    isChatLink = isChatLink,
                    allowAddParticipants = allowAddParticipants,
                ),
            )
        },
        onBack = { navigationHandler.remove(CreateGroupChatNavKey) },
    )
}

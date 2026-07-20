package mega.privacy.android.feature.contact.group.create.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.group.create.CreateChatViewModel
import mega.privacy.android.feature.contact.group.create.view.NewChatScreen
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.NewChatNavKey

/**
 * New chat entry. Renders the Compose [NewChatScreen] — a single mode-less MEGA-contacts picker whose
 * selection count decides the outcome — and publishes a [NewChatNavKey.NewChatResult] under
 * [NewChatNavKey.KEY]: a single selected contact yields a 1:1 result (null group settings); two or
 * more yields a group result carrying the chosen settings. The consuming caller creates the chat and
 * sends its content into it, mirroring the legacy `AddContactActivity` new-chat contract.
 *
 * Hosted by the app module's gated `NewChatNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun NewChatEntry(
    navigationHandler: NavigationHandler,
) {
    val viewModel: CreateChatViewModel = hiltViewModel<CreateChatViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NewChatScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onConfirmOneToOne = { handle ->
            navigationHandler.returnResult(
                NewChatNavKey.KEY,
                NewChatNavKey.NewChatResult(
                    emails = viewModel.emailsForSelected(setOf(handle)),
                    groupSettings = null,
                ),
            )
        },
        onConfirmGroup = { handles, title, isEkr, isChatLink, allowAddParticipants, imageUri ->
            navigationHandler.returnResult(
                NewChatNavKey.KEY,
                NewChatNavKey.NewChatResult(
                    emails = viewModel.emailsForSelected(handles),
                    groupSettings = NewChatNavKey.NewChatResult.GroupSettings(
                        title = title,
                        isEkr = isEkr,
                        isChatLink = isChatLink,
                        allowAddParticipants = allowAddParticipants,
                        imageUri = imageUri?.let(::UriPath),
                    ),
                ),
            )
        },
        onBack = { navigationHandler.remove(NewChatNavKey) },
    )
}

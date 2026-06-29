package mega.privacy.android.feature.contact.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.add.AddContactViewModel
import mega.privacy.android.feature.contact.add.view.AddContactsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AddChatParticipantsNavKey
import mega.privacy.android.navigation.destination.AddContactsNavKey
import mega.privacy.android.navigation.destination.AddMeetingParticipantsNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Add contacts entry. Renders the Compose MEGA-contacts multi-select picker and publishes the
 * selected contact emails as a `List<String>` under [AddContactsNavKey.KEY] when confirmed.
 *
 * Hosted by the app module's gated `AddContactsNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun AddContactsEntry(
    navigationHandler: NavigationHandler,
) {
    val viewModel = hiltViewModel<AddContactViewModel, AddContactViewModel.Factory> { factory ->
        factory.create(chatId = null, monitorCallLimit = false)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddContactsScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onConfirm = { handles ->
            navigationHandler.returnResult(
                AddContactsNavKey.KEY,
                viewModel.emailsForSelected(handles),
            )
        },
        onBack = { navigationHandler.remove(AddContactsNavKey) },
    )
}

/**
 * Add chat participants entry. Renders the same multi-select picker filtered to the contacts that
 * are not yet participants of [chatId], and publishes the selected emails as a `List<String>` under
 * [AddChatParticipantsNavKey.KEY] when confirmed.
 *
 * Hosted by the app module's gated `AddChatParticipantsNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 * @param chatId the chat whose existing participants are excluded from the picker.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun AddChatParticipantsEntry(
    navigationHandler: NavigationHandler,
    chatId: Long,
) {
    val viewModel = hiltViewModel<AddContactViewModel, AddContactViewModel.Factory> { factory ->
        factory.create(chatId = chatId, monitorCallLimit = false)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddContactsScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onConfirm = { handles ->
            navigationHandler.returnResult(
                AddChatParticipantsNavKey.KEY,
                viewModel.emailsForSelected(handles),
            )
        },
        onBack = { navigationHandler.remove(AddChatParticipantsNavKey(chatId)) },
        titleRes = sharedR.string.add_participants_title,
    )
}

/**
 * Add meeting participants entry. Like [AddChatParticipantsEntry] but for an in-call/meeting context:
 * the picker additionally surfaces the call user-limit warning. Publishes the selected emails as a
 * `List<String>` under [AddMeetingParticipantsNavKey.KEY] when confirmed.
 *
 * Hosted by the app module's gated `AddMeetingParticipantsNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 * @param chatId the meeting chat whose existing participants are excluded and whose call is monitored.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun AddMeetingParticipantsEntry(
    navigationHandler: NavigationHandler,
    chatId: Long,
) {
    val viewModel = hiltViewModel<AddContactViewModel, AddContactViewModel.Factory> { factory ->
        factory.create(chatId = chatId, monitorCallLimit = true)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddContactsScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onConfirm = { handles ->
            navigationHandler.returnResult(
                AddMeetingParticipantsNavKey.KEY,
                viewModel.emailsForSelected(handles),
            )
        },
        onBack = { navigationHandler.remove(AddMeetingParticipantsNavKey(chatId)) },
        titleRes = sharedR.string.add_participants_title,
    )
}

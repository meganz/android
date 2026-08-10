package mega.privacy.android.feature.contact.group.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.feature.contact.group.ContactGroupsViewModel
import mega.privacy.android.feature.contact.group.view.ContactGroupsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import mega.privacy.mobile.analytics.event.ContactGroupListScreenEvent

/**
 * Navigation key for the contact groups screen. Internal to the contact feature module as no
 * external feature navigates to this screen directly.
 */
@Serializable
internal data object ContactGroupsNavKey : NavKey

/**
 * Wires the [ContactGroupsScreen] to its [ContactGroupsViewModel] and navigation. Creating a
 * group launches the participant picker via [CreateGroupChatNavKey]; the returned selection is
 * forwarded to the view model, and the resulting chat is opened via [ShowChatMessagesNavKey].
 */
internal fun EntryProviderScope<NavKey>.contactGroups(navigationHandler: NavigationHandler) {
    entry<ContactGroupsNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(ContactGroupListScreenEvent)
        }
    ) {
        val viewModel: ContactGroupsViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        val newGroupChatResult by navigationHandler
            .monitorResult<Any?>(CreateGroupChatNavKey.KEY)
            .collectAsStateWithLifecycle(initialValue = null)

        LaunchedEffect(newGroupChatResult) {
            (newGroupChatResult as? CreateGroupChatNavKey.NewGroupChatResult)?.let { result ->
                navigationHandler.clearResult(CreateGroupChatNavKey.KEY)
                viewModel.createGroupChat(
                    participantEmails = ArrayList(result.emails),
                    chatTitle = result.title,
                    allowAddParticipants = result.allowAddParticipants,
                )
            }
        }

        ContactGroupsScreen(
            state = state,
            onSearchQueryChange = viewModel::setQuery,
            onGroupClick = { chatId -> navigationHandler.navigate(ShowChatMessagesNavKey(chatId)) },
            onCreateGroupClick = {
                navigationHandler.navigate(CreateGroupChatNavKey(allowEmptyGroup = false))
            },
            onGroupChatCreatedConsumed = viewModel::onGroupChatCreatedConsumed,
            onNavigateToChat = { chatId ->
                navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
            },
            onBackClick = navigationHandler::back,
        )
    }
}

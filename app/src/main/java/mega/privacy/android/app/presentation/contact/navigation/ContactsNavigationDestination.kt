package mega.privacy.android.app.presentation.contact.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.feature.contact.list.ContactListViewModel
import mega.privacy.android.feature.contact.list.view.ContactListScreen
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.mobile.analytics.event.ContactListScreenEvent

/**
 * Navigation destination for the contacts list. Behind
 * [AppFeatures.ContactsComposeUI] either renders the Compose
 * [ContactListScreen] inline (flag on) or launches the legacy
 * [ContactsActivity] and pops the entry (flag off).
 */
fun EntryProviderScope<NavKey>.contactsListDestination(
    navigationHandler: NavigationHandler,
) {
    entry<ContactsNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(ContactListScreenEvent)
        }
    ) { key ->
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyContactsEntry({ navigationHandler.remove(key) })
            },
            enabled = {
                ContactsEntry(
                    navigationHandler = navigationHandler,
                )
            },
        )
    }
}

/**
 * Contacts entry
 *
 * @param navigationHandler
 */
@Composable
private fun ContactsEntry(
    navigationHandler: NavigationHandler,
) {
    val viewModel: ContactListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ContactListScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onContactClick = viewModel::getChatRoomId,
        onContactInfoClick = { email ->
            navigationHandler.navigate(ContactInfoNavKey(email))
        },
        onAddContactClick = {
            navigationHandler.navigate(InviteContactNavKey())
        },
        onRequestsClick = {
            navigationHandler.navigate(
                ContactRequestsNavKey(ContactRequestsNavKey.NavType.ReceivedRequests)
            )
        },
        onGroupsClick = { },
        onStartCall = viewModel::onCallTap,
        onRemoveContact = viewModel::removeContact,
        onChatEventConsumed = viewModel::onChatEventConsumed,
        onCallEventConsumed = viewModel::onCallEventConsumed,
        onNavigateToChat = { chatId ->
            navigationHandler.navigate(
                ChatNavKey(
                    chatId = chatId,
                    action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                )
            )
        },
        onStartCallTriggered = { data ->
            navigationHandler.navigate(
                LegacyMeetingNavKey(
                    chatId = data.chatId,
                    meetingInfo = if (data.isExistingCall) {
                        MeetingNavKeyInfo.ReturnToInProgressCall(isGuest = false)
                    } else {
                        MeetingNavKeyInfo.StartOutgoingCall(
                            isAudioEnable = data.hasLocalAudio,
                            isVideoEnable = data.hasLocalVideo,
                        )
                    },
                )
            )
        },
    )
}

/**
 * Legacy contacts entry
 *
 * @param removeDestination
 */
@Composable
private fun LegacyContactsEntry(removeDestination: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.startActivity(ContactsActivity.getListIntent(context))
        removeDestination()
    }
}

/**
 * Navigation destination for ContactsActivity that handles the requests entry points:
 * - SentRequests: Shows sent contact requests
 * - ReceivedRequests: Shows received contact requests
 *
 * Usage examples:
 * - Navigate to sent requests: navController.navigate(ContactsNavKey(ContactsNavKey.ContactsNavType.SentRequests))
 * - Navigate to received requests: navController.navigate(ContactsNavKey(ContactsNavKey.NavType.ReceivedRequests))
 */
fun EntryProviderScope<NavKey>.contactsRequestLegacyDestination(removeDestination: () -> Unit) {
    entry<ContactRequestsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(key.navType) {
            val intent = when (key.navType) {
                ContactRequestsNavKey.NavType.SentRequests -> ContactsActivity.getSentRequestsIntent(
                    context
                )

                ContactRequestsNavKey.NavType.ReceivedRequests -> ContactsActivity.getReceivedRequestsIntent(
                    context
                )
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}

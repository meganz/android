package mega.privacy.android.feature.contact.list.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.contact.list.model.CallEventData
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AddContactFABEvent
import mega.privacy.mobile.analytics.event.ContactItemAvatarSelectedEvent
import mega.privacy.mobile.analytics.event.ContactItemSelectedEvent

/**
 * Contact list screen
 *
 * @param state
 * @param onSearchQueryChange
 * @param onContactClick
 * @param onContactInfoClick
 * @param onAddContactClick
 * @param onRequestsClick
 * @param onGroupsClick
 * @param onStartCall
 * @param onRemoveContact
 * @param onChatEventConsumed
 * @param onCallEventConsumed
 * @param onNavigateToChat
 * @param onStartCallTriggered
 * @param modifier
 */
@Composable
fun ContactListScreen(
    state: ContactListUiState,
    onSearchQueryChange: (String?) -> Unit,
    onContactClick: (handle: Long) -> Unit,
    onContactInfoClick: (email: String) -> Unit,
    onAddContactClick: () -> Unit,
    onRequestsClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onStartCall: (handle: Long, video: Boolean, audio: Boolean) -> Unit,
    onRemoveContact: (email: String) -> Unit,
    onChatEventConsumed: () -> Unit,
    onCallEventConsumed: () -> Unit,
    onNavigateToChat: (chatId: Long) -> Unit,
    onStartCallTriggered: (CallEventData) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedContactForSheet by remember { mutableStateOf<ContactItemUiState?>(null) }
    var contactPendingRemoval by remember { mutableStateOf<ContactItemUiState?>(null) }

    LaunchedEffect(searchActive) {
        if (!searchActive && searchText.isNotEmpty()) {
            searchText = ""
            onSearchQueryChange(null)
        }
    }

    if (state is ContactListUiState.Data) {
        EventEffect(
            event = state.openChatEvent,
            onConsumed = onChatEventConsumed,
        ) { chatId -> onNavigateToChat(chatId) }

        EventEffect(
            event = state.startCallEvent,
            onConsumed = onCallEventConsumed,
        ) { data -> onStartCallTriggered(data) }
    }

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONTACT_LIST_SCREEN_TAG),
        topBar = {
            MegaSearchTopAppBar(
                title = stringResource(sharedR.string.general_section_contacts),
                navigationType = AppBarNavigationType.None,
                query = searchText,
                isSearchingMode = searchActive,
                onQueryChanged = {
                    searchText = it
                    onSearchQueryChange(it.ifBlank { null })
                },
                onSearchingModeChanged = { searchActive = it },
                searchPlaceholder = stringResource(sharedR.string.contacts_search_hint),
            )
        },
        floatingActionButton = {
            if (state is ContactListUiState.Data) {
                MegaFab(
                    modifier = Modifier.testTag(CONTACT_LIST_FAB_TAG),
                    onClick = {
                        Analytics.tracker.trackEvent(AddContactFABEvent)
                        onAddContactClick()
                    },
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
                )
            }
        },
    ) { padding ->
        when (state) {
            ContactListUiState.Loading -> ContactListLoading(padding)

            is ContactListUiState.Data -> {
                val layoutDirection = LocalLayoutDirection.current
                ContactListContent(
                    state = state,
                    searchActive = searchActive,
                    contentPadding = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection),
                        top = padding.calculateTopPadding(),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding() + FAB_BOTTOM_CLEARANCE,
                    ),
                    onContactClick = {
                        Analytics.tracker.trackEvent(ContactItemSelectedEvent)
                        onContactClick(it)
                    },
                    onContactMore = { contact -> selectedContactForSheet = contact },
                    onRequestsClick = onRequestsClick,
                    onGroupsClick = onGroupsClick,
                    onContactInfoClick = {
                        Analytics.tracker.trackEvent(ContactItemAvatarSelectedEvent)
                        onContactInfoClick(it)
                    },
                )
            }
        }
    }

    val selected = selectedContactForSheet
    if (selected != null) {
        ContactActionsBottomSheet(
            contact = selected,
            onDismiss = { selectedContactForSheet = null },
            onSendMessage = {
                selectedContactForSheet = null
                onContactClick(selected.handle)
            },
            onAudioCall = {
                selectedContactForSheet = null
                onStartCall(selected.handle, false, true)
            },
            onVideoCall = {
                selectedContactForSheet = null
                onStartCall(selected.handle, true, true)
            },
            onContactInfo = {
                selectedContactForSheet = null
                if (selected.email.isNotBlank()) onContactInfoClick(selected.email)
            },
            onRemove = {
                selectedContactForSheet = null
                contactPendingRemoval = selected
            },
        )
    }

    val pending = contactPendingRemoval
    if (pending != null) {
        RemoveContactDialog(
            displayName = pending.displayName,
            onConfirm = {
                contactPendingRemoval = null
                if (pending.email.isNotBlank()) onRemoveContact(pending.email)
            },
            onDismiss = { contactPendingRemoval = null },
        )
    }
}

/**
 * Contact list loading
 *
 * @param padding
 */
@Composable
private fun ContactListLoading(padding: PaddingValues) {
    ContactListLoadingView(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag(CONTACT_LIST_LOADING_TAG),
    )
}


private val FAB_BOTTOM_CLEARANCE = 88.dp

internal const val CONTACT_LIST_SCREEN_TAG = "contact_list_screen"
internal const val CONTACT_LIST_LOADING_TAG = "contact_list_screen:loading"
internal const val CONTACT_LIST_FAB_TAG = "contact_list_screen:fab"

internal fun contactGroupHeaderTag(initial: String): String =
    "contact_list_screen:group_header_$initial"

@CombinedThemePreviews
@Composable
private fun ContactListScreenPreview(
    @PreviewParameter(ContactListUiStateProvider::class) state: ContactListUiState,
) {
    AndroidThemeForPreviews {
        ContactListScreen(
            state = state,
            onSearchQueryChange = {},
            onContactClick = {},
            onContactInfoClick = {},
            onAddContactClick = {},
            onRequestsClick = {},
            onGroupsClick = {},
            onStartCall = { _, _, _ -> },
            onRemoveContact = {},
            onChatEventConsumed = {},
            onCallEventConsumed = {},
            onNavigateToChat = {},
            onStartCallTriggered = {},
        )
    }
}

private class ContactListUiStateProvider :
    PreviewParameterProvider<ContactListUiState> {
    override val values = sequenceOf(
        ContactListUiState.Loading,
        ContactListUiState.Data(
            contacts = mapOf(
                "A" to listOf(previewContact(1L, "Alice"), previewContact(2L, "Andrew")),
                "B" to listOf(previewContact(3L, "Bob")),
            ),
            recentlyAddedContacts = persistentListOf(previewContact(4L, "Charlie")),
            incomingRequestCount = 2,
            openChatEvent = consumed(),
            startCallEvent = consumed(),
        ),
    )

    private fun previewContact(
        handle: Long,
        displayName: String,
    ) = ContactItemUiState(
        handle = handle,
        displayName = displayName,
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = Color(0xFF2E7D32),
        ),
        isVerified = false,
        email = "$displayName@test.com",
    )
}

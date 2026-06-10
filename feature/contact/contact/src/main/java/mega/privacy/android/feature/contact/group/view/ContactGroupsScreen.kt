package mega.privacy.android.feature.contact.group.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.shared.contact.model.AvatarData

/**
 * Contact groups screen. Shows the list of the current user's group chats with search and a
 * FAB to create a new group chat.
 *
 * Owns the local search state and delegates rendering to the stateless [ContactGroupsContent].
 *
 * @param state
 * @param onSearchQueryChange Called when the search query changes (null when search is cleared).
 * @param onGroupClick Called with the chat id when a group row is tapped.
 * @param onCreateGroupClick Called when the create-group FAB is tapped.
 * @param onGroupChatCreatedConsumed Called once the group-chat-created event has been handled.
 * @param onNavigateToChat Called with the chat id to open after a group chat is created.
 * @param onBackClick Called when the back navigation icon is tapped.
 * @param modifier
 */
@Composable
fun ContactGroupsScreen(
    state: ContactGroupUiState,
    onSearchQueryChange: (String?) -> Unit,
    onGroupClick: (chatId: Long) -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupChatCreatedConsumed: () -> Unit,
    onNavigateToChat: (chatId: Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchActive) {
        if (!searchActive && searchText.isNotEmpty()) {
            searchText = ""
            onSearchQueryChange(null)
        }
    }

    ContactGroupsContent(
        state = state,
        searchActive = searchActive,
        searchText = searchText,
        onSearchActiveChange = { searchActive = it },
        onSearchTextChange = {
            searchText = it
            onSearchQueryChange(it.ifBlank { null })
        },
        onGroupClick = onGroupClick,
        onCreateGroupClick = onCreateGroupClick,
        onGroupChatCreatedConsumed = onGroupChatCreatedConsumed,
        onNavigateToChat = onNavigateToChat,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@CombinedThemePreviews
@Composable
private fun ContactGroupsScreenPreview(
    @PreviewParameter(ContactGroupUiStateProvider::class) state: ContactGroupUiState,
) {
    AndroidThemeForPreviews {
        ContactGroupsScreen(
            state = state,
            onSearchQueryChange = {},
            onGroupClick = {},
            onCreateGroupClick = {},
            onGroupChatCreatedConsumed = {},
            onNavigateToChat = {},
            onBackClick = {},
        )
    }
}

private class ContactGroupUiStateProvider : PreviewParameterProvider<ContactGroupUiState> {
    override val values = sequenceOf(
        ContactGroupUiState.Loading,
        ContactGroupUiState.Data(
            groups = listOf(
                previewGroup(1L, "Design team", isPrivate = false),
                previewGroup(2L, "Android engineers", isPrivate = true),
            ),
            groupChatCreated = consumed(),
        ),
        ContactGroupUiState.Data(
            groups = emptyList(),
            groupChatCreated = consumed(),
        ),
    )

    private fun previewGroup(chatId: Long, name: String, isPrivate: Boolean) = ContactGroupItem(
        chatId = chatId,
        name = name,
        avatarData = listOf(
            AvatarData.Initials(initials = name.first().toString(), avatarColor = Color(0xFF2E7D32)),
            AvatarData.Initials(initials = "M", avatarColor = Color(0xFF1565C0)),
        ),
        isPrivate = isPrivate,
    )
}

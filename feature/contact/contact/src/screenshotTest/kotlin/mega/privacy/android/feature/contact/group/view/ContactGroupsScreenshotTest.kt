package mega.privacy.android.feature.contact.group.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.shared.contact.model.AvatarData

/**
 * Screenshot tests for the contact groups screen. Each screenshot renders the stateless
 * [ContactGroupsContent] so the new shared strings are captured in the context of the full
 * screen, ready to be mapped to images when uploading to Weblate.
 *
 * String → screenshot coverage:
 * - R.string.contacts_groups_title ("Groups") → [ContactGroupsPopulated], [ContactGroupsEmpty]
 * - R.string.contacts_groups_empty_title ("No groups") → [ContactGroupsEmpty]
 *
 * Not covered here (their UI animates in, so it renders invisible under static screenshot
 * rendering — capture these manually when running the Weblate task):
 * - R.string.contacts_groups_search_hint ("Search groups") — the search field fades in.
 * - R.string.contacts_groups_create_error ("An error occurred when creating the chat") — shown
 *   in a snackbar that animates in.
 */
class ContactGroupsScreenshotTest {

    /**
     * Populated groups list.
     *
     * Visible new strings: R.string.contacts_groups_title ("Groups").
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactGroupsPopulated() {
        AndroidThemeForPreviews {
            ContactGroupsContent(
                state = populatedState(),
                searchActive = false,
                searchText = "",
                onSearchActiveChange = {},
                onSearchTextChange = {},
                onGroupClick = {},
                onCreateGroupClick = {},
                onGroupChatCreatedConsumed = {},
                onNavigateToChat = {},
                onBackClick = {},
            )
        }
    }

    /**
     * Empty state (no group chats).
     *
     * Visible new strings: R.string.contacts_groups_title ("Groups") and
     * R.string.contacts_groups_empty_title ("No groups").
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactGroupsEmpty() {
        AndroidThemeForPreviews {
            ContactGroupsContent(
                state = ContactGroupUiState.Data(
                    groups = emptyList(),
                    groupChatCreated = consumed(),
                ),
                searchActive = false,
                searchText = "",
                onSearchActiveChange = {},
                onSearchTextChange = {},
                onGroupClick = {},
                onCreateGroupClick = {},
                onGroupChatCreatedConsumed = {},
                onNavigateToChat = {},
                onBackClick = {},
            )
        }
    }

    private fun populatedState() = ContactGroupUiState.Data(
        groups = listOf(
            group(1L, "Design team", isPrivate = false),
            group(2L, "Android engineers", isPrivate = true),
        ),
        groupChatCreated = consumed(),
    )

    private fun group(chatId: Long, name: String, isPrivate: Boolean) = ContactGroupItem(
        chatId = chatId,
        name = name,
        avatarData = listOf(
            AvatarData.Initials(initials = name.first().toString(), avatarColor = Color(0xFF2E7D32)),
            AvatarData.Initials(initials = "M", avatarColor = Color(0xFF1565C0)),
        ),
        isPrivate = isPrivate,
    )
}

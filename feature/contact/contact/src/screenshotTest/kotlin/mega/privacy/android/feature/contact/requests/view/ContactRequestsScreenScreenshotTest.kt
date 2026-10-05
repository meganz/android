package mega.privacy.android.feature.contact.requests.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.requests.model.ContactRequestTab
import mega.privacy.android.feature.contact.requests.model.ContactRequestUiItem
import mega.privacy.android.feature.contact.requests.model.ContactRequestsUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

class ContactRequestsScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactRequestsScreenReceivedPopulated() {
        AndroidThemeForPreviews {
            ContactRequestsScreen(
                state = ContactRequestsUiState.Data(
                    received = sampleRequests(isOutgoing = false),
                    sent = sampleRequests(isOutgoing = true),
                    selectedTab = ContactRequestTab.Received,
                    query = null,
                ),
                onSearchQueryChange = {},
                onTabSelected = {},
                onItemAction = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactRequestsScreenSentPopulated() {
        AndroidThemeForPreviews {
            ContactRequestsScreen(
                state = ContactRequestsUiState.Data(
                    received = sampleRequests(isOutgoing = false),
                    sent = sampleRequests(isOutgoing = true),
                    selectedTab = ContactRequestTab.Sent,
                    query = null,
                ),
                onSearchQueryChange = {},
                onTabSelected = {},
                onItemAction = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactRequestsScreenReceivedEmpty() {
        AndroidThemeForPreviews {
            ContactRequestsScreen(
                state = ContactRequestsUiState.Data(
                    received = persistentListOf(),
                    sent = persistentListOf(),
                    selectedTab = ContactRequestTab.Received,
                    query = null,
                ),
                onSearchQueryChange = {},
                onTabSelected = {},
                onItemAction = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactRequestsScreenSentEmpty() {
        AndroidThemeForPreviews {
            ContactRequestsScreen(
                state = ContactRequestsUiState.Data(
                    received = persistentListOf(),
                    sent = persistentListOf(),
                    selectedTab = ContactRequestTab.Sent,
                    query = null,
                ),
                onSearchQueryChange = {},
                onTabSelected = {},
                onItemAction = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactRequestActionsBottomSheetReceived() {
        AndroidThemeForPreviews {
            ContactRequestActionsSheetContent(
                isOutgoing = false,
                onAction = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactRequestActionsBottomSheetSent() {
        AndroidThemeForPreviews {
            ContactRequestActionsSheetContent(
                isOutgoing = true,
                onAction = {},
            )
        }
    }

    private fun sampleRequests(isOutgoing: Boolean): ImmutableList<ContactRequestUiItem> = listOf(
        sampleRequest(1L, "Alice Anderson", "alice@example.com", isOutgoing),
        sampleRequest(2L, "Bob Brown", "bob@example.com", isOutgoing),
        sampleRequest(3L, "Charlie Clark", "charlie@example.com", isOutgoing),
    ).toImmutableList()

    private fun sampleRequest(
        handle: Long,
        displayName: String,
        email: String,
        isOutgoing: Boolean,
    ) = ContactRequestUiItem(
        handle = handle,
        isOutgoing = isOutgoing,
        contact = ContactItemUiState(
            handle = handle,
            displayName = displayName,
            status = ContactItemStatus.Unknown,
            lastSeen = null,
            avatar = AvatarData.Initials(
                initials = displayName.first().toString(),
                avatarColor = Color(0xFF2E7D32),
            ),
            isVerified = false,
            email = email,
        ),
        createdTime = "2 days ago",
    )
}

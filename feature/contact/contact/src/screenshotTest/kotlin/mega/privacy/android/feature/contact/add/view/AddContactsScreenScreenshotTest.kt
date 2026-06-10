package mega.privacy.android.feature.contact.add.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

class AddContactsScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenLoading() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = AddContactUiState.Loading,
                onSearchQueryChange = {},
                onConfirm = {},
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenEmpty() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = AddContactUiState.Data(contacts = persistentListOf(), query = null),
                onSearchQueryChange = {},
                onConfirm = {},
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenWithContacts() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirm = {},
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenOneSelected() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirm = {},
                onBack = {},
                initialSelectedHandles = setOf(1L),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenMultipleSelected() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirm = {},
                onBack = {},
                initialSelectedHandles = setOf(1L, 2L),
            )
        }
    }

    private fun sampleData() = AddContactUiState.Data(
        contacts = listOf(
            contact(1L, "Alice Anderson", "alice@example.com", Color(0xFF2E7D32)),
            contact(2L, "Bob Brown", "bob@example.com", Color(0xFF1565C0)),
            contact(3L, "Charlie Clark", "charlie@example.com", Color(0xFF6A1B9A)),
        ).toImmutableList(),
        query = null,
    )

    private fun contact(
        handle: Long,
        displayName: String,
        email: String,
        avatarColor: Color,
    ) = ContactItemUiState(
        handle = handle,
        displayName = displayName,
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = avatarColor,
        ),
        isVerified = false,
        email = email,
    )
}

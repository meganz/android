package mega.privacy.android.feature.contact.group.create.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.add.view.rememberContactSelectionState
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

class NewChatScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewChatScreenLoading() {
        AndroidThemeForPreviews {
            NewChatScreen(
                state = CreateChatUiState.Loading,
                onSearchQueryChange = {},
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewChatScreenEmpty() {
        AndroidThemeForPreviews {
            NewChatScreen(
                state = CreateChatUiState.Data(
                    contacts = persistentListOf(),
                    query = null,
                    allowGroupImageSelection = true,
                ),
                onSearchQueryChange = {},
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewChatScreenNoneSelected() {
        AndroidThemeForPreviews {
            NewChatScreenContent(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
                step = NewChatStep.Selection,
                selectionState = rememberContactSelectionState(),
                stepChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewChatScreenOneSelected() {
        AndroidThemeForPreviews {
            NewChatScreenContent(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
                step = NewChatStep.Selection,
                selectionState = rememberContactSelectionState(
                    initialSelectedHandles = setOf(1L),
                ),
                stepChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewChatScreenMultipleSelected() {
        AndroidThemeForPreviews {
            NewChatScreenContent(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
                step = NewChatStep.Selection,
                selectionState = rememberContactSelectionState(
                    initialSelectedHandles = setOf(1L, 2L),
                ),
                stepChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NewChatScreenSettings() {
        AndroidThemeForPreviews {
            NewChatScreenContent(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
                step = NewChatStep.Settings,
                selectionState = rememberContactSelectionState(
                    initialSelectedHandles = setOf(1L, 2L),
                ),
                stepChange = {},
            )
        }
    }

    private fun sampleData() = CreateChatUiState.Data(
        contacts = listOf(
            contact(1L, "Alice Anderson", "alice@example.com", Color(0xFF2E7D32)),
            contact(2L, "Bob Brown", "bob@example.com", Color(0xFF1565C0)),
            contact(3L, "Charlie Clark", "charlie@example.com", Color(0xFF6A1B9A)),
        ).toImmutableList(),
        query = null,
        allowGroupImageSelection = true,
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

package mega.privacy.android.feature.contact.add.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.feature.contact.add.model.PhoneContactsSection
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

class AddContactsScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenLoading() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = AddContactUiState.Loading,
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
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
                state = AddContactUiState.Data(
                    contacts = persistentListOf(),
                    query = null,
                    showUserLimitWarning = false,
                    phoneContactsSection = PhoneContactsSection.Hidden,
                    phoneContactsPickedEvent = consumed(),
                ),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenPhoneSectionCollapsed() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(phoneSection = PhoneContactsSection.PermissionRequired),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenPhoneSectionPermissionCta() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(phoneSection = PhoneContactsSection.PermissionRequired),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
                startPhoneSectionExpanded = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenPhoneSectionPickerCta() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(phoneSection = PhoneContactsSection.PickerAvailable(persistentListOf())),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
                startPhoneSectionExpanded = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddContactsScreenPhoneSectionLoadedList() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(
                    phoneSection = PhoneContactsSection.Loaded(
                        listOf(
                            phoneContact("Phone Alice", "pa@example.com", Color(0xFF00838F)),
                            phoneContact("Phone Bob", "pb@example.com", Color(0xFFAD1457)),
                        ).toImmutableList(),
                    ),
                ),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
                startPhoneSectionExpanded = true,
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
                onConfirm = { _, _ -> },
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
                onConfirm = { _, _ -> },
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
                onConfirm = { _, _ -> },
                onBack = {},
                initialSelectedHandles = setOf(1L, 2L),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddChatParticipantsScreen() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
                titleRes = sharedR.string.add_participants_title,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddMeetingParticipantsScreenWithWarning() {
        AndroidThemeForPreviews {
            AddContactsScreen(
                state = sampleData(showUserLimitWarning = true),
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
                titleRes = sharedR.string.add_participants_title,
            )
        }
    }

    private fun sampleData(
        showUserLimitWarning: Boolean = false,
        phoneSection: PhoneContactsSection = PhoneContactsSection.Hidden,
    ) = AddContactUiState.Data(
        contacts = listOf(
            contact(1L, "Alice Anderson", "alice@example.com", Color(0xFF2E7D32)),
            contact(2L, "Bob Brown", "bob@example.com", Color(0xFF1565C0)),
            contact(3L, "Charlie Clark", "charlie@example.com", Color(0xFF6A1B9A)),
        ).toImmutableList(),
        query = null,
        showUserLimitWarning = showUserLimitWarning,
        phoneContactsSection = phoneSection,
        phoneContactsPickedEvent = consumed(),
    )

    private fun phoneContact(
        displayName: String,
        email: String,
        avatarColor: Color,
    ) = ContactItemUiState(
        handle = -1L,
        displayName = displayName,
        status = ContactItemStatus.Unknown,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = avatarColor,
        ),
        isVerified = false,
        email = email,
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

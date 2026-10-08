package mega.privacy.android.feature.contact.invite.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.invite.model.InviteContactUiState
import mega.privacy.android.feature.contact.picker.PhoneContactsSection
import mega.privacy.android.feature.contact.picker.ScannedContactDialog
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Screenshot tests for the invite-contacts screen.
 *
 * Not covered here: the open-camera confirmation dialog. It is shown from a `rememberSaveable` flag
 * that only an [de.palm.composestateevents.EventEffect] flips, and preview rendering never runs
 * effects, so a golden driven by `ongoingCallConfirmEvent` renders the plain screen instead.
 */
class InviteContactsScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteContactsScreenLoading() {
        AndroidThemeForPreviews {
            InviteContactsScreen(
                state = InviteContactUiState.Loading,
                onSearchQueryChange = {},
                onInvite = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteContactsScreenEmpty() {
        AndroidThemeForPreviews {
            InviteContactsScreen(
                state = inviteDataState(PhoneContactsSection.PickerAvailable(persistentListOf())),
                onSearchQueryChange = {},
                onInvite = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteContactsScreenPermissionDenied() {
        AndroidThemeForPreviews {
            InviteContactsScreen(
                state = inviteDataState(PhoneContactsSection.PermissionRequired),
                onSearchQueryChange = {},
                onInvite = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteContactsScreenPickerWithRecipients() {
        AndroidThemeForPreviews {
            InviteContactsScreen(
                state = inviteDataState(
                    PhoneContactsSection.PickerAvailable(
                        persistentListOf(
                            phoneContact("Alice Anderson", "alice@example.com", Color(0xFF00838F)),
                            phoneContact("Bob Brown", "", Color(0xFFAD1457)),
                        ),
                    ),
                ),
                onSearchQueryChange = {},
                onInvite = { _, _ -> },
                onBack = {},
                initialSelectedPhoneEmails = setOf("alice@example.com"),
                initialSelectedManualEmails = setOf("guest@example.org"),
                initialSelectedPhoneNumbers = setOf("+15551234567"),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteContactsScreenScannedContactFoundDialog() {
        AndroidThemeForPreviews {
            InviteContactsScreen(
                state = inviteDataState(PhoneContactsSection.PickerAvailable(persistentListOf()))
                    .copy(
                        scannedContactDialog = ScannedContactDialog.Found(
                            contactName = "Carol Clark",
                            email = "carol@example.com",
                            handle = 42L,
                            avatar = AvatarData.Initials(
                                initials = "C",
                                avatarColor = Color(0xFF6A1B9A),
                            ),
                        ),
                    ),
                onSearchQueryChange = {},
                onInvite = { _, _ -> },
                onBack = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteContactsScreenScannerNotInstalledDialog() {
        AndroidThemeForPreviews {
            InviteContactsScreen(
                state = inviteDataState(PhoneContactsSection.PickerAvailable(persistentListOf()))
                    .copy(scannedContactDialog = ScannedContactDialog.ScannerNotInstalled),
                onSearchQueryChange = {},
                onInvite = { _, _ -> },
                onBack = {},
            )
        }
    }

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
}

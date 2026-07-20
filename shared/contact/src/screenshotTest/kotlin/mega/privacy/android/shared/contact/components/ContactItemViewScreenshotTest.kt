package mega.privacy.android.shared.contact.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.contact.model.AvatarData

class ContactItemViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewOnlineVerified() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Alice Anderson",
                statusText = "Online",
                status = ContactItemStatus.Online,
                avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                isVerified = true,
                onClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewAwayLastSeen() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Bob Brown",
                statusText = "Last seen today at 09:42",
                status = ContactItemStatus.Away,
                avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                isVerified = false,
                onClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewUnknownStatus() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Charlie",
                statusText = null,
                status = ContactItemStatus.Unknown,
                avatar = AvatarData.Initials(initials = "C", avatarColor = Color(0xFF6A1B9A)),
                isVerified = false,
                onClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewSelectionModeUnselected() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Eve",
                statusText = "Busy",
                status = ContactItemStatus.Busy,
                avatar = AvatarData.Initials(initials = "E", avatarColor = Color(0xFFAD1457)),
                isVerified = true,
                onClick = {},
                inSelectionMode = true,
                selected = false,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewSelectionModeSelected() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Eve",
                statusText = "Busy",
                status = ContactItemStatus.Busy,
                avatar = AvatarData.Initials(initials = "E", avatarColor = Color(0xFFAD1457)),
                isVerified = true,
                onClick = {},
                inSelectionMode = true,
                selected = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewMoreMenu() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Eve",
                statusText = "Busy",
                status = ContactItemStatus.Busy,
                avatar = AvatarData.Initials(initials = "E", avatarColor = Color(0xFFAD1457)),
                isVerified = true,
                onClick = {},
                onMoreClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewRemove() {
        AndroidThemeForPreviews {
            ContactItemView(
                displayName = "Eve",
                statusText = "Busy",
                status = ContactItemStatus.Busy,
                avatar = AvatarData.Initials(initials = "E", avatarColor = Color(0xFFAD1457)),
                isVerified = true,
                onRemoveClicked = {},
            )
        }
    }
}
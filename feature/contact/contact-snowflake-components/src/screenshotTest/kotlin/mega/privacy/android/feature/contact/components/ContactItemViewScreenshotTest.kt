package mega.privacy.android.feature.contact.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class ContactItemViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewOnlineVerified() {
        AndroidThemeForPreviews {
            ContactItemView(
                state = ContactItemUiState(
                    displayName = "Alice Anderson",
                    statusText = "Online",
                    status = ContactItemStatus.Online,
                    avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                    isVerified = true,
                ),
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
                state = ContactItemUiState(
                    displayName = "Bob Brown",
                    statusText = "Last seen today at 09:42",
                    status = ContactItemStatus.Away,
                    avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                    isVerified = false,
                ),
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
                state = ContactItemUiState(
                    displayName = "Charlie",
                    statusText = null,
                    status = ContactItemStatus.Unknown,
                    avatar = AvatarData.Initials(initials = "C", avatarColor = Color(0xFF6A1B9A)),
                    isVerified = false,
                ),
                onClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewSelected() {
        AndroidThemeForPreviews {
            ContactItemView(
                state = ContactItemUiState(
                    displayName = "Diana",
                    statusText = "Online",
                    status = ContactItemStatus.Online,
                    avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
                    isVerified = false,
                ),
                onClick = {},
                selected = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactItemViewNoDivider() {
        AndroidThemeForPreviews {
            ContactItemView(
                state = ContactItemUiState(
                    displayName = "Eve",
                    statusText = "Busy",
                    status = ContactItemStatus.Busy,
                    avatar = AvatarData.Initials(initials = "E", avatarColor = Color(0xFFAD1457)),
                    isVerified = true,
                ),
                onClick = {},
                showDivider = false,
            )
        }
    }
}

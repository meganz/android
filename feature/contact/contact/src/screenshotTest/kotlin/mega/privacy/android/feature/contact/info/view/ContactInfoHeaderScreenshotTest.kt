package mega.privacy.android.feature.contact.info.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.contact.model.AvatarData

class ContactInfoHeaderScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoHeaderOnlineWithNickname() {
        AndroidThemeForPreviews {
            ContactInfoHeader(
                avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                displayName = "Alice Anderson",
                userChatStatus = UserChatStatus.Online,
                lastSeenMinutes = null,
                nickname = "Ally",
                email = "alice@example.com",
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoHeaderOfflineWithoutNickname() {
        AndroidThemeForPreviews {
            ContactInfoHeader(
                avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                displayName = "Bob Brown",
                userChatStatus = UserChatStatus.Offline,
                lastSeenMinutes = null,
                nickname = null,
                email = "bob@example.com",
            )
        }
    }
}

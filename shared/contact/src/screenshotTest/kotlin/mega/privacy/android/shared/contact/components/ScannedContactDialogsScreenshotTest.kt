package mega.privacy.android.shared.contact.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.contact.model.AvatarData

class ScannedContactDialogsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScannedContactFound() {
        AndroidThemeForPreviews {
            ScannedContactFoundDialog(
                contactName = "Alice Anderson",
                contactEmail = "alice@mega.co.nz",
                avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                confirmActionText = "Invite",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScannedContactFoundNoAvatar() {
        AndroidThemeForPreviews {
            ScannedContactFoundDialog(
                contactName = "Bob Brown",
                contactEmail = "bob@mega.co.nz",
                avatar = null,
                confirmActionText = "Invite",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScannedContactAlreadyAdded() {
        AndroidThemeForPreviews {
            ScannedContactAlreadyAddedDialog(
                contactEmail = "alice@mega.co.nz",
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScannedContactInvalidCode() {
        AndroidThemeForPreviews {
            ScannedContactInvalidCodeDialog(onDismiss = {})
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScannerModuleNotInstalled() {
        AndroidThemeForPreviews {
            ScannerModuleNotInstalledDialog(onDismiss = {})
        }
    }
}

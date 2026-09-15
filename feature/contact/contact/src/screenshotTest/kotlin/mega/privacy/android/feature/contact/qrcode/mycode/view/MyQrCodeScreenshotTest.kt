package mega.privacy.android.feature.contact.qrcode.mycode.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState
import mega.privacy.android.feature.contact.qrcode.mycode.model.QrCode
import mega.privacy.android.shared.account.presentation.model.TextAvatarContent

/**
 * Screenshot tests for the personal "My QR code" screen. Renders the stateless [MyQrCodeScreen] so
 * the new shared strings are captured in the context of the full screen for the Weblate upload.
 *
 * String → screenshot coverage:
 * - R.string.contacts_qr_screen_title ("QR code") → both states.
 * - R.string.contacts_qr_scan_action ("Scan QR code") → both states.
 * - R.string.contacts_qr_create_action ("Create QR code") → [MyQrCodeCreate].
 *
 * - R.string.contacts_qr_generating ("Generating QR code…") → [MyQrCodeGenerating].
 *
 * The snackbar and overflow-menu strings are covered by [MyQrCodeMessagesScreenshotTest], which
 * renders those fleeting components in isolation.
 */
class MyQrCodeScreenshotTest {

    /**
     * QR code available: title, avatar-overlaid QR, contact link and scan action.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeAvailable() {
        AndroidThemeForPreviews {
            MyQrCodeScreen(
                uiState = MyQrCodeUiState.QRCodeAvailable(
                    qrCode = QrCode(
                        contactLink = "https://mega.nz/C!abcdef",
                        avatarContent = TextAvatarContent(
                            avatarText = "A",
                            backgroundColor = 0xFFF30C14.toInt(),
                            showBorder = true,
                            textSize = 38.sp,
                        ),
                        avatarBgColor = 0xFFF30C14.toInt(),
                        qrCodeFilePath = null,
                    ),
                    message = consumed(),
                ),
                onBackClick = {},
                onCreateQRCode = {},
                onCopyContactLink = {},
                onSaveToFile = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onScanClick = {},
                onMessageConsumed = {},
            )
        }
    }

    /**
     * QR code being generated: the loading indicator and its message.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeGenerating() {
        AndroidThemeForPreviews {
            MyQrCodeScreen(
                uiState = MyQrCodeUiState.CreatingQRCode,
                onBackClick = {},
                onCreateQRCode = {},
                onCopyContactLink = {},
                onSaveToFile = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onScanClick = {},
                onMessageConsumed = {},
            )
        }
    }

    /**
     * No QR code yet: the create-QR-code prompt and scan action.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeCreate() {
        AndroidThemeForPreviews {
            MyQrCodeScreen(
                uiState = MyQrCodeUiState.QRCodeUnavailable(message = consumed()),
                onBackClick = {},
                onCreateQRCode = {},
                onCopyContactLink = {},
                onSaveToFile = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onScanClick = {},
                onMessageConsumed = {},
            )
        }
    }
}

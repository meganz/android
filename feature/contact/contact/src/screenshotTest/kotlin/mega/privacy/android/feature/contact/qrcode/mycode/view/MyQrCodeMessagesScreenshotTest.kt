package mega.privacy.android.feature.contact.qrcode.mycode.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.test.weblate.WeblateSnackbarScreenshot
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screenshot tests for the "My QR code" one-shot messages and overflow-menu actions, which never
 * settle on a static screen. Snackbars are rendered via [WeblateSnackbarScreenshot] and the menu
 * actions as their menu rows, so every new string has a golden for the Weblate upload.
 *
 * String → screenshot coverage:
 * - R.string.contacts_qr_reset_success → [MyQrCodeResetSuccessSnackbar].
 * - R.string.contacts_qr_reset_error → [MyQrCodeResetErrorSnackbar].
 * - R.string.contacts_qr_delete_error → [MyQrCodeDeleteErrorSnackbar].
 * - R.string.contacts_qr_save_success → [MyQrCodeSaveSuccessSnackbar].
 * - R.string.contacts_qr_reset_action, R.string.contacts_qr_delete_action → [MyQrCodeMenuActions].
 */
class MyQrCodeMessagesScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeResetSuccessSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.contacts_qr_reset_success),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeResetErrorSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.contacts_qr_reset_error),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeDeleteErrorSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.contacts_qr_delete_error),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeSaveSuccessSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(
                    sharedR.string.contacts_qr_save_success,
                    "Internal storage/Download",
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MyQrCodeMenuActions() {
        AndroidThemeForPreviews {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MegaText(
                    text = stringResource(sharedR.string.contacts_qr_reset_action),
                    textColor = TextColor.Primary,
                )
                MegaText(
                    text = stringResource(sharedR.string.contacts_qr_delete_action),
                    textColor = TextColor.Primary,
                )
            }
        }
    }
}

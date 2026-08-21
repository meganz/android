package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateSnackbarScreenshot
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Weblate reference renders for the Share link snackbars.
 *
 * A snackbar animates in from the activity-scoped snackbar host, so it never appears in a static
 * render of the screen itself — [WeblateSnackbarScreenshot] pins a MEGA-styled one over the screen
 * content instead, giving translators the message in the context it actually shows up in.
 */
class ShareLinkSnackbarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareLinkCopiedSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.plurals.share_link_copied_snackbar, 1),
            ) {
                ShareLinkScreenForPreview(singleNode)
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareLinkUpdatedAndCopiedSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.share_link_updated_and_copied_snackbar),
            ) {
                ShareLinkScreenForPreview(singleNode)
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareLinkKeyCopiedSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.share_link_key_copied_snackbar),
            ) {
                // The key row only renders when the key is sent separately, which is the only
                // state from which the key can be copied.
                ShareLinkScreenForPreview(singleNode.copy(isKeySeparate = true))
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareLinkPasswordCopiedSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.share_link_password_copied_snackbar),
            ) {
                // The password row only renders for a password-protected link, which is the only
                // state from which the password can be copied.
                ShareLinkScreenForPreview(
                    singleNode.copy(
                        isPasswordSet = true,
                        password = "s3cretPass",
                        linkWithPassword = "https://mega.nz/#P!encryptedLink",
                    )
                )
            }
        }
    }

    @Composable
    private fun ShareLinkScreenForPreview(uiState: ShareLinkUiState.Data) {
        ShareLinkScreen(
            uiState = uiState,
            onBack = {},
            onOpenSettings = {},
            onShareLink = {},
            onCopyLink = {},
            onCopyKey = {},
        )
    }

    private companion object {
        // hasNewLinks stays false so the screen's copy-on-open effect does not reach for the
        // clipboard during a render.
        val singleNode = ShareLinkUiState.Data(
            nodeLinks = listOf(
                ShareLinkNodeItem(
                    handle = 1L,
                    name = "Presentation.pdf",
                    isFolder = false,
                    iconRes = iconPackR.drawable.ic_pdf_medium_solid,
                    sizeInBytes = 10L * 1024 * 1024,
                    modificationTime = 1_749_000_000L,
                    childFolderCount = null,
                    childFileCount = null,
                    link = "https://mega.nz/file/abc123#decryptionKey",
                    linkWithoutKey = "https://mega.nz/file/abc123",
                    key = "decryptionKey",
                ),
            ),
            accountType = null,
        )
    }
}

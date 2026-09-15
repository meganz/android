package mega.privacy.android.feature.contact.qrcode.scan.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult

/**
 * Screenshot tests for the scanned-contact result dialogs. Renders [ScannedContactDialog] directly
 * so the new shared strings are captured for the Weblate upload.
 *
 * String → screenshot coverage:
 * - R.string.contact_invite ("Invite") → [ScanInviteNonContact] (reused shared string).
 * - R.string.contacts_qr_contact_already_exists → [ScanInviteExistingContact].
 * - R.string.contacts_qr_view_contact_action → [ScanInviteExistingContact].
 * - R.string.contacts_qr_invitation_not_sent_dialog_title → [ScanAlreadyContact] (existing string).
 * - R.string.contacts_qr_already_contact_dialog_body → [ScanAlreadyContact] (existing string).
 *
 * - R.string.contacts_qr_invite_sent_dialog_title, R.string.contacts_qr_invite_sent_dialog_body →
 *   [InviteResultSent].
 * - R.string.contacts_qr_already_sent_dialog_body → [InviteResultAlreadySent].
 * - R.string.contacts_qr_own_email_dialog_body → [InviteResultOwnEmail].
 * - R.string.contacts_qr_invite_error_dialog_body → [InviteResultError].
 */
class ScanQrCodeScreenshotTest {

    /**
     * Invite dialog for a scanned non-contact: name, email and the Invite action.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScanInviteNonContact() {
        AndroidThemeForPreviews {
            ScannedContactDialog(
                result = ScannedContactLinkResult(
                    contactName = "Alice Anderson",
                    email = "alice@example.com",
                    handle = 1L,
                    isContact = false,
                    qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK,
                ),
                avatarContentColor = Color(0xFF2E7D32),
                onViewContact = {},
                onInviteContact = { _, _ -> },
                onDismiss = {},
            )
        }
    }

    /**
     * Invite dialog for a scanned contact already in the user's list: the already-a-contact text
     * and the View action.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScanInviteExistingContact() {
        AndroidThemeForPreviews {
            ScannedContactDialog(
                result = ScannedContactLinkResult(
                    contactName = "Alice Anderson",
                    email = "alice@example.com",
                    handle = 1L,
                    isContact = true,
                    qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK,
                ),
                avatarContentColor = Color(0xFF2E7D32),
                onViewContact = {},
                onInviteContact = { _, _ -> },
                onDismiss = {},
            )
        }
    }

    /**
     * Result dialog shown when the scanned contact link already belongs to a contact.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScanAlreadyContact() {
        AndroidThemeForPreviews {
            ScannedContactDialog(
                result = ScannedContactLinkResult(
                    contactName = "Alice Anderson",
                    email = "alice@example.com",
                    handle = 1L,
                    isContact = true,
                    qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_EEXIST,
                ),
                avatarContentColor = Color(0xFF2E7D32),
                onViewContact = {},
                onInviteContact = { _, _ -> },
                onDismiss = {},
            )
        }
    }

    /**
     * Result dialog confirming an invitation was sent.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteResultSent() {
        AndroidThemeForPreviews {
            InviteResultDialog(
                request = InviteContactRequest.Sent,
                email = "alice@example.com",
                onDismiss = {},
            )
        }
    }

    /**
     * Result dialog shown when an invitation to the scanned contact was already sent before.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteResultAlreadySent() {
        AndroidThemeForPreviews {
            InviteResultDialog(
                request = InviteContactRequest.AlreadySent,
                email = "alice@example.com",
                onDismiss = {},
            )
        }
    }

    /**
     * Result dialog shown when the scanned contact link resolves to the user's own account.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteResultOwnEmail() {
        AndroidThemeForPreviews {
            InviteResultDialog(
                request = InviteContactRequest.InvalidEmail,
                email = "me@example.com",
                onDismiss = {},
            )
        }
    }

    /**
     * Result dialog shown when a contact invitation could not be sent due to an error.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteResultError() {
        AndroidThemeForPreviews {
            InviteResultDialog(
                request = InviteContactRequest.InvalidStatus,
                email = null,
                onDismiss = {},
            )
        }
    }
}

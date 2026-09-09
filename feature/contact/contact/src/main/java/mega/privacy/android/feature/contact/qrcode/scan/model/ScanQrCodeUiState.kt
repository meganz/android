package mega.privacy.android.feature.contact.qrcode.scan.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.shared.account.presentation.model.AvatarContent

/**
 * UI state for the QR code scan / invite flow.
 *
 * @property scannedContactLinkResult the details of the last successfully scanned contact
 * @property scannedContactAvatarContent the avatar of the scanned contact, if resolved
 * @property scannedContactEmail the email of the contact the last invite was sent to
 * @property inviteContactResult the result of the last contact invitation
 * @property scanCancel emitted when the user cancels scanning
 * @property message a one-shot message for the UI to surface, if any
 */
@Stable
data class ScanQrCodeUiState(
    val scannedContactLinkResult: StateEventWithContent<ScannedContactLinkResult>,
    val scannedContactAvatarContent: AvatarContent?,
    val scannedContactEmail: String?,
    val inviteContactResult: StateEventWithContent<InviteContactRequest>,
    val scanCancel: StateEvent,
    val message: StateEventWithContent<ScanQrCodeMessage>,
)

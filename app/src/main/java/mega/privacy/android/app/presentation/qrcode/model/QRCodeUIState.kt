package mega.privacy.android.app.presentation.qrcode.model

import android.graphics.Bitmap
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import java.io.File

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.qrcode.QRCodeViewModel]
 *
 * @property avatarContent  Avatar of the user
 * @property contactLink    Contact link of the user
 * @property myQRCodeState  My QR code state
 * @property qrCodeBitmap   Generated QR bitmap, that includes QR code and avatar in the center
 * @property resultMessage  Message ID if there are messages to show as result of operation
 * @property isInProgress   Whether the UI is busy in some progress
 * @property localQRCodeFile Handle to the local QR code file, for the purpose of share.
 * @property hasQRCodeBeenDeleted  Whether user has deleted the QR code
 * @property inviteContactResult   Invite contact request result
 * @property scannedContactLinkResult  Scanned contact details
 * @property scannedContactEmail    Email of the scanned contact
 * @property scannedContactAvatarContent Avatar of the scanned contact
 */
data class QRCodeUIState(
    val myQRCodeState: MyCodeUIState = MyCodeUIState.Idle,
    val avatarContent: AvatarContent? = null,
    val contactLink: String? = null,
    val qrCodeBitmap: Bitmap? = null,
    val resultMessage: StateEventWithContent<Int> = consumed(),
    val isInProgress: Boolean = false,
    val localQRCodeFile: File? = null,
    val hasQRCodeBeenDeleted: Boolean = false,
    val inviteContactResult: StateEventWithContent<InviteContactRequest> = consumed(),
    val scannedContactLinkResult: StateEventWithContent<ScannedContactLinkResult> = consumed(),
    val scannedContactEmail: String? = null,
    val scannedContactAvatarContent: AvatarContent? = null,
)
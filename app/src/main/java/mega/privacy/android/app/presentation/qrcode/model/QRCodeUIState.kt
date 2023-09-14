package mega.privacy.android.app.presentation.qrcode.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import java.io.File

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.qrcode.QRCodeViewModel]
 *
 * @property myQRCodeState  My QR code state
 * @property resultMessage  Message ID if there are messages to show as result of operation
 * @property inviteContactResult   Invite contact request result
 * @property scannedContactLinkResult  Scanned contact details
 * @property scannedContactEmail    Email of the scanned contact
 * @property scannedContactAvatarContent Avatar of the scanned contact
 * @property showCollision  Collision while saving to cloud drive
 * @property uploadFile     Upload file to the given parent handle
 */
data class QRCodeUIState(
    val myQRCodeState: MyCodeUIState = MyCodeUIState.Idle,
    val resultMessage: StateEventWithContent<Pair<Int, Array<Any>>> = consumed(),
    val inviteContactResult: StateEventWithContent<InviteContactRequest> = consumed(),
    val scannedContactLinkResult: StateEventWithContent<ScannedContactLinkResult> = consumed(),
    val scannedContactEmail: String? = null,
    val scannedContactAvatarContent: AvatarContent? = null,
    val showCollision: StateEventWithContent<NameCollision> = consumed(),
    val uploadFile: StateEventWithContent<Pair<File, Long>> = consumed(),
)
package mega.privacy.android.domain.entity.qrcode

import java.io.File

/**
 * Class representing details of the scanned contact to update the UI
 *
 * @property contactName
 * @property email
 * @property handle
 * @property isContact
 * @property avatarFile
 * @property qrCodeQueryResult
 * @property avatarColor
 */
data class ScannedContactLinkResult(
    val contactName: String,
    val email: String,
    val handle: Long,
    val isContact: Boolean,
    val qrCodeQueryResult: QRCodeQueryResults,
    val avatarFile: File? = null,
    val avatarColor: Int? = null,
)

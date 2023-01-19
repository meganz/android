package mega.privacy.android.domain.entity.qrcode

/**
 * Class representing details of the scanned contact to update the UI
 *
 * @property contactName
 * @property email
 * @property handle
 * @property isContact
 * @property qrCodeQueryResult
 */
data class ScannedContactLinkResult(
    val contactName: String,
    val email: String,
    val handle: Long,
    val isContact: Boolean,
    val qrCodeQueryResult: QRCodeQueryResults,
)

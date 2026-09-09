package mega.privacy.android.feature.contact.qrcode.mycode.model

import androidx.annotation.ColorInt
import mega.privacy.android.shared.account.presentation.model.AvatarContent

/**
 * State of the personal "My QR code" content.
 */
sealed interface MyQrCodeContentState {

    /**
     * No QR code is available yet.
     */
    data object Idle : MyQrCodeContentState

    /**
     * A QR code is being created.
     *
     * @property showLoader whether a loading indicator should be displayed
     */
    data class CreatingQRCode(val showLoader: Boolean) : MyQrCodeContentState

    /**
     * A QR code is available.
     *
     * @property contactLink the contact link encoded by the QR code
     * @property avatarContent the current user's avatar
     * @property avatarBgColor the avatar background colour
     * @property qrCodeFilePath the absolute path of the generated QR code image, if any
     */
    data class QRCodeAvailable(
        val contactLink: String,
        val avatarContent: AvatarContent,
        @ColorInt val avatarBgColor: Int,
        val qrCodeFilePath: String?,
    ) : MyQrCodeContentState

    /**
     * The QR code has been deleted.
     */
    data object QRCodeDeleted : MyQrCodeContentState
}

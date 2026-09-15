package mega.privacy.android.feature.contact.qrcode.mycode.model

import androidx.annotation.ColorInt
import mega.privacy.android.shared.account.presentation.model.AvatarContent

/**
 * Qr code
 * Data class — a value holder; `equals`, `hashCode`, `toString` and `copy` are generated.
 *
 * @property contactLink the contact link
 * @property avatarContent the avatar content
 * @property avatarBgColor the avatar bg color
 * @property qrCodeFilePath the qr code file path
 */
data class QrCode(
    val contactLink: String,
    val avatarContent: AvatarContent,
    @ColorInt val avatarBgColor: Int,
    val qrCodeFilePath: String?,
)

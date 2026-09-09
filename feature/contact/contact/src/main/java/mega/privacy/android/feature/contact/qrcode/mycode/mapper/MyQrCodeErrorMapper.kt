package mega.privacy.android.feature.contact.qrcode.mycode.mapper

import mega.privacy.android.domain.exception.QRCodeException
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeError
import javax.inject.Inject

/**
 * Maps a QR code operation failure to a [MyQrCodeError] the UI can render.
 */
class MyQrCodeErrorMapper @Inject constructor() {
    operator fun invoke(error: Throwable): MyQrCodeError = when (error) {
        is QRCodeException.ResetFailed -> MyQrCodeError.ResetFailed
        is QRCodeException.DeleteFailed -> MyQrCodeError.DeleteFailed
        else -> MyQrCodeError.General
    }
}

package mega.privacy.android.domain.exception

/**
 * My QR code exception
 */
sealed class QRCodeException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * QR Code reset failed
     */
    class ResetFailed(errorCode: Int, errorString: String? = null) :
        QRCodeException(errorCode, errorString)

    /**
     * QR Code creation failed
     */
    class CreateFailed(errorCode: Int, errorString: String? = null) :
        QRCodeException(errorCode, errorString)

    /**
     * QR code deletion failed
     */
    class DeleteFailed(errorCode: Int, errorString: String? = null) :
        QRCodeException(errorCode, errorString)

    /**
     * Unknown error
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)
}

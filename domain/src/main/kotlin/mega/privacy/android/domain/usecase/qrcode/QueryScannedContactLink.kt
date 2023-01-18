package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult

/**
 * Use case for getting contact link for scanned qr code
 */
fun interface QueryScannedContactLink {

    /**
     * Invoke
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     * @return Details of the scanned contact
     */
    suspend operator fun invoke(scannedHandle: String): ScannedContactLinkResult
}
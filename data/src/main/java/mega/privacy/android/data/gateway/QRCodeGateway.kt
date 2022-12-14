package mega.privacy.android.data.gateway

import mega.privacy.android.data.model.QRCodeBitSet

/**
 * Gateway to generate the QRCode
 */
interface QRCodeGateway {

    /**
     * Create the Bitset format of QR Code.
     *
     * @param text value of the QRCode
     * @param width width of target bitmap
     * @param height height of target bitmap
     * @return wrapped qrcode format by model [QRCodeBitSet]
     */
    suspend fun createQRCode(
        text: String,
        width: Int,
        height: Int
    ): QRCodeBitSet?
}
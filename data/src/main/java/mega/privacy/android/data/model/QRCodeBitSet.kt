package mega.privacy.android.data.model

import java.util.BitSet


/**
 * Model class of QR Code. It represents the QR code by a bit array.
 * This is the output format of ZXing library.
 */
data class QRCodeBitSet(
    /**
     * width of the QR code
     */
    val width: Int,

    /**
     * height of the QR code
     */
    val height: Int,

    /**
     * Values of the QR Code. Null if there is anything wrong.
     */
    val bits: BitSet?
)
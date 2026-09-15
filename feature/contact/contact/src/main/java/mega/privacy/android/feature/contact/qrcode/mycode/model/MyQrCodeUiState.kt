package mega.privacy.android.feature.contact.qrcode.mycode.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState.CreatingQRCode.message

/**
 * My qr code ui state
 *
 * Sealed hierarchy — all subtypes are known at compile time, ideal for exhaustive `when`.
 *
 * @property message
 */
@Stable
sealed interface MyQrCodeUiState {
    val message: StateEventWithContent<MyQrCodeMessage>

    /**
     * Creating qr code
     * Data class — a value holder; `equals`, `hashCode`, `toString` and `copy` are generated.
     *
     * @constructor Creates a new CreatingQRCode
     * @property message
     */
    data object CreatingQRCode : MyQrCodeUiState {
        override val message: StateEventWithContent<MyQrCodeMessage> = consumed()
    }

    /**
     * Qr code available
     * Data class — a value holder; `equals`, `hashCode`, `toString` and `copy` are generated.
     *
     * @constructor Creates a new QRCodeAvailable
     * @property qrCode the qr code
     * @property message
     */
    data class QRCodeAvailable(
        val qrCode: QrCode,
        override val message: StateEventWithContent<MyQrCodeMessage>,
    ) : MyQrCodeUiState

    /**
     * Qr code unavailable
     * Data class — a value holder; `equals`, `hashCode`, `toString` and `copy` are generated.
     * @property message
     */
    data class QRCodeUnavailable(
        override val message: StateEventWithContent<MyQrCodeMessage>,
    ) : MyQrCodeUiState

}

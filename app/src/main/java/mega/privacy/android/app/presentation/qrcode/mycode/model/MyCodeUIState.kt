package mega.privacy.android.app.presentation.qrcode.mycode.model

import androidx.annotation.ColorInt
import mega.privacy.android.app.presentation.avatar.model.AvatarContent

/**
 * UI state class for MyCodeFragment
 */
sealed interface MyCodeUIState {

    /**
     * State of no available QR code
     */
    object Idle : MyCodeUIState

    /**
     * In the progress of creating QR code
     */
    object CreatingQRCode : MyCodeUIState

    /**
     * QR code is created
     * @property contactLink
     * @property avatarContent
     * @property avatarBgColor
     */
    data class QRCodeAvailable(
        val contactLink: String,
        val avatarContent: AvatarContent,
        @ColorInt val avatarBgColor: Int,
    ) : MyCodeUIState

    /**
     * QR code has been deleted.
     */
    object QRCodeDeleted : MyCodeUIState

    /**
     * Reset QR code succeeded.
     */
    object QRCodeResetDone : MyCodeUIState

    /**
     * Error event
     * @property error error string
     */
    data class Error(val error: String) : MyCodeUIState
}
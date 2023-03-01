package mega.privacy.android.app.presentation.qrcode.mycode.model

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import java.io.File

/**
 * UI state class for MyCodeFragment
 */
data class MyQRCodeUIState(

    /**
     * Contact link of the user
     */
    val contactLink: String? = null,

    /**
     * Generated QR bitmap, that includes QR code and avatar in the center
     */
    val qrCodeBitmap: Bitmap? = null,

    /**
     * Whether the UI is busy in some progress.
     */
    val isInProgress: Boolean = false,

    /**
     * Message ID if there are messages to show as result of operation
     */
    @StringRes val snackBarMessage: Int? = null,

    /**
     * Handle to the local QR code file, for the purpose of share.
     */
    val localQRCodeFile: File? = null,

    /**
     * If background color of default avatar.
     * This is needed to generate default avatar, when avatar file is not available.
     */
    @ColorInt val avatarBgColor: Int? = null,

    /**
     * avatar content
     */
    val avatarContent: AvatarContent? = null,
)
package mega.privacy.android.app.presentation.twofactorauthentication.model

import android.graphics.Bitmap
import mega.privacy.android.app.presentation.twofactorauthentication.model.ScreenType.InitialisationScreen


/**
 * UI State for Two Factor authentication activity
 * @param is2FAFetchCompleted UI state to check if fetching the 2FA code was completed or not
 * @param seed The seed that show in the UI as 13 unique codes
 * @param userEmail The current user email required to generate the 2fa url to be copied to clipboard
 * @param isQRCodeGenerationCompleted UI state to check if generating the QR code process was completed or not
 * @param qrBitmap the qr code bitmap generated from the 2fa codes to display in the view
 * @param isPinSubmitted UI state to determine if the pins got submitted or not
 * @param authenticationState UI state for enabling the two factor authentication
 * @param isMasterKeyExported UI state to change the visibility of some related views in the activity
 * @param twoFactorAuthUrl The 2fa url that gets parsed to the available authentication App
 * @param viewType UI state to show different view state based on user flow in the process
 */
data class TwoFactorAuthenticationUIState(
    val is2FAFetchCompleted: Boolean = false,
    val seed: String? = null,
    val userEmail: String? = null,
    val isQRCodeGenerationCompleted: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val isPinSubmitted: Boolean = false,
    val authenticationState: AuthenticationState? = null,
    val isMasterKeyExported: Boolean = false,
    val twoFactorAuthUrl: String = "",
    val viewType: ScreenType = InitialisationScreen,
)




